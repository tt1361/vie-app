package com.iflytek.vie.app.provider.pojo;

import com.iflytek.vie.app.pojo.topic.TopicInfo;

public class TopicMarkInfo {
   private int id = 0;
   private String telephoneId;
   private TopicInfo topic;
   private int isNormal;
   private String mark;

   public int getId() {
      return this.id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public String getTelephoneId() {
      return this.telephoneId;
   }

   public void setTelephoneId(String telephoneId) {
      this.telephoneId = telephoneId;
   }

   public TopicInfo getTopic() {
      return this.topic;
   }

   public void setTopic(TopicInfo topic) {
      this.topic = topic;
   }

   public int getIsNormal() {
      return this.isNormal;
   }

   public void setIsNormal(int isNormal) {
      this.isNormal = isNormal;
   }

   public String getMark() {
      return this.mark;
   }

   public void setMark(String mark) {
      this.mark = mark;
   }
}
