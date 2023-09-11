package com.zalopay.transfer.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankTransferInfo {
    private String transId;
    private String numberAccount;
    private String userId;
    private Long amount;
}
