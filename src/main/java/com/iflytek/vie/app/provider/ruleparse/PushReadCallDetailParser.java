package com.iflytek.vie.app.provider.ruleparse;

import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.utils.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PushReadCallDetailParser extends SqlParser {
   private static final long serialVersionUID = -672548115743635489L;
   private String columns;
   private String order;
   private String orderType;
   private int pageSize;
   private int pageNum;
   private List<Long> modelIdList;
   private List<String> callIdList;
   private int topicId;
   private int topicGroupType;
   private Map<String, String> groupModelMap;
   private String dataTypeDimension;
   private String multipleDataSql;

   public PushReadCallDetailParser(
      List<Filter> filters,
      String tableName,
      String order,
      String orderType,
      String columns,
      int pageNum,
      int pageSize,
      List<String> callIdList,
      List<Long> modelIdList,
      int topicId,
      int topicGroupType,
      Map<String, String> groupModelMap,
      String dataTypeDimension,
      String multipleDataSql
   ) {
      super("", 2, filters, tableName);
      this.order = order;
      this.orderType = orderType;
      this.columns = columns;
      this.pageNum = pageNum;
      this.pageSize = pageSize;
      this.callIdList = callIdList;
      this.modelIdList = modelIdList;
      this.topicId = topicId;
      this.topicGroupType = topicGroupType;
      this.groupModelMap = groupModelMap;
      this.dataTypeDimension = dataTypeDimension;
      this.multipleDataSql = multipleDataSql;
   }

   @Override
   public Object parseSql(String dataSource) {
      StringBuffer sqlBuffer = new StringBuffer();
      String callIdStr = "";
      String modelIdStr = "";
      if (this.callIdList != null && this.callIdList.size() > 0) {
         for (int i = 0; i < this.callIdList.size(); i++) {
            if (i == this.callIdList.size() - 1) {
               callIdStr = callIdStr + "'" + this.callIdList.get(i) + "'";
            } else {
               callIdStr = callIdStr + "'" + this.callIdList.get(i) + "',";
            }
         }
      }

      if (this.modelIdList != null && this.modelIdList.size() > 0) {
         for (int ix = 0; ix < this.modelIdList.size(); ix++) {
            if (ix == this.modelIdList.size() - 1) {
               modelIdStr = modelIdStr + this.modelIdList.get(ix);
            } else {
               modelIdStr = modelIdStr + this.modelIdList.get(ix) + ",";
            }
         }
      }

      Map<String, String> map = new HashMap<>();
      if (this.topicGroupType != 1 && "".equals(modelIdStr)) {
         map.put("index", "main");
         sqlBuffer.append("select " + this.columns + ",dimDay from " + this.getTableName());
         String filterCondition = this.parseFilters();
         if (filterCondition != null) {
            sqlBuffer.append(" where (" + filterCondition + ")");
         }

         sqlBuffer.append(" and " + this.multipleDataSql);
         sqlBuffer.append(" and rownum between " + this.pageNum + " and " + this.pageSize + "");
         if (!StringUtils.isNullOrEmpry(callIdStr)) {
            if (this.topicId == 0) {
               sqlBuffer.append(" and id not in (" + callIdStr + ")");
            } else {
               sqlBuffer.append(" and id in (" + callIdStr + ")");
            }
         }

         if (this.order != null && !"".equals(this.order)) {
            sqlBuffer.append(" order by " + this.order + " " + this.orderType);
         }
      } else {
         map.put("index", "model");
         sqlBuffer.append("select " + this.columns + ",dimDay from " + IndexConstants.getModelTableName(this.getTableName()));
         String filterConditionx = this.parseFilters();
         if (filterConditionx != null) {
            sqlBuffer.append(" where (" + filterConditionx + ")");
         }

         sqlBuffer.append(" and " + this.multipleDataSql);
         if (this.topicGroupType != 1) {
            if (!StringUtils.isNullOrEmpry(modelIdStr)) {
               sqlBuffer.append(" and offLineTagId in (" + modelIdStr + ")");
            }
         } else {
            StringBuffer sbf = new StringBuffer();
            sbf.append(" and (");

            for (String group : this.groupModelMap.keySet()) {
               sbf.append("(" + this.dataTypeDimension + " = '")
                  .append(group)
                  .append("' and offLineTagId in (")
                  .append(this.groupModelMap.get(group))
                  .append(")) or ");
            }

            sqlBuffer.append(sbf.substring(0, sbf.length() - 3) + ") ");
         }

         if (!StringUtils.isNullOrEmpry(callIdStr)) {
            if (this.topicId == 0) {
               sqlBuffer.append(" and id not in (" + callIdStr + ")");
            } else {
               sqlBuffer.append(" and id in (" + callIdStr + ")");
            }
         }

         sqlBuffer.append(" group by " + this.columns + ",dimDay" + " having rownum between " + this.pageNum + " and " + this.pageSize + "");
         if (this.order != null && !"".equals(this.order)) {
            sqlBuffer.append(" order by " + this.order + " " + this.orderType);
         }
      }

      map.put("sql", sqlBuffer.toString());
      return map;
   }

   public Object parseSql() {
      return this.parseSql(null);
   }

   public String getColumns() {
      return this.columns;
   }

   public void setColumns(String columns) {
      this.columns = columns;
   }

   public String getOrder() {
      return this.order;
   }

   public void setOrder(String order) {
      this.order = order;
   }

   public String getOrderType() {
      return this.orderType;
   }

   public void setOrderType(String orderType) {
      this.orderType = orderType;
   }

   public int getPageSize() {
      return this.pageSize;
   }

   public void setPageSize(int pageSize) {
      this.pageSize = pageSize;
   }

   public int getPageNum() {
      return this.pageNum;
   }

   public void setPageNum(int pageNum) {
      this.pageNum = pageNum;
   }

   public List<Long> getModelIdList() {
      return this.modelIdList;
   }

   public void setModelIdList(List<Long> modelIdList) {
      this.modelIdList = modelIdList;
   }

   public List<String> getCallIdList() {
      return this.callIdList;
   }

   public void setCallIdList(List<String> callIdList) {
      this.callIdList = callIdList;
   }

   public int getTopicId() {
      return this.topicId;
   }

   public void setTopicId(int topicId) {
      this.topicId = topicId;
   }

   public int getTopicGroupType() {
      return this.topicGroupType;
   }

   public void setTopicGroupType(int topicGroupType) {
      this.topicGroupType = topicGroupType;
   }

   public Map<String, String> getGroupModelMap() {
      return this.groupModelMap;
   }

   public void setGroupModelMap(Map<String, String> groupModelMap) {
      this.groupModelMap = groupModelMap;
   }

   public String getDataTypeDimension() {
      return this.dataTypeDimension;
   }

   public void setDataTypeDimension(String dataTypeDimension) {
      this.dataTypeDimension = dataTypeDimension;
   }

   public String getMultipleDataSql() {
      return this.multipleDataSql;
   }

   public void setMultipleDataSql(String multipleDataSql) {
      this.multipleDataSql = multipleDataSql;
   }
}
