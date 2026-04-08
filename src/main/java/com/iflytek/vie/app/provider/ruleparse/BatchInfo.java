package com.iflytek.vie.app.provider.ruleparse;

import java.util.Date;

public class BatchInfo {
   private static final long serialVersionUID = 1L;
   private int id;
   private String batchId;
   private Integer dataType;
   private Integer ifLone;
   private Integer analysisType;
   private int status;
   private Date startTime;
   private Date endTime;
   private int importCount;
   private int handleCount;
   private String createUser;
   private String remark;
   private String models;
   private String failReason;
   private int taskStatus;

   public int getId() {
      return this.id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public String getBatchId() {
      return this.batchId;
   }

   public void setBatchId(String batchId) {
      this.batchId = batchId;
   }

   public Integer getDataType() {
      return this.dataType;
   }

   public void setDataType(Integer dataType) {
      this.dataType = dataType;
   }

   public Integer getIfLone() {
      return this.ifLone;
   }

   public void setIfLone(Integer ifLone) {
      this.ifLone = ifLone;
   }

   public Integer getAnalysisType() {
      return this.analysisType;
   }

   public void setAnalysisType(Integer analysisType) {
      this.analysisType = analysisType;
   }

   public int getStatus() {
      return this.status;
   }

   public void setStatus(int status) {
      this.status = status;
   }

   public Date getStartTime() {
      return this.startTime;
   }

   public void setStartTime(Date startTime) {
      this.startTime = startTime;
   }

   public Date getEndTime() {
      return this.endTime;
   }

   public void setEndTime(Date endTime) {
      this.endTime = endTime;
   }

   public int getImportCount() {
      return this.importCount;
   }

   public void setImportCount(int importCount) {
      this.importCount = importCount;
   }

   public int getHandleCount() {
      return this.handleCount;
   }

   public void setHandleCount(int handleCount) {
      this.handleCount = handleCount;
   }

   public String getCreateUser() {
      return this.createUser;
   }

   public void setCreateUser(String createUser) {
      this.createUser = createUser;
   }

   public String getRemark() {
      return this.remark;
   }

   public void setRemark(String remark) {
      this.remark = remark;
   }

   public String getModels() {
      return this.models;
   }

   public void setModels(String models) {
      this.models = models;
   }

   public String getFailReason() {
      return this.failReason;
   }

   public void setFailReason(String failReason) {
      this.failReason = failReason;
   }

   public int getTaskStatus() {
      return this.taskStatus;
   }

   public void setTaskStatus(int taskStatus) {
      this.taskStatus = taskStatus;
   }
}
