package com.zalopay.transfer.handler;

import com.zalopay.transfer.entity.TransferInfo;

public interface AbstractHandler {
    void handleTransaction(TransferInfo transferInfo);
}
