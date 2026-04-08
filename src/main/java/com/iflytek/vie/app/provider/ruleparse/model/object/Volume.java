package com.iflytek.vie.app.provider.ruleparse.model.object;

import com.iflytek.vie.app.provider.ruleparse.model.tag.TagType;

public class Volume extends SpeechSegment {
   public Volume(int duration, String befChannel, String afterChannel, int beginTime, int endTime) {
      super(TagType.Volume, duration, befChannel, afterChannel, beginTime, endTime);
   }
}
