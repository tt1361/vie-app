package com.iflytek.vie.app.provider.impl.topic;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.datadrill.DataDrillService;
import com.iflytek.vie.app.api.model.ModelApplyService;
import com.iflytek.vie.app.api.topic.FunnelService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.common.PagerResponse;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.topic.FunnelCallRequest;
import com.iflytek.vie.app.pojo.topic.FunnelCallResponse;
import com.iflytek.vie.app.pojo.topic.FunnelChartResponse;
import com.iflytek.vie.app.pojo.topic.FunnelTableResponse;
import com.iflytek.vie.app.pojo.topic.TopicInfo;
import com.iflytek.vie.app.pojo.topic.TopicPath;
import com.iflytek.vie.app.provider.common.ColumnInfo;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.common.SqlHelper;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.TopicDB;
import com.iflytek.vie.app.provider.database.TopicMarkDB;
import com.iflytek.vie.app.provider.database.TopicPathDB;
import com.iflytek.vie.app.provider.pojo.ModelInfoDto;
import com.iflytek.vie.app.provider.utils.BuildTopicFilterByAnydri;
import com.iflytek.vie.app.provider.utils.BuildTopicFilterBySpark;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.pojo.RequestData;
import com.iflytek.vie.pojo.ResponseData;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.SparkUtils;
import com.iflytek.vie.utils.StringUtils;
import com.iflytek.vie.utils.VieCommonException;
import java.io.IOException;
import java.sql.Clob;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunnelServiceImpl implements FunnelService {
   private final Logger logger = LoggerFactory.getLogger(FunnelServiceImpl.class);
   private TopicPathDB topicPathDB;
   private TopicDB topicDB;
   private ModelDB modelDB;
   private TopicMarkDB topicMarkDB;
   private DataDrillService dataDrillService;
   protected Log log;
   protected ObjectMapper mapper = new ObjectMapper();
   public DecimalFormat df = new DecimalFormat("0");
   public DecimalFormat df3 = new DecimalFormat("0.00");
   public int searchType;
   private CommonData commonData;
   private SparkUtils sparkUtils;
   private ModelApplyService modelApplyService;

   public String getTotalRate(FunnelCallRequest funnelCallRequest) throws VieAppServiceException {
      String totalRate = null;
      if (funnelCallRequest != null) {
         Integer dataType = funnelCallRequest.getDataType();
         String batchId = funnelCallRequest.getBatchId();
         int ifLone = funnelCallRequest.getIfLone();
         String dataSource = funnelCallRequest.getDataSource();
         String systemId = funnelCallRequest.getSystemId();
         String userId = funnelCallRequest.getUserId();
         List<String> keyList = new ArrayList<>();
         keyList.add("userId");
         keyList.add("systemId");
         keyList.add("pathId");
         keyList.add("topicId");
         keyList.add("dataSource");
         String checkResult = this.checkRequestParamNull(funnelCallRequest, keyList);
         if (checkResult != null) {
            this.logger.error("参数({})为空", checkResult);
            throw new VieAppServiceException("参数(" + checkResult + ")为空");
         } else {
            try {
               TopicPath topicPath = this.topicPathDB.getPathById(funnelCallRequest.getPathId());
               if (topicPath == null) {
                  this.logger.error("获取路径条件为空");
                  throw new VieAppServiceException("获取路径条件为空");
               } else {
                  TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(funnelCallRequest.getTopicId()));
                  if (topicInfo == null) {
                     throw new VieAppServiceException("该专题已被删除");
                  } else {
                     String topicCondition = topicInfo.getTopicCondition();
                     if (topicCondition != null && !"".equals(topicCondition)) {
                        HashMap<String, Set<String>> dataAuth = this.commonData.getDataMapAuth(userId, systemId, dataSource);
                        String dataSql = this.commonData.getDataSql(userId, systemId, dataSource);
                        String topicConDSql = BuildTopicFilterByAnydri.groupTopicCondByAnyDri(topicCondition);
                        String sql = "";
                        if (StringUtils.isNullOrEmpry(dataSql)) {
                           sql = "select sum(idCounter) as result from " + dataSource + " where " + topicConDSql + " and processed=0 and ";
                        } else {
                           sql = "select sum(idCounter) as result from " + dataSource + " where " + topicConDSql + " and " + dataSql + " and processed=0 and ";
                        }

                        String multiDatasql = MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType);
                        sql = sql + multiDatasql;
                        String pathCondition = this.topicPathDB.getPathCondition(funnelCallRequest.getPathId());
                        if (topicPath.getApplicationId() != 1 && !StringUtils.isNullOrEmpry(pathCondition)) {
                           List<HashMap<String, String>> pathCond = (List<HashMap<String, String>>)this.mapper
                              .readValue(pathCondition, new TypeReference<List<HashMap<String, String>>>() {});
                           LinkedHashMap<String, DimensionConfig> dimenMap = this.commonData.getDimensionMap(funnelCallRequest.getDataSource());
                           List<HashMap<String, String>> first = new ArrayList<>();
                           first.add(pathCond.get(0));
                           int modelTypeCount = 0;
                           List<String> modelIds = new ArrayList<>();

                           for (HashMap<String, String> currentPath : (List<HashMap<String, String>>)pathCond) {
                              if (currentPath.containsValue("model")) {
                                 modelIds.add(String.valueOf(currentPath.get("value")));
                                 modelTypeCount++;
                              }
                           }

                           List<Object[]> resultList = null;
                           if (modelIds != null && modelIds.size() > 0) {
                              resultList = this.modelDB.querySqlModelByID(modelIds);
                           }

                           List<ModelInfoDto> modelInfoDtos = new ArrayList<>();
                           if (resultList != null && resultList.size() != 0) {
                              for (Object[] modelInfoDto : resultList) {
                                 modelInfoDtos.add(this.objectsToModelInfoDto(modelInfoDto));
                              }

                              for (ModelInfoDto mid : modelInfoDtos) {
                                 if (mid.getModelStatus() == -2) {
                                    this.logger.error("该路径下存在已下线模型");
                                    return "0%";
                                 }
                              }
                           }

                           if (modelTypeCount >= 1) {
                              RequestData requestData = new RequestData();
                              requestData.setTableName(dataSource);
                              requestData.setFuncExpression("count(id) as count");
                              requestData.addColumn("id");
                              requestData.addDataAuthToFilterMap(dataAuth);
                              BuildTopicFilterBySpark.groupTopicCondRequest(topicCondition, requestData);
                              this.groupPathConDRequest(requestData, first, dimenMap);
                              BuildTopicFilterBySpark.groupProcessParam(requestData);
                              MultipleDataConstant.setMultipleDataSql(requestData, batchId, ifLone, dataType);
                              this.logger.info("【获取漏斗分析总体转化率2】：{}", JSON.toJSONString(requestData));
                              String firstValue = this.getResultData(requestData, this.df3, 0);
                              requestData.getFilterMap().clear();
                              if (requestData.getModelIdList() != null && requestData.getModelIdList().size() > 0) {
                                 requestData.getModelIdList().clear();
                              }

                              if (requestData.getExcludeModelIdList() != null && requestData.getExcludeModelIdList().size() > 0) {
                                 requestData.getExcludeModelIdList().clear();
                              }

                              BuildTopicFilterBySpark.groupProcessParam(requestData);
                              MultipleDataConstant.setMultipleDataSql(requestData, batchId, ifLone, dataType);
                              BuildTopicFilterBySpark.groupTopicCondRequest(topicCondition, requestData);
                              this.groupPathConDRequest(requestData, pathCond, dimenMap);
                              this.logger.info("【获取漏斗分析总体转化率3】：{}", JSON.toJSONString(requestData));
                              String totalValue = this.getResultData(requestData, this.df3, 0);
                              if ((int)Double.parseDouble(firstValue) == 0) {
                                 totalRate = "0.00%";
                              } else {
                                 double result = Double.parseDouble(totalValue) / Double.parseDouble(firstValue);
                                 totalRate = this.df3.format(result * 100.0) + "%";
                              }
                           } else {
                              String firstCon = this.groupPathConDSql(first, dimenMap);
                              String firstSql = sql + " and " + firstCon;
                              this.logger.info("anydri非全局获取转化率1:{}", firstSql);
                              String firstValuex = this.getResultDataByAnyDri(firstSql, this.df3, 0, dataSource);
                              String totalCon = this.groupPathConDSql(pathCond, dimenMap);
                              String totalSql = sql + " and " + totalCon;
                              this.logger.info("anydri非全局获取转化率2:{}", totalSql);
                              String totalValue = this.getResultDataByAnyDri(totalSql, this.df3, 0, dataSource);
                              if (Double.parseDouble(firstValuex) == 0.0) {
                                 totalRate = "0.00%";
                              } else {
                                 double result = Double.parseDouble(totalValue) / Double.parseDouble(firstValuex);
                                 totalRate = this.df3.format(result * 100.0) + "%";
                              }
                           }

                           return totalRate;
                        } else {
                           this.logger.info("anydri全局路径获取转化率:{}", sql);
                           return this.getResultDataByAnyDri(sql, this.df3, 100, dataSource) + "%";
                        }
                     } else {
                        this.logger.error("获取专题的原始条件为空");
                        throw new VieAppServiceException("获取专题的原始条件为空");
                     }
                  }
               }
            } catch (VieAppServiceException var36) {
               this.logger.error("该专题已被删除", var36);
               throw new VieAppServiceException("该专题已被删除", var36);
            } catch (Exception var37) {
               this.logger.error("[获取漏斗分析总体转化率]失败", var37);
               throw new VieAppServiceException("服务内部错误", var37);
            }
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public FunnelChartResponse getFunnelChart(FunnelCallRequest funnelCallRequest) throws VieAppServiceException {
      FunnelChartResponse funnelChartResponse = new FunnelChartResponse();
      List<String> nameList = new ArrayList<>();
      List<String> dataList = new ArrayList<>();
      List<String> condList = new ArrayList<>();
      List<String> rateList = new ArrayList<>();
      if (funnelCallRequest != null) {
         Integer dataType = funnelCallRequest.getDataType();
         String batchId = funnelCallRequest.getBatchId();
         int ifLone = funnelCallRequest.getIfLone();
         String dataSource = funnelCallRequest.getDataSource();
         String userId = funnelCallRequest.getUserId();
         String systemId = funnelCallRequest.getSystemId();
         List<String> keyList = new ArrayList<>();
         keyList.add("userId");
         keyList.add("systemId");
         keyList.add("pathId");
         keyList.add("topicId");
         keyList.add("dataSource");
         String checkResult = this.checkRequestParamNull(funnelCallRequest, keyList);
         if (checkResult != null) {
            this.logger.error("参数({})为空", checkResult);
            throw new VieAppServiceException("参数(" + checkResult + ")为空");
         } else {
            try {
               TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(funnelCallRequest.getTopicId()));
               if (topicInfo == null) {
                  throw new VieAppServiceException("该专题已被删除");
               } else {
                  TopicPath topicPath = this.topicPathDB.getPathById(funnelCallRequest.getPathId());
                  if (topicPath == null) {
                     this.logger.error("获取路径条件为空");
                     throw new VieAppServiceException("获取路径条件为空");
                  } else {
                     String topicCondition = topicInfo.getTopicCondition();
                     if (topicCondition != null && !"".equals(topicCondition)) {
                        String dataSql = this.commonData.getDataSql(userId, systemId, dataSource);
                        String topicConDSql = BuildTopicFilterByAnydri.groupTopicCondByAnyDri(topicCondition);
                        String sql = "";
                        if (StringUtils.isNullOrEmpry(dataSql)) {
                           sql = "select sum(idCounter) as result from " + dataSource + " where " + topicConDSql + " and processed=0 and ";
                        } else {
                           sql = "select sum(idCounter) as result from " + dataSource + " where " + topicConDSql + " and " + dataSql + " and processed=0 and ";
                        }

                        String multiDatasql = MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType);
                        sql = sql + multiDatasql;
                        HashMap<String, Set<String>> dataAuth = this.commonData.getDataMapAuth(userId, systemId, dataSource);
                        if (topicPath.getApplicationId() != 1 && !"[]".equals(topicPath.getPathCondition()) && !"".equals(topicPath.getPathCondition())) {
                           RequestData requestData = new RequestData();
                           requestData.setTableName(dataSource);
                           requestData.addColumn("id");
                           requestData.setFuncExpression("count(id) as count");
                           requestData.addDataAuthToFilterMap(dataAuth);
                           List<HashMap<String, String>> pa = (List<HashMap<String, String>>)this.mapper.readValue(topicPath.getPathCondition(), List.class);
                           LinkedHashMap<String, DimensionConfig> dimenMap = this.commonData.getDimensionMap(dataSource);
                           int modelTypeCount = 0;
                           List<String> modelIds = new ArrayList<>();

                           for (HashMap<String, String> currentPath : (List<HashMap<String, String>>)pa) {
                              if (currentPath.containsValue("model")) {
                                 modelIds.add(String.valueOf(currentPath.get("value")));
                                 modelTypeCount++;
                              }
                           }

                           List<Object[]> resultList = null;
                           if (modelIds != null && modelIds.size() > 0) {
                              resultList = this.modelDB.querySqlModelByID(modelIds);
                           }

                           List<ModelInfoDto> modelInfoDtos = new ArrayList<>();
                           if (resultList != null && resultList.size() != 0) {
                              for (Object[] modelInfoDto : resultList) {
                                 modelInfoDtos.add(this.objectsToModelInfoDto(modelInfoDto));
                              }

                              for (ModelInfoDto mid : modelInfoDtos) {
                                 if (mid.getModelStatus() == -2) {
                                    this.logger.error("该路径下存在已下线模型");
                                    return null;
                                 }
                              }
                           }

                           int count = 0;

                           for (int i = 0; i < pa.size(); i++) {
                              String pathConditonName = this.commonData.getPathConditionName(topicPath.getPathCondition(), count, dataSource);
                              List<HashMap<String, String>> li = new ArrayList<>();
                              List<HashMap<String, String>> pathList = new ArrayList<>();
                              String conditionStr = "";
                              if (modelTypeCount >= 1) {
                                 for (int j = 0; j <= i; j++) {
                                    li.clear();
                                    li.add(pa.get(j));
                                    pathList.add(pa.get(j));
                                    this.groupPathConDRequest(requestData, li, dimenMap);
                                    List<Map<String, String>> requestDataTemp = requestData.getFilterMap();
                                    if (requestDataTemp != null) {
                                       if (j == 0) {
                                          conditionStr = JSON.toJSONString(li);
                                       } else {
                                          conditionStr = conditionStr + " and; " + JSON.toJSONString(li);
                                       }
                                    }
                                 }

                                 BuildTopicFilterBySpark.groupTopicCondRequest(topicCondition, requestData);
                                 if (requestData.getModelIdList() != null && requestData.getModelIdList().size() > 0) {
                                    requestData.getModelIdList().clear();
                                 }

                                 if (requestData.getExcludeModelIdList() != null && requestData.getExcludeModelIdList().size() > 0) {
                                    requestData.getExcludeModelIdList().clear();
                                 }

                                 this.groupPathConDRequest(requestData, pathList, dimenMap);
                                 BuildTopicFilterBySpark.groupProcessParam(requestData);
                                 MultipleDataConstant.setMultipleDataSql(requestData, batchId, ifLone, dataType);
                                 this.logger.info("【获取漏斗分析图表2】：{}", JSON.toJSONString(requestData));
                                 String data = this.getResultData(requestData, this.df, 0);
                                 if (dataList != null && dataList.size() > 0) {
                                    String lastCounts = dataList.get(dataList.size() - 1);
                                    if ("0".equals(lastCounts)) {
                                       rateList.add("0%");
                                    } else {
                                       String rate = this.df3.format(Double.parseDouble(data) / Double.parseDouble(lastCounts) * 100.0) + "%";
                                       rateList.add(rate);
                                    }
                                 }

                                 nameList.add(pathConditonName);
                                 dataList.add(data);
                                 condList.add(conditionStr);
                                 count++;
                              } else {
                                 for (int jx = 0; jx <= i; jx++) {
                                    li.clear();
                                    li.add(pa.get(jx));
                                    pathList.clear();
                                    pathList.add(pa.get(jx));
                                    if (jx == 0) {
                                       conditionStr = JSON.toJSONString(li);
                                    } else {
                                       conditionStr = conditionStr + " and; " + JSON.toJSONString(li);
                                    }
                                 }

                                 String anydirllPathCondSql = this.groupPathConDSql(pathList, dimenMap);
                                 sql = sql + " and " + anydirllPathCondSql;
                                 this.logger.info("anydri非全局获取图表：{}", sql);
                                 String data = this.getResultDataByAnyDri(sql, this.df, 0, dataSource);
                                 if (dataList != null && dataList.size() > 0) {
                                    String lastCounts = dataList.get(dataList.size() - 1);
                                    if ("0".equals(lastCounts)) {
                                       rateList.add("0%");
                                    } else {
                                       String rate = this.df3.format(Double.parseDouble(data) / Double.parseDouble(lastCounts) * 100.0) + "%";
                                       rateList.add(rate);
                                    }
                                 }

                                 nameList.add(pathConditonName);
                                 dataList.add(data);
                                 condList.add(conditionStr);
                                 count++;
                              }
                           }

                           funnelChartResponse.setCondition(condList);
                           funnelChartResponse.setData(dataList);
                           funnelChartResponse.setRate(rateList);
                           funnelChartResponse.setName(nameList);
                           return funnelChartResponse;
                        } else {
                           this.logger.info("anydri全局获取图表：{}", sql);
                           String data = this.getResultDataByAnyDri(sql, this.df, 0, dataSource);
                           nameList.add("主路径");
                           dataList.add(data);
                           condList.add(topicCondition);
                           funnelChartResponse.setCondition(condList);
                           funnelChartResponse.setData(dataList);
                           funnelChartResponse.setName(nameList);
                           return funnelChartResponse;
                        }
                     } else {
                        this.logger.error("获取专题的原始条件为空");
                        throw new VieAppServiceException("获取专题的原始条件为空");
                     }
                  }
               }
            } catch (VieAppServiceException var41) {
               this.logger.error("该专题已被删除", var41);
               throw new VieAppServiceException("该专题已被删除", var41);
            } catch (Exception var42) {
               this.logger.error("[获取漏斗分析图表]失败", var42);
               throw new VieAppServiceException("服务内部错误", var42);
            }
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public FunnelTableResponse getFunnelTable(FunnelCallRequest funnelCallRequest) throws VieAppServiceException {
      FunnelTableResponse funnelTableResponse = new FunnelTableResponse();
      List<String> columnList = new ArrayList<>();
      List<String> numList = new ArrayList<>();
      List<String> rateList = new ArrayList<>();
      if (funnelCallRequest != null) {
         Integer dataType = funnelCallRequest.getDataType();
         String batchId = funnelCallRequest.getBatchId();
         int ifLone = funnelCallRequest.getIfLone();
         String dataSource = funnelCallRequest.getDataSource();
         String userId = funnelCallRequest.getUserId();
         String systemId = funnelCallRequest.getSystemId();
         List<String> keyList = new ArrayList<>();
         keyList.add("userId");
         keyList.add("systemId");
         keyList.add("pathId");
         keyList.add("topicId");
         keyList.add("dataSource");
         keyList.add("allPathFlag");
         String checkResult = this.checkRequestParamNull(funnelCallRequest, keyList);
         if (checkResult != null) {
            this.logger.error("参数({})为空", checkResult);
            throw new VieAppServiceException("参数(" + checkResult + ")为空");
         } else {
            try {
               TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(funnelCallRequest.getTopicId()));
               if (topicInfo == null) {
                  throw new VieAppServiceException("该专题已被删除");
               } else {
                  TopicPath topicPath = this.topicPathDB.getPathById(funnelCallRequest.getPathId());
                  if (topicPath == null) {
                     this.logger.error("获取路径条件为空");
                     throw new VieAppServiceException("获取路径条件为空");
                  } else {
                     String pathCondition = topicPath.getPathCondition();
                     String topicCondition = topicInfo.getTopicCondition();
                     if (topicCondition != null && !"".equals(topicCondition)) {
                        String dataSql = this.commonData.getDataSql(userId, systemId, dataSource);
                        String topicConDSql = BuildTopicFilterByAnydri.groupTopicCondByAnyDri(topicCondition);
                        String sql = "";
                        if (StringUtils.isNullOrEmpry(dataSql)) {
                           sql = "select sum(idCounter) as result from " + dataSource + " where " + topicConDSql + " and processed=0 and ";
                        } else {
                           sql = "select sum(idCounter) as result from " + dataSource + " where " + topicConDSql + " and " + dataSql + " and processed=0 and ";
                        }

                        String multiDatasql = MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType);
                        sql = sql + multiDatasql;
                        HashMap<String, Set<String>> dataAuth = this.commonData.getDataMapAuth(userId, systemId, dataSource);
                        LinkedHashMap<String, DimensionConfig> dimenMap = this.commonData.getDimensionMap(dataSource);
                        if (topicPath.getApplicationId() != 1 && !"[]".equals(topicPath.getPathCondition()) && !"".equals(topicPath.getPathCondition())) {
                           List<HashMap<String, String>> pa = null;
                           if (!StringUtils.isNullOrEmpry(pathCondition)) {
                              pa = (List<HashMap<String, String>>)this.mapper.readValue(pathCondition, List.class);
                           }

                           RequestData requestData = new RequestData();
                           requestData.setTableName(dataSource);
                           requestData.addColumn("id");
                           requestData.setFuncExpression("count(id) as count");
                           requestData.addDataAuthToFilterMap(dataAuth);
                           int modelTypeCount = 0;
                           List<String> modelIds = new ArrayList<>();
                           if (pa != null) {
                              for (HashMap<String, String> currentPath : (List<HashMap<String, String>>)pa) {
                                 if (currentPath.containsValue("model")) {
                                    modelIds.add(String.valueOf(currentPath.get("value")));
                                    modelTypeCount++;
                                 }
                              }
                           }

                           List<Object[]> resultList = null;
                           if (modelIds != null && modelIds.size() > 0) {
                              resultList = this.modelDB.querySqlModelByID(modelIds);
                           }

                           List<ModelInfoDto> modelInfoDtos = new ArrayList<>();
                           if (resultList != null && resultList.size() != 0) {
                              for (Object[] modelInfoDto : resultList) {
                                 modelInfoDtos.add(this.objectsToModelInfoDto(modelInfoDto));
                              }

                              for (ModelInfoDto mid : modelInfoDtos) {
                                 if (mid.getModelStatus() == -2) {
                                    this.logger.error("该路径下存在已下线模型");
                                    return null;
                                 }
                              }
                           }

                           List<Object> list = null;
                           if (funnelCallRequest.getAllPathFlag() == 1) {
                              if (modelTypeCount >= 1) {
                                 list = this.getPathData2(topicCondition, pathCondition, requestData, dataSource, dataType, ifLone, batchId);
                              } else {
                                 list = this.getPathDataByAnyDri2(pathCondition, dimenMap, dataSource, sql);
                              }

                              funnelTableResponse.setColumn((List)list.get(0));
                              funnelTableResponse.setNum((List)list.get(1));
                              funnelTableResponse.setRate((List)list.get(2));
                           } else {
                              String condition = funnelCallRequest.getCondition();
                              if (modelTypeCount >= 1) {
                                 list = this.getPathData(topicCondition, condition, requestData, dataSource, dataType, ifLone, batchId, dimenMap);
                              } else {
                                 list = this.getPathDataByAnyDri(condition, dimenMap, sql, dataSource);
                              }

                              funnelTableResponse.setColumn((List)list.get(0));
                              funnelTableResponse.setNum((List)list.get(1));
                           }

                           return funnelTableResponse;
                        } else {
                           this.logger.info("anydri全局获取表格：{}", sql);
                           String data = this.getResultDataByAnyDri(sql, this.df, 0, dataSource);
                           if (funnelCallRequest.getAllPathFlag() == 1) {
                              columnList.add("主路径");
                              numList.add(data);
                              if ("0".equals(data)) {
                                 rateList.add("0.00%");
                              } else {
                                 rateList.add("100.00%");
                              }
                           } else {
                              columnList = this.commonData.getFunnelColumn();
                              numList.add("0");
                              numList.add(data);
                              if ("0".equals(data)) {
                                 numList.add("0.00%");
                              } else {
                                 numList.add("100.00%");
                              }
                           }

                           funnelTableResponse.setColumn(columnList);
                           funnelTableResponse.setNum(numList);
                           funnelTableResponse.setRate(rateList);
                           return funnelTableResponse;
                        }
                     } else {
                        this.logger.error("获取专题的原始条件为空");
                        throw new VieAppServiceException("获取专题的原始条件为空");
                     }
                  }
               }
            } catch (VieAppServiceException var33) {
               this.logger.error("该专题已被删除", var33);
               throw new VieAppServiceException("该专题已被删除", var33);
            } catch (Exception var34) {
               this.logger.error("[获取漏斗分析表格]失败", var34);
               throw new VieAppServiceException("服务内部错误", var34);
            }
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public FunnelCallResponse getFunnelList(FunnelCallRequest funnelCallRequest) throws VieAppServiceException {
      FunnelCallResponse funnelCallResponse = null;
      if (funnelCallRequest == null) {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      } else {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("condition");
            keyList.add("userId");
            keyList.add("systemId");
            keyList.add("pathId");
            keyList.add("topicId");
            keyList.add("pageNum");
            keyList.add("pageSize");
            keyList.add("sortColumn");
            keyList.add("sortType");
            keyList.add("isNumData");
            String checkResult = this.checkRequestParamNull(funnelCallRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数" + checkResult + "为空");
            } else {
               Integer dataType = funnelCallRequest.getDataType();
               String batchId = funnelCallRequest.getBatchId();
               int ifLone = funnelCallRequest.getIfLone();
               Integer topicId = funnelCallRequest.getTopicId();
               String dimension = funnelCallRequest.getSearchDimension();
               String dataSource = funnelCallRequest.getDataSource();
               String systemId = funnelCallRequest.getSystemId();
               String userId = funnelCallRequest.getUserId();
               String condition = funnelCallRequest.getCondition();
               Integer pageNum = funnelCallRequest.getPageNum();
               Integer pageSize = funnelCallRequest.getPageSize();
               Integer isNumData = funnelCallRequest.getIsNumData();
               String sortType = funnelCallRequest.getSortType();
               String sortColumn = funnelCallRequest.getSortColumn();
               TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(topicId));
               if (topicInfo == null) {
                  this.logger.error("获取专题信息为空");
                  return null;
               } else {
                  if ("modelName".equals(sortColumn) || "keyword".equals(sortColumn)) {
                     sortColumn = "id";
                  }

                  String topicCondition = topicInfo.getTopicCondition();
                  RequestData requestData = new RequestData();
                  LinkedHashMap<String, DimensionConfig> dimenMap = this.commonData.getDimensionMap(dataSource);
                  List<String> durationList = BuildTopicFilterByAnydri.getDurationDimension(dimenMap);
                  requestData.setTableName(dataSource);
                  HashMap<String, Set<String>> dataAuth = this.commonData.getDataMapAuth(userId, systemId, dataSource);
                  ColumnInfo columnInfo = this.commonData.getColumnMaps2(dimension, dataSource);
                  List<String> columns = new ArrayList<>();
                  AuthorizeInfo authorizeInfo = new AuthorizeInfo();
                  authorizeInfo.setUserId(userId);
                  authorizeInfo.setDataSource(dataSource);
                  HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
                  if (columnInfo != null) {
                     columns = columnInfo.getColumns();
                  }

                  int modelTypeCount = 0;
                  String[] conditionStr = condition.split("and;");

                  for (String curentStr : conditionStr) {
                     List<HashMap<String, String>> pathList = (List<HashMap<String, String>>)this.mapper.readValue(curentStr, List.class);
                     if (pathList != null) {
                        HashMap<String, String> map = pathList.get(0);
                        if ("model".equals(map.get("field"))) {
                           modelTypeCount++;
                        }
                     }
                  }

                  funnelCallResponse = new FunnelCallResponse();
                  double inCount = 0.0;
                  double outCount = 0.0;
                  boolean flag = false;
                  boolean isSubCondFlag = false;
                  if (!StringUtils.isNullOrEmpry(condition) && !condition.contains("and;")) {
                     if (isNumData == 0) {
                        flag = true;
                     } else {
                        List<HashMap<String, Object>> list = (List<HashMap<String, Object>>)this.mapper.readValue(condition, List.class);
                        if (list != null && list.size() > 0) {
                           HashMap<String, Object> map = list.get(0);
                           if (map != null && String.valueOf(map.get("name")).equals("起止时间")) {
                              isSubCondFlag = true;
                           }
                        }
                     }
                  }

                  if (modelTypeCount >= 1) {
                     requestData.addDataAuthToFilterMap(dataAuth);
                     BuildTopicFilterBySpark.groupTopicCondRequest(topicCondition, requestData);
                     BuildTopicFilterBySpark.groupProcessParam(requestData);
                     MultipleDataConstant.setMultipleDataSql(requestData, batchId, ifLone, dataType);
                     this.logger.info(JSON.toJSONString(requestData));
                     if (isNumData == 1) {
                        this.groupInversionCondition(requestData, condition, dimenMap);
                        this.logger.info(JSON.toJSONString(requestData));
                        inCount = this.queryCountData(requestData);
                        funnelCallResponse.setTotalCount(inCount);
                     } else {
                        this.groupLossCondition(requestData, condition, dimenMap);
                        this.logger.info(JSON.toJSONString(requestData));
                        if (!isSubCondFlag && !flag) {
                           outCount = this.queryCountData(requestData);
                        } else {
                           outCount = 0.0;
                        }

                        funnelCallResponse.setTotalCount(outCount);
                     }

                     this.logger.info(JSON.toJSONString(requestData));
                     requestData.setOrderDesc("order by " + sortColumn + " " + sortType);
                     PagerResponse<LinkedHashMap<String, Object>> pagerResponse = this.queryDetailData(
                        pageNum, pageSize, requestData, columns, modelMap, outCount, isNumData, modelTypeCount, durationList, dataSource
                     );
                     funnelCallResponse.setColumns(columnInfo.getColumnMaps());
                     pagerResponse.setPageNum(pageNum);
                     pagerResponse.setPageSize(pageSize);
                     funnelCallResponse.setPreviewList(pagerResponse);
                  } else {
                     String dataSql = this.commonData.getDataSql(userId, systemId, dataSource);
                     String topicConDSql = BuildTopicFilterByAnydri.groupTopicCondByAnyDri(topicCondition);
                     String pageCondSql = this.groupPageCondSql(pageNum, pageSize);
                     String sql = "";
                     if (StringUtils.isNullOrEmpry(dataSql)) {
                        sql = "select sum(idCounter) as result from " + dataSource + " where " + topicConDSql + " and processed=0 and ";
                     } else {
                        sql = "select sum(idCounter) as result from " + dataSource + " where " + topicConDSql + " and " + dataSql + " and processed=0 and ";
                     }

                     String multiDatasql = MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType);
                     sql = sql + multiDatasql;
                     String pathConSql = "";
                     if (isNumData == 1) {
                        if (!isSubCondFlag) {
                           pathConSql = this.groupInversionCondByAnyDri(condition, dimenMap);
                        }

                        if (!StringUtils.isNullOrEmpry(pathConSql)) {
                           sql = sql + " and " + pathConSql;
                        }

                        this.logger.info("anydri查询转化量：{}", sql);
                        inCount = this.queryCountDataByAnyDri(sql, dataSource);
                        funnelCallResponse.setTotalCount(inCount);
                     } else {
                        if (!isSubCondFlag && !flag) {
                           if (!isSubCondFlag) {
                              pathConSql = this.groupLossCondByAnyDri(condition, dimenMap);
                           }

                           if (!StringUtils.isNullOrEmpry(pathConSql)) {
                              sql = sql + " and " + pathConSql;
                           }

                           this.logger.info("anydri查询流失量：{}", sql);
                           outCount = this.queryCountDataByAnyDri(sql, dataSource);
                        } else {
                           outCount = 0.0;
                        }

                        funnelCallResponse.setTotalCount(outCount);
                     }

                     PagerResponse<LinkedHashMap<String, Object>> pagerResponse = this.queryDetailDataByAnyDri(
                        columns,
                        outCount,
                        dataSql,
                        topicConDSql,
                        pageCondSql,
                        dataSource,
                        batchId,
                        ifLone,
                        dataType,
                        isNumData,
                        condition,
                        dimenMap,
                        isSubCondFlag,
                        modelMap,
                        modelTypeCount,
                        durationList,
                        sortType,
                        sortColumn
                     );
                     funnelCallResponse.setColumns(columnInfo.getColumnMaps());
                     pagerResponse.setPageNum(pageNum);
                     pagerResponse.setPageSize(pageSize);
                     int totalPages = (int)pagerResponse.getTotalRows() % pageSize == 0
                        ? (int)pagerResponse.getTotalRows() / pageSize
                        : (int)pagerResponse.getTotalRows() / pageSize + 1;
                     pagerResponse.setTotalPages(totalPages);
                     funnelCallResponse.setPreviewList(pagerResponse);
                  }

                  return funnelCallResponse;
               }
            }
         } catch (VieCommonException var46) {
            this.logger.error("getFunnelList方法内部异常", var46);
            if (1001 == var46.getErrorCode()) {
               throw new VieAppServiceException("该路径存在已下线的模型／已删除的维度，请将其移出路径后重新钻取", var46);
            } else {
               throw new VieAppServiceException("内部服务异常", var46);
            }
         } catch (Exception var47) {
            this.logger.error("获取漏斗详细列表失败", var47);
            throw new VieAppServiceException("服务器内部错误", var47);
         }
      }
   }

   private String groupPageCondSql(Integer pageNum, Integer pageSize) {
      String pageCondSql = "";
      int rownumStart = pageSize * (pageNum - 1) + 1;
      int rownumEnd = pageNum * pageSize;
      return " rownum between " + rownumStart + " and " + rownumEnd;
   }

   private double queryCountData(RequestData requestData) throws Exception {
      List<LinkedHashMap<String, Object>> mapList = null;
      double count = 0.0;
      requestData.addColumn("id");
      requestData.setFuncExpression("count(id) as count");
      ResponseData responseData = this.sparkUtils.execute(requestData);
      if (responseData != null) {
         mapList = responseData.getRsList();
      }

      if (mapList != null && mapList.size() > 0 && mapList.get(0) != null) {
         count = Double.parseDouble(String.valueOf(mapList.get(0).get("count")));
      }

      return count;
   }

   private double queryCountDataByAnyDri(String sql, String dataSource) {
      double count = 0.0;
      List<LinkedHashMap<String, Object>> mapList = null;
      String anydrillMainAddr = "";

      try {
         if (sql.contains("offLineTagId")) {
            anydrillMainAddr = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
         } else {
            anydrillMainAddr = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
         }

         this.logger.info("anydri查询统计索引地址：{}", anydrillMainAddr);
         PreparedStatement pst = new PreparedStatement(anydrillMainAddr.split(";"));
         ResultSet rs = pst.executeQuery(sql);
         mapList = rs.toList();
         if (mapList != null && mapList.size() > 0 && mapList.get(0) != null) {
            count = Double.parseDouble(String.valueOf(mapList.get(0).get("result")));
         }
      } catch (Exception var9) {
         this.logger.error("获取anydrill查询结果失败", var9);
      }

      return count;
   }

   private PagerResponse<LinkedHashMap<String, Object>> queryDetailData(
      Integer pageNum,
      Integer pageSize,
      RequestData requestData,
      List<String> columns,
      HashMap<String, String> modelMap,
      double outCount,
      Integer isNumData,
      int modelTypeCount,
      List<String> durationList,
      String dataSource
   ) throws VieAppServiceException {
      PagerResponse<LinkedHashMap<String, Object>> result = new PagerResponse();
      if (isNumData != 1 && outCount == 0.0) {
         result.setTotalRows(0L);
         result.setTotalPages(0);
         result.setRows(null);
         return result;
      } else {
         requestData.getColumn().clear();
         requestData.setColumn(columns);
         requestData.setFuncExpression(null);
         requestData.setPageNum(pageNum);
         requestData.setPageSize(pageSize);
         this.logger.info(JSON.toJSONString(requestData));

         try {
            ResponseData responseData = this.sparkUtils.execute(requestData);
            if (responseData == null) {
               return null;
            } else {
               long totalCount = responseData.getTotalCount();
               List<LinkedHashMap<String, Object>> valueList = responseData.getRsList();
               this.groupModelInfoColumn(requestData);
               boolean modelColflag = false;
               if (requestData.getColumn().contains(CommonParams.modelFiemld)) {
                  modelColflag = true;
               }

               String modelInfo = IndexConstants.getModelInfor(requestData.getColumn());
               if (!StringUtils.isNullOrEmpry(modelInfo)) {
                  requestData.setIfShowModel(true);
               }

               String anydrillAddressModel = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
               List<LinkedHashMap<String, Object>> list = this.dealModelReturnValue(
                  anydrillAddressModel.split(";"), valueList, requestData.getTableName(), modelInfo, modelMap, modelTypeCount, durationList
               );
               if (modelColflag) {
                  result.setRows(list);
               } else {
                  result.setRows(valueList);
               }

               result.setTotalRows(totalCount);
               return result;
            }
         } catch (Exception var21) {
            this.logger.error("queryDetailData接口出错", var21);
            throw new VieAppServiceException("服务内部错误", var21);
         }
      }
   }

   private PagerResponse<LinkedHashMap<String, Object>> queryDetailDataByAnyDri(
      List<String> columns,
      double outCount,
      String dataSql,
      String topicConDSql,
      String pageCondSql,
      String tableName,
      String batchId,
      Integer ifLone,
      Integer dataType,
      Integer isNumData,
      String condition,
      LinkedHashMap<String, DimensionConfig> dimenMap,
      boolean isSubCondFlag,
      HashMap<String, String> modelMap,
      int modelTypeCount,
      List<String> durationList,
      String sortType,
      String sortColumn
   ) throws VieAppServiceException {
      PagerResponse<LinkedHashMap<String, Object>> result = new PagerResponse();
      if (isNumData != 1 && outCount == 0.0) {
         result.setTotalRows(0L);
         result.setTotalPages(0);
         result.setRows(null);
         return result;
      } else {
         String sql = "select ";
         String pathConSql = "";

         try {
            for (String column : columns) {
               sql = sql + column + ", ";
            }

            if (sql.length() > 2) {
               sql = sql.substring(0, sql.length() - 2);
            }

            String multiDatasql = MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType);
            sql = sql + " from " + tableName + " where " + topicConDSql + " and " + dataSql + " and processed=0 and ";
            sql = sql + multiDatasql;
            sql = sql + " and " + pageCondSql;
            if (isNumData == 1) {
               if (!isSubCondFlag) {
                  pathConSql = this.groupInversionCondByAnyDri(condition, dimenMap);
               }

               if (!StringUtils.isNullOrEmpry(pathConSql)) {
                  sql = sql + " and " + pathConSql;
               }
            } else {
               if (!isSubCondFlag) {
                  pathConSql = this.groupLossCondByAnyDri(condition, dimenMap);
               }

               if (!StringUtils.isNullOrEmpry(pathConSql)) {
                  sql = sql + " and " + pathConSql;
               }
            }

            List<LinkedHashMap<String, Object>> mapList = null;
            long totalCount = 0L;
            String anydrillAddr = "";
            anydrillAddr = DynamicEsSource.getEsSourceByType(tableName, "mainAnydrillAddress");
            sql = sql + " order by " + sortColumn + " " + sortType;
            PreparedStatement pst = new PreparedStatement(anydrillAddr.split(";"));
            this.logger.info("anydri查询明细：{}", sql);
            this.logger.info("anydri查询明细索引地址：{}", anydrillAddr);
            ResultSet rs = pst.executeQuery(sql);
            if (rs == null) {
               return null;
            } else {
               mapList = rs.toList();
               anydrillAddr = DynamicEsSource.getEsSourceByType(tableName, "modelAnydrillAddress");
               columns.add("offLineTagId");
               columns.add("offLineTagInfo");
               columns.add("offLineTagName");
               String modelInfo = IndexConstants.getModelInfor(columns);
               mapList = this.dealModelReturnValue(anydrillAddr.split(";"), mapList, tableName, modelInfo, modelMap, modelTypeCount, durationList);
               totalCount = rs.getTotalCount();
               result.setRows(mapList);
               result.setTotalRows(totalCount);
               return result;
            }
         } catch (Exception var31) {
            this.logger.error("queryDetailData接口出错", var31);
            throw new VieAppServiceException("服务内部错误", var31);
         }
      }
   }

   private List<LinkedHashMap<String, Object>> dealModelReturnValue(
      String[] connectUrl,
      List<LinkedHashMap<String, Object>> data,
      String tableName,
      String modelInfo,
      HashMap<String, String> allModels,
      int modelTypeCount,
      List<String> durationList
   ) throws JsonParseException, JsonMappingException, IOException {
      if (data != null && data.size() != 0) {
         Map<String, List<Map<String, Object>>> modelMap = null;
         String idStr = IndexConstants.getIdStr(data);
         if (!StringUtils.isNullOrEmpry(idStr)) {
            modelMap = IndexConstants.getModelInforById(connectUrl, tableName, modelInfo, idStr, null);
         }

         List<LinkedHashMap<String, Object>> realData = new ArrayList<>();
         StringBuffer modelNames = null;
         List<Map<String, Object>> keywords = null;
         Map<String, Object> dataMaps = null;

         for (LinkedHashMap<String, Object> eachCd : data) {
            Map<String, Object> var28 = new HashMap();
            Set<Entry<String, Object>> entries = eachCd.entrySet();
            if (entries != null) {
               for (Entry<String, Object> entry : entries) {
                  String key = entry.getKey();
                  Object value = entry.getValue();
                  if (durationList.contains(key)) {
                     if (!"n0avgSpeed".equals(key) && !"n1avgSpeed".equals(key)) {
                        if (value != null && !"".equals(value)) {
                           value = Math.round(Double.parseDouble(String.valueOf(value)) / 1000.0);
                        } else {
                           value = 0;
                        }
                     } else if (value != null && !"".equals(value)) {
                        value = this.df3.format(Double.parseDouble(String.valueOf(value)) / 1000.0);
                     } else {
                        value = 0.0;
                     }
                  }

                  var28.put(key, value);
               }
            }

            String id = String.valueOf(eachCd.get("id"));
            if (modelMap != null) {
               modelNames = new StringBuffer();
               keywords = new ArrayList<>();
               List<Map<String, Object>> modelList = modelMap.get(id);
               if (modelList != null && modelList.size() > 0) {
                  for (Map<String, Object> hm : (List<Map<String, Object>>)modelList) {
                     String offLineId = String.valueOf(hm.get("offLineTagId"));
                     if (!StringUtils.isNullOrEmpry(offLineId)) {
                        String modelName = allModels.get(offLineId);
                        if (!StringUtils.isNullOrEmpry(modelName)) {
                           if (modelNames.length() > 0) {
                              modelNames.append(",");
                           }

                           modelNames.append(modelName);
                           if (hm.get("offLineTagInfo") != null && !hm.get("offLineTagInfo").equals("[]")) {
                              ObjectMapper mapper = new ObjectMapper();

                              for (HashMap<String, Object> hmss : (List<HashMap<String, Object>>)mapper.readValue(hm.get("offLineTagInfo").toString(), List.class)) {
                                 hmss.put("begin", hmss.get("beginTime"));
                                 hmss.put("end", hmss.get("endTime"));
                                 hmss.put("word", hmss.get("content"));
                                 int type = Integer.parseInt(String.valueOf(hmss.get("type")));
                                 if (keywords != null && type == 0) {
                                    keywords.add(hmss);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            HashSet hs = new HashSet<>(keywords);
            keywords.clear();
            keywords.addAll(hs);
            var28.put("modelName", modelNames);
            eachCd.clear();
            eachCd.put("keywordInfos", keywords);
            eachCd.put("dataMaps", var28);
            realData.add(eachCd);
         }

         return realData;
      } else {
         return null;
      }
   }

   private void groupModelInfoColumn(RequestData requestData) {
      List<String> colList = requestData.getColumn();
      colList.add("offLineTagId");
      colList.add("offLineTagInfo");
   }

   private String getResultData(RequestData requestData, DecimalFormat decimalFormat, int type) {
      String data = "0";

      try {
         this.logger.info(JSON.toJSONString(requestData));
         ResponseData responseData = this.sparkUtils.execute(requestData);
         if (responseData != null) {
            List<LinkedHashMap<String, Object>> valuelist = responseData.getRsList();
            if (valuelist != null && valuelist.size() > 0) {
               String va = String.valueOf(valuelist.get(0).get("count"));
               if (StringUtils.isNullOrEmpry(va) || "null".equals(va)) {
                  data = decimalFormat.format(0L);
               } else if (type != 100) {
                  data = decimalFormat.format(valuelist.get(0).get("count"));
               } else {
                  data = decimalFormat.format(100L);
               }
            }
         }

         return data;
      } catch (Exception var8) {
         this.logger.error("[获取漏斗工具结果]失败", var8);
         return data;
      }
   }

   private String getResultDataByAnyDri(String totalSql, DecimalFormat decimalFormat, int type, String dataSource) {
      String data = "0";
      List<LinkedHashMap<String, Object>> mapList = null;
      String anydrillMainAddr = "";

      try {
         if (totalSql.contains("offLineTagId")) {
            anydrillMainAddr = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
         } else {
            anydrillMainAddr = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
         }

         this.logger.info("查询的索引地址为：{}", anydrillMainAddr);
         PreparedStatement pst = new PreparedStatement(anydrillMainAddr.split(";"));
         ResultSet rs = pst.executeQuery(totalSql);
         mapList = rs.toList();
         if (mapList != null && mapList.size() > 0 && mapList.get(0) != null) {
            String va = String.valueOf(mapList.get(0).get("result"));
            if (StringUtils.isNullOrEmpry(va) || "null".equals(va)) {
               data = decimalFormat.format(0L);
            } else if (type != 100) {
               data = decimalFormat.format(mapList.get(0).get("result"));
            } else {
               data = decimalFormat.format(100L);
            }
         }
      } catch (Exception var11) {
         this.logger.error("获取anydrill查询结果失败", var11);
      }

      return data;
   }

   private List<Object> getPathData(
      String topicCondition,
      String condition,
      RequestData requestData,
      String dataSource,
      Integer dataType,
      int ifLone,
      String batchId,
      LinkedHashMap<String, DimensionConfig> dimensionMap
   ) throws Exception {
      List<Object> objects = new ArrayList<>();
      List<String> dataList = new ArrayList<>();
      BuildTopicFilterBySpark.groupTopicCondRequest(topicCondition, requestData);
      BuildTopicFilterBySpark.groupProcessParam(requestData);
      MultipleDataConstant.setMultipleDataSql(requestData, batchId, ifLone, dataType);
      String[] conArr = null;
      String firstdata = "";
      String lastData = "";
      if (!StringUtils.isNullOrEmpry(condition)) {
         conArr = condition.split("and;");
         if (conArr != null && conArr.length > 0) {
            for (int i = 0; i < conArr.length - 1; i++) {
               List<HashMap<String, String>> pathCond = (List<HashMap<String, String>>)this.mapper.readValue(conArr[i], List.class);
               this.groupPathConDRequest(requestData, pathCond, dimensionMap);
            }

            this.logger.info("【获取路径分析结果1】：{}", JSON.toJSONString(requestData));
            lastData = this.getResultData(requestData, this.df, 0);
            List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(conArr[conArr.length - 1], List.class);
            this.groupPathConDRequest(requestData, mapList, dimensionMap);
            this.logger.info("【获取路径分析结果2】：{}", JSON.toJSONString(requestData));
            firstdata = this.getResultData(requestData, this.df, 0);
         }
      }

      if (conArr != null && conArr.length != 1) {
         String lastCondition = conArr[conArr.length - 1];
         List<HashMap<String, Object>> mapList = (List<HashMap<String, Object>>)this.mapper.readValue(lastCondition, List.class);
         if (mapList != null && mapList.size() > 0) {
            HashMap<String, Object> map = mapList.get(0);
            if (String.valueOf(map.get("field")).equals("model")) {
               if ("0".equals(String.valueOf(map.get("isNegate")))) {
                  requestData.getModelIdList().clear();
               } else {
                  requestData.getExcludeModelIdList().clear();
               }
            }
         }

         int last = Integer.parseInt(lastData);
         int first = Integer.parseInt(firstdata);
         int lostData = last - first;
         if (last == 0) {
            dataList.add("0");
            dataList.add(firstdata);
            dataList.add("0%");
         } else {
            double informationRate = Double.parseDouble(firstdata) / Double.parseDouble(lastData);
            dataList.add(Integer.toString(lostData));
            dataList.add(firstdata);
            dataList.add(this.df3.format(informationRate * 100.0) + "%");
         }
      } else {
         dataList.add("0");
         dataList.add(firstdata);
         dataList.add("100%");
      }

      objects.add(this.commonData.getFunnelColumn());
      objects.add(dataList);
      return objects;
   }

   private List<Object> getPathDataByAnyDri(String condition, LinkedHashMap<String, DimensionConfig> dimensionMap, String sql, String dataSource) throws Exception {
      String firstdata = "";
      String totaldata = "";
      List<Object> objects = new ArrayList<>();
      List<String> dataList = new ArrayList<>();
      if (!StringUtils.isNullOrEmpry(condition)) {
         String firstCondSql = "";
         String lastCondSql = "";
         if (condition.indexOf("and;") == -1) {
            List<HashMap<String, String>> pathCond = (List<HashMap<String, String>>)this.mapper.readValue(condition, List.class);
            lastCondSql = lastCondSql + this.groupPathConDSql(pathCond, dimensionMap);
            sql = sql + " and " + lastCondSql;
            totaldata = this.getResultDataByAnyDri(sql, this.df, 0, dataSource);
         } else {
            String[] conArr = condition.split("and;");
            if (conArr != null && conArr.length > 0) {
               for (int i = 0; i < conArr.length - 1; i++) {
                  List<HashMap<String, String>> pathCond = (List<HashMap<String, String>>)this.mapper.readValue(conArr[i], List.class);
                  firstCondSql = firstCondSql + this.groupPathConDSql(pathCond, dimensionMap) + " and ";
               }

               if (firstCondSql.endsWith("and ")) {
                  firstCondSql = firstCondSql.substring(0, firstCondSql.length() - 4);
               }

               sql = sql + " and " + firstCondSql;
               this.logger.info("anydri非全局层级表格获取表格1：{}", sql);
               firstdata = this.getResultDataByAnyDri(sql, this.df, 0, dataSource);
               List<HashMap<String, String>> lastPathCond = (List<HashMap<String, String>>)this.mapper.readValue(conArr[conArr.length - 1], List.class);
               lastCondSql = this.groupPathConDSql(lastPathCond, dimensionMap);
               sql = sql + " and " + lastCondSql;
               this.logger.info("anydri非全局层级表格获取表格2：{}", sql);
               totaldata = this.getResultDataByAnyDri(sql, this.df, 0, dataSource);
            }
         }

         int total = "".equals(totaldata) ? 0 : Integer.parseInt(totaldata);
         int first = "".equals(firstdata) ? 0 : Integer.parseInt(firstdata);
         int lostData = first - total;
         if (first == 0) {
            dataList.add("0");
            dataList.add(totaldata);
            dataList.add("100%");
         } else {
            double informationRate = Double.parseDouble(totaldata) / Double.parseDouble(firstdata);
            dataList.add(Integer.toString(lostData));
            dataList.add(totaldata);
            dataList.add(this.df3.format(informationRate * 100.0) + "%");
         }
      } else {
         dataList.add("0");
         dataList.add(firstdata);
         dataList.add("100%");
      }

      objects.add(this.commonData.getFunnelColumn());
      objects.add(dataList);
      return objects;
   }

   private List<Object> getPathData2(
      String topicCondition, String pathCondition, RequestData requestData, String dataSource, Integer dataType, int ifLone, String batchId
   ) throws Exception {
      List<Object> resultList = new ArrayList<>();
      List<String> columnList = new ArrayList<>();
      List<String> dataList = new ArrayList<>();
      List<String> rateList = new ArrayList<>();
      List<HashMap<String, String>> pathCond = (List<HashMap<String, String>>)this.mapper
         .readValue(pathCondition, new TypeReference<List<HashMap<String, String>>>() {});
      LinkedHashMap<String, DimensionConfig> dimenMap = this.commonData.getDimensionMap(dataSource);
      int count = 0;

      for (int i = 0; i < pathCond.size(); i++) {
         String pathConditonName = this.commonData.getPathConditionName(pathCondition, count, dataSource);
         List<HashMap<String, String>> list = new ArrayList<>();

         for (int j = 0; j <= i; j++) {
            list.add(pathCond.get(j));
         }

         BuildTopicFilterBySpark.groupTopicCondRequest(topicCondition, requestData);
         this.groupPathConDRequest(requestData, list, dimenMap);
         BuildTopicFilterBySpark.groupProcessParam(requestData);
         MultipleDataConstant.setMultipleDataSql(requestData, batchId, ifLone, dataType);
         this.logger.info("【获取漏斗分析表格】：{}", JSON.toJSONString(requestData));
         String counts = this.getResultData(requestData, this.df, 0);
         String lastCounts = "";
         String rate = "";
         if (dataList != null && dataList.size() > 0) {
            lastCounts = dataList.get(dataList.size() - 1);
            lastCounts = lastCounts.split("\\(")[0];
            if (!"0".equals(counts) && !"0".equals(lastCounts)) {
               rate = this.df3.format(Double.parseDouble(counts) / Double.parseDouble(lastCounts) * 100.0) + "%";
            } else {
               rate = "0.00%";
            }
         } else if ("0".equals(counts)) {
            rate = "0.00%";
         } else {
            rate = "100.00%";
         }

         columnList.add(pathConditonName);
         dataList.add(counts);
         rateList.add(rate);
         count++;
      }

      resultList.add(columnList);
      resultList.add(dataList);
      resultList.add(rateList);
      return resultList;
   }

   private List<Object> getPathDataByAnyDri2(String pathCondition, LinkedHashMap<String, DimensionConfig> dimenMap, String dataSource, String sql) throws Exception {
      List<Object> resultList = new ArrayList<>();
      List<String> columnList = new ArrayList<>();
      List<String> dataList = new ArrayList<>();
      List<String> rateList = new ArrayList<>();
      List<HashMap<String, String>> pathCond = (List<HashMap<String, String>>)this.mapper.readValue(pathCondition, List.class);
      int count = 0;

      for (int i = 0; i < pathCond.size(); i++) {
         String pathConditonName = this.commonData.getPathConditionName(pathCondition, count, dataSource);
         List<HashMap<String, String>> list = new ArrayList<>();

         for (int j = count; j <= i; j++) {
            list.add(pathCond.get(j));
         }

         String conSql = this.groupPathConDSql(list, dimenMap);
         sql = sql + " and " + conSql;
         this.logger.info("anydri非全局主表格获取表格：{}", sql);
         String counts = this.getResultDataByAnyDri(sql, this.df, 0, dataSource);
         String lastCounts = "";
         String rate = "";
         if (dataList != null && dataList.size() > 0) {
            lastCounts = dataList.get(dataList.size() - 1);
            lastCounts = lastCounts.split("\\(")[0];
            if (!"0".equals(counts) && !"0".equals(lastCounts)) {
               rate = this.df3.format(Double.parseDouble(counts) / Double.parseDouble(lastCounts) * 100.0) + "%";
            } else {
               rate = "0.00%";
            }
         } else if ("0".equals(counts)) {
            rate = "0.00%";
         } else {
            rate = "100.00%";
         }

         columnList.add(pathConditonName);
         dataList.add(counts);
         rateList.add(rate);
         count++;
      }

      resultList.add(columnList);
      resultList.add(dataList);
      resultList.add(rateList);
      return resultList;
   }

   private String groupPathConDSql(List<HashMap<String, String>> pathMap, LinkedHashMap<String, DimensionConfig> dimenMap) throws JsonParseException, JsonMappingException, ParseException, IOException {
      String pathSql = "";
      return BuildTopicFilterByAnydri.getPathCond(pathMap, dimenMap);
   }

   private void groupPathConDRequest(RequestData requestData, List<HashMap<String, String>> pathMap, LinkedHashMap<String, DimensionConfig> dimenMap) throws JsonParseException, JsonMappingException, ParseException, IOException {
      BuildTopicFilterBySpark.builPathFilter(pathMap, dimenMap, requestData);
   }

   private void groupInversionCondition(RequestData requestData, String condition, LinkedHashMap<String, DimensionConfig> dimensionMap) throws ViePlatformServiceException {
      if (!StringUtils.isNullOrEmpry(condition)) {
         try {
            if (condition.contains("and;")) {
               String[] arr = condition.split("and;");

               for (int i = 0; i < arr.length; i++) {
                  List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(arr[i], List.class);
                  if (mapList != null) {
                     this.groupPathConDRequest(requestData, mapList, dimensionMap);
                  }
               }
            } else {
               List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(condition, List.class);
               if (mapList != null) {
                  this.groupPathConDRequest(requestData, mapList, dimensionMap);
               }
            }
         } catch (Exception var7) {
            this.logger.error("拼接转化量条件方法出错", var7);
            throw new ViePlatformServiceException("服务器内部错误", var7);
         }
      }
   }

   private void groupLossCondition(RequestData requestData, String condition, LinkedHashMap<String, DimensionConfig> dimensionMap) throws ViePlatformServiceException {
      if (!StringUtils.isNullOrEmpry(condition)) {
         try {
            if (condition.contains("and;")) {
               String[] arr = condition.split("and;");

               for (int i = 0; i < arr.length - 1; i++) {
                  List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(arr[i], List.class);
                  if (mapList != null) {
                     this.groupPathConDRequest(requestData, mapList, dimensionMap);
                  }
               }

               List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(arr[arr.length - 1], List.class);
               if (mapList != null) {
                  this.negationCondition(requestData, mapList, dimensionMap);
               }
            } else {
               List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(condition, List.class);
               if (mapList != null) {
                  this.negationCondition(requestData, mapList, dimensionMap);
               }
            }
         } catch (Exception var7) {
            this.logger.error("拼接流失量条件方法出错", var7);
            throw new ViePlatformServiceException("服务内部错误", var7);
         }
      }
   }

   private String groupInversionCondByAnyDri(String condition, LinkedHashMap<String, DimensionConfig> dimensionMap) throws ViePlatformServiceException {
      String pathCondSql = "";
      if (!StringUtils.isNullOrEmpry(condition)) {
         try {
            if (condition.contains("and;")) {
               String[] arr = condition.split("and;");

               for (int i = 0; i < arr.length; i++) {
                  List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(arr[i], List.class);
                  if (mapList != null) {
                     pathCondSql = pathCondSql + this.groupPathConDSql(mapList, dimensionMap) + " and ";
                  }
               }

               if (pathCondSql.endsWith("and ")) {
                  pathCondSql = pathCondSql.substring(0, pathCondSql.length() - 4);
               }
            } else {
               List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(condition, List.class);
               if (mapList != null) {
                  pathCondSql = pathCondSql + this.groupPathConDSql(mapList, dimensionMap);
               }
            }
         } catch (Exception var7) {
            this.logger.error("anydrill查询拼接转化量条件方法出错", var7);
            throw new ViePlatformServiceException("服务内部错误", var7);
         }
      }

      return pathCondSql;
   }

   private String groupLossCondByAnyDri(String condition, LinkedHashMap<String, DimensionConfig> dimensionMap) throws ViePlatformServiceException {
      String pathCondSql = "";
      if (!StringUtils.isNullOrEmpry(condition)) {
         try {
            if (condition.contains("and;")) {
               String[] arr = condition.split("and;");

               for (int i = 0; i < arr.length - 1; i++) {
                  List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(arr[i], List.class);
                  if (mapList != null) {
                     pathCondSql = pathCondSql + this.groupPathConDSql(mapList, dimensionMap) + " and ";
                  }
               }

               List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(arr[arr.length - 1], List.class);
               if (mapList != null) {
                  pathCondSql = pathCondSql + this.negationCondByAnyDri(mapList, dimensionMap);
               }
            } else {
               List<HashMap<String, String>> mapList = (List<HashMap<String, String>>)this.mapper.readValue(condition, List.class);
               if (mapList != null) {
                  pathCondSql = pathCondSql + this.negationCondByAnyDri(mapList, dimensionMap);
               }
            }
         } catch (Exception var7) {
            this.logger.error("anydrill查询拼接流失量条件方法出错", var7);
            throw new ViePlatformServiceException("服务内部错误", var7);
         }
      }

      return pathCondSql;
   }

   private void negationCondition(RequestData requestData, List<HashMap<String, String>> mapList, LinkedHashMap<String, DimensionConfig> dimensionMap) throws VieAppServiceException {
      if (mapList != null && mapList.size() != 0) {
         try {
            for (HashMap<String, String> hashMap : (List<HashMap<String, String>>)mapList) {
               if (String.valueOf(hashMap.get("isNegate")).equals("1")) {
                  hashMap.put("isNegate", "0");
               } else if (String.valueOf(hashMap.get("isNegate")).equals("0")) {
                  hashMap.put("isNegate", "1");
               }
            }

            this.logger.info(JSON.toJSONString(mapList));
            this.groupPathConDRequest(requestData, mapList, dimensionMap);
         } catch (Exception var6) {
            this.logger.error("组装取非条件方法出错", var6);
            throw new VieAppServiceException("服务内部错误", var6);
         }
      }
   }

   private String negationCondByAnyDri(List<HashMap<String, String>> mapList, LinkedHashMap<String, DimensionConfig> dimensionMap) throws VieAppServiceException {
      String pathCondSql = "";
      if (mapList != null && mapList.size() != 0) {
         try {
            for (HashMap<String, String> hashMap : (List<HashMap<String, String>>)mapList) {
               if (String.valueOf(hashMap.get("isNegate")).equals("1")) {
                  hashMap.put("isNegate", "0");
               } else if (String.valueOf(hashMap.get("isNegate")).equals("0")) {
                  hashMap.put("isNegate", "1");
               }
            }

            this.logger.info(JSON.toJSONString(mapList));
            return this.groupPathConDSql(mapList, dimensionMap);
         } catch (Exception var6) {
            this.logger.error("组装取非条件方法出错", var6);
            throw new VieAppServiceException("服务内部错误", var6);
         }
      } else {
         return pathCondSql;
      }
   }

   public boolean isDurationDimsion(String key, String dataSource, LinkedHashMap<String, DimensionConfig> dimensionMap) {
      boolean result = false;

      try {
         DimensionConfig dimensionConfig = dimensionMap.get(key);
         if (dimensionConfig != null) {
            int flag = dimensionConfig.getFlag();
            if (flag == 1) {
               result = true;
            }
         } else {
            this.logger.error("isDurationDimsion 找不到维度{}", key);
         }
      } catch (Exception var7) {
         this.logger.error("判读维度是否为时长维度出错，维度：{}", key);
      }

      return result;
   }

   private String checkRequestParamNull(FunnelCallRequest funnelCallRequest, List<String> list) {
      if (list != null && list.size() > 0) {
         String key = null;

         for (int i = 0; i < list.size(); i++) {
            key = list.get(i);
            if ("systemId".equals(key)) {
               if (StringUtils.isNullOrEmpry(funnelCallRequest.getSystemId())) {
                  return key;
               }
            } else if ("userId".equals(key)) {
               if (StringUtils.isNullOrEmpry(funnelCallRequest.getUserId())) {
                  return key;
               }
            } else if ("pathId".equals(key)) {
               if (funnelCallRequest.getPathId() == null) {
                  return key;
               }
            } else if ("topicId".equals(key)) {
               if (funnelCallRequest.getTopicId() == null) {
                  return key;
               }
            } else if ("dataSource".equals(key)) {
               if (StringUtils.isNullOrEmpry(funnelCallRequest.getDataSource())) {
                  return key;
               }
            } else if ("allPathFlag".equals(key) && funnelCallRequest.getAllPathFlag() == null) {
               return key;
            }
         }
      }

      return null;
   }

   public ModelInfoDto objectsToModelInfoDto(Object[] objects) {
      ModelInfoDto modelInfoDto = new ModelInfoDto();
      if (objects == null) {
         return modelInfoDto;
      } else {
         modelInfoDto.setModelId(Long.parseLong(String.valueOf(objects[0])));
         modelInfoDto.setModelName(String.valueOf(objects[1]));
         modelInfoDto.setGroupId(Long.parseLong(String.valueOf(objects[2])));
         modelInfoDto.setUpdateTime(String.valueOf(objects[3]));
         modelInfoDto.setModifierId(objects[4] == null ? 0 : Integer.parseInt(String.valueOf(objects[4])));
         modelInfoDto.setModifierName(String.valueOf(objects[5]));
         modelInfoDto.setDataSource(String.valueOf(objects[6]));
         if (objects[7] instanceof Clob) {
            String s = "";
            if (DBSelectUtil.isSqlServer()) {
               s = (String)objects[7];
            } else {
               s = SqlHelper.clobToString((Clob)objects[7]);
            }

            modelInfoDto.setScreeningRule(objects[7] == null ? null : JSON.parseArray(s));
         } else {
            modelInfoDto.setScreeningRule(objects[7] == null ? null : JSON.parseArray(String.valueOf(objects[7])));
         }

         int modelStatuts = -2;
         if (objects[8] != null) {
            modelStatuts = Integer.parseInt(String.valueOf(objects[8]));
         }

         if (modelStatuts == 1) {
            modelStatuts = 2;
         } else if (modelStatuts >= 0 && modelStatuts != 1) {
            modelStatuts = 1;
         }

         modelInfoDto.setModelStatus(modelStatuts);
         return modelInfoDto;
      }
   }

   public TopicPathDB getTopicPathDB() {
      return this.topicPathDB;
   }

   public void setTopicPathDB(TopicPathDB topicPathDB) {
      this.topicPathDB = topicPathDB;
   }

   public TopicDB getTopicDB() {
      return this.topicDB;
   }

   public void setTopicDB(TopicDB topicDB) {
      this.topicDB = topicDB;
   }

   public Log getLog() {
      return this.log;
   }

   public void setLog(Log log) {
      this.log = log;
   }

   public ObjectMapper getMapper() {
      return this.mapper;
   }

   public void setMapper(ObjectMapper mapper) {
      this.mapper = mapper;
   }

   public ModelDB getModelDB() {
      return this.modelDB;
   }

   public void setModelDB(ModelDB modelDB) {
      this.modelDB = modelDB;
   }

   public DecimalFormat getDf3() {
      return this.df3;
   }

   public void setDf3(DecimalFormat df3) {
      this.df3 = df3;
   }

   public DecimalFormat getDf() {
      return this.df;
   }

   public void setDf(DecimalFormat df) {
      this.df = df;
   }

   public TopicMarkDB getTopicMarkDB() {
      return this.topicMarkDB;
   }

   public void setTopicMarkDB(TopicMarkDB topicMarkDB) {
      this.topicMarkDB = topicMarkDB;
   }

   public int getSearchType() {
      return this.searchType;
   }

   public void setSearchType(int searchType) {
      this.searchType = searchType;
   }

   public CommonData getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonData commonData) {
      this.commonData = commonData;
   }

   public SparkUtils getSparkUtils() {
      return this.sparkUtils;
   }

   public void setSparkUtils(SparkUtils sparkUtils) {
      this.sparkUtils = sparkUtils;
   }

   public DataDrillService getDataDrillService() {
      return this.dataDrillService;
   }

   public void setDataDrillService(DataDrillService dataDrillService) {
      this.dataDrillService = dataDrillService;
   }

   public ModelApplyService getModelApplyService() {
      return this.modelApplyService;
   }

   public void setModelApplyService(ModelApplyService modelApplyService) {
      this.modelApplyService = modelApplyService;
   }
}
