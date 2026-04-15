package com.iflytek.vie.app.provider.common;

import com.iflytek.vie.app.pojo.topicgroup.ColumnMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ColumnInfo implements Serializable {
   private static final long serialVersionUID = 5974324657602867895L;
   private List<ColumnMap> columnMaps = new ArrayList<>();
   private List<String> columns = new ArrayList<>();
   private String columnsStr;

   public List<ColumnMap> getColumnMaps() {
      return this.columnMaps;
   }

   public void setColumnMaps(List<ColumnMap> columnMaps) {
      this.columnMaps = columnMaps;
   }

   public List<String> getColumns() {
      return this.columns;
   }

   public void setColumns(List<String> columns) {
      this.columns = columns;
   }

   public String getColumnsStr() {
      return this.columnsStr;
   }

   public void setColumnsStr(String columnsStr) {
      this.columnsStr = columnsStr;
   }
}
