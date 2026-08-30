package com.ams.utilitychargeservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UtilityChargeUpdateRequest {

    @NotNull(message = "Usage value is required")
    @DecimalMin(value = "0.0001", message = "Usage value must be greater than zero")
    @Digits(integer = 10, fraction = 4, message = "Usage value format invalid")
    private BigDecimal usageValue;

    @NotBlank(message = "Reason for correction is required")
    @Size(min = 10, message = "Reason must be at least 10 characters")
    private String correctionReason;
}