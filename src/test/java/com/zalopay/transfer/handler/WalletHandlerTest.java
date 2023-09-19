package com.zalopay.transfer.handler;

import com.zalopay.transfer.constants.enums.ActionTypeEnum;
import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.constants.enums.TransactionInfoStatusEnum;
import com.zalopay.transfer.data.WalletTransferInfoResponse;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.external.ZaloWalletExternalService;
import com.zalopay.transfer.listener.event.RollBackEvent;
import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.utils.Snowflake;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
public class WalletHandlerTest {
    @Mock
    private ZaloWalletExternalService zaloWalletExternalService;
    @Mock
    private TransferInfoRepository transferInfoRepo;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    private static Boolean lossConnectWallet;
    private static Boolean expectCatchException;
    private WalletHandler walletHandler;
    private final String transactionId = Snowflake.generateID();
    private final String userId = UUID.randomUUID().toString();
    private final Long amount = 100_000L;
    private TransferInfo transferInfo;

    @BeforeEach
    public void initEach() {
        Mockito.doAnswer(invocationOnMock -> {
            if (!lossConnectWallet) {
                return WalletTransferInfoResponse.builder()
                        .status("PROCESSING")
                        .subTransId(UUID.randomUUID().toString())
                        .build();
            } else {
                return WalletTransferInfoResponse.builder()
                        .status("FAILED")
                        .build();
            }
        }).when(zaloWalletExternalService).addMoneyWallet(Mockito.any());
        Mockito.doAnswer(invocationOnMock -> {
            if (!lossConnectWallet) {
                return WalletTransferInfoResponse.builder()
                        .status("PROCESSING")
                        .subTransId(UUID.randomUUID().toString())
                        .build();
            } else {
                return WalletTransferInfoResponse.builder()
                        .status("FAILED")
                        .build();
            }
        }).when(zaloWalletExternalService).deductMoneyWallet(Mockito.any());
        Mockito.doAnswer(invocationOnMock -> {
            if (!lossConnectWallet) {
                return WalletTransferInfoResponse.builder()
                        .status("PROCESSING")
                        .subTransId(UUID.randomUUID().toString())
                        .build();
            } else {
                return WalletTransferInfoResponse.builder()
                        .status("FAILED")
                        .build();
            }
        }).when(zaloWalletExternalService).revertTransaction(Mockito.any());

        Mockito.doAnswer(invocationOnMock -> {
            transferInfo = invocationOnMock.getArgument(0);
            return invocationOnMock.getArgument(0);
        }).when(transferInfoRepo).save(Mockito.any());

        Mockito.doAnswer(invocationOnMock -> {
            if (!expectCatchException) return null;
            throw new RuntimeException();
        }).when(applicationEventPublisher).publishEvent(Mockito.any());
        walletHandler = new WalletHandler(zaloWalletExternalService, transferInfoRepo, applicationEventPublisher);
    }

    @Test
    public void TestTopUpTransferExpectExceptionShouldBeOk() {
        lossConnectWallet = true;
        expectCatchException = true;
        transferInfo = initTransferInfoWithStatusActionSubTransId(TransactionInfoStatusEnum.INITIAL, ActionTypeEnum.ADD, null);
        walletHandler.handleTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Mockito.verify(applicationEventPublisher, Mockito.times(1)).publishEvent(Mockito.any(RollBackEvent.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.FAILED, transferInfo.getStatus());
        Assertions.assertNull(transferInfo.getStepId());
    }

    @Test
    public void TestTopUpTransferShouldBeOk() {
        lossConnectWallet = false;
        expectCatchException = false;
        transferInfo = initTransferInfoWithStatusActionSubTransId(TransactionInfoStatusEnum.INITIAL, ActionTypeEnum.ADD, null);
        walletHandler.handleTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any(RollBackEvent.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.PROCESSING, transferInfo.getStatus());
        Assertions.assertNotNull(transferInfo.getStepId());
    }

    @Test
    public void TestTopUpTransferLossConnectWalletShouldBeOk() {
        lossConnectWallet = true;
        expectCatchException = false;
        transferInfo = initTransferInfoWithStatusActionSubTransId(TransactionInfoStatusEnum.INITIAL, ActionTypeEnum.ADD, null);
        walletHandler.handleTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Mockito.verify(applicationEventPublisher, Mockito.times(1)).publishEvent(Mockito.any(RollBackEvent.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.FAILED, transferInfo.getStatus());
        Assertions.assertNull(transferInfo.getStepId());
    }

    @Test
    public void TestWithdrawTransferShouldBeOk() {
        lossConnectWallet = false;
        expectCatchException = false;
        transferInfo = initTransferInfoWithStatusActionSubTransId(TransactionInfoStatusEnum.INITIAL, ActionTypeEnum.DEDUCT, null);
        walletHandler.handleTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any(RollBackEvent.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.PROCESSING, transferInfo.getStatus());
        Assertions.assertNotNull(transferInfo.getStepId());
    }

    @Test
    public void TestWithdrawTransferLossConnectWalletShouldBeOk() {
        lossConnectWallet = true;
        expectCatchException = false;
        transferInfo = initTransferInfoWithStatusActionSubTransId(TransactionInfoStatusEnum.INITIAL, ActionTypeEnum.DEDUCT, null);
        walletHandler.handleTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Mockito.verify(applicationEventPublisher, Mockito.times(1)).publishEvent(Mockito.any(RollBackEvent.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.FAILED, transferInfo.getStatus());
        Assertions.assertNull(transferInfo.getStepId());
    }

    @Test
    public void TestRevertTransferShouldBeOk() {
        lossConnectWallet = false;
        expectCatchException = false;
        transferInfo = initTransferInfoWithStatusActionSubTransId(
                TransactionInfoStatusEnum.COMPLETED, ActionTypeEnum.ADD, UUID.randomUUID().toString());
        walletHandler.revertTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.REVERTING, transferInfo.getStatus());
    }

    @Test
    public void TestRevertTransferLossConnectWalletShouldBeOk() {
        lossConnectWallet = true;
        expectCatchException = false;
        transferInfo = initTransferInfoWithStatusActionSubTransId(
                TransactionInfoStatusEnum.COMPLETED, ActionTypeEnum.ADD, UUID.randomUUID().toString());
        walletHandler.revertTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.REVERT_FAILED, transferInfo.getStatus());
    }


    private TransferInfo initTransferInfoWithStatusActionSubTransId(
            TransactionInfoStatusEnum status, ActionTypeEnum actionTypeEnum, String subTransId) {
        return TransferInfo.builder()
                .step(1)
                .id(Snowflake.generateID())
                .transId(transactionId)
                .amount(amount)
                .status(status)
                .sourceType(ObjectTransactionEnum.WALLET)
                .sourceTransferId("ZLP_WALLET")
                .userSourceId("0918340208")
                .actionType(actionTypeEnum)
                .stepId(subTransId)
                .createdTime(new Timestamp(System.currentTimeMillis()))
                .updatedTime(new Timestamp(System.currentTimeMillis()))
                .build();
    }
}