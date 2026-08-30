package com.ams.utilitychargeservice.service;

import com.ams.utilitychargeservice.dto.request.UtilityRateRequest;
import com.ams.utilitychargeservice.dto.request.UtilityRateStatusRequest;
import com.ams.utilitychargeservice.dto.response.UtilityRateResponse;
import com.ams.utilitychargeservice.enums.RateStatus;
import com.ams.utilitychargeservice.enums.UtilityType;

import java.util.List;
import java.util.UUID;

public interface UtilityRateService {

    UtilityRateResponse createRate(UtilityRateRequest request);

    List<UtilityRateResponse> getAllRates(UtilityType utilityType, RateStatus status);

    UtilityRateResponse getRateById(UUID utilityRateId);

    UtilityRateResponse updateRate(UUID utilityRateId, UtilityRateRequest request);

    UtilityRateResponse updateRateStatus(UUID utilityRateId, UtilityRateStatusRequest request);
}