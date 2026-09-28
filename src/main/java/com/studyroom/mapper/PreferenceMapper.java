package com.studyroom.mapper;

import com.studyroom.model.Preference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PreferenceMapper extends BaseMapper<Preference> {
    Preference selectByUserId(@Param("userId") Long userId);
}
