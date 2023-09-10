package com.zalopay.transfer.handler;

import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.external.BankExternalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component(value = "BANK_ACCOUNT")
@RequiredArgsConstructor
public class BankHandler implements AbstractHandler {

    private final BankExternalService bankExternalService;

    @Override
    public void handleTransaction(TransferInfo transferInfo) {
        log.info("vo duoc bank roi");
    }

    public void topUpTrans() {
        bankExternalService.addMoneyBank();
    }

    public void withdrawTrans() {
    }

    public void revertTrans() {

    }

    public void getStatusSubTrans() {

    }
}
