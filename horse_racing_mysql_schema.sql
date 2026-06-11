-- ============================================================
-- Horse Racing Tournament Management System - MySQL 8 Schema
-- Author: generated for SWP391 project
-- Notes:
--   - UUID is implemented as CHAR(36) DEFAULT (UUID()) for readability.
--   - Money fields use DECIMAL(15,2).
--   - Important percentage / measurement fields use DECIMAL instead of FLOAT.
--   - Enum values are normalized to UPPER_SNAKE_CASE for Java/Spring enum mapping.
-- ============================================================

-- ============================================================
-- 00. CREATE DATABASE + NON-ROOT APP USER
-- Run this section once using a MySQL admin/root account.
-- Your Spring Boot app should connect using horse_app, not root.
-- ============================================================



CREATE DATABASE IF NOT EXISTS SWP391_Project_HRTMS
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'horse_app'@'localhost' IDENTIFIED BY 'horse_app';

GRANT ALL PRIVILEGES
ON SWP391_Project_HRTMS.* TO 'horse_app'@'localhost';

FLUSH PRIVILEGES;

USE SWP391_Project_HRTMS;

