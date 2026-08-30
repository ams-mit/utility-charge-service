-- ================================================================
-- utility_db schema
-- AMS Group 3 — utility-charge-service
-- ================================================================

CREATE TABLE IF NOT EXISTS utility_rates (
                                             id              BINARY(16)      NOT NULL,
    utility_type    VARCHAR(20)     NOT NULL,
    rate_per_unit   DECIMAL(10,4)   NOT NULL,
    unit_description VARCHAR(100)   NULL,
    effective_from  DATE            NOT NULL,
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    created_by      VARCHAR(255)    NULL,
    updated_by      VARCHAR(255)    NULL,
    CONSTRAINT pk_utility_rates PRIMARY KEY (id),
    CONSTRAINT chk_rate_positive CHECK (rate_per_unit > 0),
    CONSTRAINT chk_utility_type CHECK (utility_type IN ('WATER','ELECTRICITY','GAS','PARKING')),
    CONSTRAINT chk_rate_status CHECK (status IN ('ACTIVE','INACTIVE'))
    );

CREATE TABLE IF NOT EXISTS utility_charges (
                                               id                      BINARY(16)      NOT NULL,
    unit_id                 BINARY(16)      NOT NULL,
    utility_type            VARCHAR(20)     NOT NULL,
    billing_year            INT             NOT NULL,
    billing_month           INT             NOT NULL,
    usage_value             DECIMAL(12,4)   NOT NULL,
    rate_per_unit_snapshot  DECIMAL(10,4)   NOT NULL,
    utility_rate_id         BINARY(16)      NULL,
    calculated_amount       DECIMAL(14,2)   NOT NULL,
    created_at              DATETIME(6)     NOT NULL,
    updated_at              DATETIME(6)     NOT NULL,
    created_by              VARCHAR(255)    NULL,
    updated_by              VARCHAR(255)    NULL,
    CONSTRAINT pk_utility_charges PRIMARY KEY (id),
    CONSTRAINT uq_unit_type_period UNIQUE (unit_id, utility_type, billing_year, billing_month),
    CONSTRAINT chk_usage_positive CHECK (usage_value > 0),
    CONSTRAINT chk_billing_month CHECK (billing_month BETWEEN 1 AND 12),
    CONSTRAINT chk_billing_year CHECK (billing_year >= 2000),
    CONSTRAINT chk_utility_type_chg CHECK (utility_type IN ('WATER','ELECTRICITY','GAS','PARKING'))
    );

CREATE INDEX idx_utility_charges_unit_period
    ON utility_charges (unit_id, billing_year, billing_month);

CREATE INDEX idx_utility_charges_type
    ON utility_charges (utility_type);