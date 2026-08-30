package com.ams.utilitychargeservice.exception;

public class ResourceNotFoundException extends UtilityServiceException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(resourceName + " not found with identifier: " + identifier,
                "UTIL_" + resourceName.toUpperCase().replace(" ", "_") + "_NOT_FOUND");
    }
}