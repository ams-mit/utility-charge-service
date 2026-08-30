package com.ams.utilitychargeservice.exception;

import lombok.Getter;

@Getter
public class UtilityServiceException extends RuntimeException {

    private final String errorCode;

    public UtilityServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}