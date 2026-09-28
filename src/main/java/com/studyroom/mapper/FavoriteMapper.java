package com.studyroom.mapper;

import com.studyroom.model.Favorite;
import com.studyroom.dto.FavoriteView;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
    Favorite selectByUserAndTarget(@Param("userId") Long userId,
                                   @Param("targetType") int targetType,
                                   @Param("targetId") Long targetId);

    List<Long> selectTargetIds(@Param("userId") Long userId,
                               @Param("targetType") int targetType);

    List<FavoriteView> selectFavoriteViews(@Param("userId") Long userId,
                                           @Param("targetType") Integer targetType,
                                           @Param("offset") long offset,
                                           @Param("size") int size);

    long countByUser(@Param("userId") Long userId, @Param("targetType") Integer targetType);

    int setDeleted(@Param("id") Long id, @Param("isDeleted") int isDeleted);
}
