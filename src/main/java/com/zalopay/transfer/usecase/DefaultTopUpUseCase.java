package com.zalopay.transfer.usecase;

import com.zalopay.transfer.constants.enums.*;
import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.controller.response.TopUpResponse;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.entity.TransferTransaction;
import com.zalopay.transfer.listener.event.TransferEvent;
import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import com.zalopay.transfer.utils.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultTopUpUseCase implements TopUpUseCase {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransferTransactionRepository transactionRepo;
    private final TransferInfoRepository transferInfoRepo;

    @Override
    @Transactional
    public ResultResponse<TopUpResponse> handle(TopUpRequest request) {

        TransferTransaction transferTransaction = initTransaction(request);
        List<TransferInfo> transferInfoList = initStepTransfer(transferTransaction, request);
        try {
            transactionRepo.save(transferTransaction);
            transferInfoRepo.saveAll(transferInfoList);

            applicationEventPublisher.publishEvent(new TransferEvent(
                    this, transferTransaction.getTransId(), transferTransaction.getCreatedTime().getTime()));

        } catch (Exception e) {
            return ResultResponse.<TopUpResponse>builder()
                    .status(ErrorCode.INITIAL_TRANSACTION_FAILED.getCode())
                    .messages(Collections.singletonList(ErrorCode.INITIAL_TRANSACTION_FAILED.getMessage()))
                    .result(null)
                    .build();
        }

        return ResultResponse.<TopUpResponse>builder()
                .status(ErrorCode.SUCCESSFULLY.getCode())
                .messages(Collections.singletonList(ErrorCode.SUCCESSFULLY.getMessage()))
                .result(TopUpResponse.builder()
                        .status(TransactionStatusEnum.INITIAL.name())
                        .transId(transferTransaction.getTransId())
                        .build())
                .build();
    }

    private List<TransferInfo> initStepTransfer(TransferTransaction transferTransaction, TopUpRequest request) {

        List<TransferInfo> transferInfoList = new ArrayList<>();
        //TODO:: checking promotion, transType fields to add fee or todo something here
        // => Ignored, currently, we just add source handler and dest handler
        Timestamp initTime = new Timestamp(System.currentTimeMillis());
        transferInfoList.add(
                TransferInfo.builder()
                        .step(1)
                        .id(Snowflake.generateID())
                        .transId(transferTransaction.getTransId())
                        .userId(request.getUserId())
                        .amount(request.getAmount())
                        .status(TransactionInfoStatusEnum.INITIAL)
                        .sourceType(ObjectTransactionEnum.valueOf(request.getSourceType()))
                        .sourceTransferId(request.getSourceId())
                        .userSourceId(request.getSourceSender())
                        .activityType(ActivityTypeEnum.DEDUCT)
                        .subTransId(null)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        transferInfoList.add(
                TransferInfo.builder()
                        .step(2)
                        .id(Snowflake.generateID())
                        .transId(transferTransaction.getTransId())
                        .userId(request.getUserId())
                        .amount(request.getAmount())
                        .status(TransactionInfoStatusEnum.INITIAL)
                        .sourceType(ObjectTransactionEnum.valueOf(request.getDestType()))
                        .userSourceId(request.getDestReceiver())
                        .sourceTransferId(request.getDestId())
                        .activityType(ActivityTypeEnum.ADD)
                        .subTransId(null)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        return transferInfoList;
    }

    private TransferTransaction initTransaction(TopUpRequest request) {
        TransferTransaction transferTransaction = new TransferTransaction();
        transferTransaction.setTransId(Snowflake.generateID());
        transferTransaction.setStatus(TransactionStatusEnum.INITIAL);
        transferTransaction.setAmount(request.getAmount());
        transferTransaction.setCreatedTime(new Timestamp(System.currentTimeMillis()));
        transferTransaction.setUpdatedTime(transferTransaction.getCreatedTime());
        transferTransaction.setTransType(TransType.TOP_UP);
        return transferTransaction;
    }
}
