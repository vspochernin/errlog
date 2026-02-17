CREATE TABLE users
(
    id       BIGSERIAL PRIMARY KEY,
    login    VARCHAR(32)  NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(64)  NOT NULL
);
