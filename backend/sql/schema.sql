CREATE DATABASE IF NOT EXISTS katalog_film
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE katalog_film;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL,
  password_hash VARCHAR(255) NULL,
  google_sub VARCHAR(255) NULL,
  reset_token_hash CHAR(64) NULL,
  reset_token_expires_at DATETIME NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_email (email),
  UNIQUE KEY uq_users_google_sub (google_sub),
  KEY idx_reset_token_hash (reset_token_hash)
) ENGINE=InnoDB;
