package com.studyroom.mapper;

import com.studyroom.model.UserTasteProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserTasteProfileMapper extends BaseMapper<UserTasteProfile> {
    List<UserTasteProfile> selectAllByUserId(@Param("userId") Long userId);

    List<UserTasteProfile> selectByUserId(@Param("userId") Long userId);

    int setDeleted(@Param("id") Long id, @Param("isDeleted") int isDeleted);
}
