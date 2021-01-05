/*
Navicat MySQL Data Transfer

Source Server         : localhost
Source Server Version : 80018
Source Host           : localhost:3306
Source Database       : labelsystem

Target Server Type    : MYSQL
Target Server Version : 80018
File Encoding         : 65001

Date: 2020-01-23 15:39:57
*/
use labelsystem;
SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for alg_warehouse_alginstance
-- ----------------------------
DROP TABLE IF EXISTS `alg_warehouse_alginstance`;
CREATE TABLE `alg_warehouse_alginstance` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `alg_name` varchar(128) NOT NULL,
  `add_time` datetime(6) NOT NULL,
  `alg_type_name` varchar(128) NOT NULL,
  `alg_root_dir` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------
-- Table structure for alg_warehouse_algmodel
-- ----------------------------
DROP TABLE IF EXISTS `alg_warehouse_algmodel`;
CREATE TABLE `alg_warehouse_algmodel` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `conf_path` varchar(255) DEFAULT NULL,
  `model_name` varchar(128) NOT NULL,
  `local_path` varchar(128) DEFAULT NULL,
  `model_url` varchar(128) DEFAULT NULL,
  `alg_instance_id` int(11) NOT NULL,
  `exec_script` varchar(512) DEFAULT NULL,
  `train_script` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `alg_warehouse_algmod_alg_instance_id_d8709646_fk_alg_wareh` (`alg_instance_id`),
  CONSTRAINT `alg_warehouse_algmod_alg_instance_id_d8709646_fk_alg_wareh` FOREIGN KEY (`alg_instance_id`) REFERENCES `alg_warehouse_alginstance` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------
