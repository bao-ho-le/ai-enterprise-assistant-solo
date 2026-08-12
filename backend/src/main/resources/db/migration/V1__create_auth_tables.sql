-- ============================================================
-- V1: Create users and refresh_tokens tables
-- ============================================================

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,

    role VARCHAR(20) NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,

    jti VARCHAR(255) NOT NULL,
    token_hash VARCHAR(512) NOT NULL,

    user_id BIGINT NOT NULL,

    expires_at TIMESTAMP NOT NULL,

    revoked BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

-- ============================================================
-- Indexes
-- ============================================================

CREATE UNIQUE INDEX uk_users_username
    ON users(username);

CREATE UNIQUE INDEX uk_users_email
    ON users(email);

CREATE UNIQUE INDEX uk_refresh_tokens_jti
    ON refresh_tokens(jti);

CREATE UNIQUE INDEX uk_refresh_tokens_token_hash
    ON refresh_tokens(token_hash);

CREATE INDEX idx_refresh_token_jti
    ON refresh_tokens(jti);

CREATE INDEX idx_refresh_token_user_id
    ON refresh_tokens(user_id);