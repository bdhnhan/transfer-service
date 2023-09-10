package com.zalopay.transfer.listener.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferEventData {
    private String transactionId;
    private Long createdTime;
}
