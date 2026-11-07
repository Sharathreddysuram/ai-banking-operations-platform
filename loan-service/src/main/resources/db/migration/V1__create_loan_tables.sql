CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    agent_name VARCHAR(255),
    action VARCHAR(255),
    customer_id VARCHAR(255),
    input_summary VARCHAR(255),
    reasoning TEXT,
    output_summary VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE loan_application (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(255),
    requested_amount NUMERIC(19,2),
    term_months INTEGER NOT NULL,
    purpose VARCHAR(255),
    monthly_income NUMERIC(19,2),
    monthly_debt_payments NUMERIC(19,2),
    annual_income NUMERIC(19,2),
    credit_score INTEGER NOT NULL,
    employment_status VARCHAR(255),
    status VARCHAR(255),
    created_at TIMESTAMP
);
CREATE TABLE underwriting_assessment (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT,
    dti_ratio DOUBLE PRECISION NOT NULL,
    risk_score DOUBLE PRECISION NOT NULL,
    risk_band VARCHAR(255),
    recommendation VARCHAR(255),
    recommended_amount NUMERIC(19,2),
    reasoning TEXT,
    factors_json TEXT,
    created_at TIMESTAMP
);
CREATE TABLE financial_document (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT,
    document_type VARCHAR(255),
    extracted_data TEXT,
    verified_monthly_income NUMERIC(19,2),
    created_at TIMESTAMP
);
