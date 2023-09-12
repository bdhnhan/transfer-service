package com.zalopay.transfer.external;

import com.bank.protobuf.Bank;
import com.bank.protobuf.BankServiceGrpc;
import com.zalopay.transfer.data.BankTransferInfo;
import com.zalopay.transfer.data.BankTransferInfoResponse;
import com.zalopay.transfer.data.RevertTransferInfo;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class BankExternalService {
    private final BankServiceGrpc.BankServiceBlockingStub stub;

    public BankExternalService(ManagedChannel bankChannel) {
        this.stub = BankServiceGrpc.newBlockingStub(bankChannel);
    }

    public BankTransferInfoResponse addMoneyBank(BankTransferInfo bankTransferInfo) {
        try {
            Bank.AddMoneyBankResponse response = stub.addMoneyBank(
                    Bank.AddMoneyBankRequest.newBuilder()
                            .setAmount(bankTransferInfo.getAmount())
                            .setNumberAcc(bankTransferInfo.getNumberAccount())
                            .build()
            );

            return BankTransferInfoResponse.builder()
                    .subTransId(response.getResult().getTransId())
                    .status(response.getResult().getStatus())
                    .build();

        } catch (Exception e) {
            log.error("ERROR : Transfer addMoney Bank ERROR :: {}", e.getMessage());
            return BankTransferInfoResponse.builder()
                    .status("FAILED")
                    .build();
        }
    }

    public BankTransferInfoResponse deductMoneyBank(BankTransferInfo bankTransferInfo) {
        try {
            Bank.DeductMoneyBankResponse response = stub.deductMoneyBank(
                    Bank.DeductMoneyBankRequest.newBuilder()
                            .setAmount(bankTransferInfo.getAmount())
                            .setNumberAcc(bankTransferInfo.getNumberAccount())
                            .build()
            );

            return BankTransferInfoResponse.builder()
                    .subTransId(response.getResult().getTransId())
                    .status(response.getResult().getStatus())
                    .build();

        } catch (Exception e) {
            log.error("ERROR : Transfer deductMoney Bank ERROR :: {}", e.getMessage());
            return BankTransferInfoResponse.builder()
                    .status("FAILED")
                    .build();
        }
    }

    public BankTransferInfoResponse revertTransaction(RevertTransferInfo bankTransferInfo) {
        try {
            Bank.RevertTransferBankResponse response = stub.revertTransferBank(
                    Bank.RevertTransferBankRequest.newBuilder()
                            .setTransId(bankTransferInfo.getSubTransId())
                            .build()
            );

            return BankTransferInfoResponse.builder()
                    .subTransId(response.getResult().getTransId())
                    .status(response.getResult().getStatus())
                    .build();

        } catch (Exception e) {
            log.error("ERROR : Revert Bank ERROR :: {}", e.getMessage());
            return BankTransferInfoResponse.builder()
                    .status("FAILED")
                    .build();
        }
    }
}
