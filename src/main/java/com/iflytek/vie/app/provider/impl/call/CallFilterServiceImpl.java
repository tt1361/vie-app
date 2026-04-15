package com.iflytek.vie.app.provider.impl.call;

import com.iflytek.vie.app.api.call.CallFilterService;
import com.iflytek.vie.app.api.dimension.DimensionService;
import com.iflytek.vie.app.api.model.ModelApplyService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.call.CallFilter;
import com.iflytek.vie.app.pojo.call.CallFilterRequest;
import com.iflytek.vie.app.pojo.call.VoiceCallRequest;
import com.iflytek.vie.app.pojo.call.VoiceCallResponse;
import com.iflytek.vie.app.pojo.common.ColumnMap;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.DimensionRequest;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.CallFilterDB;
import com.iflytek.vie.constants.DimensionMappingConfig;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.StringUtils;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallFilterServiceImpl implements CallFilterService {
   private CallFilterDB callFilterDB;
   private static final Logger logger = LoggerFactory.getLogger(CallFilterServiceImpl.class);
   private Map<String, String> listType;
   private static Map<String, String> modelMap = new HashMap<>();
   private DimensionService dimensionService;
   private DataSourceInfo dataSourceInfo;
   private ModelApplyService modelApplyService;
   private final DecimalFormat df = new DecimalFormat("0.00");

   public String getDimensionByUserId(CallFilterRequest request) throws VieAppServiceException {
      if (request != null
         && request.getUserId() != null
         && !"".equals(request.getUserId())
         && request.getListType() != 0
         && request.getDataSource() != null
         && !"".equals(request.getDataSource())) {
         String filter;
         try {
            filter = this.callFilterDB.getDimensionByUserId(request.getUserId(), 0, request.getListType(), request.getDataSource());
         } catch (Exception var10) {
            logger.error("exception occur when query data from DB");
            throw new VieAppServiceException("exception occur when query data from DB", var10);
         }

         Integer insight = IndexConstants.getInsightType(request.getDataSource());
         if (filter == null) {
            filter = this.listType.get(request.getListType() + "");
            String[] filters = filter.split(",");

            for (int i = 0; i < filters.length; i++) {
               if ("id".equals(filters[i])) {
                  if (insight == 0) {
                     filters[i] = "voiceId";
                  } else {
                     filters[i] = "taskId";
                  }
                  break;
               }
            }

            String newFilter = "";

            for (String str : filters) {
               newFilter = newFilter + "," + str;
            }

            filter = newFilter.substring(1, newFilter.length());
         }

         return filter;
      } else {
         logger.error("input parameter can't be null");
         throw new VieAppServiceException("input parameter can't be null");
      }
   }

   public List<CallFilter> getFilterListByUserId(CallFilterRequest request) throws VieAppServiceException {
      if (request != null && request.getUserId() != null && !"".equals(request.getUserId()) && request.getListType() != 0 && request.getDataSource() != null) {
         try {
            return this.callFilterDB.getFiltersByUserId(request.getUserId(), 1, request.getListType(), request.getDataSource());
         } catch (Exception var3) {
            logger.error("exception occur when query data from DB", var3);
            throw new VieAppServiceException("exception occur when query data from DB", var3);
         }
      } else {
         logger.error("input parameter can't be null");
         throw new VieAppServiceException("input parameter can't be null");
      }
   }

   public void saveFilters(CallFilterRequest request) throws VieAppServiceException {
      if (request != null
         && request.getUserId() != null
         && !"".equals(request.getUserId())
         && request.getDataSource() != null
         && !"".equals(request.getDataSource())
         && request.getFilter() != null
         && !"".equals(request.getFilter())
         && request.getListType() != 0) {
         int listType = request.getListType();
         if (request.getFilterType() != 1 || request.getFilterName() != null && !"".equals(request.getFilterName())) {
            try {
               if (request.getFilterType() == 0) {
                  CallFilter callFilter = this.callFilterDB
                     .getCallFilterByUserIdAndType(request.getUserId(), request.getFilterType(), listType, request.getDataSource());
                  if (callFilter != null) {
                     this.callFilterDB.deleteCallFilterById(callFilter.getId(), request.getDataSource());
                  }

                  this.callFilterDB
                     .saveCallFilter(
                        request.getFilter(), request.getFilterName(), request.getUserId(), request.getFilterType(), listType, request.getDataSource()
                     );
               } else {
                  if (request.getId() != null && request.getId() != 0) {
                     CallFilter callFilter = this.callFilterDB.getCallFilterById(request.getId());
                     if (callFilter != null) {
                        this.callFilterDB.deleteCallFilterById(request.getId(), request.getDataSource());
                     }
                  }

                  this.callFilterDB
                     .saveCallFilter(
                        request.getFilter(), request.getFilterName(), request.getUserId(), request.getFilterType(), listType, request.getDataSource()
                     );
               }
            } catch (Exception var4) {
               logger.error("exception occur when insert data into DB");
               throw new VieAppServiceException("exception occur when insert data into DB", var4);
            }
         } else {
            logger.error("input parameter can't be null");
            throw new VieAppServiceException("input parameter can't be null");
         }
      } else {
         logger.error("input parameter can't be null");
         throw new VieAppServiceException("input parameter can't be null");
      }
   }

   public CallFilter getSingleFilterById(CallFilterRequest request) throws VieAppServiceException {
      try {
         return this.callFilterDB.getCallFilterById(request.getId());
      } catch (Exception var3) {
         logger.error("exception occur when query data from DB");
         throw new VieAppServiceException("exception occur when query data from DB", var3);
      }
   }

   public void deleteFilters(CallFilterRequest request) throws VieAppServiceException {
      try {
         this.callFilterDB.deleteCallFilterById(request.getId(), request.getDataSource());
      } catch (Exception var3) {
         logger.error("exception occur when delete data from DB");
         throw new VieAppServiceException("exception occur when delete data from DB", var3);
      }
   }

   public VoiceCallResponse queryVoiceCallList(VoiceCallRequest request) throws VieAppServiceException {
      VoiceCallResponse response = new VoiceCallResponse();
      if (StringUtils.isNullOrEmpry(request.getId())) {
         logger.error("param id can't be null");
         throw new VieAppServiceException("param id can't be null");
      } else if (StringUtils.isNullOrEmpry(request.getSearchDimension())) {
         logger.error("param searchDimension can't be null");
         throw new VieAppServiceException("param searchDimension can't be null");
      } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
         logger.error("param dataSource can't be null");
         throw new VieAppServiceException("param dataSource can't be null");
      } else if (StringUtils.isNullOrEmpry(request.getUserId())) {
         logger.error("param userId can't be null");
         throw new VieAppServiceException("param userId can't be null");
      } else {
         String[] dimensions = request.getSearchDimension().split(",");

         try {
            boolean isContainModel = false;
            boolean isContainVoiceId = false;
            boolean isContainTimeFormat = false;
            String originalName = "";
            String mappingName = "";
            StringBuffer mainSql = new StringBuffer();
            StringBuffer modelSql = new StringBuffer();
            mainSql.append("select ");
            modelSql.append("select ");
            if (dimensions != null && dimensions.length > 0) {
               String dataSource = request.getDataSource();
               DimensionRequest dimensionRequest = new DimensionRequest();
               dimensionRequest.setDataSource(dataSource);
               LinkedHashMap<String, DimensionConfig> dimensionMap = this.dimensionService.getDimensionMap(dimensionRequest);
               List<ColumnMap> showCols = this.getShowCols(dimensions, dimensionMap);
               response.setColumns(showCols);

               for (int i = 0; i < dimensions.length; i++) {
                  originalName = dimensions[i].trim();
                  if (!"sequence".equals(originalName)) {
                     mappingName = DimensionMappingConfig.getMappingName(originalName);
                     if (StringUtils.isNotNullAndEmpry(mappingName)) {
                        if ("voiceId".equals(originalName)) {
                           isContainVoiceId = true;
                        } else if ("timeFormat".equals(originalName)) {
                           isContainTimeFormat = true;
                        }

                        mainSql.append(mappingName + ", ");
                     } else if (modelMap.containsKey(originalName)) {
                        isContainModel = true;
                        modelSql.append(modelMap.get(originalName) + ", ");
                     } else {
                        mainSql.append(originalName + ", ");
                     }
                  }
               }

               if (!isContainVoiceId) {
                  mainSql.append(DimensionMappingConfig.getMappingName("voiceId") + ", ");
               }

               if (!isContainTimeFormat) {
                  mainSql.append(DimensionMappingConfig.getMappingName("timeFormat") + ", ");
               }

               if (isContainModel) {
                  modelSql.append("hitVoiceId, ");
                  if (!modelSql.toString().contains("offLineTagId")) {
                     modelSql.append("offLineTagId, ");
                  }
               }

               mainSql.delete(mainSql.length() - 2, mainSql.length());
               mainSql.append(" from " + dataSource).append(" where id = '" + request.getId() + "' ");
               String anydrillUrlMain = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
               PreparedStatement mainpst = new PreparedStatement(anydrillUrlMain.split(";"));
               logger.info("execute main sql: {}", mainSql.toString());
               ResultSet mainResultSet = mainpst.executeQuery(mainSql.toString());
               List<Map<String, Object>> list = this.dealChildInfo(mainResultSet, dimensionMap);
               Map<String, Map<String, Object>> modelInfoMap = null;
               if (list != null && list.size() > 0 && isContainModel) {
                  modelSql.delete(modelSql.length() - 2, modelSql.length());
                  String modelTableName = IndexConstants.getModelTableName(dataSource);
                  modelSql.append(" from " + modelTableName).append(" where id = '" + request.getId() + "' ");
                  if (request.getModelId() != null) {
                     modelSql.append(" and offLineTagId = " + request.getModelId());
                  }

                  String anydrillAddressModel = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
                  new ExcuteContext();
                  PreparedStatement modelpst = new PreparedStatement(anydrillAddressModel.split(";"));
                  logger.info("execute model sql: {}", modelSql.toString());
                  ResultSet modelResultSet = modelpst.executeQuery(modelSql.toString());
                  AuthorizeInfo authorizeInfo = new AuthorizeInfo();
                  authorizeInfo.setDataSource(request.getDataSource());
                  authorizeInfo.setUserId(request.getUserId());
                  Map<String, String> allModels = this.modelApplyService.getAllModelMap(authorizeInfo);
                  modelInfoMap = IndexConstants.dealModelInfor(modelResultSet, allModels, true);
               }

               this.groupResult(list, modelInfoMap);
               String sortColumn = request.getSortColumn();
               String sortType = request.getSortType();
               if (StringUtils.isNullOrEmpry(sortColumn)) {
                  sortColumn = "timeFormat";
               }

               if (StringUtils.isNullOrEmpry(sortType)) {
                  sortType = "asc";
               }

               this.sortResult(list, sortColumn, sortType, dimensionMap);
               int rownumStart = request.getPageSize() * (request.getPageNum() - 1);
               int rownumEnd = request.getPageSize() * request.getPageNum();
               int totalSize = list.size();
               if (rownumEnd > totalSize) {
                  rownumEnd = totalSize;
               }

               List<Map<String, Object>> rslist = list.subList(rownumStart, rownumEnd);
               this.addSequenceToList(rslist, rownumStart, isContainModel);
               response.setRows(rslist);
               response.setTotal(totalSize);
            }

            return response;
         } catch (Exception var28) {
            logger.error("exception occur when excute queryVoiceCallList method", var28);
            throw new VieAppServiceException("exception occur when excute queryVoiceCallList method", var28);
         }
      }
   }

   private void addSequenceToList(List<Map<String, Object>> rslist, int rownumStart, boolean isContainModel) {
      if (rslist != null && rslist.size() > 0) {
         for (int i = 0; i < rslist.size(); i++) {
            rslist.get(i).put("sequence", ++rownumStart);
            if (isContainModel) {
               rslist.get(i).remove("offLineTagId");
               rslist.get(i).remove("offLineTagInfo");
               if (!rslist.get(i).containsKey("modelName")) {
                  rslist.get(i).put("modelName", "");
               }

               if (!rslist.get(i).containsKey("keyword")) {
                  rslist.get(i).put("keyword", "");
               }
            }
         }
      }
   }

   private void sortResult(
      List<Map<String, Object>> list, final String sortColumn, final String sortType, final LinkedHashMap<String, DimensionConfig> dimensionMap
   ) {
      if (list != null && list.size() > 0) {
         Collections.sort(list, new Comparator<Map<String, Object>>() {
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
               DimensionConfig dimensionConfig = dimensionMap.get(sortColumn);
               if (dimensionConfig != null) {
                  if (!"integer".equals(dimensionConfig.getDataType()) && !"long".equals(dimensionConfig.getDataType())) {
                     String map1value = (String)o1.get(sortColumn);
                     String map2value = (String)o2.get(sortColumn);
                     return "asc".equals(sortType) ? map1value.compareTo(map2value) : map2value.compareTo(map1value);
                  } else {
                     int map1value = Integer.parseInt(String.valueOf(o1.get(sortColumn)));
                     int map2value = Integer.parseInt(String.valueOf(o2.get(sortColumn)));
                     return "asc".equals(sortType) ? map1value - map2value : map2value - map1value;
                  }
               } else {
                  return 0;
               }
            }
         });
      }
   }

   private void groupResult(List<Map<String, Object>> list, Map<String, Map<String, Object>> modelMap) {
      if (list != null && list.size() > 0 && modelMap != null && modelMap.size() > 0) {
         for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            String voiceId = String.valueOf(map.get("voiceId"));
            if (modelMap.containsKey(voiceId)) {
               map.putAll(modelMap.get(voiceId));
            }
         }
      }
   }

   private List<Map<String, Object>> dealChildInfo(ResultSet resultSet, LinkedHashMap<String, DimensionConfig> dimensionMap) {
      List<Map<String, Object>> rsList = new ArrayList<>();
      if (resultSet != null) {
         List<LinkedHashMap<String, Object>> list = resultSet.toList();
         if (list != null && list.size() > 0) {
            LinkedHashMap<String, Object> map = null;
            Set<Entry<String, Object>> set = null;
            Iterator<Entry<String, Object>> itor = null;
            Entry<String, Object> entry = null;
            String key = null;
            Object value = null;
            Set<Entry<String, Object>> childSet = null;
            Iterator<Entry<String, Object>> childItor = null;
            Entry<String, Object> childEntry = null;
            String childKey = null;
            Object childValue = null;
            String originalName = null;
            List<HashMap<String, Object>> childList = null;
            Map<String, Object> othermap = new HashMap<>();
            map = list.get(0);

            for (Entry<String, Object> var25 : map.entrySet()) {
               key = (String)var25.getKey();
               value = var25.getValue();
               if ("child_fields".equals(key)) {
                  childList = (List<HashMap<String, Object>>)value;
                  if (childList != null && childList.size() > 0) {
                     for (HashMap<String, Object> childMap : (List<HashMap<String, Object>>)childList) {
                        if (childMap != null && childMap.size() > 0) {
                           Map<String, Object> tempChildMap = new HashMap<>();

                           for (Entry<String, Object> var30 : childMap.entrySet()) {
                              childKey = (String)var30.getKey();
                              originalName = DimensionMappingConfig.getOriginalName(childKey);
                              childValue = this.dealDimension(originalName, var30.getValue(), dimensionMap);
                              tempChildMap.put(originalName, childValue);
                           }

                           rsList.add(tempChildMap);
                        }
                     }
                  }
               } else if (!modelMap.containsKey(key)) {
                  othermap.put(key, this.dealDimension(key, value, dimensionMap));
               }
            }

            this.setOtherParamToList(othermap, rsList);
         }
      }

      return rsList;
   }

   private void setOtherParamToList(Map<String, Object> map, List<Map<String, Object>> rsList) {
      if (rsList != null && rsList.size() > 0) {
         for (int i = 0; i < rsList.size(); i++) {
            Map<String, Object> rsMap = rsList.get(i);
            if (map != null && map.size() > 0) {
               Entry<String, Object> entry = null;

               for (Entry<String, Object> var8 : map.entrySet()) {
                  rsMap.put((String)var8.getKey(), var8.getValue());
               }
            }
         }
      }
   }

   private Object dealDimension(String dimensionName, Object dimensionValue, LinkedHashMap<String, DimensionConfig> dimensionMap) {
      try {
         DimensionConfig dimensionConfig = dimensionMap.get(dimensionName);
         if (dimensionConfig != null && dimensionConfig.getFlag() == 1) {
            if (dimensionValue != null) {
               dimensionValue = Math.round(Double.parseDouble(new DecimalFormat("###0").format(dimensionValue)) / 1000.0);
            }
         } else if (dimensionConfig != null && dimensionConfig.getFlag() == 2 && dimensionValue != null) {
            dimensionValue = this.toSecondTwo(dimensionValue.toString());
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      return dimensionValue;
   }

   private List<ColumnMap> getShowCols(String[] dimensions, LinkedHashMap<String, DimensionConfig> dimensionMap) {
      List<ColumnMap> showCols = new ArrayList<>();
      showCols.add(new ColumnMap("序号", "sequence"));

      try {
         this.dealSelColumns(dimensions, dimensionMap, showCols);
      } catch (Exception var5) {
         logger.error("getShowCols method has a error!", var5);
      }

      return showCols;
   }

   private List<ColumnMap> dealSelColumns(String[] dimensions, LinkedHashMap<String, DimensionConfig> dimensionMap, List<ColumnMap> showCols) {
      DimensionConfig dc = null;

      for (String sel : dimensions) {
         dc = dimensionMap.get(sel);
         if (dc != null) {
            showCols.add(new ColumnMap(dc.getShowName(), sel));
         } else if ("modelName".equals(sel)) {
            showCols.add(new ColumnMap("模型", sel));
         } else if ("keyword".equals(sel)) {
            showCols.add(new ColumnMap("关键词", sel));
         }
      }

      return showCols;
   }

   public String toSecondTwo(String millSecond) {
      if (millSecond != null && !"".equals(millSecond) && !"null".equals(millSecond)) {
         try {
            return this.df.format(this.df.parse(millSecond).floatValue() / 1000.0F);
         } catch (ParseException var3) {
            return "0";
         }
      } else {
         return "0";
      }
   }

   public void setCallFilterDB(CallFilterDB callFilterDB) {
      this.callFilterDB = callFilterDB;
   }

   public void setListType(Map<String, String> listType) {
      this.listType = listType;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public void setDimensionService(DimensionService dimensionService) {
      this.dimensionService = dimensionService;
   }

   public void setModelApplyService(ModelApplyService modelApplyService) {
      this.modelApplyService = modelApplyService;
   }

   static {
      modelMap.put("modelName", "offLineTagId");
      modelMap.put("keyword", "offLineTagInfo");
      modelMap.put("hitVoiceId", "hitVoiceId");
   }
}
