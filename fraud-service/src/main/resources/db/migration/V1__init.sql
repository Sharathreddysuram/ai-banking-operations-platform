CREATE TABLE bank_transaction (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(10),
    merchant VARCHAR(255),
    category VARCHAR(100),
    location VARCHAR(255),
    channel VARCHAR(50),
    timestamp TIMESTAMP,
    status VARCHAR(50)
);

CREATE INDEX idx_bank_transaction_customer ON bank_transaction(customer_id);
CREATE INDEX idx_bank_transaction_customer_ts ON bank_transaction(customer_id, timestamp);

CREATE TABLE fraud_assessment (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    risk_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    severity VARCHAR(50) NOT NULL,
    reasons_json TEXT,
    recommendation VARCHAR(50) NOT NULL,
    reasoning TEXT,
    created_at TIMESTAMP
);

CREATE INDEX idx_fraud_assessment_customer ON fraud_assessment(customer_id);
CREATE INDEX idx_fraud_assessment_transaction ON fraud_assessment(transaction_id);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    agent_name VARCHAR(255),
    action VARCHAR(255),
    customer_id VARCHAR(255),
    input_summary VARCHAR(255),
    reasoning TEXT,
    output_summary VARCHAR(255),
    created_at TIMESTAMP
);
