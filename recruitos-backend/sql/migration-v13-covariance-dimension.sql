-- Phase 3: job_covariance_matrix 补齐 dimension 列
USE `recruit_os`;

ALTER TABLE `job_covariance_matrix`
  ADD COLUMN `dimension` INT DEFAULT NULL COMMENT '标签维度' AFTER `matrix_data`;
