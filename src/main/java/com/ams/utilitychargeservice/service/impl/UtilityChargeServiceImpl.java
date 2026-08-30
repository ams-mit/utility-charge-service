package com.ams.utilitychargeservice.service.impl;

import com.ams.utilitychargeservice.dto.request.UtilityChargeRequest;
import com.ams.utilitychargeservice.dto.request.UtilityChargeUpdateRequest;
import com.ams.utilitychargeservice.dto.response.InternalUtilityChargeResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeResponse;
import com.ams.utilitychargeservice.dto.response.UtilityChargeSummaryResponse;
import com.ams.utilitychargeservice.enums.UtilityType;
import com.ams.utilitychargeservice.repository.UtilityChargeRepository;
import com.ams.utilitychargeservice.repository.UtilityRateRepository;
import com.ams.utilitychargeservice.service.UtilityChargeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UtilityChargeServiceImpl implements UtilityChargeService {

    private final UtilityChargeRepository utilityChargeRepository;
    private final UtilityRateRepository utilityRateRepository;

    @Override
    @Transactional
    public UtilityChargeResponse recordCharge(UtilityChargeRequest request) {
        // TODO: Sprint 2 — fetch active rate, calculate amount, check duplicate, save
        // CRITICAL: snapshot ratePerUnit at time of recording — never a FK reference to UtilityRate
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    public Page<UtilityChargeResponse> getAllCharges(UUID unitId, UtilityType utilityType,
                                                     Integer year, Integer month, Pageable pageable) {
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    public UtilityChargeResponse getChargeById(UUID utilityChargeId) {
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    @Transactional
    public UtilityChargeResponse updateCharge(UUID utilityChargeId, UtilityChargeUpdateRequest request) {
        // TODO: Sprint 2 — check no invoice exists for this unit/period before allowing update
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    @Transactional
    public void deleteCharge(UUID utilityChargeId) {
        // TODO: Sprint 2 — check no invoice exists for this unit/period before allowing delete
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    public List<UtilityChargeResponse> getChargesForUnit(UUID unitId) {
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    public List<UtilityChargeResponse> getChargesForUnitAndPeriod(UUID unitId, Integer year, Integer month) {
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    public UtilityChargeSummaryResponse getSummary(Integer year, Integer month) {
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    public List<InternalUtilityChargeResponse> getChargesForInvoiceGeneration(UUID unitId,
                                                                              Integer year,
                                                                              Integer month) {
        // TODO: Sprint 2 — called by billing-payment-service. Returns empty list (not 404) if none exist.
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }
}