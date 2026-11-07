CREATE TABLE customer (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    document_type VARCHAR(50) NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    document_holder_name VARCHAR(255),
    address VARCHAR(500),
    date_of_birth VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    risk_rating VARCHAR(50),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE kyc_verification (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    document_format_valid BOOLEAN NOT NULL DEFAULT FALSE,
    name_match_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    document_expired BOOLEAN NOT NULL DEFAULT FALSE,
    sanctions_hit BOOLEAN NOT NULL DEFAULT FALSE,
    verification_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    checks_json TEXT,
    decision VARCHAR(50) NOT NULL,
    reasoning TEXT,
    created_at TIMESTAMP
);

CREATE INDEX idx_kyc_verification_customer ON kyc_verification(customer_id);

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
