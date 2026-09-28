package com.studyroom.model;

import org.apache.ibatis.type.Alias;

@Alias("canteen")
public class Canteen {
    private Long id;
    private String canteenName;
    private String campus;
    private String location;
    private java.time.LocalTime openTime;
    private java.time.LocalTime closeTime;
    private String description;
    private String coverUrl;
    private Integer sortOrder;
    private Integer status;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCanteenName() { return canteenName; }
    public void setCanteenName(String canteenName) { this.canteenName = canteenName; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public java.time.LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(java.time.LocalTime openTime) { this.openTime = openTime; }
    public java.time.LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(java.time.LocalTime closeTime) { this.closeTime = closeTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
