package com.iflytek.vie.app.provider.common;

import com.iflytek.vie.constants.CommonParams;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataSourceInfo {
   public List<HashMap<String, String>> dataSourceList = new ArrayList<>();

   public String getTableName(String dataSource) throws Exception {
      String tableName = "";

      try {
         if ("task".equals(this.getSysDataSource(dataSource).get(CommonParams.dataSourceType))) {
            tableName = CommonParams.dataSourceTask + this.getSysDataSource(dataSource).get(CommonParams.dataSource);
         } else {
            tableName = CommonParams.dataSourceVoice + this.getSysDataSource(dataSource).get(CommonParams.dataSource);
         }

         if ("dpsnull".equals(tableName)) {
            throw new Exception("can not find dataSource " + dataSource);
         } else {
            return tableName;
         }
      } catch (Exception var4) {
         throw new Exception("get tableName fail", var4);
      }
   }

   public HashMap<String, String> getSysDataSource(String dataSource) {
      HashMap<String, String> ds = new HashMap<>();

      for (HashMap<String, String> cd : this.dataSourceList) {
         if (cd.get(CommonParams.dataSource).equals(dataSource)) {
            ds = cd;
            break;
         }
      }

      return ds;
   }

   public List<HashMap<String, String>> getDataSourceList() {
      return this.dataSourceList;
   }

   public void setDataSourceList(List<HashMap<String, String>> dataSourceList) {
      this.dataSourceList = dataSourceList;
   }
}
