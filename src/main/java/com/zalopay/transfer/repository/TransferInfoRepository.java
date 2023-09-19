package com.zalopay.transfer.repository;

import com.zalopay.transfer.entity.TransferInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface TransferInfoRepository extends CrudRepository<TransferInfo, String>, JpaRepository<TransferInfo, String> {
    Optional<TransferInfo> findByTransIdAndStep(String transId, Integer step);
    Optional<TransferInfo> findByStepId(String subTransId);
    Optional<TransferInfo> findFirstByTransIdAndStepIdIsNullOrderByStepAsc(String transId);
    List<TransferInfo> findAllByTransId(String transId);
}
