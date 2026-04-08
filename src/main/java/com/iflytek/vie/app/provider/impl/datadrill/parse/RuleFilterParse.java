package com.iflytek.vie.app.provider.impl.datadrill.parse;

import com.iflytek.vie.app.pojo.datadrill.FilterRuleEnum;
import com.iflytek.vie.app.provider.engine.FieldType;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class RuleFilterParse extends FilterParse {
   private static List<String> durationMap = new ArrayList<>();

   @Override
   public String getSql() {
      String sql = "";
      switch (this.ruleEnum) {
         case IN:
            sql = this.key + " in (" + this.toCondition() + ")";
            break;
         case NOT_IN:
            sql = this.key + " not in (" + this.toCondition() + ")";
            break;
         case FULL_TEXT:
            if (durationMap.contains(this.key)) {
               sql = this.toFullTextRange(true);
            } else {
               sql = "fulltext('+" + this.key + ":" + this.toFullText() + "')=true";
            }
            break;
         case NO_FULL_TEXT:
            if (durationMap.contains(this.key)) {
               sql = this.toFullTextRange(false);
            } else {
               sql = "fulltext('-" + this.key + ":" + this.toFullText() + "')=true";
            }
            break;
         case EQUAL:
            sql = this.key + "=" + this.toCondition();
            break;
         case NOT_EQUAL:
            sql = this.key + "!=" + this.toCondition();
            break;
         case RANGE:
            sql = this.key + " " + this.toRange();
            break;
         case GREAT_THAN:
            sql = this.key + ">" + this.toCondition();
            break;
         case LITTLE_THAN:
            sql = this.key + "<" + this.toCondition();
            break;
         case GREAT_EQUAL:
            sql = this.key + ">=" + this.toCondition();
            break;
         case LITTLE_EQUAL:
            sql = this.key + "<=" + this.toCondition();
      }

      return sql;
   }

   private String toFullTextRange(boolean isContain) {
      List valueList = (List)this.value;
      if (valueList != null && valueList.size() != 0) {
         String sql = "";

         for (int i = 0; i < valueList.size(); i++) {
            String val = (String)valueList.get(i);
            if (val.indexOf(".") <= -1) {
               int startInt = Integer.parseInt(val);
               int endInt = startInt + 1;
               if (isContain) {
                  sql = sql + "(" + this.key + ">" + startInt * 1000 + " and " + this.key + "<" + endInt * 1000 + ")";
               } else {
                  sql = sql + "(" + this.key + ">" + endInt * 1000 + " or " + this.key + "<" + startInt * 1000 + ")";
               }
            } else {
               int numStr = val.length() - val.indexOf(".") - 1;
               float fStr = Float.parseFloat(val);
               String s1 = "0.";

               for (int j = 1; j < numStr; j++) {
                  s1 = s1 + "0";
               }

               s1 = s1 + "1";
               float endStr = fStr + Float.parseFloat(s1);
               if (isContain) {
                  sql = sql + "(" + this.key + ">=" + fStr * 1000.0F + " and " + this.key + "<" + endStr * 1000.0F + ")";
               } else {
                  sql = sql + "(" + this.key + ">" + endStr * 1000.0F + " or " + this.key + "<" + fStr * 1000.0F + ")";
               }
            }

            if (i < valueList.size() - 1) {
               sql = sql + " or ";
            }
         }

         return sql;
      } else {
         return "!error[normal dimension " + this.key + " 's value must be not null!]";
      }
   }

   public String toRange() {
      List valueList = (List)this.value;
      if (valueList != null && valueList.size() != 0) {
         String condition = "";

         try {
            switch (this.fieldType) {
               case timestamp:
                  if (valueList.size() != 2) {
                     throw new RuntimeException("range value size must two! " + this.key + " length is :" + valueList.size());
                  }

                  try {
                     long startTime = SDF.parse(String.valueOf(this.toObject(valueList.get(0)))).getTime();
                     long endTime = SDF.parse(String.valueOf(this.toObject(valueList.get(1)))).getTime();
                     condition = "between " + startTime + " and " + endTime + " ";
                     break;
                  } catch (ParseException var10) {
                     throw new RuntimeException("range value parse error! " + this.key + " value0 is:" + valueList.get(0) + ",value1 is:" + valueList.get(1));
                  }
               case time:
                  if (valueList.size() != 2) {
                     throw new RuntimeException("range value size must 2! now " + this.key + " length is :" + valueList.size());
                  }

                  try {
                     String startTime = "'" + this.toObject(valueList.get(0)) + "'";
                     String endTime = "'" + this.toObject(valueList.get(1)) + "'";
                     condition = "between " + startTime + " and " + endTime + " ";
                     break;
                  } catch (Exception var9) {
                     throw new RuntimeException(
                        "range value parse error! now " + this.key + " value0 is:" + valueList.get(0) + ",value1 is:" + valueList.get(1)
                     );
                  }
               case string:
                  if (valueList.size() != 2) {
                     throw new RuntimeException("range value size must 2! now " + this.key + " length is :" + valueList.size());
                  }

                  try {
                     String startTime = "'" + this.toObject(valueList.get(0)) + "'";
                     String endTime = "'" + this.toObject(valueList.get(1)) + "'";
                     condition = "between " + startTime + " and " + endTime + " ";
                     break;
                  } catch (Exception var8) {
                     throw new RuntimeException(
                        "range value parse error! now " + this.key + " value0 is:" + valueList.get(0) + ",value1 is:" + valueList.get(1)
                     );
                  }
               case integer:
                  if (valueList.size() != 2) {
                     throw new RuntimeException("range value size must 2! now " + this.key + " length is :" + valueList.size());
                  }

                  try {
                     double startTime = DF.parse(String.valueOf(this.toObject(valueList.get(0)))).doubleValue();
                     double endTime = DF.parse(String.valueOf(this.toObject(valueList.get(1)))).doubleValue();
                     if (durationMap.contains(this.key)) {
                        condition = "between " + startTime * 1000.0 + " and " + endTime * 1000.0 + " ";
                     } else {
                        condition = "between " + startTime + " and " + endTime + " ";
                     }
                  } catch (ParseException var7) {
                     throw new RuntimeException(
                        "range value parse error! now " + this.key + " value0 is:" + valueList.get(0) + ",value1 is:" + valueList.get(1)
                     );
                  }
            }
         } catch (RuntimeException var11) {
            String errorMsg = var11.getMessage();
            return "!error[" + errorMsg + "]";
         }

         return condition.substring(0, condition.length() - 1);
      } else {
         return "!error[normal range dimension " + this.key + " 's value must be not null!]";
      }
   }

   @Override
   public String toCondition() {
      List valueList = (List)this.value;
      if (valueList != null && valueList.size() != 0) {
         String condition = "";
         switch (this.fieldType) {
            case time:
               for (int i = 0; i < valueList.size(); i++) {
                  condition = condition + "'" + this.toObject(valueList.get(i)) + "',";
               }
               break;
            case string:
               for (int ixx = 0; ixx < valueList.size(); ixx++) {
                  condition = condition + "'" + valueList.get(ixx) + "',";
               }
               break;
            case integer:
               for (int i = 0; i < valueList.size(); i++) {
                  if (durationMap.contains(this.key)) {
                     condition = condition + DF_NOEND.format(this.toDouble(valueList.get(i)) * 1000.0) + ",";
                  } else {
                     condition = condition + this.toObject(valueList.get(i)) + ",";
                  }
               }
               break;
            case numerical:
               for (int ix = 0; ix < valueList.size(); ix++) {
                  if (durationMap.contains(this.key)) {
                     condition = condition + this.toDouble(valueList.get(ix)) * 1000.0 + ",";
                  } else {
                     condition = condition + this.toObject(valueList.get(ix)) + ",";
                  }
               }
         }

         return condition.substring(0, condition.length() - 1);
      } else {
         return "!error[normal dimension " + this.key + " 's value must be not null!]";
      }
   }

   public String toFullText() {
      List valueList = (List)this.value;
      if (valueList != null && valueList.size() != 0) {
         String condition = "(";

         for (int i = 0; i < valueList.size(); i++) {
            if (durationMap.contains(this.key)) {
               condition = condition + "*" + DF_NOEND.format(this.toDouble(valueList.get(i)) * 1000.0) + "* or ";
            } else {
               String fulltext = this.toObject(valueList.get(i)).toString();

               for (int slen = 0; slen < SPECIALCHARACTERS.length; slen++) {
                  if (fulltext.contains(SPECIALCHARACTERS[slen])) {
                     fulltext = fulltext.replace(SPECIALCHARACTERS[slen], "\\" + SPECIALCHARACTERS[slen]);
                  }
               }

               condition = condition + "*" + fulltext + "* or ";
            }
         }

         return condition.substring(0, condition.length() - 4) + ")";
      } else {
         return "!error[normal fulltext dimension " + this.key + " 's value must be not null!]";
      }
   }

   public RuleFilterParse(String key, Object value, FilterRuleEnum ruleEnum, FieldType fieldType) {
      super(key, value, ruleEnum, fieldType);
   }

   static {
      durationMap.add("duration");
      durationMap.add("silenceLong");
      durationMap.add("vadDuration");
      durationMap.add("inVadDuration");
      durationMap.add("seatDuration");
      durationMap.add("customDuration");
      durationMap.add("n1avgSpeed");
      durationMap.add("n0avgSpeed");
   }
}
