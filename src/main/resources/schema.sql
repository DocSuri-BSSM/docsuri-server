-- =====================================================================
-- docsuri-server 전체 스키마 (PostgreSQL 16)
-- 엔티티는 이 정의와 정확히 일치해야 한다 (ddl-auto: validate)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 마스터 데이터 (세션과 무관, 사전 적재)
-- ---------------------------------------------------------------------

-- 무역 용어 사전
CREATE TABLE IF NOT EXISTS trade_terms (
    term         VARCHAR(100) PRIMARY KEY,
    full_name    VARCHAR(255) NOT NULL,
    korean_name  VARCHAR(255) NOT NULL,
    description  TEXT         NOT NULL
);

-- 면책 문구. 코드에 상수로 박지 말고 반드시 여기서 조회한다
CREATE TABLE IF NOT EXISTS disclaimers (
    disclaimer_id    BIGSERIAL PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    content          TEXT         NOT NULL,
    display_position VARCHAR(50)  NOT NULL,   -- GLOBAL | VALIDATION | HS_CODE | CORRECTION
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_disclaimers_position
    ON disclaimers (display_position, is_active);

-- 관세율표 마스터. HS Code 추천의 후보 풀이며, LLM은 이 테이블 밖의 코드를 만들어낼 수 없다
CREATE TABLE IF NOT EXISTS hs_codes (
    hs_code             VARCHAR(20) PRIMARY KEY,
    korean_name         VARCHAR(255) NOT NULL,
    english_name        VARCHAR(255),
    description         TEXT,
    tariff_rate         DECIMAL(10, 4),
    import_requirements JSONB,                -- string[]
    updated_at          TIMESTAMP    NOT NULL
);
-- 품명 검색용. pg_trgm 확장이 있으면 유사도 검색 성능이 크게 개선된다
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_hs_codes_korean_name_trgm
    ON hs_codes USING gin (korean_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_hs_codes_english_name_trgm
    ON hs_codes USING gin (english_name gin_trgm_ops);


-- ---------------------------------------------------------------------
-- 2. 세션 & 문서
-- ---------------------------------------------------------------------

-- 회원 개념이 없으므로 이 세션이 모든 작업의 루트다
CREATE TABLE IF NOT EXISTS document_sessions (
    session_id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS documents (
    document_id        UUID PRIMARY KEY,
    session_id         UUID         NOT NULL REFERENCES document_sessions (session_id) ON DELETE CASCADE,
    document_type      VARCHAR(30)  NOT NULL,   -- INVOICE | BILL_OF_LADING | PACKING_LIST
    original_file_name VARCHAR(255) NOT NULL,
    file_url           TEXT         NOT NULL,   -- /files/{sessionId}/{storedName}
    content_type        VARCHAR(100) NOT NULL,
    file_size           BIGINT       NOT NULL,
    created_at          TIMESTAMP    NOT NULL,
    CONSTRAINT uq_documents_session_type UNIQUE (session_id, document_type)
);
CREATE INDEX IF NOT EXISTS idx_documents_session ON documents (session_id);

-- OCR 파싱 작업
CREATE TABLE IF NOT EXISTS document_parse_jobs (
    parse_job_id         UUID PRIMARY KEY,
    session_id           UUID        NOT NULL REFERENCES document_sessions (session_id) ON DELETE CASCADE,
    ocr_language         VARCHAR(50),
    extract_fields       JSONB       NOT NULL, -- { "INVOICE": ["GROSS_WEIGHT", ...], ... }
    status               VARCHAR(30) NOT NULL, -- PENDING | PROCESSING | COMPLETED | FAILED
    progress_percent     INT         NOT NULL DEFAULT 0,
    current_step         VARCHAR(50),          -- QUEUED | PREPROCESSING | OCR | FIELD_EXTRACTION | DONE
    extraction_documents JSONB,                -- ExtractedDocument[] (02-API.md 4절 참조)
    error_message        TEXT,
    created_at           TIMESTAMP   NOT NULL,
    completed_at         TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_parse_jobs_session
    ON document_parse_jobs (session_id, created_at DESC);


-- ---------------------------------------------------------------------
-- 3. 교차 검증
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS validation_runs (
    validation_run_id       UUID PRIMARY KEY,
    session_id              UUID          NOT NULL REFERENCES document_sessions (session_id) ON DELETE CASCADE,
    rules                   JSONB         NOT NULL,  -- ValidationRule[] (문자열 배열)
    weight_tolerance_percent DECIMAL(7, 4) NOT NULL DEFAULT 0.5,
    status                  VARCHAR(30)   NOT NULL,  -- PENDING | PROCESSING | COMPLETED | FAILED
    overall_signal          VARCHAR(20),             -- GREEN | YELLOW | RED
    total_checked           INT           NOT NULL DEFAULT 0,
    normal_count            INT           NOT NULL DEFAULT 0,
    warning_count           INT           NOT NULL DEFAULT 0,
    error_count             INT           NOT NULL DEFAULT 0,
    issues                  JSONB,                   -- ValidationIssue[] (02-API.md 6절 참조)
    disclaimer              TEXT,
    error_message           TEXT,
    created_at              TIMESTAMP     NOT NULL,
    completed_at            TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_validation_runs_session
    ON validation_runs (session_id, created_at DESC);


-- ---------------------------------------------------------------------
-- 4. HS Code
--    원본 ERD에 없던 테이블. 추천/소명 응답이 각각 id를 반환하므로 영속화가 필요하다
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS hs_code_recommendations (
    recommendation_id   UUID PRIMARY KEY,
    session_id          UUID,                        -- 세션 없이도 단독 조회 가능하므로 NULL 허용
    product_name        VARCHAR(255) NOT NULL,
    product_description TEXT,
    origin_country_code VARCHAR(10),
    max_candidates      INT          NOT NULL DEFAULT 3,
    candidates          JSONB        NOT NULL,       -- HsCodeCandidate[]
    created_at          TIMESTAMP    NOT NULL
);

CREATE TABLE IF NOT EXISTS hs_code_justifications (
    justification_id    UUID PRIMARY KEY,
    hs_code             VARCHAR(20)  NOT NULL,
    product_name        VARCHAR(255) NOT NULL,
    product_description TEXT,
    additional_facts    JSONB,                       -- string[]
    title               VARCHAR(255) NOT NULL,
    content              TEXT         NOT NULL,       -- 마크다운 볼드 포함
    legal_basis          JSONB        NOT NULL,       -- string[]
    created_at           TIMESTAMP    NOT NULL
);


-- ---------------------------------------------------------------------
-- 5. 정정 요청서
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS correction_requests (
    correction_request_id UUID        PRIMARY KEY,
    session_id            UUID        NOT NULL REFERENCES document_sessions (session_id) ON DELETE CASCADE,
    validation_run_id     UUID        NOT NULL REFERENCES validation_runs (validation_run_id),
    output_language       VARCHAR(10) NOT NULL,      -- KO | EN
    additional_instruction TEXT,
    title                 VARCHAR(255),
    content               TEXT,
    variables             JSONB,                     -- CorrectionVariable[]
    status                VARCHAR(30) NOT NULL,      -- DRAFT | CONFIRMED | EXPORTED
    created_at            TIMESTAMP   NOT NULL,
    updated_at            TIMESTAMP   NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_correction_requests_session
    ON correction_requests (session_id, created_at DESC);

-- 원본 ERD에 없던 테이블. export 응답이 export_id 와 expires_at 을 반환하므로 필요하다
CREATE TABLE IF NOT EXISTS correction_request_exports (
    export_id             UUID         PRIMARY KEY,
    correction_request_id UUID         NOT NULL REFERENCES correction_requests (correction_request_id) ON DELETE CASCADE,
    format                VARCHAR(10)  NOT NULL,     -- PDF | DOCX
    include_validation_report BOOLEAN  NOT NULL DEFAULT FALSE,
    file_name             VARCHAR(255) NOT NULL,
    file_key              TEXT         NOT NULL,     -- FileStorage 키
    download_url          TEXT         NOT NULL,
    expires_at            TIMESTAMP    NOT NULL,
    created_at            TIMESTAMP    NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_exports_correction
    ON correction_request_exports (correction_request_id, created_at DESC);
