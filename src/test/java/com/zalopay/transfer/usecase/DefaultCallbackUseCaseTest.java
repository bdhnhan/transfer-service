package com.zalopay.transfer.usecase;

import com.zalopay.transfer.constants.enums.*;
import com.zalopay.transfer.controller.request.CallbackRequest;
import com.zalopay.transfer.controller.response.CallbackResponse;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.entity.Transaction;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.handler.AbstractHandler;
import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import com.zalopay.transfer.utils.Snowflake;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(SpringExtension.class)
public class DefaultCallbackUseCaseTest {

    @Mock
    private TransferTransactionRepository transferTransactionRepository;
    @Mock
    private TransferInfoRepository transferInfoRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private AbstractHandler abstractHandler;
    private static RedissonClient redissonClient;
    private CallbackUseCase callbackUseCase;
    private final String transactionId = Snowflake.generateID();
    private final String firstSubTransId = Snowflake.generateID();
    private final String secondSubTransId = Snowflake.generateID();

    private final Long amount = 100_000L;
    private Transaction transaction;
    private Map<String, TransferInfo> transferInfoMap = new HashMap<>();


    @BeforeAll
    public static void initBeforeAll() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5)
                .setConnectTimeout(3000);
        redissonClient = Redisson.create(config);
    }

    @BeforeEach
    public void initEach() {
        redissonClient.getMapCache("transId").clear();
        callbackUseCase = new DefaultCallbackUseCase(transferInfoRepository, transferTransactionRepository, applicationContext, applicationEventPublisher, redissonClient);
        AtomicReference<TransferInfo> infoAtomicReference = new AtomicReference<>();
        Mockito.doAnswer(invocation -> {
            infoAtomicReference.set(invocation.getArgument(0));
            transferInfoMap.put(infoAtomicReference.get().getId(), infoAtomicReference.get());
            return infoAtomicReference.get();
        }).when(transferInfoRepository).save(Mockito.any());
        Mockito.doAnswer(invocation -> {
            if (invocation.getArgument(0).equals(transactionId)) {
                return Optional.of(transaction);
            } else {
                return Optional.empty();
            }
        }).when(transferTransactionRepository).findById(Mockito.anyString());
        Mockito.doAnswer(invocationOnMock ->
                        transferInfoMap.values().stream()
                                .filter(transferInfo -> Objects.nonNull(transferInfo.getStepId()))
                                .filter(transferInfo -> transferInfo.getStepId().equals(invocationOnMock.getArgument(0)))
                                .findFirst())
                .when(transferInfoRepository).findByStepId(Mockito.anyString());
        AtomicReference<Transaction> transactionAtomicReference = new AtomicReference<>();
        Mockito.doAnswer(invocationOnMock -> {
            transactionAtomicReference.set(invocationOnMock.getArgument(0));
            transaction = transactionAtomicReference.get();
            return transactionAtomicReference.get();
        }).when(transferTransactionRepository).save(Mockito.any());
        Mockito.doAnswer(invocationOnMock ->
                        transferInfoMap.values().stream()
                                .filter(transferInfo -> Objects.isNull(transferInfo.getStepId()))
                                .filter(transferInfo -> transferInfo.getTransId().equals(invocationOnMock.getArgument(0)))
                                .findFirst())
                .when(transferInfoRepository).findFirstByTransIdAndStepIdIsNullOrderByStepAsc(Mockito.anyString());

        Mockito.doAnswer(invocationOnMock -> abstractHandler)
                .when(applicationContext).getBean(Mockito.anyString());

        Mockito.doAnswer(invocationOnMock -> null).when(abstractHandler).handleTransaction(Mockito.any());
    }

    @Test
    public void testCallbackFromOtherServiceSuccessShouldBeOk() {
        transaction = initTransferTransaction(transactionId, TransactionStatusEnum.PROCESSING.name());
        transferInfoMap = initStepTransaction(transactionId);

        CallbackRequest callbackRequest = CallbackRequest.builder()
                .sourceCallBack("BANK")
                .status(TransactionInfoStatusEnum.COMPLETED.name())
                .transId(firstSubTransId)
                .build();

        ResultResponse<CallbackResponse> response = callbackUseCase.handle(callbackRequest);
        Mockito.verify(abstractHandler, Mockito.times(1)).handleTransaction(Mockito.any());
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any());
        Assertions.assertEquals(200, response.getStatus());
        transferInfoMap.values().stream()
                .filter(transferInfo -> Objects.nonNull(transferInfo.getStepId()) && transferInfo.getStepId().equals(firstSubTransId))
                .forEach(transferInfo -> Assertions.assertEquals(TransactionInfoStatusEnum.COMPLETED, transferInfo.getStatus()));
    }

    @Test
    public void testCallbackFromOtherServiceWithSubTransIdNotFoundShouldBeOk() {
        transaction = initTransferTransaction(transactionId, TransactionStatusEnum.PROCESSING.name());
        transferInfoMap = initStepTransaction(transactionId);

        CallbackRequest callbackRequest = CallbackRequest.builder()
                .sourceCallBack("BANK")
                .status(TransactionInfoStatusEnum.COMPLETED.name())
                .transId(UUID.randomUUID().toString())
                .build();

        ResultResponse<CallbackResponse> response = callbackUseCase.handle(callbackRequest);
        Mockito.verify(abstractHandler, Mockito.times(0)).handleTransaction(Mockito.any());
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any());
        Assertions.assertEquals(ErrorCode.TRANS_ID_IS_NOT_FOUND.getCode(), response.getStatus());
    }

    @Test
    public void testCallbackFromOtherServiceWithSubTransIdIsFinalStepShouldBeOk() {
        transaction = initTransferTransaction(transactionId, TransactionStatusEnum.PROCESSING.name());
        transferInfoMap = initStepTransactionFinalStep(transactionId);

        CallbackRequest callbackRequest = CallbackRequest.builder()
                .sourceCallBack("ZLP_WALLET")
                .status(TransactionInfoStatusEnum.COMPLETED.name())
                .transId(secondSubTransId)
                .build();

        ResultResponse<CallbackResponse> response = callbackUseCase.handle(callbackRequest);
        Mockito.verify(abstractHandler, Mockito.times(0)).handleTransaction(Mockito.any());
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any());
        Assertions.assertEquals(ErrorCode.SUCCESSFULLY.getCode(), response.getStatus());

        transferInfoMap.values().forEach(transferInfo ->
                Assertions.assertEquals(TransactionInfoStatusEnum.COMPLETED, transferInfo.getStatus()));

        Assertions.assertEquals(TransactionStatusEnum.COMPLETED, transaction.getStatus());
        Assertions.assertEquals(TransactionStatusEnum.COMPLETED.name(), redissonClient.getMapCache("transId").get(transactionId));
    }

    @Test
    public void testCallbackFromOtherServiceButFailedShouldBeOk() {
        transaction = initTransferTransaction(transactionId, TransactionStatusEnum.PROCESSING.name());
        transferInfoMap = initStepTransaction(transactionId);

        CallbackRequest callbackRequest = CallbackRequest.builder()
                .sourceCallBack("BANK")
                .status(TransactionInfoStatusEnum.FAILED.name())
                .transId(firstSubTransId)
                .build();

        ResultResponse<CallbackResponse> response = callbackUseCase.handle(callbackRequest);
        Mockito.verify(abstractHandler, Mockito.times(0)).handleTransaction(Mockito.any());
        Mockito.verify(applicationEventPublisher, Mockito.times(1)).publishEvent(Mockito.any());
        Assertions.assertEquals(ErrorCode.SUCCESSFULLY.getCode(), response.getStatus());

        transferInfoMap.values().stream()
                .filter(transferInfo -> Objects.nonNull(transferInfo.getStepId()) && transferInfo.getStepId().equals(firstSubTransId))
                .forEach(transferInfo -> Assertions.assertEquals(TransactionInfoStatusEnum.FAILED, transferInfo.getStatus()));
    }

    @Test
    public void testCallbackFromOtherServiceWithStepRevertingShouldBeOk() {
        transaction = initTransferTransaction(transactionId, TransactionStatusEnum.FAILED.name());
        transferInfoMap = initStepTransactionRevertingCase(transactionId);

        CallbackRequest callbackRequest = CallbackRequest.builder()
                .sourceCallBack("BANK")
                .status(TransactionInfoStatusEnum.COMPLETED.name())
                .transId(firstSubTransId)
                .build();

        ResultResponse<CallbackResponse> response = callbackUseCase.handle(callbackRequest);
        Mockito.verify(abstractHandler, Mockito.times(0)).handleTransaction(Mockito.any());
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any());
        Assertions.assertEquals(ErrorCode.SUCCESSFULLY.getCode(), response.getStatus());

        transferInfoMap.values().stream()
                .filter(transferInfo -> Objects.nonNull(transferInfo.getStepId()) && transferInfo.getStepId().equals(firstSubTransId))
                .forEach(transferInfo -> Assertions.assertEquals(TransactionInfoStatusEnum.ROLLBACK, transferInfo.getStatus()));
    }

    @Test
    public void testCallbackFromOtherServiceWithStepRevertingButFailed() {
        transaction = initTransferTransaction(transactionId, TransactionStatusEnum.FAILED.name());
        transferInfoMap = initStepTransactionRevertingCase(transactionId);

        CallbackRequest callbackRequest = CallbackRequest.builder()
                .sourceCallBack("BANK")
                .status(TransactionInfoStatusEnum.FAILED.name())
                .transId(firstSubTransId)
                .build();

        ResultResponse<CallbackResponse> response = callbackUseCase.handle(callbackRequest);
        Mockito.verify(abstractHandler, Mockito.times(0)).handleTransaction(Mockito.any());
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any());
        Assertions.assertEquals(ErrorCode.SUCCESSFULLY.getCode(), response.getStatus());

        transferInfoMap.values().stream()
                .filter(transferInfo -> Objects.nonNull(transferInfo.getStepId()) && transferInfo.getStepId().equals(firstSubTransId))
                .forEach(transferInfo -> Assertions.assertEquals(TransactionInfoStatusEnum.REVERT_FAILED, transferInfo.getStatus()));
    }

    private Transaction initTransferTransaction(String transactionId, String status) {
        return new Transaction(
                transactionId,
                TransactionStatusEnum.valueOf(status),
                amount,
                TransType.TOP_UP,
                "",
                UUID.randomUUID().toString(),
                new Timestamp(System.currentTimeMillis() - 100_000L),
                new Timestamp(System.currentTimeMillis()));
    }

    private Map<String, TransferInfo> initStepTransaction(String transactionId) {
        Map<String, TransferInfo> transferInfo = new HashMap<>();
        Timestamp initTime = new Timestamp(System.currentTimeMillis());
        String userId = UUID.randomUUID().toString();
        String first = Snowflake.generateID();
        String second = Snowflake.generateID();
        transferInfo.put(first,
                TransferInfo.builder()
                        .step(1)
                        .id(first)
                        .transId(transactionId)
                        .amount(amount)
                        .status(TransactionInfoStatusEnum.PROCESSING)
                        .sourceType(ObjectTransactionEnum.BANK_ACCOUNT)
                        .sourceTransferId("BANK_VCB")
                        .userSourceId(userId)
                        .actionType(ActionTypeEnum.DEDUCT)
                        .stepId(firstSubTransId)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        transferInfo.put(second,
                TransferInfo.builder()
                        .step(2)
                        .id(second)
                        .transId(transactionId)
                        .amount(amount)
                        .status(TransactionInfoStatusEnum.INITIAL)
                        .sourceType(ObjectTransactionEnum.WALLET)
                        .userSourceId("ZLP_WALLET")
                        .sourceTransferId("0918340208")
                        .actionType(ActionTypeEnum.ADD)
                        .stepId(null)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        return transferInfo;
    }

    private Map<String, TransferInfo> initStepTransactionFinalStep(String transactionId) {
        Map<String, TransferInfo> transferInfo = new HashMap<>();
        Timestamp initTime = new Timestamp(System.currentTimeMillis());
        String userId = UUID.randomUUID().toString();
        String first = Snowflake.generateID();
        String second = Snowflake.generateID();
        transferInfo.put(first,
                TransferInfo.builder()
                        .step(1)
                        .id(first)
                        .transId(transactionId)
                        .amount(amount)
                        .status(TransactionInfoStatusEnum.COMPLETED)
                        .sourceType(ObjectTransactionEnum.BANK_ACCOUNT)
                        .sourceTransferId("BANK_VCB")
                        .userSourceId(userId)
                        .actionType(ActionTypeEnum.DEDUCT)
                        .stepId(firstSubTransId)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        transferInfo.put(second,
                TransferInfo.builder()
                        .step(2)
                        .id(second)
                        .transId(transactionId)
                        .amount(amount)
                        .status(TransactionInfoStatusEnum.PROCESSING)
                        .sourceType(ObjectTransactionEnum.WALLET)
                        .userSourceId("ZLP_WALLET")
                        .sourceTransferId("0918340208")
                        .actionType(ActionTypeEnum.ADD)
                        .stepId(secondSubTransId)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        return transferInfo;
    }

    private Map<String, TransferInfo> initStepTransactionRevertingCase(String transactionId) {
        Map<String, TransferInfo> transferInfo = new HashMap<>();
        Timestamp initTime = new Timestamp(System.currentTimeMillis());
        String userId = UUID.randomUUID().toString();
        String first = Snowflake.generateID();
        String second = Snowflake.generateID();
        transferInfo.put(first,
                TransferInfo.builder()
                        .step(1)
                        .id(first)
                        .transId(transactionId)
                        .amount(amount)
                        .status(TransactionInfoStatusEnum.REVERTING)
                        .sourceType(ObjectTransactionEnum.BANK_ACCOUNT)
                        .sourceTransferId("BANK_VCB")
                        .userSourceId(userId)
                        .actionType(ActionTypeEnum.DEDUCT)
                        .stepId(firstSubTransId)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        transferInfo.put(second,
                TransferInfo.builder()
                        .step(2)
                        .id(second)
                        .transId(transactionId)
                        .amount(amount)
                        .status(TransactionInfoStatusEnum.FAILED)
                        .sourceType(ObjectTransactionEnum.WALLET)
                        .userSourceId("ZLP_WALLET")
                        .sourceTransferId("0918340208")
                        .actionType(ActionTypeEnum.ADD)
                        .stepId(secondSubTransId)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        return transferInfo;
    }

}
