package com.studyroom.mapper;

import com.studyroom.model.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
    AppUser selectByUsername(@Param("username") String username);

    int touchLastLogin(@Param("id") Long id);
}
