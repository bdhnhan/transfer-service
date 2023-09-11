package com.zalopay.transfer.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CallbackRequest {
    private String sourceCallBack;
    private String transId;
    private String status;
}
