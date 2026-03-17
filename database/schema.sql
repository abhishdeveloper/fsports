-- Enable strict mode and disable foreign key checks temporarily for seamless table creation
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;

-- --------------------------------------------------------
-- Users Table
-- --------------------------------------------------------
CREATE TABLE `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `roll_number` VARCHAR(50) NOT NULL,
  `branch` VARCHAR(100) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL, -- Specifically accommodated for Argon2id
  `total_coins` INT NOT NULL DEFAULT 1000,
  `role` ENUM('admin', 'user') NOT NULL DEFAULT 'user',
  `refresh_token` VARCHAR(255) DEFAULT NULL,
  `account_status` ENUM('active', 'suspended', 'banned') NOT NULL DEFAULT 'active',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`),
  UNIQUE KEY `idx_email` (`email`),
  UNIQUE KEY `idx_roll_number` (`roll_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Sports Table
-- --------------------------------------------------------
CREATE TABLE `sports` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `sport_name` VARCHAR(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_sport_name` (`sport_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Teams Table
-- --------------------------------------------------------
CREATE TABLE `teams` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `team_name` VARCHAR(100) NOT NULL,
  `branch_name` VARCHAR(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_team_branch` (`team_name`, `branch_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Players Table (IPL 2026 Phase 10)
-- --------------------------------------------------------
CREATE TABLE `players` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `team_id` INT UNSIGNED NOT NULL,
  `player_name` VARCHAR(100) NOT NULL,
  `role` ENUM('Batsman', 'Bowler', 'All-rounder', 'Wicketkeeper') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_team_id` (`team_id`),
  CONSTRAINT `fk_player_team` FOREIGN KEY (`team_id`) REFERENCES `teams` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Matches Table
-- --------------------------------------------------------
CREATE TABLE `matches` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `sport_id` INT UNSIGNED NOT NULL,
  `team_a_id` INT UNSIGNED NOT NULL,
  `team_b_id` INT UNSIGNED NOT NULL,
  `start_time` DATETIME NOT NULL,
  `match_status` ENUM('upcoming', 'live', 'completed', 'cancelled') NOT NULL DEFAULT 'upcoming',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sport_id` (`sport_id`),
  KEY `idx_team_a_id` (`team_a_id`),
  KEY `idx_team_b_id` (`team_b_id`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_match_status` (`match_status`),
  CONSTRAINT `fk_match_sport` FOREIGN KEY (`sport_id`) REFERENCES `sports` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_match_team_a` FOREIGN KEY (`team_a_id`) REFERENCES `teams` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_match_team_b` FOREIGN KEY (`team_b_id`) REFERENCES `teams` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_different_teams` CHECK (`team_a_id` != `team_b_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Questions Table
-- --------------------------------------------------------
CREATE TABLE `questions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `match_id` BIGINT UNSIGNED NOT NULL,
  `question_text` TEXT NOT NULL,
  `option_a` VARCHAR(255) NOT NULL,
  `option_b` VARCHAR(255) NOT NULL,
  `option_c` VARCHAR(255) DEFAULT NULL,
  `option_d` VARCHAR(255) DEFAULT NULL,
  `correct_option` ENUM('A', 'B', 'C', 'D') DEFAULT NULL,
  `points_multiplier` DECIMAL(5,2) NOT NULL DEFAULT 1.00,
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_match_id` (`match_id`),
  CONSTRAINT `fk_question_match` FOREIGN KEY (`match_id`) REFERENCES `matches` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Predictions Table
-- --------------------------------------------------------
CREATE TABLE `predictions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `question_id` BIGINT UNSIGNED NOT NULL,
  `selected_option` ENUM('A', 'B', 'C', 'D') NOT NULL,
  `status` ENUM('pending', 'won', 'lost') NOT NULL DEFAULT 'pending',
  `locked_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_question` (`user_id`, `question_id`), -- Prevents double betting
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_prediction_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_prediction_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Security Logs Table
-- --------------------------------------------------------
CREATE TABLE `security_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `ip_address` VARCHAR(45) NOT NULL, -- Sized for IPv6 support
  `endpoint_accessed` VARCHAR(255) NOT NULL,
  `attempt_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` ENUM('success', 'failed') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ip_address` (`ip_address`),
  KEY `idx_attempt_time` (`attempt_time`),
  KEY `idx_ip_status_time` (`ip_address`, `status`, `attempt_time`) -- Optimized index for rate-limiting queries
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

COMMIT;
SET FOREIGN_KEY_CHECKS = 1;
