package com.zalopay.transfer.usecase;

import com.zalopay.transfer.constants.enums.ErrorCode;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.entity.Transaction;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultGetStatusTransactionUseCase implements GetStatusTransactionUseCase {

    private final TransferTransactionRepository transactionRepo;
    private final RedissonClient redissonClient;

    @Override
    @Transactional
    public ResultResponse<String> handle(String request) {

        String transactionStatus = (String) redissonClient.getMapCache("transId").get(request);
        if (!Strings.isEmpty(transactionStatus)) {
            log.info("Get status from Redis transId [{}] status [{}]", request, transactionStatus);
            return ResultResponse.<String>builder()
                    .status(ErrorCode.SUCCESSFULLY.getCode())
                    .messages(Collections.singletonList(ErrorCode.SUCCESSFULLY.getMessage()))
                    .result(transactionStatus)
                    .build();
        }
        Optional<Transaction> transferTransactionOptional = transactionRepo.findById(request);
        if (transferTransactionOptional.isPresent()) {
            return ResultResponse.<String>builder()
                    .status(ErrorCode.SUCCESSFULLY.getCode())
                    .messages(Collections.singletonList(ErrorCode.SUCCESSFULLY.getMessage()))
                    .result(transferTransactionOptional.get().getStatus().name())
                    .build();
        } else {
            return ResultResponse.<String>builder()
                    .status(ErrorCode.HANDLE_API_FAILED.getCode())
                    .messages(Collections.singletonList(ErrorCode.HANDLE_API_FAILED.getMessage()))
                    .result(null)
                    .build();
        }
    }

}
