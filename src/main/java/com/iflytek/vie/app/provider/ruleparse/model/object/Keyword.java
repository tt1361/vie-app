package com.iflytek.vie.app.provider.ruleparse.model.object;

import com.iflytek.vie.app.provider.ruleparse.model.tag.TagType;

public class Keyword extends SpeechElement {
   public String keyword;
   public int beginPosition;
   public int endPosition;
   public int position;
   public boolean hasPosition = false;

   public Keyword(String keyword, int beginPosition, int endPosition) {
      super(TagType.Keyword);
      this.keyword = keyword;
      this.beginPosition = beginPosition;
      this.endPosition = endPosition;
   }

   @Override
   public String toString() {
      return this.hasPosition
         ? this.keyword + "-" + this.beginPosition + "-" + this.endPosition + "-" + this.position
         : this.keyword + "-" + this.beginPosition + "-" + this.endPosition;
   }
}
