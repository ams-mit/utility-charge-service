package com.ams.utilitychargeservice.exception;

public class DuplicateResourceException extends UtilityServiceException {

    public DuplicateResourceException(String message) {
        super(message, "UTIL_DUPLICATE_RESOURCE");
    }
}