-- Table structure for authtoken_token
-- ----------------------------
DROP TABLE IF EXISTS `authtoken_token`;
CREATE TABLE `authtoken_token` (
  `token` varchar(40) NOT NULL,
  `created` datetime(6) NOT NULL,
  `user_id` int(11) NOT NULL,
  PRIMARY KEY (`token`),
  UNIQUE KEY `user_id` (`user_id`),
  CONSTRAINT `authtoken_token_user_id_35299eff_fk_users_userprofile_id` FOREIGN KEY (`user_id`) REFERENCES `users_userprofile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------
-- Table structure for auth_group
-- ----------------------------
DROP TABLE IF EXISTS `auth_group`;
CREATE TABLE `auth_group` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(80) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------
-- Table structure for auth_group_permissions
-- ----------------------------
DROP TABLE IF EXISTS `auth_group_permissions`;
CREATE TABLE `auth_group_permissions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `group_id` int(11) NOT NULL,
  `permission_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `auth_group_permissions_group_id_permission_id_0cd325b0_uniq` (`group_id`,`permission_id`),
  KEY `auth_group_permissio_permission_id_84c5c92e_fk_auth_perm` (`permission_id`),
  CONSTRAINT `auth_group_permissio_permission_id_84c5c92e_fk_auth_perm` FOREIGN KEY (`permission_id`) REFERENCES `auth_permission` (`id`),
  CONSTRAINT `auth_group_permissions_group_id_b120cbf9_fk_auth_group_id` FOREIGN KEY (`group_id`) REFERENCES `auth_group` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------
-- Table structure for auth_permission
-- ----------------------------
DROP TABLE IF EXISTS `auth_permission`;
CREATE TABLE `auth_permission` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `content_type_id` int(11) NOT NULL,
  `codename` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `auth_permission_content_type_id_codename_01ab375a_uniq` (`content_type_id`,`codename`),
  CONSTRAINT `auth_permission_content_type_id_2f476e4b_fk_django_co` FOREIGN KEY (`content_type_id`) REFERENCES `django_content_type` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=89 DEFAULT CHARSET=utf8mb4 ;


-- ----------------------------
-- Table structure for samples_manager_sampleclass
-- ----------------------------
DROP TABLE IF EXISTS `class_manage`;
CREATE TABLE `class_manage` (
  `id` int(11) NOT NULL,
  `class_name` varchar(200) NOT NULL,
  `super_class_name` varchar(200)  NULL,
  `class_desc` varchar(1000)  NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------
-- Table structure for tasks_labeltask
-- ----------------------------
DROP TABLE IF EXISTS `tasks_labeltask`;
CREATE TABLE `tasks_labeltask` (
  `id` varchar(32) NOT NULL,
  `task_name` varchar(64) NOT NULL,
  `task_add_time` datetime(6) NOT NULL,
  `relate_task_id` varchar(128) DEFAULT NULL,
  `relate_task_name` varchar(400) DEFAULT NULL,
  `finished_picture` int(11) DEFAULT NULL,
  `total_picture` int(11) DEFAULT NULL,
  `task_type` int(11) DEFAULT NULL,
  `zip_object_name` varchar(255) DEFAULT NULL,
  `zip_bucket_name` varchar(255) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `assign_user_id` int(11) DEFAULT NULL,
  `relate_other_label_task` varchar(400) DEFAULT NULL,
  `task_flow_type` int(11) DEFAULT NULL,
  `task_label_type_info` VARCHAR(4000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tasks_labeltask_user_id_97415fea_fk_users_userprofile_id` (`user_id`),
  CONSTRAINT `tasks_labeltask_user_id_97415fea_fk_users_userprofile_id` FOREIGN KEY (`user_id`) REFERENCES `users_userprofile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------
-- Table structure for tasks_labeltaskitem
-- ----------------------------
DROP TABLE IF EXISTS `tasks_labeltaskitem`;
CREATE TABLE `tasks_labeltaskitem` (
  `id` varchar(32) NOT NULL,
  `pic_url` varchar(256) DEFAULT NULL,
  `pic_object_name` varchar(128) DEFAULT NULL,
  `label_info` TEXT(65535) NULL,
  `label_task_id` varchar(32) NOT NULL,
  `item_add_time` datetime(6) NOT NULL,
  `pic_image_field` varchar(400) DEFAULT NULL,
  `label_status` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

ALTER TABLE `labelsystem`.`tasks_labeltaskitem` 
ADD COLUMN `display_order1` INT(11) NULL AFTER `label_status`,
ADD COLUMN `display_order2` INT(11) NULL AFTER `display_order1`;



DROP TABLE IF EXISTS `tasks_labeldcmtaskitem`;
CREATE TABLE `tasks_labeldcmtaskitem` (
  `id` varchar(32) NOT NULL,
  `pic_url` varchar(256) DEFAULT NULL,
  `pic_object_name` varchar(128) DEFAULT NULL,
  `label_info` TEXT(65535) NULL,
  `label_task_id` varchar(32) NOT NULL,
  `item_add_time` datetime(6) NOT NULL,
  `pic_image_field` varchar(400) DEFAULT NULL,
  `label_status` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

ALTER TABLE `labelsystem`.`tasks_labeldcmtaskitem` 
ADD COLUMN `display_order1` INT(11) NULL AFTER `label_status`,
ADD COLUMN `display_order2` INT(11) NULL AFTER `display_order1`;
-- ----------------------------
-- Table structure for tasks_prepredictresult
-- ----------------------------
DROP TABLE IF EXISTS `tasks_prepredictresult`;
CREATE TABLE `tasks_prepredictresult` (
  `id` varchar(32) NOT NULL,
  `pic_url` varchar(256) DEFAULT NULL,
  `pic_object_name` varchar(128) DEFAULT NULL,
  `label_info` TEXT(65535) NULL,
  `item_add_time` datetime(6) NOT NULL,
  `pre_predict_task_id` varchar(32) NOT NULL,
  `pic_image_field` varchar(400) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tasks_prepredictresu_pre_predict_task_id_74edf8fc_fk_tasks_pre` (`pre_predict_task_id`),
  CONSTRAINT `tasks_prepredictresu_pre_predict_task_id_74edf8fc_fk_tasks_pre` FOREIGN KEY (`pre_predict_task_id`) REFERENCES `tasks_prepredicttasks` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------
-- Table structure for tasks_prepredicttasks
-- ----------------------------
DROP TABLE IF EXISTS `tasks_prepredicttasks`;
CREATE TABLE `tasks_prepredicttasks` (
  `id` varchar(32) NOT NULL,
  `task_name` varchar(400) NOT NULL,
  `zip_object_name` varchar(128)  NULL,
  `task_start_time` datetime(6) NOT NULL,
  `task_finish_time` datetime(6) DEFAULT NULL,
  `task_status` int(11) NOT NULL,
  `alg_model_id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `zip_bucket_name` varchar(64)  NULL,
  `task_status_desc` varchar(2000) CHARACTER SET utf8mb4  DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tasks_prepredicttask_alg_model_id_99ab292d_fk_alg_wareh` (`alg_model_id`),
  KEY `tasks_prepredicttasks_user_id_c531c6a7_fk_users_userprofile_id` (`user_id`),
  CONSTRAINT `tasks_prepredicttask_alg_model_id_99ab292d_fk_alg_wareh` FOREIGN KEY (`alg_model_id`) REFERENCES `alg_warehouse_algmodel` (`id`),
  CONSTRAINT `tasks_prepredicttasks_user_id_c531c6a7_fk_users_userprofile_id` FOREIGN KEY (`user_id`) REFERENCES `users_userprofile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

ALTER TABLE `labelsystem`.`tasks_prepredicttasks` 
ADD COLUMN `dataset_id` VARCHAR(64) NULL AFTER `task_status_desc`;


DROP TABLE IF EXISTS `tasks_reidtask`;
CREATE TABLE `tasks_reidtask` (
  `id` varchar(32) NOT NULL,
  `task_name` varchar(400) NOT NULL,
  `src_predict_taskid` varchar(128)  NULL,
  `dest_predict_taskid` varchar(4000)  NULL,
  `task_start_time` datetime(6) NOT NULL,
  `task_finish_time` datetime(6) DEFAULT NULL,
  `task_status` int(11) NOT NULL,
  `alg_model_id` int(11) NULL,
  `user_id` int(11) NOT NULL,
  `src_bucket_name` varchar(64)  NULL,
  `dest_bucket_name` varchar(64)  NULL,
  `task_status_desc` varchar(1000) CHARACTER SET utf8mb4  DEFAULT NULL,
  `assign_user_id` int(11) DEFAULT NULL,
  `relate_other_label_task` varchar(400) DEFAULT NULL,
  `task_flow_type` int(11) DEFAULT NULL,
  `task_type` int(11) DEFAULT NULL,
  `task_label_type_info` VARCHAR(4000) DEFAULT NULL,
  `finished_picture` int(11) DEFAULT NULL,
  `total_picture` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tasks_reidtask_user_id` (`user_id`),
  CONSTRAINT `tasks_reidtask_user_id` FOREIGN KEY (`user_id`) REFERENCES `users_userprofile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;
ALTER TABLE `labelsystem`.`tasks_reidtask` 
ADD COLUMN `reid_obj_type` INT(11) NULL DEFAULT NULL AFTER `total_picture`;


DROP TABLE IF EXISTS `tasks_reidtaskitem`;
CREATE TABLE `tasks_reidtaskitem` (
  `id` varchar(32) NOT NULL,
  `pic_url` varchar(256) DEFAULT NULL,
  `pic_object_name` varchar(128) DEFAULT NULL,
  `label_info` TEXT(65535) NULL,
  `label_task_id` varchar(32) NOT NULL,
  `item_add_time` datetime(6) NOT NULL,
  `pic_image_field` varchar(400) DEFAULT NULL,
  `label_status` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;
ALTER TABLE `labelsystem`.`tasks_reidtaskitem` 
ADD COLUMN `display_order1` INT(11) NULL AFTER `label_status`,
ADD COLUMN `display_order2` INT(11) NULL AFTER `display_order1`;



DROP TABLE IF EXISTS `tasks_reidtask_show_result`;
CREATE TABLE `tasks_reidtask_show_result` (
  `label_task_id` varchar(32)  NOT NULL,
  `reid_name` varchar(500) NOT NULL,
  `related_info` TEXT(65535) NULL,
  PRIMARY KEY (`label_task_id`,`reid_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;


DROP TABLE IF EXISTS `tasks_reidtask_result`;
CREATE TABLE `tasks_reidtask_result` (
  `id` varchar(64) NOT NULL,
  `src_image_info` varchar(500) NOT NULL,
  `label_task_id` varchar(32)  NOT NULL,
  `label_task_name` varchar(400)  NULL,
  `related_info` TEXT(65535) NULL,
  PRIMARY KEY (`id`, `src_image_info`, `label_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;


-- ----------------------------
-- Table structure for tasks_retrainresult
-- ----------------------------
DROP TABLE IF EXISTS `tasks_retrainresult`;
CREATE TABLE `tasks_retrainresult` (
  `id` varchar(32) NOT NULL,
  `loss_train` varchar(128) DEFAULT NULL,
  `lr` varchar(128) DEFAULT NULL,
  `epoch_num` varchar(128) DEFAULT NULL,
  `epoch_total` varchar(128) DEFAULT NULL,
  `step_num` varchar(128) DEFAULT NULL,
  `step_total` varchar(128) DEFAULT NULL,
  `learning_rate` varchar(128) DEFAULT NULL,
  `accuracy_rate_train` varchar(128) DEFAULT NULL,
  `item_add_time` datetime(6) DEFAULT NULL,
  `item_cur_time` datetime(6) DEFAULT NULL,
  `alg_model_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `tasks_retrainresult_alg_model_id_906f1909_fk_alg_wareh` (`alg_model_id`),
  CONSTRAINT `tasks_retrainresult_alg_model_id_906f1909_fk_alg_wareh` FOREIGN KEY (`alg_model_id`) REFERENCES `alg_warehouse_algmodel` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------
-- Table structure for tasks_retraintasks
-- ----------------------------
DROP TABLE IF EXISTS `tasks_retraintasks`;
CREATE TABLE `tasks_retraintasks` (
  `id` varchar(32) NOT NULL,
  `task_name` varchar(400) NOT NULL,
  `task_start_time` datetime(6) DEFAULT NULL,
  `task_finish_time` datetime(6) DEFAULT NULL,
  `task_status` int(11) NOT NULL,
  `task_status_desc` varchar(2000) DEFAULT NULL,
  `alg_model_id` int(11) NOT NULL,
  `pre_predict_task_id` varchar(32) NOT NULL,
  `user_id` int(11) NOT NULL,
  `pid` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `tasks_retraintasks_alg_model_id_e097d239_fk_alg_wareh` (`alg_model_id`),
  KEY `tasks_retraintasks_pre_predict_task_id_650daa6d_fk_tasks_pre` (`pre_predict_task_id`),
  KEY `tasks_retraintasks_user_id_2fde571d_fk_users_userprofile_id` (`user_id`),
  CONSTRAINT `tasks_retraintasks_alg_model_id_e097d239_fk_alg_wareh` FOREIGN KEY (`alg_model_id`) REFERENCES `alg_warehouse_algmodel` (`id`),
  CONSTRAINT `tasks_retraintasks_pre_predict_task_id_650daa6d_fk_tasks_pre` FOREIGN KEY (`pre_predict_task_id`) REFERENCES `tasks_prepredicttasks` (`id`),
  CONSTRAINT `tasks_retraintasks_user_id_2fde571d_fk_users_userprofile_id` FOREIGN KEY (`user_id`) REFERENCES `users_userprofile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------
-- Table structure for users_userprofile
-- ----------------------------
DROP TABLE IF EXISTS `users_userprofile`;
CREATE TABLE `users_userprofile` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `password` varchar(128) NOT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `is_superuser` tinyint(1) NOT NULL,
  `username` varchar(150) NOT NULL,
  `first_name` varchar(30) CHARACTER SET utf8mb4  DEFAULT NULL,
  `last_name` varchar(30) CHARACTER SET utf8mb4  DEFAULT NULL,
  `email` varchar(254) NOT NULL,
  `is_staff` tinyint(1) NOT NULL,
  `is_active` tinyint(1) NOT NULL,
  `date_joined` datetime(6) NOT NULL,
  `nick_name` varchar(50) DEFAULT NULL,
  `address` varchar(100) DEFAULT NULL,
  `mobile` varchar(100) DEFAULT NULL,
  `company` varchar(128) DEFAULT NULL,
  `parent_invite_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `mobile` (`mobile`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 ;



DROP TABLE IF EXISTS `tasks_dataset`;
CREATE TABLE `tasks_dataset` (
  `id` varchar(32) NOT NULL,
  `task_name` varchar(400) NOT NULL,
  `task_desc` varchar(1000) DEFAULT NULL,
  `task_add_time` datetime(6) NOT NULL,
  `dataset_type` int(11) DEFAULT NULL,
  `total` int(11) DEFAULT NULL,
  `zip_object_name` varchar(255) DEFAULT NULL,
  `zip_bucket_name` varchar(255) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `assign_user_id` int(11) DEFAULT NULL,
  `task_status` int(11) DEFAULT NULL,
  `file_bucket_name` VARCHAR(255) NULL,
  `camera_number` VARCHAR(256) NULL,
  `camera_gps` VARCHAR(64) NULL,
  `camera_date` VARCHAR(64) NULL,
  PRIMARY KEY (`id`),
  KEY `tasks_dataset_user_id_97415fea_fk_users_userprofile_id` (`user_id`),
  CONSTRAINT `tasks_dataset_user_id_97415fea_fk_users_userprofile_id` FOREIGN KEY (`user_id`) REFERENCES `users_userprofile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



DROP TABLE IF EXISTS `tasks_videocounttask`;
CREATE TABLE `tasks_videocounttask` (
  `id` varchar(32) NOT NULL,
  `task_name` varchar(400) NOT NULL,
  `dataset_id` varchar(128)  NULL,
  `task_add_time` datetime(6) NOT NULL,
  `task_finish_time` datetime(6) DEFAULT NULL,
  `task_status` int(11) NOT NULL,
  `zip_object_name` varchar(255) DEFAULT NULL,
  `zip_bucket_name` varchar(255) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `task_status_desc` varchar(1000) CHARACTER SET utf8mb4  DEFAULT NULL,
  `assign_user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tasks_videocounttask_user_id` (`user_id`),
  CONSTRAINT `tasks_videocounttask_user_id` FOREIGN KEY (`user_id`) REFERENCES `users_userprofile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

DROP TABLE IF EXISTS `tasks_videocounttaskitem`;
CREATE TABLE `tasks_videocounttaskitem` (
  `id` varchar(32) NOT NULL,
  `pic_url` varchar(256) DEFAULT NULL,
  `pic_object_name` varchar(128) DEFAULT NULL,
  `label_info` TEXT(65535) NULL,
  `label_task_id` varchar(32) NOT NULL,
  `item_add_time` datetime(6) NOT NULL,
  `pic_image_field` varchar(400) DEFAULT NULL,
  `label_status` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

ALTER TABLE `labelsystem`.`tasks_videocounttaskitem` 
ADD COLUMN `display_order1` INT(11) NULL AFTER `label_status`,
ADD COLUMN `display_order2` INT(11) NULL AFTER `display_order1`;



DROP TABLE IF EXISTS `log_info`;
CREATE TABLE `log_info` (
  `id` varchar(32) NOT NULL,
  `oper_type` int(11) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `oper_name` varchar(200) NULL,
  `oper_id` varchar(200) NULL,
  `oper_json_content_old` TEXT(65535) NULL,
  `oper_json_content_new` TEXT(65535) NULL,
  `oper_time_start` datetime(6) NOT NULL,
  `oper_time_end` datetime(6) NOT NULL,
  `record_id` varchar(64) NULL,
  `extend1` varchar(400) NULL,
  `extend2` varchar(400) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;


DROP TABLE IF EXISTS `report_label_task`;
CREATE TABLE `report_label_task` (
  `user_id` int(11) NOT NULL,
  `oper_time` datetime(6) NOT NULL,
  `rectUpdate` int(11)  NULL,
  `rectAdd` int(11)  NULL,
  `properties`  int(11)  NULL,
  `pictureUpdate`  int(11)  NULL,
  PRIMARY KEY (`user_id`,`oper_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;




-- -------------------------------------------
-- 0424---------
-- -------------------------------------------
ALTER TABLE `labelsystem`.`tasks_prepredicttasks` 
ADD COLUMN `delete_no_label_picture` TINYINT NULL DEFAULT NULL AFTER `dataset_id`;



-- --------------------------------------------
-- 0427---------
-- --------------------------------------------

ALTER TABLE `labelsystem`.`tasks_dataset` 
ADD COLUMN `videoSet` VARCHAR(4000) NULL AFTER `camera_date`;



DROP TABLE IF EXISTS `tasks_dataset_videoinfo`;
CREATE TABLE `tasks_dataset_videoinfo` (
  `id` varchar(32) NOT NULL,
  `dataset_id` varchar(400) NOT NULL,
  `minio_url` varchar(1000) DEFAULT NULL,
  `video_info` MediumText DEFAULT NULL,
  `camera_number` VARCHAR(256) NULL,
  `camera_gps` VARCHAR(64) NULL,
  `camera_date` VARCHAR(64) NULL,
  `duration` VARCHAR(64) NULL,
  `bitrate` VARCHAR(64) NULL,
  `startTime` VARCHAR(64) NULL,
  `videoCode` VARCHAR(400) NULL,
  `videoFormat` VARCHAR(400) NULL,
  `resolutionRatio` VARCHAR(64) NULL,
  `audioCode` VARCHAR(64) NULL,
  `audioFrequncy` VARCHAR(64) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------
-- 0429---------
-- --------------------------------------------

ALTER TABLE `labelsystem`.`tasks_dataset_videoinfo` 
ADD COLUMN `fps` VARCHAR(45) NULL DEFAULT NULL AFTER `audioFrequncy`;

ALTER TABLE `labelsystem`.`tasks_dataset` 
ADD COLUMN `mainVideoInfo` VARCHAR(2000) NULL DEFAULT NULL AFTER `videoSet`;



-- ---------------------------------------------
-- 0430---------
-- ---------------------------------------------
DROP TABLE IF EXISTS `tasks_videolabeltask`;
CREATE TABLE `tasks_videolabeltask` (
  `id` varchar(32) NOT NULL,
  `task_name` varchar(400) NOT NULL,
  `dataset_id` varchar(128)  NULL,
  `task_add_time` datetime(6) NOT NULL,
  `task_finish_time` datetime(6) DEFAULT NULL,
  `task_status` int(11) NOT NULL,
  `zip_object_name` varchar(255) DEFAULT NULL,
  `zip_bucket_name` varchar(255) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `task_status_desc` varchar(1000) CHARACTER SET utf8mb4  DEFAULT NULL,
  `assign_user_id` int(11) DEFAULT NULL,
  `mainVideoInfo` VARCHAR(2000) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tasks_videolabeltask_user_id` (`user_id`),
  CONSTRAINT `tasks_videolabeltask_user_id` FOREIGN KEY (`user_id`) REFERENCES `users_userprofile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

DROP TABLE IF EXISTS `tasks_videolabeltaskitem`;
CREATE TABLE `tasks_videolabeltaskitem` (
  `id` varchar(32) NOT NULL,
  `pic_url` varchar(256) DEFAULT NULL,
  `pic_object_name` varchar(128) DEFAULT NULL,
  `label_info` TEXT(65535) NULL,
  `label_task_id` varchar(32) NOT NULL,
  `item_add_time` datetime(6) NOT NULL,
  `pic_image_field` varchar(400) DEFAULT NULL,
  `label_status` int(11) NOT NULL,
  `display_order1` INT(11) NULL,
  `display_order2` INT(11) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;


-- -----------------------------------------
-- 0508
-- -----------------------------------------
ALTER TABLE `labelsystem`.`tasks_videolabeltask` 
ADD COLUMN `task_label_type_info` VARCHAR(4000) NULL DEFAULT NULL AFTER `mainVideoInfo`;



-- ---------------------------
-- 0616
-- ---------------------------
ALTER TABLE `labelsystem`.`tasks_retraintasks` 
ADD COLUMN `confPath` VARCHAR(200) NULL DEFAULT NULL AFTER `pid`,
ADD COLUMN `modelPath` VARCHAR(200) NULL DEFAULT 'NULl' AFTER `confPath`;

-- ---------------------------
-- 0617
-- ---------------------------
ALTER TABLE `labelsystem`.`alg_warehouse_algmodel` 
ADD COLUMN `type_list` VARCHAR(2000) NULL DEFAULT NULL AFTER `train_script`;



-- ---------------------------
-- 0618
-- ---------------------------


ALTER TABLE `labelsystem`.`alg_warehouse_algmodel` 
ADD COLUMN `threshold` DOUBLE NULL DEFAULT NULL AFTER `type_list`;


-- --------------------------
-- 0628
-- --------------------------
ALTER TABLE `labelsystem`.`tasks_labeltask` 
ADD COLUMN `verify_user_id` INT(11) NULL DEFAULT NULL AFTER `task_flow_type`,
ADD COLUMN `task_status` INT(11) NULL DEFAULT NULL AFTER `verify_user_id`,
ADD COLUMN `task_status_desc` VARCHAR(400) NULL DEFAULT NULL AFTER `task_status`;

ALTER TABLE `labelsystem`.`tasks_labeldcmtaskitem` 
ADD COLUMN `verify_status` INT(11) NULL DEFAULT NULL AFTER `display_order2`,
ADD COLUMN `verify_desc` VARCHAR(400) NULL DEFAULT NULL AFTER `verify_status`;


ALTER TABLE `labelsystem`.`tasks_labeltaskitem` 
ADD COLUMN `verify_status` INT(11) NULL DEFAULT NULL AFTER `display_order2`,
ADD COLUMN `verify_desc` VARCHAR(400) NULL DEFAULT NULL AFTER `verify_status`;

ALTER TABLE `labelsystem`.`tasks_reidtask` 
ADD COLUMN `verify_user_id` INT(11) NULL DEFAULT NULL AFTER `reid_obj_type`;

ALTER TABLE `labelsystem`.`tasks_reidtaskitem` 
ADD COLUMN `verify_status` INT(11) NULL DEFAULT NULL AFTER `display_order2`,
ADD COLUMN `verify_desc` VARCHAR(400) NULL DEFAULT NULL AFTER `verify_status`;


ALTER TABLE `labelsystem`.`tasks_videolabeltask` 
ADD COLUMN `verify_user_id` INT(11) NULL DEFAULT NULL AFTER `task_label_type_info`;

ALTER TABLE `labelsystem`.`tasks_videolabeltaskitem` 
ADD COLUMN `verify_status` INT(11) NULL DEFAULT NULL AFTER `display_order2`,
ADD COLUMN `verify_desc` VARCHAR(400) NULL DEFAULT NULL AFTER `verify_status`;

ALTER TABLE `labelsystem`.`tasks_videocounttask` 
ADD COLUMN `verify_user_id` INT(11) NULL DEFAULT NULL AFTER `assign_user_id`;

ALTER TABLE `labelsystem`.`tasks_videocounttaskitem` 
ADD COLUMN `verify_status` INT(11) NULL DEFAULT NULL AFTER `display_order2`,
ADD COLUMN `verify_desc` VARCHAR(400) NULL DEFAULT NULL AFTER `verify_status`;

-- -------------------------
-- 0630
-- -------------------------
ALTER TABLE `labelsystem`.`report_label_task` 
ADD COLUMN `notValide` INT(11) NULL DEFAULT NULL AFTER `pictureUpdate`;


-- --------------------------
-- 0715
-- --------------------------
DROP TABLE IF EXISTS `log_info_history`;
CREATE TABLE `log_info_history` (
  `id` varchar(32) NOT NULL,
  `oper_type` int(11) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `oper_name` varchar(200) NULL,
  `oper_id` varchar(200) NULL,
  `oper_json_content_old` TEXT(65535) NULL,
  `oper_json_content_new` TEXT(65535) NULL,
  `oper_time_start` datetime(6) NOT NULL,
  `oper_time_end` datetime(6) NOT NULL,
  `record_id` varchar(64) NULL,
  `extend1` varchar(400) NULL,
  `extend2` varchar(400) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;


-- --------------------------------
-- 0720
-- --------------------------------
ALTER TABLE `labelsystem`.`tasks_retraintasks` 
DROP FOREIGN KEY `tasks_retraintasks_pre_predict_task_id_650daa6d_fk_tasks_pre`;
ALTER TABLE `labelsystem`.`tasks_retraintasks` 
DROP INDEX `tasks_retraintasks_pre_predict_task_id_650daa6d_fk_tasks_pre` ;


ALTER TABLE `labelsystem`.`tasks_retraintasks` 
CHANGE COLUMN `pre_predict_task_id` `pre_predict_task_id` VARCHAR(32) NULL ,
CHANGE COLUMN `pid` `pid` INT(11) NULL ;



INSERT INTO alg_warehouse_alginstance(id, alg_name, add_time, alg_type_name, alg_root_dir) VALUES ('4', 'FreeAnchor', '2020-01-08 17:44:24.000000', '目标检测', '/mmdetection/');
INSERT INTO alg_warehouse_alginstance(id, alg_name, add_time, alg_type_name, alg_root_dir) values('6', 'MEB-Net(ReID)', '2020-03-28 17:44:24.000000', '行人再识别', '/MEB-Net/');
INSERT INTO alg_warehouse_alginstance(id, alg_name, add_time, alg_type_name, alg_root_dir) VALUES ('5', 'Retinanet', '2020-01-08 17:44:24.000000', '目标检测', '/mmdetection/');
INSERT INTO alg_warehouse_alginstance(`id`, `alg_name`, `add_time`, `alg_type_name`) VALUES ('7', 'WiseMedical', '2020-05-14 17:44:24.000000', '智慧医疗');
INSERT INTO alg_warehouse_alginstance(`id`, `alg_name`, `add_time`, `alg_type_name`, `alg_root_dir`) VALUES ('8', 'Face Detection', '2020-05-19 17:44:24.000000', '人脸检测', '/A-Light-and-Fast-Face-Detector-for-Edge-Devices/');
INSERT INTO alg_warehouse_alginstance(`id`, `alg_name`, `add_time`, `alg_type_name`, `alg_root_dir`) VALUES ('9', 'Tracking', '2020-05-25 17:44:24.000000', '目标跟踪', '/vision/SiamR-CNN/');
INSERT INTO alg_warehouse_alginstance(`id`, `alg_name`, `add_time`, `alg_type_name`, `alg_root_dir`) VALUES ('10', 'Tracking_pyECO', '2020-08-19 17:44:24.000000', '目标跟踪', '/pyECO/');
INSERT INTO alg_warehouse_alginstance(`id`, `alg_name`, `add_time`, `alg_type_name`, `alg_root_dir`) VALUES ('11', 'FastReID', '2020-08-19 17:44:24.000000', '目标重识别', '/fastreid/');
INSERT INTO alg_warehouse_alginstance(`id`, `alg_name`, `add_time`, `alg_type_name`, `alg_root_dir`) VALUES ('12', 'YOlOV3', '2020-06-18 17:44:24.000000', '目标检测', '/detection_tool/');



INSERT INTO alg_warehouse_algmodel (id, conf_path, model_name, local_path, model_url, alg_instance_id, exec_script, train_script, type_list, threshold) VALUES ('4', 'configs/free_anchor/retinanet_free_anchor_r50_fpn_1x.py', 'FreeAnchor', '', 'model/retinanet_free_anchor_r50_fpn_1x/epoch_12.pth', '4', 'python3 demoForJava.py --cfg {configPath} --checkpoint {modelPath}', 'python3  tools/train.py {configPath}',NULL,NULL);

INSERT INTO alg_warehouse_algmodel(id, conf_path, model_name, local_path, model_url, alg_instance_id, exec_script, train_script, type_list, threshold) VALUES ('5', 'configs/retinanet/retinanet_x101_64x4d_fpn_1x.py', 'Retinanet(person+car)', '', 'model/retinanet_x101_64x4d_fpn_1x/epoch_20.pth', '5', 'python3 demoForJava.py --cfg {configPath} --checkpoint {modelPath}', 'python3  tools/train.py {configPath}',NULL,NULL);
INSERT INTO alg_warehouse_algmodel(id, conf_path, model_name, local_path, model_url, alg_instance_id, exec_script, train_script, type_list, threshold) VALUES ('6', 'configs/retinanet/retinanet_x101_64x4d_fpn_1x_car.py', 'Retinanet(car)', NULL, 'model/retinanet_x101_64x4d_fpn_1x/epoch_car_17.pth', '5', 'python3 demoForJava.py --cfg {configPath} --checkpoint {modelPath}', 'python3  tools/train.py {configPath}',NULL,NULL);
INSERT INTO alg_warehouse_algmodel(id, conf_path, model_name, local_path, model_url, alg_instance_id, exec_script, train_script, type_list, threshold) VALUES ('7', 'configs/retinanet/retinanet_x101_64x4d_fpn_1x_car.py', 'Retinanet(all car)', NULL, 'model/retinanet_x101_64x4d_fpn_1x/epoch_9_car_new.pth', '5', 'python3 demoForJava.py --cfg {configPath} --checkpoint {modelPath}', 'python3  tools/train.py {configPath}',NULL,NULL);

insert into alg_warehouse_algmodel(id, conf_path, model_name, local_path, model_url, alg_instance_id, exec_script, train_script, type_list, threshold) values('8', NULL, 'ReID(person)', NULL, NULL, '6', 'python3 main/model_test.py -b 256 -j 8  --dataset-target market1501 -a resnet50 --resume /MEB-Net/resnet50_best_person.pth.tar --data-dir {data_dir}', NULL, '[\"person\"]', NULL);

insert into alg_warehouse_algmodel(id, conf_path, model_name, local_path, model_url, alg_instance_id, exec_script, train_script, type_list, threshold) values('9', NULL, 'ReID(car)', NULL, NULL, '6', 'python3 main/model_test.py -b 256 -j 8  --dataset-target market1501 -a resnet50 --resume /MEB-Net/model_best_car.pth.tar --data-dir {data_dir}', NULL, '[\"car\"]', NULL);

INSERT INTO `alg_warehouse_algmodel` (`id`, `model_name`, `alg_instance_id`) VALUES ('10', 'WiseMedical(Cell)', '7');
INSERT INTO `alg_warehouse_algmodel` (`id`, `model_name`, `alg_instance_id`, `exec_script`) VALUES ('11', 'Face Detection', '8', 'python3 face_detection/demo/demoForJava.py');
INSERT INTO `alg_warehouse_algmodel` (`id`, `model_name`, `alg_instance_id`, `exec_script`) VALUES ('12', 'Tracking', '9', 'python3 tracking/do_tracking.py --main main_custom --custom_dataset_root_dir {data_dir} --custom_dataset_name {data_name}');

INSERT INTO `alg_warehouse_algmodel` (`id`, `model_name`, `alg_instance_id`, `exec_script`) VALUES ('16', 'Tracking_pyECO', '10', 'python3 bin/demo_ECO_hc.py --video_dir  {data_dir} --custom_dataset_name  {data_name}');
INSERT INTO `alg_warehouse_algmodel` (`id`, `model_name`, `alg_instance_id`, `exec_script`, `type_list`) VALUES ('17', 'FastReID(Person)', '11', 'python3 tools/test_net.py --config-file ./configs/Person/sbs_R101-ibn-test.yml --datadir  {data_dir} --json_path  {data_dir}/test.json', '[\"person\"]');
INSERT INTO alg_warehouse_algmodel (`id`, `conf_path`, `model_name`, `alg_instance_id`, `exec_script`, `type_list`) VALUES ('18', '', 'YOLOV3', '12', 'python3 processYoLov3DemoForJava.py', '[\"person\"]');


INSERT INTO `users_userprofile`(id,password,last_login,is_superuser,username,first_name,last_name,email,is_staff,is_active,date_joined,nick_name,address,mobile,company,parent_invite_code) VALUES ('5', '�R�\'�~2~�f#Vd`8�7V����;}�t\\h�', NULL, '0', 'LabelSystem01', null, null, 'zouap@pcl.com.cn', '0', '0', '2019-12-24 16:02:52.000000', 'admin', '鹏城实验室', '1235698755', '实验室', null);



UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `type_list` = '[\"car\",\"person\"]' WHERE (`id` = '5');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `type_list` = '[\"person\"]' WHERE (`id` = '3');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `type_list` = '[\"person\"]' WHERE (`id` = '4');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `type_list` = '[\"car\"]' WHERE (`id` = '6');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `type_list` = '[\"car\"]' WHERE (`id` = '7');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `type_list` = '[\"car\"]' WHERE (`id` = '9');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `type_list` = '[\"person\"]' WHERE (`id` = '8');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `type_list` = '[\"person\"]' WHERE (`id` = '13');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `type_list` = '[\"cell\"]' WHERE (`id` = '10');



-- -------------------------------
-- 0721
-- --------------------------------
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `train_script` = './tools/dist_train.sh {configPath} {gpunum}  --validate' WHERE (`id` = '3');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `train_script` = './tools/dist_train.sh {configPath} {gpunum}  --validate' WHERE (`id` = '4');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `train_script` = './tools/dist_train.sh {configPath} {gpunum}  --validate' WHERE (`id` = '5');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `train_script` = './tools/dist_train.sh {configPath} {gpunum}  --validate' WHERE (`id` = '6');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `train_script` = './tools/dist_train.sh {configPath} {gpunum}  --validate' WHERE (`id` = '7');

-- -------------------------------
-- 0730
-- -------------------------------
ALTER TABLE `labelsystem`.`tasks_videocounttask` 
ADD COLUMN `mainVideoInfo` VARCHAR(2000) NULL DEFAULT NULL AFTER `verify_user_id`;

-- -------------------------------
-- 0803
-- -------------------------------
ALTER TABLE `labelsystem`.`tasks_prepredicttasks` 
ADD COLUMN `score_threshold` DOUBLE NULL DEFAULT NULL AFTER `delete_no_label_picture`;


-- --------------------------------
-- 0810
-- --------------------------------
DROP TABLE IF EXISTS `tasks_largepicturelabeltask`;
CREATE TABLE `tasks_largepicturelabeltask` (
  `id` varchar(32) NOT NULL,
  `task_name` varchar(400) NOT NULL,
  `dataset_id` varchar(128)  NULL,
  `task_add_time` datetime(6) NOT NULL,
  `task_finish_time` datetime(6) DEFAULT NULL,
  `task_status` int(11) NOT NULL,
  `zip_object_name` varchar(255) DEFAULT NULL,
  `zip_bucket_name` varchar(255) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `task_status_desc` varchar(1000) CHARACTER SET utf8mb4  DEFAULT NULL,
  `assign_user_id` int(11) DEFAULT NULL,
  `verify_user_id` INT(11) NULL,
  `mainVideoInfo` VARCHAR(2000) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `tasks_largepicturelabeltask_user_id` (`user_id`),
  CONSTRAINT `tasks_largepicturelabeltask_user_id` FOREIGN KEY (`user_id`) REFERENCES `users_userprofile` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

DROP TABLE IF EXISTS `tasks_largepicturetaskitem`;
CREATE TABLE `tasks_largepicturetaskitem` (
  `id` varchar(32) NOT NULL,
  `pic_url` varchar(256) DEFAULT NULL,
  `pic_object_name` varchar(128) DEFAULT NULL,
  `label_info` MediumText NULL,
  `label_task_id` varchar(32) NOT NULL,
  `item_add_time` datetime(6) NOT NULL,
  `pic_image_field` varchar(400) DEFAULT NULL,
  `label_status` int(11) NOT NULL,
  `verify_status` INT(11) NULL,
  `verify_desc` VARCHAR(400) NULL,
  `display_order1` INT(11) NULL,
  `display_order2` INT(11) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- --------------------------------
-- 0817
-- --------------------------------

ALTER TABLE `labelsystem`.`tasks_retraintasks` 
ADD COLUMN `retrain_type` VARCHAR(45) NULL DEFAULT NULL AFTER `modelPath`,
ADD COLUMN `retrain_data` VARCHAR(4000) NULL AFTER `retrain_type`,
ADD COLUMN `detection_type` VARCHAR(45) NULL AFTER `retrain_data`,
ADD COLUMN `detection_type_input` VARCHAR(45) NULL AFTER `detection_type`,
ADD COLUMN `retrain_model_name` VARCHAR(128) NULL AFTER `detection_type_input`,
ADD COLUMN `testTrainRatio` DOUBLE NULL AFTER `retrain_model_name`,
CHANGE COLUMN `modelPath` `modelPath` VARCHAR(200) NULL DEFAULT NULL ;



-- --------------------------------
-- 0902
-- --------------------------------
-- add progress table
DROP TABLE IF EXISTS `tasks_progress`;
CREATE TABLE `tasks_progress` (
  `id` varchar(500) NOT NULL,
  `taskId` varchar(256) DEFAULT NULL,
  `progress` INT(11) NULL,
  `status` INT(11) NULL,
  `startTime` INT(11) NULL,
  `totalTime` INT(11) NULL,
  `exceedTime`  INT(11) NULL,
  `relatedFileName` varchar(2000) DEFAULT NULL,
  `info` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ----------------------------------
-- 0904
-- ----------------------------------
ALTER TABLE `labelsystem`.`authtoken_token` 
ADD COLUMN `loginTime` BIGINT(20) NULL AFTER `user_id`;


-- ---------------------------------
-- 0907  add secure log info.
-- ---------------------------------
DROP TABLE IF EXISTS `log_sec_info`;
CREATE TABLE `log_sec_info` (
  `id` varchar(32) NOT NULL,
  `oper_type` int(11) DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `oper_name` varchar(200) NULL,
  `oper_id` varchar(200) NULL,
  `log_info` varchar(2000) NULL,
  `oper_time_start` datetime(6) NOT NULL,
  `oper_time_end` datetime(6) NOT NULL,
  `record_id` varchar(64) NULL,
  `extend1` varchar(400) NULL,
  `extend2` varchar(400) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ---------------------------------
-- 0910  add login error info.
-- ---------------------------------
DROP TABLE IF EXISTS `login_info`;
CREATE TABLE `login_info` (
  `user_id` int(11) NOT NULL,
  `login_error_time` int(11) NULL,
  `last_login_time` datetime(6) NOT NULL,
  `extend1` varchar(400) NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;

-- ---------------------------------------
-- 0922
-- --------------------------------------
INSERT INTO `labelsystem`.`alg_warehouse_alginstance` (`id`, `alg_name`, `add_time`, `alg_type_name`, `alg_root_dir`) VALUES ('13', 'Distinguish', '2020-09-22 17:44:24.000000', '车辆识别', '/mmdetection/');
INSERT INTO `labelsystem`.`alg_warehouse_algmodel` (`id`, `conf_path`, `model_name`, `model_url`, `alg_instance_id`, `exec_script`, `train_script`, `type_list`) VALUES ('19', 'configs/retinanet/faster_rcnn_r50_caffe_c4_1x.py', 'Car Distinguish', 'model/faster_rcnn_r50_caffe_c4_1x/epoch_48.pth', '13', 'python3 tools/pred_car_type.py --cfg {configPath} --checkpoint {modelPath}', '', '[\"car\"]');

DROP TABLE IF EXISTS `user_extend`;
CREATE TABLE `user_extend` (
  `user_id` int(11) NOT NULL,
  `func_table_name`  varchar(4000) NULL,
  `properties` varchar(4000) NULL,
  `oper_time` datetime(6) NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ;


-- 1009
INSERT INTO `labelsystem`.`alg_warehouse_alginstance` (`id`, `alg_name`, `add_time`, `alg_type_name`, `alg_root_dir`) VALUES ('14', 'Multiple Target FairMOT Tracking ', '2020-10-09 17:44:24.000000', '多目标跟踪', '/FairMOT/');
INSERT INTO `labelsystem`.`alg_warehouse_alginstance` (`id`, `alg_name`, `add_time`, `alg_type_name`, `alg_root_dir`) VALUES ('15', 'Multiple Target CenterTrack Tracking ', '2020-10-09 17:44:24.000000', '多目标跟踪', '/CenterTrack/');

INSERT INTO `labelsystem`.`alg_warehouse_algmodel` (`id`, `model_name`, `alg_instance_id`, `exec_script`, `type_list`) VALUES ('20', 'Multiple Target FairMOT Tracking', '14', 'python3 src/image_demo.py  mot --load_model models/fairmot_dla34.pth --conf_thres 0.6 --data_dir {data_dir} --out_file {output}', '[\"person\"]');

INSERT INTO `labelsystem`.`alg_warehouse_algmodel` (`id`, `model_name`, `alg_instance_id`, `exec_script`, `type_list`) VALUES ('21', 'Multiple Target CenterTrack Tracking', '15', 'python3 src/demo.py tracking --load_model models/mot17_half.pth --num_class 1 --demo {data_dir} --save_results --vis_thresh 0.6 --out_file {output} --select_name {class_name}', '[\"car\",\"person\"]');

ALTER TABLE `labelsystem`.`alg_warehouse_algmodel` 
ADD COLUMN `model_type` INT(11) NULL DEFAULT NULL AFTER `threshold`;

UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `model_type` = '1' WHERE (`id` = '21');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `model_type` = '1' WHERE (`id` = '16');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `model_type` = '1' WHERE (`id` = '12');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `model_type` = '1' WHERE (`id` = '20');

UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `model_name` = 'Multiple Target FairMOT Tracking(person)' WHERE (`id` = '20');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `model_name` = 'Multiple Target CenterTrack Tracking(car+person)' WHERE (`id` = '21');

ALTER TABLE `labelsystem`.`alg_warehouse_algmodel` 
ADD COLUMN `auto_used` INT(11) NULL DEFAULT NULL AFTER `model_type`;

UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `auto_used` = '1' WHERE (`id` = '3');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `auto_used` = '1' WHERE (`id` = '4');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `auto_used` = '1' WHERE (`id` = '5');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `auto_used` = '1' WHERE (`id` = '6');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `auto_used` = '1' WHERE (`id` = '7');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `auto_used` = '1' WHERE (`id` = '11');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `auto_used` = '1' WHERE (`id` = '18');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `auto_used` = '1' WHERE (`id` = '20');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `auto_used` = '1' WHERE (`id` = '21');


ALTER TABLE `labelsystem`.`alg_warehouse_algmodel` 
ADD COLUMN `hand_label_used` INT(11) NULL DEFAULT NULL AFTER `auto_used`;


UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '3');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '4');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '5');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '6');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '7');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '10');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '11');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '12');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '16');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '18');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '20');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `hand_label_used` = '1' WHERE (`id` = '21');


UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `model_name` = 'Tracking_pyECO (Single Target)' WHERE (`id` = '16');
UPDATE `labelsystem`.`alg_warehouse_algmodel` SET `model_name` = 'Tracking (Single Target)' WHERE (`id` = '12');

-- 1015
ALTER TABLE `labelsystem`.`tasks_labeltask` 
ADD COLUMN `total_label` INT(11) NULL AFTER `task_status_desc`;


ALTER TABLE `labelsystem`.`tasks_prepredicttasks` 
ADD COLUMN `delete_similar_picture` INT(11) NULL AFTER `score_threshold`;

ALTER TABLE `labelsystem`.`tasks_prepredicttasks` 
CHANGE COLUMN `score_threshold` `score_threshhold` DOUBLE NULL DEFAULT NULL ;


-- 20201029
INSERT INTO `labelsystem`.`alg_warehouse_alginstance` (`id`, `alg_name`, `add_time`, `alg_type_name`, `alg_root_dir`) VALUES ('16', 'Lung Auto Label', '2020-10-29 17:44:24.000000', '肺自动标注', '/Lung_Segmentation/');

INSERT INTO `labelsystem`.`alg_warehouse_algmodel` (`id`, `model_name`, `model_url`, `alg_instance_id`, `exec_script`, `type_list`, `auto_used`) VALUES ('50', 'Lung Auto Label', 'checkpoints/no_normalize/CP_epoch50.pth', '16', 'python3 predict_for_autolabel.py --checkpoint {modelPath} ', '[\"Lung\"]', '1');


-- 20201216
ALTER TABLE `labelsystem`.`tasks_largepicturelabeltask` 
ADD COLUMN `appid` VARCHAR(300) NULL DEFAULT NULL AFTER `mainVideoInfo`;

