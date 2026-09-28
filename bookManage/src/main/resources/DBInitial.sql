-- ----------------------------
-- 图书管理系统数据库建表脚本 (MySQL 5.7+ / 8.0+)
-- ----------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
                            `username` varchar(32) NOT NULL COMMENT '登录账号，唯一',
                            `password` varchar(128) NOT NULL COMMENT '登录密码(加密)',
                            `email` varchar(100) NOT NULL COMMENT '用户邮箱，唯一',
                            `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
                            `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
                            `avatar_url` varchar(500) DEFAULT NULL COMMENT '用户头像地址',
                            `role` varchar(20) NOT NULL DEFAULT 'READER' COMMENT '角色: READER, LIBRARIAN, ADMIN',
                            `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, DISABLED, LOCKED',
                            `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_username` (`username`),
                            UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ----------------------------
-- 2. 图书分类表
-- ----------------------------
DROP TABLE IF EXISTS `b_category`;
CREATE TABLE `b_category` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类唯一标识',
                              `name` varchar(50) NOT NULL COMMENT '分类名称',
                              `description` varchar(500) DEFAULT NULL COMMENT '分类描述',
                              `sort_order` int DEFAULT '0' COMMENT '排序权重',
                              `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, DISABLED',
                              `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书分类表';

-- ----------------------------
-- 3. 图书信息表
-- ----------------------------
DROP TABLE IF EXISTS `b_book`;
CREATE TABLE `b_book` (
                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '图书唯一标识',
                          `title` varchar(200) NOT NULL COMMENT '图书书名',
                          `author` varchar(100) NOT NULL COMMENT '图书作者',
                          `isbn` varchar(20) NOT NULL COMMENT '国际标准书号(去除连字符)',
                          `publisher` varchar(100) DEFAULT NULL COMMENT '出版社',
                          `publish_date` date DEFAULT NULL COMMENT '出版日期',
                          `description` text COMMENT '图书简介',
                          `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图片地址',
                          `category_id` bigint NOT NULL COMMENT '关联分类ID',
                          `total_stock` int NOT NULL DEFAULT '0' COMMENT '总库存量',
                          `available_stock` int NOT NULL DEFAULT '0' COMMENT '可用库存量',
                          `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, INACTIVE',
                          `is_deleted` tinyint(1) DEFAULT '0' COMMENT '软删除标记: 0-正常, 1-已删除',
                          `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_isbn` (`isbn`),
                          KEY `idx_category_id` (`category_id`),
                          CONSTRAINT `fk_book_category_id` FOREIGN KEY (`category_id`) REFERENCES `b_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书信息表';

-- ----------------------------
-- 4. 借阅单表
-- ----------------------------
DROP TABLE IF EXISTS `b_borrow_order`;
CREATE TABLE `b_borrow_order` (
                                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单唯一标识',
                                  `order_no` varchar(32) NOT NULL COMMENT '业务流水单号',
                                  `user_id` bigint NOT NULL COMMENT '借阅读者ID',
                                  `book_id` bigint NOT NULL COMMENT '借阅图书ID',
                                  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '订单状态: PENDING, APPROVED, REJECTED, BORROWED, RETURN_REQUESTED, RETURNED, CANCELLED, OVERDUE',
                                  `remark` varchar(500) DEFAULT NULL COMMENT '读者备注',
                                  `audit_remark` varchar(500) DEFAULT NULL COMMENT '管理员审核备注',
                                  `return_remark` varchar(200) DEFAULT NULL COMMENT '读者发起归还时的说明',
                                  `return_condition` varchar(20) DEFAULT NULL COMMENT '归还验收状态: GOOD, DAMAGED, LOST',
                                  `reserved_at` datetime NOT NULL COMMENT '预约发起时间',
                                  `approved_at` datetime DEFAULT NULL COMMENT '审核通过时间',
                                  `borrowed_at` datetime DEFAULT NULL COMMENT '借出确认时间',
                                  `due_at` datetime DEFAULT NULL COMMENT '应还时间',
                                  `returned_at` datetime DEFAULT NULL COMMENT '实际归还时间',
                                  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`id`),
                                  UNIQUE KEY `uk_order_no` (`order_no`),
                                  KEY `idx_user_id` (`user_id`),
                                  KEY `idx_book_id` (`book_id`),
                                  KEY `idx_status` (`status`),
                                  CONSTRAINT `fk_borrow_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`),
                                  CONSTRAINT `fk_borrow_book_id` FOREIGN KEY (`book_id`) REFERENCES `b_book` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书借阅订单表';

-- ----------------------------
-- 5. 图书库存流水表
-- ----------------------------
DROP TABLE IF EXISTS `b_book_stock_log`;
CREATE TABLE `b_book_stock_log` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水唯一标识',
                                    `book_id` bigint NOT NULL COMMENT '图书ID',
                                    `operator_id` bigint NOT NULL COMMENT '操作人ID',
                                    `change_amount` int NOT NULL COMMENT '变更量(正负数)',
                                    `reason` varchar(200) NOT NULL COMMENT '调整原因',
                                    `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_book_id` (`book_id`),
                                    CONSTRAINT `fk_stock_book_id` FOREIGN KEY (`book_id`) REFERENCES `b_book` (`id`),
                                    CONSTRAINT `fk_stock_operator_id` FOREIGN KEY (`operator_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图书库存变更流水表';

-- ----------------------------
-- 6. 刷新令牌表
-- ----------------------------
-- 刷新令牌需要服务端可主动失效（退出登录、重置密码），纯无状态 JWT 做不到，
-- 因此落库保存并校验，避免为此额外引入 Redis 依赖。
DROP TABLE IF EXISTS `sys_refresh_token`;
CREATE TABLE `sys_refresh_token` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录唯一标识',
                                     `user_id` bigint NOT NULL COMMENT '所属用户ID',
                                     `token_id` varchar(64) NOT NULL COMMENT '刷新令牌 jti，唯一',
                                     `revoked` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已撤销: 0-有效, 1-已撤销',
                                     `expires_at` datetime NOT NULL COMMENT '过期时间',
                                     `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '签发时间',
                                     PRIMARY KEY (`id`),
                                     UNIQUE KEY `uk_token_id` (`token_id`),
                                     KEY `idx_user_id` (`user_id`),
                                     CONSTRAINT `fk_refresh_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='刷新令牌表';

SET FOREIGN_KEY_CHECKS = 1;