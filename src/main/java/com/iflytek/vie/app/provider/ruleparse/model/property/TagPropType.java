package com.iflytek.vie.app.provider.ruleparse.model.property;

public enum TagPropType {
   Channel("ATR001"),
   BeforeChannel("ATR007"),
   AfterChannel("ATR008"),
   NumOfOccurrences("ATR002"),
   Size("ATR003"),
   List("ATR005"),
   RelativePosition("ATR004"),
   AbsolutePosition("ATR009"),
   SingleSpeed("ATR010"),
   AvgSpeed("ATR011");

   private String code;

   private TagPropType(String code) {
      this.code = code;
   }

   public static TagPropType getType(String code) {
      if (code.equalsIgnoreCase("ATR001")) {
         return Channel;
      } else if (code.equalsIgnoreCase("ATR002")) {
         return NumOfOccurrences;
      } else if (code.equalsIgnoreCase("ATR003")) {
         return Size;
      } else if (code.equalsIgnoreCase("ATR004")) {
         return RelativePosition;
      } else if (code.equalsIgnoreCase("ATR005")) {
         return List;
      } else if (code.equalsIgnoreCase("ATR007")) {
         return BeforeChannel;
      } else if (code.equalsIgnoreCase("ATR008")) {
         return AfterChannel;
      } else if (code.equalsIgnoreCase("ATR009")) {
         return AbsolutePosition;
      } else if (code.equalsIgnoreCase("ATR010")) {
         return SingleSpeed;
      } else {
         return code.equalsIgnoreCase("ATR011") ? AvgSpeed : null;
      }
   }
}
