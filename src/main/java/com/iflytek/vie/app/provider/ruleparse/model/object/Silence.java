package com.iflytek.vie.app.provider.ruleparse.model.object;

import com.iflytek.vie.app.provider.ruleparse.model.tag.TagType;

public class Silence extends SpeechSegment {
   public Silence(int duration, String befChannel, String afterChannel, int beginTime, int endTime) {
      super(TagType.Silence, duration, befChannel, afterChannel, beginTime, endTime);
   }
}
