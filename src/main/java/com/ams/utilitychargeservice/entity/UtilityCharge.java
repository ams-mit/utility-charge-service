package com.ams.utilitychargeservice.entity;

import com.ams.utilitychargeservice.enums.UtilityType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "utility_charges",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_unit_type_period",
                columnNames = {"unit_id", "utility_type", "billing_year", "billing_month"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilityCharge extends BaseEntity {

    @Column(name = "unit_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID unitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", nullable = false, length = 20)
    private UtilityType utilityType;

    @Column(name = "billing_year", nullable = false)
    private Integer billingYear;

    @Column(name = "billing_month", nullable = false)
    private Integer billingMonth;

    @Column(name = "usage_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal usageValue;

    /**
     * Snapshot of ratePerUnit at the time of recording.
     * Changing the utility rate later does NOT affect this record.
     */
    @Column(name = "rate_per_unit_snapshot", nullable = false, precision = 10, scale = 4)
    private BigDecimal ratePerUnitSnapshot;

    @Column(name = "utility_rate_id", columnDefinition = "BINARY(16)")
    private UUID utilityRateId;

    @Column(name = "calculated_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal calculatedAmount;
}