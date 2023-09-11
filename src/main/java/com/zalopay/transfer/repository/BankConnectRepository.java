package com.zalopay.transfer.repository;

import com.zalopay.transfer.entity.BankConnect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BankConnectRepository extends CrudRepository<BankConnect, String>, JpaRepository<BankConnect, String> {
    Optional<BankConnect> findByBankCodeAndUserId(String bankCode, String userId);
}
