package com.zalopay.transfer.external;

import com.zalowallet.protobuf.ZalopayServiceGrpc;
import com.zalowallet.protobuf.Zalowallet;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ZaloWalletExternalService {
    private final ZalopayServiceGrpc.ZalopayServiceBlockingStub stub;

    public ZaloWalletExternalService(ManagedChannel zaloWalletChannel) {
        this.stub = ZalopayServiceGrpc.newBlockingStub(zaloWalletChannel);
    }

    public void addMoneyWallet() {
    }
}
