package com.iflytek.vie.app.provider.ruleparse.model.operator;

public class StringOperator extends Operator<String> {
   public StringOperator(OperatorType type) {
      super(type);
   }

   public boolean compare(String value1, String value2, String... value3) {
      switch (this.type) {
         case SeatChannel:
            return value1.equalsIgnoreCase("0");
         case CusChannel:
            return value1.equalsIgnoreCase("1");
         case RelativeBefore:
            return value1.equalsIgnoreCase(value2);
         case RelativeAfter:
            return value1.equalsIgnoreCase(value2);
         case Equal:
            return value1.equalsIgnoreCase(value2);
         default:
            return false;
      }
   }
}
