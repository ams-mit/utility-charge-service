package com.ams.utilitychargeservice.repository;

import com.ams.utilitychargeservice.entity.UtilityCharge;
import com.ams.utilitychargeservice.enums.UtilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilityChargeRepository extends JpaRepository<UtilityCharge, UUID> {

    // UTIL-006: duplicate check before recording
    Optional<UtilityCharge> findByUnitIdAndUtilityTypeAndBillingYearAndBillingMonth(
            UUID unitId, UtilityType utilityType, Integer billingYear, Integer billingMonth);

    // UTIL-007: paginated, all 4 filters
    Page<UtilityCharge> findByUnitIdAndUtilityTypeAndBillingYearAndBillingMonth(
            UUID unitId, UtilityType utilityType, Integer year, Integer month, Pageable pageable);

    // UTIL-007: paginated, unitId + year + month (no type filter)
    @Query("SELECT uc FROM UtilityCharge uc WHERE uc.unitId = :unitId " +
            "AND uc.billingYear = :year AND uc.billingMonth = :month")
    Page<UtilityCharge> findByUnitIdAndBillingYearAndBillingMonthPageable(
            @Param("unitId") UUID unitId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            Pageable pageable);

    // UTIL-007: paginated, unitId only
    Page<UtilityCharge> findByUnitId(UUID unitId, Pageable pageable);

    // UTIL-007: paginated, type only
    Page<UtilityCharge> findByUtilityType(UtilityType utilityType, Pageable pageable);

    // UTIL-011: full history for a unit (no pagination — resident view)
    List<UtilityCharge> findByUnitId(UUID unitId);

    // UTIL-012 + UTIL-014: charges for a unit in a specific period
    List<UtilityCharge> findByUnitIdAndBillingYearAndBillingMonth(
            UUID unitId, Integer billingYear, Integer billingMonth);

    // UTIL-013: all charges for a period (summary aggregation)
    List<UtilityCharge> findByBillingYearAndBillingMonth(
            Integer billingYear, Integer billingMonth);

    // invoice-exists guard — check without loading full records
    boolean existsByUnitIdAndBillingYearAndBillingMonth(
            UUID unitId, Integer billingYear, Integer billingMonth);
}