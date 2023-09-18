package com.zalopay.transfer.usecase;

import com.zalopay.transfer.constants.enums.TransType;
import com.zalopay.transfer.constants.enums.TransactionStatusEnum;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.entity.TransferTransaction;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
public class DefaultGetStatusTransactionUseCaseTest {

    private static final String transId = Snowflake.generateID();
    private final Long amount = 100_000L;
    private static final TransactionStatusEnum transactionStatusEnum = TransactionStatusEnum.PROCESSING;

    @Mock
    private static TransferTransactionRepository transferTransactionRepository;

    private static RedissonClient redissonClient;
    private static GetStatusTransactionUseCase getStatusTransactionUseCase;

    @BeforeAll
    public static void initBefore() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://localhost:6379")
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5)
                .setConnectTimeout(3000);
        redissonClient = Redisson.create(config);
    }

    @BeforeEach
    public void init() {
        getStatusTransactionUseCase = new DefaultGetStatusTransactionUseCase(transferTransactionRepository, redissonClient);
        redissonClient.getMapCache("transId").clear();
        Mockito.doAnswer(invocation -> {
            if (invocation.getArgument(0).equals(transId)) {
                return Optional.of(initTransferTransaction(transId));
            } else {
                return Optional.empty();
            }
        }).when(transferTransactionRepository).findById(Mockito.anyString());
    }

    @Test
    public void testGetStatusTransactionIdFromRedisShouldBeOK() {
        redissonClient.getMapCache("transId").put(transId, transactionStatusEnum.name());
        ResultResponse<String> response = getStatusTransactionUseCase.handle(transId);
        Assertions.assertEquals(transactionStatusEnum.name(), response.getResult());
    }

    @Test
    public void testGetStatusTransactionIdFromDBShouldBeOK() {
        ResultResponse<String> response = getStatusTransactionUseCase.handle(transId);
        Assertions.assertEquals(transactionStatusEnum.name(), response.getResult());
    }

    @Test
    public void testGetStatusTransactionIdFromDBIsNullShouldBeOK() {
        ResultResponse<String> response = getStatusTransactionUseCase.handle("");
        Assertions.assertNull(response.getResult());
        Assertions.assertEquals(603, response.getStatus());
    }

    private TransferTransaction initTransferTransaction(String transactionId) {
        return new TransferTransaction(
                transactionId,
                transactionStatusEnum,
                amount,
                TransType.TOP_UP,
                "",
                new Timestamp(System.currentTimeMillis() - 100_000L),
                new Timestamp(System.currentTimeMillis()));
    }
}
