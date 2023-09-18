package com.zalopay.transfer.controller.validator;

import br.com.fluentvalidator.context.ValidationResult;
import com.zalopay.transfer.constants.enums.ObjectTransactionEnum;
import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.controller.request.WithdrawRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
public class WithdrawValidatorTest {
    private final Long moneyTransfer = 100L;
    private final String userId = UUID.randomUUID().toString();
    private final String sourceId = "ZLP_WALLET";
    private final String destId = "BANK_VCD";
    private final String sourceSender = "0918340208";

    @Test
    public void testWithdrawRequestShouldBeOk() {
        WithdrawRequest request = WithdrawRequest.builder()
                .amount(moneyTransfer)
                .userId(userId)
                .sourceType(ObjectTransactionEnum.WALLET.name())
                .sourceId(sourceId)
                .sourceSender(sourceSender)
                .destType(ObjectTransactionEnum.BANK_ACCOUNT.name())
                .destId(destId)
                .destReceiver(userId)
                .build();
        ValidationResult validator = new WithdrawValidator().validate(request);
        Assertions.assertTrue(validator.isValid());
        Assertions.assertTrue(validator.getErrors().isEmpty());
    }

    @Test
    public void testWithdrawRequestWithEmptyUserIdAndAmountLessThan1ShouldBeOk() {
        WithdrawRequest request = WithdrawRequest.builder()
                .amount(-1L)
                .userId("userIdIsNotCorrect")
                .sourceType(ObjectTransactionEnum.WALLET.name())
                .sourceId(sourceId)
                .sourceSender(sourceSender)
                .destType(ObjectTransactionEnum.BANK_ACCOUNT.name())
                .destId(destId)
                .destReceiver(userId)
                .build();

        List<String> expectError = Arrays.asList("userId is not UUID", "Amount must greater than 0");
        ValidationResult validator = new WithdrawValidator().validate(request);
        Assertions.assertFalse(validator.isValid());
        Assertions.assertEquals(2, validator.getErrors().size());
        validator.getErrors().forEach(error -> {
            Assertions.assertTrue(expectError.contains(error.getMessage()));
        });
    }
}
