package com.zalopay.transfer.controller.validator;

import br.com.fluentvalidator.AbstractValidator;
import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.constants.enums.TransType;
import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.constants.enums.TopUpTypeEnum;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.UUID;

public class TopUpValidator extends AbstractValidator<TopUpRequest> {
    @Override
    public void rules() {

        ruleFor(TopUpRequest::getUserId)
                .must(this::isValidUUID).withMessage("userId is not UUID");
        ruleFor(TopUpRequest::getAmount)
                .must(amount -> amount > 0).withMessage("Amount must greater than 0");
        ruleFor(TopUpRequest::getSourceType)
                .must(sourceTrans -> ObjectTransactionEnum.BANK_ACCOUNT.name().equals(sourceTrans)).withMessage("sourceType is not correct");
        ruleFor(TopUpRequest::getSourceId)
                .must(StringUtils::isNotEmpty).withMessage("sourceId is not correct");
        ruleFor(TopUpRequest::getSourceSender)
                .must(this::isValidUUID).withMessage("sourceSender is not UUID");
        ruleFor(TopUpRequest::getDestType)
                .must(dest -> ObjectTransactionEnum.WALLET.name().equals(dest)).withMessage("destType is not correct");
        ruleFor(TopUpRequest::getDestId)
                .must(StringUtils::isNotEmpty).withMessage("destId is not correct");
        ruleFor(TopUpRequest::getDestReceiver)
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
