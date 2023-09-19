package com.zalopay.transfer.handler;

import com.zalopay.transfer.constants.enums.ActionTypeEnum;
import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.constants.enums.TransactionInfoStatusEnum;
import com.zalopay.transfer.data.BankTransferInfoResponse;
import com.zalopay.transfer.entity.BankConnect;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.external.BankExternalService;
import com.zalopay.transfer.listener.event.RollBackEvent;
import com.zalopay.transfer.repository.BankConnectRepository;
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
import java.util.Optional;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
public class BankHandlerTest {
    @Mock
    private BankExternalService bankExternalService;
    @Mock
    private TransferInfoRepository transferInfoRepo;
    @Mock
    private BankConnectRepository bankConnectRepo;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    private static Boolean lossConnectBank;
    private static Boolean lossConnectDB;
    private BankHandler bankHandler;
    private final String transactionId = Snowflake.generateID();
    private final String userId = UUID.randomUUID().toString();
    private final Long amount = 100_000L;
    private BankConnect bankConnect;
    private final String numberAccount = "123456789";
    private TransferInfo transferInfo;

    @BeforeEach
    public void initEach() {
        BankConnect bankConnect1 = new BankConnect();
        bankConnect1.setBankCode("BANK_VBC");
        bankConnect1.setUserId(userId);
        bankConnect1.setNumberAccount(numberAccount);
        bankConnect = bankConnect1;

        Mockito.doAnswer(invocationOnMock -> {
            if (!lossConnectBank) {
                return BankTransferInfoResponse.builder()
                        .status("PROCESSING")
                        .subTransId(UUID.randomUUID().toString())
                        .build();
            } else {
                return BankTransferInfoResponse.builder()
                        .status("FAILED")
                        .build();
            }
        }).when(bankExternalService).addMoneyBank(Mockito.any());
        Mockito.doAnswer(invocationOnMock -> {
            if (!lossConnectBank) {
                return BankTransferInfoResponse.builder()
                        .status("PROCESSING")
                        .subTransId(UUID.randomUUID().toString())
                        .build();
            } else {
                return BankTransferInfoResponse.builder()
                        .status("FAILED")
                        .build();
            }
        }).when(bankExternalService).deductMoneyBank(Mockito.any());
        Mockito.doAnswer(invocationOnMock -> {
            if (!lossConnectBank) {
                return BankTransferInfoResponse.builder()
                        .status("PROCESSING")
                        .subTransId(UUID.randomUUID().toString())
                        .build();
            } else {
                return BankTransferInfoResponse.builder()
                        .status("FAILED")
                        .build();
            }
        }).when(bankExternalService).revertTransaction(Mockito.any());

        Mockito.doAnswer(invocationOnMock -> {
            if (!lossConnectDB) {
                return Optional.of(bankConnect);
            }
            throw new Exception("loss connect DB");
        }).when(bankConnectRepo).findByBankCodeAndUserId(Mockito.any(), Mockito.any());

        Mockito.doAnswer(invocationOnMock -> {
            transferInfo = invocationOnMock.getArgument(0);
            return invocationOnMock.getArgument(0);
        }).when(transferInfoRepo).save(Mockito.any());
        bankHandler = new BankHandler(bankExternalService, transferInfoRepo, bankConnectRepo, applicationEventPublisher);
    }

    @Test
    public void TestTopUpTransferLossConnectDBExpectExceptionShouldBeOk() {
        lossConnectBank = false;
        lossConnectDB = true;
        transferInfo = initTransferInfoWithStatusActionSubtransId(TransactionInfoStatusEnum.INITIAL, ActionTypeEnum.ADD, null);
        bankHandler.handleTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any(RollBackEvent.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.FAILED, transferInfo.getStatus());
        Assertions.assertNull(transferInfo.getStepId());
    }

