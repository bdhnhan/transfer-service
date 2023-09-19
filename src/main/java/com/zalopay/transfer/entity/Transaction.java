package com.zalopay.transfer.entity;

import com.zalopay.transfer.constants.enums.TransType;
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
public class Transaction {
    @Id
    private String id;
    @Enumerated(EnumType.STRING)
    private TransactionStatusEnum status;
    private Long amount;
    @Enumerated(EnumType.STRING)
    private TransType transType;
    private String description = "";
    private String userId;
    private Timestamp createdTime;
    private Timestamp updatedTime;
}
