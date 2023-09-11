package com.zalopay.transfer.usecase;

import com.google.gson.Gson;
import com.zalopay.transfer.constants.Constant;
import com.zalopay.transfer.constants.enums.*;
import com.zalopay.transfer.controller.request.CallbackRequest;
import com.zalopay.transfer.controller.response.CallbackResponse;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.entity.TransferTransaction;
import com.zalopay.transfer.handler.AbstractHandler;
import com.zalopay.transfer.listener.event.TransferEventData;
import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCallbackUseCase implements CallbackUseCase {

    private final TransferInfoRepository transferInfoRepo;
    private final TransferTransactionRepository transferTransactionRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationContext applicationContext;

    @Override
    @Transactional
    public ResultResponse<CallbackResponse> handle(CallbackRequest request) {
        Optional<TransferInfo> transferInfoOpt = transferInfoRepo.findBySubTransId(request.getTransId());
        if (transferInfoOpt.isPresent()) {
            transferInfoOpt.get().setUpdatedTime(new Timestamp(System.currentTimeMillis()));
            if (request.getStatus().equals(TransactionInfoStatusEnum.COMPLETED.name())) {
                transferInfoOpt.get().setStatus(TransactionInfoStatusEnum.COMPLETED);
                transferInfoRepo.save(transferInfoOpt.get());
                processNextStep(transferInfoOpt.get().getTransId());
            } else {
                transferInfoOpt.get().setStatus(TransactionInfoStatusEnum.FAILED);
                transferInfoRepo.save(transferInfoOpt.get());
                rollbackTransaction(transferInfoOpt.get().getTransId());
            }
        }

        return ResultResponse.<CallbackResponse>builder()
                .status(200)
                .result(CallbackResponse.builder()
                        .status("SUCCESSFULLY")
                        .build())
                .build();
    }

    @Transactional
    public void rollbackTransaction(String transId) {
        kafkaTemplate.send(
                Constant.Kafka.ROLL_BACK_STEPS_TOPIC,
                new Gson().toJson(new TransferEventData(transId, System.currentTimeMillis()))
        );
    }
    @Transactional
    public void processNextStep(String transId) {
        Optional<TransferInfo> transferInfoOpt = transferInfoRepo
                .findFirstByTransIdAndSubTransIdIsNullOrderByStepAsc(transId);
        transferInfoOpt.ifPresentOrElse(transferInfo -> {
            AbstractHandler classHandler = (AbstractHandler) applicationContext.getBean(transferInfo.getSourceType().name());
            classHandler.handleTransaction(transferInfo);
        }, () -> {
            Optional<TransferTransaction> transferTransactionOptional = transferTransactionRepo.findById(transId);
            transferTransactionOptional.ifPresent(transferTransaction -> {
                transferTransaction.setStatus(TransactionStatusEnum.COMPLETED);
                transferTransactionRepo.save(transferTransaction);
            });
        });
    }

}
