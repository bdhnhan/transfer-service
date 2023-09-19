package com.zalopay.transfer.usecase;

import com.zalopay.transfer.constants.enums.*;
import com.zalopay.transfer.controller.request.TransferUserRequest;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.controller.response.TransferUserResponse;
import com.zalopay.transfer.entity.Transaction;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.listener.event.TransferEvent;
import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import com.zalopay.transfer.utils.Snowflake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class DefaultTransferUserUseCase implements TransferUserUseCase {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final TransferTransactionRepository transactionRepo;
    private final TransferInfoRepository transferInfoRepo;

    @Override
    @Transactional
    public ResultResponse<TransferUserResponse> handle(TransferUserRequest request) {
        Transaction transaction = initTransaction(request);
        List<TransferInfo> transferInfoList = initStepTransfer(transaction, request);
        try {
            transactionRepo.save(transaction);
            transferInfoRepo.saveAll(transferInfoList);

            applicationEventPublisher.publishEvent(new TransferEvent(
                    this, transaction.getId(), transaction.getCreatedTime().getTime()));

        } catch (Exception e) {
            return ResultResponse.<TransferUserResponse>builder()
                    .status(ErrorCode.INITIAL_TRANSACTION_FAILED.getCode())
                    .messages(Collections.singletonList(ErrorCode.INITIAL_TRANSACTION_FAILED.getMessage()))
                    .result(null)
                    .build();
        }

        return ResultResponse.<TransferUserResponse>builder()
                .status(ErrorCode.SUCCESSFULLY.getCode())
                .messages(Collections.singletonList(ErrorCode.SUCCESSFULLY.getMessage()))
                .result(TransferUserResponse.builder()
                        .status(TransactionStatusEnum.INITIAL.name())
                        .transId(transaction.getId())
                        .build())
                .build();
    }

    private List<TransferInfo> initStepTransfer(Transaction transaction, TransferUserRequest request) {

        List<TransferInfo> transferInfoList = new ArrayList<>();
        //TODO:: checking promotion, transType fields to add fee or todo something here
        // => Ignored, currently, we just add source handler and dest handler
        Timestamp initTime = new Timestamp(System.currentTimeMillis());
        transferInfoList.add(
                TransferInfo.builder()
                        .step(1)
                        .id(Snowflake.generateID())
                        .transId(transaction.getId())
                        .amount(request.getAmount())
                        .status(TransactionInfoStatusEnum.INITIAL)
                        .sourceType(ObjectTransactionEnum.valueOf(request.getSourceType()))
                        .sourceTransferId(request.getSourceId())
                        .userSourceId(request.getSourceSender())
                        .actionType(ActionTypeEnum.DEDUCT)
                        .stepId(null)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        transferInfoList.add(
                TransferInfo.builder()
                        .step(2)
                        .id(Snowflake.generateID())
                        .transId(transaction.getId())
                        .amount(request.getAmount())
                        .status(TransactionInfoStatusEnum.INITIAL)
                        .sourceType(ObjectTransactionEnum.valueOf(request.getDestType()))
                        .userSourceId(request.getDestReceiver())
                        .sourceTransferId(request.getDestId())
                        .actionType(ActionTypeEnum.ADD)
                        .stepId(null)
                        .createdTime(initTime)
                        .updatedTime(initTime)
                        .build()
        );
        return transferInfoList;
    }

    private Transaction initTransaction(TransferUserRequest request) {
        Transaction transaction = new Transaction();
        transaction.setId(Snowflake.generateID());
        transaction.setStatus(TransactionStatusEnum.INITIAL);
        transaction.setAmount(request.getAmount());
        transaction.setCreatedTime(new Timestamp(System.currentTimeMillis()));
        transaction.setUpdatedTime(transaction.getCreatedTime());
        transaction.setTransType(TransType.TRANSFER);
        transaction.setUserId(request.getUserId());
        return transaction;
    }
}
