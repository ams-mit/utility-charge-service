package com.ams.utilitychargeservice.service;

import com.ams.utilitychargeservice.dto.request.UtilityChargeRequest;
import com.ams.utilitychargeservice.dto.request.UtilityChargeUpdateRequest;
import com.ams.utilitychargeservice.dto.response.InternalUtilityChargeResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeSummaryResponse;
import com.ams.utilitychargeservice.enums.UtilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UtilityChargeService {

    UtilityChargeResponse recordCharge(UtilityChargeRequest request);

    Page<UtilityChargeResponse> getAllCharges(UUID unitId, UtilityType utilityType,
                                              Integer year, Integer month, Pageable pageable);

    UtilityChargeResponse getChargeById(UUID utilityChargeId);

    UtilityChargeResponse updateCharge(UUID utilityChargeId, UtilityChargeUpdateRequest request);

    void deleteCharge(UUID utilityChargeId);

    List<UtilityChargeResponse> getChargesForUnit(UUID unitId);

    List<UtilityChargeResponse> getChargesForUnitAndPeriod(UUID unitId, Integer year, Integer month);

    UtilityChargeSummaryResponse getSummary(Integer year, Integer month);

    // Called by billing-payment-service via internal endpoint
    List<InternalUtilityChargeResponse> getChargesForInvoiceGeneration(UUID unitId, Integer year, Integer month);
}