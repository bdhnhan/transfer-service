package com.zalopay.transfer.controller;

import br.com.fluentvalidator.context.Error;
import br.com.fluentvalidator.context.ValidationResult;
import com.zalopay.transfer.controller.request.CallbackRequest;
import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.controller.request.TransferUserRequest;
import com.zalopay.transfer.controller.request.WithdrawRequest;
import com.zalopay.transfer.controller.response.*;
import com.zalopay.transfer.constants.enums.ErrorCode;
import com.zalopay.transfer.controller.validator.TopUpValidator;
import com.zalopay.transfer.controller.validator.TransferUserValidator;
import com.zalopay.transfer.controller.validator.WithdrawValidator;
import com.zalopay.transfer.usecase.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/transfer")
@Slf4j
public class TransferController {
    private final TopUpUseCase topUpUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final CallbackUseCase callbackUseCase;
    private final TransferUserUseCase transferUserUseCase;
    private final GetStatusTransactionUseCase getStatusTransactionUseCase;

    public TransferController(TopUpUseCase topUpUseCase, WithdrawUseCase withdrawUseCase,
                              CallbackUseCase callbackUseCase, TransferUserUseCase transferUserUseCase,
                              GetStatusTransactionUseCase getStatusTransactionUseCase) {
        this.topUpUseCase = topUpUseCase;
        this.withdrawUseCase = withdrawUseCase;
        this.callbackUseCase = callbackUseCase;
        this.transferUserUseCase = transferUserUseCase;
        this.getStatusTransactionUseCase = getStatusTransactionUseCase;
    }

    @PostMapping("/callback")
    public ResultResponse<CallbackResponse> callBackTransaction(@RequestBody CallbackRequest request) {
        log.info("Receive callback from [{}] - subTransId [{}]", request.getSourceCallBack(), request.getTransId());
        ResultResponse<CallbackResponse> callbackResponseResultResponse = callbackUseCase.handle(request);
        return callbackResponseResultResponse;
    }

    @GetMapping("/status/{transId}")
    public ResultResponse<String> getStatus(@PathVariable String transId) {
        log.info("Receive get status from TransId [{}]", transId);
        ResultResponse<String> getStatusResponse = getStatusTransactionUseCase.handle(transId);
        return getStatusResponse;
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

    @PostMapping("/withdraw")
    public ResultResponse<WithdrawResponse> withdraw(@RequestBody WithdrawRequest request) {

        //TODO:: needing check duplicate request base on requestId from request

        //validate
        ValidationResult validationResult = new WithdrawValidator().validate(request);
        if (!validationResult.isValid()) {
            return ResultResponse.<WithdrawResponse>builder()
                    .status(ErrorCode.MANDATORY_FIELD.getCode())
                    .messages(
                            validationResult.getErrors().stream().map(Error::getMessage)
                                    .collect(Collectors.toList()))
                    .result(null)
                    .build();
        }
        ResultResponse<WithdrawResponse> response = withdrawUseCase.handle(request);
        return response;
    }

    @PostMapping("/transferUser")
    public ResultResponse<TransferUserResponse> transferUser(@RequestBody TransferUserRequest request) {

        //TODO:: needing check duplicate request base on requestId from request

        //validate
        ValidationResult validationResult = new TransferUserValidator().validate(request);
        if (!validationResult.isValid()) {
            return ResultResponse.<TransferUserResponse>builder()
                    .status(ErrorCode.MANDATORY_FIELD.getCode())
                    .messages(
                            validationResult.getErrors().stream().map(Error::getMessage)
                                    .collect(Collectors.toList()))
                    .result(null)
                    .build();
        }
        ResultResponse<TransferUserResponse> response = transferUserUseCase.handle(request);
        return response;
    }
}
