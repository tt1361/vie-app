package com.iflytek.vie.app.provider.impl.datamining;

import com.alibaba.fastjson.JSONArray;
import com.iflytek.vie.app.api.datamining.ClusterService;
import com.iflytek.vie.app.api.permission.DataAuthService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.datamining.ClusterRequest;
import com.iflytek.vie.app.pojo.datamining.ClusterResponse;
import com.iflytek.vie.app.pojo.datamining.ClusterTableRequest;
import com.iflytek.vie.app.pojo.permission.ContextRequest;
import com.iflytek.vie.app.pojo.permission.DataResourceAuth;
import com.iflytek.vie.app.pojo.permission.DimensionAuth;
import com.iflytek.vie.app.provider.database.DayClusterDB;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class ClusterServiceImpl implements ClusterService {
   private final Logger logger = LoggerFactory.getLogger(ClusterServiceImpl.class);
   private DayClusterDB dayClusterDB;
   private DataAuthService dataAuthService;
   private int isSentence = 0;
   @Autowired
   private Environment env;

   public List<ClusterResponse> getClusterDataService(ClusterRequest clusterRequest) throws ViePlatformServiceException {
      List<ClusterResponse> resultList = new ArrayList<>();

      try {
         Date date = DateUtils.parseStringToDate(clusterRequest.getSelectTime());
         String dateString = DateUtils.parseDateToString(date, "yyyy-MM-dd");
         this.logger.info("当前时间:" + dateString);
         String yesterday = DateUtils.getDaybeforeOrAfter(dateString, -1);
         Long yesterdayLong = Long.parseLong(yesterday.replace("-", ""));
         String batchId = clusterRequest.getBatchId();
         if (StringUtils.isNullOrEmpry(batchId)) {
            batchId = "0";
         }

         Integer dataType = clusterRequest.getDataType();
         if (dataType == null) {
            dataType = 2;
         }

         Integer ifLone = clusterRequest.getIfLone();
         if (ifLone == null || ifLone == 0) {
            ifLone = 0;
            batchId = "0";
         }

         this.logger.info("聚类查询时间:" + yesterdayLong);
         String center = clusterRequest.getSelectCenter();
         if (center == null || center.equals("")) {
            AuthorizeInfo authorizeInfo = new AuthorizeInfo(clusterRequest.getUserId(), clusterRequest.getSystemId(), clusterRequest.getDataSource());
            center = this.getCenter(authorizeInfo, clusterRequest.getDataTypeDimension());
         }

         this.logger.info("聚类权限：" + center);
         String result = this.dayClusterDB.getDayCluster(clusterRequest.getDataSource(), date, this.isSentence, center, batchId, ifLone, dataType);
         if (result.length() > 2) {
            resultList = this.sortDayCluster(result);
         }

         return resultList;
      } catch (Exception var12) {
         this.logger.info("获取聚类数据失败！");
         var12.printStackTrace();
         throw new ViePlatformServiceException("获取聚类数据失败！", var12);
      }
   }

   public String getClusterTableDataService(ClusterTableRequest clusterTableRequest) throws ViePlatformServiceException {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      Date date = null;

      try {
         date = sdf.parse(clusterTableRequest.getSelectTime());
      } catch (ParseException var10) {
         this.logger.info("日期解析失败！");
         throw new ViePlatformServiceException("日期解析失败！", var10);
      }

      String dimDay = DateUtils.parseDateToString(date, "yyyyMMdd");
      String selectCenter = clusterTableRequest.getSelectCenter();
      if (clusterTableRequest.getCenterFlag() == 0) {
         selectCenter = "all";
      }

      Long dayClusterId = null;
      String tid = "";

      try {
         dayClusterId = this.dayClusterDB.getDayClusterId(dimDay, selectCenter, clusterTableRequest.getDataSource());
         return this.dayClusterDB.getCluserTelephoneId(dayClusterId, clusterTableRequest.getClusterId());
      } catch (Exception var9) {
         this.logger.info("获取聚类流水号集合失败！");
         throw new ViePlatformServiceException("获取聚类流水号集合失败！", var9);
      }
   }

   public List<ClusterResponse> sortDayCluster(String result) {
      List<ClusterResponse> clusterList = JSONArray.parseArray(result, ClusterResponse.class);
      Collections.sort(clusterList);
      return clusterList;
   }

   public String getCenter(AuthorizeInfo authorizeInfo, String dataTypeDimension) throws ViePlatformServiceException {
      ContextRequest cr = new ContextRequest();
      cr.setContextPath(authorizeInfo.getSystemId());
      cr.setUserId(Long.parseLong(authorizeInfo.getUserId()));
      cr.setDataSource(authorizeInfo.getDataSource());
      this.logger.info("userid" + authorizeInfo.getSystemId());
      Map<String, DataResourceAuth> dataMap = null;
      HashMap<String, Set<String>> hashMap = new HashMap<>();
      List<DimensionAuth> funList = null;
      List<String> valueList = null;
      String centers = "";

      try {
         dataMap = this.dataAuthService.getDataAuths(cr);
         if (dataMap == null) {
            this.logger.info("获取权限为空！");
            return "";
         } else {
            for (String ck : dataMap.keySet()) {
               if (ck.equals(authorizeInfo.getDataSource()) && dataMap.get(ck).getEnglishName().equals(authorizeInfo.getDataSource())) {
                  for (DimensionAuth da : dataMap.get(ck).getDataDimension()) {
                     Set<String> values = new HashSet<>();
                     if (da.getEnglishName().equals(dataTypeDimension)) {
                        for (int i = 0; i < da.getValue().size(); i++) {
                           values.add((String)da.getValue().get(i));
                        }
                     }

                     if (hashMap.containsKey(da.getEnglishName())) {
                        values.addAll(hashMap.get(da.getEnglishName()));
                     }

                     hashMap.put(da.getEnglishName(), values);
                  }
               }
            }

            StringBuilder sb = new StringBuilder();

            for (String ckx : hashMap.keySet()) {
               if (ckx.equals(dataTypeDimension)) {
                  for (String value : hashMap.get(ckx)) {
                     sb.append(value + ",");
                  }
               }
            }

            return sb.toString();
         }
      } catch (PermissionServiceException var15) {
         this.logger.info("获取聚类数据失败！");
         throw new ViePlatformServiceException("获取聚类数据失败！", var15);
      }
   }

   public DayClusterDB getDayClusterDB() {
      return this.dayClusterDB;
   }

   public void setDayClusterDB(DayClusterDB dayClusterDB) {
      this.dayClusterDB = dayClusterDB;
   }

   public DataAuthService getDataAuthService() {
      return this.dataAuthService;
   }

   public void setDataAuthService(DataAuthService dataAuthService) {
      this.dataAuthService = dataAuthService;
   }
}
