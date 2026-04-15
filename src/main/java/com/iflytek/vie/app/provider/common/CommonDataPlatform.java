package com.iflytek.vie.app.provider.common;

import com.iflytek.vie.app.api.permission.DataAuthService;
import com.iflytek.vie.app.pojo.permission.ContextRequest;
import com.iflytek.vie.app.pojo.permission.DataResourceAuth;
import com.iflytek.vie.app.pojo.permission.DimensionAuth;
import com.iflytek.vie.utils.ListUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class CommonDataPlatform {
   private final Logger logger = LoggerFactory.getLogger(CommonDataPlatform.class);
   private DataAuthService dataAuthService;
   @Autowired
   private Environment env;

   public String getDataSql(String userId, String systemId, String dataSource) {
      StringBuilder sb = new StringBuilder();

      try {
         ContextRequest request = new ContextRequest();
         request.setUserId(Long.parseLong(userId));
         request.setContextPath(systemId);
         request.setDataSource(dataSource);
         Map<String, DataResourceAuth> dataMap = null;
         List<DimensionAuth> funList = null;
         dataMap = this.dataAuthService.getDataAuths(request);
         sb.append("(");
         if (dataMap == null) {
            sb.append(" processed = -10");
         } else {
            for (String ck : dataMap.keySet()) {
               if (ck.equals(dataSource) && dataMap.get(ck).getEnglishName().equals(dataSource)) {
                  if (sb.toString().trim().length() > 1) {
                     sb.append(" or ");
                  }

                  funList = dataMap.get(ck).getDataDimension();
                  if (!ListUtil.isNullOrEmpty(funList)) {
                     int j = 0;

                     for (DimensionAuth da : funList) {
                        StringBuilder csb = new StringBuilder();
                        List<String> strList = da.getValue();
                        List<String> listStr = new ArrayList<>();

                        for (String str : strList) {
                           if (str.contains("*")) {
                              csb.append(" fulltext('" + da.getEnglishName() + ":(" + str + ")')=true and ");
                           } else {
                              listStr.add(str);
                           }
                        }

                        csb.append(" " + da.getEnglishName() + " in (");

                        for (int i = 0; i < listStr.size(); i++) {
                           if (i == listStr.size() - 1) {
                              csb.append("'" + listStr.get(i) + "')");
                           } else {
                              csb.append("'" + listStr.get(i) + "',");
                           }
                        }

                        if (++j == funList.size()) {
                           sb.append("(" + csb.toString() + " )");
                        } else {
                           sb.append("(" + csb.toString() + " )" + " and ");
                        }
                     }
                  } else {
                     if (sb.toString().trim().length() > 1) {
                        sb.append(" or ");
                     }

                     sb.append(" processed = -10");
                  }
               }
            }
         }

         if ("(".equals(sb.toString())) {
            sb.append(" processed = -10");
         }

         sb.append(")");
      } catch (Exception var18) {
         this.logger.error("method getDataSql exception");
      }

      this.logger.info("success get user limit sql");
      return sb.toString();
   }

   public List<String> getDataStringList(String userId, String systemId, String dataSource) {
      this.logger.info("the userid is : " + userId);
      this.logger.info("the systemid is : " + systemId);
      List<String> list = null;

      try {
         ContextRequest request = new ContextRequest();
         request.setUserId(Long.parseLong(userId));
         request.setContextPath(systemId);
         request.setDataSource(dataSource);
         Map<String, DataResourceAuth> dataMap = null;
         dataMap = this.dataAuthService.getDataAuths(request);
         if (dataMap != null) {
            list = this.analyzeDataAuthList(dataMap, dataSource);
            if (list == null || list.isEmpty()) {
               list = new ArrayList<>();
               list.add("");
            }
         }
      } catch (Exception var7) {
         this.logger.error("内部服务错误", var7);
      }

      return list;
   }

   public HashMap<String, Set<String>> getDataMapAuth(String userId, String systemId, String dataSource) {
      this.logger.info("the userid is : " + userId);
      this.logger.info("the systemid is : " + systemId);
      HashMap<String, Set<String>> authMap = null;

      try {
         ContextRequest request = new ContextRequest();
         request.setUserId(Long.parseLong(userId));
         request.setContextPath(systemId);
         request.setDataSource(dataSource);
         Map<String, DataResourceAuth> dataMap = null;
         dataMap = this.dataAuthService.getDataAuths(request);
         if (dataMap != null) {
            authMap = this.getMapDataAuth(dataMap, dataSource);
         }
      } catch (Exception var7) {
         this.logger.error("内部服务错误", var7);
      }

      return authMap;
   }

   public HashMap<String, Set<String>> getMapDataAuth(Map<String, DataResourceAuth> data, String dataSource) {
      HashMap<String, Set<String>> hashMap = new HashMap<>();

      try {
         if (data != null) {
            DataResourceAuth dataResourceAuth = null;

            for (String ck : data.keySet()) {
               if (ck.equals(dataSource)) {
                  dataResourceAuth = data.get(ck);
                  if (dataResourceAuth != null) {
                     for (DimensionAuth da : dataResourceAuth.getDataDimension()) {
                        String engName = da.getEnglishName();
                        Set<String> values = new HashSet<>();

                        for (String value : da.getValue()) {
                           values.add(value);
                        }

                        hashMap.put(engName, values);
                     }
                  }
               }
            }
         }

         return hashMap;
      } catch (Exception var14) {
         this.logger.error("method getMapDataAuth exception");
         throw new RuntimeException(var14);
      }
   }

   private List<String> analyzeDataAuthList(Map<String, DataResourceAuth> data, String dataSource) {
      List<String> dataAuthList = new ArrayList<>();

      try {
         if (data != null) {
            DataResourceAuth dataResourceAuth = null;

            for (String ck : data.keySet()) {
               if (ck.equals(dataSource)) {
                  dataResourceAuth = data.get(ck);
                  if (dataResourceAuth != null) {
                     List<DimensionAuth> dimensionList = dataResourceAuth.getDataDimension();
                     if (dimensionList != null && dimensionList.size() > 0) {
                        DimensionAuth dimensionAuth = null;

                        for (int i = 0; i < dimensionList.size(); i++) {
                           dimensionAuth = dimensionList.get(i);
                           if (dimensionAuth != null && dimensionAuth.getValue() != null && dimensionAuth.getValue().size() > 0) {
                              dataAuthList.addAll(dimensionAuth.getValue());
                           }
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var10) {
         this.logger.error("内部服务错误", var10);
      }

      return dataAuthList;
   }

   public DataAuthService getDataAuthService() {
      return this.dataAuthService;
   }

   public void setDataAuthService(DataAuthService dataAuthService) {
      this.dataAuthService = dataAuthService;
   }
}
