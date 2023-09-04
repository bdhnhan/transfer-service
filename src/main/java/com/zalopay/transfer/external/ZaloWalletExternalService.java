package com.zalopay.transfer.external;

import com.zalowallet.protobuf.ZalopayServiceGrpc;
import com.zalowallet.protobuf.Zalowallet;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
//@DependsOn({"zaloWalletChannel"})
@Slf4j
public class ZaloWalletExternalService {
    private final ZalopayServiceGrpc.ZalopayServiceBlockingStub stub;

    public ZaloWalletExternalService(ManagedChannel zaloWalletChannel) {
        this.stub = ZalopayServiceGrpc.newBlockingStub(zaloWalletChannel);
    }

    public void topUpWallet() {
        Zalowallet.TopUpWalletResponse response = stub.topUpWallet(
                Zalowallet.TopUpWalletRequest.newBuilder()
                        .setAmount(30000)
                        .setPhoneNumber("0918340208")
                        .setCreatedTime(System.currentTimeMillis())
                        .build()
        );
    }
}
