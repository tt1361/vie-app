package com.iflytek.vie.app.provider.pojo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class DimensionTask implements Serializable {
   private long taskId;
   private String taskName;
   private String message;
   private int taskState;
   private String createTime;
   private String updateTime;
   private String createName;
   private Set<DimensionPersonalTask> dpSet = new HashSet<>();

   public long getTaskId() {
      return this.taskId;
   }

   public void setTaskId(long taskId) {
      this.taskId = taskId;
   }

   public String getTaskName() {
      return this.taskName;
   }

   public void setTaskName(String taskName) {
      this.taskName = taskName;
   }

   public String getMessage() {
      return this.message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public int getTaskState() {
      return this.taskState;
   }

   public void setTaskState(int taskState) {
      this.taskState = taskState;
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

   public String getCreateName() {
      return this.createName;
   }

   public void setCreateName(String createName) {
      this.createName = createName;
   }

   public Set<DimensionPersonalTask> getDpSet() {
      return this.dpSet;
   }

   public void setDpSet(Set<DimensionPersonalTask> dpSet) {
      this.dpSet = dpSet;
   }
}
