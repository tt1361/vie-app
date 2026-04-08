package com.iflytek.vie.app.provider.ruleparse.parse;

import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagProperty;
import com.iflytek.vie.app.provider.ruleparse.model.tag.Tag;

public class DurationParse {
   private Tag tag;

   public DurationParse(Tag tag) {
      this.tag = tag;
   }

   public String parse() {
      StringBuffer filter = new StringBuffer();

      for (TagProperty prop : this.tag.tagProTypeMap.get(TagPropType.Size)) {
         switch (prop.operator.type) {
            case GreaterThan:
               filter.append("duration>");
               filter.append(prop.intValue * 1000);
               filter.append(" OR ");
               break;
            case LessThan:
               filter.append("duration<");
               filter.append(prop.intValue * 1000);
               filter.append(" OR ");
               break;
            case Equal:
               filter.append("duration=");
               filter.append(prop.intValue * 1000);
               filter.append(" OR ");
               break;
            case GTOrEqual:
               filter.append("duration>=");
               filter.append(prop.intValue * 1000);
               filter.append(" OR ");
               break;
            case LTOrEqual:
               filter.append("duration<=");
               filter.append(prop.intValue * 1000);
               filter.append(" OR ");
               break;
            case NotEqual:
               filter.append("duration!=");
               filter.append(prop.intValue * 1000);
               filter.append(" OR ");
         }
      }

      String filterStr = filter.toString();
      filterStr = filterStr.substring(0, filterStr.length() - 4);
      return this.tag.isNot ? "NOT (" + filterStr + ")" : "(" + filterStr + ")";
   }
}
