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

CREATE TABLE support_ticket (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(255),
    subject VARCHAR(255),
    question TEXT,
    answer TEXT,
    category VARCHAR(255),
    status VARCHAR(255),
    escalated BOOLEAN NOT NULL DEFAULT FALSE,
    reasoning TEXT,
    created_at TIMESTAMP
);
CREATE TABLE customer_account (
    id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(255),
    account_number VARCHAR(255),
    account_type VARCHAR(255),
    balance NUMERIC(19,2),
    status VARCHAR(255)
);
