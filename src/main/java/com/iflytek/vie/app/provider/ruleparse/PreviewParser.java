package com.iflytek.vie.app.provider.ruleparse;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.utils.StringUtils;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreviewParser extends SqlParserPlatform implements Serializable {
   private static final long serialVersionUID = 9031797323739751878L;
   private int pageSize;
   private int pageNum;
   private String order;
   private String orderType = "asc";
   private String textRules;
   private String dataLimitSql;
   private String objecntRule;
   private String textColumn;
   private String quaCondition;
   private String textFilter;
   private String channelRule;

   public String getTextFilter() {
      return this.textFilter;
   }

   public void setTextFilter(String textFilter) {
      this.textFilter = textFilter;
   }

   public PreviewParser(
      String textRule,
      int channel,
      List<FilterPlatform> filters,
      String tableName,
      int pageNum,
      int pageSize,
      String order,
      String orderType,
      String textRules,
      String dataLimit,
      String objecntRule,
      String textColumn,
      String quaCondition,
      String textFilter
   ) {
      super(textRule, channel, filters, tableName);
      this.pageNum = pageNum;
      this.pageSize = pageSize;
      this.pageNum = pageNum;
      this.order = order;
      this.orderType = orderType;
      this.textRules = textRules;
      this.dataLimitSql = dataLimit;
      this.objecntRule = objecntRule;
      this.textColumn = textColumn;
      this.quaCondition = quaCondition;
      this.textFilter = textFilter;
   }

   public PreviewParser(
      String textRule,
      int channel,
      List<FilterPlatform> filters,
      String tableName,
      int pageNum,
      int pageSize,
      String order,
      String orderType,
      String textRules,
      String dataLimit,
      String objecntRule,
      String textColumn,
      String quaCondition,
      String textFilter,
      String channelRule
   ) {
      super(textRule, channel, filters, tableName);
      this.pageNum = pageNum;
      this.pageSize = pageSize;
      this.pageNum = pageNum;
      this.order = order;
      this.orderType = orderType;
      this.textRules = textRules;
      this.dataLimitSql = dataLimit;
      this.objecntRule = objecntRule;
      this.textColumn = textColumn;
      this.quaCondition = quaCondition;
      this.textFilter = textFilter;
      this.channelRule = channelRule;
   }

   @Override
   public Object parseFragmentSql(String dataSource) {
      int insightType = IndexConstants.getInsightType(dataSource);
      this.init();
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("select ");
      sqlBuffer.append("datamining('rule-analyse','");
      String jsonStr = this.dataMiningParams();
      sqlBuffer.append(jsonStr.substring(1, jsonStr.length() - 1));
      sqlBuffer.append("',id");
      boolean isContent = false;
      boolean isContentN0 = false;
      boolean isContentN1 = false;
      switch (this.getChannel()) {
         case 0:
            isContentN0 = true;
            if (1 == insightType) {
               sqlBuffer.append(",contentN0,child_fields.childContentLength,child_fields.childVoiceId");
            } else {
               sqlBuffer.append(",contentN0");
            }
            break;
         case 1:
            isContentN1 = true;
            if (1 == insightType) {
               sqlBuffer.append(",contentN1,child_fields.childContentLength,child_fields.childVoiceId");
            } else {
               sqlBuffer.append(",contentN1");
            }
            break;
         default:
            isContent = true;
            if (1 == insightType) {
               sqlBuffer.append(",content,child_fields.childContentLength,child_fields.childVoiceId");
            } else {
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
      sqlBuffer.append(this.getTableName());
      String filterCondition = this.parseFilters();
      String textCondition = this.textRules;
      if (filterCondition != null && !"".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
         sqlBuffer.append(" where ").append(!StringUtils.isNullOrEmpry(this.dataLimitSql) ? this.dataLimitSql : "processed=0 ");
         if ((filterCondition == null || "".equals(filterCondition)) && textCondition != null && !"".equals(textCondition)) {
            sqlBuffer.append(" and (" + textCondition + ")");
         } else if (filterCondition == null || "".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
            sqlBuffer.append(" and (" + filterCondition + ") and (" + textCondition + ")");
         } else {
            sqlBuffer.append(" and (" + filterCondition + ")");
         }

         if (!StringUtils.isNullOrEmpry(this.textFilter)) {
            sqlBuffer.append(" and (" + this.textFilter + ")");
         }

         if (!StringUtils.isNullOrEmpry(this.quaCondition)) {
            sqlBuffer.append(" and (" + this.quaCondition + ")");
         }

         if (!StringUtils.isNullOrEmpry(this.objecntRule)) {
            sqlBuffer.append(" and dataType=0 ");
         } else if (super.textRule.contains("@")) {
            sqlBuffer.append(" and dataType=0 ");
         } else {
            sqlBuffer.append(" and (rownum between " + (this.pageSize * (this.pageNum - 1) + 1) + " and " + this.pageNum * this.pageSize + ")");
         }

         this.setSql(sqlBuffer.toString());
         return sqlBuffer.toString();
      } else {
         return sqlBuffer.toString();
      }
   }

   @Override
   public Object parseSql(String dataSource) {
      int insightType = IndexConstants.getInsightType(dataSource);
      this.init();
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("select ");
      sqlBuffer.append("datamining('rule-analyse','");
      String jsonStr = this.dataMiningParams();
      sqlBuffer.append(jsonStr.substring(1, jsonStr.length() - 1));
      sqlBuffer.append("',id");
      boolean isContent = false;
      boolean isContentN0 = false;
      boolean isContentN1 = false;
      if (super.textRule.contains("@")) {
         if (1 == insightType) {
            sqlBuffer.append(",content,contentN0,contentN1,child_fields.childContentLength,child_fields.childVoiceId");
         } else {
            sqlBuffer.append(",content,contentN0,contentN1");
         }
      } else {
         switch (this.getChannel()) {
            case 0:
               isContentN0 = true;
               if (1 == insightType) {
                  sqlBuffer.append(",contentN0,child_fields.childContentLength,child_fields.childVoiceId");
               } else {
                  sqlBuffer.append(",contentN0");
               }
               break;
            case 1:
               isContentN1 = true;
               if (1 == insightType) {
                  sqlBuffer.append(",contentN1,child_fields.childContentLength,child_fields.childVoiceId");
               } else {
                  sqlBuffer.append(",contentN1");
               }
               break;
            default:
               isContent = true;
               if (1 == insightType) {
                  sqlBuffer.append(",content,child_fields.childContentLength,child_fields.childVoiceId");
               } else {
                  sqlBuffer.append(",content");
               }
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
      sqlBuffer.append(this.getTableName());
      String filterCondition = this.parseFilters();
      String textCondition = this.textRules;
      if (filterCondition != null && !"".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
         sqlBuffer.append(" where ").append(!StringUtils.isNullOrEmpry(this.dataLimitSql) ? this.dataLimitSql : "processed=0 ");
         if ((filterCondition == null || "".equals(filterCondition)) && textCondition != null && !"".equals(textCondition)) {
            sqlBuffer.append(" and (" + textCondition + ")");
         } else if (filterCondition == null || "".equals(filterCondition) || textCondition != null && !"".equals(textCondition)) {
            sqlBuffer.append(" and (" + filterCondition + ") and (" + textCondition + ")");
         } else {
            sqlBuffer.append(" and (" + filterCondition + ")");
         }

         if (!StringUtils.isNullOrEmpry(this.textFilter)) {
            sqlBuffer.append(" and (" + this.textFilter + ")");
         }

         if (!StringUtils.isNullOrEmpry(this.quaCondition)) {
            sqlBuffer.append(" and (" + this.quaCondition + ")");
         }

         if (!StringUtils.isNullOrEmpry(this.objecntRule)) {
            sqlBuffer.append(" and dataType=0 ");
         } else if (super.textRule.contains("@")) {
            sqlBuffer.append(" and dataType=0 ");
         } else {
            sqlBuffer.append(" and (rownum between " + (this.pageSize * (this.pageNum - 1) + 1) + " and " + this.pageNum * this.pageSize + ")");
         }

         this.setSql(sqlBuffer.toString());
         return sqlBuffer.toString();
      } else {
         return sqlBuffer.toString();
      }
   }

   public void init() {
      StringBuffer columnBuffer = new StringBuffer();
      columnBuffer.append("id");
      if (this.getTableName() != null && this.getTableName().startsWith("dpms")) {
         columnBuffer.append(",");
         columnBuffer.append("voiceAnalysisInfo");
      }

      if (!Strings.isNullOrEmpty(this.getTextRule())) {
         columnBuffer.append(",");
         switch (this.getChannel()) {
            case 0:
               columnBuffer.append("contentN0,");
               break;
            case 1:
               columnBuffer.append("contentN1,");
               break;
            default:
               columnBuffer.append("content,");
         }
      }
   }

   public String dataMiningParams() {
      Map<String, String> paramsMap = new HashMap<>();
      if (this.getTextRule() != null && !"".equals(this.getTextRule()) && !"null".equals(this.getTextRule())) {
         paramsMap.put("textRule", "(" + this.getTextRule() + ")");
      } else {
         paramsMap.put("textRule", "()");
      }

      if (null != this.channelRule && !"".equals(this.channelRule)) {
         paramsMap.put("channelRule", this.channelRule);
      }

      paramsMap.put("objectRule", this.objecntRule);
      return JSON.toJSONString(paramsMap);
   }
}
