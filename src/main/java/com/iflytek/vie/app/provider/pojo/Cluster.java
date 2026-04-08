package com.iflytek.vie.app.provider.pojo;

public class Cluster {
   private Long togatherId;
   private String togatherName;
   private Long parentId;
   private String chartLeftInfo;
   private String charRightInfo;
   private Long pathId;
   private String telephoneIdInfo;
   private String togatherNumber;
   private Long togatherCheck;
   private Long togatherStatus;
   private String dataAuth;
   private Integer dataType;
   private String batchId;
   private int ifLone;

   public String getDataAuth() {
      return this.dataAuth;
   }

   public void setDataAuth(String dataAuth) {
      this.dataAuth = dataAuth;
   }

   public Long getTogatherId() {
      return this.togatherId;
   }

   public void setTogatherId(Long togatherId) {
      this.togatherId = togatherId;
   }

   public Long getTogatherStatus() {
      return this.togatherStatus;
   }

   public void setTogatherStatus(Long togatherStatus) {
      this.togatherStatus = togatherStatus;
   }

   public Long getTogatherCheck() {
      return this.togatherCheck;
   }

   public void setTogatherCheck(Long togatherCheck) {
      this.togatherCheck = togatherCheck;
   }

   public String getTogatherNumber() {
      return this.togatherNumber;
   }

   public void setTogatherNumber(String togatherNumber) {
      this.togatherNumber = togatherNumber;
   }

   public String getTelephoneIdInfo() {
      return this.telephoneIdInfo;
   }

   public void setTelephoneIdInfo(String telephoneIdInfo) {
      this.telephoneIdInfo = telephoneIdInfo;
   }

   public Long getPathId() {
      return this.pathId;
   }

   public void setPathId(Long pathId) {
      this.pathId = pathId;
   }

   public String getCharRightInfo() {
      return this.charRightInfo;
   }

   public void setCharRightInfo(String charRightInfo) {
      this.charRightInfo = charRightInfo;
   }

   public String getChartLeftInfo() {
      return this.chartLeftInfo;
   }

   public void setChartLeftInfo(String chartLeftInfo) {
      this.chartLeftInfo = chartLeftInfo;
   }

   public Long getParentId() {
      return this.parentId;
   }

   public void setParentId(Long parentId) {
      this.parentId = parentId;
   }

   public String getTogatherName() {
      return this.togatherName;
   }

   public void setTogatherName(String togatherName) {
      this.togatherName = togatherName;
   }

   public Integer getDataType() {
      return this.dataType;
   }

   public void setDataType(Integer dataType) {
      this.dataType = dataType;
   }

   public String getBatchId() {
      return this.batchId;
   }

   public void setBatchId(String batchId) {
      this.batchId = batchId;
   }

   public int getIfLone() {
      return this.ifLone;
   }

   public void setIfLone(int ifLone) {
      this.ifLone = ifLone;
   }
}
