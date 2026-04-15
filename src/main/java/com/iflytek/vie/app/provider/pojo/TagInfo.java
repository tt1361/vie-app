package com.iflytek.vie.app.provider.pojo;

public class TagInfo {
   private int beginTime;
   private int endTime;
   private String content = "";
   private int type;
   private String position = "";

   public int getBeginTime() {
      return this.beginTime;
   }

   public void setBeginTime(int beginTime) {
      this.beginTime = beginTime;
   }

   public int getEndTime() {
      return this.endTime;
   }

   public void setEndTime(int endTime) {
      this.endTime = endTime;
   }

   public String getContent() {
      return this.content;
   }

   public void setContent(String content) {
      this.content = content;
   }

   public int getType() {
      return this.type;
   }

   public void setType(int type) {
      this.type = type;
   }

   public String getPosition() {
      return this.position;
   }

   public void setPosition(String position) {
      this.position = position;
   }
}
