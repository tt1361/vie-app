package com.iflytek.vie.app.provider.ruleparse.model.object;

import com.iflytek.vie.app.provider.ruleparse.model.tag.TagType;

public class Speed extends SpeechSegment {
   public Speed(int duration, String befChannel, String afterChannel, int beginTime, int endTime) {
      super(TagType.Speed, duration, befChannel, afterChannel, beginTime, endTime);
   }
}
