-- company_id references the external Company service: no FK on job_offers.
CREATE TABLE company_configs
(
    company_id              UUID PRIMARY KEY,
    approval_required       BOOLEAN NOT NULL DEFAULT FALSE,
    partial_save_enabled    BOOLEAN NOT NULL DEFAULT FALSE,
    manual_posting_required BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE job_offers
(
    id            UUID PRIMARY KEY,
    company_id    UUID                     NOT NULL,
    title         VARCHAR(200),
    location_type VARCHAR(50),
    address_line1 VARCHAR(100),
    address_line2 VARCHAR(100),
    address_line3 VARCHAR(100),
    postal_code   VARCHAR(20),
    city          VARCHAR(100),
    region        VARCHAR(100),
    country_code  VARCHAR(2),
    status        VARCHAR(50)              NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at  TIMESTAMP WITH TIME ZONE,
    version       BIGINT                   NOT NULL DEFAULT 0
);

CREATE INDEX idx_job_offers_company_status_created ON job_offers (company_id, status, created_at DESC);
CREATE INDEX idx_job_offers_status_created ON job_offers (status, created_at DESC);

CREATE TABLE job_offer_salaries
(
    id           BIGSERIAL PRIMARY KEY,
    job_offer_id UUID           NOT NULL,
    type         VARCHAR(20)    NOT NULL,
    amount       NUMERIC(12, 2) NOT NULL,
    currency     VARCHAR(3)     NOT NULL,
    CONSTRAINT fk_salary_job_offer FOREIGN KEY (job_offer_id) REFERENCES job_offers (id) ON DELETE CASCADE
);

CREATE INDEX idx_job_offer_salaries_job_offer_id ON job_offer_salaries (job_offer_id);

CREATE TABLE job_offer_bonuses
(
    id           BIGSERIAL PRIMARY KEY,
    job_offer_id UUID           NOT NULL,
    type         VARCHAR(20)    NOT NULL,
    amount       NUMERIC(12, 2) NOT NULL,
    currency     VARCHAR(3)     NOT NULL,
    CONSTRAINT fk_bonus_job_offer FOREIGN KEY (job_offer_id) REFERENCES job_offers (id) ON DELETE CASCADE
);

CREATE INDEX idx_job_offer_bonuses_job_offer_id ON job_offer_bonuses (job_offer_id);
