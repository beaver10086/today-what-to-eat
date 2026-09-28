package com.studyroom.model;

import org.apache.ibatis.type.Alias;

@Alias("preference")
public class Preference {
    private Long id;
    private Long userId;
    private Integer maxSpiceLevel;
    private java.math.BigDecimal budgetMin;
    private java.math.BigDecimal budgetMax;
    private String likeTagIds;
    private String dislikeTagIds;
    private Integer dietType;
    private String profileSummary;
    private String questionnaireVersion;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getMaxSpiceLevel() { return maxSpiceLevel; }
    public void setMaxSpiceLevel(Integer maxSpiceLevel) { this.maxSpiceLevel = maxSpiceLevel; }
    public java.math.BigDecimal getBudgetMin() { return budgetMin; }
    public void setBudgetMin(java.math.BigDecimal budgetMin) { this.budgetMin = budgetMin; }
    public java.math.BigDecimal getBudgetMax() { return budgetMax; }
    public void setBudgetMax(java.math.BigDecimal budgetMax) { this.budgetMax = budgetMax; }
    public String getLikeTagIds() { return likeTagIds; }
    public void setLikeTagIds(String likeTagIds) { this.likeTagIds = likeTagIds; }
    public String getDislikeTagIds() { return dislikeTagIds; }
    public void setDislikeTagIds(String dislikeTagIds) { this.dislikeTagIds = dislikeTagIds; }
    public Integer getDietType() { return dietType; }
    public void setDietType(Integer dietType) { this.dietType = dietType; }
    public String getProfileSummary() { return profileSummary; }
    public void setProfileSummary(String profileSummary) { this.profileSummary = profileSummary; }
    public String getQuestionnaireVersion() { return questionnaireVersion; }
    public void setQuestionnaireVersion(String questionnaireVersion) { this.questionnaireVersion = questionnaireVersion; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
