package com.ams.utilitychargeservice.service.impl;

import com.ams.utilitychargeservice.dto.request.UtilityRateRequest;
import com.ams.utilitychargeservice.dto.request.UtilityRateStatusRequest;
import com.ams.utilitychargeservice.dto.response.UtilityRateResponse;
import com.ams.utilitychargeservice.enums.RateStatus;
import com.ams.utilitychargeservice.enums.UtilityType;
import com.ams.utilitychargeservice.repository.UtilityRateRepository;
import com.ams.utilitychargeservice.service.UtilityRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UtilityRateServiceImpl implements UtilityRateService {

    private final UtilityRateRepository utilityRateRepository;

    @Override
    @Transactional
    public UtilityRateResponse createRate(UtilityRateRequest request) {
        // TODO: Sprint 2 — validate no duplicate active rate for same type, map to entity, save, return response
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    public List<UtilityRateResponse> getAllRates(UtilityType utilityType, RateStatus status) {
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    public UtilityRateResponse getRateById(UUID utilityRateId) {
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    @Transactional
    public UtilityRateResponse updateRate(UUID utilityRateId, UtilityRateRequest request) {
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }

    @Override
    @Transactional
    public UtilityRateResponse updateRateStatus(UUID utilityRateId, UtilityRateStatusRequest request) {
        throw new UnsupportedOperationException("Not yet implemented — Sprint 2");
    }
}