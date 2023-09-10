package com.zalopay.transfer.listener;

import com.google.gson.Gson;
import com.zalopay.transfer.constants.Constant;
import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.handler.AbstractHandler;
import com.zalopay.transfer.listener.event.TransferEvent;
import com.zalopay.transfer.listener.event.TransferEventData;
import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@AllArgsConstructor
public class TransferRequestedConsumer {

    private final ApplicationContext applicationContext;
    private final TransferInfoRepository transferInfoRepo;
    @KafkaListener(
            topics = Constant.Kafka.TRANSFER_TOPIC,
            groupId = "groupId"
    )
    public void handle(String messageData) {
        TransferEventData data = new Gson().fromJson(messageData, TransferEventData.class);
        log.info("Listen from topic transfer-topic with data :: {}", data.getTransactionId());

        Optional<TransferInfo> transferInfoOptional = transferInfoRepo.findByTransIdAndStep(data.getTransactionId(), 1);

        transferInfoOptional.ifPresent(transferInfo -> {
            AbstractHandler classHandler = (AbstractHandler) applicationContext.getBean(transferInfoOptional.get().getType().name());
            classHandler.handleTransaction(transferInfo);
        });
    }
}
