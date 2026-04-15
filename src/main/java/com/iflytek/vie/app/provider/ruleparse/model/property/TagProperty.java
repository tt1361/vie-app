package com.iflytek.vie.app.provider.ruleparse.model.property;

import com.iflytek.vie.app.provider.ruleparse.model.operator.Operator;
import com.iflytek.vie.app.provider.ruleparse.parse.TextRuleParse;

public class TagProperty {
   public TagPropType type;
   public Operator operator;
   public String strValue;
   public int intValue;
   public double doubleValue;
   public TextRuleParse textRuleParse;

   public <T> boolean compare(T value1, T value2, T... value3) {
      return this.operator.compare(value1, value2, value3);
   }

   public boolean compareInt(Integer value1, Integer... value3) {
      return this.operator.compare(value1, this.intValue, value3);
   }

   public boolean compareDouble(Double value1, Double... value3) {
      return this.operator.compare(value1, this.doubleValue, value3);
   }

   public boolean compareString(String value1, String... value3) {
      return this.operator.compare(value1, this.strValue, value3);
   }
}
