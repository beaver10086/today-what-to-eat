-- v1.1 菜单元数据增量脚本
--
-- 背景：v1.1 引入「档口排队热度」「菜品打包适配度」「数据来源」三个字段后，
-- 这三个字段只被加进了全新安装脚本 V1.0.0__init.sql，仓库中缺少对应的升级脚本，
-- 而 README 又明确要求「升级已有数据库需执行一次 docs/sql/V1.0.3__menu_metadata.sql」。
-- 结果是：使用旧库部署时，Mapper 仍会查询这三个列并抛出
-- BadSqlGrammarException: Unknown column 'takeout_suitability' in 'field list'，
-- 而维护者按 README 操作时找不到该文件。本脚本补齐这条升级路径。
--
-- 使用方式（已有数据库执行一次即可，可重复执行，已存在的列会被跳过）：
--   mysql -u root -p what_to_eat < docs/sql/V1.0.3__menu_metadata.sql
-- 全新数据库无需执行：V1.0.0__init.sql 已内置这些列。

USE `what_to_eat`;

DROP PROCEDURE IF EXISTS `wte_add_column_if_missing`;

DELIMITER $$
CREATE PROCEDURE `wte_add_column_if_missing`(
    IN p_table  VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_ddl    TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE()
          AND `TABLE_NAME` = p_table
          AND `COLUMN_NAME` = p_column
    ) THEN
        SET @ddl = p_ddl;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 档口排队热度：0 无需排队 / 1 较短 / 2 较长 / 3 很长
CALL `wte_add_column_if_missing`(
    'shop',
    'queue_heat',
    'ALTER TABLE `shop` ADD COLUMN `queue_heat` tinyint DEFAULT NULL COMMENT '' 排队热度档位： 0 无需排队  1 较短  2 较长  3 很长 '' AFTER `avg_price`'
);

-- 菜品打包适配度：0 不适合 / 1 一般 / 2 适合
CALL `wte_add_column_if_missing`(
    'dish',
    'takeout_suitability',
    'ALTER TABLE `dish` ADD COLUMN `takeout_suitability` tinyint DEFAULT NULL COMMENT '' 外卖适宜度档位： 0 不适合  1 一般  2 适合 '' AFTER `calorie`'
);

-- 数据来源，例如「实地核对」
CALL `wte_add_column_if_missing`(
    'dish',
    'data_source',
    'ALTER TABLE `dish` ADD COLUMN `data_source` varchar(100) DEFAULT NULL COMMENT '' 数据来源 '' AFTER `takeout_suitability`'
);

DROP PROCEDURE IF EXISTS `wte_add_column_if_missing`;
