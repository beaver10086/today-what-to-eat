package com.studyroom.model;

import org.apache.ibatis.type.Alias;

@Alias("feedbackRecord")
public class FeedbackRecord {
    private Long id;
    private Long userId;
    private Long recommendationId;
    private Long dishId;
    private Integer feedbackType;
    private String reasonTag;
    private String comment;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRecommendationId() { return recommendationId; }
    public void setRecommendationId(Long recommendationId) { this.recommendationId = recommendationId; }
    public Long getDishId() { return dishId; }
    public void setDishId(Long dishId) { this.dishId = dishId; }
    public Integer getFeedbackType() { return feedbackType; }
    public void setFeedbackType(Integer feedbackType) { this.feedbackType = feedbackType; }
    public String getReasonTag() { return reasonTag; }
    public void setReasonTag(String reasonTag) { this.reasonTag = reasonTag; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
