package com.zalopay.transfer.controller;

import br.com.fluentvalidator.context.Error;
import br.com.fluentvalidator.context.ValidationResult;
import com.zalopay.transfer.controller.request.CallbackRequest;
import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.controller.response.TopUpResponse;
import com.zalopay.transfer.constants.enums.ErrorCode;
import com.zalopay.transfer.external.ZaloWalletExternalService;
import com.zalopay.transfer.controller.validator.TopUpValidator;
import com.zalopay.transfer.usecase.TopUpUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/transfer")
@Slf4j
public class TransferController {
    private final ZaloWalletExternalService zaloWalletExternalService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TopUpUseCase topUpUseCase;

    public TransferController(ZaloWalletExternalService zaloWalletExternalService,
                              KafkaTemplate<String, String> kafkaTemplate,
                              TopUpUseCase topUpUseCase) {
        this.zaloWalletExternalService = zaloWalletExternalService;
        this.kafkaTemplate = kafkaTemplate;
        this.topUpUseCase = topUpUseCase;
    }

    @PostMapping("/callback")
    public String callBackTransaction(@RequestBody CallbackRequest request) {
        log.info("Receive request :: {}", request);
        return "OK";
    }

    @PostMapping("/topUp")
    public ResultResponse<TopUpResponse> topUp(@RequestBody TopUpRequest request) {

        //TODO:: needing check duplicate request base on requestId from request

        //validate
        ValidationResult validationResult = new TopUpValidator().validate(request);
        if (!validationResult.isValid()) {
            return ResultResponse.<TopUpResponse>builder()
                    .status(ErrorCode.MANDATORY_FIELD.getCode())
                    .messages(
                            validationResult.getErrors().stream().map(Error::getMessage)
                                    .collect(Collectors.toList()))
                    .result(null)
                    .build();
        }
        ResultResponse<TopUpResponse> response = topUpUseCase.handle(request);
        return response;
    }
}
