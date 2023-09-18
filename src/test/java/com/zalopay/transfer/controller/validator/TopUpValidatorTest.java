package com.zalopay.transfer.controller.validator;

import br.com.fluentvalidator.context.ValidationResult;
import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.controller.request.TopUpRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
public class TopUpValidatorTest {
    private final Long moneyTransfer = 100L;
    private final String userId = UUID.randomUUID().toString();
    private final String sourceId = "BANK_VCB";
    private final String destId = "ZLP_WALLET";
    private final String destReceiver = "0918340208";

    @Test
    public void testTopUpRequestShouldBeOk() {
        TopUpRequest request = TopUpRequest.builder()
                .amount(moneyTransfer)
                .userId(userId)
                .sourceType(ObjectTransactionEnum.BANK_ACCOUNT.name())
                .sourceId(sourceId)
                .sourceSender(userId)
                .destType(ObjectTransactionEnum.WALLET.name())
                .destId(destId)
                .destReceiver(destReceiver)
                .build();
        ValidationResult validator = new TopUpValidator().validate(request);
        Assertions.assertTrue(validator.isValid());
        Assertions.assertTrue(validator.getErrors().isEmpty());
    }

    @Test
    public void testTopUpRequestWithEmptyUserIdAndAmountLessThan1ShouldBeOk() {
        TopUpRequest request = TopUpRequest.builder()
                .amount(-1L)
                .userId("userIdIsNotCorrect")
                .sourceType(ObjectTransactionEnum.BANK_ACCOUNT.name())
                .sourceId(sourceId)
                .sourceSender(userId)
                .destType(ObjectTransactionEnum.WALLET.name())
                .destId(destId)
                .destReceiver(destReceiver)
                .build();

        List<String> expectError = Arrays.asList("userId is not UUID", "Amount must greater than 0");
        ValidationResult validator = new TopUpValidator().validate(request);
        Assertions.assertFalse(validator.isValid());
        Assertions.assertEquals(2, validator.getErrors().size());
        validator.getErrors().forEach(error -> {
            Assertions.assertTrue(expectError.contains(error.getMessage()));
        });
    }
}
