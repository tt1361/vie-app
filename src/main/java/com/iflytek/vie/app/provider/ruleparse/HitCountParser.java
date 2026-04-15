package com.iflytek.vie.app.provider.ruleparse;

import com.iflytek.vie.utils.StringUtils;
import java.io.Serializable;
import java.util.List;

public class HitCountParser extends SqlParser implements Serializable {
   private boolean isFilter;
   private String dataLimit;
   private String otherRuleText;
   private int searchType;
   private String textRule;
   private int channel;

   public HitCountParser(
      String textRule, int channel, List<Filter> filters, String tableName, boolean isFilter, String dataLimitSql, String otherRuleText, int searchType
   ) {
      super(textRule, channel, filters, tableName);
      this.isFilter = isFilter;
      this.dataLimit = dataLimitSql;
      this.otherRuleText = otherRuleText;
      this.searchType = searchType;
      this.textRule = textRule;
      this.channel = channel;
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

         if ((filterCondition == null || "".equals(filterCondition)) && textCondition != null && !"".equals(textCondition)) {
            sqlBuffer.append(" where " + textCondition);
         } else if (filterCondition == null || "".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
            sqlBuffer.append(" where (" + filterCondition + ") and (" + textCondition + ")");
         } else {
            sqlBuffer.append(" where " + filterCondition);
         }
      }

      if (!StringUtils.isNullOrEmpry(this.dataLimit)) {
         sqlBuffer.append(" and " + this.dataLimit);
      }

      if (!StringUtils.isNullOrEmpry(this.otherRuleText)) {
         sqlBuffer.append(" and " + this.otherRuleText);
      }

      this.setSql(sqlBuffer.toString());
      return sqlBuffer.toString();
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
