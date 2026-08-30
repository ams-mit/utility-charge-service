package com.ams.utilitychargeservice.dto.request;

import com.ams.utilitychargeservice.enums.RateStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UtilityRateStatusRequest {

    @NotNull(message = "Status is required")
    private RateStatus status;
}