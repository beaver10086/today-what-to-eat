package com.studyroom.model;

import org.apache.ibatis.type.Alias;

@Alias("operationLog")
public class OperationLog {
    private Long id;
    private Long operatorId;
    private String module;
    private Integer action;
    private Long targetId;
    private String beforeData;
    private String afterData;
    private String ip;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public Integer getAction() { return action; }
    public void setAction(Integer action) { this.action = action; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getBeforeData() { return beforeData; }
    public void setBeforeData(String beforeData) { this.beforeData = beforeData; }
    public String getAfterData() { return afterData; }
    public void setAfterData(String afterData) { this.afterData = afterData; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
    public java.time.LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(java.time.LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}
