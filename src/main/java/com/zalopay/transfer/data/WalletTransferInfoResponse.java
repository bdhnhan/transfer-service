package com.zalopay.transfer.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WalletTransferInfoResponse {
    private String subTransId;
    private String status;
}
