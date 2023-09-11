package com.zalopay.transfer.handler;

import com.zalopay.transfer.constants.enums.ActivityTypeEnum;
import com.zalopay.transfer.constants.enums.TransactionInfoStatusEnum;
import com.zalopay.transfer.data.BankTransferInfo;
import com.zalopay.transfer.data.BankTransferInfoResponse;
import com.zalopay.transfer.entity.BankConnect;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.external.BankExternalService;
import com.zalopay.transfer.repository.BankConnectRepository;
import com.zalopay.transfer.repository.TransferInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component(value = "BANK_ACCOUNT")
@RequiredArgsConstructor
public class BankHandler implements AbstractHandler {

    private final BankExternalService bankExternalService;
    private final TransferInfoRepository transferInfoRepo;
    private final BankConnectRepository bankConnectRepo;

    @Override
    @Transactional
    public void handleTransaction(TransferInfo transferInfo) {
        try {
            Optional<BankConnect> bankConnectOptional = bankConnectRepo
                    .findByBankCodeAndUserId(transferInfo.getSourceTransferId(), transferInfo.getUserId());
            bankConnectOptional.ifPresent(bankConnect -> {
                BankTransferInfo bankTransferInfo = BankTransferInfo.builder()
                        .numberAccount(bankConnect.getNumberAccount())
                        .transId(transferInfo.getTransId())
                        .amount(transferInfo.getAmount())
                        .build();
                if (ActivityTypeEnum.ADD.equals(transferInfo.getActivityType())) {
                    topUpTrans(transferInfo, bankTransferInfo);
                } else {
                    withdrawTrans(transferInfo, bankTransferInfo);
                }
            });
        } catch (Exception e) {
            transferInfo.setStatus(TransactionInfoStatusEnum.FAILED);
        }
        transferInfoRepo.save(transferInfo);
    }

    @Transactional
    public void topUpTrans(TransferInfo transferInfo, BankTransferInfo bankTransferInfo) {
        BankTransferInfoResponse bankTransferInfoResponse = bankExternalService.addMoneyBank(bankTransferInfo);
        if (bankTransferInfoResponse.getStatus().equals("PROCESSING")) {
            transferInfo.setStatus(TransactionInfoStatusEnum.PROCESSING);
            transferInfo.setSubTransId(bankTransferInfoResponse.getSubTransId());
        } else {
            transferInfo.setStatus(TransactionInfoStatusEnum.FAILED);
        }
    }

    public void withdrawTrans(TransferInfo transferInfo, BankTransferInfo bankTransferInfo) {
        BankTransferInfoResponse bankTransferInfoResponse = bankExternalService.deductMoneyBank(bankTransferInfo);
        if (bankTransferInfoResponse.getStatus().equals("PROCESSING")) {
            transferInfo.setStatus(TransactionInfoStatusEnum.PROCESSING);
            transferInfo.setSubTransId(bankTransferInfoResponse.getSubTransId());
        } else {
            transferInfo.setStatus(TransactionInfoStatusEnum.FAILED);
        }
    }

    public void revertTrans() {

    }

    public void getStatusSubTrans() {

    }
}
