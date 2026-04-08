package com.iflytek.vie.app.provider.ruleparse.parse;

import com.iflytek.vie.app.provider.ruleparse.model.tag.Tag;

public class InterjectParse extends SilenceParse {
   public InterjectParse(Tag tag) {
      super(tag, "interrupted");
   }
}
