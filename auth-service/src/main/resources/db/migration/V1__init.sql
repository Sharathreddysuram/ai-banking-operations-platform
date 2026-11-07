CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    full_name VARCHAR(255),
    role VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP
);

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
