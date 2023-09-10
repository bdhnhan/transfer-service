package com.zalopay.transfer.business;

import com.zalopay.transfer.repository.TransferInfoRepository;
import com.zalopay.transfer.repository.TransferTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TransferBusiness {
    private final TransferTransactionRepository transferTransactionRepo;
    private final TransferInfoRepository transferInfoRepo;
}
