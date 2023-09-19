package com.zalopay.transfer.listener;

import com.google.gson.Gson;
import com.zalopay.transfer.constants.Constant;
import com.zalopay.transfer.constants.enums.TransactionInfoStatusEnum;
import com.zalopay.transfer.constants.enums.TransactionStatusEnum;
import com.zalopay.transfer.entity.Transaction;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.handler.AbstractHandler;
import com.zalopay.transfer.listener.event.TransferEventData;
import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@AllArgsConstructor
public class RollBackRequestedConsumer {

    private final ApplicationContext applicationContext;
    private final TransferTransactionRepository transferTransactionRepo;
    private final TransferInfoRepository transferInfoRepo;
    private final RedissonClient redissonClient;

    @KafkaListener(
            topics = Constant.Kafka.ROLL_BACK_STEPS_TOPIC,
            groupId = "groupId"
    )
    public void handle(String messageData) {
        TransferEventData data = new Gson().fromJson(messageData, TransferEventData.class);
        log.info("Listen from topic roll-back-steps-topic with data :: {}", data.getTransactionId());

        Optional<Transaction> transferTransactionOptional = transferTransactionRepo.findById(data.getTransactionId());

        if (transferTransactionOptional.isPresent()) {
            Transaction transaction = transferTransactionOptional.get();
            transaction.setStatus(TransactionStatusEnum.FAILED);
            transaction.setUpdatedTime(new Timestamp(System.currentTimeMillis()));
            transferTransactionRepo.save(transaction);
            redissonClient.getMapCache("transId")
                    .put(data.getTransactionId(), TransactionStatusEnum.FAILED.name(),1, TimeUnit.MINUTES);
            rollbackSteps(transaction.getId());
        }
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
