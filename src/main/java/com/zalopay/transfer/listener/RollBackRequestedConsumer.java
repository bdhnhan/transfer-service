package com.zalopay.transfer.listener;

import com.google.gson.Gson;
import com.zalopay.transfer.constants.Constant;
import com.zalopay.transfer.constants.enums.TransactionInfoStatusEnum;
import com.zalopay.transfer.constants.enums.TransactionStatusEnum;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.entity.TransferTransaction;
import com.zalopay.transfer.handler.AbstractHandler;
import com.zalopay.transfer.listener.event.TransferEventData;
import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@AllArgsConstructor
public class RollBackRequestedConsumer {

    private final ApplicationContext applicationContext;
    private final TransferTransactionRepository transferTransactionRepo;
    private final TransferInfoRepository transferInfoRepo;
    @KafkaListener(
            topics = Constant.Kafka.ROLL_BACK_STEPS_TOPIC,
            groupId = "groupId"
    )
    public void handle(String messageData) {
        TransferEventData data = new Gson().fromJson(messageData, TransferEventData.class);
        log.info("Listen from topic roll-back-steps-topic with data :: {}", data.getTransactionId());

        Optional<TransferTransaction> transferTransactionOptional = transferTransactionRepo.findById(data.getTransactionId());

        if (transferTransactionOptional.isPresent()) {
            TransferTransaction transferTransaction = transferTransactionOptional.get();
            transferTransaction.setStatus(TransactionStatusEnum.FAILED);
            transferTransaction.setUpdatedTime(new Timestamp(System.currentTimeMillis()));
            transferTransactionRepo.save(transferTransaction);
            rollbackSteps(transferTransaction.getTransId());
        }
        transferTransactionOptional.ifPresent(transferTransaction -> {
            transferTransaction.setStatus(TransactionStatusEnum.FAILED);
            transferTransaction.setUpdatedTime(new Timestamp(System.currentTimeMillis()));
            transferTransactionRepo.save(transferTransaction);
        });
    }

    public void rollbackSteps(String transId) {
        List<TransferInfo> transferInfoList = transferInfoRepo.findAllByTransId(transId);
        if (!transferInfoList.isEmpty()) {
            List<TransferInfo> transactionRollback = new ArrayList<>();
            transferInfoList.forEach(transferInfo -> {
                if (transferInfo.getStatus().equals(TransactionInfoStatusEnum.COMPLETED)) {
                    transactionRollback.add(transferInfo);
                }
            });

            if (!transactionRollback.isEmpty()) {
                transactionRollback.parallelStream().forEach(transferInfo -> {
                    AbstractHandler abstractHandler = (AbstractHandler) applicationContext.getBean(transferInfo.getSourceType().name());
                    abstractHandler.revertTransaction(transferInfo);
                });
            }
        }
    }
}
