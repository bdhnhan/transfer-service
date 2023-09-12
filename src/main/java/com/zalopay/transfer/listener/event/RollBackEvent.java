package com.zalopay.transfer.listener.event;

import org.springframework.context.ApplicationEvent;

public class RollBackEvent extends ApplicationEvent {

    private String transactionId;
    private Long createdTime;
    public RollBackEvent(Object source, String transactionId, Long createdTime) {
        super(source);
        this.transactionId = transactionId;
        this.createdTime = createdTime;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }
}
