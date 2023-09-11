package com.zalopay.transfer.handler;

import com.zalopay.transfer.constants.enums.ActivityTypeEnum;
import com.zalopay.transfer.constants.enums.TransactionInfoStatusEnum;
import com.zalopay.transfer.data.BankTransferInfo;
import com.zalopay.transfer.data.BankTransferInfoResponse;
import com.zalopay.transfer.data.WalletTransferInfo;
import com.zalopay.transfer.data.WalletTransferInfoResponse;
import com.zalopay.transfer.entity.BankConnect;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.external.ZaloWalletExternalService;
import com.zalopay.transfer.repository.TransferInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component(value = "WALLET")
@RequiredArgsConstructor
public class WalletHandler implements AbstractHandler {

    private final ZaloWalletExternalService zaloWalletExternalService;
    private final TransferInfoRepository transferInfoRepo;

    @Override
    @Transactional
    public void handleTransaction(TransferInfo transferInfo) {
        try {
            WalletTransferInfo walletTransferInfo = WalletTransferInfo.builder()
                    .phoneNumber(transferInfo.getUserSourceId())
                    .transId(transferInfo.getTransId())
                    .amount(transferInfo.getAmount())
                    .build();
            if (ActivityTypeEnum.ADD.equals(transferInfo.getActivityType())) {
                topUpTrans(transferInfo, walletTransferInfo);
            } else {
                withdrawTrans(transferInfo, walletTransferInfo);
            }
        } catch (Exception e) {
            transferInfo.setStatus(TransactionInfoStatusEnum.FAILED);
        }
        transferInfoRepo.save(transferInfo);
    }

    @Transactional
    public void topUpTrans(TransferInfo transferInfo, WalletTransferInfo walletTransferInfo) {
        WalletTransferInfoResponse walletTransferInfoResponse = zaloWalletExternalService.addMoneyWallet(walletTransferInfo);
        if (walletTransferInfoResponse.getStatus().equals("PROCESSING")) {
            transferInfo.setStatus(TransactionInfoStatusEnum.PROCESSING);
            transferInfo.setSubTransId(walletTransferInfoResponse.getSubTransId());
        } else {
            transferInfo.setStatus(TransactionInfoStatusEnum.FAILED);
        }
    }

    public void withdrawTrans(TransferInfo transferInfo, WalletTransferInfo walletTransferInfo) {
        WalletTransferInfoResponse walletTransferInfoResponse = zaloWalletExternalService.deductMoneyWallet(walletTransferInfo);
        if (walletTransferInfoResponse.getStatus().equals("PROCESSING")) {
            transferInfo.setStatus(TransactionInfoStatusEnum.PROCESSING);
            transferInfo.setSubTransId(walletTransferInfoResponse.getSubTransId());
        } else {
            transferInfo.setStatus(TransactionInfoStatusEnum.FAILED);
        }
    }

    public void revertTrans() {

    }

    public void getStatusSubTrans() {

    }
}
