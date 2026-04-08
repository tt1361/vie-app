package com.iflytek.vie.app.provider.pojo;

public class DimensionPersonalTask {
   private long id;
   private String taskName;
   private String condition;
   private String originalCondition;
   private String dimensionName;
   private String dimensionAnotherName;
   private String dimensionValueName;
   private String createTime;
   private String updateTime;
   private int state;
   private String message;
   private String num;
   private DimensionTask dimensionTask;

   public long getId() {
      return this.id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public String getTaskName() {
      return this.taskName;
   }

   public void setTaskName(String taskName) {
      this.taskName = taskName;
   }

   public String getCondition() {
      return this.condition;
   }

   public void setCondition(String condition) {
      this.condition = condition;
   }

   public String getOriginalCondition() {
      return this.originalCondition;
   }

   public void setOriginalCondition(String originalCondition) {
      this.originalCondition = originalCondition;
   }

   public String getDimensionName() {
      return this.dimensionName;
   }

   public void setDimensionName(String dimensionName) {
      this.dimensionName = dimensionName;
   }

   public String getDimensionValueName() {
      return this.dimensionValueName;
   }

   public void setDimensionValueName(String dimensionValueName) {
      this.dimensionValueName = dimensionValueName;
   }

   public String getCreateTime() {
      return this.createTime;
   }

   public void setCreateTime(String createTime) {
      this.createTime = createTime;
   }

   public String getUpdateTime() {
      return this.updateTime;
   }

   public void setUpdateTime(String updateTime) {
      this.updateTime = updateTime;
   }

   public DimensionTask getDimensionTask() {
      return this.dimensionTask;
   }

   public void setDimensionTask(DimensionTask dimensionTask) {
      this.dimensionTask = dimensionTask;
   }

   public String getDimensionAnotherName() {
      return this.dimensionAnotherName;
   }

   public void setDimensionAnotherName(String dimensionAnotherName) {
      this.dimensionAnotherName = dimensionAnotherName;
   }

   public int getState() {
      return this.state;
   }

   public void setState(int state) {
      this.state = state;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public String getNum() {
      return this.num;
   }

   public void setNum(String num) {
      this.num = num;
   }
}
