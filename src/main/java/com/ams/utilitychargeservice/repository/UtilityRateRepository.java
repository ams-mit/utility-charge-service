package com.ams.utilitychargeservice.repository;

import com.ams.utilitychargeservice.entity.UtilityRate;
import com.ams.utilitychargeservice.enums.RateStatus;
import com.ams.utilitychargeservice.enums.UtilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilityRateRepository extends JpaRepository<UtilityRate, UUID> {

    List<UtilityRate> findByStatus(RateStatus status);

    List<UtilityRate> findByUtilityTypeAndStatus(UtilityType utilityType, RateStatus status);

    Optional<UtilityRate> findFirstByUtilityTypeAndStatusOrderByEffectiveFromDesc(
            UtilityType utilityType, RateStatus status
    );

    List<UtilityRate> findByUtilityType(UtilityType utilityType);
}