CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(100)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255)  NOT NULL,
    name            VARCHAR(50)   NOT NULL,
    role            VARCHAR(20)   NOT NULL DEFAULT 'STUDENT',
    student_id      VARCHAR(20),
    data_collection_consent BOOLEAN NOT NULL DEFAULT FALSE,
    consent_at      TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
