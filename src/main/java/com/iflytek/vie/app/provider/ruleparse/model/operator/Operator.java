package com.iflytek.vie.app.provider.ruleparse.model.operator;

public abstract class Operator<T> {
   public OperatorType type;

   public Operator() {
   }

   public Operator(OperatorType type) {
      this.type = type;
   }

   public boolean compare(T value1, T value2, T... value3) {
      return true;
   }
}
