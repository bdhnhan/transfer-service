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

    @NotBlank(message = "sourceType is mandatory")
    private String sourceType;
    @NotBlank(message = "sourceId is mandatory")
    private String sourceId;
    @NotBlank(message = "sourceSender is mandatory")
    private String sourceSender;

    @NotBlank(message = "destType is mandatory")
    private String destType;
    @NotBlank(message = "destId is mandatory")
    private String destId;
    @NotBlank(message = "destReceiver is mandatory")
    private String destReceiver;
    private String promotion;

    @NotBlank(message = "amount is mandatory")
    private Long amount;
}
