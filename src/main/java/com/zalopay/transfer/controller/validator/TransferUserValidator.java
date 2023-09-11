package com.zalopay.transfer.controller.validator;

import br.com.fluentvalidator.AbstractValidator;
import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.controller.request.TransferUserRequest;
import com.zalopay.transfer.controller.request.WithdrawRequest;

import java.util.UUID;

public class TransferUserValidator extends AbstractValidator<TransferUserRequest> {
    @Override
    public void rules() {

        ruleFor(TransferUserRequest::getUserId)
                .must(this::isValidUUID)
                .withMessage("userId is not UUID")
                .critical();

        ruleFor(TransferUserRequest::getAmount)
                .must(amount -> amount > 0)
                .withMessage("Amount must greater than 0")
                .critical();

        ruleFor(TransferUserRequest::getSourceType)
                .must(sourceTrans -> ObjectTransactionEnum.WALLET.name().equals(sourceTrans))
                .withMessage("sourceType is not correct")
                .critical();

        ruleFor(TransferUserRequest::getDestType)
                .must(dest -> ObjectTransactionEnum.WALLET.name().equals(dest))
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
