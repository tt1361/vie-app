package com.iflytek.vie.app.provider.ruleparse.model.operator;

public enum OperatorType {
   GreaterThan("OP001"),
   Equal("OP002"),
   LessThan("OP003"),
   GTOrEqual("OP004"),
   LTOrEqual("OP005"),
   NotEqual("OP006"),
   RelativeBefore("OP007"),
   RelativeAfter("OP008"),
   EndSpeech("OP009"),
   SeatChannel("n0"),
   CusChannel("n1");

   private String code;

   private OperatorType(String code) {
      this.code = code;
   }

   public static OperatorType getType(String code) {
      if (code.equalsIgnoreCase("OP001")) {
         return GreaterThan;
      } else if (code.equalsIgnoreCase("OP002")) {
         return Equal;
      } else if (code.equalsIgnoreCase("OP003")) {
         return LessThan;
      } else if (code.equalsIgnoreCase("OP004")) {
         return GTOrEqual;
      } else if (code.equalsIgnoreCase("OP005")) {
         return LTOrEqual;
      } else if (code.equalsIgnoreCase("OP006")) {
         return NotEqual;
      } else if (code.equalsIgnoreCase("OP007")) {
         return RelativeBefore;
      } else if (code.equalsIgnoreCase("OP008")) {
         return RelativeAfter;
      } else if (code.equalsIgnoreCase("OP009")) {
         return EndSpeech;
      } else if (code.equalsIgnoreCase("n0")) {
         return SeatChannel;
      } else {
         return code.equalsIgnoreCase("n1") ? CusChannel : null;
      }
   }
}
