package com.zalopay.transfer.handler;

import com.zalopay.transfer.constants.enums.ActionTypeEnum;
import com.zalopay.transfer.constants.enums.TransactionInfoStatusEnum;
import com.zalopay.transfer.data.RevertTransferInfo;
import com.zalopay.transfer.data.WalletTransferInfo;
import com.zalopay.transfer.data.WalletTransferInfoResponse;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.external.ZaloWalletExternalService;
import com.zalopay.transfer.listener.event.RollBackEvent;
import com.zalopay.transfer.repository.TransferInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component(value = "WALLET")
@RequiredArgsConstructor
public class WalletHandler implements AbstractHandler {

    private final ZaloWalletExternalService zaloWalletExternalService;
    private final TransferInfoRepository transferInfoRepo;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void handleTransaction(TransferInfo transferInfo) {
        try {
            WalletTransferInfo walletTransferInfo = WalletTransferInfo.builder()
                    .phoneNumber(transferInfo.getUserSourceId())
                    .stepId(transferInfo.getId())
                    .amount(transferInfo.getAmount())
                    .build();
            if (ActionTypeEnum.ADD.equals(transferInfo.getActionType())) {
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
            transferInfo.setStepId(walletTransferInfoResponse.getSubTransId());
        } else {
            transferInfo.setStatus(TransactionInfoStatusEnum.FAILED);
            applicationEventPublisher.publishEvent(new RollBackEvent(this, transferInfo.getTransId(), System.currentTimeMillis()));
        }
    }

    @Transactional
    public void withdrawTrans(TransferInfo transferInfo, WalletTransferInfo walletTransferInfo) {
        WalletTransferInfoResponse walletTransferInfoResponse = zaloWalletExternalService.deductMoneyWallet(walletTransferInfo);
        if (walletTransferInfoResponse.getStatus().equals("PROCESSING")) {
            transferInfo.setStatus(TransactionInfoStatusEnum.PROCESSING);
            transferInfo.setStepId(walletTransferInfoResponse.getSubTransId());
        } else {
            transferInfo.setStatus(TransactionInfoStatusEnum.FAILED);
            applicationEventPublisher.publishEvent(new RollBackEvent(this, transferInfo.getTransId(), System.currentTimeMillis()));
        }
    }

    @Override
    public void revertTransaction(TransferInfo transferInfo) {
        RevertTransferInfo revertTransferInfo = RevertTransferInfo.builder().subTransId(transferInfo.getStepId()).build();
        WalletTransferInfoResponse walletTransferInfoResponse = zaloWalletExternalService.revertTransaction(revertTransferInfo);
        if (walletTransferInfoResponse.getStatus().equals("PROCESSING")) {
            transferInfo.setStatus(TransactionInfoStatusEnum.REVERTING);
        } else {
            transferInfo.setStatus(TransactionInfoStatusEnum.REVERT_FAILED);
        }
        transferInfoRepo.save(transferInfo);
    }
}
