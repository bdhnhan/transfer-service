package com.zalopay.transfer.listener;

import com.google.gson.Gson;
import com.zalopay.transfer.constants.enums.*;
import com.zalopay.transfer.entity.Transaction;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.handler.AbstractHandler;
import com.zalopay.transfer.handler.BankHandler;
import com.zalopay.transfer.handler.WalletHandler;
import com.zalopay.transfer.listener.event.TransferEventData;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(SpringExtension.class)
class TransferRequestedConsumerTest {
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private TransferTransactionRepository transferTransactionRepo;
    @Mock
    private TransferInfoRepository transferInfoRepo;
    @Mock
    private AbstractHandler abstractHandler;
    private static RedissonClient redissonClient;
    private TransferRequestedConsumer transferRequestedConsumer;

    private Map<String, Transaction> transactionMap = new HashMap<>();
    private Map<String, TransferInfo> transferInfoMap = new HashMap<>();
    private final String transactionId = Snowflake.generateID();
    private final String firstSubTransId = Snowflake.generateID();
    private final String secondSubTransId = Snowflake.generateID();

    private final Long amount = 100_000L;

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
        transactionMap.clear();
        transferInfoMap.clear();
        Mockito.doAnswer(invocationOnMock -> Optional.of(
                        transactionMap.getOrDefault((String) invocationOnMock.getArgument(0), null)))
                .when(transferTransactionRepo).findById(Mockito.anyString());

        AtomicReference<Transaction> transactionAtomicReference = new AtomicReference<>();
        Mockito.doAnswer(invocationOnMock -> {
            transactionAtomicReference.set(invocationOnMock.getArgument(0));
            transactionMap.put(transactionAtomicReference.get().getId(), transactionAtomicReference.get());
            return transactionAtomicReference.get();
        }).when(transferTransactionRepo).save(Mockito.any());

        Mockito.doAnswer(invocationOnMock -> {
            return transferInfoMap.values().stream()
                    .filter(transferInfo -> transferInfo.getTransId().equals(invocationOnMock.getArgument(0)))
                    .filter(transferInfo -> transferInfo.getStep().equals(invocationOnMock.getArgument(1)))
                    .findFirst();
        }).when(transferInfoRepo).findByTransIdAndStep(Mockito.anyString(), Mockito.anyInt());

        Mockito.doAnswer(invocationOnMock -> {
            if (invocationOnMock.getArgument(0).equals(ObjectTransactionEnum.BANK_ACCOUNT.name())) {
                abstractHandler = Mockito.mock(BankHandler.class);
            } else if (invocationOnMock.getArgument(0).equals(ObjectTransactionEnum.WALLET.name())) {
                abstractHandler = Mockito.mock(WalletHandler.class);
            }
            return abstractHandler;
        }).when(applicationContext).getBean(Mockito.anyString());

        Mockito.doAnswer(invocationOnMock -> null).when(abstractHandler).revertTransaction(Mockito.any());

        transferRequestedConsumer = new TransferRequestedConsumer(
                applicationContext, transferTransactionRepo, transferInfoRepo, redissonClient);
    }

    @Test
    public void testHandleTransferTransactionShouldBeOk() {
        transactionMap.put(transactionId, initTransferTransaction(transactionId));
        transferInfoMap = initStepTransaction(transactionId);
        String messageTransfer = new Gson().toJson(new TransferEventData(transactionId, System.currentTimeMillis()));
        transferRequestedConsumer.handle(messageTransfer);

        Mockito.verify(transferTransactionRepo, Mockito.times(1)).save(Mockito.any(Transaction.class));
        Mockito.verify(transferTransactionRepo, Mockito.times(1)).findById(transactionId);
        Mockito.verify(transferInfoRepo, Mockito.times(1)).findByTransIdAndStep(transactionId, 1);
        Mockito.verify(applicationContext, Mockito.times(1)).getBean(Mockito.anyString());
        Mockito.verify(abstractHandler, Mockito.times(1)).handleTransaction(Mockito.any(TransferInfo.class));

        Assertions.assertEquals(TransactionStatusEnum.PROCESSING, transactionMap.get(transactionId).getStatus());
        Assertions.assertEquals(TransactionStatusEnum.PROCESSING.name(), redissonClient.getMapCache("transId").get(transactionId));
        Assertions.assertInstanceOf(BankHandler.class, abstractHandler);
    }

    private Transaction initTransferTransaction(String transactionId) {
        return new Transaction(
                transactionId,
                TransactionStatusEnum.INITIAL,
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
                        .status(TransactionInfoStatusEnum.INITIAL)
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
                        .stepId(secondSubTransId)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        return transferInfo;
    }
}