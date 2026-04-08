package com.iflytek.vie.app.provider.impl.search;

import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.common.ColumnMap;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.search.SearchRequest;
import com.iflytek.vie.app.pojo.search.SearchResultResponse;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.ModelGroupDB;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.pojo.RequestData;
import com.iflytek.vie.pojo.ResponseData;
import com.iflytek.vie.utils.SparkUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkSearchQuery extends SearchQuery {
   private final Logger logger = LoggerFactory.getLogger(SparkSearchQuery.class);
   private String modelAnydrillUrl;
   private CommonData commonData;
   private SparkUtils sparkUtils;
   private int batchNum;

   protected SparkSearchQuery(SparkUtils sparkUtils, int batchNum, CommonData commonData) {
      this.sparkUtils = sparkUtils;
      this.batchNum = batchNum;
      this.commonData = commonData;
   }

   protected SparkSearchQuery(String modelAnydrillUrl, SparkUtils sparkUtils, int batchNum, CommonData commonData) {
      this.modelAnydrillUrl = modelAnydrillUrl;
      this.sparkUtils = sparkUtils;
      this.batchNum = batchNum;
      this.commonData = commonData;
   }

   @Override
   public SearchResultResponse queryTextSearchList(
      SearchRequest searchRequest, LinkedHashMap<String, DimensionConfig> dimensionMap, List<HashMap<String, Object>> searchConD, String tableName
   ) throws VieAppServiceException {
      SearchResultResponse result = new SearchResultResponse();
      RequestData requestData = new RequestData();
      String userId = searchRequest.getUserId();
      String systemId = searchRequest.getSystemId();
      String startTime = searchRequest.getStartTime();
      String endTime = searchRequest.getEndTime();
      String batchId = searchRequest.getBatchId();
      Integer dataType = searchRequest.getDataType();
      Integer ifLone = searchRequest.getIfLone();
      int pageNum = searchRequest.getPageNum();
      int pageSize = searchRequest.getPageSize();
      int methodType = searchRequest.getMethodType();

      try {
         MultipleDataConstant.setMultipleDataSql(requestData, batchId, ifLone, dataType);
         HashMap<String, Set<String>> dataAuth = this.commonData.getDataMapAuth(userId, systemId, searchRequest.getDataSource());
         requestData.addDataAuthToFilterMap(dataAuth);
         requestData.setPageNum(pageNum);
         requestData.setPageSize(pageSize);
         requestData.setTableName(tableName);
         SparkSearchParse sparkParse = new SparkSearchParse(dimensionMap, startTime, endTime, requestData, searchRequest.getOptionType());
         sparkParse.dealCondition(searchConD, tableName);
         SparkSearchOption so = new SparkSearchOption(
            searchRequest.getContent(),
            tableName,
            dimensionMap,
            searchRequest.getChanel(),
            searchRequest.getOptionType(),
            searchRequest.getSearchType(),
            searchRequest.getSelColMap(),
            requestData
         );
         so.setSearchSql();
         if (StringUtils.isNullOrEmpry(so.getErrorMsg())) {
            ResponseData responseData = this.sparkUtils.execute(requestData);
            if (responseData == null) {
               return result;
            } else {
               long totalCount = responseData.getTotalCount();
               List<LinkedHashMap<String, Object>> rsList = responseData.getRsList();
               List<LinkedHashMap<String, Object>> data = this.handleListData(rsList, so.getChanel(), so.switchColMap, methodType);
               if (methodType != 1) {
                  this.setAllRec(tableName, batchId, ifLone, dataType, startTime, endTime, searchRequest.getOptionType(), dimensionMap, searchConD, result);
               } else {
                  this.groupTextBatchResult(data, totalCount, requestData, so);
               }

               result.setRows(data);
               result.setTotal(totalCount);
               return result;
            }
         } else {
            throw new VieAppServiceException(1002, so.getErrorMsg());
         }
      } catch (VieAppServiceException var25) {
         this.logger.error("queryTextSearchList service has a error!", var25);
         if (var25.getErrorCode() == 1002) {
            throw new VieAppServiceException(var25.getMessage(), var25);
         } else {
            throw new VieAppServiceException("服务内部错误", var25);
         }
      } catch (Exception var26) {
         this.logger.error("queryTextSearchList service has a error!", var26);
         throw new VieAppServiceException("服务内部错误", var26);
      }
   }

   @Override
   public SearchResultResponse queryTableSearchList(
      SearchRequest searchRequest,
      LinkedHashMap<String, DimensionConfig> dimensionMap,
      List<HashMap<String, Object>> searchConD,
      String tableName,
      LinkedHashMap<String, String> selOthersMap,
      List<ColumnMap> showCols,
      ModelDB modelDB,
      ModelGroupDB modelGroupDB,
      GroupAuthService groupAuthService
   ) throws VieAppServiceException {
      SearchResultResponse result = new SearchResultResponse();
      result.setColumns(showCols);
      RequestData requestData = new RequestData();
      String userId = searchRequest.getUserId();
      String systemId = searchRequest.getSystemId();
      String startTime = searchRequest.getStartTime();
      String endTime = searchRequest.getEndTime();
      String batchId = searchRequest.getBatchId();
      Integer dataType = searchRequest.getDataType();
      Integer ifLone = searchRequest.getIfLone();
      int methodType = searchRequest.getMethodType();

      try {
         MultipleDataConstant.setMultipleDataSql(requestData, batchId, ifLone, dataType);
         HashMap<String, Set<String>> dataAuth = this.commonData.getDataMapAuth(userId, systemId, searchRequest.getDataSource());
         requestData.addDataAuthToFilterMap(dataAuth);
         int pageNum = searchRequest.getPageNum();
         int pageSize = searchRequest.getPageSize();
         requestData.setPageNum(pageNum);
         requestData.setPageSize(pageSize);
         requestData.setTableName(tableName);
         SparkSearchParse sparkParse = new SparkSearchParse(dimensionMap, startTime, endTime, requestData, searchRequest.getOptionType());
         sparkParse.dealCondition(searchConD, tableName);
         SparkSearchOption so = new SparkSearchOption(
            searchRequest.getContent(),
            tableName,
            dimensionMap,
            searchRequest.getChanel(),
            searchRequest.getOptionType(),
            searchRequest.getSearchType(),
            selOthersMap,
            requestData
         );
         so.setTableSearchSql();
         if (StringUtils.isNullOrEmpry(so.getErrorMsg())) {
            String sortColumn = searchRequest.getSortColumn();
            if (!"".equals(sortColumn) && sortColumn != null) {
               String orderDesc = " order by " + sortColumn + " " + searchRequest.getSortType();
               requestData.setOrderDesc(orderDesc);
            }

            if (!requestData.isIfRelateContent() && requestData.isIfQueryInAllModel()) {
               String columnStr = this.getColumnStr(requestData.getColumn());
               if (StringUtils.isNotNullAndEmpry(columnStr)) {
                  requestData.setGroupExpression("group by " + columnStr);
               }
            }

            ResponseData responseData = this.sparkUtils.execute(requestData);
            if (responseData == null) {
               return result;
            } else {
               long totalCount = responseData.getTotalCount();
               List<Long> groupIds = this.getModelGroupIds(groupAuthService, 1, userId, searchRequest.getDataSource());
               HashMap<Integer, String> allModels = this.getAllModels(modelGroupDB, modelDB, groupIds, tableName, null, searchRequest.getDataSource());
               List<LinkedHashMap<String, Object>> rsList = responseData.getRsList();
               String modelInfo = "offLineTagId,offLineTagInfo";
               List<LinkedHashMap<String, Object>> list = this.dealAnydrillReturenValue(
                  this.modelAnydrillUrl.split(";"), rsList, tableName, modelInfo, allModels, methodType
               );
               if (methodType == 1) {
                  this.groupTableBatchResult(list, totalCount, modelInfo, allModels, requestData, methodType);
               } else {
                  this.setAllRec(tableName, batchId, ifLone, dataType, startTime, endTime, searchRequest.getOptionType(), dimensionMap, searchConD, result);
               }

               result.setRows(list);
               result.setTotal(totalCount);
               return result;
            }
         } else {
            throw new VieAppServiceException(1002, so.getErrorMsg());
         }
      } catch (VieAppServiceException var34) {
         this.logger.error("queryTableSearchList service has a error!", var34);
         if (var34.getErrorCode() == 1002) {
            throw new VieAppServiceException(var34.getMessage(), var34);
         } else {
            throw new VieAppServiceException("服务内部错误", var34);
         }
      } catch (Exception var35) {
         this.logger.error("queryTableSearchList service has a error!", var35);
         throw new VieAppServiceException("服务内部错误", var35);
      }
   }

   private void groupTableBatchResult(
      List<LinkedHashMap<String, Object>> list, long totalCount, String modelInfo, HashMap<Integer, String> allModels, RequestData requestData, int methodType
   ) {
      if (totalCount > this.batchNum) {
         int totalPage = (int)(totalCount / this.batchNum + 1L);
         List<LinkedHashMap<String, Object>> batchList = null;

         for (int i = 2; i <= totalPage; i++) {
            requestData.setPageNum(i);
            requestData.setPageSize(this.batchNum);

            try {
               ResponseData responseData = this.sparkUtils.execute(requestData);
               if (responseData != null) {
                  List<LinkedHashMap<String, Object>> rsList = responseData.getRsList();
                  batchList = this.dealAnydrillReturenValue(
                     this.modelAnydrillUrl.split(";"), rsList, requestData.getTableName(), modelInfo, allModels, methodType
                  );
                  if (batchList != null && batchList.size() > 0) {
                     if (list == null) {
                        list = new ArrayList<>();
                     }

                     list.addAll(batchList);
                  }
               }
            } catch (Exception var13) {
               this.logger.error("groupTableBatchResult method has a error!", var13);
            }
         }
      }
   }

   private void setAllRec(
      String tableName,
      String batchId,
      Integer ifLone,
      Integer dataType,
      String startTime,
      String endTime,
      String optionType,
      LinkedHashMap<String, DimensionConfig> dimensionMap,
      List<HashMap<String, Object>> searchConD,
      SearchResultResponse result
   ) {
      try {
         RequestData requestDataAll = new RequestData();
         requestDataAll.setTableName(tableName);
         requestDataAll.addColumn("voiceId");
         MultipleDataConstant.setMultipleDataSql(requestDataAll, batchId, ifLone, dataType);
         SparkSearchParse sparkParse = new SparkSearchParse(dimensionMap, startTime, endTime, requestDataAll, optionType);
         sparkParse.dealCondition(searchConD, tableName);
         if (requestDataAll.isIfQueryInAllModel()) {
            requestDataAll.setFuncExpression("count(DISTINCT voiceId) as count");
         } else {
            requestDataAll.setFuncExpression("count(voiceId) as count");
         }

         ResponseData responseDataAll = this.sparkUtils.execute(requestDataAll);
         if (responseDataAll == null) {
            return;
         }

         List<LinkedHashMap<String, Object>> datac = responseDataAll.getRsList();
         if (datac != null && datac.size() > 0) {
            if (!String.valueOf(datac.get(0).get("count")).contains("E") && !String.valueOf(datac.get(0).get("count")).contains("e")) {
               result.setAllRec(String.valueOf(datac.get(0).get("count")).replace(".0", ""));
            } else {
               result.setAllRec(String.valueOf(datac.get(0).get("count")));
            }
         }
      } catch (Exception var15) {
         this.logger.error("setAllRec service has a error!", var15);
      }
   }

   private void groupTextBatchResult(List<LinkedHashMap<String, Object>> data, long totalCount, RequestData requestData, SparkSearchOption so) {
      if (totalCount > this.batchNum) {
         int totalPage = (int)(totalCount / this.batchNum + 1L);
         List<LinkedHashMap<String, Object>> batchList = null;

         for (int i = 2; i <= totalPage; i++) {
            requestData.setPageNum(i);
            requestData.setPageSize(this.batchNum);

            try {
               ResponseData responseData = this.sparkUtils.execute(requestData);
               if (responseData != null) {
                  List<LinkedHashMap<String, Object>> rsList = responseData.getRsList();
                  batchList = this.handleListData(rsList, so.getChanel(), so.switchColMap, 1);
                  if (batchList != null && batchList.size() > 0) {
                     if (data == null) {
                        data = new ArrayList<>();
                     }

                     data.addAll(batchList);
                  }
               }
            } catch (Exception var11) {
               this.logger.error("groupTextBatchResult method has a error!", var11);
            }
         }
      }
   }

   private String getColumnStr(List<String> columnList) {
      StringBuffer sb = new StringBuffer();
      if (columnList != null && columnList.size() > 0) {
         for (int i = 0; i < columnList.size(); i++) {
            sb.append(columnList.get(i) + ",");
         }
      }

      if (sb.length() > 0) {
         sb.deleteCharAt(sb.length() - 1);
      }

      return sb.toString();
   }
}
