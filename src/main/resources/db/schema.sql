DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    birthday TIMESTAMP
);

DROP TABLE IF EXISTS role;
CREATE TABLE role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(255) NOT NULL
);

DROP TABLE IF EXISTS users_role;
CREATE TABLE users_role (
    users_id BIGINT,
    role_id BIGINT
);