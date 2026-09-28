package com.studyroom.model;

import org.apache.ibatis.type.Alias;

@Alias("dish")
public class Dish {
    private Long id;
    private Long shopId;
    private String dishName;
    private java.math.BigDecimal price;
    private Integer category;
    private Integer mealType;
    private Integer spiceLevel;
    private Integer calorie;
    private String description;
    private String imageUrl;
    private Integer isSignature;
    private Integer isAvailable;
    private java.math.BigDecimal rating;
    private Integer ratingCount;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer favoriteCount;
    private Integer recommendCount;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }
    public java.math.BigDecimal getPrice() { return price; }
    public void setPrice(java.math.BigDecimal price) { this.price = price; }
    public Integer getCategory() { return category; }
    public void setCategory(Integer category) { this.category = category; }
    public Integer getMealType() { return mealType; }
    public void setMealType(Integer mealType) { this.mealType = mealType; }
    public Integer getSpiceLevel() { return spiceLevel; }
    public void setSpiceLevel(Integer spiceLevel) { this.spiceLevel = spiceLevel; }
    public Integer getCalorie() { return calorie; }
    public void setCalorie(Integer calorie) { this.calorie = calorie; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getIsSignature() { return isSignature; }
    public void setIsSignature(Integer isSignature) { this.isSignature = isSignature; }
    public Integer getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Integer isAvailable) { this.isAvailable = isAvailable; }
    public java.math.BigDecimal getRating() { return rating; }
    public void setRating(java.math.BigDecimal rating) { this.rating = rating; }
    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Integer getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(Integer dislikeCount) { this.dislikeCount = dislikeCount; }
    public Integer getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(Integer favoriteCount) { this.favoriteCount = favoriteCount; }
    public Integer getRecommendCount() { return recommendCount; }
    public void setRecommendCount(Integer recommendCount) { this.recommendCount = recommendCount; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
