-- 账号种子：补齐「今天吃什么」数据库设计（修正版）第 15 页示例种子中要求的 app_user。
-- 原 V1.0.1__seed.sql 仅含食堂/档口/菜品/标签演示数据，未插入任何用户，故此处补管理员与演示账号。
-- 依赖 V1.0.0__init.sql；使用 BCrypt($2a$10$...) 加密密码，与 Spring Security BCryptPasswordEncoder 兼容。
-- 固定使用 800000 段 ID，便于反复导入/清理（DELETE FROM app_user WHERE id BETWEEN 800001 AND 800002）。
USE `what_to_eat`;

-- 管理员：admin / admin123  (role=2)
INSERT INTO `app_user` (`id`, `username`, `password`, `nickname`, `role`, `status`)
VALUES (800001, 'admin', '$2a$10$6adcoOX8h5vhIZDHPlPKJe96jFVLj26peqWuqsiSTa/IAiImimTFS', '系统管理员', 2, 1);

-- 演示普通用户：demo / demo123  (role=1)
INSERT INTO `app_user` (`id`, `username`, `password`, `nickname`, `role`, `status`)
VALUES (800002, 'demo', '$2a$10$McT51g9G0GlMBLU3GSN/re4ap8dSgul1iaSsN4a4/tZo03eJgs1Ua', '演示用户', 1, 1);
