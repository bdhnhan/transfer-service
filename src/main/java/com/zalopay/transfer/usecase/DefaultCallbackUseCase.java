package com.zalopay.transfer.usecase;

import com.zalopay.transfer.constants.enums.ErrorCode;
import com.zalopay.transfer.constants.enums.TransactionInfoStatusEnum;
import com.zalopay.transfer.constants.enums.TransactionStatusEnum;
import com.zalopay.transfer.controller.request.CallbackRequest;
import com.zalopay.transfer.controller.response.CallbackResponse;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.entity.Transaction;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.handler.AbstractHandler;
import com.zalopay.transfer.listener.event.RollBackEvent;
import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultCallbackUseCase implements CallbackUseCase {

    private final TransferInfoRepository transferInfoRepo;
    private final TransferTransactionRepository transferTransactionRepo;
    private final ApplicationContext applicationContext;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public ResultResponse<CallbackResponse> handle(CallbackRequest request) {
        Optional<TransferInfo> transferInfoOpt = transferInfoRepo.findByStepId(request.getTransId());
        if (transferInfoOpt.isPresent()) {
            transferInfoOpt.get().setUpdatedTime(new Timestamp(System.currentTimeMillis()));
            if (transferInfoOpt.get().getStatus().equals(TransactionInfoStatusEnum.REVERTING)) {
                if (request.getStatus().equals(TransactionInfoStatusEnum.COMPLETED.name())) {
                    transferInfoOpt.get().setStatus(TransactionInfoStatusEnum.ROLLBACK);
                    transferInfoRepo.save(transferInfoOpt.get());
                } else {
                    transferInfoOpt.get().setStatus(TransactionInfoStatusEnum.REVERT_FAILED);
                    transferInfoRepo.save(transferInfoOpt.get());
                }
            } else {
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
                    .status(ErrorCode.SUCCESSFULLY.getCode())
                    .result(CallbackResponse.builder()
                            .status(ErrorCode.SUCCESSFULLY.name())
                            .build())
                    .build();
        } else {
            return ResultResponse.<CallbackResponse>builder()
                    .status(ErrorCode.TRANS_ID_IS_NOT_FOUND.getCode())
                    .result(CallbackResponse.builder()
                            .status(ErrorCode.TRANS_ID_IS_NOT_FOUND.name())
                            .build())
                    .build();
        }
    }

    @Transactional
    public void rollbackTransaction(String transId) {
        applicationEventPublisher.publishEvent(new RollBackEvent(this, transId, System.currentTimeMillis()));
    }
    @Transactional
    public void processNextStep(String transId) {
        Optional<TransferInfo> transferInfoOpt = transferInfoRepo
                .findFirstByTransIdAndStepIdIsNullOrderByStepAsc(transId);
        transferInfoOpt.ifPresentOrElse(transferInfo -> {
            AbstractHandler classHandler = (AbstractHandler) applicationContext.getBean(transferInfo.getSourceType().name());
            classHandler.handleTransaction(transferInfo);
        }, () -> {
            Optional<Transaction> transferTransactionOptional = transferTransactionRepo.findById(transId);
            transferTransactionOptional.ifPresent(transferTransaction -> {
                transferTransaction.setStatus(TransactionStatusEnum.COMPLETED);
                transferTransactionRepo.save(transferTransaction);
                redissonClient.getMapCache("transId").put(transferTransaction.getId(), TransactionStatusEnum.COMPLETED.name(),1, TimeUnit.MINUTES);
            });
        });
    }

}
