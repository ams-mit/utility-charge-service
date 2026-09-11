package com.ams.utilitychargeservice.service.impl;

import com.ams.utilitychargeservice.dto.request.UtilityRateRequest;
import com.ams.utilitychargeservice.dto.request.UtilityRateStatusRequest;
import com.ams.utilitychargeservice.dto.response.UtilityRateResponse;
import com.ams.utilitychargeservice.entity.UtilityRate;
import com.ams.utilitychargeservice.enums.RateStatus;
import com.ams.utilitychargeservice.enums.UtilityType;
import com.ams.utilitychargeservice.exception.DuplicateResourceException;
import com.ams.utilitychargeservice.exception.ResourceNotFoundException;
import com.ams.utilitychargeservice.repository.UtilityRateRepository;
import com.ams.utilitychargeservice.service.UtilityRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UtilityRateServiceImpl implements UtilityRateService {

    private final UtilityRateRepository utilityRateRepository;

    @Override
    @Transactional
    public UtilityRateResponse createRate(UtilityRateRequest request) {
        log.info("Creating utility rate for type: {}", request.getUtilityType());

        // Validate no duplicate active rate for the same type starting on the same date
        utilityRateRepository.findByUtilityTypeAndStatus(request.getUtilityType(), RateStatus.ACTIVE)
                .stream()
                .filter(rate -> rate.getEffectiveFrom().equals(request.getEffectiveFrom()))
                .findFirst()
                .ifPresent(rate -> {
                    throw new DuplicateResourceException("An active utility rate already exists for this type starting on the same date");
                });

        UtilityRate rate = UtilityRate.builder()
                .utilityType(request.getUtilityType())
                .ratePerUnit(request.getRatePerUnit())
                .unitDescription(request.getUnitDescription())
                .effectiveFrom(request.getEffectiveFrom())
                .status(RateStatus.ACTIVE)
                .build();

        UtilityRate savedRate = utilityRateRepository.save(rate);
        return mapToResponse(savedRate);
    }

    @Override
    public List<UtilityRateResponse> getAllRates(UtilityType utilityType, RateStatus status) {
        log.info("Fetching utility rates. Type: {}, Status: {}", utilityType, status);

        List<UtilityRate> rates;
        if (utilityType != null && status != null) {
            rates = utilityRateRepository.findByUtilityTypeAndStatus(utilityType, status);
        } else if (status != null) {
            rates = utilityRateRepository.findByStatus(status);
        } else if (utilityType != null) {
            rates = utilityRateRepository.findByUtilityType(utilityType);
        } else {
            rates = utilityRateRepository.findAll();
        }

        return rates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UtilityRateResponse getRateById(UUID utilityRateId) {
        log.info("Fetching utility rate by ID: {}", utilityRateId);
        UtilityRate rate = utilityRateRepository.findById(utilityRateId)
                .orElseThrow(() -> new ResourceNotFoundException("Utility Rate", utilityRateId));
        return mapToResponse(rate);
    }

    @Override
    @Transactional
    public UtilityRateResponse updateRate(UUID utilityRateId, UtilityRateRequest request) {
        log.info("Updating utility rate ID: {}", utilityRateId);
        UtilityRate rate = utilityRateRepository.findById(utilityRateId)
                .orElseThrow(() -> new ResourceNotFoundException("Utility Rate", utilityRateId));

        rate.setUtilityType(request.getUtilityType());
        rate.setRatePerUnit(request.getRatePerUnit());
        rate.setUnitDescription(request.getUnitDescription());
        rate.setEffectiveFrom(request.getEffectiveFrom());

        UtilityRate updatedRate = utilityRateRepository.save(rate);
        return mapToResponse(updatedRate);
    }

    @Override
    @Transactional
    public UtilityRateResponse updateRateStatus(UUID utilityRateId, UtilityRateStatusRequest request) {
        log.info("Updating status of utility rate ID: {} to {}", utilityRateId, request.getStatus());
        UtilityRate rate = utilityRateRepository.findById(utilityRateId)
                .orElseThrow(() -> new ResourceNotFoundException("Utility Rate", utilityRateId));

        rate.setStatus(request.getStatus());

        UtilityRate updatedRate = utilityRateRepository.save(rate);
        return mapToResponse(updatedRate);
    }

    private UtilityRateResponse mapToResponse(UtilityRate rate) {
        return UtilityRateResponse.builder()
                .id(rate.getId())
                .utilityType(rate.getUtilityType())
                .ratePerUnit(rate.getRatePerUnit())
                .unitDescription(rate.getUnitDescription())
                .effectiveFrom(rate.getEffectiveFrom())
                .status(rate.getStatus())
                .createdAt(rate.getCreatedAt())
                .createdBy(rate.getCreatedBy())
                .build();
    }
}
