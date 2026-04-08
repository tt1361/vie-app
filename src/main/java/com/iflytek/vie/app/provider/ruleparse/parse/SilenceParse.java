package com.iflytek.vie.app.provider.ruleparse.parse;

import com.google.common.base.Strings;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagProperty;
import com.iflytek.vie.app.provider.ruleparse.model.tag.Tag;
import java.util.List;

public class SilenceParse {
   protected Tag tag;
   protected String field;
   protected boolean isBefChannel;
   protected boolean isAfterChannel;
   protected String befChannel = null;
   protected String afterChannel = null;
   public String querySyntax;

   public SilenceParse(Tag tag) {
      this(tag, "silences");
   }

   public SilenceParse(Tag tag, String field) {
      if (tag.isRelative && tag.isNot) {
         this.querySyntax = null;
      } else {
         tag.containAbsolueAndSizeIsZero();
         if (tag.sizeIsZeroWithOthers) {
            this.querySyntax = null;
         } else if (tag.isAbsolue && tag.isNot) {
            this.querySyntax = null;
         } else {
            this.tag = tag;
            this.field = field;
            this.isBefChannel = tag.contain(TagPropType.BeforeChannel);
            this.isAfterChannel = tag.contain(TagPropType.AfterChannel);
            if (this.isBefChannel) {
               this.befChannel = tag.getNO1Property(TagPropType.BeforeChannel).strValue;
            }

            if (this.isAfterChannel) {
               this.afterChannel = tag.getNO1Property(TagPropType.AfterChannel).strValue;
            }

            List<TagProperty> sizeTags = tag.tagProTypeMap.get(TagPropType.Size);
            String rule = this.sizeParse();
            if (!Strings.isNullOrEmpty(rule)) {
               this.querySyntax = "(" + rule + ")";
            }
         }
      }
   }

   public String sizeParse() {
      List<TagProperty> sizeTags = this.tag.tagProTypeMap.get(TagPropType.Size);
      if (sizeTags == null) {
         System.out.println("can't match tag's type :" + this.tag.type.name);
         return null;
      } else {
         String rule = "";

         for (int i = 0; i < sizeTags.size(); i++) {
            TagProperty sizeTag = sizeTags.get(i);
            String duration = this.fillNumber(sizeTag.intValue);
            String tumRule = "";
            switch (sizeTag.operator.type) {
               case Equal:
                  tumRule = this.channelParse(this.field + " between '{0}{1}" + duration + "' and '{0}{1}" + duration.substring(0, 7) + "999'");
                  break;
               case GreaterThan:
                  if (this.tag.isNot) {
                     duration = this.fillNumber(sizeTag.intValue > 0 ? sizeTag.intValue + 1 : 0);
                  }

                  tumRule = this.channelParse(this.field + ">'{0}{1}" + duration + "' and " + this.field + "<'{0}{1}9999999999'");
                  break;
               case GTOrEqual:
                  tumRule = this.channelParse(this.field + " between '{0}{1}" + duration + "' and '{0}{1}9999999999'");
                  break;
               case LessThan:
                  if (duration.equals("0000000000")) {
                     tumRule = this.channelParse(this.field + ">'{0}{1}0000000000' and " + this.field + "< '{0}{1}0000000000'");
                  } else {
                     if (this.tag.isNot) {
                        duration = this.fillNumber(sizeTag.intValue > 0 ? sizeTag.intValue - 1 : 0);
                     }

                     tumRule = this.channelParse(this.field + ">'{0}{1}0000000000' and " + this.field + "< '{0}{1}" + duration.substring(0, 7) + "999'");
                  }
                  break;
               case LTOrEqual:
                  tumRule = this.channelParse(this.field + " between '{0}{1}0000000000' and '{0}{1}" + duration.substring(0, 7) + "999'");
            }

            if (tumRule.length() > 0) {
               if (rule.length() > 0) {
                  rule = rule + " AND (" + tumRule + ")";
               } else {
                  rule = tumRule;
               }
            }
         }

         if (this.tag.containNumIsZero()) {
            rule = rule + " OR ( " + this.field + "='000000000000')";
         } else {
            rule = rule + " AND NOT ( " + " " + this.field + "='000000000000')";
         }

         return rule;
      }
   }

   public String channelParse(String rule) {
      String tempRule = rule;
      String finalRule = "";
      String orAnd = " OR (";
      String notOrAnd = " AND NOT (";
      if (this.isBefChannel) {
         tempRule = rule.replace("{0}", this.befChannel);
      }

      if (this.isAfterChannel) {
         tempRule = rule.replace("{1}", this.afterChannel);
      }

      if (this.tag.isNot) {
         orAnd = " AND NOT (";
      }

      if (tempRule.contains("{0}") && tempRule.contains("{1}")) {
         if (this.tag.isNot) {
            finalRule = " NOT (" + tempRule.replace("{0}{1}", "00") + ")";
         } else {
            finalRule = "  (" + tempRule.replace("{0}{1}", "00") + ")";
         }

         finalRule = finalRule + orAnd + tempRule.replace("{0}{1}", "01") + ")";
         finalRule = finalRule + orAnd + tempRule.replace("{0}{1}", "02") + ")";
         finalRule = finalRule + orAnd + tempRule.replace("{0}{1}", "10") + ")";
         finalRule = finalRule + orAnd + tempRule.replace("{0}{1}", "11") + ")";
         finalRule = finalRule + orAnd + tempRule.replace("{0}{1}", "12") + ")";
         finalRule = finalRule + orAnd + tempRule.replace("{0}{1}", "20") + ")";
         finalRule = finalRule + orAnd + tempRule.replace("{0}{1}", "21") + ")";
         finalRule = finalRule + orAnd + tempRule.replace("{0}{1}", "22") + ")";
      } else if (tempRule.contains("{0}")) {
         if (this.tag.isNot) {
            finalRule = " NOT (" + tempRule.replace("{0}", "0") + ")";
         } else {
            finalRule = "(" + tempRule.replace("{0}", "0") + ")";
         }

         finalRule = finalRule + orAnd + tempRule.replace("{0}", "1") + ")";
         finalRule = finalRule + orAnd + tempRule.replace("{0}", "2") + ")";
      } else if (tempRule.contains("{1}")) {
         if (this.tag.isNot) {
            finalRule = " NOT (" + tempRule.replace("{1}", "0") + ")";
         } else {
            finalRule = "(" + tempRule.replace("{1}", "0") + ")";
         }

         finalRule = finalRule + orAnd + tempRule.replace("{1}", "0") + ")";
         finalRule = finalRule + orAnd + tempRule.replace("{1}", "1") + ")";
         finalRule = finalRule + orAnd + tempRule.replace("{1}", "2") + ")";
      }

      return finalRule;
   }

   public String fillNumber(int number) {
      String newNum = number * 1000 + "";
      return Strings.padStart(newNum, 10, '0');
   }
}
