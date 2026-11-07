-- =============================================================================
-- AI Banking Operations Platform — PostgreSQL initialisation
--
-- This script runs ONCE, as the `postgres` superuser, the first time the
-- postgres container starts with an empty pgdata volume
-- (mounted at /docker-entrypoint-initdb.d/init.sql).
--
-- It creates:
--   * the shared application user `bank` used by every service
--   * one database per service (database-per-service pattern)
--
-- To re-run it, delete the volume and start again:
--   docker compose down -v && docker compose up --build
-- =============================================================================

-- Application user ------------------------------------------------------------
-- Idempotent guard: CREATE USER has no IF NOT EXISTS form, so use a DO block.
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'bank') THEN
        CREATE USER bank WITH PASSWORD 'bankpass';
    ELSE
        ALTER USER bank WITH PASSWORD 'bankpass';
    END IF;
END
$$;

-- The services own their schemas via JPA/Hibernate (ddl-auto) and need full
-- rights on their own databases. SUPERUSER keeps local development friction-
-- free; in production, drop SUPERUSER and keep only the per-database grants
-- below.
ALTER USER bank WITH SUPERUSER;

-- Databases (one per service) -------------------------------------------------
-- CREATE DATABASE cannot run inside a transaction / DO block, so each
-- statement is issued directly. The postgres image runs init scripts with
-- ON_ERROR_STOP off by default for .sql files piped through psql in some
-- setups and on in others — these databases do not exist on a fresh volume,
-- so plain CREATE DATABASE is correct for the first-boot contract.
CREATE DATABASE auth_db;
CREATE DATABASE kyc_db;
CREATE DATABASE fraud_db;
CREATE DATABASE loan_db;
CREATE DATABASE support_db;
CREATE DATABASE decision_db;

-- Ownership -------------------------------------------------------------------
ALTER DATABASE auth_db OWNER TO bank;
ALTER DATABASE kyc_db OWNER TO bank;
ALTER DATABASE fraud_db OWNER TO bank;
ALTER DATABASE loan_db OWNER TO bank;
ALTER DATABASE support_db OWNER TO bank;
ALTER DATABASE decision_db OWNER TO bank;

-- Grants ----------------------------------------------------------------------
GRANT ALL PRIVILEGES ON DATABASE auth_db TO bank;
GRANT ALL PRIVILEGES ON DATABASE kyc_db TO bank;
GRANT ALL PRIVILEGES ON DATABASE fraud_db TO bank;
GRANT ALL PRIVILEGES ON DATABASE loan_db TO bank;
GRANT ALL PRIVILEGES ON DATABASE support_db TO bank;
GRANT ALL PRIVILEGES ON DATABASE decision_db TO bank;

-- Per-database schema grants --------------------------------------------------
-- \connect switches database so the public schema grant lands in the
-- right place on PostgreSQL 15+ (where public is no longer world-writable).
\connect auth_db
GRANT ALL ON SCHEMA public TO bank;

\connect kyc_db
GRANT ALL ON SCHEMA public TO bank;

\connect fraud_db
GRANT ALL ON SCHEMA public TO bank;

\connect loan_db
GRANT ALL ON SCHEMA public TO bank;

\connect support_db
GRANT ALL ON SCHEMA public TO bank;

\connect decision_db
GRANT ALL ON SCHEMA public TO bank;

-- Return to the default database ---------------------------------------------
\connect postgres
