package com.ams.utilitychargeservice.exception;

public class InvoiceAlreadyExistsException extends UtilityServiceException {

    public InvoiceAlreadyExistsException(String unitId, int year, int month) {
        super(
                "An invoice already exists for unit " + unitId + " for period " + year + "-" + month +
                        ". Utility charge cannot be modified or deleted.",
                "UTIL_INVOICE_ALREADY_EXISTS"
        );
    }
}