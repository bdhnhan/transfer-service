package com.zalopay.transfer.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankTransferInfoResponse {
    private String subTransId;
    private String status;
}
