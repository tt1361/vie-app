package com.iflytek.vie.app.provider.impl.topicgroup;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.iflytek.vie.app.api.topicgroup.TopicGroupService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.common.PagerResponse;
import com.iflytek.vie.app.pojo.topicgroup.AllCallReadRequest;
import com.iflytek.vie.app.pojo.topicgroup.BusiAndPushRequest;
import com.iflytek.vie.app.pojo.topicgroup.CallCounTrendResponse;
import com.iflytek.vie.app.pojo.topicgroup.CallListResponse;
import com.iflytek.vie.app.pojo.topicgroup.CallReadRequest;
import com.iflytek.vie.app.pojo.topicgroup.ColumnInfo;
import com.iflytek.vie.app.pojo.topicgroup.ColumnMap;
import com.iflytek.vie.app.pojo.topicgroup.DataInfo;
import com.iflytek.vie.app.pojo.topicgroup.PushReadCall;
import com.iflytek.vie.app.pojo.topicgroup.TopicGroup;
import com.iflytek.vie.app.pojo.topicgroup.TopicGroupModel;
import com.iflytek.vie.app.pojo.topicgroup.TopicGroupRequest;
import com.iflytek.vie.app.pojo.topicgroup.TopicModelDTO;
import com.iflytek.vie.app.pojo.topicgroup.YDataResponse;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.PushReadCallDB;
import com.iflytek.vie.app.provider.database.TopicGroupDB;
import com.iflytek.vie.app.provider.database.TopicMarkDB;
import com.iflytek.vie.app.provider.ruleparse.Filter;
import com.iflytek.vie.app.provider.ruleparse.FilterParser;
import com.iflytek.vie.app.provider.ruleparse.PushReadCallProcess;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.pojo.RequestData;
import com.iflytek.vie.pojo.ResponseData;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.ESQueryUtils;
import com.iflytek.vie.utils.LoadCommonConfig;
import com.iflytek.vie.utils.SparkUtils;
import com.iflytek.vie.utils.StringUtils;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicGroupServiceImpl implements TopicGroupService {
   private final Logger logger = LoggerFactory.getLogger(TopicGroupServiceImpl.class);
   private TopicGroupDB topicGroupDB;
   private PushReadCallDB pushReadCallDB;
   private ModelDB modelDB;
   private TopicMarkDB topicMarkDB;
   private CommonData commonData;
   private DataSourceInfo dataSourceInfo;
   private SparkUtils sparkUtils;

   public List<TopicModelDTO> queryBusiOverview(BusiAndPushRequest busiAndPushRequest) throws VieAppServiceException {
      List<TopicModelDTO> groups = null;
      if (busiAndPushRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("systemId");
            keyList.add("userId");
            keyList.add("topicGroupType");
            keyList.add("beginDate");
            keyList.add("endDate");
            keyList.add("dataSource");
            keyList.add("timeThreshold");
            keyList.add("dataTypeDimension");
            String checkResult = this.checkRequestParamNull(busiAndPushRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               int topicGroupType = busiAndPushRequest.getTopicGroupType();
               int settingPage = busiAndPushRequest.getSettingPage();
               List<String> seatGroupList = this.commonData
                  .getDataStringList(busiAndPushRequest.getUserId(), busiAndPushRequest.getSystemId(), busiAndPushRequest.getDataSource());
               if (seatGroupList != null && seatGroupList.size() != 0) {
                  List<Object[]> resultList = this.topicGroupDB.getParentGroupByType(topicGroupType, seatGroupList, settingPage);
                  if (resultList != null && !resultList.isEmpty()) {
                     if (topicGroupType == 1) {
                        Date startTimes = DateUtils.parseStringToDate(busiAndPushRequest.getBeginDate());
                        Date now = new Date();
                        int timeThreshold = busiAndPushRequest.getTimeThreshold();
                        if ((now.getTime() - startTimes.getTime()) / 86400000L > timeThreshold) {
                           this.logger.error("开始时间必须选择{}天以内", timeThreshold);
                           throw new VieAppServiceException("开始时间必须选择" + timeThreshold + "天以内");
                        }
                     }

                     return this.getTopicGroups(resultList, seatGroupList, busiAndPushRequest);
                  } else {
                     this.logger.error("当前数据为空，请先配置专题组！");
                     throw new VieAppServiceException("当前数据为空，请先配置专题组！");
                  }
               } else {
                  this.logger.error("获取数据权限失败");
                  throw new VieAppServiceException("获取数据权限失败");
               }
            }
         } catch (VieAppServiceException var12) {
            this.logger.error(var12.getMessage());
            throw new VieAppServiceException(var12.getMessage());
         } catch (Exception var13) {
            this.logger.error("queryBusiOverview service has a error!", var13);
            throw new VieAppServiceException("服务内部错误", var13);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public CallCounTrendResponse getCallCountTrend(BusiAndPushRequest busiAndPushRequest) throws VieAppServiceException {
      CallCounTrendResponse callCounTrendResponse = null;
      if (busiAndPushRequest == null) {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      } else {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("id");
            keyList.add("modelOrGroup");
            keyList.add("timeType");
            keyList.add("beginDate");
            keyList.add("endDate");
            keyList.add("dataSource");
            String checkResult = this.checkRequestParamNull(busiAndPushRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               String modelIdStr = this.getModelIdStr(busiAndPushRequest.getId(), busiAndPushRequest.getModelOrGroup());
               long startTemp = this.getStartDateTime(busiAndPushRequest.getBeginDate());
               long endTemp = this.getEndDateTime(busiAndPushRequest.getEndDate());
               String batchId = StringUtils.isNullOrEmpry(busiAndPushRequest.getBatchId()) ? "0" : busiAndPushRequest.getBatchId();
               int dataType = busiAndPushRequest.getDataType() == null ? 2 : busiAndPushRequest.getDataType();
               Integer ifLone = busiAndPushRequest.getIfLone();
               String getIdSql = "select id from "
                  + IndexConstants.getModelTableName(busiAndPushRequest.getDataSource())
                  + " where timestamp between "
                  + startTemp
                  + " and "
                  + endTemp
                  + " and "
                  + MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType)
                  + " and processed = 0 and  offLineTagId in ("
                  + modelIdStr
                  + ") group by id";
               this.logger.info("getIdSql:{}", getIdSql);
               String[] anydrillAddressModel = LoadCommonConfig.get("modelAnydrillAddress").split(";");
               PreparedStatement ps = new PreparedStatement(anydrillAddressModel);
               ResultSet rs = ps.executeQuery(getIdSql);
               StringBuffer idList = new StringBuffer();
               if (rs != null && rs.getTotalCount() > 0L) {
                  for (LinkedHashMap<String, Object> linkedHashMap : rs.toList()) {
                     idList.append("'").append(linkedHashMap.get("id").toString()).append("',");
                  }

                  idList.delete(idList.length() - 1, idList.length());
               } else {
                  idList.append("'null'");
               }

               String sql = "";
               if ("hour".equals(busiAndPushRequest.getTimeType())) {
                  sql = "select dimHour as calltime , sum(idCounter) as callCount  from "
                     + busiAndPushRequest.getDataSource()
                     + " where id in ("
                     + idList
                     + ") group by dimHour"
                     + " order by dimHour asc";
               } else {
                  sql = "select dimDay as calltime , sum(idCounter) as callCount  from "
                     + busiAndPushRequest.getDataSource()
                     + " where id in ("
                     + idList
                     + ") group by dimDay"
                     + " order by dimDay asc";
               }

               this.logger.info("sql:{}", sql);
               return this.getCallCounTrend(sql, busiAndPushRequest.getBeginDate(), busiAndPushRequest.getEndDate(), busiAndPushRequest.getTimeType());
            }
         } catch (VieAppServiceException var20) {
            this.logger.error(var20.getMessage());
            throw new VieAppServiceException(var20.getMessage());
         } catch (Exception var21) {
            this.logger.error("getCallCountTrend service has a error!", var21);
            throw new VieAppServiceException("服务内部错误", var21);
         }
      }
   }

   public CallListResponse queryCallList(BusiAndPushRequest busiAndPushRequest) throws VieAppServiceException {
      CallListResponse callListResponse = new CallListResponse();
      if (busiAndPushRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("id");
            keyList.add("modelOrGroup");
            keyList.add("beginDate");
            keyList.add("endDate");
            keyList.add("dataSource");
            keyList.add("dataTypeDimension");
            String checkResult = this.checkRequestParamNull(busiAndPushRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               String sortColumn = busiAndPushRequest.getSortColumn();
               if ("modelName".equals(sortColumn) || "mark".equals(sortColumn) || "keyword".equals(sortColumn)) {
                  sortColumn = "id";
               }

               ColumnInfo columnInfo = this.getColumnMaps(busiAndPushRequest.getSearchDimension());
               FilterParser filterParser = this.getFilterParser(busiAndPushRequest.getBeginDate(), busiAndPushRequest.getEndDate());
               List<Long> modelIdList = this.getModelIdList(busiAndPushRequest.getId(), 0, busiAndPushRequest.getModelOrGroup());
               int pageSize = busiAndPushRequest.getPageSize();
               int pageNum = busiAndPushRequest.getPageNum();
               String batchId = StringUtils.isNullOrEmpry(busiAndPushRequest.getBatchId()) ? "0" : busiAndPushRequest.getBatchId();
               int dataType = busiAndPushRequest.getDataType() == null ? 2 : busiAndPushRequest.getDataType();
               Integer ifLone = busiAndPushRequest.getIfLone();
               PushReadCallProcess topicProcess = new PushReadCallProcess(
                  filterParser.getFilters(),
                  busiAndPushRequest.getDataSource(),
                  pageSize * (pageNum - 1) + 1,
                  pageNum * pageSize,
                  sortColumn,
                  busiAndPushRequest.getSortType(),
                  columnInfo.getColumnsStr().replace("[", "").replace("]", ""),
                  this.modelDB,
                  this.topicMarkDB,
                  null,
                  modelIdList,
                  -1,
                  null,
                  0,
                  null,
                  busiAndPushRequest.getDataTypeDimension(),
                  MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType)
               );
               topicProcess.process();
               callListResponse.setTotalCount(topicProcess.getDimCount());
               callListResponse.setColumns(columnInfo.getColumnMaps());
               PagerResponse<DataInfo> pagerResponse = new PagerResponse();
               this.setPagerInfo(pagerResponse, callListResponse.getTotalCount(), topicProcess.getDataInfos(), pageNum, pageSize);
               callListResponse.setPreviewList(pagerResponse);
               return callListResponse;
            }
         } catch (VieAppServiceException var16) {
            this.logger.error(var16.getMessage());
            throw new VieAppServiceException(var16.getMessage());
         } catch (Exception var17) {
            this.logger.error("queryCallList service has a error!", var17);
            throw new VieAppServiceException("服务内部错误", var17);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public int getUnReadCallCount(BusiAndPushRequest busiAndPushRequest) throws VieAppServiceException {
      int unReadCallCount = 0;
      if (busiAndPushRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("systemId");
            keyList.add("userId");
            keyList.add("beginDate");
            keyList.add("endDate");
            keyList.add("dataSource");
            keyList.add("dataTypeDimension");
            String checkResult = this.checkRequestParamNull(busiAndPushRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               List<String> seatGroupList = this.commonData
                  .getDataStringList(busiAndPushRequest.getUserId(), busiAndPushRequest.getSystemId(), busiAndPushRequest.getDataSource());
               if (seatGroupList != null && seatGroupList.size() != 0) {
                  List<Object[]> resultList = this.topicGroupDB.getParentGroupByType(1, seatGroupList, 0);
                  if (resultList != null && !resultList.isEmpty()) {
                     Date startTime = DateUtils.parseStringToDate(busiAndPushRequest.getBeginDate());
                     Date endTime = DateUtils.parseStringToDate(busiAndPushRequest.getEndDate());
                     int ifLone = busiAndPushRequest.getIfLone() == null ? 0 : busiAndPushRequest.getIfLone();
                     String batchId = StringUtils.isNullOrEmpry(busiAndPushRequest.getBatchId()) ? "0" : busiAndPushRequest.getBatchId();
                     int dataType = busiAndPushRequest.getDataType() == null ? 2 : busiAndPushRequest.getDataType();

                     for (int i = 0; i < resultList.size(); i++) {
                        try {
                           Object[] objs = resultList.get(i);
                           Long groupId = ((BigDecimal)objs[0]).longValue();
                           List<Object[]> groupModelList = this.topicGroupDB.getGroupModelList(groupId, seatGroupList);
                           Map<String, String> groupNameModelMap = this.convertToNameMap(groupModelList);
                           Long callCount = 0L;
                           if (groupNameModelMap != null && !groupNameModelMap.isEmpty()) {
                              callCount = this.getCallCount(
                                 null,
                                 null,
                                 1,
                                 groupNameModelMap,
                                 busiAndPushRequest.getBeginDate(),
                                 busiAndPushRequest.getEndDate(),
                                 busiAndPushRequest.getDataSource(),
                                 busiAndPushRequest.getDataTypeDimension(),
                                 ifLone,
                                 batchId,
                                 dataType
                              );
                           }

                           if (callCount > 0L) {
                              Map<Long, String> groupIdModelMap = this.convertToIdMap(groupModelList);
                              long readCallNum = this.pushReadCallDB
                                 .getCallIdCount(1, null, null, startTime, endTime, groupIdModelMap, ifLone, batchId, dataType);
                              if (readCallNum == 0L) {
                                 unReadCallCount = (int)(unReadCallCount + callCount);
                              } else {
                                 unReadCallCount = (int)(unReadCallCount + this.getUnReadCallNum(callCount, readCallNum));
                              }
                           }
                        } catch (Exception var21) {
                           this.logger.error("getUnReadCallCount service has a error!", var21);
                        }
                     }

                     return unReadCallCount;
                  } else {
                     return 0;
                  }
               } else {
                  this.logger.error("获取数据权限失败");
                  throw new VieAppServiceException("获取数据权限失败");
               }
            }
         } catch (VieAppServiceException var22) {
            this.logger.error(var22.getMessage());
            throw new VieAppServiceException(var22.getMessage());
         } catch (Exception var23) {
            this.logger.error("getUnReadCallCount service has a error!", var23);
            throw new VieAppServiceException("服务内部错误", var23);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public CallListResponse queryPushCallList(BusiAndPushRequest busiAndPushRequest) throws VieAppServiceException {
      CallListResponse callListResponse = new CallListResponse();
      PagerResponse<DataInfo> pagerResponse = new PagerResponse();
      if (busiAndPushRequest == null) {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      } else {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("id");
            keyList.add("systemId");
            keyList.add("userId");
            keyList.add("modelOrGroup");
            keyList.add("beginDate");
            keyList.add("endDate");
            keyList.add("dataSource");
            keyList.add("timeThreshold");
            keyList.add("dataTypeDimension");
            String checkResult = this.checkRequestParamNull(busiAndPushRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               Long id = busiAndPushRequest.getId();
               Long parentGroupId = busiAndPushRequest.getParentGroupId();
               int pageSize = busiAndPushRequest.getPageSize();
               int pageNum = busiAndPushRequest.getPageNum();
               int modelOrGroup = busiAndPushRequest.getModelOrGroup();
               int ifLone = busiAndPushRequest.getIfLone() == null ? 0 : busiAndPushRequest.getIfLone();
               String batchId = StringUtils.isNullOrEmpry(busiAndPushRequest.getBatchId()) ? "0" : busiAndPushRequest.getBatchId();
               int dataType = busiAndPushRequest.getDataType() == null ? 2 : busiAndPushRequest.getDataType();
               Date startTime = DateUtils.parseStringToDate(busiAndPushRequest.getBeginDate());
               Date endTime = DateUtils.parseStringToDate(busiAndPushRequest.getEndDate());
               Date now = new Date();
               int timeThreshold = busiAndPushRequest.getTimeThreshold();
               if ((now.getTime() - startTime.getTime()) / 86400000L > timeThreshold) {
                  this.logger.error("开始时间必须选择{}天以内", timeThreshold);
                  throw new VieAppServiceException("开始时间必须选择" + timeThreshold + "天以内");
               } else {
                  Long callCount = 0L;
                  Long unReadCallCount = 0L;
                  Map<Long, String> mapId = null;
                  Map<String, String> groupNameModelMap = new HashMap<>();
                  if (busiAndPushRequest.getModelOrGroup() == 0) {
                     if (parentGroupId == null) {
                        this.logger.error("参数(parentGroupId)为空");
                        throw new VieAppServiceException("参数(parentGroupId)为空");
                     }

                     TopicGroup topicGroup = this.topicGroupDB.getEntityById(parentGroupId);
                     if (topicGroup == null) {
                        this.logger.error("通过专题组id无法获取该专题组实体");
                        throw new VieAppServiceException("通过专题组id无法获取该专题组实体");
                     }

                     groupNameModelMap.put(topicGroup.getGroupName(), id.toString());
                     callCount = this.getCallCount(
                        null,
                        null,
                        1,
                        groupNameModelMap,
                        busiAndPushRequest.getBeginDate(),
                        busiAndPushRequest.getEndDate(),
                        busiAndPushRequest.getDataSource(),
                        busiAndPushRequest.getDataTypeDimension(),
                        ifLone,
                        batchId,
                        dataType
                     );
                     if (callCount > 0L) {
                        mapId = new HashMap<>();
                        mapId.put(parentGroupId, id.toString());
                        long readCallNum = this.pushReadCallDB.getCallIdCount(0, id, parentGroupId, startTime, endTime, mapId, ifLone, batchId, dataType);
                        if (readCallNum == 0L) {
                           unReadCallCount = callCount;
                        } else {
                           unReadCallCount = this.getUnReadCallNum(callCount, readCallNum);
                        }
                     }
                  } else {
                     List<String> seatGroupList = this.commonData
                        .getDataStringList(busiAndPushRequest.getUserId(), busiAndPushRequest.getSystemId(), busiAndPushRequest.getDataSource());
                     if (seatGroupList == null || seatGroupList.size() == 0) {
                        this.logger.error("获取数据权限失败");
                        throw new VieAppServiceException("获取数据权限失败");
                     }

                     List<Object[]> groupModelList = this.topicGroupDB.getGroupModelList(id, seatGroupList);
                     groupNameModelMap = this.convertToNameMap(groupModelList);
                     if (groupNameModelMap != null && !groupNameModelMap.isEmpty()) {
                        callCount = this.getCallCount(
                           null,
                           null,
                           1,
                           groupNameModelMap,
                           busiAndPushRequest.getBeginDate(),
                           busiAndPushRequest.getEndDate(),
                           busiAndPushRequest.getDataSource(),
                           busiAndPushRequest.getDataTypeDimension(),
                           ifLone,
                           batchId,
                           dataType
                        );
                     }

                     if (callCount > 0L) {
                        mapId = this.convertToIdMap(groupModelList);
                        long readCallNum = this.pushReadCallDB.getCallIdCount(1, null, null, startTime, endTime, mapId, ifLone, batchId, dataType);
                        if (readCallNum == 0L) {
                           unReadCallCount = callCount;
                        } else {
                           unReadCallCount = this.getUnReadCallNum(callCount, readCallNum);
                        }
                     }
                  }

                  ColumnInfo columnInfo = this.getColumnMaps(busiAndPushRequest.getSearchDimension());
                  callListResponse.setColumns(columnInfo.getColumnMaps());
                  callListResponse.setTotalCount(callCount.intValue());
                  if (callCount == 0L) {
                     this.setPagerInfo(pagerResponse, callListResponse.getTotalCount(), null, pageNum, pageSize);
                     callListResponse.setPreviewList(pagerResponse);
                     return callListResponse;
                  } else {
                     FilterParser filterParser = this.getFilterParser(busiAndPushRequest.getBeginDate(), busiAndPushRequest.getEndDate());
                     List<Long> modelIdList = this.getModelIdList(id, 1, modelOrGroup);
                     Map<Long, List<Long>> modelGroupMap = this.getModelGroupMap(id, modelOrGroup, parentGroupId);
                     if (unReadCallCount != 0L && unReadCallCount != callCount) {
                        List<String> callIdList = null;
                        if (busiAndPushRequest.getModelOrGroup() == 0) {
                           callIdList = this.pushReadCallDB.getCallIdList(0, id, parentGroupId, startTime, endTime, mapId, ifLone, batchId, dataType);
                        } else {
                           callIdList = this.pushReadCallDB.getCallIdList(1, null, null, startTime, endTime, mapId, ifLone, batchId, dataType);
                        }

                        if (unReadCallCount >= pageNum * pageSize) {
                           PushReadCallProcess topicProcess = new PushReadCallProcess(
                              filterParser.getFilters(),
                              busiAndPushRequest.getDataSource(),
                              (pageNum - 1) * pageSize + 1,
                              pageNum * pageSize,
                              "id",
                              "asc",
                              columnInfo.getColumnsStr().replace("[", "").replace("]", ""),
                              this.modelDB,
                              this.topicMarkDB,
                              callIdList,
                              modelIdList,
                              0,
                              modelGroupMap,
                              1,
                              groupNameModelMap,
                              busiAndPushRequest.getDataTypeDimension(),
                              MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType)
                           );
                           topicProcess.process();
                           this.setPagerInfo(pagerResponse, callListResponse.getTotalCount(), topicProcess.getDataInfos(), pageNum, pageSize);
                        } else if ((pageNum * pageSize - unReadCallCount) / pageSize > 0L) {
                           PushReadCallProcess topicProcess = new PushReadCallProcess(
                              filterParser.getFilters(),
                              busiAndPushRequest.getDataSource(),
                              (pageNum - 1) * pageSize - unReadCallCount.intValue() + 1,
                              pageNum * pageSize - unReadCallCount.intValue(),
                              "id",
                              "asc",
                              columnInfo.getColumnsStr().replace("[", "").replace("]", ""),
                              this.modelDB,
                              this.topicMarkDB,
                              callIdList,
                              modelIdList,
                              1,
                              modelGroupMap,
                              1,
                              groupNameModelMap,
                              busiAndPushRequest.getDataTypeDimension(),
                              MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType)
                           );
                           topicProcess.process();
                           this.setPagerInfo(pagerResponse, callListResponse.getTotalCount(), topicProcess.getDataInfos(), pageNum, pageSize);
                        } else {
                           PushReadCallProcess topicProcess = new PushReadCallProcess(
                              filterParser.getFilters(),
                              busiAndPushRequest.getDataSource(),
                              (pageNum - 1) * pageSize + 1,
                              unReadCallCount.intValue(),
                              "id",
                              "asc",
                              columnInfo.getColumnsStr().replace("[", "").replace("]", ""),
                              this.modelDB,
                              this.topicMarkDB,
                              callIdList,
                              modelIdList,
                              0,
                              modelGroupMap,
                              1,
                              groupNameModelMap,
                              busiAndPushRequest.getDataTypeDimension(),
                              MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType)
                           );
                           topicProcess.process();
                           PushReadCallProcess topicProcess2 = new PushReadCallProcess(
                              filterParser.getFilters(),
                              busiAndPushRequest.getDataSource(),
                              1,
                              pageNum * pageSize - unReadCallCount.intValue(),
                              "id",
                              "asc",
                              columnInfo.getColumnsStr().replace("[", "").replace("]", ""),
                              this.modelDB,
                              this.topicMarkDB,
                              callIdList,
                              modelIdList,
                              1,
                              modelGroupMap,
                              1,
                              groupNameModelMap,
                              busiAndPushRequest.getDataTypeDimension(),
                              MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType)
                           );
                           topicProcess2.process();
                           List<DataInfo> list = topicProcess.getDataInfos();
                           List<DataInfo> list2 = topicProcess2.getDataInfos();
                           list.addAll(list2);
                           this.setPagerInfo(pagerResponse, callListResponse.getTotalCount(), list, pageNum, pageSize);
                        }

                        callListResponse.setPreviewList(pagerResponse);
                        return callListResponse;
                     } else {
                        PushReadCallProcess topicProcess = new PushReadCallProcess(
                           filterParser.getFilters(),
                           busiAndPushRequest.getDataSource(),
                           (pageNum - 1) * pageSize + 1,
                           pageNum * pageSize,
                           "id",
                           "asc",
                           columnInfo.getColumnsStr().replace("[", "").replace("]", ""),
                           this.modelDB,
                           this.topicMarkDB,
                           null,
                           modelIdList,
                           unReadCallCount == 0L ? 1 : 0,
                           modelGroupMap,
                           1,
                           groupNameModelMap,
                           busiAndPushRequest.getDataTypeDimension(),
                           MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType)
                        );
                        topicProcess.process();
                        this.setPagerInfo(pagerResponse, callListResponse.getTotalCount(), topicProcess.getDataInfos(), pageNum, pageSize);
                        callListResponse.setPreviewList(pagerResponse);
                        return callListResponse;
                     }
                  }
               }
            }
         } catch (VieAppServiceException var31) {
            this.logger.error(var31.getMessage());
            throw new VieAppServiceException(var31.getMessage());
         } catch (Exception var32) {
            this.logger.error("queryPushCallList service has a error!", var32);
            throw new VieAppServiceException("服务内部错误", var32);
         }
      }
   }

   public boolean updateCallRead(CallReadRequest callReadRequest) throws VieAppServiceException {
      boolean resultFlag = true;
      if (callReadRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("modelIdListStr");
            keyList.add("callId");
            keyList.add("callTime");
            String checkResult = this.checkRequestParamNull(callReadRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               int ifLone = callReadRequest.getIfLone() == null ? 0 : callReadRequest.getIfLone();
               String batchId = StringUtils.isNullOrEmpry(callReadRequest.getBatchId()) ? "0" : callReadRequest.getBatchId();
               int dataType = callReadRequest.getDataType() == null ? 2 : callReadRequest.getDataType();
               String[] modelGroups = callReadRequest.getModelIdListStr().split("\\|");
               SimpleDateFormat sdff = new SimpleDateFormat("yyyyMMdd");
               List<PushReadCall> calls = new ArrayList<>();
               PushReadCall readCall = null;

               for (String modelGroup : modelGroups) {
                  String[] modelGroupSplit = modelGroup.split(",");
                  if (modelGroupSplit[0] != null && modelGroupSplit[1] != null) {
                     Long modelId = Long.valueOf(modelGroupSplit[0]);
                     Long groupId = Long.valueOf(modelGroupSplit[1]);
                     readCall = new PushReadCall();
                     readCall.setGroupId(groupId);
                     readCall.setModelId(modelId);
                     readCall.setCallId(callReadRequest.getCallId());
                     readCall.setCallTime(sdff.parse(callReadRequest.getCallTime()));
                     readCall.setDeleted(0);
                     readCall.setIfLone(ifLone);
                     readCall.setBatchId(batchId);
                     readCall.setDataType(dataType);
                     calls.add(readCall);
                  }
               }

               this.pushReadCallDB.savePushReadCalls(calls);
               return resultFlag;
            }
         } catch (VieAppServiceException var19) {
            this.logger.error(var19.getMessage());
            throw new VieAppServiceException(var19.getMessage());
         } catch (Exception var20) {
            this.logger.error("updateCallRead service has a error!", var20);
            throw new VieAppServiceException("服务内部错误", var20);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public boolean updateAllCallRead(AllCallReadRequest allCallReadRequest) throws VieAppServiceException {
      boolean resultFlag = true;
      if (allCallReadRequest == null) {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      } else {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("id");
            keyList.add("systemId");
            keyList.add("userId");
            keyList.add("modelOrGroup");
            keyList.add("beginDate");
            keyList.add("endDate");
            keyList.add("dataSource");
            keyList.add("dataTypeDimension");
            String checkResult = this.checkRequestParamNull(allCallReadRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               Long id = allCallReadRequest.getId();
               int modelOrGroup = allCallReadRequest.getModelOrGroup();
               Long parentGroupId = allCallReadRequest.getParentGroupId();
               Date startTime = DateUtils.parseStringToDate(allCallReadRequest.getBeginDate());
               Date endTime = DateUtils.parseStringToDate(allCallReadRequest.getEndDate());
               int ifLone = allCallReadRequest.getIfLone() == null ? 0 : allCallReadRequest.getIfLone();
               String batchId = StringUtils.isNullOrEmpry(allCallReadRequest.getBatchId()) ? "0" : allCallReadRequest.getBatchId();
               int dataType = allCallReadRequest.getDataType() == null ? 2 : allCallReadRequest.getDataType();
               Long callCount = 0L;
               Long unReadCallCount = 0L;
               Map<String, String> groupNameModelMap = new HashMap<>();
               Map<Long, String> mapId = null;
               if (modelOrGroup == 0) {
                  if (parentGroupId == null) {
                     this.logger.error("参数(parentGroupId)为空");
                     throw new VieAppServiceException("参数(parentGroupId)为空");
                  }

                  TopicGroup topicGroup = this.topicGroupDB.getEntityById(allCallReadRequest.getParentGroupId());
                  if (topicGroup == null) {
                     this.logger.error("通过专题组id无法获取该专题组实体");
                     throw new VieAppServiceException("通过专题组id无法获取该专题组实体");
                  }

                  groupNameModelMap.put(topicGroup.getGroupName(), id.toString());
                  callCount = this.getCallCount(
                     null,
                     null,
                     1,
                     groupNameModelMap,
                     allCallReadRequest.getBeginDate(),
                     allCallReadRequest.getEndDate(),
                     allCallReadRequest.getDataSource(),
                     allCallReadRequest.getDataTypeDimension(),
                     ifLone,
                     batchId,
                     dataType
                  );
                  if (callCount > 0L) {
                     mapId = new HashMap<>();
                     mapId.put(parentGroupId, id.toString());
                     long readCallNum = this.pushReadCallDB.getCallIdCount(0, id, parentGroupId, startTime, endTime, mapId, ifLone, batchId, dataType);
                     if (readCallNum == 0L) {
                        unReadCallCount = callCount;
                     } else {
                        unReadCallCount = this.getUnReadCallNum(callCount, readCallNum);
                     }
                  }
               } else {
                  List<String> seatGroupList = this.commonData
                     .getDataStringList(allCallReadRequest.getUserId(), allCallReadRequest.getSystemId(), allCallReadRequest.getDataSource());
                  if (seatGroupList == null || seatGroupList.size() == 0) {
                     this.logger.error("获取数据权限失败");
                     throw new VieAppServiceException("获取数据权限失败");
                  }

                  List<Object[]> groupModelList = this.topicGroupDB.getGroupModelList(id, seatGroupList);
                  groupNameModelMap = this.convertToNameMap(groupModelList);
                  if (groupNameModelMap != null && !groupNameModelMap.isEmpty()) {
                     callCount = this.getCallCount(
                        null,
                        null,
                        1,
                        groupNameModelMap,
                        allCallReadRequest.getBeginDate(),
                        allCallReadRequest.getEndDate(),
                        allCallReadRequest.getDataSource(),
                        allCallReadRequest.getDataTypeDimension(),
                        ifLone,
                        batchId,
                        dataType
                     );
                  }

                  if (callCount > 0L) {
                     mapId = this.convertToIdMap(groupModelList);
                     long readCallNum = this.pushReadCallDB.getCallIdCount(1, null, null, startTime, endTime, mapId, ifLone, batchId, dataType);
                     if (readCallNum == 0L) {
                        unReadCallCount = callCount;
                     } else {
                        unReadCallCount = this.getUnReadCallNum(callCount, readCallNum);
                     }
                  }
               }

               if (callCount != 0L && unReadCallCount != 0L) {
                  List<LinkedHashMap<String, Object>> result = this.getTotalResultList(allCallReadRequest, callCount, groupNameModelMap);
                  long start = System.currentTimeMillis();
                  List<PushReadCall> readCalls = new ArrayList<>();
                  if (result != null && result.size() > 0) {
                     List<String> callIdList = null;
                     if (modelOrGroup == 0) {
                        callIdList = this.pushReadCallDB.getCallIdList(0, id, parentGroupId, startTime, endTime, mapId, ifLone, batchId, dataType);
                     } else {
                        callIdList = this.pushReadCallDB.getCallIdList(1, null, null, startTime, endTime, mapId, ifLone, batchId, dataType);
                     }

                     SimpleDateFormat sdff = new SimpleDateFormat("yyyyMMdd");
                     Map<Long, List<Long>> modelGroupMap = this.getModelGroupMap(id, modelOrGroup, parentGroupId);
                     Map<String, String> callIdMap = this.changeListToMap(callIdList);

                     for (LinkedHashMap<String, Object> hashMap : result) {
                        String callId = String.valueOf(hashMap.get("id"));
                        long modelIdd = Long.parseLong(hashMap.get("offLineTagId").toString());
                        List<Long> groups = modelGroupMap.get(modelIdd);
                        if (groups != null && !groups.isEmpty()) {
                           for (Long group : groups) {
                              if (!callIdMap.containsKey(callId)) {
                                 PushReadCall readCall = new PushReadCall();
                                 readCall.setModelId(modelIdd);
                                 readCall.setGroupId(group);
                                 readCall.setCallId(callId);
                                 readCall.setCallTime(sdff.parse((String)hashMap.get("dimDay")));
                                 readCall.setDeleted(0);
                                 readCall.setIfLone(ifLone);
                                 readCall.setBatchId(batchId);
                                 readCall.setDataType(dataType);
                                 readCalls.add(readCall);
                              }
                           }
                        }
                     }

                     long end = System.currentTimeMillis();
                     this.logger.info("处理数据：{}条； 共耗时：{}毫秒", result.size(), end - start);
                     this.pushReadCallDB.savePushReadCalls(readCalls);
                  }

                  return resultFlag;
               } else {
                  return resultFlag;
               }
            }
         } catch (VieAppServiceException var34) {
            this.logger.error(var34.getMessage());
            throw new VieAppServiceException(var34.getMessage());
         } catch (Exception var35) {
            this.logger.error("updateAllCallRead service has a error!", var35);
            throw new VieAppServiceException("服务内部错误", var35);
         }
      }
   }

   public List<TopicModelDTO> queryAllGroupDatas(AllCallReadRequest allCallReadRequest) throws VieAppServiceException {
      List<TopicModelDTO> resultList = new ArrayList<>();
      if (allCallReadRequest == null) {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      } else {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("systemId");
            keyList.add("userId");
            keyList.add("parentGroupId");
            keyList.add("beginDate");
            keyList.add("endDate");
            keyList.add("dataSource");
            keyList.add("dataTypeDimension");
            String checkResult = this.checkRequestParamNull(allCallReadRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               Long parentGroupId = allCallReadRequest.getParentGroupId();
               int ifLone = allCallReadRequest.getIfLone() == null ? 0 : allCallReadRequest.getIfLone();
               String batchId = StringUtils.isNullOrEmpry(allCallReadRequest.getBatchId()) ? "0" : allCallReadRequest.getBatchId();
               int dataType = allCallReadRequest.getDataType() == null ? 2 : allCallReadRequest.getDataType();
               Date startTime = DateUtils.parseStringToDate(allCallReadRequest.getBeginDate());
               Date endTime = DateUtils.parseStringToDate(allCallReadRequest.getEndDate());
               TopicModelDTO group = new TopicModelDTO();
               group.setModelOrGroup(1);
               group.setModelId(parentGroupId);
               List<String> seatGroupList = this.commonData
                  .getDataStringList(allCallReadRequest.getUserId(), allCallReadRequest.getSystemId(), allCallReadRequest.getDataSource());
               if (seatGroupList != null && seatGroupList.size() != 0) {
                  List<Object[]> groupModelList = this.topicGroupDB.getGroupModelList(parentGroupId, seatGroupList);
                  Map<String, String> groupNameModelMap = this.convertToNameMap(groupModelList);
                  Long callCount = 0L;
                  if (groupModelList != null && !groupModelList.isEmpty()) {
                     callCount = this.getCallCount(
                        null,
                        null,
                        1,
                        groupNameModelMap,
                        allCallReadRequest.getBeginDate(),
                        allCallReadRequest.getEndDate(),
                        allCallReadRequest.getDataSource(),
                        allCallReadRequest.getDataTypeDimension(),
                        ifLone,
                        batchId,
                        dataType
                     );
                  }

                  if (callCount == 0L) {
                     group.setUnReadCall(0L);
                     group.setCallCount(0L);
                  } else {
                     Map<Long, String> groupIdModelMap = this.convertToIdMap(groupModelList);
                     long readCallNum = this.pushReadCallDB.getCallIdCount(1, null, null, startTime, endTime, groupIdModelMap, ifLone, batchId, dataType);
                     if (readCallNum == 0L) {
                        group.setUnReadCall(callCount);
                     } else {
                        group.setUnReadCall(this.getUnReadCallNum(callCount, readCallNum));
                     }

                     group.setCallCount(callCount);
                  }

                  resultList.add(group);

                  for (TopicModelDTO topicModelDTO : this.getModels(parentGroupId, startTime, endTime, seatGroupList, allCallReadRequest)) {
                     resultList.add(topicModelDTO);
                     if (topicModelDTO.getModelOrGroup() == 1) {
                        for (TopicModelDTO topicModelDTO2 : this.getModels(topicModelDTO.getModelId(), startTime, endTime, seatGroupList, allCallReadRequest)) {
                           resultList.add(topicModelDTO2);
                           if (topicModelDTO2.getModelOrGroup() == 1) {
                              for (TopicModelDTO topicModelDTO3 : this.getModels(
                                 topicModelDTO2.getModelId(), startTime, endTime, seatGroupList, allCallReadRequest
                              )) {
                                 resultList.add(topicModelDTO3);
                                 if (topicModelDTO3.getModelOrGroup() == 1) {
                                    for (TopicModelDTO topicModelDTO4 : this.getModels(
                                       topicModelDTO3.getModelId(), startTime, endTime, seatGroupList, allCallReadRequest
                                    )) {
                                       resultList.add(topicModelDTO4);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }

                  return resultList;
               } else {
                  this.logger.error("获取数据权限失败");
                  throw new VieAppServiceException("获取数据权限失败");
               }
            }
         } catch (VieAppServiceException var28) {
            this.logger.error(var28.getMessage());
            throw new VieAppServiceException(var28.getMessage());
         } catch (Exception var29) {
            this.logger.error("queryAllGroupDatas service has a error!", var29);
            throw new VieAppServiceException("服务内部错误", var29);
         }
      }
   }

   public List<TopicModelDTO> queryChildrenGroup(BusiAndPushRequest busiAndPushRequest) throws VieAppServiceException {
      List<TopicModelDTO> resultList = new ArrayList<>();
      if (busiAndPushRequest == null) {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      } else {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("id");
            keyList.add("systemId");
            keyList.add("userId");
            keyList.add("topicGroupType");
            keyList.add("beginDate");
            keyList.add("endDate");
            keyList.add("dataSource");
            keyList.add("dataSource");
            keyList.add("dataTypeDimension");
            String checkResult = this.checkRequestParamNull(busiAndPushRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               Long id = busiAndPushRequest.getId();
               int topicGroupType = busiAndPushRequest.getTopicGroupType();
               int settingPage = busiAndPushRequest.getSettingPage();
               Date startTime = DateUtils.parseStringToDate(busiAndPushRequest.getBeginDate());
               Date endTime = DateUtils.parseStringToDate(busiAndPushRequest.getEndDate());
               int ifLone = busiAndPushRequest.getIfLone() == null ? 0 : busiAndPushRequest.getIfLone();
               String batchId = StringUtils.isNullOrEmpry(busiAndPushRequest.getBatchId()) ? "0" : busiAndPushRequest.getBatchId();
               int dataType = busiAndPushRequest.getDataType() == null ? 2 : busiAndPushRequest.getDataType();
               TopicModelDTO model = null;
               Long callCountAll = 0L;
               List<String> seatGroupList = this.commonData
                  .getDataStringList(busiAndPushRequest.getUserId(), busiAndPushRequest.getSystemId(), busiAndPushRequest.getDataSource());
               if (seatGroupList != null && seatGroupList.size() != 0) {
                  List<Object[]> childrenGroup = this.topicGroupDB.queryChildrenGroup(id, topicGroupType, seatGroupList, settingPage);
                  if (childrenGroup != null && !childrenGroup.isEmpty()) {
                     for (Object[] objs : childrenGroup) {
                        try {
                           Long modelId = ((BigDecimal)objs[0]).longValue();
                           String modelName = String.valueOf(objs[1]);
                           model = new TopicModelDTO();
                           model.setModelId(modelId);
                           model.setModelName(modelName);
                           model.setModelOrGroup(1);
                           if (settingPage == 1) {
                              resultList.add(model);
                           } else {
                              if (topicGroupType == 1) {
                                 List<Object[]> groupModelList = this.topicGroupDB.getGroupModelList(modelId, seatGroupList);
                                 Map<String, String> groupNameModelMap = this.convertToNameMap(groupModelList);
                                 Long callCount = 0L;
                                 if (groupNameModelMap != null && !groupNameModelMap.isEmpty()) {
                                    callCount = this.getCallCount(
                                       null,
                                       null,
                                       1,
                                       groupNameModelMap,
                                       busiAndPushRequest.getBeginDate(),
                                       busiAndPushRequest.getEndDate(),
                                       busiAndPushRequest.getDataSource(),
                                       busiAndPushRequest.getDataTypeDimension(),
                                       ifLone,
                                       batchId,
                                       dataType
                                    );
                                 }

                                 if (callCount == 0L) {
                                    model.setUnReadCall(0L);
                                    model.setCallCount(0L);
                                 } else {
                                    Map<Long, String> groupIdModelMap = this.convertToIdMap(groupModelList);
                                    long readCallNum = this.pushReadCallDB
                                       .getCallIdCount(1, null, null, startTime, endTime, groupIdModelMap, ifLone, batchId, dataType);
                                    if (readCallNum == 0L) {
                                       model.setUnReadCall(callCount);
                                    } else {
                                       model.setUnReadCall(this.getUnReadCallNum(callCount, readCallNum));
                                    }

                                    model.setCallCount(callCount);
                                 }
                              } else {
                                 if (childrenGroup.indexOf(objs) == 0) {
                                    callCountAll = this.getCallCount(
                                       null,
                                       null,
                                       0,
                                       null,
                                       busiAndPushRequest.getBeginDate(),
                                       busiAndPushRequest.getEndDate(),
                                       busiAndPushRequest.getDataSource(),
                                       busiAndPushRequest.getDataTypeDimension(),
                                       ifLone,
                                       batchId,
                                       dataType
                                    );
                                 }

                                 if (callCountAll == 0L) {
                                    model.setCallCount(0L);
                                    model.setCallPercent("0.00%");
                                 } else {
                                    List<BigDecimal> modelIdList = this.topicGroupDB.getModelIdList(modelId, topicGroupType);
                                    Long callCountx = 0L;
                                    if (modelIdList != null && !modelIdList.isEmpty()) {
                                       String modelIdStr = this.convertListToString(modelIdList);
                                       callCountx = this.getCallCount(
                                          modelIdStr,
                                          null,
                                          0,
                                          null,
                                          busiAndPushRequest.getBeginDate(),
                                          busiAndPushRequest.getEndDate(),
                                          busiAndPushRequest.getDataSource(),
                                          busiAndPushRequest.getDataTypeDimension(),
                                          ifLone,
                                          batchId,
                                          dataType
                                       );
                                    }

                                    Double callPercent = callCountx.doubleValue() / callCountAll.doubleValue();
                                    model.setCallCount(callCountx);
                                    model.setCallPercent(this.getPercent(callPercent));
                                 }
                              }

                              resultList.add(model);
                           }
                        } catch (Exception var28) {
                           this.logger.error("queryChildrenGroup service has a error!", var28);
                        }
                     }
                  }

                  List<Object[]> modelIdList = this.topicGroupDB.getChildrenModel(id);
                  if (modelIdList != null && !modelIdList.isEmpty()) {
                     for (Object[] objs : modelIdList) {
                        try {
                           Long modelId = ((BigDecimal)objs[0]).longValue();
                           String modelName = String.valueOf(objs[1]);
                           model = new TopicModelDTO();
                           model.setModelId(modelId);
                           model.setModelName(modelName);
                           model.setModelOrGroup(0);
                           if (settingPage == 1) {
                              resultList.add(model);
                           } else {
                              if (topicGroupType == 1) {
                                 Long callCountx = 0L;
                                 Map<String, String> mapName = new HashMap<>();
                                 TopicGroup topicGroup = this.topicGroupDB.getEntityById(id);
                                 if (topicGroup == null) {
                                    this.logger.error("通过专题组id无法获取该专题组实体");
                                    continue;
                                 }

                                 mapName.put(topicGroup.getGroupName(), "" + modelId);
                                 callCountx = this.getCallCount(
                                    null,
                                    null,
                                    1,
                                    mapName,
                                    busiAndPushRequest.getBeginDate(),
                                    busiAndPushRequest.getEndDate(),
                                    busiAndPushRequest.getDataSource(),
                                    busiAndPushRequest.getDataTypeDimension(),
                                    ifLone,
                                    batchId,
                                    dataType
                                 );
                                 if (callCountx == 0L) {
                                    model.setUnReadCall(0L);
                                    model.setCallCount(0L);
                                 } else {
                                    Map<Long, String> mapId = new HashMap<>();
                                    mapId.put(id, "" + modelId);
                                    long readCallNum = this.pushReadCallDB.getCallIdCount(0, modelId, id, startTime, endTime, mapId, ifLone, batchId, dataType);
                                    if (readCallNum == 0L) {
                                       model.setUnReadCall(callCountx);
                                    } else {
                                       model.setUnReadCall(this.getUnReadCallNum(callCountx, readCallNum));
                                    }

                                    model.setCallCount(callCountx);
                                 }
                              } else {
                                 if (modelIdList.indexOf(objs) == 0 && callCountAll == 0L) {
                                    callCountAll = this.getCallCount(
                                       null,
                                       null,
                                       0,
                                       null,
                                       busiAndPushRequest.getBeginDate(),
                                       busiAndPushRequest.getEndDate(),
                                       busiAndPushRequest.getDataSource(),
                                       busiAndPushRequest.getDataTypeDimension(),
                                       ifLone,
                                       batchId,
                                       dataType
                                    );
                                 }

                                 if (callCountAll == 0L) {
                                    model.setCallCount(0L);
                                    model.setCallPercent("0.00%");
                                 } else {
                                    Long callCountxx = this.getCallCount(
                                       String.valueOf(modelId),
                                       null,
                                       0,
                                       null,
                                       busiAndPushRequest.getBeginDate(),
                                       busiAndPushRequest.getEndDate(),
                                       busiAndPushRequest.getDataSource(),
                                       busiAndPushRequest.getDataTypeDimension(),
                                       ifLone,
                                       batchId,
                                       dataType
                                    );
                                    Double callPercent = callCountxx.doubleValue() / callCountAll.doubleValue();
                                    model.setCallCount(callCountxx);
                                    model.setCallPercent(this.getPercent(callPercent));
                                 }
                              }

                              resultList.add(model);
                           }
                        } catch (Exception var29) {
                           this.logger.error("queryChildrenGroup service has a error!", var29);
                        }
                     }
                  }

                  return resultList;
               } else {
                  this.logger.error("获取数据权限失败");
                  throw new VieAppServiceException("获取数据权限失败");
               }
            }
         } catch (VieAppServiceException var30) {
            this.logger.error(var30.getMessage());
            throw new VieAppServiceException(var30.getMessage());
         } catch (Exception var31) {
            this.logger.error("queryChildrenGroup service has a error!", var31);
            throw new VieAppServiceException("服务内部错误", var31);
         }
      }
   }

   public Long addTopicGroup(TopicGroupRequest topicGroupRequest) throws VieAppServiceException {
      Long groupId = null;
      if (topicGroupRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("parentGroupId");
            keyList.add("topicGroupType");
            keyList.add("topicGroupName");
            String checkResult = this.checkRequestParamNull(topicGroupRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               long parentGroupId = topicGroupRequest.getParentGroupId();
               String topicGroupName = topicGroupRequest.getTopicGroupName();
               int topicGroupType = topicGroupRequest.getTopicGroupType();
               boolean isExistName = this.topicGroupDB.isExistName(parentGroupId, topicGroupName, topicGroupType);
               if (isExistName) {
                  this.logger.error("已存在相同的专题组名称");
                  throw new VieAppServiceException("已存在相同的专题组名称");
               } else {
                  TopicGroup topicGroup = new TopicGroup();
                  topicGroup.setGroupName(topicGroupName);
                  topicGroup.setGroupType(topicGroupType);
                  topicGroup.setDeleted(0);
                  topicGroup.setCreateTime(DateUtils.parseDateToString(new Date(), "yyyy-MM-dd HH:mm:ss"));
                  topicGroup.setParentGroupId(parentGroupId);
                  topicGroup.setRemark("create by addTopicGroup");
                  groupId = this.topicGroupDB.saveTopicGroup(topicGroup);
                  if (groupId == null) {
                     throw new VieAppServiceException("保存失败");
                  } else {
                     return groupId;
                  }
               }
            }
         } catch (VieAppServiceException var11) {
            this.logger.error(var11.getMessage());
            throw new VieAppServiceException(var11.getMessage());
         } catch (Exception var12) {
            this.logger.error("addTopicGroup service has a error!", var12);
            throw new VieAppServiceException("服务内部错误", var12);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public boolean updateTopicGroup(TopicGroupRequest topicGroupRequest) throws VieAppServiceException {
      boolean resultFlag = true;
      if (topicGroupRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("id");
            keyList.add("topicGroupName");
            String checkResult = this.checkRequestParamNull(topicGroupRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               Long id = topicGroupRequest.getId();
               String topicGroupName = topicGroupRequest.getTopicGroupName();
               TopicGroup topicGroup = this.topicGroupDB.getEntityById(id);
               if (topicGroup == null) {
                  this.logger.error("该专题组已不存在！");
                  throw new VieAppServiceException("该专题组已不存在！");
               } else {
                  boolean isExistName = this.topicGroupDB.isExistName(topicGroup.getParentGroupId(), topicGroupName, topicGroup.getGroupType());
                  if (isExistName) {
                     this.logger.error("已存在相同的专题组名称，请重新命名");
                     throw new VieAppServiceException("已存在相同的专题组名称，请重新命名");
                  } else {
                     boolean isUpdate = this.topicGroupDB.updateTopicGroup(id, topicGroupName);
                     if (!isUpdate) {
                        resultFlag = false;
                     }

                     return resultFlag;
                  }
               }
            }
         } catch (VieAppServiceException var10) {
            this.logger.error(var10.getMessage());
            throw new VieAppServiceException(var10.getMessage());
         } catch (Exception var11) {
            this.logger.error("updateTopicGroup service has a error!", var11);
            throw new VieAppServiceException("服务内部错误", var11);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public boolean deleteTopicGroup(Long id) throws VieAppServiceException {
      boolean resultFlag = true;
      if (id != null) {
         try {
            return this.topicGroupDB.deleteTopicGroup(id);
         } catch (Exception var4) {
            this.logger.error("updateTopicGroup service has a error!", var4);
            throw new VieAppServiceException("服务内部错误", var4);
         }
      } else {
         this.logger.error("参数(id)为空");
         throw new VieAppServiceException("参数(id)为空");
      }
   }

   public boolean addGroupModels(TopicGroupRequest topicGroupRequest) throws VieAppServiceException {
      boolean resultFlag = true;
      if (topicGroupRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("id");
            keyList.add("modelIds");
            String checkResult = this.checkRequestParamNull(topicGroupRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               List<TopicGroupModel> models = new ArrayList<>();
               String[] modelIds = topicGroupRequest.getModelIds().trim().split(",");

               for (String modelId : modelIds) {
                  TopicGroupModel model = new TopicGroupModel();
                  model.setTopicGroupId(topicGroupRequest.getId());
                  model.setModelId(Long.valueOf(modelId));
                  models.add(model);
               }

               this.topicGroupDB.addGroupModels(models);
               return resultFlag;
            }
         } catch (VieAppServiceException var12) {
            this.logger.error(var12.getMessage());
            throw new VieAppServiceException(var12.getMessage());
         } catch (Exception var13) {
            this.logger.error("addGroupModels service has a error!", var13);
            throw new VieAppServiceException("服务内部错误", var13);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public boolean deleteGroupModel(TopicGroupRequest topicGroupRequest) throws VieAppServiceException {
      boolean resultFlag = true;
      if (topicGroupRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("parentGroupId");
            keyList.add("modelId");
            String checkResult = this.checkRequestParamNull(topicGroupRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               boolean isDeleted = this.topicGroupDB.deleteGroupModel(topicGroupRequest.getParentGroupId(), topicGroupRequest.getModelId());
               if (isDeleted) {
                  resultFlag = true;
               } else {
                  resultFlag = false;
               }

               return resultFlag;
            }
         } catch (VieAppServiceException var6) {
            this.logger.error(var6.getMessage());
            throw new VieAppServiceException(var6.getMessage());
         } catch (Exception var7) {
            this.logger.error("deleteGroupModel service has a error!", var7);
            throw new VieAppServiceException("服务内部错误", var7);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   private List<TopicModelDTO> getModels(Long groupId, Date startTime, Date endTime, List<String> seatGroupList, AllCallReadRequest allCallReadRequest) {
      List<TopicModelDTO> models = new ArrayList<>();
      TopicModelDTO model = null;
      int ifLone = allCallReadRequest.getIfLone() == null ? 0 : allCallReadRequest.getIfLone();
      String batchId = StringUtils.isNullOrEmpry(allCallReadRequest.getBatchId()) ? "0" : allCallReadRequest.getBatchId();
      int dataType = allCallReadRequest.getDataType() == null ? 2 : allCallReadRequest.getDataType();
      List<Object[]> childrenGroup = this.topicGroupDB.queryChildrenGroup(groupId, 1, seatGroupList, 0);
      if (childrenGroup != null && !childrenGroup.isEmpty()) {
         for (Object[] objs : childrenGroup) {
            try {
               Long modelId = ((BigDecimal)objs[0]).longValue();
               String modelName = String.valueOf(objs[1]);
               model = new TopicModelDTO();
               model.setModelId(modelId);
               model.setModelName(modelName);
               model.setModelOrGroup(1);
               List<Object[]> groupModelList = this.topicGroupDB.getGroupModelList(modelId, seatGroupList);
               Map<String, String> groupNameModelMap = this.convertToNameMap(groupModelList);
               Long callCount = 0L;
               if (groupNameModelMap != null && !groupNameModelMap.isEmpty()) {
                  callCount = this.getCallCount(
                     null,
                     null,
                     1,
                     groupNameModelMap,
                     allCallReadRequest.getBeginDate(),
                     allCallReadRequest.getEndDate(),
                     allCallReadRequest.getDataSource(),
                     allCallReadRequest.getDataTypeDimension(),
                     ifLone,
                     batchId,
                     dataType
                  );
               }

               if (callCount == 0L) {
                  model.setUnReadCall(0L);
                  model.setCallCount(0L);
               } else {
                  Map<Long, String> groupIdModelMap = this.convertToIdMap(groupModelList);
                  long readCallNum = this.pushReadCallDB.getCallIdCount(1, null, null, startTime, endTime, groupIdModelMap, ifLone, batchId, dataType);
                  if (readCallNum == 0L) {
                     model.setUnReadCall(callCount);
                  } else {
                     model.setUnReadCall(this.getUnReadCallNum(callCount, readCallNum));
                  }

                  model.setCallCount(callCount);
               }

               models.add(model);
            } catch (Exception var23) {
               this.logger.error("getModels method has a error!", var23);
            }
         }
      }

      List<Object[]> modelIdList = this.topicGroupDB.getChildrenModel(groupId);
      if (modelIdList != null && !modelIdList.isEmpty()) {
         for (Object[] objs : modelIdList) {
            try {
               Long modelIdx = ((BigDecimal)objs[0]).longValue();
               String modelNamex = String.valueOf(objs[1]);
               model = new TopicModelDTO();
               model.setModelId(modelIdx);
               model.setModelName(modelNamex);
               model.setModelOrGroup(0);
               Long callCountx = 0L;
               Map<String, String> mapName = new HashMap<>();
               mapName.put(this.topicGroupDB.getEntityById(groupId).getGroupName(), "" + modelIdx);
               callCountx = this.getCallCount(
                  null,
                  null,
                  1,
                  mapName,
                  allCallReadRequest.getBeginDate(),
                  allCallReadRequest.getEndDate(),
                  allCallReadRequest.getDataSource(),
                  allCallReadRequest.getDataTypeDimension(),
                  ifLone,
                  batchId,
                  dataType
               );
               if (callCountx == 0L) {
                  model.setUnReadCall(0L);
                  model.setCallCount(0L);
               } else {
                  Map<Long, String> mapId = new HashMap<>();
                  mapId.put(groupId, "" + modelIdx);
                  long readCallNum = this.pushReadCallDB.getCallIdCount(0, modelIdx, groupId, startTime, endTime, mapId, ifLone, batchId, dataType);
                  if (readCallNum == 0L) {
                     model.setUnReadCall(callCountx);
                  } else {
                     model.setUnReadCall(this.getUnReadCallNum(callCountx, readCallNum));
                  }

                  model.setCallCount(callCountx);
               }

               models.add(model);
            } catch (Exception var22) {
               this.logger.error("getModels method has a error!", var22);
            }
         }
      }

      return models;
   }

   public String parseFilters(List<Filter> filters) {
      if (filters != null && filters.size() != 0) {
         StringBuffer conditionBuffer = new StringBuffer();
         int count = 0;

         for (int i = 0; i < filters.size(); i++) {
            Filter filter = filters.get(i);
            String condition = filter.toSqlCondition();
            if (condition != null && !"".equals(condition)) {
               if (count++ != 0) {
                  conditionBuffer.append(" and ");
               }

               conditionBuffer.append("(" + condition + ")");
            }
         }

         return conditionBuffer.length() == 0 ? conditionBuffer.toString() : conditionBuffer.append(" and (processed=0) ").toString();
      } else {
         return "";
      }
   }

   private Map<Long, List<Long>> getModelGroupMap(Long queryId, int queryModelOrGroup, Long parentGroupId) {
      Map<Long, List<Long>> map = new HashMap<>();
      List<Long> groupIdList = new ArrayList<>();
      if (queryModelOrGroup == 0) {
         groupIdList.add(parentGroupId);
         map.put(queryId, groupIdList);
      } else {
         List<Object[]> resultList = this.topicGroupDB.getModelAndGroupIdList(queryId);
         if (resultList != null && !resultList.isEmpty()) {
            for (Object[] objs : resultList) {
               Long modelId = ((BigDecimal)objs[0]).longValue();
               Long groupId = ((BigDecimal)objs[1]).longValue();
               if (map.get(modelId) == null) {
                  List<Long> groups = new ArrayList<>();
                  groups.add(groupId);
                  map.put(modelId, groups);
               } else {
                  List<Long> groups = map.get(modelId);
                  groups.add(groupId);
                  map.put(modelId, groups);
               }
            }
         }
      }

      return map;
   }

   public void setPagerInfo(PagerResponse<DataInfo> pager, int totalNum, List<DataInfo> list, int pageNum, int pageSize) {
      int totalPage = totalNum % pageSize == 0 ? totalNum / pageSize : totalNum / pageSize + 1;
      pager.setTotalRows(totalNum);
      pager.setTotalPages(totalPage);
      pager.setPageNum(pageNum);
      pager.setPageSize(pageSize);
      pager.setRows(list);
   }

   private List<Long> getModelIdList(Long queryId, int queryTopicGroupType, int queryModelOrGroup) {
      List<Long> modelIdList = new ArrayList<>();
      if (queryModelOrGroup == 0) {
         modelIdList.add(queryId);
      } else {
         List<BigDecimal> resultList = this.topicGroupDB.getModelIdList(queryId, queryTopicGroupType);
         if (resultList != null && !resultList.isEmpty()) {
            for (BigDecimal result : resultList) {
               modelIdList.add(result.longValue());
            }
         }
      }

      if (modelIdList.isEmpty()) {
         modelIdList.add(-1L);
      }

      return modelIdList;
   }

   private FilterParser getFilterParser(String beginDate, String endDate) throws ParseException {
      List<Map<String, Object>> dimensionList = new ArrayList<>();
      Map<String, Object> dimension = new HashMap<>();
      dimension.put("dataType", "long");
      dimension.put("uptonow", 0);
      dimension.put("name", "时间");
      List<String> timeList = new ArrayList<>();
      timeList.add(beginDate);
      timeList.add(endDate);
      dimension.put("value", timeList);
      dimension.put("type", "timeRange");
      dimension.put("key", "timestamp");
      dimensionList.add(dimension);
      FilterParser filterParser = new FilterParser(JSONObject.toJSONString(dimensionList));
      filterParser.parserFilters();
      return filterParser;
   }

   private ColumnInfo getColumnMaps(String searchDimension) {
      ColumnInfo columnInfo = new ColumnInfo();
      columnInfo.getColumnMaps().add(new ColumnMap("流水号", "id"));
      columnInfo.getColumnMaps().add(new ColumnMap("通话时长（秒）", "duration"));
      columnInfo.getColumns().add("id");
      columnInfo.getColumns().add("duration");
      if (!StringUtils.isNullOrEmpry(searchDimension)) {
         List<JSONObject> list = (List<JSONObject>)JSON.parseObject(searchDimension, List.class);
         if (list != null && list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
               JSONObject jsonObject = list.get(i);
               ColumnMap columnMap = new ColumnMap(jsonObject.getString("columnName"), jsonObject.getString("column"));
               columnInfo.getColumnMaps().add(columnMap);
               columnInfo.getColumns().add(columnMap.getColumn());
            }
         }
      }

      columnInfo.getColumnMaps().add(new ColumnMap("关键词", "keyword"));
      columnInfo.getColumnMaps().add(new ColumnMap("模型", "modelName"));
      columnInfo.setColumnsStr(columnInfo.getColumns().toString());
      return columnInfo;
   }

   private CallCounTrendResponse getCallCounTrend(String sql, String stime, String timetmp, String timeType) {
      CallCounTrendResponse callCounTrendResponse = null;
      List<LinkedHashMap<String, Object>> linkedHashMapList = new ArrayList<>();
      String[] anydrillAddressMain = LoadCommonConfig.get("mainAnydrillAddress").split(";");
      PreparedStatement ps = new PreparedStatement(anydrillAddressMain);
      ResultSet rs = ps.executeQuery(sql);
      if (rs != null) {
         linkedHashMapList = rs.toList();
      }

      if ("hour".equals(timeType)) {
         callCounTrendResponse = this.dealListDataOfHour(linkedHashMapList, stime, timetmp);
      } else if ("day".equals(timeType)) {
         callCounTrendResponse = this.dealListDataOfDay(linkedHashMapList, stime, timetmp);
      }

      return callCounTrendResponse;
   }

   private CallCounTrendResponse dealListDataOfDay(List<LinkedHashMap<String, Object>> data, String startTime, String endTime) {
      try {
         CallCounTrendResponse result = new CallCounTrendResponse();
         Date startDate = DateUtils.parseStringToDate(startTime);
         Date endDate = DateUtils.parseStringToDate(endTime);
         YDataResponse yDataResponse = new YDataResponse();
         List<String> list = new ArrayList<>();
         List<Long> timeList = new ArrayList<>();
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(startDate);
         String startTemp = "";
         int day = (int)((endDate.getTime() - startDate.getTime()) / 86400000L);

         for (int c = day / 30; calendar.getTime().before(endDate) || calendar.getTime().equals(endDate); calendar.add(5, 1 + c)) {
            String calenTemp = new SimpleDateFormat("yyyyMMdd").format(calendar.getTime());

            for (LinkedHashMap<String, Object> current : data) {
               String callTimeFlag = String.valueOf(current.get("calltime"));
               long timeLenFlag = Double.valueOf(String.valueOf(current.get("callCount"))).longValue();
               if (callTimeFlag.equals(calenTemp)) {
                  list.add(this.dateFormatOfDay(calendar.getTime()));
                  timeList.add(timeLenFlag);
                  startTemp = calenTemp;
                  break;
               }
            }

            if (!startTemp.equals(calenTemp)) {
               list.add(this.dateFormatOfDay(calendar.getTime()));
               timeList.add(0L);
            }
         }

         if (!list.get(list.size() - 1).equals(new SimpleDateFormat("MM-dd").format(endDate))) {
            for (LinkedHashMap<String, Object> currentx : data) {
               String callTimeFlag = String.valueOf(currentx.get("calltime"));
               long timeLenFlag = Double.valueOf(String.valueOf(currentx.get("callCount"))).longValue();
               if (callTimeFlag.equals(new SimpleDateFormat("yyyyMMdd").format(endDate))) {
                  list.add(this.dateFormatOfDay(endDate));
                  timeList.add(timeLenFlag);
                  startTemp = callTimeFlag;
                  break;
               }
            }

            if (!startTemp.equals(this.dateFormatOfDay(endDate))) {
               list.add(this.dateFormatOfDay(endDate));
               timeList.add(0L);
            }
         }

         yDataResponse.setName("通话量");
         yDataResponse.setData(timeList);
         result.setxData(list);
         result.setyData(yDataResponse);
         return result;
      } catch (Exception var20) {
         this.logger.error("dealListDataOfDay method has a error!", var20);
         return null;
      }
   }

   private String dateFormatOfDay(Date startTime) {
      SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
      return sdf.format(startTime);
   }

   private CallCounTrendResponse dealListDataOfHour(List<LinkedHashMap<String, Object>> data, String startTime, String endTime) {
      try {
         CallCounTrendResponse result = new CallCounTrendResponse();
         Date startDate = DateUtils.parseStringToDate(startTime);
         Date endDate = DateUtils.parseStringToDate(endTime);
         Date now = new Date();
         if (DateUtils.parseDateToString(now, "yyyy-MM-dd").equals(endTime)) {
            Calendar c = Calendar.getInstance();
            c.setTime(now);
            c.set(14, 0);
            c.set(13, 0);
            c.set(12, 0);
            endDate = c.getTime();
         } else {
            Calendar c = Calendar.getInstance();
            c.setTime(endDate);
            c.set(14, 0);
            c.set(13, 0);
            c.set(12, 0);
            c.set(11, 23);
            endDate = c.getTime();
         }

         YDataResponse yDataResponse = new YDataResponse();
         List<String> list = new ArrayList<>();
         List<Long> timeList = new ArrayList<>();
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(startDate);
         String startTemp = "";
         int day = (int)((endDate.getTime() - startDate.getTime()) / 3600000L);

         for (int c = day / 30; calendar.getTime().before(endDate) || calendar.getTime().equals(endDate); calendar.add(10, 1 + c)) {
            String calenTemp = new SimpleDateFormat("yyyyMMddHH").format(calendar.getTime());

            for (LinkedHashMap<String, Object> current : data) {
               String callTimeFlag = String.valueOf(current.get("calltime"));
               Long timeLenFlag = Double.valueOf(String.valueOf(current.get("callCount"))).longValue();
               if (callTimeFlag.equals(calenTemp)) {
                  list.add(this.dateFormatOfHour(calendar.getTime()));
                  timeList.add(timeLenFlag);
                  startTemp = calenTemp;
                  break;
               }
            }

            if (!startTemp.equals(calenTemp)) {
               list.add(this.dateFormatOfHour(calendar.getTime()));
               timeList.add(0L);
            }
         }

         if (!list.get(list.size() - 1).equals(new SimpleDateFormat("MM-dd HH:mm").format(endDate))) {
            for (LinkedHashMap<String, Object> currentx : data) {
               String callTimeFlag = String.valueOf(currentx.get("calltime"));
               long timeLenFlag = Double.valueOf(String.valueOf(currentx.get("callCount"))).longValue();
               if (callTimeFlag.equals(new SimpleDateFormat("yyyyMMddHH").format(endDate))) {
                  list.add(this.dateFormatOfHour(endDate));
                  timeList.add(timeLenFlag);
                  startTemp = callTimeFlag;
                  break;
               }
            }

            if (!startTemp.equals(this.dateFormatOfHour(endDate))) {
               list.add(this.dateFormatOfHour(endDate));
               timeList.add(0L);
            }
         }

         yDataResponse.setName("通话量");
         yDataResponse.setData(timeList);
         result.setxData(list);
         result.setyData(yDataResponse);
         return result;
      } catch (Exception var20) {
         this.logger.error("dealListDataOfHour method has a error!", var20);
         return null;
      }
   }

   private String dateFormatOfHour(Date startTime) {
      SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
      return sdf.format(startTime);
   }

   private String getModelIdStr(Long queryId, int queryModelOrGroup) {
      String modelIdStr = "";
      if (queryModelOrGroup == 0) {
         modelIdStr = String.valueOf(queryId);
      } else {
         List<BigDecimal> modelIdList = this.topicGroupDB.getModelIdList(queryId, 0);
         if (modelIdList != null && !modelIdList.isEmpty()) {
            modelIdStr = this.convertListToString(modelIdList);
         }
      }

      if (StringUtils.isNullOrEmpry(modelIdStr)) {
         modelIdStr = "-1";
      }

      return modelIdStr;
   }

   private List<TopicModelDTO> getTopicGroups(List<Object[]> resultList, List<String> seatGroupList, BusiAndPushRequest busiAndPushRequest) {
      List<TopicModelDTO> groups = new ArrayList<>();
      TopicModelDTO group = null;
      Long callCountAll = 0L;

      for (int i = 0; i < resultList.size(); i++) {
         try {
            Object[] objs = resultList.get(i);
            Long groupId = Long.parseLong(String.valueOf(objs[0]));
            String groupName = String.valueOf(objs[1]);
            group = new TopicModelDTO();
            group.setModelOrGroup(1);
            group.setModelId(groupId);
            group.setModelName(groupName);
            if (busiAndPushRequest.getSettingPage() == 1) {
               groups.add(group);
               continue;
            }

            int ifLone = busiAndPushRequest.getIfLone() == null ? 0 : busiAndPushRequest.getIfLone();
            String batchId = StringUtils.isNullOrEmpry(busiAndPushRequest.getBatchId()) ? "0" : busiAndPushRequest.getBatchId();
            int dataType = busiAndPushRequest.getDataType() == null ? 2 : busiAndPushRequest.getDataType();
            if (busiAndPushRequest.getTopicGroupType() == 1) {
               List<Object[]> groupModelList = this.topicGroupDB.getGroupModelList(groupId, seatGroupList);
               Map<String, String> groupNameModelMap = this.convertToNameMap(groupModelList);
               Long callCount = 0L;
               if (groupNameModelMap != null && !groupNameModelMap.isEmpty()) {
                  callCount = this.getCallCount(
                     null,
                     null,
                     1,
                     groupNameModelMap,
                     busiAndPushRequest.getBeginDate(),
                     busiAndPushRequest.getEndDate(),
                     busiAndPushRequest.getDataSource(),
                     busiAndPushRequest.getDataTypeDimension(),
                     ifLone,
                     batchId,
                     dataType
                  );
               }

               if (callCount == 0L) {
                  group.setUnReadCall(0L);
                  group.setCallCount(0L);
               } else {
                  Date startTime = DateUtils.parseStringToDate(busiAndPushRequest.getBeginDate());
                  Date endTime = DateUtils.parseStringToDate(busiAndPushRequest.getEndDate());
                  Map<Long, String> groupIdModelMap = this.convertToIdMap(groupModelList);
                  long readCallNum = this.pushReadCallDB.getCallIdCount(1, null, null, startTime, endTime, groupIdModelMap, ifLone, batchId, dataType);
                  if (readCallNum == 0L) {
                     group.setUnReadCall(callCount);
                  } else {
                     group.setUnReadCall(this.getUnReadCallNum(callCount, readCallNum));
                  }

                  group.setCallCount(callCount);
               }
            } else {
               if (i == 0) {
                  callCountAll = this.getCallCount(
                     null,
                     null,
                     0,
                     null,
                     busiAndPushRequest.getBeginDate(),
                     busiAndPushRequest.getEndDate(),
                     busiAndPushRequest.getDataSource(),
                     busiAndPushRequest.getDataTypeDimension(),
                     ifLone,
                     batchId,
                     dataType
                  );
               }

               if (callCountAll == 0L) {
                  group.setCallCount(0L);
                  group.setCallPercent("0.00%");
               } else {
                  List<BigDecimal> modelIdList = this.topicGroupDB.getModelIdList(groupId, 0);
                  Long callCountx = 0L;
                  if (modelIdList != null && !modelIdList.isEmpty()) {
                     String modelIdStr = this.convertListToString(modelIdList);
                     callCountx = this.getCallCount(
                        modelIdStr,
                        null,
                        0,
                        null,
                        busiAndPushRequest.getBeginDate(),
                        busiAndPushRequest.getEndDate(),
                        busiAndPushRequest.getDataSource(),
                        busiAndPushRequest.getDataTypeDimension(),
                        ifLone,
                        batchId,
                        dataType
                     );
                  }

                  Double callPercent = callCountx.doubleValue() / callCountAll.doubleValue();
                  group.setCallCount(callCountx);
                  group.setCallPercent(this.getPercent(callPercent));
               }
            }
         } catch (Exception var22) {
            this.logger.error("getTopicGroups method has a error!", var22);
            continue;
         }

         groups.add(group);
      }

      return groups;
   }

   private String getPercent(Double callPercent) {
      NumberFormat nf = NumberFormat.getPercentInstance();
      nf.setMinimumFractionDigits(2);
      return nf.format(callPercent);
   }

   private Long getCallCount(
      String modelIds,
      String callIdStr,
      int topicGroupType,
      Map<String, String> groupModelMap,
      String beginDate,
      String endDate,
      String tableName,
      String dataTypeDimension,
      int ifLone,
      String batchId,
      int dataType
   ) {
      try {
         long startTime = this.getStartDateTime(beginDate);
         long endTime = this.getEndDateTime(endDate);
         String sql = "select count(idCounter) as callCount  from "
            + IndexConstants.getModelTableName(tableName)
            + " where processed = 0 and timestamp between "
            + startTime
            + " and "
            + endTime
            + " and "
            + MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType);
         if (topicGroupType != 1) {
            if (!StringUtils.isNullOrEmpry(modelIds)) {
               sql = sql + " and offLineTagId in (" + modelIds + ") ";
            }
         } else if (groupModelMap != null && groupModelMap.size() > 0) {
            StringBuffer sbf = new StringBuffer();
            sbf.append(" and (");

            for (Entry<String, String> group : groupModelMap.entrySet()) {
               sbf.append("(" + dataTypeDimension + " = '").append(group.getKey()).append("' and offLineTagId in (").append(group.getValue());
               sbf.append(")) or ");
            }

            sql = sql + sbf.substring(0, sbf.length() - 3) + ") ";
         }

         if (!StringUtils.isNullOrEmpry(callIdStr)) {
            sql = sql + " and id not in (" + callIdStr + ") ";
         }

         sql = sql + " group by id";
         this.logger.info("getCallCount查询的sql为：{}", sql);
         String[] anydrillAddressModel = LoadCommonConfig.get("modelAnydrillAddress").split(";");
         PreparedStatement ps = new PreparedStatement(anydrillAddressModel);
         ResultSet result = ps.executeQuery(sql);
         if (result != null && result.hasRow()) {
            long callCount = result.getTotalCount();
            return callCount;
         }
      } catch (Exception var22) {
         this.logger.error("getCallCount method has a error!", var22);
      }

      return 0L;
   }

   private long getStartDateTime(String startTime) {
      if (StringUtils.isNullOrEmpry(startTime)) {
         return 0L;
      } else {
         long time = 0L;

         try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            time = sdf.parse(startTime).getTime();
         } catch (ParseException var5) {
            this.logger.error("getStartDateTime method has a error!", var5);
         }

         return time;
      }
   }

   private long getEndDateTime(String endTime) {
      if (StringUtils.isNullOrEmpry(endTime)) {
         return 0L;
      } else {
         long time = 0L;

         try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if ("hour".equals(endTime)) {
               Date now = new Date();
               if (sdf.format(now).equals(endTime)) {
                  time = now.getTime();
               } else {
                  time = sdf.parse(endTime).getTime() + 86400000L - 1L;
               }
            } else {
               time = sdf.parse(endTime).getTime() + 86400000L - 1L;
            }
         } catch (ParseException var6) {
            this.logger.error("getEndDateTime method has a error!", var6);
         }

         return time;
      }
   }

   private Map<Long, String> convertToIdMap(List<Object[]> groupModelList) {
      Map<Long, String> map = new HashMap<>();

      for (Object[] groupModel : groupModelList) {
         Long groupId = Long.parseLong(String.valueOf(groupModel[0]));
         Long modelId = Long.parseLong(String.valueOf(groupModel[2]));
         String models = map.get(groupId);
         if (StringUtils.isNullOrEmpry(models)) {
            map.put(groupId, "" + modelId);
         } else {
            map.put(groupId, models + "," + modelId);
         }
      }

      return map;
   }

   private Map<String, String> convertToNameMap(List<Object[]> groupModelList) {
      Map<String, String> map = new HashMap<>();

      for (Object[] groupModel : groupModelList) {
         String groupName = (String)groupModel[1];
         Long modelId = Long.parseLong(String.valueOf(groupModel[2]));
         String models = map.get(groupName);
         if (StringUtils.isNullOrEmpry(models)) {
            map.put(groupName, "" + modelId);
         } else {
            map.put(groupName, models + "," + modelId);
         }
      }

      return map;
   }

   private String convertListToString(List<BigDecimal> modelIdList) {
      StringBuffer sbf = new StringBuffer();

      for (BigDecimal modelid : modelIdList) {
         sbf.append(modelid).append(", ");
      }

      return sbf.substring(0, sbf.length() - 2);
   }

   private String checkRequestParamNull(BusiAndPushRequest busiAndPushRequest, List<String> list) {
      if (list != null && list.size() > 0) {
         String key = null;

         for (int i = 0; i < list.size(); i++) {
            key = list.get(i);
            if ("id".equals(key)) {
               if (busiAndPushRequest.getId() == null) {
                  return key;
               }
            } else if ("systemId".equals(key)) {
               if (StringUtils.isNullOrEmpry(busiAndPushRequest.getSystemId())) {
                  return key;
               }
            } else if ("userId".equals(key)) {
               if (StringUtils.isNullOrEmpry(busiAndPushRequest.getUserId())) {
                  return key;
               }
            } else if ("topicGroupType".equals(key)) {
               if (busiAndPushRequest.getTopicGroupType() == null) {
                  return key;
               }
            } else if ("modelOrGroup".equals(key)) {
               if (busiAndPushRequest.getModelOrGroup() == null) {
                  return key;
               }
            } else if ("timeType".equals(key)) {
               if (StringUtils.isNullOrEmpry(busiAndPushRequest.getTimeType())) {
                  return key;
               }
            } else if ("beginDate".equals(key)) {
               if (StringUtils.isNullOrEmpry(busiAndPushRequest.getBeginDate())) {
                  return key;
               }
            } else if ("endDate".equals(key)) {
               if (StringUtils.isNullOrEmpry(busiAndPushRequest.getEndDate())) {
                  return key;
               }
            } else if ("dataSource".equals(key)) {
               if (StringUtils.isNullOrEmpry(busiAndPushRequest.getDataSource())) {
                  return key;
               }
            } else if ("timeThreshold".equals(key)) {
               if (busiAndPushRequest.getTimeThreshold() == null) {
                  return key;
               }
            } else if ("dataTypeDimension".equals(key) && StringUtils.isNullOrEmpry(busiAndPushRequest.getDataTypeDimension())) {
               return key;
            }
         }
      }

      return null;
   }

   private String checkRequestParamNull(CallReadRequest callReadRequest, List<String> list) {
      if (list != null && list.size() > 0) {
         String key = null;

         for (int i = 0; i < list.size(); i++) {
            key = list.get(i);
            if ("modelIdListStr".equals(key)) {
               if (StringUtils.isNullOrEmpry(callReadRequest.getModelIdListStr())) {
                  return key;
               }
            } else if ("callId".equals(key)) {
               if (StringUtils.isNullOrEmpry(callReadRequest.getCallId())) {
                  return key;
               }
            } else if ("callTime".equals(key) && StringUtils.isNullOrEmpry(callReadRequest.getCallTime())) {
               return key;
            }
         }
      }

      return null;
   }

   private String checkRequestParamNull(AllCallReadRequest allCallReadRequest, List<String> list) {
      if (list != null && list.size() > 0) {
         String key = null;

         for (int i = 0; i < list.size(); i++) {
            key = list.get(i);
            if ("id".equals(key)) {
               if (allCallReadRequest.getId() == null) {
                  return key;
               }
            } else if ("systemId".equals(key)) {
               if (StringUtils.isNullOrEmpry(allCallReadRequest.getSystemId())) {
                  return key;
               }
            } else if ("userId".equals(key)) {
               if (StringUtils.isNullOrEmpry(allCallReadRequest.getUserId())) {
                  return key;
               }
            } else if ("modelOrGroup".equals(key)) {
               if (allCallReadRequest.getModelOrGroup() == null) {
                  return key;
               }
            } else if ("parentGroupId".equals(key)) {
               if (allCallReadRequest.getParentGroupId() == null) {
                  return key;
               }
            } else if ("beginDate".equals(key)) {
               if (StringUtils.isNullOrEmpry(allCallReadRequest.getBeginDate())) {
                  return key;
               }
            } else if ("endDate".equals(key)) {
               if (StringUtils.isNullOrEmpry(allCallReadRequest.getEndDate())) {
                  return key;
               }
            } else if ("dataSource".equals(key)) {
               if (StringUtils.isNullOrEmpry(allCallReadRequest.getDataSource())) {
                  return key;
               }
            } else if ("dataTypeDimension".equals(key) && StringUtils.isNullOrEmpry(allCallReadRequest.getDataTypeDimension())) {
               return key;
            }
         }
      }

      return null;
   }

   private String checkRequestParamNull(TopicGroupRequest topicGroupRequest, List<String> list) {
      if (list != null && list.size() > 0) {
         String key = null;

         for (int i = 0; i < list.size(); i++) {
            key = list.get(i);
            if ("id".equals(key)) {
               if (topicGroupRequest.getId() == null) {
                  return key;
               }
            } else if ("systemId".equals(key)) {
               if (StringUtils.isNullOrEmpry(topicGroupRequest.getSystemId())) {
                  return key;
               }
            } else if ("parentGroupId".equals(key)) {
               if (topicGroupRequest.getParentGroupId() == null) {
                  return key;
               }
            } else if ("topicGroupType".equals(key)) {
               if (topicGroupRequest.getTopicGroupType() == null) {
                  return key;
               }
            } else if ("topicGroupName".equals(key)) {
               if (StringUtils.isNullOrEmpry(topicGroupRequest.getTopicGroupName())) {
                  return key;
               }
            } else if ("modelIds".equals(key)) {
               if (StringUtils.isNullOrEmpry(topicGroupRequest.getModelIds())) {
                  return key;
               }
            } else if ("modelId".equals(key) && topicGroupRequest.getModelId() == null) {
               return key;
            }
         }
      }

      return null;
   }

   private long getUnReadCallNum(long totalCallCount, long readCallNum) {
      return totalCallCount - readCallNum >= 0L ? totalCallCount - readCallNum : 0L;
   }

   private Map<String, String> changeListToMap(List<String> callIdList) {
      Map<String, String> map = new HashMap<>();
      if (callIdList != null && callIdList.size() != 0) {
         for (int i = 0; i < callIdList.size(); i++) {
            map.put(callIdList.get(i), "1");
         }

         return map;
      } else {
         return map;
      }
   }

   private List<LinkedHashMap<String, Object>> getTotalResultList(AllCallReadRequest allCallReadRequest, long callCount, Map<String, String> groupNameModelMap) throws Exception {
      RequestData requestData = new RequestData();
      requestData.setTableName(allCallReadRequest.getDataSource());
      requestData.addColumn("id");
      requestData.addColumn("dimDay");
      requestData.addColumn("offLineTagId");
      requestData.setIfQueryInAllModel(true);
      requestData.setGroupExpression(" group by id,dimDay,offLineTagId");
      Map<String, String> map = new HashMap<>();
      map.put("connectType", "must");
      map.put("queryType", "term");
      map.put("expression", ESQueryUtils.getExpression("processed", "0"));
      map.put("ifSqlOrJson", "json");
      requestData.addfilterMap(map);
      long start = System.currentTimeMillis();
      List<LinkedHashMap<String, Object>> list = null;
      long startTime = DateUtils.getTimestamp(allCallReadRequest.getBeginDate());
      long endTime = DateUtils.getTimestamp(DateUtils.getDaybeforeOrAfter(allCallReadRequest.getEndDate(), 1));
      map = new HashMap<>();
      String rangeExpression = ESQueryUtils.getRangeExpression("timestamp", startTime, endTime);
      map.put("connectType", "and");
      map.put("queryType", "range");
      map.put("expression", rangeExpression);
      map.put("ifSqlOrJson", "json");
      requestData.addfilterMap(map);

      for (Entry<String, String> group : groupNameModelMap.entrySet()) {
         Map<String, String> var19 = new HashMap();
         String expression = "[{\"term\":{\""
            + allCallReadRequest.getDataTypeDimension()
            + "\":\""
            + group.getKey()
            + "\"}}, "
            + "{\"terms\":{\"offLineTagId\":["
            + group.getValue()
            + "]}}]";
         var19.put("connectType", "or");
         var19.put("queryType", "and");
         var19.put("expression", expression);
         var19.put("ifSqlOrJson", "json");
         requestData.addfilterMap(var19);
      }

      ResponseData responseData = this.sparkUtils.execute(requestData);
      if (responseData == null) {
         this.logger.error("查询spark出错");
         return null;
      } else {
         list = responseData.getRsList();
         long end = System.currentTimeMillis();
         this.logger.info("查询索引：{}条；总共耗时：{}毫秒", list.size(), end - start);
         return list;
      }
   }

   public TopicGroupDB getTopicGroupDB() {
      return this.topicGroupDB;
   }

   public void setTopicGroupDB(TopicGroupDB topicGroupDB) {
      this.topicGroupDB = topicGroupDB;
   }

   public PushReadCallDB getPushReadCallDB() {
      return this.pushReadCallDB;
   }

   public void setPushReadCallDB(PushReadCallDB pushReadCallDB) {
      this.pushReadCallDB = pushReadCallDB;
   }

   public Logger getLogger() {
      return this.logger;
   }

   public ModelDB getModelDB() {
      return this.modelDB;
   }

   public void setModelDB(ModelDB modelDB) {
      this.modelDB = modelDB;
   }

   public TopicMarkDB getTopicMarkDB() {
      return this.topicMarkDB;
   }

   public void setTopicMarkDB(TopicMarkDB topicMarkDB) {
      this.topicMarkDB = topicMarkDB;
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

   public void setSparkUtils(SparkUtils sparkUtils) {
      this.sparkUtils = sparkUtils;
   }
}
