package com.studyroom.mapper;

import com.studyroom.model.DishTag;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DishTagMapper extends BaseMapper<DishTag> {
    List<DishTag> selectByDishId(@Param("dishId") Long dishId);

    List<DishTag> selectAllByDishId(@Param("dishId") Long dishId);

    List<com.studyroom.dto.DishTagWeight> selectTagsByDishIds(@Param("dishIds") List<Long> dishIds,
                                                               @Param("userId") Long userId);

    int deleteActiveByDishId(@Param("dishId") Long dishId);

    int setDeleted(@Param("id") Long id, @Param("deleted") int deleted);
}
