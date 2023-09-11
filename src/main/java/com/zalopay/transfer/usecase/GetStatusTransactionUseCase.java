package com.zalopay.transfer.usecase;

import com.zalopay.transfer.controller.request.CallbackRequest;
import com.zalopay.transfer.controller.response.CallbackResponse;
import com.zalopay.transfer.controller.response.ResultResponse;

public interface GetStatusTransactionUseCase extends UseCase<String, ResultResponse<String>> {
}
