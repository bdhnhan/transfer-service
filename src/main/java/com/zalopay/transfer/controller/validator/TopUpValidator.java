package com.zalopay.transfer.controller.validator;

import br.com.fluentvalidator.AbstractValidator;
import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.constants.enums.TopUpTypeEnum;
import org.apache.commons.lang3.EnumUtils;

import java.util.UUID;

public class TopUpValidator extends AbstractValidator<TopUpRequest> {
    @Override
    public void rules() {

        ruleFor(TopUpRequest::getUserId)
                .must(this::isValidUUID)
                .withMessage("userId is not UUID")
                .critical();

        ruleFor(TopUpRequest::getAmount)
                .must(amount -> amount > 0)
                .withMessage("Amount must greater than 0")
                .critical();

        ruleFor(TopUpRequest::getTransType)
                .must(transType -> EnumUtils.isValidEnum(TopUpTypeEnum.class, transType))
                .withMessage("transType is not correct")
                .critical();

        ruleFor(TopUpRequest::getSourceTrans)
                .must(sourceTrans -> EnumUtils.isValidEnum(ObjectTransactionEnum.class, sourceTrans))
                .withMessage("sourceTrans is not correct")
                .critical();

        ruleFor(TopUpRequest::getDestTrans)
                .must(dest -> EnumUtils.isValidEnum(ObjectTransactionEnum.class, dest))
                .withMessage("destTrans is not correct")
                .critical();

        ruleFor(TopUpRequest::getDestTrans)
                .must(dest -> EnumUtils.isValidEnum(ObjectTransactionEnum.class, dest))
                .withMessage("destTrans is not correct")
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
