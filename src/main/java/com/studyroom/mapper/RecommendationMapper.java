package com.studyroom.mapper;

import com.studyroom.model.Recommendation;
import com.studyroom.dto.DishView;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationMapper extends BaseMapper<Recommendation> {
    List<DishView> selectCandidates(@Param("mealMask") int mealMask,
                                    @Param("maxSpice") int maxSpice,
                                    @Param("budgetMin") BigDecimal budgetMin,
                                    @Param("budgetMax") BigDecimal budgetMax,
                                    @Param("dietType") int dietType,
                                    @Param("excludeDishIds") List<Long> excludeDishIds,
                                    @Param("dislikeTagIds") List<Long> dislikeTagIds);
}
