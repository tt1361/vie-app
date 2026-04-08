package com.iflytek.vie.app.provider.pojo;

import java.io.Serializable;

public class TopicModelDTO implements Serializable {
   private static final long serialVersionUID = 3984085130837917798L;
   private Long modelId;
   private String modelName;
   private Long callCount;
   private String callPercent;
   private int modelOrGroup;
   private Long unReadCall;
   private Long readCall;

   public Long getModelId() {
      return this.modelId;
   }

   public void setModelId(Long modelId) {
      this.modelId = modelId;
   }

   public String getModelName() {
      return this.modelName;
   }

   public void setModelName(String modelName) {
      this.modelName = modelName;
   }

   public Long getCallCount() {
      return this.callCount;
   }

   public void setCallCount(Long callCount) {
      this.callCount = callCount;
   }

   public String getCallPercent() {
      return this.callPercent;
   }

   public void setCallPercent(String callPercent) {
      this.callPercent = callPercent;
   }

   public int getModelOrGroup() {
      return this.modelOrGroup;
   }

   public void setModelOrGroup(int modelOrGroup) {
      this.modelOrGroup = modelOrGroup;
   }

   public Long getUnReadCall() {
      return this.unReadCall;
   }

   public void setUnReadCall(Long unReadCall) {
      this.unReadCall = unReadCall;
   }

   public Long getReadCall() {
      return this.readCall;
   }

   public void setReadCall(Long readCall) {
      this.readCall = readCall;
   }
}
