package com.iflytek.vie.app.provider.common;

import java.io.Serializable;

public class ColumnMapPlatform implements Serializable {
   private static final long serialVersionUID = 1459287261482263553L;
   private String columnName;
   private String column;

   public ColumnMapPlatform(String columnName, String column) {
      this.columnName = columnName;
      this.column = column;
   }

   public String getColumnName() {
      return this.columnName;
   }

   public void setColumnName(String columnName) {
      this.columnName = columnName;
   }

   public String getColumn() {
      return this.column;
   }

   public void setColumn(String column) {
      this.column = column;
   }
}
