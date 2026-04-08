package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;

public class ResultTable implements Serializable {
   private int pageNum = 1;
   private int pageSize = 10;
   private int totalPages;
   private int totalRows;
   private Object rows;
   private String sortColumn;
   private String sortMethod;

   public String getSortMethod() {
      return this.sortMethod;
   }

   public void setSortMethod(String sortMethod) {
      this.sortMethod = sortMethod;
   }

   public int getPageNum() {
      return this.pageNum;
   }

   public void setPageNum(int pageNum) {
      this.pageNum = pageNum;
   }

   public int getPageSize() {
      return this.pageSize;
   }

   public void setPageSize(int pageSize) {
      this.pageSize = pageSize;
   }

   public int getTotalPages() {
      return this.totalPages;
   }

   public void setTotalPages(int totalPages) {
      this.totalPages = totalPages;
   }

   public int getTotalRows() {
      return this.totalRows;
   }

   public void setTotalRows(int totalRows) {
      this.totalRows = totalRows;
   }

   public Object getRows() {
      return this.rows;
   }

   public void setRows(Object rows) {
      this.rows = rows;
   }

   public String getSortColumn() {
      return this.sortColumn;
   }

   public void setSortColumn(String sortColumn) {
      this.sortColumn = sortColumn;
   }
}
