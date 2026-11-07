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

CREATE TABLE decision_case (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(255),
    case_type VARCHAR(255),
    kyc_summary TEXT,
    fraud_summary TEXT,
    loan_summary TEXT,
    support_summary TEXT,
    final_recommendation VARCHAR(255),
    overall_risk_score DOUBLE PRECISION NOT NULL,
    status VARCHAR(255),
    reviewed_by VARCHAR(255),
    review_notes TEXT,
    reasoning TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
