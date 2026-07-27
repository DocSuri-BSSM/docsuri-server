-- ddl-auto: validate 로 전환할 때 사용할 DDL (Document 도메인 3개 테이블)

CREATE TABLE IF NOT EXISTS document_sessions (
    session_id  UUID PRIMARY KEY,
    created_at  TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS documents (
    document_id        UUID PRIMARY KEY,
    session_id         UUID NOT NULL REFERENCES document_sessions (session_id),
    document_type      VARCHAR(30)  NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_url           TEXT         NOT NULL,
    created_at         TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_documents_session ON documents (session_id);

CREATE TABLE IF NOT EXISTS document_parse_jobs (
    parse_job_id         UUID PRIMARY KEY,
    session_id           UUID NOT NULL REFERENCES document_sessions (session_id),
    ocr_language         VARCHAR(50),
    extract_fields       JSONB       NOT NULL,
    status               VARCHAR(30) NOT NULL,
    progress_percent     INT         NOT NULL DEFAULT 0,
    current_step         VARCHAR(50),
    extraction_documents JSONB,
    created_at           TIMESTAMP   NOT NULL,
    completed_at         TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_parse_jobs_session ON document_parse_jobs (session_id, created_at DESC);
