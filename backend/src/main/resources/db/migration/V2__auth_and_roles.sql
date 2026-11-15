-- M1: Users, Wallets, and Auth Challenges

CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(36) PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS wallets (
    id         VARCHAR(36) PRIMARY KEY,
    user_id    VARCHAR(36) NOT NULL,
    address    VARCHAR(42) NOT NULL,
    linked_at  TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP   NULL,
    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_active_wallet (user_id, address)
);

CREATE TABLE IF NOT EXISTS auth_challenges (
    nonce      VARCHAR(64) PRIMARY KEY,
    user_id    VARCHAR(36) NULL,
    message    VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP   NULL,
    created_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_challenges_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
