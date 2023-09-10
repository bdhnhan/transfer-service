package com.zalopay.transfer.entity;

import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.constants.enums.SourceTypeEnum;
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
    private String sourceTransferId;
    private String userId;
    private Long amount;
    @Enumerated(EnumType.STRING)
    private TransactionInfoStatusEnum status;
    @Enumerated(EnumType.STRING)
    private ObjectTransactionEnum type;
    @Enumerated(EnumType.STRING)
    private SourceTypeEnum sourceType;
    private Integer step;
    @Column(name = "subtrans_id")
    private String subTransId;
    private Timestamp createdTime;
    private Timestamp updatedTime;
}
