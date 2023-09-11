package com.zalopay.transfer.controller.validator;

import br.com.fluentvalidator.AbstractValidator;
import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.constants.enums.TransType;
import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.controller.request.WithdrawRequest;
import org.apache.commons.lang3.EnumUtils;

import java.util.UUID;

public class WithdrawValidator extends AbstractValidator<WithdrawRequest> {
    @Override
    public void rules() {

        ruleFor(WithdrawRequest::getUserId)
                .must(this::isValidUUID)
                .withMessage("userId is not UUID")
                .critical();

        ruleFor(WithdrawRequest::getAmount)
                .must(amount -> amount > 0)
                .withMessage("Amount must greater than 0")
                .critical();

        ruleFor(WithdrawRequest::getSourceType)
                .must(sourceTrans -> ObjectTransactionEnum.WALLET.name().equals(sourceTrans))
                .withMessage("sourceType is not correct")
                .critical();

        ruleFor(WithdrawRequest::getDestType)
                .must(dest -> ObjectTransactionEnum.BANK_ACCOUNT.name().equals(dest))
                .withMessage("destType is not correct")
                .critical();
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
