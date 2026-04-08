package com.iflytek.vie.app.provider.ruleparse;

import com.iflytek.vie.app.provider.common.SqlHelper;
import com.iflytek.vie.utils.StringUtils;
import java.io.Serializable;
import java.util.List;

public class PreviewDetailParser extends SqlParserPlatform implements Serializable {
   private static final long serialVersionUID = 7086949156430520657L;
   private String columns;
   private String order;
   private String orderType;
   private List ids;
   private String dataLimit;

   public PreviewDetailParser(
      String textRule,
      int channel,
      List<FilterPlatform> filters,
      String tableName,
      String order,
      String orderType,
      String columns,
      List ids,
      String dataLimitSql
   ) {
      super(textRule, channel, filters, tableName);
      this.order = order;
      this.orderType = orderType;
      this.columns = columns;
      this.ids = ids;
      this.dataLimit = dataLimitSql;
   }

   @Override
   public Object parseSql(String dataSource) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("select " + this.columns + " from " + this.getTableName());
      if (this.ids != null && this.ids.size() > 0) {
         sqlBuffer.append(" where processed=0 and id ");
         String idsIn = SqlHelper.createIn(this.ids, true);
         sqlBuffer.append(idsIn);
      }

      if (!StringUtils.isNullOrEmpry(this.dataLimit)) {
         sqlBuffer.append(" and " + this.dataLimit);
      }

      if (this.order != null && !"".equals(this.order)) {
      }

      return sqlBuffer.toString();
   }

   @Override
   public Object parseFragmentSql(String dataSource) {
      return null;
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
