CREATE TABLE IF NOT EXISTS job_schema_version (version INTEGER PRIMARY KEY);
CREATE TABLE IF NOT EXISTS job_mutex (id INTEGER PRIMARY KEY);
MERGE INTO job_mutex (id) KEY(id) VALUES (1);
CREATE TABLE IF NOT EXISTS analysis_job (id VARCHAR(36) PRIMARY KEY, idempotency_key VARCHAR(36) NOT NULL UNIQUE, payload CLOB NOT NULL);
CREATE TABLE IF NOT EXISTS conversation_context (id VARCHAR(36) PRIMARY KEY, session_id VARCHAR(200) NOT NULL, expires_at BIGINT NOT NULL, payload CLOB NOT NULL);
CREATE TABLE IF NOT EXISTS conversation_state (session_id VARCHAR(200) PRIMARY KEY, latest_context_id VARCHAR(36) NOT NULL);
MERGE INTO job_schema_version (version) KEY(version) VALUES (1);
