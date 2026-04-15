package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataInfo implements Serializable {
   private static final long serialVersionUID = 1526278489157586456L;
   private String id;
   private int mark;
   private int isExist;
   private String remark;
   private int remarked;
   private long dataRemarkId;
   private long dataMarkId;
   private long fragmentId;
   private List<Map<String, Object>> keywordInfos;
   private LinkedHashMap<String, Object> dataMaps;

   public DataInfo() {
   }

   public DataInfo(String id, int mark, int isExist) {
      this.id = id;
      this.mark = mark;
      this.isExist = isExist;
   }

   public String getId() {
      return this.id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public int getIsExist() {
      return this.isExist;
   }

   public void setIsExist(int isExist) {
      this.isExist = isExist;
   }

   public int getMark() {
      return this.mark;
   }

   public void setMark(int mark) {
      this.mark = mark;
   }

   public LinkedHashMap<String, Object> getDataMaps() {
      return this.dataMaps;
   }

   public void setDataMaps(LinkedHashMap<String, Object> dataMaps) {
      this.dataMaps = dataMaps;
   }

   public List<Map<String, Object>> getKeywordInfos() {
      return this.keywordInfos;
   }

   public void setKeywordInfos(List<Map<String, Object>> keywordInfos) {
      this.keywordInfos = keywordInfos;
   }

   public long getDataMarkId() {
      return this.dataMarkId;
   }

   public void setDataMarkId(long dataMarkId) {
      this.dataMarkId = dataMarkId;
   }

   public long getFragmentId() {
      return this.fragmentId;
   }

   public void setFragmentId(long fragmentId) {
      this.fragmentId = fragmentId;
   }

   public String getRemark() {
      return this.remark;
   }

   public void setRemark(String remark) {
      this.remark = remark;
   }

   public int getRemarked() {
      return this.remarked;
   }

   public void setRemarked(int remarked) {
      this.remarked = remarked;
   }

   public long getDataRemarkId() {
      return this.dataRemarkId;
   }

   public void setDataRemarkId(long dataRemarkId) {
      this.dataRemarkId = dataRemarkId;
   }
}
