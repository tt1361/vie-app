package com.iflytek.vie.app.provider.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ColumnInfoPlatform implements Serializable {
   private static final long serialVersionUID = -853572846267773209L;
   private List<ColumnMapPlatform> columnMaps = new ArrayList<>();
   private List<String> columns = new ArrayList<>();
   private String columnsStr;

   public List<ColumnMapPlatform> getColumnMaps() {
      return this.columnMaps;
   }

   public void setColumnMaps(List<ColumnMapPlatform> columnMaps) {
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
