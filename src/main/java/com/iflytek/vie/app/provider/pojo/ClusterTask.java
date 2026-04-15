package com.iflytek.vie.app.provider.pojo;

public class ClusterTask {
   private Long taskId;
   private Long togatherId;
   private Long pathId;
   private Integer togatherStatus;
   private String pathContent;
   private String dataAuth;
   private String updateTime;
   private Integer dataType;
   private String batchId;
   private int ifLone;

   public String getUpdateTime() {
      return this.updateTime;
   }

   public void setUpdateTime(String updateTime) {
      this.updateTime = updateTime;
   }

   public String getDataAuth() {
      return this.dataAuth;
   }

   public void setDataAuth(String dataAuth) {
      this.dataAuth = dataAuth;
   }

   public Long getTaskId() {
      return this.taskId;
   }

   public void setTaskId(Long taskId) {
      this.taskId = taskId;
   }

   public Long getTogatherId() {
      return this.togatherId;
   }

   public void setTogatherId(Long togatherId) {
      this.togatherId = togatherId;
   }

   public Long getPathId() {
      return this.pathId;
   }

   public void setPathId(Long pathId) {
      this.pathId = pathId;
   }

   public Integer getTogatherStatus() {
      return this.togatherStatus;
   }

   public void setTogatherStatus(Integer togatherStatus) {
      this.togatherStatus = togatherStatus;
   }

   public String getPathContent() {
      return this.pathContent;
   }

   public void setPathContent(String pathContent) {
      this.pathContent = pathContent;
   }

   public Integer getDataType() {
      return this.dataType;
   }

   public void setDataType(Integer dataType) {
      this.dataType = dataType;
   }

   public String getBatchId() {
      return this.batchId;
   }

   public void setBatchId(String batchId) {
      this.batchId = batchId;
   }

   public int getIfLone() {
      return this.ifLone;
   }

   public void setIfLone(int ifLone) {
      this.ifLone = ifLone;
   }
}
