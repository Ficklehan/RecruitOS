-- 上传简历时可能尚未关联候选人
USE `recruit_os`;

ALTER TABLE `resume`
    MODIFY COLUMN `candidate_id` BIGINT NULL COMMENT '关联候选人，上传后可空';
