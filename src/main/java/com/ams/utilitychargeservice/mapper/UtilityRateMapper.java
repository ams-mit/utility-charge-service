package com.ams.utilitychargeservice.mapper;

import com.ams.utilitychargeservice.dto.response.UtilityRateResponse;
import com.ams.utilitychargeservice.entity.UtilityRate;
import org.springframework.stereotype.Component;

/**
 * Converts UtilityRate JPA entities to UtilityRateResponse DTOs.
 *
 * WHY A SEPARATE MAPPER?
 * - Entities belong to the database layer. DTOs belong to the API layer.
 * - If you change the database table later, you only change the entity and
 *   this mapper — the API response stays the same.
 * - Keeps entity classes clean (no API-specific logic inside them).
 */
@Component
public class UtilityRateMapper {

    /**
     * Converts a single UtilityRate entity to a UtilityRateResponse DTO.
     * Called every time we need to return rate data to the API caller.
     */
    public UtilityRateResponse toResponse(UtilityRate rate) {
        return UtilityRateResponse.builder()
                .id(rate.getId())
                .utilityType(rate.getUtilityType() != null ? rate.getUtilityType().name() : null)
                .ratePerUnit(rate.getRatePerUnit())
                .unitDescription(rate.getUnitDescription())
                .effectiveFrom(rate.getEffectiveFrom())
                .status(rate.getStatus() != null ? rate.getStatus().name() : null)
                .createdAt(rate.getCreatedAt())
                .createdBy(rate.getCreatedBy())
                .build();
    }

    /**
     * Converts a list of UtilityRate entities to a list of UtilityRateResponse DTOs.
     * Used for list endpoints (UTIL-002).
     */
    public java.util.List<UtilityRateResponse> toResponseList(java.util.List<UtilityRate> rates) {
        return rates.stream()
                .map(this::toResponse)
                .toList();
    }
}
