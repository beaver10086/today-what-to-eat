package com.studyroom.mapper;

import com.studyroom.model.FeedbackRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FeedbackRecordMapper extends BaseMapper<FeedbackRecord> {
    List<FeedbackRecord> selectRecentByUserAndDish(@Param("userId") Long userId,
                                                    @Param("dishId") Long dishId,
                                                    @Param("limit") int limit);
}
