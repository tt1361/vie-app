package com.iflytek.vie.app.provider.ruleparse.model.operator;

public class DoubleOperator extends Operator<Double> {
   public DoubleOperator(OperatorType type) {
      super(type);
   }

   public boolean compare(Double value1, Double value2, Double... value3) {
      switch (this.type) {
         case GreaterThan:
            return value1 > value2;
         case LessThan:
            return value1 < value2;
         case Equal:
            return value1 == value2;
         case GTOrEqual:
            return value1 >= value2;
         case LTOrEqual:
            return value1 <= value2;
         case NotEqual:
            return value1 != value2;
         case RelativeBefore: {
            double dif = value2 - value1;
            return dif >= 0.0 && dif <= value3[0];
         }
         case RelativeAfter:
            double var6 = value1 - value3[0];
            return var6 >= 0.0 && var6 <= value3[1];
         case EndSpeech: {
            double dif = value2 - value1;
            return dif >= 0.0 && dif <= value3[0];
         }
         default:
            return false;
      }
   }
}
