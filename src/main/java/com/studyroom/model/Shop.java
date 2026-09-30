package com.studyroom.model;

import org.apache.ibatis.type.Alias;

@Alias("shop")
public class Shop {
    private Long id;
    private Long canteenId;
    private String shopName;
    private String locationDesc;
    private java.time.LocalTime openTime;
    private java.time.LocalTime closeTime;
    private String cuisine;
    private java.math.BigDecimal avgPrice;
    private Integer queueHeat;
    private java.math.BigDecimal rating;
    private Integer ratingCount;
    private String coverUrl;
    private String description;
    private Integer status;
    private Integer sortOrder;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCanteenId() { return canteenId; }
    public void setCanteenId(Long canteenId) { this.canteenId = canteenId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getLocationDesc() { return locationDesc; }
    public void setLocationDesc(String locationDesc) { this.locationDesc = locationDesc; }
    public java.time.LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(java.time.LocalTime openTime) { this.openTime = openTime; }
    public java.time.LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(java.time.LocalTime closeTime) { this.closeTime = closeTime; }
    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public java.math.BigDecimal getAvgPrice() { return avgPrice; }
    public void setAvgPrice(java.math.BigDecimal avgPrice) { this.avgPrice = avgPrice; }
    public Integer getQueueHeat() { return queueHeat; }
    public void setQueueHeat(Integer queueHeat) { this.queueHeat = queueHeat; }
    public java.math.BigDecimal getRating() { return rating; }
    public void setRating(java.math.BigDecimal rating) { this.rating = rating; }
    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
