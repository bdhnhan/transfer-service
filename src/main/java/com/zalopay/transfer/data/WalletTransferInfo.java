package com.zalopay.transfer.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletTransferInfo {
    private String stepId;
    private String phoneNumber;
    private String userId;
    private Long amount;
}
