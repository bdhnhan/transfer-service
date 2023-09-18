package com.zalopay.transfer.listener;

import com.google.gson.Gson;
import com.zalopay.transfer.constants.enums.*;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.entity.TransferTransaction;
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
import java.util.stream.Collectors;

@ExtendWith(SpringExtension.class)
public class RollBackRequestedConsumerTest {

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private TransferTransactionRepository transferTransactionRepo;
    @Mock
    private TransferInfoRepository transferInfoRepo;
    @Mock
    private AbstractHandler abstractHandler;
    private static RedissonClient redissonClient;
    private RollBackRequestedConsumer rollBackRequestedConsumer;

    private Map<String, TransferTransaction> transactionMap = new HashMap<>();
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

        AtomicReference<TransferTransaction> transactionAtomicReference = new AtomicReference<>();
        Mockito.doAnswer(invocationOnMock -> {
            transactionAtomicReference.set(invocationOnMock.getArgument(0));
            transactionMap.put(transactionAtomicReference.get().getTransId(), transactionAtomicReference.get());
            return transactionAtomicReference.get();
        }).when(transferTransactionRepo).save(Mockito.any());

        Mockito.doAnswer(invocationOnMock -> transferInfoMap.values().stream()
                        .filter(transferInfo -> transferInfo.getTransId().equals(invocationOnMock.getArgument(0)))
                        .collect(Collectors.toList()))
                .when(transferInfoRepo).findAllByTransId(Mockito.anyString());

        Mockito.doAnswer(invocationOnMock -> {
            if (invocationOnMock.getArgument(0).equals(ObjectTransactionEnum.BANK_ACCOUNT.name())) {
                abstractHandler = Mockito.mock(BankHandler.class);
            } else if (invocationOnMock.getArgument(0).equals(ObjectTransactionEnum.WALLET.name())) {
                abstractHandler = Mockito.mock(WalletHandler.class);
            }
            return abstractHandler;
        }).when(applicationContext).getBean(Mockito.anyString());

        Mockito.doAnswer(invocationOnMock -> null).when(abstractHandler).revertTransaction(Mockito.any());
        rollBackRequestedConsumer = new RollBackRequestedConsumer(
                applicationContext, transferTransactionRepo, transferInfoRepo, redissonClient);
    }

    @Test
    public void testRollbackTransactionWithFinalStepFailedShouldBeOk() {
        transactionMap.put(transactionId, initTransferTransaction(transactionId, TransactionStatusEnum.PROCESSING.name()));
        transferInfoMap = initStepTransactionFinalStepFailed(transactionId);

        String messageRollBack = new Gson().toJson(new TransferEventData(transactionId, System.currentTimeMillis()));
        rollBackRequestedConsumer.handle(messageRollBack);

        Mockito.verify(transferTransactionRepo, Mockito.times(1)).save(Mockito.any(TransferTransaction.class));
        Assertions.assertEquals(redissonClient.getMapCache("transId").get(transactionId), TransactionStatusEnum.FAILED.name());
        Mockito.verify(transferInfoRepo, Mockito.times(1)).findAllByTransId(Mockito.anyString());
        Mockito.verify(abstractHandler, Mockito.times(1)).revertTransaction(Mockito.any(TransferInfo.class));
        Assertions.assertInstanceOf(BankHandler.class, abstractHandler);
    }

    private TransferTransaction initTransferTransaction(String transactionId, String status) {
        return new TransferTransaction(
                transactionId,
                TransactionStatusEnum.valueOf(status),
                amount,
                TransType.TOP_UP,
                "",
                new Timestamp(System.currentTimeMillis() - 100_000L),
                new Timestamp(System.currentTimeMillis()));
    }

    private Map<String, TransferInfo> initStepTransactionFinalStepFailed(String transactionId) {
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
                        .userId(userId)
                        .amount(amount)
                        .status(TransactionInfoStatusEnum.COMPLETED)
                        .sourceType(ObjectTransactionEnum.BANK_ACCOUNT)
                        .sourceTransferId("BANK_VCB")
                        .userSourceId(userId)
                        .activityType(ActivityTypeEnum.DEDUCT)
                        .subTransId(firstSubTransId)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        transferInfo.put(second,
                TransferInfo.builder()
                        .step(2)
                        .id(second)
                        .transId(transactionId)
                        .userId(userId)
                        .amount(amount)
                        .status(TransactionInfoStatusEnum.FAILED)
                        .sourceType(ObjectTransactionEnum.WALLET)
                        .userSourceId("ZLP_WALLET")
                        .sourceTransferId("0918340208")
                        .activityType(ActivityTypeEnum.ADD)
                        .subTransId(secondSubTransId)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        return transferInfo;
    }

}