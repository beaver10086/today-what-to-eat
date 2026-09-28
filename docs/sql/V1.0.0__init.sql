-- Initial schema based on the database design.
-- Change: create v1.0.0 base tables.
CREATE DATABASE IF NOT EXISTS `what_to_eat`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE `what_to_eat`;
-- 1. 用户

CREATE TABLE `app_user` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT ' 主键 ', `username` varchar(32) NOT NULL COMMENT ' 登录名 ', `password` varchar(100) NOT NULL COMMENT 'BCrypt 加密密码 ', `nickname` varchar(32) DEFAULT NULL COMMENT ' 昵称 ', `avatar_url` varchar(255) DEFAULT NULL COMMENT ' 头像 ', `phone` varchar(20) DEFAULT NULL COMMENT ' 手机号 ', `role` tinyint NOT NULL DEFAULT 1 COMMENT '1 普通用户  2 管理员 ', `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 正常  0 禁用 ', `last_login_time` datetime DEFAULT NULL COMMENT ' 最近登录时间 ', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_username` (`username`, `is_deleted`),

  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 用户表 ';

CREATE TABLE `canteen` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT ' 主键 ', `canteen_name` varchar(50) NOT NULL COMMENT ' 食堂名称 ', `campus` varchar(50) NOT NULL COMMENT ' 所属校区 ', `location` varchar(100) DEFAULT NULL COMMENT ' 位置描述 ', `open_time` time DEFAULT NULL COMMENT ' 开门时间 ', `close_time` time DEFAULT NULL COMMENT ' 关门时间 ', `description` varchar(500) DEFAULT NULL COMMENT ' 简介 ', `cover_url` varchar(255) DEFAULT NULL COMMENT ' 封面图 ', `sort_order` int NOT NULL DEFAULT 0 COMMENT ' 排序 ', `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 营业  0 停业 ', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_campus_name` (`campus`, `canteen_name`, `is_deleted`), KEY `idx_campus_status` (`campus`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 食堂表 ';

CREATE TABLE `shop` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT ' 主键 ', `canteen_id` bigint unsigned NOT NULL COMMENT ' 所属食堂 ID ，必须为有效食堂 ', `shop_name` varchar(50) NOT NULL COMMENT ' 档口名称 ', `location_desc` varchar(100) DEFAULT NULL COMMENT ' 楼层 / 窗口号 ', `open_time` time DEFAULT NULL COMMENT ' 开门时间 ', `close_time` time DEFAULT NULL COMMENT ' 关门时间 ', `cuisine` varchar(30) DEFAULT NULL COMMENT ' 主营菜系 ', `avg_price` decimal(8,2) DEFAULT NULL COMMENT ' 人均价格 ', `rating` decimal(2,1) NOT NULL DEFAULT 0.0 COMMENT ' 评分 ', `rating_count` int NOT NULL DEFAULT 0 COMMENT ' 评分人数 ', `cover_url` varchar(255) DEFAULT NULL COMMENT ' 封面图 ', `description` varchar(500) DEFAULT NULL COMMENT ' 简介 ', `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 营业  0 停业 ', `sort_order` int NOT NULL DEFAULT 0 COMMENT ' 排序 ', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_canteen_shop` (`canteen_id`, `shop_name`, `is_deleted`), KEY `idx_canteen_id` (`canteen_id`), KEY `idx_shop_name` (`shop_name`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 食堂档口表 ';

CREATE TABLE `dish` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT ' 主键 ', `shop_id` bigint unsigned NOT NULL COMMENT ' 所属门店，必须挂载 ', `dish_name` varchar(60) NOT NULL COMMENT ' 菜品名称 ', `price` decimal(8,2) NOT NULL COMMENT ' 价格 ', `category` tinyint NOT NULL COMMENT '1 主食  2 荤菜  3 素菜  4 汤羹  5 小吃  6 饮品  7 甜点 ',

  `meal_type` tinyint NOT NULL DEFAULT 15 COMMENT ' 餐段位掩码： 1 早  2 午  4 晚  8 夜宵，默认 15 全餐段 ', `spice_level` tinyint NOT NULL DEFAULT 0 COMMENT '0 不辣  1 微辣  2 中辣  3 重辣 ', `calorie` int DEFAULT NULL COMMENT ' 热量 ( 千卡 )', `description` varchar(300) DEFAULT NULL COMMENT ' 简介 ', `image_url` varchar(255) DEFAULT NULL COMMENT ' 图片 ', `is_signature` tinyint NOT NULL DEFAULT 0 COMMENT ' 是否招牌 ', `is_available` tinyint NOT NULL DEFAULT 1 COMMENT '1 上架  0 下架 ', `rating` decimal(2,1) NOT NULL DEFAULT 0.0 COMMENT ' 菜品评分 ', `rating_count` int NOT NULL DEFAULT 0 COMMENT ' 评分人数 ', `like_count` int NOT NULL DEFAULT 0 COMMENT ' 喜欢数 ', `dislike_count` int NOT NULL DEFAULT 0 COMMENT ' 不喜欢数 ', `favorite_count` int NOT NULL DEFAULT 0 COMMENT ' 收藏数 ', `recommend_count` int NOT NULL DEFAULT 0 COMMENT ' 被推荐次数 ', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), KEY `idx_shop_id` (`shop_id`), KEY `idx_price` (`price`), KEY `idx_category` (`category`), KEY `idx_dish_name` (`dish_name`), KEY `idx_spice_level` (`spice_level`), KEY `idx_meal_type` (`meal_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 菜品表 ';

CREATE TABLE `tag` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT ' 主键 ', `tag_name` varchar(30) NOT NULL COMMENT ' 标签名 ', `tag_type` tinyint NOT NULL COMMENT '1 口味  2 菜系  3 食材  4 忌口 / 饮食限制  5 场景 / 特征 ', `sort_order` int NOT NULL DEFAULT 0 COMMENT ' 排序 ', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_type_name` (`tag_type`, `tag_name`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 标签表 ';

CREATE TABLE `dish_tag` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT, `dish_id` bigint unsigned NOT NULL, `tag_id` bigint unsigned NOT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_dish_tag` (`dish_id`, `tag_id`, `is_deleted`), KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 菜品标签关联表 ';

CREATE TABLE `preference` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT, `user_id` bigint unsigned NOT NULL, `max_spice_level` tinyint NOT NULL DEFAULT 3 COMMENT ' 可接受最高辣度 ',

  `budget_min` decimal(8,2) DEFAULT NULL, `budget_max` decimal(8,2) DEFAULT NULL, `like_tag_ids` json DEFAULT NULL COMMENT ' 喜欢的标签 ID 列表 ', `dislike_tag_ids` json DEFAULT NULL COMMENT ' 忌口 / 不喜欢标签 ID 列表 ', `diet_type` tinyint NOT NULL DEFAULT 0 COMMENT '0 无限制  1 素食  2 清真  3 其他 ', `profile_summary` varchar(500) DEFAULT NULL COMMENT ' 口味画像摘要 ', `questionnaire_version` varchar(10) NOT NULL DEFAULT 'v1', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_user_id` (`user_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 用户口味问卷表 ';

CREATE TABLE `user_taste_profile` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT, `user_id` bigint unsigned NOT NULL, `tag_id` bigint unsigned NOT NULL, `weight` decimal(5,2) NOT NULL DEFAULT 0.00 COMMENT ' 偏好权重  -10~10', `source` tinyint NOT NULL DEFAULT 1 COMMENT '1 问卷  2 反馈调整 ', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_user_tag` (`user_id`, `tag_id`, `is_deleted`), KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 用户口味画像表 ';

CREATE TABLE `recommendation` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT, `request_no` varchar(32) NOT NULL COMMENT ' 推荐请求编号 ', `user_id` bigint unsigned NOT NULL, `meal_type` tinyint NOT NULL COMMENT '1 早  2 午  3 晚  4 夜宵 ', `budget_max` decimal(8,2) DEFAULT NULL, `weather` varchar(20) DEFAULT NULL, `source` tinyint NOT NULL COMMENT '1 大模型  2 规则降级  3 缓存 ', `is_fallback` tinyint NOT NULL DEFAULT 0 COMMENT ' 是否降级 ', `llm_model` varchar(50) DEFAULT NULL, `prompt_version` varchar(20) DEFAULT NULL, `cost_ms` int DEFAULT NULL COMMENT ' 耗时 ( 毫秒 )', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_request_no` (`request_no`, `is_deleted`), KEY `idx_user_time` (`user_id`, `create_time`), KEY `idx_source_fallback` (`source`, `is_fallback`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 推荐记录表 ';

CREATE TABLE `recommendation_item` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT, `recommendation_id` bigint unsigned NOT NULL, `dish_id` bigint unsigned NOT NULL,

  `shop_id` bigint unsigned NOT NULL COMMENT ' 冗余门店 ID', `rank_no` tinyint NOT NULL DEFAULT 1, `score` decimal(6,3) DEFAULT NULL, `reason` varchar(500) NOT NULL COMMENT ' 推荐理由 ', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_rec_dish` (`recommendation_id`, `dish_id`, `is_deleted`), KEY `idx_recommendation_id` (`recommendation_id`), KEY `idx_dish_id` (`dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 推荐明细表 ';

CREATE TABLE `feedback_record` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT, `user_id` bigint unsigned NOT NULL, `recommendation_id` bigint unsigned NOT NULL, `dish_id` bigint unsigned NOT NULL, `feedback_type` tinyint NOT NULL COMMENT '1 喜欢  2 不喜欢  3 换一个  4 已吃过 ', `reason_tag` varchar(50) DEFAULT NULL COMMENT ' 原因标签 ', `comment` varchar(255) DEFAULT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), KEY `idx_user_dish` (`user_id`, `dish_id`), KEY `idx_recommendation_id` (`recommendation_id`), KEY `idx_feedback_type` (`feedback_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 推荐反馈表 ';

CREATE TABLE `favorite` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT, `user_id` bigint unsigned NOT NULL, `target_type` tinyint NOT NULL COMMENT '1 菜品  2 门店 ', `target_id` bigint unsigned NOT NULL, `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '1= 已取消收藏 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=' 收藏表 ';

CREATE TABLE `operation_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `operator_id` bigint unsigned NOT NULL,
  `module` varchar(30) NOT NULL,
  `action` tinyint NOT NULL,
  `target_id` bigint unsigned NOT NULL,
  `before_data` json DEFAULT NULL,
  `after_data` json DEFAULT NULL,
  `ip` varchar(45) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_module_target` (`module`, `target_id`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作留痕表';

CREATE TABLE `prompt_template` ( `id` bigint unsigned NOT NULL AUTO_INCREMENT, `template_code` varchar(50) NOT NULL COMMENT ' 模板编码 ', `version` varchar(20) NOT NULL COMMENT ' 版本号 ', `content` text NOT NULL COMMENT 'Prompt 内容 ', `is_active` tinyint NOT NULL DEFAULT 0 COMMENT ' 是否当前生效 ', `remark` varchar(200) DEFAULT NULL COMMENT ' 变更说明 ', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP, `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, `is_deleted` tinyint NOT NULL DEFAULT 0 COMMENT '0 正常  1 删除 ', PRIMARY KEY (`id`), UNIQUE KEY `uk_code_version` (`template_code`, `version`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt 模板表 ';
