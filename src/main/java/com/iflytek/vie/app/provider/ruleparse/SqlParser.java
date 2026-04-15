package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.List;

public abstract class SqlParser implements Serializable {
   private String sql;
   private String tableName;
   private String textRule;
   private int channel;
   private List<Filter> filters;

   public SqlParser(String textRule, int channel, List<Filter> filters, String tableName) {
      this.textRule = textRule;
      this.channel = channel;
      this.filters = filters;
      this.tableName = tableName;
   }

   public abstract Object parseSql(String var1);

   public String parseTextRule() {
      if (this.textRule != null && !"".equals(this.textRule) && !"#".equals(this.textRule) && !"(#)".equals(this.textRule)) {
         StringBuffer conditionBuffer = new StringBuffer();
         conditionBuffer.append("fulltext('(");
         switch (this.channel) {
            case 0:
               conditionBuffer.append("contentN0:(");
               break;
            case 1:
               conditionBuffer.append("contentN1:(");
               break;
            default:
               conditionBuffer.append("content:(");
         }

         conditionBuffer.append(this.textRule + "))','content-word-query-parser')=true");
         return conditionBuffer.toString();
      } else {
         return "";
      }
   }

   public String parseFilters() {
      if (this.filters != null && this.filters.size() != 0) {
         StringBuffer conditionBuffer = new StringBuffer();
         int count = 0;

         for (int i = 0; i < this.filters.size(); i++) {
            Filter filter = this.filters.get(i);
            String condition = filter.toSqlCondition();
            if (condition != null && !"".equals(condition)) {
               if (count++ != 0) {
                  conditionBuffer.append(" and ");
               }

               conditionBuffer.append("(" + condition + ")");
            }
         }

         return conditionBuffer.length() == 0 ? conditionBuffer.toString() : conditionBuffer.append(" and (processed=0) ").toString();
      } else {
         return "";
      }
   }

   public String getSuccessDataSql() {
      return " processed=0 ";
   }

   public String getTableName() {
      return this.tableName;
   }

   public void setTableName(String tableName) {
      this.tableName = tableName;
   }

   public String getTextRule() {
      return this.textRule;
   }

   public void setTextRule(String textRule) {
      this.textRule = textRule;
   }

   public int getChannel() {
      return this.channel;
   }

   public void setChannel(int channel) {
      this.channel = channel;
   }

   public List<Filter> getFilters() {
      return this.filters;
   }

   public void setFilters(List<Filter> filters) {
      this.filters = filters;
   }

   public String getSql() {
      return this.sql;
   }

   public void setSql(String sql) {
      this.sql = sql;
   }
}
