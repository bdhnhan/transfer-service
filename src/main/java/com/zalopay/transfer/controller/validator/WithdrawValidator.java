package com.zalopay.transfer.controller.validator;

import br.com.fluentvalidator.AbstractValidator;
import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.constants.enums.TransType;
import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.controller.request.WithdrawRequest;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class WithdrawValidator extends AbstractValidator<WithdrawRequest> {
    @Override
    public void rules() {
        ruleFor(WithdrawRequest::getUserId)
                .must(this::isValidUUID).withMessage("userId is not UUID");
        ruleFor(WithdrawRequest::getAmount)
                .must(amount -> amount > 0).withMessage("Amount must greater than 0");
        ruleFor(WithdrawRequest::getSourceType)
                .must(sourceTrans -> ObjectTransactionEnum.WALLET.name().equals(sourceTrans)).withMessage("sourceType is not correct");
        ruleFor(WithdrawRequest::getSourceId)
                .must(StringUtils::isNotEmpty).withMessage("sourceId is not correct");
        ruleFor(WithdrawRequest::getSourceSender)
                .must(StringUtils::isNotEmpty).withMessage("sourceSender is not correct");
        ruleFor(WithdrawRequest::getDestType)
                .must(dest -> ObjectTransactionEnum.BANK_ACCOUNT.name().equals(dest)).withMessage("destType is not correct");
        ruleFor(WithdrawRequest::getDestId)
                .must(StringUtils::isNotEmpty).withMessage("destId is not correct");
        ruleFor(WithdrawRequest::getDestReceiver)
                .must(this::isValidUUID).withMessage("destReceiver is not UUID");
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
