package com.zalopay.transfer.listener;

import com.google.gson.Gson;
import com.zalopay.transfer.constants.Constant;
import com.zalopay.transfer.constants.enums.TransactionStatusEnum;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.entity.Transaction;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@AllArgsConstructor
public class TransferRequestedConsumer {

    private final ApplicationContext applicationContext;
    private final TransferTransactionRepository transferTransactionRepo;
    private final TransferInfoRepository transferInfoRepo;
    private final RedissonClient redissonClient;
    @KafkaListener(
            topics = Constant.Kafka.TRANSFER_TOPIC,
            groupId = "groupId",
            concurrency = "8"
    )
    public void handle(String messageData) {
        TransferEventData data = new Gson().fromJson(messageData, TransferEventData.class);
        log.info("Listen from topic transfer-topic with data :: {}", data.getTransactionId());

        Optional<Transaction> transferTransactionOptional = transferTransactionRepo.findById(data.getTransactionId());
        transferTransactionOptional.ifPresent(transferTransaction -> {
            transferTransaction.setStatus(TransactionStatusEnum.PROCESSING);
            transferTransaction.setUpdatedTime(new Timestamp(System.currentTimeMillis()));
            transferTransactionRepo.save(transferTransaction);
            redissonClient.getMapCache("transId")
                    .put(transferTransaction.getId(), TransactionStatusEnum.PROCESSING.name(),1, TimeUnit.MINUTES);
        });
        Optional<TransferInfo> transferInfoOptional = transferInfoRepo.findByTransIdAndStep(data.getTransactionId(), 1);
        transferInfoOptional.ifPresent(transferInfo -> {
            AbstractHandler classHandler = (AbstractHandler) applicationContext.getBean(transferInfoOptional.get().getSourceType().name());
            classHandler.handleTransaction(transferInfo);
        });
    }
}
