package com.zalopay.transfer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@Data
public class BankConnect {
    @Id
    private String id;
    private String bankCode;
    private String numberAccount;
    private String userId;
    private Timestamp createdTime;
}
