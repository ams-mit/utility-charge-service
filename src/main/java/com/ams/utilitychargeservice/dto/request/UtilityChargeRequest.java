package com.ams.utilitychargeservice.dto.request;

import com.ams.utilitychargeservice.enums.UtilityType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class UtilityChargeRequest {

    @NotNull(message = "Unit ID is required")
    private UUID unitId;

    @NotNull(message = "Utility type is required")
    private UtilityType utilityType;

    @NotNull(message = "Billing year is required")
    @Min(value = 2000, message = "Billing year must be 2000 or later")
    @Max(value = 2100, message = "Billing year is out of range")
    private Integer billingYear;

    @NotNull(message = "Billing month is required")
    @Min(value = 1, message = "Billing month must be between 1 and 12")
    @Max(value = 12, message = "Billing month must be between 1 and 12")
    private Integer billingMonth;

    @NotNull(message = "Usage value is required")
    @DecimalMin(value = "0.0001", message = "Usage value must be greater than zero")
    @Digits(integer = 10, fraction = 4, message = "Usage value format invalid")
    private BigDecimal usageValue;
}