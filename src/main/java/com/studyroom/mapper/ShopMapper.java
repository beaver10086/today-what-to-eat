package com.studyroom.mapper;

import com.studyroom.model.Shop;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {
    long countByCanteenId(@org.apache.ibatis.annotations.Param("canteenId") Long canteenId);
}
