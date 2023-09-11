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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultGetStatusTransactionUseCase implements GetStatusTransactionUseCase {

    private final TransferTransactionRepository transactionRepo;

    @Override
    @Transactional
    public ResultResponse<String> handle(String request) {

        Optional<TransferTransaction> transferTransactionOptional = transactionRepo.findById(request);
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
