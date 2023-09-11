package com.zalopay.transfer.repository;

import com.zalopay.transfer.entity.TransferInfo;
import com.zalopay.transfer.entity.TransferTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface TransferInfoRepository extends CrudRepository<TransferInfo, String>, JpaRepository<TransferInfo, String> {
    Optional<TransferInfo> findByTransIdAndStep(String transId, Integer step);
    Optional<TransferInfo> findBySubTransId(String subTransId);
    Optional<TransferInfo> findFirstByTransIdAndSubTransIdIsNullOrderByStepAsc(String transId);
}
