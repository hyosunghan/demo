DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    birthday TIMESTAMP,
    status INT
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

DROP TABLE IF EXISTS permission;
CREATE TABLE permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    permission_name VARCHAR(255) NOT NULL,
    url VARCHAR(255) NOT NULL
);

DROP TABLE IF EXISTS role_permission;
CREATE TABLE role_permission (
    role_id BIGINT,
    permission_id BIGINT
);