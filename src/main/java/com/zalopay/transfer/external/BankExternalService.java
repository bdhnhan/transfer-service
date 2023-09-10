package com.zalopay.transfer.external;

import com.bank.protobuf.Bank;
import com.bank.protobuf.BankServiceGrpc;
import com.zalowallet.protobuf.ZalopayServiceGrpc;
import com.zalowallet.protobuf.Zalowallet;
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

    public void addMoneyBank() {
    }
}
