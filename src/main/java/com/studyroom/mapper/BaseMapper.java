package com.studyroom.mapper;

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
