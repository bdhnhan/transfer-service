package com.zalopay.transfer.listener.event;

import com.google.gson.Gson;
import com.zalopay.transfer.constants.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class PublishRollBackEvent {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEvent(RollBackEvent event) {
        log.info("PUBLISH MESSAGE ROLLING BACK TRANSACTION ID {} TO KAFKA", event.getTransactionId());

        kafkaTemplate.send(Constant.Kafka.ROLL_BACK_STEPS_TOPIC, new Gson().toJson(
                new TransferEventData(event.getTransactionId(), event.getCreatedTime())
        ));
    }
}
