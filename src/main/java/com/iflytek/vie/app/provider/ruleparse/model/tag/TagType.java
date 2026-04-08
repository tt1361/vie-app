package com.iflytek.vie.app.provider.ruleparse.model.tag;

public enum TagType {
   Keyword("OBJ001"),
   Silence("OBJ002"),
   Speed("OBJ003"),
   Volume("OBJ004"),
   Interject("OBJ005"),
   Duration("OBJ006");

   public String code;
   public String name;

   private TagType(String code) {
      this.code = code;
      this.name = this.name().toLowerCase();
   }

   public static TagType getType(String code) {
      if (code.equalsIgnoreCase("OBJ001")) {
         return Keyword;
      } else if (code.equalsIgnoreCase("OBJ002")) {
         return Silence;
      } else if (code.equalsIgnoreCase("OBJ003")) {
         return Speed;
      } else if (code.equalsIgnoreCase("OBJ004")) {
         return Volume;
      } else if (code.equalsIgnoreCase("OBJ005")) {
         return Interject;
      } else {
         return code.equalsIgnoreCase("OBJ006") ? Duration : null;
      }
   }
}
