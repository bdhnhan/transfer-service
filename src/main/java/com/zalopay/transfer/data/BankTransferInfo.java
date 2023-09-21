package com.zalopay.transfer.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankTransferInfo {
    private String stepId;
    private String numberAccount;
    private String userId;
    private Long amount;
}
