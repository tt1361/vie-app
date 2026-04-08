package com.iflytek.vie.app.provider.impl.report;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.common.ColumnMap;
import com.iflytek.vie.app.pojo.common.PagerResponse;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.report.ReportTableDataRequest;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.DimensionMappingConfig;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.pojo.RequestData;
import com.iflytek.vie.pojo.ResponseData;
import com.iflytek.vie.utils.SparkUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkQuery extends SearchQuery {
   private final Logger logger = LoggerFactory.getLogger(SparkQuery.class);
   private CommonData commonData;
   private SparkUtils sparkUtils;

   protected SparkQuery() {
   }

   protected SparkQuery(SparkUtils sparkUtils, CommonData commonData) {
      this.sparkUtils = sparkUtils;
      this.commonData = commonData;
   }

   @Override
   public PagerResponse<LinkedHashMap<String, Object>> queryVoiceList(
      ReportTableDataRequest request,
      LinkedHashMap<String, DimensionConfig> dimensionMap,
      List<HashMap<String, Object>> searchConD,
      String tableName,
      LinkedHashMap<String, String> selOthersMap,
      List<ColumnMap> showCols,
      HashMap<String, String> modelMap,
      LinkedList<String> correctModel,
      boolean modelColFlag
   ) throws VieAppServiceException {
      PagerResponse<LinkedHashMap<String, Object>> result = new PagerResponse();
      RequestData requestData = new RequestData();
      String userId = request.getUserId();
      String systemId = request.getSystemId();
      String startTime = request.getStartTime();
      String endTime = request.getEndTime();
      String batchId = request.getBatchId();
      Integer dataType = request.getDataType();
      Integer ifLone = request.getIfLone();
      String dataSource = request.getDataSource();

      try {
         List<String> columnList = new ArrayList<>();
         MultipleDataConstant.setMultipleDataSql(requestData, batchId, ifLone, dataType);
         HashMap<String, Set<String>> dataAuth = this.commonData.getDataMapAuth(userId, systemId, dataSource);
         requestData.addDataAuthToFilterMap(dataAuth);
         int pageNum = request.getPageNum();
         int pageSize = request.getPageSize();
         requestData.setPageNum(pageNum);
         requestData.setPageSize(pageSize);
         requestData.setTableName(tableName);
         SparkParse sparkParse = new SparkParse(dimensionMap, startTime, endTime, requestData, null);
         boolean containDuration = false;
         boolean containVoiceId = false;

         for (ColumnMap map : showCols) {
            columnList.add(map.getColumn());
            DimensionConfig dimensionConfig = dimensionMap.get(map.getColumn());
            if (dimensionConfig != null && (dimensionConfig.getFlag() == 1 || dimensionConfig.getFlag() == 2)) {
               containDuration = true;
            }

            if (map.getColumn().equals("voiceId") && IndexConstants.getInsightType(tableName) == 1) {
               containVoiceId = true;
            }
         }

         sparkParse.dealCondition(searchConD, containVoiceId, correctModel);
         String funcExpression = "";

         for (ColumnMap columnMap : showCols) {
            boolean flag = !columnMap.getColumn().equals(CommonParams.modelFiemld) && !columnMap.getColumn().equals(CommonParams.modelFieldInfo);
            if (flag) {
               if (containVoiceId) {
                  String column = DimensionMappingConfig.getMappingName(columnMap.getColumn()) == null
                     ? columnMap.getColumn()
                     : DimensionMappingConfig.getMappingName(columnMap.getColumn());
                  requestData.addColumn(column);
               } else {
                  requestData.addColumn(columnMap.getColumn());
               }

               if (containDuration) {
                  funcExpression = funcExpression + this.dealDimension(columnMap.getColumn(), dimensionMap, containVoiceId) + ",";
               }
            }
         }

         if (funcExpression.length() > 0 && containDuration) {
            requestData.setFuncExpression(funcExpression.substring(0, funcExpression.length() - 1));
         }

         String sortColumn = request.getSortColumn();
         if (!"".equals(sortColumn)
            && sortColumn != null
            && !CommonParams.modelFiemld.equals(sortColumn)
            && !CommonParams.modelFieldInfo.equals(sortColumn)
            && !CommonParams.keyword.equals(sortColumn)) {
            if (containVoiceId) {
               sortColumn = DimensionMappingConfig.getMappingName(sortColumn) == null ? sortColumn : DimensionMappingConfig.getMappingName(sortColumn);
            }

            String orderDesc = " order by " + sortColumn + " " + request.getSortType();
            requestData.setOrderDesc(orderDesc);
         }

         ResponseData responseData = this.sparkUtils.execute(requestData);
         if (responseData == null) {
            return result;
         } else {
            long totalCount = responseData.getTotalCount();
            String modelInfo = IndexConstants.getModelInfor(columnList);
            if (!StringUtils.isNullOrEmpry(modelInfo)) {
               requestData.setIfShowModel(true);
            }

            List<LinkedHashMap<String, Object>> rsList = responseData.getRsList();
            if (modelColFlag && containVoiceId) {
               String anydrillAddressModel = DynamicEsSource.getEsSourceByType(tableName, "modelAnydrillAddress");
               List<LinkedHashMap<String, Object>> list = new SparkQuery()
                  .dealHitVoiceValue(anydrillAddressModel.split(";"), rsList, tableName, modelInfo, modelMap, 0, columnList);
               result.setRows(list);
            } else if (modelColFlag) {
               String anydrillAddressModel = DynamicEsSource.getEsSourceByType(tableName, "modelAnydrillAddress");
               List<LinkedHashMap<String, Object>> list = this.dealModelReturenValue(
                  anydrillAddressModel.split(";"), rsList, tableName, modelInfo, modelMap, 0, columnList
               );
               result.setRows(list);
            } else {
               result.setRows(rsList);
            }

            result.setTotalRows(totalCount);
            return result;
         }
      } catch (Exception var36) {
         this.logger.error("queryTextSearchList service has a error!", var36);
         throw new VieAppServiceException("服务内部错误", var36);
      }
   }

   private String dealDimension(String colFiled, LinkedHashMap<String, DimensionConfig> dimensionMap, boolean containVoiceId) {
      String selCol = "";

      try {
         DimensionConfig dimensionConfig = dimensionMap.get(colFiled);
         if (dimensionConfig != null && (dimensionConfig.getFlag() == 1 || dimensionConfig.getFlag() == 2)) {
            if (containVoiceId) {
               colFiled = DimensionMappingConfig.getMappingName(colFiled) == null ? colFiled : DimensionMappingConfig.getMappingName(colFiled);
            }

            if (!colFiled.equals("n0avgSpeed") && !colFiled.equals("n1avgSpeed")) {
               selCol = "round(" + colFiled + "/1000.0) as " + colFiled;
            } else {
               selCol = "round(" + colFiled + "/1000.0, 2) as " + colFiled;
            }
         } else {
            if (containVoiceId) {
               colFiled = DimensionMappingConfig.getMappingName(colFiled) == null ? colFiled : DimensionMappingConfig.getMappingName(colFiled);
            }

            selCol = colFiled;
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }

      return selCol;
   }

   public SparkUtils getSparkUtils() {
      return this.sparkUtils;
   }

   public void setSparkUtils(SparkUtils sparkUtils) {
      this.sparkUtils = sparkUtils;
   }
}
