package com.iflytek.vie.app.provider.ruleparse;

import com.alibaba.fastjson.JSON;
import com.iflytek.vie.app.provider.common.SqlHelper;
import com.iflytek.vie.utils.StringUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarkDetailParser extends SqlParser implements Serializable {
   private String columns;
   private String order;
   private String orderType;
   private List ids;
   private int batchSize;
   private int totalBatchNum;
   private String textRule;
   private int channel;
   private int searchType;
   private boolean isIdQuery;
   private String objectRule;
   private String textColumn;
   private String quaCondition;
   private String paseRule;
   private String dataLimit;
   private String textFilter;
   private String channelRule;

   public MarkDetailParser(
      String textRule,
      int channel,
      List<Filter> filters,
      String tableName,
      int batchSize,
      int totalCount,
      String columns,
      List<String> ids,
      boolean isIdQuery,
      int searchType
   ) {
      super(textRule, channel, filters, tableName);
      this.columns = columns;
      this.ids = ids;
      this.batchSize = batchSize;
      this.isIdQuery = isIdQuery;
      this.totalBatchNum = totalCount % batchSize == 0 ? totalCount / batchSize : totalCount / batchSize + 1;
      this.textRule = textRule;
      this.channel = channel;
      this.searchType = searchType;
   }

   public MarkDetailParser(
      String textRule,
      int channel,
      List<Filter> filters,
      String tableName,
      int batchSize,
      int totalCount,
      String columns,
      List<String> ids,
      boolean isIdQuery,
      int searchType,
      String paseRule,
      String objectRule,
      String textColumn,
      String quaCondition,
      String dataLimit,
      String textFilter,
      String channelRule
   ) {
      super(textRule, channel, filters, tableName);
      this.columns = columns;
      this.ids = ids;
      this.batchSize = batchSize;
      this.isIdQuery = isIdQuery;
      this.totalBatchNum = totalCount % batchSize == 0 ? totalCount / batchSize : totalCount / batchSize + 1;
      this.textRule = textRule;
      this.channel = channel;
      this.searchType = searchType;
      this.objectRule = objectRule;
      this.textColumn = textColumn;
      this.quaCondition = quaCondition;
      this.paseRule = paseRule;
      this.dataLimit = dataLimit;
      this.textFilter = textFilter;
      this.channelRule = channelRule;
   }

   public MarkDetailParser(
      String textRule,
      int channel,
      List<Filter> filters,
      String tableName,
      int batchSize,
      int totalCount,
      String columns,
      List<String> ids,
      boolean isIdQuery,
      int searchType,
      String paseRule,
      String objectRule,
      String textColumn,
      String quaCondition,
      String dataLimit
   ) {
      super(textRule, channel, filters, tableName);
      this.columns = columns;
      this.ids = ids;
      this.batchSize = batchSize;
      this.isIdQuery = isIdQuery;
      this.totalBatchNum = totalCount % batchSize == 0 ? totalCount / batchSize : totalCount / batchSize + 1;
      this.textRule = textRule;
      this.channel = channel;
      this.searchType = searchType;
      this.objectRule = objectRule;
      this.textColumn = textColumn;
      this.quaCondition = quaCondition;
      this.paseRule = paseRule;
      this.dataLimit = dataLimit;
   }

   @Override
   public Object parseSql(String dataSource) {
      List<String> sqlList = new ArrayList<>();

      for (int i = 0; i < this.totalBatchNum; i++) {
         StringBuffer sqlBuffer = new StringBuffer();
         sqlBuffer.append("select " + this.columns + " from " + this.getTableName() + " where");
         String filterCondition = this.parseFilters();
         String textCondition = this.paseRule.contains("fulltext") ? this.paseRule : this.parseTextRule();
         sqlBuffer.append(" processed=0");
         if (!this.isIdQuery) {
            if (this.dataLimit != null && !"".equals(this.dataLimit)) {
               sqlBuffer.append(" and ").append(this.dataLimit);
            }

            sqlBuffer.append(" and ");
            if (filterCondition != null && !"".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
               if ((filterCondition == null || "".equals(filterCondition)) && textCondition != null && !"".equals(textCondition)) {
                  sqlBuffer.append(" (" + textCondition + ")");
               } else if (filterCondition == null || "".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
                  sqlBuffer.append(" (" + filterCondition + ") and (" + textCondition + ")");
               } else {
                  sqlBuffer.append(" (" + filterCondition + ")");
               }
            }
         }

         String inIds = SqlHelper.createIn(this.ids, true);
         sqlBuffer.append(" and (id " + inIds + ")").append(" and (rownum between " + (i * this.batchSize + 1) + " and " + (i + 1) * this.batchSize + ")");
         sqlList.add(sqlBuffer.toString());
      }

      return sqlList;
   }

   public Object parseSilenceSql() {
      List<String> sqlList = new ArrayList<>();
      StringBuffer sqlBuffer = null;

      for (int i = 0; i < this.totalBatchNum; i++) {
         sqlBuffer = new StringBuffer();
         sqlBuffer.append("select ");
         sqlBuffer.append("datamining('rule-analyse','");
         String jsonStr = this.dataMiningParams();
         sqlBuffer.append(jsonStr.substring(1, jsonStr.length() - 1));
         sqlBuffer.append("',id");
         boolean isContent = false;
         boolean isContentN0 = false;
         boolean isContentN1 = false;
         if (null != this.textRule && this.textRule.contains("@")) {
            sqlBuffer.append(",content,contentN0,contentN1");
         } else {
            switch (this.getChannel()) {
               case 0:
                  isContentN0 = true;
                  sqlBuffer.append(",contentN0");
                  break;
               case 1:
                  isContentN1 = true;
                  sqlBuffer.append(",contentN1");
                  break;
               default:
                  isContent = true;
                  sqlBuffer.append(",content");
            }
         }

         if (!StringUtils.isNullOrEmpry(this.textColumn)) {
            String[] selColumns = this.textColumn.split(",");

            for (String col : selColumns) {
               if (!"id".equals(col)
                  && (!"content".equals(col) || !isContent)
                  && (!"contentN0".equals(col) || !isContentN0)
                  && (!"contentN1".equals(col) || !isContentN1)) {
                  sqlBuffer.append("," + col);
               }
            }
         }

         sqlBuffer.append(") as result from ");
         sqlBuffer.append(this.getTableName() + " where");
         String filterCondition = this.parseFilters();
         String textCondition = this.parseTextRule();
         sqlBuffer.append(" processed=0");
         if (!this.isIdQuery) {
            if (this.dataLimit != null && !"".equals(this.dataLimit)) {
               sqlBuffer.append(" and ").append(this.dataLimit);
            }

            sqlBuffer.append(" and ");
            if (filterCondition != null && !"".equals(filterCondition)
               || textCondition != null && !"".equals(textCondition)
               || this.paseRule != null && !"".equals(this.paseRule)
               || this.quaCondition != null && !"".equals(this.quaCondition)
               || textCondition == null
               || textCondition.contains("@")) {
               if ((filterCondition == null || "".equals(filterCondition)) && textCondition != null && !"".equals(textCondition)) {
                  sqlBuffer.append(" (" + textCondition + ")");
               } else if (filterCondition == null || "".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
                  sqlBuffer.append(" (" + filterCondition + ") ");
                  if (textCondition != null && !textCondition.contains("@")) {
                     sqlBuffer.append(" and (" + textCondition + ")");
                  }
               } else {
                  sqlBuffer.append(" (" + filterCondition + ")");
               }
            }
         }

         if (!StringUtils.isNullOrEmpry(this.textFilter)) {
            sqlBuffer.append(" and (" + this.textFilter + ")");
         }

         if (this.searchType == 1 && this.paseRule != null && !"".equals(this.paseRule)) {
            sqlBuffer.append(" and (" + this.paseRule + ")");
         }

         if (!StringUtils.isNullOrEmpry(this.quaCondition)) {
            sqlBuffer.append(" and (" + this.quaCondition + ")");
            sqlBuffer.append(" and dataType=0");
         }

         String inIds = SqlHelper.createIn(this.ids, true);
         sqlBuffer.append(" and (id " + inIds + ")").append(" and (rownum between " + (i * this.batchSize + 1) + " and " + (i + 1) * this.batchSize + ")");
         sqlList.add(sqlBuffer.toString());
      }

      return sqlList;
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

         if (this.searchType == 1) {
            conditionBuffer.append(this.textRule + "))','content-query-parser')=true");
         } else {
            conditionBuffer.append(this.textRule + "))','content-word-query-parser')=true");
         }

         return conditionBuffer.toString();
      } else {
         return "";
      }
   }

   public String dataMiningParams() {
      Map<String, String> paramsMap = new HashMap<>();
      if (this.getTextRule() != null && !"null".equals(this.getTextRule()) && !"".equals(this.getTextRule())) {
         paramsMap.put("textRule", "(" + this.getTextRule() + ")");
      } else {
         paramsMap.put("textRule", "()");
      }

      if (null != this.channelRule && !"".equals(this.channelRule)) {
         paramsMap.put("channelRule", this.channelRule);
      }

      paramsMap.put("objectRule", this.objectRule);
      return JSON.toJSONString(paramsMap);
   }
}
