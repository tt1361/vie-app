package com.iflytek.vie.app.provider.ruleparse.model.object;

import com.iflytek.vie.app.provider.ruleparse.model.tag.TagType;

public abstract class SpeechElement {
   public TagType type;
   public int beginTime;
   public int endTime;

   public SpeechElement(TagType type) {
      this.type = type;
   }

   public String getId() {
      return this.type.code + "_" + this.beginTime + "_" + this.endTime;
   }
}
