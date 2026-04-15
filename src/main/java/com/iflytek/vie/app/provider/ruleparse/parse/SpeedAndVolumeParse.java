package com.iflytek.vie.app.provider.ruleparse.parse;

import com.google.common.base.Strings;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagProperty;
import com.iflytek.vie.app.provider.ruleparse.model.tag.Tag;
import java.util.List;

public class SpeedAndVolumeParse extends SilenceParse {
   public SpeedAndVolumeParse(Tag tag) {
      super(tag);
   }

   @Override
   public String sizeParse() {
      List<TagProperty> sizeTags = null;
      boolean isVolume = false;
      switch (this.tag.type) {
         case Speed:
            this.field = this.tag.contain(TagPropType.Channel) && this.tag.getNO1Property(TagPropType.Channel).strValue.equals("1") ? "n1Speeds" : "n0Speeds";
            sizeTags = this.tag.tagProTypeMap.get(TagPropType.SingleSpeed);
            break;
         case Volume:
            isVolume = true;
            this.field = this.tag.contain(TagPropType.Channel) && this.tag.getNO1Property(TagPropType.Channel).strValue.equals("1") ? "n1Energys" : "n0Energys";
            sizeTags = this.tag.tagProTypeMap.get(TagPropType.Size);
      }

      if (sizeTags == null) {
         System.out.println("can't match tag's type :" + this.tag.type.name);
         return null;
      } else {
         String orAnd = " AND (";
         String rule = "";
         if (this.tag.isNot && !this.tag.sizeEqualZero()) {
            orAnd = " AND NOT (";
         }

         for (int i = 0; i < sizeTags.size(); i++) {
            TagProperty sizeTag = sizeTags.get(i);
            String duration = isVolume ? this.fillNumber(sizeTag.intValue) : this.fillNumber(sizeTag.doubleValue);
            String tumRule = "";
            switch (sizeTag.operator.type) {
               case Equal:
                  tumRule = this.field + " ='00" + duration + "'";
                  break;
               case GreaterThan:
                  tumRule = this.field + ">'00" + duration + "'";
                  break;
               case GTOrEqual:
                  tumRule = this.field + " between '00" + duration + "' and '009999999999'";
                  break;
               case LessThan:
                  tumRule = this.field + "< '00" + duration + "'";
                  break;
               case LTOrEqual:
                  tumRule = this.field + " between '000000000000' and '00" + duration + "'";
                  break;
               case NotEqual:
                  tumRule = this.field + " !='00" + duration + "'";
            }

            if (tumRule.length() > 0) {
               if (rule.length() > 0) {
                  rule = rule + orAnd + tumRule + ")";
               } else if (this.tag.isNot) {
                  rule = " NOT (" + tumRule + ")";
               } else {
                  rule = tumRule;
               }
            }
         }

         return rule;
      }
   }

   public String fillNumber(double number) {
      String newNum = (int)(number * 1000.0) + "";
      return Strings.padStart(newNum, 10, '0');
   }
}
