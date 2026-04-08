package com.iflytek.vie.app.provider.ruleparse;

import com.iflytek.vie.app.provider.common.SqlHelper;
import java.io.Serializable;
import java.util.List;

public class MarkCountParser extends SqlParser implements Serializable {
   private List<String> ids;
   private int seatchType;
   private String textRule;
   private int channel;
   private String objecntRule;
   private String textColumn;
   private String quaCondition;
   private String paseRule;

   public MarkCountParser(String textRule, int channel, List<Filter> filters, String tableName, List<String> ids, int searchType) {
      super(textRule, channel, filters, tableName);
      this.ids = ids;
      this.seatchType = searchType;
      this.textRule = textRule;
      this.channel = channel;
   }

   public MarkCountParser(
      String textRule,
      int channel,
      List<Filter> filters,
      String tableName,
      List<String> ids,
      int searchType,
      String paseRule,
      String objectRule,
      String textColumn,
      String quaCondition
   ) {
      super(textRule, channel, filters, tableName);
      this.ids = ids;
      this.seatchType = searchType;
      this.textRule = textRule;
      this.channel = channel;
      this.paseRule = paseRule;
      this.objecntRule = objectRule;
      this.textColumn = textColumn;
      this.quaCondition = quaCondition;
   }

   @Override
   public Object parseSql(String dataSource) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("select sum(idCounter) as hitCount from " + this.getTableName());
      String filterCondition = this.parseFilters();
      String textCondition = this.parseTextRule();
      boolean noWhere = false;
      if (filterCondition != null && !"".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
         if ((filterCondition == null || "".equals(filterCondition)) && textCondition != null && !"".equals(textCondition)) {
            sqlBuffer.append(" where " + textCondition);
         } else if (filterCondition == null || "".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
            sqlBuffer.append(" where (" + filterCondition + ") and (" + textCondition + ")");
         } else {
            sqlBuffer.append(" where " + filterCondition);
         }
      } else {
         noWhere = true;
      }

      String idsIn = SqlHelper.createIn(this.ids, true);
      if (noWhere) {
         sqlBuffer.append(" where id " + idsIn);
      } else {
         sqlBuffer.append(" and (id " + idsIn + ")");
      }

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

         if (this.seatchType == 1) {
            conditionBuffer.append(this.textRule + "))','content-query-parser')=true");
         } else {
            conditionBuffer.append(this.textRule + "))','content-word-query-parser')=true");
         }

         return conditionBuffer.toString();
      } else {
         return "";
      }
   }

   public List<String> getIds() {
      return this.ids;
   }

   public void setIds(List<String> ids) {
      this.ids = ids;
   }
}
