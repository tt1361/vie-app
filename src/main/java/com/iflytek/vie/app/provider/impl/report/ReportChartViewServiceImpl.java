package com.iflytek.vie.app.provider.impl.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.datadrill.DataDrillService;
import com.iflytek.vie.app.api.dimension.DimensionService;
import com.iflytek.vie.app.api.model.ModelApplyService;
import com.iflytek.vie.app.api.report.ReportChartViewService;
import com.iflytek.vie.app.api.report.ReportService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.common.ColumnMap;
import com.iflytek.vie.app.pojo.common.PagerResponse;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.DimensionRequest;
import com.iflytek.vie.app.pojo.report.ReportTableDataRequest;
import com.iflytek.vie.app.pojo.topic.TopicPath;
import com.iflytek.vie.app.provider.common.AnyDrillParse;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.common.EsQueryParse;
import com.iflytek.vie.app.provider.common.SparkSqlParse;
import com.iflytek.vie.app.provider.database.TopicDB;
import com.iflytek.vie.app.provider.pojo.CommonFun;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.CommonXmlConfig;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.constants.DimensionMappingConfig;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExceptionUtil;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.LoadCommonConfig;
import com.iflytek.vie.utils.SparkUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.anydrill.calculate.set.ResultSet;
import org.apache.commons.collections.CollectionUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportChartViewServiceImpl implements ReportChartViewService {
   private final Logger logger = LoggerFactory.getLogger(ReportChartViewServiceImpl.class);
   protected ObjectMapper mapper = new ObjectMapper();
   private ModelApplyService modelApplyService;
   private DataDrillService dataDrillService;
   private DimensionService dimensionService;
   private ReportService reportService;
   private SparkUtils sparkUtils;
   private CommonData commonData;
   private DataSourceInfo dataSourceInfo;
   private TopicDB topicDB;
   private int pieMaxCount;
   private int chartXMaxCount;
   private Map<String, String> durationMap = new HashMap<>();
   public DecimalFormat decimalFormat = new DecimalFormat("###0.0");
   public DecimalFormat longFormat = new DecimalFormat("###0");
   public DecimalFormat doubleFormat = new DecimalFormat("###0.00");
   public String sparkTemptableName = "reportStatSparkTable";

   public HashMap<String, Object> getTableData(ReportTableDataRequest request) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(request.getUserId())) {
         this.logger.error("用户id不能为空");
         throw new VieAppServiceException("用户id不能为空");
      } else if (StringUtils.isNullOrEmpry(request.getSystemId())) {
         this.logger.error("systemId不能为空");
         throw new VieAppServiceException("systemId不能为空");
      } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
         this.logger.error("数据源不能为空");
         throw new VieAppServiceException("数据源不能为空");
      } else if (StringUtils.isNullOrEmpry(request.getTableParams())) {
         this.logger.error("参数异常");
         throw new VieAppServiceException("参数异常");
      } else {
         try {
            HashMap<String, Object> tableMap = (HashMap<String, Object>)this.mapper
               .readValue(request.getTableParams(), new TypeReference<HashMap<String, Object>>() {});
            String tableType = String.valueOf(tableMap.get(CommonParams.tabType));
            new HashMap();
            String orderType = tableMap.get(CommonParams.tabOrderType) == null ? "" : tableMap.get(CommonParams.tabOrderType).toString();
            String orderField = String.valueOf(tableMap.get(CommonParams.tabOrder));
            request.setSortColumn(String.valueOf(orderField));
            request.setSortType(String.valueOf(orderType));
            if (tableType.equals("detail")) {
               if (!StringUtils.isNullOrEmpry(orderField) && !CommonParams.modelFiemld.equals(orderField) && !CommonParams.keyword.equals(orderField)) {
                  request.setSortColumn(String.valueOf(orderField));
                  request.setSortType(orderType);
               }

               return this.queryVoiceList(request.getStartTime(), request.getEndTime(), tableMap, request);
            } else {
               return this.queryDataListAnyDrillAndSpark(tableMap, request);
            }
         } catch (Exception var7) {
            if (var7 instanceof VieAppServiceException) {
               if (((VieAppServiceException)var7).getErrorCode() == 601) {
                  return new HashMap<>();
               } else {
                  this.logger.error("获取报表表格数据接口getTableData出错", var7);
                  throw new VieAppServiceException(((VieAppServiceException)var7).getErrorCode(), ((VieAppServiceException)var7).getMessage());
               }
            } else {
               this.logger.error("获取报表表格数据接口getTableData出错", var7);
               throw new VieAppServiceException("获取报表表格数据接口出错", var7);
            }
         }
      }
   }

   public boolean isContainModelCol(List<HashMap<String, String>> talColList) {
      boolean isContainModelCol = false;

      for (HashMap<String, String> talCol : (List<HashMap<String, String>>)talColList) {
         String colFiled = talCol.get(CommonParams.tabColFiled);
         if (CommonParams.modelFiemld.equals(colFiled) || "keyword".endsWith(colFiled)) {
            isContainModelCol = true;
         }
      }

      return isContainModelCol;
   }

   public boolean isContainModelCon(List<HashMap<String, Object>> talColList, List<String> modelListCondition, List<String> modelList) throws VieAppServiceException {
      boolean isContainModelCon = false;

      for (HashMap<String, Object> currentCond : (List<HashMap<String, Object>>)talColList) {
         String type = currentCond.get(CommonParams.conditionType).toString().trim();
         if (DimType.valueOf(type).equals(DimType.offLineTagId)) {
            Object condValue = currentCond.get(CommonParams.conditionValue);

            for (HashMap<String, Object> current : (List<HashMap<String, Object>>)condValue) {
               modelListCondition.add(current.get("key").toString());
            }
         }

         int sizeModelCondition = modelListCondition.size();
         modelListCondition.retainAll(modelList);
         if (sizeModelCondition > 0 && modelListCondition.size() == 0) {
            throw new VieAppServiceException(601, "模型条件均已下线，请重新编辑条件");
         }

         if (modelListCondition.size() > 0) {
            isContainModelCon = true;
         }
      }

      return isContainModelCon;
   }

   public void createTable(
      HashMap<String, Object> tableMap,
      SQLContext sqlContext,
      String tableName,
      String query,
      boolean existingModelCol,
      boolean existingModelCon,
      List<String> modelListCondition
   ) throws VieAppServiceException {
      List<String> excludeTopicModelIdList = (List<String>)(tableMap.get("excludeTopicModelIdList") == null
         ? new LinkedList<>()
         : (List)tableMap.get("excludeTopicModelIdList"));
      List<String> topicModelIdList = (List<String>)(tableMap.get("topicModelIdList") == null ? new LinkedList<>() : (List)tableMap.get("topicModelIdList"));
      String sparkTableName = this.sparkTemptableName;
      Map<String, String> options = new HashMap<>();
      options.put("pushdown", "true");
      if (StringUtils.isNotNullAndEmpry(query)) {
         options.put("es.query", query);
      }

      String ips = "";
      if (!existingModelCon && !existingModelCol) {
         ips = DynamicEsSource.getEsSourceByType(tableName, "mainEsAddress");
         ips = ips.replaceAll(";", ",");
         options.put("es.nodes", ips);
         Dataset<Row> df = sqlContext.read().format("org.elasticsearch.spark.sql").options(options).load(tableName + "_*/" + tableName);
         df.createOrReplaceTempView(sparkTableName);
      } else {
         ips = DynamicEsSource.getEsSourceByType(tableName, "modelEsAddress");
         ips = ips.replaceAll(";", ",");
         options.put("es.nodes", ips);
         String[] modelListConArray = modelListCondition.toArray(new String[modelListCondition.size()]);
         this.logger.info("modelListCondition = {}", modelListCondition);
         this.logger.info("modelListConArray = {}", Arrays.asList(modelListConArray));
         if (modelListConArray.length <= 0) {
            throw new VieAppServiceException("注册临时表出错，模型id列表为空");
         }

         Dataset<Row> df = sqlContext.read()
            .format("org.elasticsearch.spark.sql")
            .options(options)
            .load(tableName + "model_" + modelListConArray[0] + "/" + tableName + "model");
         df.createOrReplaceTempView(sparkTableName + modelListConArray[0]);

         for (int i = 1; i < modelListConArray.length; i++) {
            Dataset<Row> dftemp = sqlContext.read()
               .format("org.elasticsearch.spark.sql")
               .options(options)
               .load(tableName + "model_" + modelListConArray[i] + "/" + tableName + "model");
            dftemp.createOrReplaceTempView(sparkTableName + modelListConArray[i]);
            df = df.union(dftemp);
         }

         df.createOrReplaceTempView(sparkTableName);
         if (CollectionUtils.isEmpty(topicModelIdList) && CollectionUtils.isNotEmpty(excludeTopicModelIdList)) {
            ips = DynamicEsSource.getEsSourceByType(tableName, "mainEsAddress");
            ips = ips.replaceAll(";", ",");
            options.put("es.nodes", ips);
            df = sqlContext.read().format("org.elasticsearch.spark.sql").options(options).load(tableName + "_*/" + tableName);
            df.createOrReplaceTempView(sparkTableName + "main");
         }
      }
   }

   public HashMap<String, HashMap<String, String>> getMeasureMap() {
      HashMap<String, HashMap<String, String>> data = new HashMap<>();

      for (HashMap<String, String> cm : this.commonData.getMeasureList()) {
         data.put(cm.get(CommonParams.measure), cm);
      }

      return data;
   }

   private List<LinkedHashMap<String, Object>> dealTableData(
      Dataset<Row> resultSet,
      LinkedHashMap<String, String> rangeMap,
      List<String> showColOrder,
      HashMap<String, String> modelMapForRdd,
      boolean existingModelCon,
      boolean existingModelCol
   ) {
      List<LinkedHashMap<String, Object>> currentData = new LinkedList<>();
      HashMap<String, HashMap<String, String>> measureMap = new HashMap<>(this.getMeasureMap());

      try {
         for (Row row : resultSet.toJavaRDD().collect()) {
            LinkedHashMap<String, Object> trueData = new LinkedHashMap<>();
            currentData.add(trueData);

            for (String col : showColOrder) {
               if (rangeMap.containsKey(col)) {
                  if (row.getAs(rangeMap.get(col)) == null) {
                     trueData.put(col, "");
                  }

                  trueData.put(col, row.getAs(rangeMap.get(col)));
               } else if ("offLineTagId".equals(col)) {
                  String modelName = "";
                  if (row.getAs(col) != null) {
                     modelName = modelMapForRdd.get(row.getAs(col) + "");
                  }

                  trueData.put(col, modelName);
                  if (modelName == null) {
                     this.logger.warn("model " + row.getAs(col) + " can find name!");
                  }
               } else if ("keyword".equals(col)) {
                  String kwdName = "";
                  trueData.put(col, kwdName);
               } else {
                  Object obj = row.getAs(col);
                  if (measureMap.keySet().contains(col) && obj != null) {
                     obj = Math.round(Double.parseDouble(row.getAs(col).toString()));
                     obj = Long.parseLong(obj.toString());
                  } else if (obj == null) {
                     obj = "";
                  }

                  trueData.put(col, obj);
               }
            }
         }
      } catch (Exception var16) {
         this.logger.error(ExceptionUtil.getStackTrace(var16));
      }

      return currentData;
   }

   private HashMap<String, Object> queryDataListBySpark(HashMap<String, Object> tableMap, ReportTableDataRequest request) throws VieAppServiceException {
      HashMap<String, Object> resultMap = new HashMap<>();

      try {
         DimensionRequest dimensionRequest = new DimensionRequest();
         dimensionRequest.setDataSource(request.getDataSource());
         LinkedHashMap<String, DimensionConfig> dimensionMap = this.dimensionService.getDimensionMap(dimensionRequest);
         AuthorizeInfo authorizeInfo = new AuthorizeInfo();
         authorizeInfo.setUserId(request.getUserId());
         authorizeInfo.setDataSource(request.getDataSource());
         HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
         List<String> modelList = new LinkedList<>(modelMap.keySet());
         String order = tableMap.get(CommonParams.tabOrder) == null ? "" : tableMap.get(CommonParams.tabOrder).toString();
         String orderType = tableMap.get(CommonParams.tabOrderType) == null ? "" : tableMap.get(CommonParams.tabOrderType).toString();
         String sql = "";
         List<String> modelListCondition = new LinkedList<>();
         EsQueryParseReport esQueryParseReport = new EsQueryParseReport(dimensionMap);
         List<HashMap<String, String>> talColList = (List<HashMap<String, String>>)tableMap.get(CommonParams.tabCol);
         List<HashMap<String, Object>> talConList = (List<HashMap<String, Object>>)tableMap.get(CommonParams.condition);
         boolean existingModelCol = this.isContainModelCol(talColList);
         boolean existingModelCon = this.isContainModelCon(talConList, modelListCondition, modelList);
         List<String> excludeTopicModelIdList = (List<String>)(tableMap.get("excludeTopicModelIdList") == null
            ? new LinkedList<>()
            : (List)tableMap.get("excludeTopicModelIdList"));
         List<String> topicModelIdList = (List<String>)(tableMap.get("topicModelIdList") == null ? new LinkedList<>() : (List)tableMap.get("topicModelIdList"));
         String modelConNotIn = "";
         String modelConIn = "";
         if (excludeTopicModelIdList.size() > 0) {
            existingModelCon = true;
            modelListCondition.addAll(excludeTopicModelIdList);
            StringBuffer excludeTopicModelIdSql = new StringBuffer("");

            for (String excludeTopicModelId : excludeTopicModelIdList) {
               excludeTopicModelIdSql.append("(select id from " + this.sparkTemptableName + excludeTopicModelId + ") ");
               excludeTopicModelIdSql.append(" UNION ");
            }

            modelConNotIn = modelConNotIn + excludeTopicModelIdSql.substring(0, excludeTopicModelIdSql.length() - " UNION ".length());
            this.logger.info("export 专题取非 modelConNotIn = {}", modelConNotIn);
         }

         if (topicModelIdList.size() > 0) {
            existingModelCon = true;
            modelListCondition.addAll(topicModelIdList);
            StringBuffer topicModelIdSql = new StringBuffer("");

            for (String topicModelId : topicModelIdList) {
               topicModelIdSql.append("(select id from " + this.sparkTemptableName + topicModelId + ") ");
               topicModelIdSql.append(" INTERSECT ");
            }

            modelConIn = modelConIn + topicModelIdSql.substring(0, topicModelIdSql.length() - " INTERSECT ".length());
            this.logger.info("export 专题取交 modelConIn = {}", modelConIn);
         }

         List<String> queryList = esQueryParseReport.dealReportCondition(talConList);
         String esTimeQuery = EsQueryParse.getTimeRangeQuery(request.getStartTime(), request.getEndTime());
         queryList.add(esTimeQuery);
         String dataSql = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
         EsQueryParseReport.dealBatchQuery(queryList, request.getBatchId(), request.getDataType(), request.getIfLone(), dataSql);
         String query = EsQueryParse.getResultQuery(queryList.toArray(new String[queryList.size()]));
         this.logger.info("export es query : {}", query);
         SQLContext sqlContext = this.sparkUtils.getSqlContext();
         String tableName = request.getDataSource();
         if (!existingModelCon && existingModelCol) {
            modelListCondition = modelList;
         }

         this.createTable(tableMap, sqlContext, tableName, query, existingModelCol, existingModelCon, modelListCondition);
         SparkSqlParse sparkSqlParse = new SparkSqlParse(
            this.commonData.getMeasureList(),
            dimensionMap,
            talConList,
            this.sparkTemptableName,
            request.getStartTime(),
            request.getEndTime(),
            modelList,
            request.getPageNum(),
            request.getPageSize(),
            order,
            orderType,
            esQueryParseReport.rangeMap,
            CommonXmlConfig.getDurationMap()
         );
         sql = sparkSqlParse.dealStatsTable(tableMap, false, modelConIn, modelConNotIn);
         this.logger.info("export spark sql : {}", sql);
         Dataset<Row> resultSet = sqlContext.sql(sql).cache();
         this.logger.info("dealTableData start");
         int pageSize = request.getPageSize();
         int pageNum = request.getPageNum();
         int totalRows = (int)resultSet.count();
         String rownumCondition = "";
         List<LinkedHashMap<String, Object>> currentData = new LinkedList<>();
         int totalPages = totalRows % pageSize == 0 ? totalRows / pageSize : totalRows / pageSize + 1;
         if (totalRows > 0) {
            Dataset<Row> resultSubSet;
            if (pageSize != -1 && pageNum != -1) {
               int rownumStart = pageSize * (pageNum - 1) + 1;
               int rownumEnd = pageNum * pageSize;
               rownumCondition = " rownum >= " + rownumStart + " and  rownum <= " + rownumEnd;
               this.logger.info("export sparkrownum sql : {}", rownumCondition);
               resultSubSet = resultSet.filter(rownumCondition);
            } else {
               resultSubSet = resultSet;
            }

            currentData = this.dealTableData(resultSubSet, sparkSqlParse.rangeShowMap, sparkSqlParse.showColOrder, modelMap, existingModelCon, existingModelCol);
         }

         resultMap.put("totalRows", totalRows);
         resultMap.put("rows", currentData);
         resultMap.put("totalPages", totalPages);
         resultMap.put("pageNum", pageNum);
         resultMap.put("pageSize", pageSize);
         resultMap.put("sortColumn", request.getSortColumn());
         resultMap.put("sortMethod", request.getSortType());
         return resultMap;
      } catch (Exception var39) {
         this.logger.error("调用queryDataListByS..查询接口出错", var39);
         throw new VieAppServiceException(var39);
      }
   }

   private List<LinkedHashMap<String, Object>> dealTableData(
      List<LinkedHashMap<String, Object>> anydrillResultData, LinkedHashMap<String, String> rangeMap, List<String> showColOrder
   ) {
      List<LinkedHashMap<String, Object>> currentData = new LinkedList<>();
      HashMap<String, HashMap<String, String>> measureMap = new HashMap<>(this.getMeasureMap());

      try {
         for (LinkedHashMap<String, Object> current : anydrillResultData) {
            LinkedHashMap<String, Object> trueData = new LinkedHashMap<>();
            currentData.add(trueData);

            for (String col : showColOrder) {
               if (rangeMap.containsKey(col)) {
                  if (current.get(rangeMap.get(col)) == null) {
                     trueData.put(col, "");
                  }

                  trueData.put(col, current.get(rangeMap.get(col)));
               } else {
                  Object obj = current.get(col);
                  if (measureMap.keySet().contains(col) && obj != null) {
                     obj = Math.round(Double.parseDouble(current.get(col).toString()));
                     obj = Long.parseLong(obj.toString());
                  } else if (obj == null) {
                     obj = "";
                  }

                  trueData.put(col, obj);
               }
            }
         }
      } catch (Exception var12) {
         this.logger.error(ExceptionUtil.getStackTrace(var12));
      }

      return currentData;
   }

   private HashMap<String, Object> queryDataListByAnyDrill(HashMap<String, Object> tableMap, ReportTableDataRequest request) throws VieAppServiceException {
      HashMap<String, Object> resultMap = new HashMap<>();

      try {
         AuthorizeInfo authorizeInfo = new AuthorizeInfo();
         authorizeInfo.setDataSource(request.getDataSource());
         authorizeInfo.setUserId(request.getUserId());
         HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
         List<String> modelList = new LinkedList<>(modelMap.keySet());
         DimensionRequest dimensionRequest = new DimensionRequest();
         dimensionRequest.setDataSource(request.getDataSource());
         LinkedHashMap<String, DimensionConfig> dimensionMap = this.dimensionService.getDimensionMap(dimensionRequest);
         List<HashMap<String, String>> measureList = this.commonData.getMeasureList();
         ExcuteContext excuteContext = new ExcuteContext();
         String order = tableMap.get(CommonParams.tabOrder) == null ? "" : tableMap.get(CommonParams.tabOrder).toString();
         String orderType = tableMap.get(CommonParams.tabOrderType) == null ? "" : tableMap.get(CommonParams.tabOrderType).toString();
         String sql = "";
         String urls = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress");
         String tableName = request.getDataSource();
         String dataSql = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
         int pageSize = request.getPageSize();
         int pageNum = request.getPageNum() > 0 ? request.getPageNum() - 1 : request.getPageNum();
         AnyDrillParse anyDrillParse = new AnyDrillParse(
            measureList,
            dimensionMap,
            tableName,
            request.getStartTime(),
            request.getEndTime(),
            modelList,
            pageNum,
            pageSize,
            order,
            orderType,
            dataSql,
            this.logger
         );
         sql = anyDrillParse.dealStatsTable(tableMap, false, request.getDataSource());
         String multipleDataSql = MultipleDataConstant.getMultipleDataSql(request.getBatchId(), request.getIfLone(), request.getDataType());
         sql = sql.replace("where", "where " + multipleDataSql + " and ");
         this.logger.info("export count anydrill sql: {}", sql);
         ResultSet rs = excuteContext.executeSearchQuery(urls, sql);
         int tableSize = rs == null ? 0 : (int)rs.getTotalCount();
         this.logger.info(" export table anydrill count: {}", tableSize);
         if (tableSize > 0 && pageSize != -1 && pageNum != -1) {
            anyDrillParse = new AnyDrillParse(
               this.commonData.getMeasureList(),
               dimensionMap,
               tableName,
               request.getStartTime(),
               request.getEndTime(),
               modelList,
               pageNum,
               pageSize,
               order,
               orderType,
               dataSql,
               this.logger
            );
            sql = anyDrillParse.dealStatsTable(tableMap, false, request.getDataSource());
            sql = sql.replace("where", "where " + multipleDataSql + " and ");
            this.logger.info("export table anydrill sql: {}", sql);
            this.logger.info("export table anydrill urls: {}", urls);
            rs = excuteContext.executeSearchQuery(urls, sql);
         }

         List<LinkedHashMap<String, Object>> currentData = new LinkedList<>();
         if (tableSize > 0) {
            List<LinkedHashMap<String, Object>> anydrillResultData = rs.toList();
            currentData = this.dealTableData(anydrillResultData, anyDrillParse.rangeShowMap, anyDrillParse.showColOrder);
         }

         int totalPages = tableSize % pageSize == 0 ? tableSize / pageSize : tableSize / pageSize + 1;
         resultMap.put("totalRows", tableSize);
         resultMap.put("rows", currentData);
         resultMap.put("totalPages", totalPages);
         resultMap.put("pageNum", pageNum);
         resultMap.put("pageSize", pageSize);
         resultMap.put("sortColumn", request.getSortColumn());
         resultMap.put("sortMethod", request.getSortType());
         return resultMap;
      } catch (Exception var25) {
         this.logger.error("调用queryDataListByAnyDrill查询报表统计数据接口出错", var25);
         throw new VieAppServiceException("调用统计数据接口出错", var25);
      }
   }

   private HashMap<String, Object> queryDataListAnyDrillAndSpark(HashMap<String, Object> tableMap, ReportTableDataRequest request) throws VieAppServiceException, ViePlatformServiceException {
      HashMap<String, Object> resultMap = new HashMap<>();
      List<HashMap<String, String>> talColList = (List<HashMap<String, String>>)tableMap.get(CommonParams.tabCol);
      List<HashMap<String, Object>> talConList = (List<HashMap<String, Object>>)tableMap.get(CommonParams.condition);
      DimensionRequest dimensionRequest = new DimensionRequest();
      dimensionRequest.setDataSource(request.getDataSource());
      LinkedHashMap<String, DimensionConfig> dimensionMap = this.dimensionService.getDimensionMap(dimensionRequest);
      String order = tableMap.get(CommonParams.tabOrder) == null ? "" : tableMap.get(CommonParams.tabOrder).toString();
      int pageNum = request.getPageNum();
      int pageSize = request.getPageSize();
      if (this.isDurationDimsion(order, dimensionMap)) {
         request.setPageNum(-1);
         request.setPageSize(-1);
      }

      AuthorizeInfo authorizeInfo = new AuthorizeInfo();
      authorizeInfo.setDataSource(request.getDataSource());
      authorizeInfo.setUserId(request.getUserId());
      HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
      List<String> modelList = new LinkedList<>(modelMap.keySet());
      boolean existingModelCol = this.isContainModelCol(talColList);
      if (existingModelCol && CollectionUtils.isEmpty(modelList)) {
         return resultMap;
      } else {
         boolean existingModelCon = this.isContainModelCon(talConList, new LinkedList<String>(), modelList);
         this.logger.info("existingModelCol = {}", existingModelCol);
         this.logger.info("existingModelCon = {}", existingModelCon);
         this.logger.info("tableMap = {} ", tableMap);
         long start = 0L;
         long end = 0L;
         start = System.currentTimeMillis();
         List<String> excludeTopicModelIdList = (List<String>)(tableMap.get("excludeTopicModelIdList") == null
            ? new LinkedList<>()
            : (List)tableMap.get("excludeTopicModelIdList"));
         List<String> topicModelIdList = (List<String>)(tableMap.get("topicModelIdList") == null ? new LinkedList<>() : (List)tableMap.get("topicModelIdList"));

         try {
            List<String> durationCols = new LinkedList<>();
            if (!existingModelCol && !existingModelCon && CollectionUtils.isEmpty(topicModelIdList) && CollectionUtils.isEmpty(excludeTopicModelIdList)) {
            }

            resultMap = this.queryDataListBySpark(tableMap, request);
            end = System.currentTimeMillis();
            this.logger.info("调用queryDataListBySpark，时间 = {}", end - start);
            if (this.isDurationDimsion(order, dimensionMap) || durationCols.size() > 1) {
               List<LinkedHashMap<String, Object>> currentData = (List<LinkedHashMap<String, Object>>)resultMap.get("rows");
               if (this.isDurationDimsion(order, dimensionMap)) {
                  currentData = this.orderByDurationDimsion(currentData, order, tableMap.get(CommonParams.tabOrderType).toString());
               }

               if (durationCols.size() > 1) {
                  currentData = this.filterDate(currentData, durationCols);
               }

               int totalRows = currentData.size();
               int totalPages = 0;
               if (pageSize != -1 && pageNum != -1) {
                  totalPages = totalRows % pageSize == 0 ? totalRows / pageSize : totalRows / pageSize + 1;
                  if (currentData != null && totalRows > 0 && pageNum <= totalPages) {
                     int rownumStart = pageSize * (pageNum - 1);
                     int rownumEnd = pageNum * pageSize > totalRows ? totalRows : pageNum * pageSize;
                     currentData = currentData.subList(rownumStart, rownumEnd);
                  } else {
                     currentData = new LinkedList<>();
                  }
               }

               resultMap.put("rows", currentData);
               resultMap.put("totalRows", totalRows);
               resultMap.put("totalPages", totalPages);
               resultMap.put("pageNum", pageNum);
               resultMap.put("pageSize", pageSize);
               resultMap.put("sortColumn", request.getSortColumn());
               resultMap.put("sortMethod", request.getSortType());
            }

            return resultMap;
         } catch (Exception var28) {
            if (var28 instanceof VieAppServiceException) {
               this.logger.error("接口queryDataListAnyDrillAndS..出错", var28);
               throw new VieAppServiceException(((VieAppServiceException)var28).getErrorCode(), ((VieAppServiceException)var28).getMessage());
            } else {
               throw new VieAppServiceException(var28);
            }
         }
      }
   }

   private List<LinkedHashMap<String, Object>> filterDate(List<LinkedHashMap<String, Object>> dataList, List<String> durationCols) {
      List<LinkedHashMap<String, Object>> result = new LinkedList<>(dataList);

      for (LinkedHashMap<String, Object> dataMap : dataList) {
         for (String key : durationCols) {
            if (dataMap.get(key) == null || StringUtils.isNullOrEmpry(dataMap.get(key).toString())) {
               result.remove(dataMap);
               break;
            }
         }
      }

      return result;
   }

   private List<LinkedHashMap<String, Object>> orderByDurationDimsion(List<LinkedHashMap<String, Object>> currentData, String order, String orderType) {
      for (LinkedHashMap<String, Object> dataMap : currentData) {
         if (dataMap.get(order) != null) {
            String[] durationArray = dataMap.get(order).toString().split("~");
            dataMap.put("startDuration", durationArray[0]);
            if (durationArray.length > 1) {
               dataMap.put("endDuration", durationArray[1]);
            }
         }
      }

      Collections.sort(currentData, new Comparator<Map<String, Object>>() {
         public int compare(Map<String, Object> o1, Map<String, Object> o2) {
            if (o1 == null && o2 == null) {
               return 0;
            } else if (o1 == null) {
               return -1;
            } else if (o2 == null) {
               return 1;
            } else if (o1.get("startDuration") == null && o2.get("startDuration") == null) {
               return 0;
            } else if (o1.get("startDuration") == null) {
               return -1;
            } else if (o2.get("startDuration") == null) {
               return 1;
            } else {
               double start1value = Double.parseDouble(String.valueOf(o1.get("startDuration")));
               double start2value = Double.parseDouble(String.valueOf(o2.get("startDuration")));
               if (Double.compare(start1value - start2value, 0.0) != 0) {
                  return (int)(start1value - start2value);
               } else if (o1.get("endDuration") == null && o2.get("endDuration") == null) {
                  return 0;
               } else if (o1.get("endDuration") == null) {
                  return -1;
               } else if (o2.get("endDuration") == null) {
                  return 1;
               } else {
                  double end1value = Double.parseDouble(String.valueOf(o1.get("endDuration")));
                  double end2value = Double.parseDouble(String.valueOf(o2.get("endDuration")));
                  return Double.compare(end1value - end2value, 0.0) != 0 ? (int)(end1value - end2value) : 0;
               }
            }
         }
      });
      if ("desc".equals(orderType)) {
         Collections.reverse(currentData);
      }

      return currentData;
   }

   public List<String> isContainDurationCol(List<HashMap<String, String>> talColList, LinkedHashMap<String, DimensionConfig> dimensionMap) {
      List<String> durationCols = new LinkedList<>();

      for (HashMap<String, String> talCol : (List<HashMap<String, String>>)talColList) {
         String colFiled = talCol.get(CommonParams.tabColFiled);
         if (this.isDurationDimsion(colFiled, dimensionMap)) {
            durationCols.add(colFiled);
         }
      }

      return durationCols;
   }

   private HashMap<String, Object> queryVoiceList(
      String startTime, String endTime, HashMap<String, Object> tableMap, ReportTableDataRequest reportTableDataRequest
   ) throws VieAppServiceException {
      try {
         List<HashMap<String, String>> columnList = (List<HashMap<String, String>>)tableMap.get(CommonParams.tabCol);
         boolean containsKeyword = false;
         List<String> showColumn = new ArrayList<>();

         for (HashMap<String, String> columnMap : (List<HashMap<String, String>>)columnList) {
            if (columnMap.get("filed").equals(CommonParams.keyword)) {
               showColumn.add(CommonParams.modelFieldInfo);
               containsKeyword = true;
            } else {
               showColumn.add(columnMap.get("filed"));
            }
         }

         if (containsKeyword && !showColumn.contains(CommonParams.modelFiemld)) {
            showColumn.add(CommonParams.modelFiemld);
         }

         boolean modelColflag = false;
         if (showColumn.contains(CommonParams.modelFiemld)) {
            modelColflag = true;
         }

         showColumn.add("id");
         AuthorizeInfo authorizeInfo = new AuthorizeInfo();
         authorizeInfo.setDataSource(reportTableDataRequest.getDataSource());
         authorizeInfo.setUserId(reportTableDataRequest.getUserId());
         HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
         List<String> modelList = new LinkedList<>(modelMap.keySet());
         HashMap<String, Object> resultMap = new HashMap<>();
         if (tableMap.get(CommonParams.condition) != null) {
            List<HashMap<String, String>> measureList = this.reportService.getMeasureList();
            DimensionRequest dimensionRequest = new DimensionRequest();
            dimensionRequest.setDataSource(reportTableDataRequest.getDataSource());
            LinkedHashMap<String, DimensionConfig> dimensionMap = this.dimensionService.getDimensionMap(dimensionRequest);
            List<HashMap<String, Object>> conditionList = (List<HashMap<String, Object>>)tableMap.get(CommonParams.condition);
            boolean offLineSize = false;
            LinkedList<String> correctModel = new LinkedList<>();
            offLineSize = this.isContainModelCon(conditionList, correctModel, modelList);
            if ((String.valueOf(tableMap.get(CommonParams.condition)).indexOf("offLineTagId") == -1 || !offLineSize)
               && !"2".equals(LoadCommonConfig.get("es.query.mode"))) {
               String tableName = reportTableDataRequest.getDataSource();
               AnyDrillParse anyDrillParse = new AnyDrillParse(
                  measureList,
                  dimensionMap,
                  tableName,
                  startTime,
                  endTime,
                  modelList,
                  reportTableDataRequest.getPageNum() - 1,
                  reportTableDataRequest.getPageSize(),
                  reportTableDataRequest.getSortColumn(),
                  reportTableDataRequest.getSortType(),
                  this.commonData.getDataSql(reportTableDataRequest.getUserId(), reportTableDataRequest.getSystemId(), reportTableDataRequest.getDataSource()),
                  this.logger
               );
               String sql = anyDrillParse.dealStatsTable(tableMap, true, reportTableDataRequest.getDataSource());
               if (StringUtils.isNullOrEmpry(anyDrillParse.erroeMsg)) {
                  String multipleDataSql = MultipleDataConstant.getMultipleDataSql(
                     reportTableDataRequest.getBatchId(), reportTableDataRequest.getIfLone(), reportTableDataRequest.getDataType()
                  );
                  sql = sql.replace("where", "where " + multipleDataSql + " and ");
                  this.logger.info("详细表格查询anyDrill,sql:" + sql);
                  String anydrillAddressMain = DynamicEsSource.getEsSourceByType(reportTableDataRequest.getDataSource(), "mainAnydrillAddress");
                  ResultSet rs = new ExcuteContext().executeSearchQuery(anydrillAddressMain, sql);
                  List<LinkedHashMap<String, Object>> rsList = (List<LinkedHashMap<String, Object>>)(rs == null ? new LinkedList<>() : rs.toList());
                  if (showColumn.contains("voiceId") && modelColflag && IndexConstants.getInsightType(reportTableDataRequest.getDataSource()) == 1) {
                     String modelInfo = IndexConstants.getModelInfor(showColumn);
                     String anydrillAddressModel = DynamicEsSource.getEsSourceByType(reportTableDataRequest.getDataSource(), "modelAnydrillAddress");
                     List<LinkedHashMap<String, Object>> list = null;
                     list = new SparkQuery().dealHitVoiceValue(anydrillAddressModel.split(";"), rsList, tableName, modelInfo, modelMap, 0, showColumn);
                     List<LinkedHashMap<String, Object>> voiceList = null;
                     if (showColumn.contains("voiceId")) {
                        voiceList = this.dealVoiceDurationResult(list, showColumn, anyDrillParse);
                     } else {
                        voiceList = list;
                     }

                     this.dealDurationResult(voiceList, showColumn, anyDrillParse);
                     resultMap.put("rows", voiceList);
                  } else if ((modelColflag || containsKeyword) && modelMap != null && modelMap.size() > 0) {
                     String modelInfo = IndexConstants.getModelInfor(showColumn);
                     String anydrillAddressModel = DynamicEsSource.getEsSourceByType(reportTableDataRequest.getDataSource(), "modelAnydrillAddress");
                     List<LinkedHashMap<String, Object>> list = null;
                     list = new SparkQuery().dealModelReturenValue(anydrillAddressModel.split(";"), rsList, tableName, modelInfo, modelMap, 0, showColumn);
                     List<LinkedHashMap<String, Object>> voiceList = null;
                     if (showColumn.contains("voiceId") && IndexConstants.getInsightType(reportTableDataRequest.getDataSource()) == 1) {
                        voiceList = this.dealVoiceDurationResult(list, showColumn, anyDrillParse);
                     } else {
                        voiceList = list;
                     }

                     this.dealDurationResult(voiceList, showColumn, anyDrillParse);
                     resultMap.put("rows", voiceList);
                  } else {
                     List<LinkedHashMap<String, Object>> voiceList = null;
                     if (showColumn.contains("voiceId") && IndexConstants.getInsightType(reportTableDataRequest.getDataSource()) == 1) {
                        voiceList = this.dealVoiceDurationResult(rsList, showColumn, anyDrillParse);
                     } else {
                        voiceList = rsList;
                     }

                     this.dealDurationResult(voiceList, showColumn, anyDrillParse);
                     resultMap.put("rows", voiceList);
                  }

                  long totalCount = rs == null ? 0L : rs.getTotalCount();
                  resultMap.put("totalRows", totalCount);
               }
            } else {
               SearchQuery searchQuery = new SparkQuery(this.sparkUtils, this.commonData);
               List<ColumnMap> showCols = this.dealSelColumns(showColumn, dimensionMap, reportTableDataRequest.getDataSource());
               PagerResponse<LinkedHashMap<String, Object>> response = searchQuery.queryVoiceList(
                  reportTableDataRequest,
                  dimensionMap,
                  conditionList,
                  reportTableDataRequest.getDataSource(),
                  null,
                  showCols,
                  modelMap,
                  correctModel,
                  modelColflag
               );
               List<LinkedHashMap<String, Object>> list = (List<LinkedHashMap<String, Object>>)(response.getRows() == null
                  ? new ArrayList<>()
                  : response.getRows());
               List<LinkedHashMap<String, Object>> voiceList = null;
               if (showColumn.contains("voiceId") && IndexConstants.getInsightType(reportTableDataRequest.getDataSource()) == 1) {
                  voiceList = this.dealVoiceDurationResult(list, showColumn, dimensionMap);
               } else {
                  voiceList = list;
               }

               resultMap.put("totalRows", response.getTotalRows());
               resultMap.put("rows", voiceList);
            }

            resultMap.put("pageNum", reportTableDataRequest.getPageNum());
            resultMap.put("pageSize", reportTableDataRequest.getPageSize());
            resultMap.put("sortColumn", reportTableDataRequest.getSortColumn());
            resultMap.put("sortMethod", reportTableDataRequest.getSortType());
         }

         return resultMap;
      } catch (Exception var30) {
         if (var30 instanceof VieAppServiceException) {
            this.logger.error("接口queryDataListAnyDrillAndS..出错", var30);
            throw new VieAppServiceException(((VieAppServiceException)var30).getErrorCode(), ((VieAppServiceException)var30).getMessage());
         } else {
            this.logger.error("调用数据查询接口出错", var30);
            throw new VieAppServiceException(var30);
         }
      }
   }

   public List<LinkedHashMap<String, Object>> dealVoiceDurationResult(
      List<LinkedHashMap<String, Object>> list, List<String> showColumns, AnyDrillParse anyDrillParse
   ) {
      List<LinkedHashMap<String, Object>> voiceList = new ArrayList<>();
      if (list == null) {
         return voiceList;
      } else {
         for (LinkedHashMap<String, Object> currentMap : list) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();

            for (String showColumn : showColumns) {
               if (anyDrillParse.isDurationDimsion(showColumn)) {
                  String voiceColumn = DimensionMappingConfig.getMappingName(showColumn) == null
                     ? showColumn
                     : DimensionMappingConfig.getMappingName(showColumn);
                  map.put(showColumn, currentMap.get(voiceColumn) == null ? 0.0 : Double.parseDouble(String.valueOf(currentMap.get(voiceColumn))) / 1000.0);
               } else if (showColumn.equals(CommonParams.modelFieldInfo)) {
                  map.put(CommonParams.keyword, currentMap.get(CommonParams.keyword));
               } else {
                  String voiceColumn = DimensionMappingConfig.getMappingName(showColumn) == null
                     ? showColumn
                     : DimensionMappingConfig.getMappingName(showColumn);
                  map.put(showColumn, currentMap.get(voiceColumn));
               }
            }

            voiceList.add(map);
         }

         return voiceList;
      }
   }

   public List<LinkedHashMap<String, Object>> dealVoiceDurationResult(
      List<LinkedHashMap<String, Object>> list, List<String> showColumns, LinkedHashMap<String, DimensionConfig> dimensionMap
   ) {
      List<LinkedHashMap<String, Object>> voiceList = new ArrayList<>();
      if (list == null) {
         return voiceList;
      } else {
         for (LinkedHashMap<String, Object> currentMap : list) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();

            for (String showColumn : showColumns) {
               DimensionConfig dimension = dimensionMap.get(showColumn);
               if (dimension != null && dimension.getFlag() == 1) {
                  String voiceColumn = DimensionMappingConfig.getMappingName(showColumn);
                  if (voiceColumn != null) {
                     map.put(showColumn, currentMap.get(voiceColumn) == null ? 0.0 : Double.parseDouble(String.valueOf(currentMap.get(voiceColumn))));
                  } else {
                     map.put(showColumn, currentMap.get(showColumn));
                  }
               } else if (showColumn.equals(CommonParams.modelFieldInfo)) {
                  map.put(CommonParams.keyword, currentMap.get(CommonParams.keyword));
               } else {
                  String voiceColumn = DimensionMappingConfig.getMappingName(showColumn) == null
                     ? showColumn
                     : DimensionMappingConfig.getMappingName(showColumn);
                  map.put(showColumn, currentMap.get(voiceColumn));
               }
            }

            voiceList.add(map);
         }

         return voiceList;
      }
   }

   private void dealDurationResult(List<LinkedHashMap<String, Object>> list, List<String> showColumns, AnyDrillParse anyDrillParse) {
      if (list != null) {
         for (LinkedHashMap<String, Object> currentMap : list) {
            for (String showColumn : showColumns) {
               if (anyDrillParse.isDurationDimsion(showColumn)) {
                  Object value = null;
                  if (!showColumn.equals("n0avgSpeed") && !showColumn.equals("n1avgSpeed")) {
                     value = Math.round(Double.parseDouble(String.valueOf(currentMap.get(showColumn))));
                  } else if (currentMap.get(showColumn) != null) {
                     value = this.doubleFormat.format(Double.parseDouble(String.valueOf(currentMap.get(showColumn))));
                  }

                  currentMap.put(showColumn, currentMap.get(showColumn) == null ? "" : value);
               }
            }
         }
      }
   }

   private List<ColumnMap> dealSelColumns(List<String> showCols, LinkedHashMap<String, DimensionConfig> dimensionMap, String dataSource) {
      List<ColumnMap> selColums = new ArrayList<>();
      DimensionConfig dc = null;

      for (String sel : showCols) {
         dc = dimensionMap.get(sel);
         if (dc != null) {
            selColums.add(new ColumnMap(dc.getShowName(), sel));
         } else if ("id".equals(sel)) {
            if (IndexConstants.getInsightType(dataSource) == 1) {
               selColums.add(new ColumnMap("taskId", sel));
            } else {
               selColums.add(new ColumnMap("voiceId", sel));
            }
         } else if ("offLineTagId".equals(sel)) {
            selColums.add(new ColumnMap("模型", sel));
         } else if ("offLineTagInfo".equals(sel)) {
            selColums.add(new ColumnMap("关键字", sel));
         } else {
            selColums.add(new ColumnMap(sel, sel));
         }
      }

      return selColums;
   }

   public List<Map<String, Object>> getLineColumData(ReportTableDataRequest reportTableDataRequest) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(reportTableDataRequest.getUserId())) {
         this.logger.error("用户id不能为空");
         throw new VieAppServiceException("用户id不能为空");
      } else if (StringUtils.isNullOrEmpry(reportTableDataRequest.getSystemId())) {
         this.logger.error("systemId不能为空");
         throw new VieAppServiceException("systemId不能为空");
      } else if (StringUtils.isNullOrEmpry(reportTableDataRequest.getDataSource())) {
         this.logger.error("数据源不能为空");
         throw new VieAppServiceException("数据源不能为空");
      } else if (StringUtils.isNullOrEmpry(reportTableDataRequest.getChartParams())) {
         this.logger.error("参数异常");
         throw new VieAppServiceException("参数异常");
      } else {
         List<Map<String, Object>> result = new LinkedList<>();

         try {
            HashMap<String, Object> chartMap = (HashMap<String, Object>)this.mapper
               .readValue(reportTableDataRequest.getChartParams(), new TypeReference<HashMap<String, Object>>() {});
            HashMap<String, HashMap<String, String>> allAnalyseFields = new HashMap<>();
            List<HashMap<String, String>> talColList = new LinkedList<>();
            if (chartMap != null) {
               String xAxis = CommonFun.getParamsValue(chartMap, CommonParams.lineColXAxis);
               String order = chartMap.get(CommonParams.lineColOrder) == null ? xAxis : chartMap.get(CommonParams.lineColOrder).toString();
               String orderType = chartMap.get(CommonParams.lineColOrderType) == null ? "asc" : chartMap.get(CommonParams.lineColOrderType).toString();
               AuthorizeInfo authorizeInfo = new AuthorizeInfo();
               authorizeInfo.setUserId(reportTableDataRequest.getUserId());
               authorizeInfo.setDataSource(reportTableDataRequest.getDataSource());
               HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
               List<String> modelList = new ArrayList<>();
               modelList.addAll(modelMap.keySet());
               DimensionRequest request = new DimensionRequest();
               request.setDataSource(reportTableDataRequest.getDataSource());
               LinkedHashMap<String, DimensionConfig> dimensionMap = this.dimensionService.getDimensionMap(request);
               Object pathId = chartMap.get(CommonParams.pathID);
               List<HashMap<String, Object>> pathMap = new ArrayList<>();
               List<String> excludeTopicModelIdList = new LinkedList<>();
               List<String> topicModelIdList = new LinkedList<>();
               if (pathId != null) {
                  TopicPath topicPath = this.topicDB.fetchTopicPathById(Integer.parseInt(String.valueOf(pathId)));
                  if (topicPath != null && !StringUtils.isNullOrEmpry(topicPath.getPathCondition())) {
                     pathMap = (List<HashMap<String, Object>>)(List<?>)this.mapper
                        .readValue(topicPath.getPathCondition(), new TypeReference<List<HashMap<String, Object>>>() {});
                     if (pathMap != null && pathMap.size() > 0) {
                        for (HashMap<String, Object> singleFilterMap : pathMap) {
                           String field = String.valueOf(singleFilterMap.get("field"));
                           String type = String.valueOf(singleFilterMap.get("type"));
                           String exclude = String.valueOf(singleFilterMap.get("isNegate"));
                           String value = String.valueOf(singleFilterMap.get("value"));
                           if ("model".equals(type)) {
                              type = "topicModel";
                              if (!modelList.contains(value)) {
                                 throw new VieAppServiceException(601, "存在已下线或已删除的模型，请重新编辑条件");
                              }

                              if (exclude.equals("0")) {
                                 topicModelIdList.add(value);
                              } else if (exclude.equals("1")) {
                                 excludeTopicModelIdList.add(value);
                              }
                           } else {
                              DimensionConfig dimensionConfig = dimensionMap.get(field);
                              if (dimensionConfig == null) {
                                 this.logger.error("{}维度不存在!", field);
                                 throw new VieAppServiceException(field + "维度不存在!");
                              }

                              type = dimensionConfig.getShowType();
                              if (this.isDurationDimsion(field, dimensionMap)) {
                                 value = value.replaceAll("~", "|");
                              } else if (this.isRangeDimsion(field, dimensionMap)) {
                                 value = value.replaceAll("~", "|");
                              }

                              List<String> valueList = Arrays.asList(value.split(","));
                              singleFilterMap.put("value", valueList);
                           }

                           singleFilterMap.put("type", type);
                           singleFilterMap.put(CommonParams.conditionFiled, field);
                        }
                     }
                  }

                  chartMap.put(CommonParams.condition, pathMap);
                  chartMap.put("topicModelIdList", topicModelIdList);
                  chartMap.put("excludeTopicModelIdList", excludeTopicModelIdList);
                  if ("hour".equals(xAxis)) {
                     xAxis = "dimHour";
                     order = "dimHour";
                  } else if ("day".equals(xAxis)) {
                     xAxis = "dimDay";
                     order = "dimDay";
                  } else if ("weekOfYear".equals(xAxis)) {
                     order = "year," + order;
                     HashMap<String, String> yearMap = new HashMap<>();
                     yearMap.put("type", "dimension");
                     yearMap.put("filed", "year");
                     talColList.add(yearMap);
                  }

                  chartMap.put(CommonParams.tabOrder, order);
               }

               reportTableDataRequest.setSortColumn(String.valueOf(order));
               reportTableDataRequest.setSortType(String.valueOf(orderType));
               String xAxisText = chartMap.get("xAxisText") == null ? "" : chartMap.get("xAxisText").toString();
               String xAxisType = "dimension";
               HashMap<String, String> xAxisMap = new HashMap<>();
               xAxisMap.put("name", xAxisText);
               xAxisMap.put("text", xAxisText);
               xAxisMap.put("type", xAxisType);
               xAxisMap.put("filed", xAxis);
               talColList.add(xAxisMap);
               allAnalyseFields.put(xAxis, xAxisMap);
               List<HashMap<String, String>> mainAxisMapList = (List<HashMap<String, String>>)chartMap.get("mainAxis");

               for (HashMap<String, String> map : (List<HashMap<String, String>>)mainAxisMapList) {
                  map.put(CommonParams.tabColFiled, map.get(CommonParams.lineColMeasure));
                  allAnalyseFields.put(map.get(CommonParams.lineColMeasure), map);
               }

               talColList.addAll(mainAxisMapList);
               List<HashMap<String, String>> secondaryAxisMapList = (List<HashMap<String, String>>)chartMap.get("secondaryAxis");

               for (HashMap<String, String> map : (List<HashMap<String, String>>)secondaryAxisMapList) {
                  map.put(CommonParams.tabColFiled, map.get(CommonParams.lineColMeasure));
                  allAnalyseFields.put(map.get(CommonParams.lineColMeasure), map);
               }

               talColList.addAll(secondaryAxisMapList);
               chartMap.put("column", talColList);
               reportTableDataRequest.setPageNum(-1);
               reportTableDataRequest.setPageSize(-1);
               HashMap<String, Object> resultMap = this.queryDataListAnyDrillAndSpark(chartMap, reportTableDataRequest);
               List<LinkedHashMap<String, Object>> currentData = (List<LinkedHashMap<String, Object>>)resultMap.get("rows");
               currentData = this.dealLineColumDataForLinkhash(currentData);
               Set<String> columList = allAnalyseFields.keySet();
               columList.remove(xAxis);

               for (LinkedHashMap<String, Object> dataMap : currentData) {
                  for (String key : columList) {
                     HashMap<String, Object> hashMap = new HashMap<>();
                     HashMap<String, String> analyseFieldMap = allAnalyseFields.get(key);
                     hashMap.put(CommonParams.lineColXAxis, dataMap.get(xAxis));
                     hashMap.put("text", analyseFieldMap.get("text"));
                     hashMap.put("value", dataMap.get(key));
                     hashMap.put(analyseFieldMap.get(CommonParams.tabColFiled), dataMap.get(key));
                     hashMap.put(CommonParams.tabColFiled, analyseFieldMap.get(CommonParams.tabColFiled));
                     result.add(hashMap);
                  }
               }

               return result;
            } else {
               this.logger.error("柱折图x轴不能为空！");
               throw new VieAppServiceException("柱折图x轴不能为空！");
            }
         } catch (Exception var32) {
            if (var32 instanceof VieAppServiceException) {
               if (((VieAppServiceException)var32).getErrorCode() == 601) {
                  return new LinkedList<>();
               } else {
                  this.logger.error("获取柱折图数据接口getLineColumData出错", var32);
                  throw new VieAppServiceException(((VieAppServiceException)var32).getErrorCode(), ((VieAppServiceException)var32).getMessage());
               }
            } else {
               this.logger.error("获取柱折图数据接口getLineColumData出错", var32);
               throw new VieAppServiceException("获取报表柱折图数据接口出错", var32);
            }
         }
      }
   }

   public List<Map<String, Object>> dealLineColumData(List<Map<String, Object>> resultList) {
      List<Map<String, Object>> trueData = new ArrayList<>();

      try {
         int dataSize = resultList.size();
         int index = dataSize / this.chartXMaxCount;
         index = dataSize % this.chartXMaxCount == 0 ? index : index + 1;

         for (int i = 0; i < resultList.size(); i++) {
            if (i % index == 0) {
               Map<String, Object> current = resultList.get(i);
               trueData.add(current);
            }
         }

         return trueData;
      } catch (Exception var7) {
         this.logger.error("处理柱折图返回值，添加跨步处理出错", var7);
         return trueData;
      }
   }

   private List<LinkedHashMap<String, Object>> dealLineColumDataForLinkhash(List<LinkedHashMap<String, Object>> resultList) {
      List<LinkedHashMap<String, Object>> trueData = new ArrayList<>();

      try {
         int dataSize = resultList.size();
         int index = dataSize / this.chartXMaxCount;
         index = dataSize % this.chartXMaxCount == 0 ? index : index + 1;

         for (int i = 0; i < resultList.size(); i++) {
            if (i % index == 0) {
               LinkedHashMap<String, Object> current = resultList.get(i);
               trueData.add(current);
            }
         }

         return trueData;
      } catch (Exception var7) {
         this.logger.error("处理柱折图返回值，添加跨步处理出错", var7);
         return trueData;
      }
   }

   public boolean isDurationDimsion(String key, LinkedHashMap<String, DimensionConfig> dimensionMap) {
      boolean result = false;

      try {
         if (dimensionMap == null || dimensionMap.size() == 0) {
            return result;
         }

         DimensionConfig dimensionConfig = dimensionMap.get(key);
         if (dimensionConfig != null) {
            int flag = dimensionConfig.getFlag();
            if (flag == 1 || flag == 2) {
               result = true;
            }
         }
      } catch (Exception var6) {
         this.logger.error("判读维度是否为时长维度出错，维度：{}", key);
      }

      return result;
   }

   public boolean isRangeDimsion(String key, LinkedHashMap<String, DimensionConfig> dimensionMap) {
      boolean result = false;

      try {
         if (dimensionMap == null || dimensionMap.size() == 0) {
            return result;
         }

         DimensionConfig dimensionConfig = dimensionMap.get(key);
         if (dimensionConfig != null && dimensionConfig.getShowType().equals("range")) {
            result = true;
         }
      } catch (Exception var5) {
         this.logger.error("判读维度判断维度是否是区间isRangeDimsion，维度：{}", key);
      }

      return result;
   }

   public Map<String, Object> getPieData(ReportTableDataRequest reportTableDataRequest) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(reportTableDataRequest.getUserId())) {
         this.logger.error("用户id不能为空");
         throw new VieAppServiceException("用户id不能为空");
      } else if (StringUtils.isNullOrEmpry(reportTableDataRequest.getSystemId())) {
         this.logger.error("systemId不能为空");
         throw new VieAppServiceException("systemId不能为空");
      } else if (StringUtils.isNullOrEmpry(reportTableDataRequest.getDataSource())) {
         this.logger.error("数据源不能为空");
         throw new VieAppServiceException("数据源不能为空");
      } else if (StringUtils.isNullOrEmpry(reportTableDataRequest.getChartParams())) {
         this.logger.error("参数异常");
         throw new VieAppServiceException("参数异常");
      } else {
         Map<String, Object> result = new HashMap<>();

         try {
            HashMap<String, Object> chartMap = (HashMap<String, Object>)this.mapper
               .readValue(reportTableDataRequest.getChartParams(), new TypeReference<HashMap<String, Object>>() {});
            HashMap<String, HashMap<String, String>> allAnalyseFields = new HashMap<>();
            if (chartMap == null) {
               this.logger.error("饼图chartParams不能为空！");
               throw new VieAppServiceException("饼图chartParams不能为空！");
            } else {
               String measure = chartMap.get(CommonParams.pieMeasure) == null ? "" : chartMap.get(CommonParams.pieMeasure).toString();
               String pieMText = chartMap.get(CommonParams.pieMText) == null ? "" : chartMap.get(CommonParams.pieMText).toString();
               String pieLegend = chartMap.get(CommonParams.pieLegend) == null ? "" : chartMap.get(CommonParams.pieLegend).toString();
               List<HashMap<String, String>> talColList = new LinkedList<>();
               HashMap<String, String> legendMap = new HashMap<>();
               legendMap.put("name", "");
               legendMap.put("text", "");
               legendMap.put("type", "dimension");
               legendMap.put("filed", pieLegend);
               talColList.add(legendMap);
               allAnalyseFields.put(pieLegend, legendMap);
               HashMap<String, String> measureMap = new HashMap<>();
               measureMap.put("name", pieMText);
               measureMap.put("text", pieMText);
               measureMap.put("type", chartMap.get(CommonParams.pieMType).toString());
               measureMap.put("filed", measure);
               measureMap.put("expression", chartMap.get(CommonParams.pieMExp).toString());
               measureMap.put("showType", "");
               talColList.add(measureMap);
               allAnalyseFields.put(measure, measureMap);
               chartMap.put("column", talColList);
               String orderType = "desc";
               reportTableDataRequest.setSortColumn(String.valueOf(measure));
               reportTableDataRequest.setSortType(orderType);
               chartMap.put("order", measure);
               chartMap.put("orderType", orderType);
               reportTableDataRequest.setPageNum(-1);
               reportTableDataRequest.setPageSize(-1);
               HashMap<String, Object> resultMap = this.queryDataListAnyDrillAndSpark(chartMap, reportTableDataRequest);
               List<LinkedHashMap<String, Object>> currentData = (List<LinkedHashMap<String, Object>>)resultMap.get("rows");
               Set<String> columList = allAnalyseFields.keySet();
               columList.remove(pieLegend);
               List<Map<String, Object>> resultList = new ArrayList<>();

               for (LinkedHashMap<String, Object> dataMap : currentData) {
                  for (String key : columList) {
                     HashMap<String, Object> hashMap = new HashMap<>();
                     HashMap<String, String> analyseFieldMap = allAnalyseFields.get(key);
                     hashMap.put(CommonParams.pieLegend, dataMap.get(pieLegend));
                     hashMap.put("text", analyseFieldMap.get("text"));
                     long value = dataMap.get(key) != null && !StringUtils.isNullOrEmpry(dataMap.get(key).toString())
                        ? Math.round(Double.valueOf(dataMap.get(key).toString()))
                        : 0L;
                     hashMap.put("value", value);
                     resultList.add(hashMap);
                  }
               }

               int maxSize = this.pieMaxCount;
               if (resultList.size() > maxSize) {
                  long otherSum = 0L;

                  for (int i = maxSize; i < resultList.size(); i++) {
                     long tempValue = resultList.get(i).get("value") != null && !StringUtils.isNullOrEmpry(resultList.get(i).get("value").toString())
                        ? Math.round(Double.valueOf(resultList.get(i).get("value").toString()))
                        : 0L;
                     otherSum += tempValue;
                  }

                  Map<String, Object> otherMap = new HashMap<>();
                  otherMap.put("value", otherSum);
                  otherMap.put(CommonParams.pieLegend, "其他");
                  otherMap.put("text", pieMText);
                  resultList = resultList.subList(0, maxSize);
                  resultList.add(otherMap);
               }

               result.put("data", resultList);
               return result;
            }
         } catch (Exception var25) {
            if (var25 instanceof VieAppServiceException) {
               if (((VieAppServiceException)var25).getErrorCode() == 601) {
                  return new HashMap<>();
               } else {
                  this.logger.error(var25.getMessage());
                  throw new VieAppServiceException(((VieAppServiceException)var25).getErrorCode(), ((VieAppServiceException)var25).getMessage());
               }
            } else {
               this.logger.error("获取饼图数据接口getPieDataBySpark出错", var25);
               throw new VieAppServiceException("获取报表饼图数据接口出错", var25);
            }
         }
      }
   }

   public long toSecond(long millSecond) {
      return millSecond == 0L ? 0L : Math.round((float)millSecond / 1000.0F);
   }

   public double toSecondFloat(double millSecond) {
      return Double.compare(millSecond, 0.0) == 0 ? 0.0 : millSecond / 1000.0;
   }

   public ObjectMapper getMapper() {
      return this.mapper;
   }

   public void setMapper(ObjectMapper mapper) {
      this.mapper = mapper;
   }

   public ModelApplyService getModelApplyService() {
      return this.modelApplyService;
   }

   public void setModelApplyService(ModelApplyService modelApplyService) {
      this.modelApplyService = modelApplyService;
   }

   public DataDrillService getDataDrillService() {
      return this.dataDrillService;
   }

   public void setDataDrillService(DataDrillService dataDrillService) {
      this.dataDrillService = dataDrillService;
   }

   public DimensionService getDimensionService() {
      return this.dimensionService;
   }

   public void setDimensionService(DimensionService dimensionService) {
      this.dimensionService = dimensionService;
   }

   public TopicDB getTopicDB() {
      return this.topicDB;
   }

   public void setTopicDB(TopicDB topicDB) {
      this.topicDB = topicDB;
   }

   public ReportService getReportService() {
      return this.reportService;
   }

   public void setReportService(ReportService reportService) {
      this.reportService = reportService;
   }

   public int getPieMaxCount() {
      return this.pieMaxCount;
   }

   public void setPieMaxCount(int pieMaxCount) {
      this.pieMaxCount = pieMaxCount;
   }

   public int getChartXMaxCount() {
      return this.chartXMaxCount;
   }

   public void setChartXMaxCount(int chartXMaxCount) {
      this.chartXMaxCount = chartXMaxCount;
   }

   public Map<String, String> getDurationMap() {
      return this.durationMap;
   }

   public void setDurationMap(Map<String, String> durationMap) {
      this.durationMap = durationMap;
   }

   public SparkUtils getSparkUtils() {
      return this.sparkUtils;
   }

   public void setSparkUtils(SparkUtils sparkUtils) {
      this.sparkUtils = sparkUtils;
   }

   public CommonData getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonData commonData) {
      this.commonData = commonData;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }
}
