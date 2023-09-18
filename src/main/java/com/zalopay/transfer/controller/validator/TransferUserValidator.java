package com.zalopay.transfer.controller.validator;

import br.com.fluentvalidator.AbstractValidator;
import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.controller.request.TransferUserRequest;
import com.zalopay.transfer.controller.request.WithdrawRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class TransferUserValidator extends AbstractValidator<TransferUserRequest> {
    @Override
    public void rules() {

        ruleFor(TransferUserRequest::getUserId)
                .must(this::isValidUUID).withMessage("userId is not UUID");
        ruleFor(TransferUserRequest::getAmount)
                .must(amount -> amount > 0).withMessage("Amount must greater than 0");
        ruleFor(TransferUserRequest::getSourceType)
                .must(sourceTrans -> ObjectTransactionEnum.WALLET.name().equals(sourceTrans)).withMessage("sourceType is not correct");
        ruleFor(TransferUserRequest::getSourceId)
                .must(StringUtils::isNotEmpty).withMessage("sourceId is not correct");
        ruleFor(TransferUserRequest::getSourceSender)
                .must(StringUtils::isNotEmpty).withMessage("sourceSender is not correct");
        ruleFor(TransferUserRequest::getDestType)
                .must(dest -> ObjectTransactionEnum.WALLET.name().equals(dest)).withMessage("destType is not correct");
        ruleFor(TransferUserRequest::getDestId)
                .must(StringUtils::isNotEmpty).withMessage("destId is not correct");
        ruleFor(TransferUserRequest::getDestReceiver)
                .must(StringUtils::isNotEmpty).withMessage("destReceiver is not correct");
    }

    public boolean isValidUUID(String uuid) {
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
