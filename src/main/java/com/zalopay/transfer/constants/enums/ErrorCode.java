package com.zalopay.transfer.constants.enums;

public enum ErrorCode {
    MANDATORY_FIELD(600, "Mandatory field"),
    SOURCE_OR_DEST_INVALID(601, "Source or destination to transfer invalid"),
    INITIAL_TRANSACTION_FAILED(602, "Transaction initial failed"),
    SUCCESSFULLY(200, "API handle completed"),
    HANDLE_API_FAILED(603, "Can not handle with this request");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
