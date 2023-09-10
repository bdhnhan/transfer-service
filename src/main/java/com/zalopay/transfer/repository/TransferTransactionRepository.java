package com.zalopay.transfer.repository;

import com.zalopay.transfer.entity.TransferTransaction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferTransactionRepository extends CrudRepository<TransferTransaction, String> {
}
