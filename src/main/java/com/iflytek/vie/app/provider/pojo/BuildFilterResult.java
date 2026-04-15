package com.iflytek.vie.app.provider.pojo;

import com.iflytek.vie.app.pojo.datadrill.FilterField;
import java.util.Map;

public class BuildFilterResult {
   private String expression;
   private Map<String, FilterField> filterFields;

   public String getExpression() {
      return this.expression;
   }

   public void setExpression(String expression) {
      this.expression = expression;
   }

   public Map<String, FilterField> getFilterFields() {
      return this.filterFields;
   }

   public void setFilterFields(Map<String, FilterField> filterFields) {
      this.filterFields = filterFields;
   }
}
