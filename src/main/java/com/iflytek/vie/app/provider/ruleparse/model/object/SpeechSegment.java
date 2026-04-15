package com.iflytek.vie.app.provider.ruleparse.model.object;

import com.iflytek.vie.app.provider.ruleparse.model.tag.TagType;

public class SpeechSegment extends SpeechElement {
   public int duration;
   public String befChannel;
   public String afterChannel;

   public SpeechSegment(TagType tagType, int duration, String befChannel, String afterChannel, int beginTime, int endTime) {
      super(tagType);
      this.duration = duration;
      this.befChannel = befChannel;
      this.afterChannel = afterChannel;
      this.beginTime = beginTime;
      this.endTime = endTime;
   }

   @Override
   public String toString() {
      return this.duration + "-" + this.beginTime + "-" + this.endTime;
   }
}
