package com.ams.utilitychargeservice.dto.request;

import com.ams.utilitychargeservice.enums.UtilityType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UtilityRateRequest {

    @NotNull(message = "Utility type is required")
    private UtilityType utilityType;

    @NotNull(message = "Rate per unit is required")
    @DecimalMin(value = "0.0001", message = "Rate must be greater than zero")
    @Digits(integer = 8, fraction = 4, message = "Rate must have at most 8 integer and 4 decimal digits")
    private BigDecimal ratePerUnit;

    @Size(max = 100, message = "Unit description must not exceed 100 characters")
    private String unitDescription;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFrom;
}