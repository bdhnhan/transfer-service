package com.zalopay.transfer.usecase;

import com.zalopay.transfer.constants.enums.*;
import com.zalopay.transfer.controller.request.TransferUserRequest;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.controller.response.TransferUserResponse;
import com.zalopay.transfer.entity.Transaction;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.listener.event.TransferEvent;
import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class DefaultTransferUserUseCaseTest {

    private static final Long moneyTransfer = 100_000L;
    private static final String userId = UUID.randomUUID().toString();
    private static final String destId = "ZLP_WALLET";
    private static final String sourceId = "ZLP_WALLET";
    private static final String sourceSender = "0918340208";
    private static final String destReceiver = "0364634208";

    @Mock
    private TransferTransactionRepository transferTransactionRepository;
    @Mock
    private TransferInfoRepository transferInfoRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private DefaultTransferUserUseCase transferUserUseCase;

    private Map<String, Transaction> transactionMap = new HashMap<>();
    private Map<String, TransferInfo> infoMap = new HashMap<>();
    private Map<String, TransferEvent> eventMap = new HashMap<>();
    @BeforeEach
    public void initMock() {
        AtomicReference<Transaction> transactionAtomicReference = new AtomicReference<>();
        Mockito.doAnswer((Answer<Transaction>) invocation -> {
            transactionAtomicReference.set(invocation.getArgument(0));
            transactionMap.put(transactionAtomicReference.get().getId(), transactionAtomicReference.get());
            return transactionAtomicReference.get();
        }).when(transferTransactionRepository).save(Mockito.any());

        AtomicReference<List<TransferInfo>> infoAtomicReference = new AtomicReference<>();
        Mockito.doAnswer((Answer<List<TransferInfo>>) invocation -> {
            infoAtomicReference.set(invocation.getArgument(0));
            if (infoAtomicReference.get().get(0).getSourceTransferId() != null) {
                infoMap.putAll(infoAtomicReference.get().stream().collect(Collectors.toMap(TransferInfo::getId, transferInfo -> transferInfo)));
                return infoAtomicReference.get();
            }
            throw new Exception("This is an exception");
        }).when(transferInfoRepository).saveAll(Mockito.any());

        AtomicReference<TransferEvent> eventAtomicReference = new AtomicReference<>();
        Mockito.doAnswer((Answer<TransferEvent>) invocation -> {
            eventAtomicReference.set(invocation.getArgument(0));
            eventMap.put(eventAtomicReference.get().getTransactionId(), eventAtomicReference.get());
            return null;
        }).when(applicationEventPublisher).publishEvent(Mockito.any());
    }

    @Test
    public void testRequestTransferMoneyBetweenUsersShouldBeOk() {
        TransferUserRequest request = TransferUserRequest.builder()
                .requestId("")
                .requestedTime(System.currentTimeMillis())
                .amount(moneyTransfer)
                .userId(userId)
                .sourceType(ObjectTransactionEnum.WALLET.name())
                .sourceId(sourceId)
                .sourceSender(sourceSender)
                .destType(ObjectTransactionEnum.WALLET.name())
                .destId(destId)
                .destReceiver(destReceiver)
                .build();

        TransferUserResponse response = transferUserUseCase.handle(request).getResult();

        Mockito.verify(transferInfoRepository, Mockito.times(1)).saveAll(Mockito.any());
        Mockito.verify(transferTransactionRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(applicationEventPublisher, Mockito.times(1)).publishEvent(Mockito.any());

        assertEquals(1, transactionMap.size());
        assertEquals(2, infoMap.size());
        assertNotNull(response);

        Transaction transaction = new ArrayList<>(transactionMap.values()).get(0);
        assertEquals(eventMap.get(transaction.getId()).getCreatedTime(), transaction.getCreatedTime().getTime());
        assertEquals(request.getAmount(), transaction.getAmount());
        assertEquals(TransType.TRANSFER, transaction.getTransType());
        assertEquals(TransactionStatusEnum.INITIAL, transaction.getStatus());

        for (TransferInfo transferInfo : infoMap.values()) {
            assertNotNull(transferInfo);
            assertEquals(transferInfo.getTransId(), transaction.getId());
            assertEquals(request.getAmount(), transferInfo.getAmount());
            assertEquals(TransactionInfoStatusEnum.INITIAL, transferInfo.getStatus());
        }
    }

    @Test()
    public void testRequestTransferMoneyBetweenUsersWithExceptionSaveEntityOk() {

        TransferUserRequest request = TransferUserRequest.builder()
                .requestId("")
                .requestedTime(System.currentTimeMillis())
                .amount(moneyTransfer)
                .userId(userId)
                .sourceType(ObjectTransactionEnum.WALLET.name())
                .sourceId(null)
                .sourceSender(sourceSender)
                .destType(ObjectTransactionEnum.WALLET.name())
                .destId(null)
                .destReceiver(destReceiver)
                .build();

        ResultResponse<TransferUserResponse> response = transferUserUseCase.handle(request);

        Mockito.verify(transferInfoRepository, Mockito.times(1)).saveAll(Mockito.any());
        Mockito.verify(transferTransactionRepository, Mockito.times(1)).save(Mockito.any());
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any());

        assertEquals(ErrorCode.INITIAL_TRANSACTION_FAILED.getCode(), response.getStatus());
        assertEquals(Collections.singletonList(ErrorCode.INITIAL_TRANSACTION_FAILED.getMessage()), response.getMessages());
        assertNull(response.getResult());
    }
}
