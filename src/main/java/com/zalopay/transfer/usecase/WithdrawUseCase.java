package com.zalopay.transfer.usecase;

import com.zalopay.transfer.controller.request.TopUpRequest;
import com.zalopay.transfer.controller.request.WithdrawRequest;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.controller.response.TopUpResponse;
import com.zalopay.transfer.controller.response.WithdrawResponse;

public interface WithdrawUseCase extends UseCase<WithdrawRequest, ResultResponse<WithdrawResponse>> {
}
