package com.iflytek.vie.app.provider.ruleparse.model.object;

import com.iflytek.vie.app.provider.ruleparse.model.tag.TagType;

public class Interject extends SpeechSegment {
   public Interject(int duration, String befChannel, String afterChannel, int beginTime, int endTime) {
      super(TagType.Interject, duration, befChannel, afterChannel, beginTime, endTime);
   }
}
