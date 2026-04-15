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
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnydrillSearchQuery extends SearchQuery {
   private final Logger logger = LoggerFactory.getLogger(AnydrillSearchQuery.class);
   private String modelAnydrillUrl;
   private String mainAnydrillUrl;
   private CommonData commonData;
   private int batchNum;

   protected AnydrillSearchQuery(String mainAnydrillUrl, int batchNum, CommonData commonData) {
      this.mainAnydrillUrl = mainAnydrillUrl;
      this.batchNum = batchNum;
      this.commonData = commonData;
   }

   protected AnydrillSearchQuery(String mainAnydrillUrl, String modelAnydrillUrl, int batchNum, CommonData commonData) {
      this.mainAnydrillUrl = mainAnydrillUrl;
      this.modelAnydrillUrl = modelAnydrillUrl;
      this.batchNum = batchNum;
      this.commonData = commonData;
   }

   @Override
   public SearchResultResponse queryTextSearchList(
      SearchRequest searchRequest, LinkedHashMap<String, DimensionConfig> dimensionMap, List<HashMap<String, Object>> searchConD, String tableName
   ) throws VieAppServiceException {
      SearchResultResponse result = new SearchResultResponse();
      String userId = searchRequest.getUserId();
      String systemId = searchRequest.getSystemId();
      String startTime = searchRequest.getStartTime();
      String endTime = searchRequest.getEndTime();
      String batchId = searchRequest.getBatchId();
      Integer dataType = searchRequest.getDataType();
      Integer ifLone = searchRequest.getIfLone();
      String multipleSql = MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType);
      String dataSql = this.commonData.getDataSql(userId, systemId, searchRequest.getDataSource());
      int pageNum = searchRequest.getPageNum();
      int pageSize = searchRequest.getPageSize();
      int methodType = searchRequest.getMethodType();

      try {
         AnydrillSearchParse anyDrillParse = new AnydrillSearchParse(dimensionMap, tableName, startTime, endTime, dataSql);
         AnydrillSearchOption so = new AnydrillSearchOption(
            searchRequest.getContent(),
            tableName,
            searchConD,
            dimensionMap,
            anyDrillParse,
            searchRequest.getChanel(),
            searchRequest.getOptionType(),
            searchRequest.getSearchType(),
            pageNum - 1,
            pageSize,
            searchRequest.getSelColMap()
         );
         String sql = so.getSearchSql(multipleSql);
         if (StringUtils.isNullOrEmpry(so.getErrorMsg())) {
            this.logger.info("文本搜索:" + sql);
            PreparedStatement pst = new PreparedStatement(this.mainAnydrillUrl.split(";"));
            ResultSet rs = pst.executeQuery(sql);
            if (rs == null) {
               return result;
            } else {
               long totalCount = rs.getTotalCount();
               List<LinkedHashMap<String, Object>> data = rs.toList();
               this.handleListData(data, so.getChanel(), so.getSwitchColMap(), methodType);
               if (methodType != 1) {
                  sql = so.getSearchTotalCountSql(multipleSql);
                  ResultSet rsc = pst.executeQuery(sql);
                  if (rsc == null) {
                     return result;
                  }

                  List<LinkedHashMap<String, Object>> datac = rsc.toList();
                  if (datac != null && datac.size() > 0) {
                     if (!String.valueOf(datac.get(0).get("count")).contains("E") && !String.valueOf(datac.get(0).get("count")).contains("e")) {
                        result.setAllRec(String.valueOf(datac.get(0).get("count")).replace(".0", ""));
                     } else {
                        result.setAllRec(String.valueOf(datac.get(0).get("count")));
                     }
                  }
               } else {
                  this.logger.info("本次导出的总数为====" + totalCount);
                  this.groupTextBatchResult(pst, data, totalCount, sql, so, methodType);
               }

               result.setRows(data);
               result.setTotal(totalCount);
               return result;
            }
         } else {
            throw new VieAppServiceException(1002, so.getErrorMsg());
         }
      } catch (VieAppServiceException var28) {
         this.logger.error("queryTextSearchList service has a error!", var28);
         if (var28.getErrorCode() == 1002) {
            throw new VieAppServiceException(var28.getMessage(), var28);
         } else {
            throw new VieAppServiceException("服务内部错误", var28);
         }
      } catch (Exception var29) {
         this.logger.error("queryTextSearchList service has a error!", var29);
         throw new VieAppServiceException("服务内部错误", var29);
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
      String userId = searchRequest.getUserId();
      String systemId = searchRequest.getSystemId();
      String startTime = searchRequest.getStartTime();
      String endTime = searchRequest.getEndTime();
      String batchId = searchRequest.getBatchId();
      Integer dataType = searchRequest.getDataType();
      Integer ifLone = searchRequest.getIfLone();
      String multipleSql = MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType);
      String dataSql = this.commonData.getDataSql(userId, systemId, searchRequest.getDataSource());
      int pageNum = searchRequest.getPageNum();
      int pageSize = searchRequest.getPageSize();
      int methodType = searchRequest.getMethodType();
      String sortColumn = searchRequest.getSortColumn();

      try {
         AnydrillSearchParse anyDrillParse = new AnydrillSearchParse(dimensionMap, tableName, startTime, endTime, dataSql);
         AnydrillSearchOption so = new AnydrillSearchOption(
            searchRequest.getContent(),
            tableName,
            searchConD,
            dimensionMap,
            anyDrillParse,
            searchRequest.getChanel(),
            searchRequest.getOptionType(),
            searchRequest.getSearchType(),
            pageNum - 1,
            pageSize,
            selOthersMap
         );
         String sql = so.getTableSearchSql(multipleSql);
         if (!StringUtils.isNullOrEmpry(so.getErrorMsg())) {
            throw new VieAppServiceException(1002, so.getErrorMsg());
         } else {
            if (!"".equals(sortColumn) && sortColumn != null) {
               sql = sql + " order by " + sortColumn + " " + searchRequest.getSortType();
            }

            this.logger.info("列表搜索:" + sql);
            PreparedStatement pst = new PreparedStatement(this.mainAnydrillUrl.split(";"));
            ResultSet rs = pst.executeQuery(sql);
            if (rs == null) {
               return result;
            } else {
               long totalCount = rs.getTotalCount();
               List<Long> groupIds = this.getModelGroupIds(groupAuthService, 1, userId, searchRequest.getDataSource());
               HashMap<Integer, String> allModels = this.getAllModels(modelGroupDB, modelDB, groupIds, tableName, null, searchRequest.getDataSource());
               List<LinkedHashMap<String, Object>> data = rs.toList();
               String modelInfo = "offLineTagId,offLineTagInfo";
               List<LinkedHashMap<String, Object>> list = this.dealAnydrillReturenValue(
                  this.modelAnydrillUrl.split(";"), data, tableName, modelInfo, allModels, methodType
               );
               if (list != null && list.size() > 0) {
                  for (LinkedHashMap<String, Object> linkmap : list) {
                     for (Entry<String, Object> entry : linkmap.entrySet()) {
                        Object value = entry.getValue();
                        DimensionConfig dimensionConfig = dimensionMap.get(entry.getKey());
                        if (dimensionConfig != null && dimensionConfig.getFlag() == 1) {
                           value = Math.round(Double.parseDouble(String.valueOf(value)));
                           entry.setValue(value);
                        }
                     }
                  }
               }

               if (methodType == 1) {
                  this.groupTableBatchResult(pst, list, totalCount, sql, allModels, tableName, modelInfo, methodType);
               } else {
                  sql = so.getSearchTotalCountSql(multipleSql);
                  ResultSet rsc = pst.executeQuery(sql);
                  if (rsc == null) {
                     return result;
                  }

                  List<LinkedHashMap<String, Object>> datac = rsc.toList();
                  if (datac != null && datac.size() > 0) {
                     if (!String.valueOf(datac.get(0).get("count")).contains("E") && !String.valueOf(datac.get(0).get("count")).contains("e")) {
                        result.setAllRec(String.valueOf(datac.get(0).get("count")).replace(".0", ""));
                     } else {
                        result.setAllRec(String.valueOf(datac.get(0).get("count")));
                     }
                  }
               }

               result.setRows(list);
               result.setTotal(totalCount);
               return result;
            }
         }
      } catch (VieAppServiceException var42) {
         this.logger.error("queryTableSearchList service has a error!", var42);
         if (var42.getErrorCode() == 1002) {
            throw new VieAppServiceException(var42.getMessage(), var42);
         } else {
            throw new VieAppServiceException("服务内部错误", var42);
         }
      } catch (Exception var43) {
         this.logger.error("queryTableSearchList service has a error!", var43);
         throw new VieAppServiceException("服务内部错误", var43);
      }
   }

   private void groupTableBatchResult(
      PreparedStatement pst,
      List<LinkedHashMap<String, Object>> list,
      long totalCount,
      String sql,
      HashMap<Integer, String> allModels,
      String tableName,
      String modelInfo,
      int methodType
   ) {
      if (totalCount > this.batchNum) {
         int totalPage = (int)(totalCount / this.batchNum + 1L);
         String batchSql = "";
         List<LinkedHashMap<String, Object>> batchList = null;

         for (int i = 1; i < totalPage; i++) {
            int startIndex = i * this.batchNum + 1;
            int endIndex = (i + 1) * this.batchNum;
            batchSql = this.getSql(sql, startIndex, endIndex);
            this.logger.info("列表搜索" + i + ": " + batchSql);
            if (!StringUtils.isNullOrEmpry(batchSql)) {
               try {
                  ResultSet rsSet = pst.executeQuery(batchSql);
                  if (rsSet != null) {
                     List<LinkedHashMap<String, Object>> rsList = rsSet.toList();
                     batchList = this.dealAnydrillReturenValue(this.modelAnydrillUrl.split(";"), rsList, tableName, modelInfo, allModels, methodType);
                     if (batchList != null && batchList.size() > 0) {
                        if (list == null) {
                           list = new ArrayList<>();
                        }

                        list.addAll(batchList);
                     }
                  }
               } catch (Exception var18) {
                  this.logger.error("groupTableBatchResult method has a error!", var18);
               }
            }
         }
      }
   }

   private void groupTextBatchResult(
      PreparedStatement pst, List<LinkedHashMap<String, Object>> data, long totalCount, String sql, AnydrillSearchOption so, int methodType
   ) {
      if (totalCount > this.batchNum) {
         int totalPage = (int)(totalCount / this.batchNum + 1L);
         String batchSql = "";
         List<LinkedHashMap<String, Object>> batchList = null;

         for (int i = 1; i < totalPage; i++) {
            int startIndex = i * this.batchNum + 1;
            int endIndex = (i + 1) * this.batchNum;
            batchSql = this.getSql(sql, startIndex, endIndex);
            this.logger.info("文本搜索" + i + ": " + batchSql);
            if (!StringUtils.isNullOrEmpry(batchSql)) {
               try {
                  ResultSet rsSet = pst.executeQuery(batchSql);
                  if (rsSet != null) {
                     batchList = rsSet.toList();
                     this.handleListData(batchList, so.getChanel(), so.getSwitchColMap(), 1);
                     if (batchList != null && batchList.size() > 0) {
                        if (data == null) {
                           data = new ArrayList<>();
                        }

                        data.addAll(batchList);
                     }
                  }
               } catch (Exception var15) {
                  this.logger.error("groupTextBatchResult method has a error!", var15);
               }
            }
         }
      }
   }

   private String getSql(String sourceSql, int startIndex, int endIndex) {
      String sql = "";
      Pattern p = Pattern.compile(".*(rownum\\s+between\\s+\\d+\\s+and\\s+\\d+).*");
      Matcher m = p.matcher(sourceSql);
      if (m.matches()) {
         String oldStr = m.group(1);
         sql = sourceSql.replace(oldStr, "rownum between " + startIndex + " and " + endIndex);
      }

      return sql;
   }
}