    @Test
    public void TestTopUpTransferShouldBeOk() {
        lossConnectBank = false;
        lossConnectDB = false;
        transferInfo = initTransferInfoWithStatusActionSubtransId(TransactionInfoStatusEnum.INITIAL, ActionTypeEnum.ADD, null);
        bankHandler.handleTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any(RollBackEvent.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.PROCESSING, transferInfo.getStatus());
        Assertions.assertNotNull(transferInfo.getStepId());
    }

    @Test
    public void TestTopUpTransferLossConnectBankShouldBeOk() {
        lossConnectBank = true;
        lossConnectDB = false;
        transferInfo = initTransferInfoWithStatusActionSubtransId(TransactionInfoStatusEnum.INITIAL, ActionTypeEnum.ADD, null);
        bankHandler.handleTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Mockito.verify(applicationEventPublisher, Mockito.times(1)).publishEvent(Mockito.any(RollBackEvent.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.FAILED, transferInfo.getStatus());
        Assertions.assertNull(transferInfo.getStepId());
    }

    @Test
    public void TestWithdrawTransferShouldBeOk() {
        lossConnectBank = false;
        lossConnectDB = false;
        transferInfo = initTransferInfoWithStatusActionSubtransId(TransactionInfoStatusEnum.INITIAL, ActionTypeEnum.DEDUCT, null);
        bankHandler.handleTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Mockito.verify(applicationEventPublisher, Mockito.times(0)).publishEvent(Mockito.any(RollBackEvent.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.PROCESSING, transferInfo.getStatus());
        Assertions.assertNotNull(transferInfo.getStepId());
    }

    @Test
    public void TestWithdrawTransferLossConnectBankShouldBeOk() {
        lossConnectBank = true;
        lossConnectDB = false;
        transferInfo = initTransferInfoWithStatusActionSubtransId(TransactionInfoStatusEnum.INITIAL, ActionTypeEnum.DEDUCT, null);
        bankHandler.handleTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Mockito.verify(applicationEventPublisher, Mockito.times(1)).publishEvent(Mockito.any(RollBackEvent.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.FAILED, transferInfo.getStatus());
        Assertions.assertNull(transferInfo.getStepId());
    }

    @Test
    public void TestRevertTransferShouldBeOk() {
        lossConnectBank = false;
        lossConnectDB = false;
        transferInfo = initTransferInfoWithStatusActionSubtransId(
                TransactionInfoStatusEnum.COMPLETED, ActionTypeEnum.ADD, UUID.randomUUID().toString());
        bankHandler.revertTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.REVERTING, transferInfo.getStatus());
    }

    @Test
    public void TestRevertTransferLossConnectBankShouldBeOk() {
        lossConnectBank = true;
        lossConnectDB = false;
        transferInfo = initTransferInfoWithStatusActionSubtransId(
                TransactionInfoStatusEnum.COMPLETED, ActionTypeEnum.ADD, UUID.randomUUID().toString());
        bankHandler.revertTransaction(transferInfo);

        Mockito.verify(transferInfoRepo, Mockito.times(1)).save(Mockito.any(TransferInfo.class));
        Assertions.assertEquals(TransactionInfoStatusEnum.REVERT_FAILED, transferInfo.getStatus());
    }


    private TransferInfo initTransferInfoWithStatusActionSubtransId(
            TransactionInfoStatusEnum status, ActionTypeEnum actionTypeEnum, String subTransId) {
        return TransferInfo.builder()
                .step(1)
                .id(Snowflake.generateID())
                .transId(transactionId)
                .amount(amount)
                .status(status)
                .sourceType(ObjectTransactionEnum.BANK_ACCOUNT)
                .sourceTransferId("BANK_VCB")
                .userSourceId(userId)
                .actionType(actionTypeEnum)
                .stepId(subTransId)
                .createdTime(new Timestamp(System.currentTimeMillis()))
                .updatedTime(new Timestamp(System.currentTimeMillis()))
                .build();
    }
}