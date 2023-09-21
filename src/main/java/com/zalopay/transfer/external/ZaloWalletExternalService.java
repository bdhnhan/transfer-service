package com.zalopay.transfer.external;

import com.zalopay.transfer.data.RevertTransferInfo;
import com.zalopay.transfer.data.WalletTransferInfo;
import com.zalopay.transfer.data.WalletTransferInfoResponse;
import com.zalowallet.protobuf.ZalopayServiceGrpc;
import com.zalowallet.protobuf.Zalowallet;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ZaloWalletExternalService {
    private final ZalopayServiceGrpc.ZalopayServiceBlockingStub stub;

    public ZaloWalletExternalService(ManagedChannel zaloWalletChannel) {
        this.stub = ZalopayServiceGrpc.newBlockingStub(zaloWalletChannel);
    }

    public WalletTransferInfoResponse addMoneyWallet(WalletTransferInfo walletTransferInfo) {
        try {
            Zalowallet.AddMoneyWalletResponse response = stub.addMoneyWallet(
                    Zalowallet.AddMoneyWalletRequest.newBuilder()
                            .setPhoneNumber(walletTransferInfo.getPhoneNumber())
                            .setAmount(walletTransferInfo.getAmount())
                            .setKeySource(walletTransferInfo.getStepId())
                            .build()
            );

            return WalletTransferInfoResponse.builder()
                    .subTransId(response.getResult().getTransId())
                    .status(response.getResult().getStatus())
                    .build();

        } catch (Exception e) {
            log.error("ERROR : Transfer Wallet ERROR :: {}", e.getMessage());
            return WalletTransferInfoResponse.builder()
                    .status("FAILED")
                    .build();
        }
    }

    public WalletTransferInfoResponse deductMoneyWallet(WalletTransferInfo walletTransferInfo) {
        try {
            Zalowallet.DeductMoneyWalletResponse response = stub.deductMoneyWallet(
                    Zalowallet.DeductMoneyWalletRequest.newBuilder()
                            .setAmount(walletTransferInfo.getAmount())
                            .setPhoneNumber(walletTransferInfo.getPhoneNumber())
                            .setKeySource(walletTransferInfo.getStepId())
                            .build()
            );

            return WalletTransferInfoResponse.builder()
                    .subTransId(response.getResult().getTransId())
                    .status(response.getResult().getStatus())
                    .build();

        } catch (Exception e) {
            log.error("ERROR : Transfer Wallet ERROR :: {}", e.getMessage());
            return WalletTransferInfoResponse.builder()
                    .status("FAILED")
                    .build();
        }
    }

    public WalletTransferInfoResponse revertTransaction(RevertTransferInfo bankTransferInfo) {
        try {
            Zalowallet.RevertTransferWalletResponse response = stub.revertTransferWallet(
                    Zalowallet.RevertTransferWalletRequest.newBuilder()
                            .setTransId(bankTransferInfo.getSubTransId())
                            .build()
            );

            return WalletTransferInfoResponse.builder()
                    .subTransId(response.getResult().getTransId())
                    .status(response.getResult().getStatus())
                    .build();

        } catch (Exception e) {
            log.error("ERROR : Revert Wallet ERROR :: {}", e.getMessage());
            return WalletTransferInfoResponse.builder()
                    .status("FAILED")
                    .build();
        }
    }
}
