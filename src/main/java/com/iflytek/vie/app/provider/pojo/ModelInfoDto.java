package com.iflytek.vie.app.provider.pojo;

import java.io.Serializable;
import java.util.List;

public class ModelInfoDto implements Serializable {
   private static final long serialVersionUID = 6361261006842830246L;
   private long modelId;
   private String modelName;
   private List screeningRule;
   private String updateTime;
   private int modifierId;
   private String modifierName;
   private int modelStatus;
   private String dataSource;
   private long groupId;
   private String modelCount;
   private int isUp;
   private String modelComment;
   private String modelAccuracy;
   private String releasetime;

   public String getReleasetime() {
      return this.releasetime;
   }

   public void setReleasetime(String releasetime) {
      this.releasetime = releasetime;
   }

   public String getModelName() {
      return this.modelName;
   }

   public void setModelName(String modelName) {
      this.modelName = modelName;
   }

   public List getScreeningRule() {
      return this.screeningRule;
   }

   public void setScreeningRule(List screeningRule) {
      this.screeningRule = screeningRule;
   }

   public String getUpdateTime() {
      return this.updateTime;
   }

   public void setUpdateTime(String updateTime) {
      this.updateTime = updateTime;
   }

   public String getModifierName() {
      return this.modifierName;
   }

   public void setModifierName(String modifierName) {
      this.modifierName = modifierName;
   }

   public int getModifierId() {
      return this.modifierId;
   }

   public void setModifierId(int modifierId) {
      this.modifierId = modifierId;
   }

   public String getDataSource() {
      return this.dataSource;
   }

   public void setDataSource(String dataSource) {
      this.dataSource = dataSource;
   }

   public long getModelId() {
      return this.modelId;
   }

   public void setModelId(long modelId) {
      this.modelId = modelId;
   }

   public int getModelStatus() {
      return this.modelStatus;
   }

   public void setModelStatus(int modelStatus) {
      this.modelStatus = modelStatus;
   }

   public long getGroupId() {
      return this.groupId;
   }

   public void setGroupId(long groupId) {
      this.groupId = groupId;
   }

   public String getModelCount() {
      return this.modelCount;
   }

   public void setModelCount(String modelCount) {
      this.modelCount = modelCount;
   }

   public int getIsUp() {
      return this.isUp;
   }

   public void setIsUp(int isUp) {
      this.isUp = isUp;
   }

   public String getModelComment() {
      return this.modelComment;
   }

   public void setModelComment(String modelComment) {
      this.modelComment = modelComment;
   }

   public String getModelAccuracy() {
      return this.modelAccuracy;
   }

   public void setModelAccuracy(String modelAccuracy) {
      this.modelAccuracy = modelAccuracy;
   }
}
