package com.iflytek.vie.app.provider.ruleparse.parse;

import com.google.common.base.Strings;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagProperty;
import com.iflytek.vie.app.provider.ruleparse.model.tag.Tag;
import java.util.List;

public class VolumeParse extends SilenceParse {
   public VolumeParse(Tag tag) {
      super(tag, tag.contain(TagPropType.Channel) && tag.getNO1Property(TagPropType.Channel).strValue.equals("1") ? "n1Energys" : "n0Energys");
   }

   @Override
   public String sizeParse() {
      List<TagProperty> sizeTags = this.tag.tagProTypeMap.get(TagPropType.Size);
      String orAnd = " AND (";
      String rule = "";
      if (this.tag.isNot && !this.tag.sizeEqualZero()) {
         orAnd = " AND NOT (";
      }

      for (int i = 0; i < sizeTags.size(); i++) {
         TagProperty sizeTag = sizeTags.get(i);
         String duration = this.fillNumber(sizeTag.doubleValue);
         String tumRule = "";
         switch (sizeTag.operator.type) {
            case Equal:
               tumRule = this.field + " between '00" + duration + "' and '00" + duration.substring(0, 7) + "999'";
               break;
            case GreaterThan:
               tumRule = this.field + ">'00" + duration + "' and " + this.field + "<'009999999999'";
               break;
            case GTOrEqual:
               tumRule = this.field + " between '00" + duration + "' and '009999999999'";
               break;
            case LessThan:
               tumRule = this.field + ">'000000000000' and " + this.field + "< '00" + duration.substring(0, 7) + "999'";
               break;
            case LTOrEqual:
               tumRule = this.field + " between '000000000000' and '00" + duration.substring(0, 7) + "999'";
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

   public String fillNumber(double number) {
      String newNum = (int)number * 1000 + "";
      return Strings.padStart(newNum, 10, '0');
   }
}
