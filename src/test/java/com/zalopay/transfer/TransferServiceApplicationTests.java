package com.zalopay.transfer;

import br.com.fluentvalidator.context.ValidationResult;
import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.controller.validator.TopUpValidator;
import org.junit.jupiter.api.Test;

class TransferServiceApplicationTests {

    @Test
    void contextLoads() {
        TopUpRequest topUpRequest = new TopUpRequest();
        topUpRequest.setRequestId("s");
        topUpRequest.setAmount(0L);
        topUpRequest.setDestId("w");
        topUpRequest.setPromotion("");
        topUpRequest.setTransType("");
        topUpRequest.setSourceId("e");

        TopUpValidator topUpValidator = new TopUpValidator();
        ValidationResult validationResult = topUpValidator.validate(topUpRequest);
        if (!validationResult.isValid()) {
            validationResult.getErrors().forEach(error -> System.out.println(error.getMessage()));
        }

    }

}
