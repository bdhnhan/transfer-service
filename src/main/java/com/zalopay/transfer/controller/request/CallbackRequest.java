package com.zalopay.transfer.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CallbackRequest {
    private String sourceCallBack;
    private String transId;
    private String status;
}
