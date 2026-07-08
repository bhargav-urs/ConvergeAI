-- ConvergeAI initial schema.
-- Requires the pgvector extension (available on Neon free tier and pgvector/pgvector Docker images).

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename      VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100),
    size_bytes    BIGINT       NOT NULL DEFAULT 0,
    status        VARCHAR(50)  NOT NULL DEFAULT 'PROCESSING',
    chunk_count   INT          NOT NULL DEFAULT 0,
    char_count    BIGINT       NOT NULL DEFAULT 0,
    error_message TEXT,
    uploaded_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE document_chunks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    chunk_index INT  NOT NULL,
    content     TEXT NOT NULL,
    char_count  INT  NOT NULL DEFAULT 0,
    -- all-MiniLM-L6-v2 produces 384-dimensional sentence embeddings
    embedding   vector(384) NOT NULL
);

CREATE INDEX idx_document_chunks_document_id ON document_chunks (document_id);

-- HNSW gives good recall/latency without the tuning ivfflat needs at small scale.
-- MiniLM embeddings are L2-normalised, so cosine distance is the natural metric.
CREATE INDEX idx_document_chunks_embedding
    ON document_chunks USING hnsw (embedding vector_cosine_ops);

CREATE TABLE questions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id        UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    question_text      TEXT NOT NULL,
    status             VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message      TEXT,
    processing_time_ms BIGINT,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at       TIMESTAMPTZ
);

CREATE INDEX idx_questions_document_id ON questions (document_id);
CREATE INDEX idx_questions_created_at ON questions (created_at DESC);

-- Which chunks were retrieved for a question, with their similarity rank.
-- Kept as a first-class table so agent citations ([Chunk n]) stay auditable.
CREATE TABLE question_context (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    chunk_id    UUID NOT NULL REFERENCES document_chunks (id) ON DELETE CASCADE,
    rank        INT  NOT NULL,
    distance    DOUBLE PRECISION NOT NULL
);

CREATE INDEX idx_question_context_question_id ON question_context (question_id);

CREATE TABLE agent_responses (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id       UUID NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    agent_name        VARCHAR(50) NOT NULL,
    model             VARCHAR(120),
    round             INT NOT NULL,
    response          TEXT,
    critique_received TEXT,
    status            VARCHAR(50) NOT NULL DEFAULT 'OK',
    error_message     TEXT,
    latency_ms        BIGINT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_agent_responses_question_id ON agent_responses (question_id);

CREATE TABLE consensus_results (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id         UUID NOT NULL UNIQUE REFERENCES questions (id) ON DELETE CASCADE,
    final_answer        TEXT NOT NULL,
    agreement_points    JSONB,
    disagreement_points JSONB,
    confidence_score    INT NOT NULL,
    model               VARCHAR(120),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
