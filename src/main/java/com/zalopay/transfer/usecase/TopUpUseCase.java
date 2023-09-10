package com.zalopay.transfer.usecase;

import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.controller.response.TopUpResponse;

public interface TopUpUseCase extends UseCase<TopUpRequest, ResultResponse<TopUpResponse>> {
}
