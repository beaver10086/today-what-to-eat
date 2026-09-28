import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
schema = (root / "docs/sql/V1.0.0__init.sql").read_text(encoding="utf-8")
tables = re.findall(
    r"CREATE TABLE `([a-z_]+)`\s*\((.*?)\)\s*ENGINE=InnoDB[^;]*;",
    schema,
    re.S,
)
if len(tables) != 14:
    raise SystemExit(f"expected 14 schema tables, got {len(tables)}")

sql_types = {
    "bigint": "Long",
    "int": "Integer",
    "tinyint": "Integer",
    "decimal": "java.math.BigDecimal",
    "datetime": "java.time.LocalDateTime",
    "time": "java.time.LocalTime",
    "varchar": "String",
    "text": "String",
    "json": "String",
}


def camel(name):
    return re.sub(r"_([a-z])", lambda match: match.group(1).upper(), name)


def pascal(name):
    return "".join(part.title() for part in name.split("_"))


def java_type(type_name):
    match = re.match(r"([a-z]+)", type_name)
    return sql_types[match.group(1)]


def java_columns(body):
    found = re.findall(r"`([a-z_]+)`\s+((?:bigint|tinyint|int|decimal|datetime|time|varchar|text|json)[^,\n]*)", body)
    columns = []
    for name, column_type in found:
        if name in {"primary", "unique", "key"}:
            continue
        columns.append((name, camel(name), java_type(column_type.strip())))
    return columns


model_dir = root / "src/main/java/com/studyroom/model"
mapper_dir = root / "src/main/java/com/studyroom/mapper"
xml_dir = root / "src/main/resources/mapper"
xml_dir.mkdir(parents=True, exist_ok=True)

for table, body in tables:
    class_name = pascal(table)
    columns = java_columns(body)
    fields = "\n".join(f"    private {t} {field};" for _, field, t in columns)
    methods = []
    for _, field, t in columns:
        title = field[0].upper() + field[1:]
        methods.extend(
            [
                f"    public {t} get{title}() {{ return {field}; }}",
                f"    public void set{title}({t} {field}) {{ this.{field} = {field}; }}",
            ]
        )
    (model_dir / f"{class_name}.java").write_text(
        f"""package com.studyroom.model;

import org.apache.ibatis.type.Alias;

@Alias("{camel(table)}")
public class {class_name} {{
{fields}

{chr(10).join(methods)}
}}
""",
        encoding="utf-8",
    )

    mapper_name = class_name + "Mapper"
    (mapper_dir / f"{mapper_name}.java").write_text(
        f"""package com.studyroom.mapper;

import com.studyroom.model.{class_name};
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface {mapper_name} extends BaseMapper<{class_name}> {{
}}
""",
        encoding="utf-8",
    )

    business = [(name, field) for name, field, _ in columns if name not in {"id", "create_time", "update_time", "is_deleted"}]
    all_columns = ", ".join(f"`{name}`" for name, _, _ in columns)
    insert_columns = "\n".join(
        f'            <if test="{field} != null">`{name}`,</if>' for name, field in business
    )
    insert_values = "\n".join(
        f'            <if test="{field} != null">#{{{field}}},</if>' for _, field in business
    )
    updates = ",\n        ".join(f"`{name}` = #{{{field}}}" for name, field in business)
    xml_columns = ",\n        ".join(f"`{name}`" for name, _, _ in columns)
    xml = f"""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.studyroom.mapper.{mapper_name}">
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO `{table}`
        <trim prefix="(" suffix=")" suffixOverrides=",">
{insert_columns}
        </trim>
        VALUES
        <trim prefix="(" suffix=")" suffixOverrides=",">
{insert_values}
        </trim>
    </insert>
    <select id="selectById" resultType="com.studyroom.model.{class_name}">
        SELECT {xml_columns} FROM `{table}` WHERE `id` = #{{id}} AND `is_deleted` = 0
    </select>
    <update id="update">
        UPDATE `{table}` SET {updates}, `update_time` = CURRENT_TIMESTAMP
        WHERE `id` = #{{id}} AND `is_deleted` = 0
    </update>
    <update id="deleteById">
        UPDATE `{table}` SET `is_deleted` = 1, `update_time` = CURRENT_TIMESTAMP
        WHERE `id` = #{{id}} AND `is_deleted` = 0
    </update>
    <select id="selectPage" resultType="com.studyroom.model.{class_name}">
        SELECT {xml_columns} FROM `{table}` WHERE `is_deleted` = 0
        ORDER BY `id` DESC LIMIT #{{size}} OFFSET #{{offset}}
    </select>
    <select id="count" resultType="long">
        SELECT COUNT(*) FROM `{table}` WHERE `is_deleted` = 0
    </select>
</mapper>
"""
    (xml_dir / f"{mapper_name}.xml").write_text(xml, encoding="utf-8")

base_mapper = mapper_dir / "BaseMapper.java"
base_mapper.write_text(
    """package com.studyroom.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface BaseMapper<T> {
    int insert(T entity);

    T selectById(@Param("id") Long id);

    int update(T entity);

    int deleteById(@Param("id") Long id);

    List<T> selectPage(@Param("offset") long offset, @Param("size") int size);

    long count();
}
""",
    encoding="utf-8",
)
print(f"Generated {len(tables)} entity classes, mapper interfaces, and XML mappings.")
