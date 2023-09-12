package com.zalopay.transfer.data;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RevertTransferInfo {
    private String subTransId;
}
