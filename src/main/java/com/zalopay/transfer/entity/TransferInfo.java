package com.zalopay.transfer.entity;

import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.constants.enums.ActionTypeEnum;
import com.zalopay.transfer.constants.enums.TransactionInfoStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

@Table
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class TransferInfo {
    @Id
    private String id;
    private String transId;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private TransactionInfoStatusEnum status;
    @Enumerated(EnumType.STRING)
    private ObjectTransactionEnum sourceType;
    private String sourceTransferId;
    private String userSourceId;
    @Enumerated(EnumType.STRING)
    private ActionTypeEnum actionType;

    private Integer step;
    private String stepId;

    private Timestamp createdTime;
    private Timestamp updatedTime;
}