package com.zalopay.transfer.controller.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopUpRequest {
    @NotBlank(message = "requestId is mandatory")
    private String requestId;
    private Long requestedTime;

    @NotBlank(message = "userId is mandatory")
    private String userId;
    @NotBlank(message = "sourceTrans is mandatory")
    private String sourceTrans;
    @NotBlank(message = "destTrans is mandatory")
    private String destTrans;
    @NotBlank(message = "sourceId is mandatory")
    private String sourceId;
    @NotBlank(message = "destId is mandatory")
    private String destId;
    @NotBlank(message = "transType is mandatory")
    private String transType;
    private String promotion;
    @NotBlank(message = "amount is mandatory")
    private Long amount;
}
