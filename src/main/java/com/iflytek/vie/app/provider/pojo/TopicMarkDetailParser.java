package com.iflytek.vie.app.provider.pojo;

import com.iflytek.vie.app.provider.common.SqlHelper;
import com.iflytek.vie.app.provider.ruleparse.Filter;
import com.iflytek.vie.app.provider.ruleparse.SqlParser;
import com.iflytek.vie.utils.StringUtils;
import java.util.List;

public class TopicMarkDetailParser extends SqlParser {
   private static final long serialVersionUID = -9153021578342212122L;
   private String columns;
   private String order;
   private String orderType;
   private List ids;
   private int pageSize;
   private int pageNum;
   private String dataLimitSql;

   public TopicMarkDetailParser(
      String textRule,
      int channel,
      List<Filter> filters,
      String tableName,
      String order,
      String orderType,
      String columns,
      List ids,
      int pageNum,
      int pageSize,
      String dataLimit
   ) {
      super(textRule, channel, filters, tableName);
      this.order = order;
      this.orderType = orderType;
      this.columns = columns;
      this.ids = ids;
      this.pageNum = pageNum;
      this.pageSize = pageSize;
      this.dataLimitSql = dataLimit;
   }

   public Object parseSql() {
      return this.parseSql(null);
   }

   @Override
   public Object parseSql(String datasource) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("select " + this.columns + ",child_fields.offLineTagId,child_fields.offLineTagInfo from " + this.getTableName());
      if (this.ids != null && this.ids.size() > 0) {
         sqlBuffer.append(" where processed=0 and id ");
         String idsIn = SqlHelper.createIn(this.ids, true);
         sqlBuffer.append(idsIn);
      }

      sqlBuffer.append(" and (rownum between " + (this.pageSize * (this.pageNum - 1) + 1) + " and " + this.pageNum * this.pageSize + ")");
      if (!StringUtils.isNullOrEmpry(this.dataLimitSql)) {
         sqlBuffer.append(" and " + this.dataLimitSql);
      }

      if (this.order != null && !"".equals(this.order) && !"mark".equals(this.order)) {
         sqlBuffer.append(" order by " + this.order + " " + this.orderType);
      }

      return sqlBuffer.toString();
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

   public List getIds() {
      return this.ids;
   }

   public void setIds(List ids) {
      this.ids = ids;
   }
}
