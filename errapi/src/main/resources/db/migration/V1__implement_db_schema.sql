CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,

    login         VARCHAR(64)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    role          VARCHAR(32)  NOT NULL DEFAULT 'NONE',

    CONSTRAINT users_login_uk UNIQUE (login),
    CONSTRAINT users_email_uk UNIQUE (email),
    CONSTRAINT users_role_chk CHECK (role IN ('NONE', 'READER', 'ADMIN', 'OWNER'))
);
