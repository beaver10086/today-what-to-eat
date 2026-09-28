package com.studyroom.mapper;

import com.studyroom.model.RecommendationItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationItemMapper extends BaseMapper<RecommendationItem> {
    List<RecommendationItem> selectByRecommendationId(@Param("recommendationId") Long recommendationId);
}
