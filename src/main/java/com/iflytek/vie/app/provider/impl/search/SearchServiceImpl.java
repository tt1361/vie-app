package com.iflytek.vie.app.provider.impl.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.dimension.DimensionService;
import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.api.search.SearchService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.common.ColumnInfo;
import com.iflytek.vie.app.pojo.common.ColumnMap;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.DimensionRequest;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.pojo.search.ModelKeyWord;
import com.iflytek.vie.app.pojo.search.SearchKeyWordRequest;
import com.iflytek.vie.app.pojo.search.SearchKeyWordResponse;
import com.iflytek.vie.app.pojo.search.SearchRequest;
import com.iflytek.vie.app.pojo.search.SearchResultResponse;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.ModelGroupDB;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.LoadCommonConfig;
import com.iflytek.vie.utils.SparkUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchServiceImpl implements SearchService {
   private final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);
   private DataSourceInfo dataSourceInfo;
   private DimensionService dimensionService;
   private CommonData commonData;
   private ModelDB modelDB;
   private ModelGroupDB modelGroupDB;
   private GroupAuthService groupAuthService;
   private SparkUtils sparkUtils;
   private final int batchNum = 10000;
   private final long defaultMaxExportNum = 100000L;
   private final DecimalFormat df = new DecimalFormat("0.00");

   public SearchResultResponse queryTextSearchList(SearchRequest searchRequest) throws VieAppServiceException {
      SearchResultResponse result = null;
      if (searchRequest == null) {
         throw new VieAppServiceException("参数传递有误");
      } else {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("dataSource");
            keyList.add("systemId");
            keyList.add("userId");
            keyList.add("startTime");
            keyList.add("endTime");
            keyList.add("chanel");
            keyList.add("optionType");
            keyList.add("searchType");
            keyList.add("selColMap");
            String checkResult = this.checkRequestParamNull(searchRequest, keyList);
            if (checkResult != null) {
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            }

            String dimensionParams = searchRequest.getDimensionParams();
            String dataSource = searchRequest.getDataSource();
            DimensionRequest request = new DimensionRequest();
            request.setDataSource(dataSource);
            LinkedHashMap<String, DimensionConfig> dimensionMap = this.dimensionService.getDimensionMap(request);
            List<HashMap<String, Object>> searchConD = null;
            if (!StringUtils.isNullOrEmpry(dimensionParams)) {
               ObjectMapper mapper = new ObjectMapper();
               searchConD = (List<HashMap<String, Object>>)mapper.readValue(dimensionParams, new TypeReference<List<HashMap<String, Object>>>() {});
            }

            SearchQuery searchQuery = null;
            if (dimensionParams.indexOf("offLineTagId") == -1 && !"2".equals(LoadCommonConfig.get("es.query.mode"))) {
               String anydrillAddressMain = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
               searchQuery = new AnydrillSearchQuery(anydrillAddressMain, 10000, this.commonData);
            } else {
               searchQuery = new SparkSearchQuery(this.sparkUtils, 10000, this.commonData);
            }

            result = searchQuery.queryTextSearchList(searchRequest, dimensionMap, searchConD, dataSource);
         } catch (VieAppServiceException var12) {
            this.logger.error(var12.getMessage());
         } catch (Exception var13) {
            this.logger.error("queryTextSearchList service has a error!", var13);
         }

         return result;
      }
   }

   public SearchResultResponse queryTableSearchList(SearchRequest searchRequest) throws VieAppServiceException {
      SearchResultResponse result = null;
      if (searchRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("dataSource");
            keyList.add("systemId");
            keyList.add("userId");
            keyList.add("startTime");
            keyList.add("endTime");
            keyList.add("chanel");
            keyList.add("optionType");
            keyList.add("searchType");
            String checkResult = this.checkRequestParamNull(searchRequest, keyList);
            if (checkResult != null) {
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            }

            String dimensionParams = searchRequest.getDimensionParams();
            String dataSource = searchRequest.getDataSource();
            DimensionRequest request = new DimensionRequest();
            request.setDataSource(dataSource);
            LinkedHashMap<String, DimensionConfig> dimensionMap = this.dimensionService.getDimensionMap(request);
            LinkedHashMap<String, String> selOthersMap = new LinkedHashMap<>();
            ColumnInfo columnInfo = this.getColumnMaps(searchRequest.getSearchDimension());
            List<String> selCols = columnInfo.getColumns();

            for (String eachCol : selCols) {
               selOthersMap.put(eachCol, eachCol);
            }

            List<ColumnMap> showCols = new ArrayList<>();
            if (IndexConstants.getInsightType(searchRequest.getDataSource()) == 0) {
               showCols.add(new ColumnMap("流水号", "id"));
            } else {
               showCols.add(new ColumnMap("任务号", "id"));
            }

            this.dealSelColumns(selCols, dimensionMap, showCols);
            showCols.add(new ColumnMap("模型", "modelName"));
            showCols.add(new ColumnMap("关键词", "keyword"));
            List<HashMap<String, Object>> searchConD = null;
            if (!StringUtils.isNullOrEmpry(dimensionParams)) {
               ObjectMapper mapper = new ObjectMapper();
               searchConD = (List<HashMap<String, Object>>)mapper.readValue(dimensionParams, new TypeReference<List<HashMap<String, Object>>>() {});
            }

            String anydrillAddressMain = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
            String anydrillAddressModel = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
            SearchQuery searchQuery = null;
            if (dimensionParams.indexOf("offLineTagId") == -1 && !"2".equals(LoadCommonConfig.get("es.query.mode"))) {
               searchQuery = new AnydrillSearchQuery(anydrillAddressMain, anydrillAddressModel, 10000, this.commonData);
            } else {
               searchQuery = new SparkSearchQuery(anydrillAddressModel, this.sparkUtils, 10000, this.commonData);
            }

            result = searchQuery.queryTableSearchList(
               searchRequest, dimensionMap, searchConD, dataSource, selOthersMap, showCols, this.modelDB, this.modelGroupDB, this.groupAuthService
            );

            for (LinkedHashMap<String, Object> res : result.getRows()) {
               String n0avgSpeed = String.valueOf(res.get("n0avgSpeed"));
               String n1avgSpeed = String.valueOf(res.get("n1avgSpeed"));
               res.put("n0avgSpeed", this.toSecondTwo(n0avgSpeed));
               res.put("n1avgSpeed", this.toSecondTwo(n1avgSpeed));
            }
         } catch (VieAppServiceException var21) {
            this.logger.error(var21.getMessage());
         } catch (Exception var22) {
            this.logger.error("queryTableSearchList service has a error!", var22);
         }

         return result;
      } else {
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public SearchKeyWordResponse queryModelKeyWord(SearchKeyWordRequest searchKeyWordRequest) throws VieAppServiceException {
      SearchKeyWordResponse result = new SearchKeyWordResponse();
      if (searchKeyWordRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("dataSource");
            keyList.add("userId");
            keyList.add("id");
            String checkResult = this.checkRequestParamNull(searchKeyWordRequest, keyList);
            if (checkResult != null) {
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            }

            String dataSource = searchKeyWordRequest.getDataSource();
            String keyword = searchKeyWordRequest.getKeyword();
            String id = searchKeyWordRequest.getId();
            String userId = searchKeyWordRequest.getUserId();
            String batchId = searchKeyWordRequest.getBatchId();
            Integer dataType = searchKeyWordRequest.getDataType();
            Integer ifLone = searchKeyWordRequest.getIfLone();
            String multipleSql = MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType);
            int pageNum = searchKeyWordRequest.getPageNum();
            int pageSize = searchKeyWordRequest.getPageSize();
            int rownumStart = (pageNum - 1) * pageSize;
            int rownumEnd = pageNum * pageSize;
            String extendSql = "";
            if (keyword != null && !"".equals(keyword)) {
               String modelIdsStr = this.modelDB.queryModelByModelNameLike(keyword);
               if ("".equals(modelIdsStr)) {
                  return result;
               }

               extendSql = " processed = 0 and " + multipleSql + " and offLineTagId in(" + modelIdsStr + ")";
            } else {
               extendSql = " processed = 0 and " + multipleSql;
            }

            String modelInfo = "offLineTagId,offLineTagInfo";
            String voiceId = searchKeyWordRequest.getVoiceId();
            boolean isContainVoiceId = false;
            if (StringUtils.isNotNullAndEmpry(voiceId)) {
               isContainVoiceId = true;
               modelInfo = modelInfo + ",hitVoiceId";
            }

            String idStr = "'" + id + "'";
            String anydrillAddressModel = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
            Map<String, List<Map<String, Object>>> modelMap = IndexConstants.getModelInforById(
               anydrillAddressModel.split(";"), dataSource, modelInfo, idStr, extendSql
            );
            List<ModelKeyWord> modelKeyWordList = new ArrayList<>();
            ModelKeyWord modelKeyWord = null;
            int total = 0;
            if (modelMap != null && modelMap.size() > 0) {
               List<Map<String, String>> keywordlist = null;
               Set<Entry<String, List<Map<String, Object>>>> set = modelMap.entrySet();
               Iterator<Entry<String, List<Map<String, Object>>>> itor = set.iterator();
               Entry<String, List<Map<String, Object>>> entry = null;
               if (itor.hasNext()) {
                  entry = itor.next();
                  List<Map<String, Object>> keywordList = entry.getValue();
                  if (keywordList != null && keywordList.size() > 0) {
                     List<Long> groupIds = this.getModelGroupIds(1, userId, searchKeyWordRequest.getDataSource());
                     HashMap<Integer, String> allModels = this.getAllModels(groupIds, dataSource, keyword, searchKeyWordRequest.getDataSource());
                     Iterator i$ = keywordList.iterator();

                     label122:
                     while (true) {
                        Map<String, Object> hm;
                        String hitVoiceId;
                        do {
                           if (!i$.hasNext()) {
                              break label122;
                           }

                           hm = (Map<String, Object>)i$.next();
                           if (!isContainVoiceId) {
                              break;
                           }

                           hitVoiceId = String.valueOf(hm.get("hitVoiceId"));
                        } while (StringUtils.isNullOrEmpry(hitVoiceId) || !hitVoiceId.contains(voiceId));

                        modelKeyWord = new ModelKeyWord();
                        List<Map<String, String>> var49 = new ArrayList();
                        hitVoiceId = String.valueOf(hm.get("offLineTagId"));
                        if (!StringUtils.isNullOrEmpry(hitVoiceId)) {
                           String modelName = allModels.get(Integer.parseInt(hitVoiceId));
                           if (!StringUtils.isNullOrEmpry(modelName)) {
                              if (hm.get("offLineTagInfo") != null && !hm.get("offLineTagInfo").equals("[]")) {
                                 ObjectMapper mapper = new ObjectMapper();

                                 for (HashMap<String, Object> hmss : (List<HashMap<String, Object>>)mapper.readValue(String.valueOf(hm.get("offLineTagInfo")), List.class)) {
                                    Map<String, String> hms = new HashMap<>();
                                    if (!isContainVoiceId || voiceId.equals(String.valueOf(hmss.get("voiceId")))) {
                                       if (IndexConstants.getInsightType(searchKeyWordRequest.getDataSource()) == 1) {
                                          hms.put("voiceId", String.valueOf(hmss.get("voiceId")));
                                          hms.put("machineId", String.valueOf(hmss.get("machineId")));
                                          hms.put("voiceUri", String.valueOf(hmss.get("voiceUri")));
                                       }

                                       hms.put("word", String.valueOf(hmss.get("content")));
                                       hms.put("begin", String.valueOf(hmss.get("beginTime")));
                                       hms.put("end", String.valueOf(hmss.get("endTime")));
                                       if (!var49.contains(hms)) {
                                          var49.add(hms);
                                       }
                                    }
                                 }
                              }

                              total++;
                              modelKeyWord.setModelName(modelName);
                              modelKeyWord.setModelId(hitVoiceId);
                              modelKeyWord.setKeyword(var49);
                              modelKeyWordList.add(modelKeyWord);
                           }
                        }
                     }
                  }
               }

               List<ModelKeyWord> rowlist = new ArrayList<>();
               if (modelKeyWordList != null && modelKeyWordList.size() > 0) {
                  int dataEnd = rownumEnd > modelKeyWordList.size() ? modelKeyWordList.size() : rownumEnd;

                  for (int i = rownumStart; i < dataEnd; i++) {
                     rowlist.add(modelKeyWordList.get(i));
                  }
               }

               result.setRows(rowlist);
               result.setTotal(total);
            }
         } catch (VieAppServiceException var44) {
            this.logger.error(var44.getMessage());
         } catch (Exception var45) {
            this.logger.error("queryModelKeyWord service has a error!", var45);
         }

         return result;
      } else {
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public SearchResultResponse exportSearchText(SearchRequest searchRequest) throws VieAppServiceException {
      SearchResultResponse result = null;
      if (searchRequest != null) {
         try {
            searchRequest.setPageSize(10000);
            searchRequest.setPageNum(1);
            searchRequest.setMethodType(1);
            result = this.queryTextSearchList(searchRequest);
            long maxExportNum = searchRequest.getMaxExportNum() != null ? searchRequest.getMaxExportNum() : 100000L;
            if (result.getTotal() > maxExportNum) {
               throw new VieAppServiceException("导出数据超过" + maxExportNum + "条");
            }
         } catch (VieAppServiceException var5) {
            this.logger.error(var5.getMessage());
         } catch (Exception var6) {
            this.logger.error("exportSearchText service has a error!", var6);
         }

         return result;
      } else {
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public SearchResultResponse exportSearchTable(SearchRequest searchRequest) throws VieAppServiceException {
      SearchResultResponse result = null;
      if (searchRequest != null) {
         try {
            if (searchRequest.getMaxExportNum() == null) {
               throw new VieAppServiceException("参数(maxExportNum)为空");
            }

            searchRequest.setPageSize(10000);
            searchRequest.setPageNum(1);
            searchRequest.setMethodType(1);
            result = this.queryTableSearchList(searchRequest);
            long maxExportNum = searchRequest.getMaxExportNum() != null ? searchRequest.getMaxExportNum() : 100000L;
            if (result.getTotal() > maxExportNum) {
               throw new VieAppServiceException("导出数据超过" + maxExportNum + "条");
            }
         } catch (VieAppServiceException var5) {
            this.logger.error(var5.getMessage());
         } catch (Exception var6) {
            this.logger.error("exportSearchTable service has a error!", var6);
         }

         return result;
      } else {
         throw new VieAppServiceException("参数传递有误");
      }
   }

   private List<Long> getModelGroupIds(int type, String userId, String dataSource) {
      GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
      groupAuthRequest.setUserId(Long.parseLong(userId));
      groupAuthRequest.setDataSource(dataSource);
      groupAuthRequest.setGroupType(type);
      List<Long> groupId = null;

      try {
         return this.groupAuthService.queryGroupAuth(groupAuthRequest);
      } catch (PermissionServiceException var7) {
         this.logger.error("getModelGroupIds method has a error!", var7);
         return groupId;
      }
   }

   private HashMap<Integer, String> getAllModels(List<Long> groupIds, String tableName, String keyword, String dataSource) {
      HashMap<Integer, String> allModels = null;
      List<Long> allGroupIds = null;

      try {
         if (groupIds != null) {
            allGroupIds = this.modelGroupDB.getModelGroupIdById(groupIds, dataSource);
            allModels = this.modelDB.queryModelOnline(keyword, allGroupIds, tableName);
         } else {
            allModels = new HashMap<>();
         }

         return allModels;
      } catch (Exception var8) {
         this.logger.error("getAllModels method has a error!", var8);
         return new HashMap<>();
      }
   }

   private List<ColumnMap> dealSelColumns(List<String> selColums, LinkedHashMap<String, DimensionConfig> dimensionMap, List<ColumnMap> showCols) {
      DimensionConfig dc = null;

      for (String sel : selColums) {
         dc = dimensionMap.get(sel);
         if (dc != null) {
            showCols.add(new ColumnMap(dc.getShowName(), sel));
         }
      }

      return showCols;
   }

   private ColumnInfo getColumnMaps(String searchDimension) {
      ColumnInfo columnInfo = new ColumnInfo();
      columnInfo.getColumns().add("id");
      columnInfo.getColumns().add("timeFormat");
      columnInfo.getColumns().add("duration");
      if (searchDimension != null && !"".equals(searchDimension)) {
         List list = (List)JSON.parseObject(searchDimension, List.class);
         if (list != null && list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
               JSONObject jsonObject = (JSONObject)list.get(i);
               ColumnMap columnMap = new ColumnMap(jsonObject.getString("columnName"), jsonObject.getString("column"));
               columnInfo.getColumns().add(columnMap.getColumn());
            }
         }
      }

      return columnInfo;
   }

   private String checkRequestParamNull(SearchRequest searchRequest, List<String> list) {
      if (list != null && list.size() > 0) {
         String key = null;

         for (int i = 0; i < list.size(); i++) {
            key = list.get(i);
            if ("dataSource".equals(key)) {
               if (StringUtils.isNullOrEmpry(searchRequest.getDataSource())) {
                  return key;
               }
            } else if ("systemId".equals(key)) {
               if (StringUtils.isNullOrEmpry(searchRequest.getSystemId())) {
                  return key;
               }
            } else if ("userId".equals(key)) {
               if (StringUtils.isNullOrEmpry(searchRequest.getUserId())) {
                  return key;
               }
            } else if ("startTime".equals(key)) {
               if (StringUtils.isNullOrEmpry(searchRequest.getStartTime())) {
                  return key;
               }
            } else if ("endTime".equals(key)) {
               if (StringUtils.isNullOrEmpry(searchRequest.getEndTime())) {
                  return key;
               }
            } else if ("chanel".equals(key)) {
               if (searchRequest.getChanel() == null) {
                  return key;
               }
            } else if ("optionType".equals(key)) {
               if (StringUtils.isNullOrEmpry(searchRequest.getOptionType())) {
                  return key;
               }
            } else if ("searchType".equals(key)) {
               if (searchRequest.getSearchType() == null) {
                  return key;
               }
            } else if ("maxExportNum".equals(key)) {
               if (searchRequest.getMaxExportNum() == null) {
                  return key;
               }
            } else if ("selColMap".equals(key) && (searchRequest.getSelColMap() == null || searchRequest.getSelColMap().size() == 0)) {
               return key;
            }
         }
      }

      return null;
   }

   private String checkRequestParamNull(SearchKeyWordRequest searchKeyWordRequest, List<String> list) {
      if (list != null && list.size() > 0) {
         String key = null;

         for (int i = 0; i < list.size(); i++) {
            key = list.get(i);
            if ("dataSource".equals(key)) {
               if (StringUtils.isNullOrEmpry(searchKeyWordRequest.getDataSource())) {
                  return key;
               }
            } else if ("userId".equals(key)) {
               if (StringUtils.isNullOrEmpry(searchKeyWordRequest.getUserId())) {
                  return key;
               }
            } else if ("id".equals(key) && StringUtils.isNullOrEmpry(searchKeyWordRequest.getId())) {
               return key;
            }
         }
      }

      return null;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public void setDimensionService(DimensionService dimensionService) {
      this.dimensionService = dimensionService;
   }

   public void setCommonData(CommonData commonData) {
      this.commonData = commonData;
   }

   public void setModelDB(ModelDB modelDB) {
      this.modelDB = modelDB;
   }

   public void setGroupAuthService(GroupAuthService groupAuthService) {
      this.groupAuthService = groupAuthService;
   }

   public void setModelGroupDB(ModelGroupDB modelGroupDB) {
      this.modelGroupDB = modelGroupDB;
   }

   public SparkUtils getSparkUtils() {
      return this.sparkUtils;
   }

   public void setSparkUtils(SparkUtils sparkUtils) {
      this.sparkUtils = sparkUtils;
   }

   public static void main(String[] args) {
      String sql = "select child_fields.offLineTagId,child_fields.offLineTagInfo,voiceId as voiceId,timeFormat as timeFormat,Math.round(duration/1000.0) as durationtime from dpspingan where  rownum  between  1 and  15  and (processed=0 and (( year in ('2015','2016') )) and  timestamp  between 1461945600000 and 1486655999999) order by voiceId asc";
      Pattern p = Pattern.compile(".*(rownum\\s+between\\s+\\d+\\s+and\\s+\\d+).*");
      Matcher m = p.matcher(sql);
      boolean b = m.matches();
      if (m.matches()) {
         System.out.println(m.group(1));
         sql = sql.replace(m.group(1), "rownum between 100 and 1511");
         System.out.println(sql);
      }

      System.out.println(b);
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
}
