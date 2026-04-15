package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;

public class SpecDimValue implements Serializable {
   private String key;
   private String value;

   public String getValue() {
      return this.value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public String getKey() {
      return this.key;
   }

   public void setKey(String key) {
      this.key = key;
   }
}
