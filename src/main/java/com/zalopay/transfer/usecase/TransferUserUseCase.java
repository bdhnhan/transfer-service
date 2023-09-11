package com.zalopay.transfer.usecase;

import com.zalopay.transfer.controller.request.TransferUserRequest;
import com.zalopay.transfer.controller.request.WithdrawRequest;
import com.zalopay.transfer.controller.response.ResultResponse;
import com.zalopay.transfer.controller.response.TransferUserResponse;
import com.zalopay.transfer.controller.response.WithdrawResponse;

public interface TransferUserUseCase extends UseCase<TransferUserRequest, ResultResponse<TransferUserResponse>> {
}
