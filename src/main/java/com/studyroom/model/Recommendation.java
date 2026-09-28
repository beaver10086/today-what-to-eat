package com.studyroom.model;

import org.apache.ibatis.type.Alias;

@Alias("recommendation")
public class Recommendation {
    private Long id;
    private String requestNo;
    private Long userId;
    private Integer mealType;
    private java.math.BigDecimal budgetMax;
    private String weather;
    private Integer source;
    private Integer isFallback;
    private String llmModel;
    private String promptVersion;
    private Integer costMs;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getMealType() { return mealType; }
    public void setMealType(Integer mealType) { this.mealType = mealType; }
    public java.math.BigDecimal getBudgetMax() { return budgetMax; }
    public void setBudgetMax(java.math.BigDecimal budgetMax) { this.budgetMax = budgetMax; }
    public String getWeather() { return weather; }
    public void setWeather(String weather) { this.weather = weather; }
    public Integer getSource() { return source; }
    public void setSource(Integer source) { this.source = source; }
    public Integer getIsFallback() { return isFallback; }
    public void setIsFallback(Integer isFallback) { this.isFallback = isFallback; }
    public String getLlmModel() { return llmModel; }
    public void setLlmModel(String llmModel) { this.llmModel = llmModel; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public Integer getCostMs() { return costMs; }
    public void setCostMs(Integer costMs) { this.costMs = costMs; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
