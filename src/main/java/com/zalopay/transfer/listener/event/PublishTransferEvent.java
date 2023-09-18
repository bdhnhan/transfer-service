package com.zalopay.transfer.listener.event;

import com.google.gson.Gson;
import com.zalopay.transfer.constants.Constant;
import com.zalopay.transfer.constants.enums.TransactionStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PublishTransferEvent {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(TransferEvent event) {
        redissonClient.getMapCache("transId")
                .put(event.getTransactionId(), TransactionStatusEnum.INITIAL.name(),1, TimeUnit.MINUTES);
        log.info("PUBLISH MESSAGE PROCESSING TRANSACTION ID {} TO KAFKA", event.getTransactionId());
        kafkaTemplate.send(Constant.Kafka.TRANSFER_TOPIC, new Gson().toJson(
                new TransferEventData(event.getTransactionId(), event.getCreatedTime())
        ));
    }
}
