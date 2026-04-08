package com.iflytek.vie.app.provider.common;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.dimension.DimensionService;
import com.iflytek.vie.app.api.permission.DataAuthService;
import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.DimensionPersonal;
import com.iflytek.vie.app.pojo.dimension.DimensionRequest;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.app.pojo.permission.ContextRequest;
import com.iflytek.vie.app.pojo.permission.DataResourceAuth;
import com.iflytek.vie.app.pojo.permission.DimensionAuth;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.pojo.topicgroup.ColumnMap;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.TopicDB;
import com.iflytek.vie.app.provider.ruleparse.FilterParser;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.utils.ListUtil;
import com.iflytek.vie.utils.StringUtils;
import java.beans.PropertyDescriptor;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonData {
   private static final Logger logger = LoggerFactory.getLogger(CommonData.class);
   protected Log log;
   private DataAuthService dataAuthService;
   private GroupAuthService groupAuthService;
   private DimensionService dimensionService;
   private DataSourceInfo dataSourceInfo;
   private List<HashMap<String, String>> measureList = new ArrayList<>();
   public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
   protected ObjectMapper mapper = new ObjectMapper();
   public List<Long> oppositeModelIdList = new ArrayList<>();
   public List<Long> modelIdList = new ArrayList<>();
   public ModelDB modelDB;
   public TopicDB topicDB;
   public String searchDimension;
   public String sortColumn;
   public DecimalFormat df = new DecimalFormat("0.00");

   public String getDataSql(String userId, String systemId, String dataSource) {
      logger.info("the userid is : " + userId);
      logger.info("the systemid is : " + systemId);
      logger.info("the dataSource is" + dataSource);
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
            for (Entry<String, DataResourceAuth> entry : dataMap.entrySet()) {
               if (entry.getKey().equals(dataSource) && entry.getValue().getEnglishName().equals(dataSource)) {
                  if (sb.toString().trim().length() > 1) {
                     sb.append(" or ");
                  }

                  funList = entry.getValue().getDataDimension();
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
         logger.error("[获取数据权限(getDataSql)]出现异常", var18);
      }

      return sb.toString();
   }

   public HashMap<String, Set<String>> getDataMapAuth(String userId, String systemId, String dataSource) {
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
         logger.error("内部服务错误", var7);
      }

      return authMap;
   }

   public List<String> getDataStringList(String userId, String systemId, String dataSource) {
      logger.info("the userid is : " + userId);
      logger.info("the systemid is : " + systemId);
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
         logger.error("内部服务错误", var7);
      }

      return list;
   }

   private List<String> analyzeDataAuthList(Map<String, DataResourceAuth> data, String dataSource) {
      List<String> dataAuthList = new ArrayList<>();

      try {
         if (data != null) {
            DataResourceAuth dataResourceAuth = null;

            for (Entry<String, DataResourceAuth> entry : data.entrySet()) {
               if (entry.getKey().equals(dataSource)) {
                  dataResourceAuth = entry.getValue();
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
         logger.error("内部服务错误", var10);
      }

      return dataAuthList;
   }

   public List<DimensionAuth> getDataMap(long userId, String context, String dataSource) {
      List<DimensionAuth> dataMap = new ArrayList<>();
      ContextRequest request = new ContextRequest();
      request.setUserId(userId);
      request.setContextPath(context);
      request.setDataSource(dataSource);

      try {
         Map<String, DataResourceAuth> map = this.dataAuthService.getDataAuths(request);
         if (map == null) {
            return dataMap;
         }

         if (!map.containsKey(dataSource)) {
            return dataMap;
         }

         DataResourceAuth dataAuth = map.get(dataSource);
         if (dataAuth != null) {
            LinkedHashMap<String, Object> list = this.beanToMap(dataAuth);
            dataMap = (List<DimensionAuth>)list.get("dataDimension");
         }
      } catch (PermissionServiceException var10) {
         logger.info("getDataMap方法出现异常", var10);
      }

      return dataMap;
   }

   private LinkedHashMap<String, Object> beanToMap(Object obj) {
      LinkedHashMap<String, Object> params = new LinkedHashMap<>();

      try {
         PropertyUtilsBean propertyUtilsBean = new PropertyUtilsBean();
         PropertyDescriptor[] descriptors = propertyUtilsBean.getPropertyDescriptors(obj);

         for (int i = 0; i < descriptors.length; i++) {
            String name = descriptors[i].getName();
            if (!"class".equals(name)) {
               params.put(name, propertyUtilsBean.getNestedProperty(obj, name));
            }
         }
      } catch (Exception var7) {
         logger.info("javaBean转Map出现异常", var7);
      }

      return params;
   }

   public LinkedHashMap<String, DimensionConfig> getDimensionMap(String dataSource) throws ViePlatformServiceException {
      DimensionRequest request = new DimensionRequest();
      request.setDataSource(dataSource);
      return this.dimensionService.getDimensionMap(request);
   }

   public HashMap<String, Object> dealPathCondition(String pathCondition, String dataSourceName) throws Exception {
      this.oppositeModelIdList.clear();
      this.modelIdList.clear();
      HashMap<String, Object> returnHm = new HashMap<>();
      if (StringUtils.isNullOrEmpry(pathCondition)) {
         logger.error("路径条件为空");
         return null;
      } else {
         List<HashMap<String, Object>> pcList = (List<HashMap<String, Object>>)this.mapper.readValue(pathCondition, List.class);
         HashMap<String, String> dataSourceHm = this.dataSourceInfo.getSysDataSource(dataSourceName);
         String dataSource = dataSourceHm.get("dateSource");
         String modelRule = "";
         List<HashMap<String, Object>> dimensionList = new ArrayList<>();

         for (HashMap<String, Object> pchm : (List<HashMap<String, Object>>)pcList) {
            String type = pchm.get("type").toString();
            String isNegate = pchm.get("isNegate").toString();
            String exclude = "true";
            if ("0".equals(isNegate)) {
               exclude = "false";
            }

            if ("model".equals(type)) {
               long modelId = Long.parseLong(pchm.get("value").toString());
               int isFalse = Integer.parseInt(pchm.get("isNegate") + "");
               if (isFalse == 1) {
                  this.oppositeModelIdList.add(modelId);
               } else {
                  this.modelIdList.add(modelId);
               }
            } else {
               HashMap<String, Object> dimensionHm = new HashMap<>();
               String filed = pchm.get("field").toString();
               String value = pchm.get("value").toString();
               String[] valueArr = value.split(",");
               DimensionConfig dimensionConfig = this.dimensionService.getValuesOfDim(dataSource, filed);
               dimensionHm.put("key", filed);
               dimensionHm.put("dataType", dimensionConfig.getDataType());
               dimensionHm.put("name", dimensionConfig.getShowName());
               dimensionHm.put("type", dimensionConfig.getShowType());
               dimensionHm.put("value", valueArr);
               dimensionHm.put("exclude", exclude);
               dimensionList.add(dimensionHm);
            }
         }

         if (!StringUtils.isNullOrEmpry(modelRule)) {
            modelRule = modelRule.substring(0, modelRule.length() - 1);
         } else {
            modelRule = "";
         }

         returnHm.put("textRule", modelRule);
         returnHm.put("dimension", dimensionList);
         return returnHm;
      }
   }

   public FilterParser getFilterParser(String dimension) throws ParseException {
      dimension = this.filterTime(dimension);
      FilterParser filterParser = new FilterParser(dimension);
      filterParser.parserFilters();
      return filterParser;
   }

   private String filterTime(String dimension) {
      ObjectMapper mapper = new ObjectMapper();

      try {
         List<HashMap<String, Object>> dimensionList = (List<HashMap<String, Object>>)mapper.readValue(dimension, List.class);
         List<HashMap<String, Object>> dl = new ArrayList<>();

         for (int i = 0; i < dimensionList.size(); i++) {
            HashMap<String, Object> hs = dimensionList.get(i);
            if (!hs.containsValue("silenceLong") && !hs.containsValue("duration") && !hs.containsValue("inVadDuration")) {
               dl.add(i, hs);
            } else {
               List<String> valueList = (List<String>)hs.get("value");
               List<String> resultList = new ArrayList<>();

               for (int j = 0; j < valueList.size(); j++) {
                  String[] arr = valueList.get(j).split("~");
                  if (arr.length > 1) {
                     String st = Integer.parseInt(arr[0]) * 1000 - 500 + "";
                     String le = Integer.parseInt(arr[1]) * 1000 + 499 + "";
                     resultList.add(st + "~" + le);
                  }
               }

               hs.put("value", resultList);
               dl.add(i, hs);
            }
         }

         return mapper.writeValueAsString(dl);
      } catch (Exception var13) {
         logger.error("[更改时间设置(filterTime)]出现异常", var13);
         return "";
      }
   }

   public String getPathConditionName(String pathCondition, int num, String dataSource) throws Exception {
      List<HashMap<String, Object>> pcList = (List<HashMap<String, Object>>)this.mapper.readValue(pathCondition, List.class);
      HashMap<String, Object> pcHm = pcList.get(num);
      String type = pcHm.get("type").toString();
      String result = "";
      if ("model".equals(type)) {
         long modelId = Long.parseLong(pcHm.get("value").toString());
         ModelInfo modelInfo = this.modelDB.queryModelById(modelId);
         int isFalse = Integer.parseInt(pcHm.get("isNegate") + "");
         if (isFalse == 1) {
            result = "!模型:" + modelInfo.getModelName();
         } else {
            result = "模型:" + modelInfo.getModelName();
         }
      } else {
         String name = String.valueOf(pcHm.get("field"));
         List<DimensionConfig> dcList = this.dimensionService.getSystemDimensionByField(name, dataSource);
         if (!ListUtil.isNullOrEmpty(dcList) && dcList.size() > 0) {
            name = dcList.get(0).getShowName();
         } else {
            List<String> nameList = new ArrayList<>();
            nameList.add(name);
            List<DimensionPersonal> dpList = this.dimensionService.getDimensionByAnoterName(nameList);
            if (dpList != null && dpList.size() > 0) {
               name = dpList.get(0).getDimensionName();
            }
         }

         String value = String.valueOf(pcHm.get("value"));
         result = name + ":" + value.replace(",", "、");
      }

      return result;
   }

   public List<String> getFunnelColumn() {
      List<String> columnList = new ArrayList<>();
      columnList.add("流失量");
      columnList.add("转化量");
      columnList.add("转化率");
      return columnList;
   }

   public ColumnInfo getColumnMaps2(String dimension, String dataSource) {
      ColumnInfo columnInfo = new ColumnInfo();
      int insightType = IndexConstants.getInsightType(dataSource);
      if (insightType == 0) {
         columnInfo.getColumnMaps().add(new ColumnMap("录音编号", "id"));
      } else if (insightType == 1) {
         columnInfo.getColumnMaps().add(new ColumnMap("任务号", "id"));
      }

      columnInfo.getColumnMaps().add(new ColumnMap("通话时长（秒）", "duration"));
      columnInfo.getColumns().add("id");
      columnInfo.getColumns().add("duration");
      if (dimension != null && !"".equals(dimension)) {
         List<Object> list = (List<Object>)JSON.parseObject(dimension, List.class);
         if (list != null && list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
               JSONObject jsonObject = (JSONObject)list.get(i);
               ColumnMap columnMap = new ColumnMap(jsonObject.getString("columnName"), jsonObject.getString("column"));
               columnInfo.getColumnMaps().add(columnMap);
               columnInfo.getColumns().add(columnMap.getColumn());
            }
         }
      }

      columnInfo.getColumnMaps().add(new ColumnMap("关键词", "keyword"));
      columnInfo.getColumnMaps().add(new ColumnMap("模型", "modelName"));
      return columnInfo;
   }

   public List<Long> getGroupIdList(int groupType, Long userId) throws VieAppServiceException {
      GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
      groupAuthRequest.setUserId(userId);
      groupAuthRequest.setGroupType(groupType);

      try {
         return this.groupAuthService.queryGroupAuth(groupAuthRequest);
      } catch (Exception var5) {
         logger.error("获取组id列表出错", var5);
         throw new VieAppServiceException("获取组id列表出错", var5);
      }
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
         logger.error("method getMapDataAuth exception");
         throw new RuntimeException(var14);
      }
   }

   public DataAuthService getDataAuthService() {
      return this.dataAuthService;
   }

   public void setDataAuthService(DataAuthService dataAuthService) {
      this.dataAuthService = dataAuthService;
   }

   public List<HashMap<String, String>> getMeasureList() {
      return this.measureList;
   }

   public void setMeasureList(List<HashMap<String, String>> measureList) {
      this.measureList = measureList;
   }

   public SimpleDateFormat getSdf() {
      return this.sdf;
   }

   public void setSdf2(SimpleDateFormat sdf) {
      this.sdf = sdf;
   }

   public ObjectMapper getMapper() {
      return this.mapper;
   }

   public void setMapper(ObjectMapper mapper) {
      this.mapper = mapper;
   }

   public List<Long> getOppositeModelIdList() {
      return this.oppositeModelIdList;
   }

   public void setOppositeModelIdList(List<Long> oppositeModelIdList) {
      this.oppositeModelIdList = oppositeModelIdList;
   }

   public List<Long> getModelIdList() {
      return this.modelIdList;
   }

   public void setModelIdList(List<Long> modelIdList) {
      this.modelIdList = modelIdList;
   }

   public ModelDB getModelDB() {
      return this.modelDB;
   }

   public void setModelDB(ModelDB modelDB) {
      this.modelDB = modelDB;
   }

   public TopicDB getTopicDB() {
      return this.topicDB;
   }

   public void setTopicDB(TopicDB topicDB) {
      this.topicDB = topicDB;
   }

   public String getSearchDimension() {
      return this.searchDimension;
   }

   public void setSearchDimension(String searchDimension) {
      this.searchDimension = searchDimension;
   }

   public void setDimensionService(DimensionService dimensionService) {
      this.dimensionService = dimensionService;
   }

   public void setGroupAuthService(GroupAuthService groupAuthService) {
      this.groupAuthService = groupAuthService;
   }

   public String getSortColumn() {
      return this.sortColumn;
   }

   public void setSortColumn(String sortColumn) {
      this.sortColumn = sortColumn;
   }

   public Log getLog() {
      return this.log;
   }

   public void setLog(Log log) {
      this.log = log;
   }

   public DecimalFormat getDf() {
      return this.df;
   }

   public void setDf(DecimalFormat df) {
      this.df = df;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }
}
