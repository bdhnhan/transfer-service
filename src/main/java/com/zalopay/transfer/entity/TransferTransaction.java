package com.zalopay.transfer.entity;

import com.zalopay.transfer.constants.enums.TransactionStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferTransaction {
    @Id
    private String transId;
    @Enumerated(EnumType.STRING)
    private TransactionStatusEnum status;
    private Long amount;
    private String transType;
    private String description = "";
    private Timestamp createdTime;
    private Timestamp updatedTime;
}
