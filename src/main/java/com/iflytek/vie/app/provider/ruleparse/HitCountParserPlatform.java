package com.iflytek.vie.app.provider.ruleparse;

import com.iflytek.vie.utils.StringUtils;
import java.io.Serializable;
import java.util.List;

class HitCountParserPlatform extends SqlParserPlatform implements Serializable {
   private static final long serialVersionUID = -4670755560629098669L;
   private boolean isFilter;
   private String dataLimit;
   private String otherRuleText;
   private int searchType;
   private String textRule;
   private int channel;

   public HitCountParserPlatform(
      String textRule, int channel, List<FilterPlatform> filters, String tableName, boolean isFilter, String dataLimitSql, String otherRuleText, int searchType
   ) {
      super(textRule, channel, filters, tableName);
      this.isFilter = isFilter;
      this.dataLimit = dataLimitSql;
      this.otherRuleText = otherRuleText;
      this.searchType = searchType;
      this.textRule = textRule;
      this.channel = channel;
   }

   public Object parseSql() {
      return this.parseSql(null);
   }

   @Override
   public Object parseSql(String dataSource) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("select sum(idCounter) as hitCount from " + this.getTableName());
      String filterCondition = this.parseFilters();
      String textCondition = this.parseTextRule();
      if (this.isFilter) {
         if (filterCondition != null && !"".equals(filterCondition)) {
            sqlBuffer.append(" where " + filterCondition);
         }
      } else {
         if ((filterCondition == null || "".equals(filterCondition)) && (textCondition == null || "".equals(textCondition))) {
            return sqlBuffer.toString();
         }

         sqlBuffer.append(" where ").append(!StringUtils.isNullOrEmpry(this.dataLimit) ? this.dataLimit : "processed=0 ");
         if ((filterCondition == null || "".equals(filterCondition)) && textCondition != null && !"".equals(textCondition)) {
            sqlBuffer.append(" and ").append(textCondition);
         } else if (filterCondition == null || "".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
            sqlBuffer.append(" and (").append(filterCondition + ") and (" + textCondition + ")");
         } else {
            sqlBuffer.append(" and ").append(filterCondition);
         }
      }

      if (!StringUtils.isNullOrEmpry(this.otherRuleText)) {
         sqlBuffer.append(" and " + this.otherRuleText);
      }

      this.setSql(sqlBuffer.toString());
      return sqlBuffer.toString();
   }

   @Override
   public Object parseFragmentSql(String dataSource) {
      return null;
   }

   @Override
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

         if (this.searchType == 0) {
            conditionBuffer.append(this.textRule + "))','content-word-query-parser')=true");
         } else {
            conditionBuffer.append(this.textRule + "))','content-query-parser')=true");
         }

         return conditionBuffer.toString();
      } else {
         return "";
      }
   }
}
