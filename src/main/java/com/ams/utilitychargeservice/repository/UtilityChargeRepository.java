package com.ams.utilitychargeservice.repository;

import com.ams.utilitychargeservice.entity.UtilityCharge;
import com.ams.utilitychargeservice.enums.UtilityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilityChargeRepository extends JpaRepository<UtilityCharge, UUID> {

    Optional<UtilityCharge> findByUnitIdAndUtilityTypeAndBillingYearAndBillingMonth(
            UUID unitId, UtilityType utilityType, Integer billingYear, Integer billingMonth
    );

    List<UtilityCharge> findByUnitIdAndBillingYearAndBillingMonth(
            UUID unitId, Integer billingYear, Integer billingMonth
    );

    List<UtilityCharge> findByUnitId(UUID unitId);

    Page<UtilityCharge> findByUnitIdAndUtilityTypeAndBillingYearAndBillingMonth(
            UUID unitId, UtilityType utilityType, Integer year, Integer month, Pageable pageable
    );

    boolean existsByUnitIdAndBillingYearAndBillingMonth(
            UUID unitId, Integer billingYear, Integer billingMonth
    );
}