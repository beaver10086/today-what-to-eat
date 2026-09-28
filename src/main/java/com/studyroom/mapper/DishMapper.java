package com.studyroom.mapper;

import com.studyroom.model.Dish;
import com.studyroom.dto.DishQuery;
import com.studyroom.dto.DishView;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
    List<DishView> search(@Param("query") DishQuery query,
                          @Param("offset") long offset,
                          @Param("size") int size);

    long countMatches(@Param("query") DishQuery query);

    long countByShopId(@Param("shopId") Long shopId);
}
