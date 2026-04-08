package com.iflytek.vie.app.provider.pojo;

import java.util.Date;

public class StoreFileData {
   private int id;
   private String fileName;
   private String fileFormat;
   private Long fileSize;
   private String storePath;
   private Date uploadDate;
   private String dataSource;
   private int status;

   public int getId() {
      return this.id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public String getFileName() {
      return this.fileName;
   }

   public void setFileName(String fileName) {
      this.fileName = fileName;
   }

   public String getFileFormat() {
      return this.fileFormat;
   }

   public void setFileFormat(String fileFormat) {
      this.fileFormat = fileFormat;
   }

   public Long getFileSize() {
      return this.fileSize;
   }

   public void setFileSize(Long fileSize) {
      this.fileSize = fileSize;
   }

   public String getStorePath() {
      return this.storePath;
   }

   public void setStorePath(String storePath) {
      this.storePath = storePath;
   }

   public Date getUploadDate() {
      return this.uploadDate;
   }

   public void setUploadDate(Date uploadDate) {
      this.uploadDate = uploadDate;
   }

   public String getDataSource() {
      return this.dataSource;
   }

   public void setDataSource(String dataSource) {
      this.dataSource = dataSource;
   }

   public int getStatus() {
      return this.status;
   }

   public void setStatus(int status) {
      this.status = status;
   }
}
