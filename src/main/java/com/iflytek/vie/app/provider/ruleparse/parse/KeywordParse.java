package com.iflytek.vie.app.provider.ruleparse.parse;

import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.model.tag.Tag;

public class KeywordParse {
   private Tag tag;

   public KeywordParse(Tag tag) {
      this.tag = tag;
   }

   public String parse(String contentField) {
      if (this.tag.isNot && this.tag.isRelative) {
         return null;
      } else {
         String rule = this.tag.getNO1Property(TagPropType.List).textRuleParse.rule;
         rule = contentField + ":(" + rule + ")";
         if (this.tag.isNot && !this.tag.isAbsolue) {
            rule = "-(" + rule + ")";
         } else {
            rule = "+(" + rule + ")";
         }

         return rule;
      }
   }
}
