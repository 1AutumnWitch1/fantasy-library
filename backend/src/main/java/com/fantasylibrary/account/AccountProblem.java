package com.fantasylibrary.account;

import com.fantasylibrary.api.ApiResponse.Code;
import org.springframework.http.HttpStatus;

public class AccountProblem extends RuntimeException {
    private final HttpStatus status;
    private final Code code;

    public AccountProblem(HttpStatus status, Code code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public Code code() {
        return code;
    }
}
