-- 功能测试用例的数据夹具
--
-- 用途：为 TC-DISH-05（菜品详情返回打包适配度 / 数据来源）与
--       TC-SHOP-01（档口详情返回排队热度）准备可验证的非空取值。
--
-- 背景：种子数据（V1.0.1__seed.sql）中这三个字段默认为 NULL，而应用配置了
--       spring.jackson.default-property-inclusion: non_null，
--       值为 NULL 时字段不会出现在响应体中，导致用例无法验证「字段确实被返回」。
--       因此这里显式写入取值后再执行用例。
--
-- 执行顺序（全新数据库）：
--   docs/sql/V1.0.0__init.sql  ->  docs/sql/V1.0.1__seed.sql
--   -> docs/sql/V1.0.2__accounts.sql -> docs/sql/V1.0.3__menu_metadata.sql
--   -> tests/functional/fixture.sql

USE `what_to_eat`;

-- 供 TC-DISH-05 使用：打包适配度 = 2（适合），数据来源 = 实地核对
UPDATE `dish`
SET `takeout_suitability` = 2,
    `data_source`        = '实地核对'
WHERE `id` = 920001;

-- 供 TC-SHOP-01 使用：排队热度 = 2（较长）
UPDATE `shop`
SET `queue_heat` = 2
WHERE `id` = 910001;
