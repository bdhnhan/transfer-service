package com.zalopay.transfer.usecase;

import com.zalopay.transfer.controller.request.CallbackRequest;
import com.zalopay.transfer.controller.response.CallbackResponse;
import com.zalopay.transfer.controller.response.ResultResponse;

public interface CallbackUseCase extends UseCase<CallbackRequest, ResultResponse<CallbackResponse>> {
}
