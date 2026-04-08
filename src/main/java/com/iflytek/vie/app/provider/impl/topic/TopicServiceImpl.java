package com.iflytek.vie.app.provider.impl.topic;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.iflytek.vie.api.task.NotifyTaskService;
import com.iflytek.vie.app.api.model.ModelApplyService;
import com.iflytek.vie.app.api.topic.TopicService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.model.ModelGroup;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.app.pojo.permission.DimensionAuth;
import com.iflytek.vie.app.pojo.topic.AddMarkRequest;
import com.iflytek.vie.app.pojo.topic.ClusterInfo;
import com.iflytek.vie.app.pojo.topic.CreateClusterRequest;
import com.iflytek.vie.app.pojo.topic.CreateTopicRequest;
import com.iflytek.vie.app.pojo.topic.DeleteMarkRequest;
import com.iflytek.vie.app.pojo.topic.DeleteTopicRequest;
import com.iflytek.vie.app.pojo.topic.DimensionRequest;
import com.iflytek.vie.app.pojo.topic.EditPathRequest;
import com.iflytek.vie.app.pojo.topic.EditTopicRequest;
import com.iflytek.vie.app.pojo.topic.EditTopicResponse;
import com.iflytek.vie.app.pojo.topic.GetClusterStatusRequest;
import com.iflytek.vie.app.pojo.topic.GetPathDataRequest;
import com.iflytek.vie.app.pojo.topic.GetPathDataResponse;
import com.iflytek.vie.app.pojo.topic.GetPathValueRequest;
import com.iflytek.vie.app.pojo.topic.HotViewRequest;
import com.iflytek.vie.app.pojo.topic.LoadCountRequest;
import com.iflytek.vie.app.pojo.topic.MarkQueryRequest;
import com.iflytek.vie.app.pojo.topic.MarkQueryResponse;
import com.iflytek.vie.app.pojo.topic.ModelInfoMap;
import com.iflytek.vie.app.pojo.topic.ModelStatusRequest;
import com.iflytek.vie.app.pojo.topic.ModelStatusResponse;
import com.iflytek.vie.app.pojo.topic.PageRequest;
import com.iflytek.vie.app.pojo.topic.PathDimension;
import com.iflytek.vie.app.pojo.topic.PathDimensionDetail;
import com.iflytek.vie.app.pojo.topic.PathInfoRequest;
import com.iflytek.vie.app.pojo.topic.Preview;
import com.iflytek.vie.app.pojo.topic.PreviewList;
import com.iflytek.vie.app.pojo.topic.TogetherPath;
import com.iflytek.vie.app.pojo.topic.TogetherPathRequest;
import com.iflytek.vie.app.pojo.topic.TopicInfo;
import com.iflytek.vie.app.pojo.topic.TopicInfoOutResponse;
import com.iflytek.vie.app.pojo.topic.TopicInfosResponse;
import com.iflytek.vie.app.pojo.topic.TopicOptionLoad;
import com.iflytek.vie.app.pojo.topic.TopicPath;
import com.iflytek.vie.app.pojo.topic.TopicPathRequest;
import com.iflytek.vie.app.pojo.topic.TopicPathResponse;
import com.iflytek.vie.app.pojo.topic.TopicSelectDimension;
import com.iflytek.vie.app.pojo.topic.TopicTogatherInfo;
import com.iflytek.vie.app.pojo.topic.UpdateTopicDimensionRequest;
import com.iflytek.vie.app.pojo.topic.UpdateTopicNameRequest;
import com.iflytek.vie.app.pojo.topic.UpdateTopicTimeRequest;
import com.iflytek.vie.app.pojo.topicgroup.ColumnMap;
import com.iflytek.vie.app.pojo.topicgroup.DataInfo;
import com.iflytek.vie.app.provider.common.ColumnInfo;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.common.SqlHelper;
import com.iflytek.vie.app.provider.database.ClusterDB;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.TopicDB;
import com.iflytek.vie.app.provider.database.TopicMarkDB;
import com.iflytek.vie.app.provider.database.TopicPathDB;
import com.iflytek.vie.app.provider.database.TopicTogatherDB;
import com.iflytek.vie.app.provider.pojo.Cluster;
import com.iflytek.vie.app.provider.pojo.ClusterTask;
import com.iflytek.vie.app.provider.pojo.ModelInfoDto;
import com.iflytek.vie.app.provider.pojo.TopicAllDimension;
import com.iflytek.vie.app.provider.pojo.TopicMarkInfo;
import com.iflytek.vie.app.provider.utils.BuildTopicFilterByAnydri;
import com.iflytek.vie.app.provider.utils.BuildTopicFilterBySpark;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.pojo.NotifyTaskRequest;
import com.iflytek.vie.pojo.RequestData;
import com.iflytek.vie.pojo.ResponseData;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.ExceptionUtil;
import com.iflytek.vie.utils.SparkUtils;
import com.iflytek.vie.utils.StringUtils;
import com.iflytek.vie.utils.VieCommonException;
import java.io.IOException;
import java.sql.Clob;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicServiceImpl implements TopicService {
   private TopicDB topicDB;
   private ModelDB modelDB;
   private TopicMarkDB topicMarkDB;
   private SparkUtils sparkUtils;
   private CommonData commonData;
   private ClusterDB clusterDB;
   private TopicPathDB topicPathDB;
   private TopicTogatherDB topicTogatherDB;
   private ModelApplyService modelApplyService;
   private Integer limitNum;
   private NotifyTaskService notifyTaskService;
   protected ObjectMapper mapper = new ObjectMapper();
   private DecimalFormat df = new DecimalFormat("0.00");
   private static final Logger logger = LoggerFactory.getLogger(TopicServiceImpl.class);

   public TopicInfosResponse findAllTopics(PageRequest request) throws Exception {
      if (null != request && null != request.getDataSource() && !"".equals(request.getDataSource())) {
         int pageNum = request.getPageNum() == 0 ? 1 : request.getPageNum();
         int pageSize = request.getPageSize() == 0 ? 15 : request.getPageSize();
         String keyword = request.getKeyword();
         String dataSource = request.getDataSource();
         int totalCount = this.topicDB.queryAllTopicsCount(dataSource, keyword);
         if (totalCount == 0) {
            return new TopicInfosResponse();
         } else {
            int totalPage = totalCount % pageSize == 0 ? totalCount / pageSize : totalCount / pageSize + 1;
            List<Object[]> retList = this.topicDB.queryAllTopics(dataSource, pageNum, pageSize, keyword);
            if (null != retList && retList.size() != 0) {
               TopicInfosResponse topicInfoList = new TopicInfosResponse();
               List<TopicInfoOutResponse> topicInfos = new ArrayList<>();

               for (Object[] objects : retList) {
                  TopicInfoOutResponse topic = new TopicInfoOutResponse();
                  topic.setTopicId(String.valueOf(objects[0]));
                  topic.setTopicName(String.valueOf(objects[1]));
                  topic.setGroupName(String.valueOf(objects[2]));
                  if (String.valueOf(objects[4]).equals("null")) {
                     topic.setUpdateUserName("");
                  } else {
                     topic.setUpdateUserName(String.valueOf(objects[4]));
                  }

                  if (String.valueOf(objects[5]).equals("null")) {
                     topic.setUpdateTime("");
                  } else {
                     topic.setUpdateTime(String.valueOf(objects[5]));
                  }

                  if (String.valueOf(objects[7]).equals("null")) {
                     topic.setTopicCondition("");
                  } else {
                     String s = "";
                     if (!DBSelectUtil.isSqlServer() && !DBSelectUtil.isMysql()) {
                        s = SqlHelper.clobToString((Clob)objects[7]);
                     } else {
                        s = (String)objects[7];
                     }

                     topic.setTopicCondition(s);
                  }

                  topicInfos.add(topic);
               }

               topicInfoList.setPageNum(pageNum);
               topicInfoList.setRows(topicInfos);
               topicInfoList.setPageSize(pageSize);
               topicInfoList.setTotalPages(totalPage);
               topicInfoList.setTotalRows(totalCount);
               return topicInfoList;
            } else {
               logger.error("查询结果为空");
               throw new VieAppServiceException("查询结果为空");
            }
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public TopicPathResponse createTopic(CreateTopicRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getTopicName()
         && !"".equals(request.getTopicName())
         && null != request.getTopicCondition()
         && !"".equals(request.getTopicCondition())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())
         && null != request.getLoginUserName()
         && !"".equals(request.getLoginUserName())
         && null != request.getRoleId()
         && !"".equals(request.getRoleId())) {
         TopicPathResponse result = new TopicPathResponse();
         int count = 0;
         String topicName = request.getTopicName();
         String topicCondition = request.getTopicCondition();
         String dimesionName = request.getDimesionName();
         String dataSource = "";

         try {
            dataSource = request.getDataSource();
            count = this.topicDB.queryTopicCountByName(dataSource, topicName);
         } catch (Exception var17) {
            logger.error("服务提供方异常", var17);
            throw new VieAppServiceException("服务提供方异常", var17);
         }

         if (count > 0) {
            logger.error("专题名称重复");
            throw new VieAppServiceException("专题名称重复！");
         } else {
            TopicInfo topicInfo = new TopicInfo();
            topicInfo.setModelGroup(null);
            topicInfo.setTopicCondition(topicCondition);

            try {
               for (HashMap<String, Object> topMap : this.jsonToMap(topicCondition)) {
                  if (topMap.containsKey("timeType")) {
                     topicInfo.setTimeType(Integer.parseInt(String.valueOf(topMap.get("timeType"))));
                     break;
                  }
               }
            } catch (Exception var18) {
               logger.error("json转map异常", var18);
            }

            try {
               topicInfo.setTopicName(topicName);
               topicInfo.setDataSource(dataSource);
               topicInfo.setRoleId(Integer.parseInt(request.getRoleId()));
               SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
               topicInfo.setCreateTime(sdf.format(new Date()));
               topicInfo.setUpdateTime(sdf.format(new Date()));
               topicInfo.setCreateUserNme(request.getLoginUserName());
               topicInfo.setUpdateUserName(request.getLoginUserName());
               topicInfo.setTopicStatus(0);
               Integer topicId = this.topicDB.saveTopic(topicInfo);
               TopicPath topicPath = new TopicPath();
               topicPath.setApplicationId(1);
               topicPath.setCreateTime(topicInfo.getCreateTime());
               topicPath.setPathCondition("");
               topicPath.setPathName("全局");
               topicPath.setPathStatus(0);
               topicPath.setTopic(topicInfo);
               int pathId = this.topicDB.saveMainPath(topicPath);
               TopicAllDimension topicAllDimension = new TopicAllDimension();
               topicAllDimension.setDimensionJson(dimesionName);
               boolean flag = this.topicDB.deleteDimensions();
               if (flag) {
                  logger.info("删除维度成功");
                  this.topicDB.saveDimensionJson(topicAllDimension);
               } else {
                  logger.info("删除维度失败,未保存");
               }

               result.setPathId(String.valueOf(pathId));
               result.setTopicId(String.valueOf(topicId));
               return result;
            } catch (Exception var16) {
               logger.error("服务提供方异常", var16);
               throw new VieAppServiceException("服务提供方异常", var16);
            }
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public void updateTopic(UpdateTopicDimensionRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getTopicId()
         && !"".equals(request.getTopicId())
         && null != request.getPathDimension()
         && !"".equals(request.getPathDimension())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())) {
         try {
            String topicId = request.getTopicId();
            TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(topicId);
            if (topicInfo == null) {
               logger.error("该专题已被删除");
               throw new VieAppServiceException("该专题已被删除");
            } else {
               for (HashMap<String, Object> pathDimensionMap : this.jsonToMap(request.getPathDimension())) {
                  String pathId = String.valueOf(pathDimensionMap.get("pathId"));
                  Object obj = pathDimensionMap.get("callListDimensions");
                  String callListDimensions = JSON.toJSONString(obj);
                  Object obj1 = pathDimensionMap.get("chartDimensions");
                  String chartDimensions = JSON.toJSONString(obj1);
                  Object obj2 = pathDimensionMap.get("togetherDimensions");
                  String togetherDimensions = JSON.toJSONString(obj2);
                  String xDimension = String.valueOf(pathDimensionMap.get("xDimension"));
                  String yDimension = String.valueOf(pathDimensionMap.get("yDimension"));
                  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                  this.topicDB.updateTopicInfo(topicId, sdf.format(new Date()), request.getLoginUserName());
                  String togetherPath = JSON.toJSONString(pathDimensionMap.get("togetherPath"));
                  List<HashMap<String, Object>> togetherDimensionList = this.jsonToMap(togetherPath);
                  this.topicDB.updateDimension(pathId, callListDimensions, chartDimensions, togetherDimensions, xDimension, yDimension, topicInfo, 1);
                  String dataSQL = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
                  TopicPath topicPath = this.topicDB.queryPathById(pathId);
                  this.topicDB.deletePrePathByPathId(pathId);

                  for (HashMap<String, Object> togetherDimensionMap : (List<HashMap<String, Object>>)togetherDimensionList) {
                     String togetherId = String.valueOf(togetherDimensionMap.get("togetherId"));
                     TopicTogatherInfo topicTogatherInfo = this.topicDB.queryTogetherById(togetherId);
                     if (topicTogatherInfo != null) {
                        TopicOptionLoad topicOptionLoad = new TopicOptionLoad();
                        topicOptionLoad.setLoadIndex(String.valueOf(togetherDimensionMap.get("togetherIndex")));
                        topicOptionLoad.setLoadName(String.valueOf(togetherDimensionMap.get("togetherName")));
                        topicOptionLoad.setPath(topicPath);
                        topicOptionLoad.setDataAuth(dataSQL);
                        topicTogatherInfo.setPath(topicPath);
                        topicOptionLoad.setTogather(topicTogatherInfo);
                        this.topicDB.saveTogetherPath(topicOptionLoad);
                     }
                  }
               }
            }
         } catch (VieAppServiceException var26) {
            logger.error("该专题已被删除", var26);
            throw new VieAppServiceException("该专题已被删除", var26);
         } catch (Exception var27) {
            logger.error("服务提供方异常", var27);
            throw new VieAppServiceException("服务提供方异常", var27);
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public EditTopicResponse editTopic(EditTopicRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getTopicId()
         && !"".equals(request.getTopicId())
         && null != request.getUserId()
         && !"".equals(request.getUserId())
         && null != request.getContext()
         && !"".equals(request.getContext())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())) {
         EditTopicResponse result = new EditTopicResponse();
         String topicId = request.getTopicId();
         TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(topicId);
         if (topicInfo == null) {
            logger.error("该专题已被删除");
            throw new VieAppServiceException("该专题已被删除");
         } else {
            String topicName2 = topicInfo.getTopicName();
            String topicCondition = topicInfo.getTopicCondition();
            List<ModelInfoMap> modelInfos = new ArrayList<>();
            String dimensions = "";

            try {
               List<HashMap<String, Object>> topicConditionList = this.jsonToMap(topicCondition);
               boolean isContain = this.isContain(
                  topicConditionList, this.commonData.getDataMap(Long.valueOf(request.getUserId()), request.getContext(), request.getDataSource())
               );
               if (!isContain) {
                  logger.error("数据权限已修改！该专题不能使用");
                  throw new VieAppServiceException("数据权限已修改！该专题不能使用");
               } else {
                  TopicSelectDimension topicSelectDimension = topicInfo.getTopicSelectDimension();
                  if (topicSelectDimension != null) {
                     String modelIdString = topicSelectDimension.getModelIds();
                     if (modelIdString != null) {
                        String[] modelIds = modelIdString.split(",");
                        List<Long> modelIdList = new ArrayList<>();

                        for (String mid : modelIds) {
                           if (!"null".equals(mid)) {
                              modelIdList.add(Long.parseLong(mid));
                           }
                        }

                        if (modelIdList.size() != 0) {
                           for (ModelInfo modelInfo : this.modelDB.queryModelByIds(modelIdList)) {
                              String modelName = modelInfo.getModelName();
                              String modelId = String.valueOf(modelInfo.getModelId());
                              String ruleText = modelInfo.getTextRule();
                              ModelInfoMap m = new ModelInfoMap();
                              m.setRuleText(ruleText);
                              m.setKey(modelId);
                              m.setValue(modelName);
                              modelInfos.add(m);
                           }
                        }
                     }

                     dimensions = topicSelectDimension.getDimensions();
                     if (dimensions == null) {
                        dimensions = "";
                     }
                  }

                  ModelGroup modelGroup = topicInfo.getModelGroup();
                  String modelGroupName = "";
                  String modelGroupId = "";
                  if (modelGroup != null) {
                     modelGroupName = modelGroup.getGroupName();
                     modelGroupId = String.valueOf(modelGroup.getGroupId());
                  }

                  List<TopicPath> topicPathMapList = this.topicDB.queryDimensionInfo(topicId);
                  new ArrayList();
                  List<PathDimensionDetail> pathDimension = new ArrayList<>();
                  if (topicPathMapList != null) {
                     AuthorizeInfo authorizeInfo = new AuthorizeInfo(request.getUserId(), request.getDataSource());
                     authorizeInfo.setDataSource(request.getDataSource());
                     authorizeInfo.setUserId(request.getUserId());
                     HashMap<String, String> dataMap = this.modelApplyService.getAllModelMap(authorizeInfo);

                     for (TopicPath topicMap : topicPathMapList) {
                        String pathId = String.valueOf(topicMap.getPathId());
                        String pathName = String.valueOf(topicMap.getPathName());
                        String pathStatus2 = String.valueOf(topicMap.getPathStatus());
                        String xDimension = "";
                        String yDimension = "";
                        String s = topicMap.getPathCondition();
                        List<HashMap<String, Object>> var47 = this.jsonToMap(s);
                        if (var47 != null) {
                           for (HashMap<String, Object> pathConditionMap : (List<HashMap<String, Object>>)var47) {
                              String type = String.valueOf(pathConditionMap.get("type"));
                              if (type != null && "model".equals(type)) {
                                 String modelId = String.valueOf(pathConditionMap.get("value"));
                                 Object[] objects = this.modelDB.queryModelInfoById(Long.parseLong(modelId));
                                 if (objects != null && objects.length != 0) {
                                    ModelInfoDto modelInfoDto = SqlHelper.objectsToModelInfoDto(objects);
                                    if (StringUtils.isNotNullAndEmpry(modelInfoDto.getModelName())) {
                                       pathConditionMap.put("name", modelInfoDto.getModelName());
                                    }

                                    pathConditionMap.put("status", modelInfoDto.getModelStatus());
                                    if (!dataMap.keySet().contains(modelId)) {
                                       if (StringUtils.isNotNullAndEmpry(modelInfoDto.getModelName())) {
                                          pathConditionMap.put("name", modelInfoDto.getModelName());
                                       }

                                       pathConditionMap.put("status", 0);
                                    }
                                 } else {
                                    Object[] objects1 = this.modelDB.queryModelInfoByStatus(Long.parseLong(modelId));
                                    ModelInfoDto modelInfoDtox = SqlHelper.objectsToModelInfoDto(objects1);
                                    if (StringUtils.isNotNullAndEmpry(modelInfoDtox.getModelName())) {
                                       pathConditionMap.put("name", modelInfoDtox.getModelName());
                                    }

                                    pathConditionMap.put("status", 0);
                                 }
                              }
                           }
                        }

                        String callListDimensions = topicMap.getCallListDimensions();
                        String chartDimensions = topicMap.getChartDimensions();
                        String togetherDimensions = topicMap.getTogetherDimensions();
                        if (topicMap.getxDimension() != null) {
                           xDimension = String.valueOf(topicMap.getxDimension());
                        }

                        if (topicMap.getyDimension() != null) {
                           yDimension = String.valueOf(topicMap.getyDimension());
                        }

                        List<TopicOptionLoad> togetherOptionLoadList = this.topicDB.queryTogetherOptionLoad(pathId);
                        List<TogetherPath> togetherPath = new ArrayList<>();
                        if (togetherOptionLoadList != null) {
                           for (TopicOptionLoad togetherOptionLoadMap : togetherOptionLoadList) {
                              String togetherId = String.valueOf(togetherOptionLoadMap.getTogather().getTogatherId());
                              String togetherName = String.valueOf(togetherOptionLoadMap.getLoadName());
                              String togetherIndex = String.valueOf(togetherOptionLoadMap.getLoadIndex());
                              TogetherPath t = new TogetherPath();
                              t.setTogetherId(togetherId);
                              t.setTogetherName(togetherName);
                              t.setTogetherIndex(togetherIndex);
                              togetherPath.add(t);
                           }
                        }

                        PathDimensionDetail p = new PathDimensionDetail();
                        p.setPathId(pathId);
                        p.setPathName(pathName);
                        p.setPathParam(JSON.toJSONString(var47));
                        p.setCallListDimensions(callListDimensions);
                        p.setChartDimensions(chartDimensions);
                        p.setTogetherDimensions(togetherDimensions);
                        p.setxDimension(xDimension);
                        p.setyDimension(yDimension);
                        p.setPathStatus(pathStatus2);
                        p.setTogetherPath(togetherPath);
                        pathDimension.add(p);
                     }
                  }

                  result.setTopicId(topicId);
                  result.setTopicName(topicName2);
                  result.setTopicCondition(this.jsonToMap(topicCondition));
                  result.setDimensions(dimensions);
                  result.setModelGroupId(modelGroupId);
                  result.setModelGroupName(modelGroupName);
                  result.setPathDimension(pathDimension);
                  result.setModelInfo(modelInfos);
                  return result;
               }
            } catch (VieAppServiceException var39) {
               throw new VieAppServiceException("该专题已被删除", var39);
            } catch (Exception var40) {
               logger.error("服务提供方异常", var40);
               throw new VieAppServiceException("服务提供方异常", var40);
            }
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public int getPathValue(GetPathValueRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getTopicId()
         && !"".equals(request.getTopicId())
         && null != request.getPathPram()
         && !"".equals(request.getPathPram())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())) {
         try {
            String userId = request.getUserId();
            String systemId = request.getSystemId();
            TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(request.getTopicId());
            if (topicInfo == null) {
               logger.error("该专题已被删除");
               throw new VieAppServiceException("该专题已被删除");
            } else {
               HashMap<String, Set<String>> dataAuth = this.commonData.getDataMapAuth(request.getUserId(), request.getSystemId(), request.getDataSource());
               ObjectMapper mapper = new ObjectMapper();
               List<HashMap<String, String>> pathMap = (List<HashMap<String, String>>)mapper.readValue(request.getPathPram(), List.class);
               String topicCondition = topicInfo.getTopicCondition();
               boolean exitModel = false;

               for (Map<String, String> map : pathMap) {
                  if ("model".equals(map.get("type").toString()) || "offLineTagId".equals(map.get("type").toString())) {
                     exitModel = true;
                     break;
                  }
               }

               LinkedHashMap<String, DimensionConfig> dimenMap = this.commonData.getDimensionMap(request.getDataSource());
               List<LinkedHashMap<String, Object>> values = null;
               if (exitModel) {
                  logger.info("getPathValue方法spark查询###############");
                  RequestData requestData = new RequestData();
                  requestData.addDataAuthToFilterMap(dataAuth);
                  requestData.setTableName(request.getDataSource());
                  requestData.setFuncExpression("count(id) as idCount");
                  requestData.addColumn("id");
                  BuildTopicFilterBySpark.groupProcessParam(requestData);
                  BuildTopicFilterBySpark.groupTopicCondRequest(topicCondition, requestData);
                  BuildTopicFilterBySpark.builPathFilter(pathMap, dimenMap, requestData);
                  MultipleDataConstant.setMultipleDataSql(requestData, request.getBatchId(), request.getIfLone(), request.getDataType());
                  ResponseData responseData = this.sparkUtils.execute(requestData);
                  if (responseData == null) {
                     logger.error("spark查询出错");
                     return 0;
                  }

                  values = responseData.getRsList();
               }

               if (!exitModel) {
                  logger.info("getPathValue方法anydrill查询###############");
                  StringBuffer sb = new StringBuffer();
                  String dataSql = this.commonData.getDataSql(userId, systemId, request.getDataSource());
                  String topicConDSql = BuildTopicFilterByAnydri.groupTopicCondByAnyDri(topicCondition);
                  if (StringUtils.isNullOrEmpry(dataSql)) {
                     sb.append("select count(id) as idCount from " + request.getDataSource() + " where processed = 0 and " + topicConDSql);
                  } else {
                     sb.append("select count(id) as idCount from " + request.getDataSource() + " where processed = 0 and " + dataSql + " and " + topicConDSql);
                  }

                  String pathSql = BuildTopicFilterByAnydri.getPathCond(pathMap, dimenMap);
                  sb.append(" and " + pathSql);
                  String multiDataSql = MultipleDataConstant.getMultipleDataSql(request.getBatchId(), request.getIfLone(), request.getDataType());
                  sb.append(" and " + multiDataSql);
                  String anydrillAddressMain = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress");
                  PreparedStatement pst = new PreparedStatement(anydrillAddressMain.split(";"));
                  logger.info("getPathValue方法sql：{}", sb.toString());
                  ResultSet rs = pst.executeQuery(sb.toString());
                  values = rs.toList();
               }

               int idCount = 0;
               if (values != null && values.size() > 0) {
                  idCount = (int)Double.parseDouble(String.valueOf(values.get(0).get("idCount")));
               }

               return idCount;
            }
         } catch (VieCommonException var20) {
            logger.error("getPathValue方法内部异常", var20);
            if (1001 == var20.getErrorCode()) {
               throw new VieAppServiceException("该路径存在已下线的模型／已删除的维度，请将其移出路径后重新预览", var20);
            } else {
               throw new VieAppServiceException("服务内部错误", var20);
            }
         } catch (VieAppServiceException var21) {
            throw new VieAppServiceException("该专题已被删除", var21);
         } catch (Exception var22) {
            logger.error("getPathValue方法内部异常", var22);
            throw new VieAppServiceException("服务内部错误", var22);
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public void updateTopicName(UpdateTopicNameRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getTopicId()
         && !"".equals(request.getTopicId())
         && null != request.getTopicName()
         && !"".equals(request.getTopicName())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())) {
         int count = 0;

         try {
            String dataSource = request.getDataSource();
            count = this.topicDB.queryTopicCountByName(dataSource, request.getTopicName());
         } catch (Exception var5) {
            logger.error("系统内部异常", var5);
            throw new VieAppServiceException("系统内部异常", var5);
         }

         if (count > 0) {
            logger.error("专题名称重复");
            throw new VieAppServiceException("专题名称重复！");
         } else {
            TopicInfo tI = this.topicDB.fetchTopicInfoById(request.getTopicId());
            if (tI == null) {
               logger.error("该专题已被删除");
               throw new VieAppServiceException("该专题已被删除");
            } else {
               tI.setTopicName(request.getTopicName());
               boolean saveTopicFlag = this.topicDB.saveOrUpdateTopicInfo(tI);
               if (!saveTopicFlag) {
                  logger.error("更新专题名称失败");
                  throw new VieAppServiceException("更新专题名称失败");
               }
            }
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public void updateTopicTime(UpdateTopicTimeRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getTopicId()
         && !"".equals(request.getTopicId())
         && null != request.getTopicCondition()
         && !"".equals(request.getTopicCondition())) {
         try {
            TopicInfo topicInfo = this.topicDB.getTopicById(Integer.parseInt(request.getTopicId()));
            if (topicInfo == null) {
               throw new VieAppServiceException("该专题已被删除");
            } else {
               topicInfo.setTopicCondition(request.getTopicCondition());

               for (HashMap<String, Object> topMap : this.jsonToMap(request.getTopicCondition())) {
                  if (topMap.containsKey("timeType")) {
                     topicInfo.setTimeType(Integer.parseInt(String.valueOf(topMap.get("timeType"))));
                     break;
                  }
               }

               this.topicDB.saveOrUpdateTopicInfo(topicInfo);
            }
         } catch (VieAppServiceException var6) {
            logger.error("该专题已被删除", var6);
            throw new VieAppServiceException("该专题已被删除", var6);
         } catch (Exception var7) {
            logger.error("系统内部异常", var7);
            throw new VieAppServiceException("系统内部异常", var7);
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public void deleteTopics(DeleteTopicRequest request) throws VieAppServiceException {
      if (null != request.getTopicId() && !"".equals(request.getTopicId())) {
         String[] topicIdStrs = request.getTopicId().split(",");
         List<Integer> topicList = new ArrayList<>();

         for (String tpId : topicIdStrs) {
            topicList.add(Integer.parseInt(tpId));
         }

         this.topicDB.deleteTopicsByIds(topicList);

         for (Integer tid : topicList) {
            TopicInfo topicInfo = this.topicDB.getTopicById(tid);
            TopicSelectDimension tsd = topicInfo.getTopicSelectDimension();
            if (tsd != null) {
               String idString = tsd.getModelIds();
               if (idString != null) {
                  String[] mIds = idString.split(",");

                  for (String mId : mIds) {
                     this.modelDB.modelOffline(mId);
                  }
               }
            }
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public String fetchPreDimension() throws VieAppServiceException {
      List<Object> dimensionJsonList = this.topicDB.queryPreDimensionJson();
      String dimensionJson = "";
      if (dimensionJsonList != null && dimensionJsonList.size() != 0) {
         if (DBSelectUtil.isSqlServer()) {
            dimensionJson = (String)dimensionJsonList.get(0);
         } else {
            dimensionJson = SqlHelper.clobToString((Clob)dimensionJsonList.get(0));
         }
      }

      return dimensionJson;
   }

   public void addMark(AddMarkRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getTelephonId()
         && !"".equals(request.getTelephonId())
         && null != request.getMark()
         && !"".equals(request.getMark())) {
         TopicInfo topicInfo = this.topicDB.getTopicById(request.getTopicId());
         if (null == topicInfo) {
            logger.error("该专题已被删除");
            throw new VieAppServiceException("该专题已被删除");
         } else {
            TopicMarkInfo topicMarkInfo = this.topicMarkDB.getMarkInfo(request.getTelephonId(), request.getTopicId());
            if (topicMarkInfo != null) {
               topicMarkInfo.setMark(request.getMark());
               topicMarkInfo.setTopic(topicInfo);
               topicMarkInfo.setIsNormal(0);
               topicMarkInfo.setTelephoneId(request.getTelephonId());
            } else {
               topicMarkInfo = new TopicMarkInfo();
               topicMarkInfo.setMark(request.getMark());
               topicMarkInfo.setTopic(topicInfo);
               topicMarkInfo.setIsNormal(0);
               topicMarkInfo.setTelephoneId(request.getTelephonId());
            }

            Integer markId = this.topicMarkDB.saveMark(topicMarkInfo);
            if (markId == null) {
               logger.error("服务提供方异常");
               throw new VieAppServiceException("服务提供方异常");
            }
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public int editPath(EditPathRequest request) throws VieAppServiceException {
      if (null == request
         || null == request.getPathName()
         || "".equals(request.getPathName())
         || null == request.getPathPram()
         || "".equals(request.getPathPram())
         || null == request.getTopicId()
         || "".equals(request.getTopicId())) {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      } else {
         return StringUtils.isNullOrEmpry(request.getPathId())
            ? this.addPathInfo(request.getTopicId(), request.getPathName(), request.getPathPram())
            : this.editPathInfo(request.getTopicId(), request.getPathName(), request.getPathPram(), request.getPathId());
      }
   }

   public void deletePath(TopicPathRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getPathId()
         && !"".equals(request.getPathId())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())) {
         String pathId = request.getPathId();
         String topicId = request.getTopicId();
         TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(topicId);
         if (topicInfo == null) {
            logger.error("该专题已被删除");
            throw new VieAppServiceException("该专题已被删除");
         } else {
            String dataSQL = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
            TopicPath topicPath = this.topicDB.fetchTopicPathById(Integer.parseInt(pathId));

            try {
               if (topicPath == null) {
                  logger.error("路径不存在！");
                  throw new VieAppServiceException("路径不存在！");
               } else {
                  List<TopicOptionLoad> topicOptionLoads = this.clusterDB.queryTOLByPathID(pathId, dataSQL);
                  if (topicOptionLoads != null) {
                     for (TopicOptionLoad topicOptionLoad : topicOptionLoads) {
                        this.clusterDB.deleterTopicOptionLoad(topicOptionLoad);
                     }
                  }

                  List<ClusterTask> clusterTaskList = this.clusterDB.queryClusterTaskByPathID(pathId, dataSQL);
                  if (clusterTaskList != null) {
                     for (ClusterTask clusterTask : clusterTaskList) {
                        if (clusterTask.getTogatherStatus() == 2) {
                           throw new VieAppServiceException("该路径下正在聚类，删除失败！");
                        }
                     }

                     for (ClusterTask clusterTaskx : clusterTaskList) {
                        this.clusterDB.deleteClusterTask(clusterTaskx);
                     }
                  }

                  List<Cluster> clusterList = this.clusterDB.queryClusterByPathID(pathId, dataSQL);
                  if (clusterList != null) {
                     for (Cluster cluster : clusterList) {
                        this.clusterDB.deleteCluster(cluster);
                     }
                  }

                  this.topicDB.deleteTopicPath(topicPath);
               }
            } catch (VieAppServiceException var12) {
               logger.error("该路径下正在聚类，删除失败！", var12);
               throw new VieAppServiceException("该路径下正在聚类，删除失败！", var12);
            } catch (Exception var13) {
               logger.error("内部异常", var13);
               throw new VieAppServiceException("内部异常", var13);
            }
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public String getHotviewById(HotViewRequest request) throws VieAppServiceException {
      if (request != null && null != request.getClusterId()) {
         Long targetId = Long.parseLong(request.getClusterId());
         int ifLone = request.getIfLone();
         String batchId = request.getBatchId();
         Integer dataType = request.getDataType() == null ? 2 : request.getDataType();

         try {
            return this.clusterDB.getHotviewById(targetId, dataType, ifLone, batchId);
         } catch (Exception var7) {
            logger.error("内部异常", var7);
            throw new VieAppServiceException("内部异常", var7);
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空或参数不对");
      }
   }

   public int createCluster(CreateClusterRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getPid()
         && !"".equals(request.getPid())
         && null != request.getCid()
         && !"".equals(request.getCid())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())) {
         TopicPath topicPath = this.topicDB.fetchTopicPathById(Integer.parseInt(request.getPid()));
         if (topicPath != null) {
            int topicId = topicPath.getTopic().getTopicId();
            TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(topicId));
            if (topicInfo == null) {
               throw new VieAppServiceException("该专题已被删除");
            }
         }

         int ifLone = request.getIfLone();
         String batchId = request.getBatchId();
         Integer dataType = request.getDataType() == null ? 2 : request.getDataType();
         String dataSQL = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
         this.clusterDB.reCreateClu(Long.valueOf(request.getCid()), Long.valueOf(request.getPid()), dataSQL, dataType, ifLone, batchId);

         try {
            long i = this.clusterDB.isCluExist(Long.valueOf(request.getCid()), Long.valueOf(request.getPid()), dataSQL, dataType, ifLone, batchId);
            int r = 0;
            if (i > 0L && !request.isFlushTime()) {
               return 1;
            } else {
               long parentId = Long.valueOf(request.getCid());
               if (parentId > 0L) {
                  r = this.clusterDB.getClusterStatus(parentId, Long.parseLong(request.getPid()), dataSQL, request.isFlushTime(), dataType, ifLone, batchId);
               } else {
                  r = this.clusterDB.getClusterStatus(0L, Long.parseLong(request.getPid()), dataSQL, request.isFlushTime(), dataType, ifLone, batchId);
               }

               if (r == 6) {
                  return 5;
               } else {
                  if (r != 2) {
                     long taskId = this.clusterDB
                        .createCluster(Long.valueOf(request.getCid()), Long.valueOf(request.getPid()), dataSQL, dataType, ifLone, batchId);
                     NotifyTaskRequest notifyTaskRequest = new NotifyTaskRequest();
                     notifyTaskRequest.setDataSource(request.getDataSource());
                     notifyTaskRequest.setTaskId((int)taskId);
                     notifyTaskRequest.setTaskType(1);
                     boolean flag = this.notifyTaskService.notifyTask(notifyTaskRequest);
                  }

                  return 0;
               }
            }
         } catch (Exception var18) {
            logger.error("内部异常", var18);
            throw new VieAppServiceException("内部异常", var18);
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public String flushClusterTime(GetClusterStatusRequest request) throws VieAppServiceException {
      if (null != request && null != request.getPid() && !"".equals(request.getPid()) && null != request.getDataSource() && !"".equals(request.getDataSource())
         )
       {
         TopicPath topicPath = this.topicDB.fetchTopicPathById(Integer.parseInt(request.getPid()));
         if (topicPath != null) {
            int topicId = topicPath.getTopic().getTopicId();
            TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(topicId));
            if (topicInfo == null) {
               logger.error("该专题已被删除");
               throw new VieAppServiceException("该专题已被删除");
            }
         }

         try {
            int ifLone = request.getIfLone();
            String batchId = request.getBatchId();
            Integer dataType = request.getDataType() == null ? 2 : request.getDataType();
            String dataSQL = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
            ClusterTask ct = this.clusterDB.queryClusterTaskByPathID2(Long.valueOf(request.getPid()), dataSQL, dataType, ifLone, batchId);
            String updateTime = "";
            if (ct != null) {
               updateTime = ct.getUpdateTime();
            }

            return updateTime;
         } catch (Exception var9) {
            logger.error("内部异常", var9);
            throw new VieAppServiceException("内部异常", var9);
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public GetPathDataResponse getPathData(GetPathDataRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getTopicId()
         && !"".equals(request.getTopicId())
         && null != request.getPathId()
         && !"".equals(request.getPathId())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())) {
         String topicId = request.getTopicId();
         GetPathDataResponse response = new GetPathDataResponse();
         response.setLimitNum(this.limitNum);
         String sortColumn = request.getSortColumn();
         if ("modelName".equals(sortColumn) || "mark".equals(sortColumn) || "keyword".equals(sortColumn)) {
            sortColumn = "id";
         }

         long totalCount = 0L;

         try {
            int pageNum = request.getPageNum();
            int pageSize = request.getPageSize();
            if (pageNum < 1) {
               pageNum = 1;
            }

            String topicCondition = this.topicDB.getTopicCondition(Integer.valueOf(request.getTopicId()));
            String pathCondition = this.topicPathDB.getPathCondition(Integer.parseInt(request.getPathId()));
            TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(topicId);
            if (topicInfo == null) {
               throw new VieAppServiceException("该专题已被删除");
            } else {
               LinkedHashMap<String, DimensionConfig> dimenMap = this.commonData.getDimensionMap(request.getDataSource());
               List<String> durationList = BuildTopicFilterByAnydri.getDurationDimension(dimenMap);
               boolean exitModel = false;
               List<HashMap<String, String>> pathList = null;
               if (!StringUtils.isNullOrEmpry(pathCondition)) {
                  pathList = (List<HashMap<String, String>>)this.mapper.readValue(pathCondition, List.class);

                  for (HashMap<String, String> map : (List<HashMap<String, String>>)pathList) {
                     if ("model".equals(map.get("type").toString()) || "offLineTagId".equals(map.get("type").toString())) {
                        exitModel = true;
                        break;
                     }
                  }
               }

               List<LinkedHashMap<String, Object>> values = null;
               ColumnInfo columnInfo = this.getColumnMaps(request.getSearchDimension());
               String[] columnArray = columnInfo.getColumnsStr().replace("[", "").replace("]", "").split(",");
               HashMap<String, Set<String>> dataAuth = this.commonData.getDataMapAuth(request.getUserId(), request.getSystemId(), request.getDataSource());
               String tableName = request.getDataSource();
               if (exitModel) {
                  logger.info("getPathData方法spark查询###############");
                  RequestData requestData = new RequestData();
                  if (null != columnArray && columnArray.length > 0) {
                     for (String str : columnArray) {
                        requestData.addColumn(str);
                     }
                  }

                  requestData.setOrderDesc("order by " + sortColumn + " " + request.getSortType() + "");
                  if (!request.isExport()) {
                     requestData.setPageNum(pageNum);
                     requestData.setPageSize(pageSize);
                  }

                  requestData.setTableName(tableName);
                  requestData.addDataAuthToFilterMap(dataAuth);
                  BuildTopicFilterBySpark.groupProcessParam(requestData);
                  BuildTopicFilterBySpark.groupTopicCondRequest(topicCondition, requestData);
                  BuildTopicFilterBySpark.builPathFilter(pathList, dimenMap, requestData);
                  MultipleDataConstant.setMultipleDataSql(requestData, request.getBatchId(), request.getIfLone(), request.getDataType());
                  ResponseData responseData = this.sparkUtils.execute(requestData);
                  if (responseData == null) {
                     logger.error("spark查询出错");
                     response.setTotalCount(0);
                     response.setColumns(columnInfo.getColumnMaps());
                     Preview pre = new Preview();
                     pre.setTotalRows(0);
                     pre.setTotalPages(0);
                     pre.setPageNum(pageNum);
                     pre.setPageSize(pageSize);
                     pre.setRows(null);
                     response.setPreviewList(pre);
                     return response;
                  }

                  totalCount = responseData.getTotalCount();
                  if (totalCount == 0L) {
                     response.setTotalCount(0);
                     response.setColumns(columnInfo.getColumnMaps());
                     Preview pre = new Preview();
                     pre.setTotalRows(0);
                     pre.setTotalPages(0);
                     pre.setPageNum(pageNum);
                     pre.setPageSize(pageSize);
                     pre.setRows(null);
                     response.setPreviewList(pre);
                     return response;
                  }

                  values = responseData.getRsList();
               }

               if (!exitModel) {
                  logger.info("getPathData方法anydrill查询###############");
                  StringBuffer sb = new StringBuffer();
                  String column = "";
                  if (columnInfo != null) {
                     column = columnInfo.getColumnsStr().replace("[", "").replace("]", "");
                  }

                  String dataSql = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
                  String topicConDSql = BuildTopicFilterByAnydri.groupTopicCondByAnyDri(topicCondition);
                  if (StringUtils.isNullOrEmpry(dataSql)) {
                     sb.append("select " + column + " from " + tableName + " where processed = 0" + " and " + topicConDSql);
                  } else {
                     sb.append("select " + column + " from " + tableName + " where processed = 0 and " + dataSql + " and " + topicConDSql);
                  }

                  String pathSql = BuildTopicFilterByAnydri.getPathCond(pathList, dimenMap);
                  if (!"".equals(pathSql)) {
                     sb.append(" and " + pathSql);
                  }

                  String multiDataSql = MultipleDataConstant.getMultipleDataSql(request.getBatchId(), request.getIfLone(), request.getDataType());
                  sb.append(" and " + multiDataSql);
                  if (!request.isExport()) {
                     int beginPage = pageSize * (pageNum - 1) + 1;
                     int endPage = pageNum * pageSize;
                     sb.append(" and rownum between " + beginPage + " and " + endPage + "");
                  }

                  sb.append(" order by " + sortColumn + " " + request.getSortType() + "");
                  String anydrillAddressMain = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress");
                  PreparedStatement pst = new PreparedStatement(anydrillAddressMain.split(";"));
                  logger.info("getpathdata方法sql：{}", sb.toString());
                  ResultSet rs = pst.executeQuery(sb.toString());
                  totalCount = rs.getTotalCount();
                  if (totalCount == 0L) {
                     response.setTotalCount(0);
                     response.setColumns(columnInfo.getColumnMaps());
                     Preview pre = new Preview();
                     pre.setTotalRows(0);
                     pre.setTotalPages(0);
                     pre.setPageNum(pageNum);
                     pre.setPageSize(pageSize);
                     pre.setRows(null);
                     response.setPreviewList(pre);
                     return response;
                  }

                  values = rs.toList();
               }

               if (values != null && values.size() != 0) {
                  boolean exceedsLimit = false;
                  if (!request.isExport() && response.getTotalCount() > this.limitNum) {
                     response.setExceedsLimit(true);
                     return response;
                  } else {
                     response.setExceedsLimit(exceedsLimit);
                     AuthorizeInfo authorizeInfo = new AuthorizeInfo();
                     authorizeInfo.setUserId(request.getUserId());
                     authorizeInfo.setDataSource(request.getDataSource());
                     HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
                     String modelInfo = "offLineTagInfo,offLineTagId";
                     String anydrillAddressModel = DynamicEsSource.getEsSourceByType(request.getDataSource(), "modelAnydrillAddress");
                     String[] anydrillAddressModels = anydrillAddressModel.split(";");
                     List<DataInfo> list = this.dealModelReturnValue(anydrillAddressModels, values, tableName, modelInfo, modelMap, topicId, durationList);
                     int total = (int)totalCount;
                     response.setTotalCount(total);
                     response.setColumns(columnInfo.getColumnMaps());
                     Preview result = new Preview();
                     result.setTotalRows(total);
                     int count = 0;
                     if (!request.isExport()) {
                        count = total % pageSize == 0 ? total / pageSize : total / pageSize + 1;
                     }

                     result.setTotalPages(count);
                     result.setPageNum(pageNum);
                     result.setPageSize(pageSize);
                     result.setRows(list);
                     response.setPreviewList(result);
                     return response;
                  }
               } else {
                  response.setTotalCount(0);
                  response.setColumns(columnInfo.getColumnMaps());
                  Preview pre = new Preview();
                  pre.setTotalRows(0);
                  pre.setTotalPages(0);
                  pre.setPageNum(pageNum);
                  pre.setPageSize(pageSize);
                  pre.setRows(null);
                  response.setPreviewList(pre);
                  return response;
               }
            }
         } catch (VieCommonException var31) {
            logger.error("getPathData方法内部异常", var31);
            if (1001 == var31.getErrorCode()) {
               throw new VieAppServiceException("该路径存在已下线的模型／已删除的维度，请将其移出路径后重新预览", var31);
            } else {
               throw new VieAppServiceException("内部服务异常", var31);
            }
         } catch (VieAppServiceException var32) {
            throw new VieAppServiceException("该专题已被删除", var32);
         } catch (Exception var33) {
            logger.error("内部服务异常", var33);
            throw new VieAppServiceException("内部服务异常", var33);
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   private List<DataInfo> dealModelReturnValue(
      String[] connectUrl,
      List<LinkedHashMap<String, Object>> data,
      String tableName,
      String modelInfo,
      HashMap<String, String> allModels,
      String topicId,
      List<String> durationList
   ) throws JsonParseException, JsonMappingException, IOException {
      if (data != null && data.size() != 0) {
         Map<String, List<Map<String, Object>>> modelMap = null;
         String idStr = IndexConstants.getIdStr(data);
         if (!StringUtils.isNullOrEmpry(idStr)) {
            modelMap = IndexConstants.getModelInforById(connectUrl, tableName, modelInfo, idStr, null);
         }

         List<String> ids = new ArrayList<>();

         for (LinkedHashMap<String, Object> result : data) {
            ids.add(result.get("id").toString());
         }

         List<TopicMarkInfo> topicMarkInfos = null;
         if (!StringUtils.isNullOrEmpry(topicId)) {
            topicMarkInfos = this.topicMarkDB.getMarkInfoList(ids, Integer.valueOf(topicId));
         }

         Map<String, TopicMarkInfo> topicMarkInfosMap = new HashMap<>();
         if (topicMarkInfos != null) {
            for (TopicMarkInfo t : topicMarkInfos) {
               topicMarkInfosMap.put(t.getTelephoneId(), t);
            }
         }

         List<DataInfo> realData = new ArrayList<>();
         StringBuffer modelNames = null;
         List<Map<String, Object>> keywords = null;
         LinkedHashMap<String, Object> dataMaps = null;

         for (LinkedHashMap<String, Object> eachCd : data) {
            dataMaps = new LinkedHashMap<>();
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
                        value = this.df.format(Double.parseDouble(String.valueOf(value)) / 1000.0);
                     } else {
                        value = 0.0;
                     }
                  }

                  dataMaps.put(key, value);
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
            DataInfo dataInfo = new DataInfo();
            TopicMarkInfo topicMarkInfo = topicMarkInfosMap.get(id);
            boolean isHaveMark = false;
            String mark = null;
            Integer markId = null;
            if (topicMarkInfo != null) {
               isHaveMark = true;
               mark = topicMarkInfo.getMark();
               markId = topicMarkInfo.getId();
            }

            dataMaps.put("remark", mark);
            dataMaps.put("isHaveRemark", isHaveMark);
            dataMaps.put("markId", markId);
            dataMaps.put("modelName", modelNames);
            dataInfo.setKeywordInfos(keywords);
            dataInfo.setDataMaps(dataMaps);
            realData.add(dataInfo);
         }

         return realData;
      } else {
         return null;
      }
   }

   public MarkQueryResponse getMarkData(MarkQueryRequest request) throws VieAppServiceException {
      MarkQueryResponse response = new MarkQueryResponse();
      String sortColumn = request.getSortColumn();
      String topicId = request.getTopicId();
      String dataSource = request.getDataSource();
      String sort = "mark".equals(sortColumn) ? request.getSortType() : "";

      try {
         List<TopicMarkInfo> topicMarkInfoList = this.topicMarkDB.getMarkByTopic(Integer.valueOf(request.getTopicId()), sort);
         TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(topicId);
         if (topicInfo == null) {
            logger.error("该专题已被删除");
            throw new VieAppServiceException("该专题已被删除");
         } else {
            String tableName = request.getDataSource();
            if (null != topicMarkInfoList && topicMarkInfoList.size() != 0) {
               ColumnInfo columnInfo = this.getColumnMaps2(request.getSearchDimension(), dataSource);
               if ("keyword".equals(sortColumn) || "modelName".equals(sortColumn) || "mark".equals(sortColumn)) {
                  sortColumn = "id";
               }

               StringBuffer sb = new StringBuffer();
               String dataSql = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
               if (StringUtils.isNullOrEmpry(dataSql)) {
                  sb.append("select " + columnInfo.getColumnsStr() + " from " + tableName + " where processed = 0 ");
               } else {
                  sb.append("select " + columnInfo.getColumnsStr() + " from " + tableName + " where processed = 0 and " + dataSql);
               }

               StringBuffer idStr = new StringBuffer();
               int i = 0;

               for (int length = topicMarkInfoList.size(); i < length; i++) {
                  idStr.append("'" + topicMarkInfoList.get(i).getTelephoneId() + "'" + ",");
               }

               sb.append(" and id in (" + idStr.substring(0, idStr.length() - 1) + ")");
               sb.append(" and" + MultipleDataConstant.getMultipleDataSql(request.getBatchId(), request.getIfLone(), request.getDataType()));
               i = request.getPageSize() * (request.getPageNum() - 1) + 1;
               int endPage = request.getPageNum() * request.getPageSize();
               sb.append(" and rownum between " + i + " and " + endPage + "");
               sb.append(" order by " + sortColumn + " " + request.getSortType() + "");
               String anydrillAddressMain = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress");
               PreparedStatement pst = new PreparedStatement(anydrillAddressMain.split(";"));
               logger.info("getMarkData方法sql：{}", sb.toString());
               ResultSet rs = pst.executeQuery(sb.toString());
               List<LinkedHashMap<String, Object>> values = rs.toList();
               if (values != null && values.size() != 0) {
                  AuthorizeInfo authorizeInfo = new AuthorizeInfo();
                  authorizeInfo.setUserId(request.getUserId());
                  authorizeInfo.setDataSource(request.getDataSource());
                  HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
                  LinkedHashMap<String, DimensionConfig> dimenMap = this.commonData.getDimensionMap(request.getDataSource());
                  List<String> durationList = BuildTopicFilterByAnydri.getDurationDimension(dimenMap);
                  String modelInfo = "offLineTagId,offLineTagInfo";
                  String anydrillAddressModel = DynamicEsSource.getEsSourceByType(request.getDataSource(), "modelAnydrillAddress");
                  String[] anydrillAddressModels = anydrillAddressModel.split(";");
                  List<DataInfo> list = this.dealModelReturnValue(anydrillAddressModels, values, tableName, modelInfo, modelMap, topicId, durationList);
                  response.setTotalCount((int)rs.getTotalCount());
                  response.setColumns(columnInfo.getColumnMaps());
                  PreviewList previewList = new PreviewList();
                  previewList.setPageNum(request.getPageNum());
                  previewList.setPageSize(request.getPageSize());
                  previewList.setSortColumn(sortColumn);
                  previewList.setRows(list);
                  response.setPreviewList(previewList);
                  return response;
               } else {
                  response.setTotalCount(0);
                  response.setColumns(columnInfo.getColumnMaps());
                  PreviewList previewList = new PreviewList();
                  previewList.setPageNum(request.getPageNum());
                  previewList.setPageSize(request.getPageSize());
                  previewList.setSortColumn(sortColumn);
                  previewList.setRows(null);
                  response.setPreviewList(previewList);
                  return response;
               }
            } else {
               logger.info("查询结果为空");
               return null;
            }
         }
      } catch (VieAppServiceException var29) {
         logger.error("该专题已被删除", var29);
         throw new VieAppServiceException("该专题已被删除", var29);
      } catch (Exception var30) {
         logger.error("内部异常", var30);
         throw new VieAppServiceException("内部异常", var30);
      }
   }

   public void deleteMark(DeleteMarkRequest request) throws VieAppServiceException {
      if (request != null && !StringUtils.isNullOrEmpry(request.getMarkIds())) {
         String[] markIdArray = request.getMarkIds().split(",");
         List<Integer> markList = new ArrayList<>();

         for (String str : markIdArray) {
            markList.add(Integer.valueOf(str));
         }

         boolean isDelete = this.topicMarkDB.deleteMarkInfo(markList);
         if (!isDelete) {
            logger.error("删除标记库出现异常");
            throw new VieAppServiceException("删除标记库出现异常");
         }
      } else {
         logger.error("参数不能为空");
         throw new VieAppServiceException("参数不能为空");
      }
   }

   public int getLoadCount(LoadCountRequest request) throws VieAppServiceException {
      try {
         StringBuffer sqlBuffer = new StringBuffer();
         String topicCondition = this.topicDB.getTopicCondition(Integer.valueOf(request.getTopicId()));
         TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(request.getTopicId());
         if (StringUtils.isNullOrEmpry(topicCondition)) {
            logger.error("专题条件为空");
            return 0;
         } else if (topicInfo == null) {
            throw new VieAppServiceException("该专题已被删除");
         } else {
            String topicConDSql = BuildTopicFilterByAnydri.groupTopicCondByAnyDri(topicCondition);
            String dataSQL = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
            if (!StringUtils.isNullOrEmpry(dataSQL)) {
               sqlBuffer.append(
                  "select sum(idCounter) as hitCount from " + request.getDataSource() + " where processed=0 and " + topicConDSql + " and " + dataSQL
               );
            } else {
               sqlBuffer.append("select sum(idCounter) as hitCount from " + request.getDataSource() + " where processed=0 and " + topicConDSql);
            }

            sqlBuffer.append(" and " + MultipleDataConstant.getMultipleDataSql(request.getBatchId(), request.getIfLone(), request.getDataType()));
            logger.info("getLoadCount方法sql：{}", sqlBuffer.toString());
            int count = 0;
            String anydrillAddressMain = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress");
            PreparedStatement pst = new PreparedStatement(anydrillAddressMain.split(";"));
            ResultSet rs = pst.executeQuery(sqlBuffer.toString());
            if (rs != null && rs.getTotalCount() != 0L && rs.hasRow()) {
               rs.next();
               if (rs.getValue("hitCount") != null) {
                  count = (int)((Double)rs.getValue("hitCount")).doubleValue();
               }
            }

            return count;
         }
      } catch (VieAppServiceException var11) {
         logger.error("该专题已被删除", var11);
         throw new VieAppServiceException("该专题已被删除", var11);
      } catch (Exception var12) {
         logger.error("内部服务异常", var12);
         throw new VieAppServiceException("内部服务异常", var12);
      }
   }

   public List<PathDimension> searchPathDim(TopicPathRequest request) throws VieAppServiceException {
      try {
         List<PathDimension> dimList = this.topicDB.queryAllPathDim(request.getPathId());
         return (List<PathDimension>)(null == dimList ? new ArrayList<>() : dimList);
      } catch (Exception var3) {
         logger.error("内部服务异常", var3);
         throw new VieAppServiceException("内部服务异常", var3);
      }
   }

   public int addPathDim(DimensionRequest request) throws VieAppServiceException {
      try {
         String dimId = request.getDimId();
         if (!StringUtils.isNullOrEmpry(dimId)) {
            PathDimension pathDimension = this.topicDB.queryPathDimById(Integer.parseInt(dimId));
            if (pathDimension != null) {
               pathDimension.setPathDimJson(request.getPathDim());
               this.topicDB.addPathDim(pathDimension);
               return pathDimension.getId();
            } else {
               logger.error("维度编辑失败");
               throw new VieAppServiceException("维度编辑失败");
            }
         } else {
            PathDimension pathDimension = new PathDimension();
            pathDimension.setPathDimJson(request.getPathDim());
            pathDimension.setPathId(request.getPathId());
            this.topicDB.addPathDim(pathDimension);
            return pathDimension.getId();
         }
      } catch (Exception var4) {
         logger.error("内部服务异常", var4);
         throw new VieAppServiceException("内部服务异常", var4);
      }
   }

   public void deletePathDim(DimensionRequest request) throws VieAppServiceException {
      try {
         PathDimension pathDimension = this.topicDB.queryPathDimById(Integer.parseInt(request.getDimId()));
         if (pathDimension != null) {
            this.topicDB.deletePathDim(pathDimension);
         } else {
            logger.error("维度删除失败");
            throw new VieAppServiceException("维度删除失败");
         }
      } catch (Exception var3) {
         logger.error("内部服务异常", var3);
         throw new VieAppServiceException("内部服务异常", var3);
      }
   }

   public void editBatchPath(PathInfoRequest request) throws VieAppServiceException {
      if (null != request && null != request.getPathInfo() && !"".equals(request.getPathInfo())) {
         String topicId = request.getTopicId();
         List<HashMap<String, Object>> pathList = null;

         try {
            pathList = this.jsonToMap(request.getPathInfo());
         } catch (Exception var11) {
            logger.error("路径信息转换异常", var11);
            throw new VieAppServiceException("路径信息转换异常", var11);
         }

         List<Object> pathNameList = new ArrayList<>();
         Set<Object> pathNameSet = new HashSet<>();
         if (pathList.size() != 0) {
            for (HashMap<String, Object> pathMap : (List<HashMap<String, Object>>)pathList) {
               pathNameList.add(pathMap.get("pathName"));
            }

            pathNameSet.addAll(pathNameList);
            if (pathNameSet.size() != pathNameList.size()) {
               logger.error("路径名称存在重复");
               throw new VieAppServiceException("路径名称存在重复");
            } else {
               try {
                  for (HashMap<String, Object> pathMap : (List<HashMap<String, Object>>)pathList) {
                     String pName = String.valueOf(pathMap.get("pathName"));
                     String pId = String.valueOf(pathMap.get("pathID"));
                     String param = String.valueOf(pathMap.get("pathPram"));
                     this.editOnePath(pName, pId, param, topicId);
                  }
               } catch (Exception var12) {
                  logger.error("内部服务异常", var12);
                  throw new VieAppServiceException("内部服务异常", var12);
               }
            }
         }
      } else {
         logger.error("参数异常");
         throw new VieAppServiceException("参数异常");
      }
   }

   public List<ModelStatusResponse> getModelStatus(ModelStatusRequest request) throws VieAppServiceException {
      if (null != request
         && !StringUtils.isNullOrEmpry(request.getModelIds())
         && !StringUtils.isNullOrEmpry(request.getTopicId())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())) {
         List<ModelStatusResponse> result = new ArrayList<>();

         try {
            TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(request.getTopicId());
            if (topicInfo == null) {
               throw new VieAppServiceException("该专题已被删除");
            } else {
               String topicCondition = topicInfo.getTopicCondition();
               List<String> modelIdList = Arrays.asList(request.getModelIds().split(","));
               List<Object[]> resultList = this.modelDB.querySqlModelByID(modelIdList);
               List<ModelInfoDto> modelInfoDtos = new ArrayList<>();
               if (resultList != null && resultList.size() != 0) {
                  for (Object[] modelInfoDto : resultList) {
                     modelInfoDtos.add(this.objectsToModelInfoDto(modelInfoDto));
                  }

                  AuthorizeInfo authorizeInfo = new AuthorizeInfo();
                  authorizeInfo.setDataSource(request.getDataSource());
                  authorizeInfo.setUserId(request.getUserId());
                  HashMap<String, String> dataMap = this.modelApplyService.getAllModelMap(authorizeInfo);
                  String tmpModelList = "";

                  for (ModelInfoDto mid : modelInfoDtos) {
                     if (mid.getModelStatus() == 2) {
                        tmpModelList = tmpModelList + mid.getModelId() + ",";
                     } else {
                        ModelStatusResponse modelStatusResponse = new ModelStatusResponse();
                        String modelID = mid.getModelId() + "";
                        int modelStatus = dataMap.containsKey(modelID) ? mid.getModelStatus() : -2;
                        modelStatusResponse.setModelId(modelID);
                        modelStatusResponse.setStatus(modelStatus);
                        modelStatusResponse.setValue(null);
                        result.add(modelStatusResponse);
                     }
                  }

                  String topicConDSql = BuildTopicFilterByAnydri.groupTopicCondByAnyDri(topicCondition);
                  if (!"".equals(tmpModelList)) {
                     tmpModelList = tmpModelList.substring(0, tmpModelList.length() - 1);
                     String sql = "select count(id) as id_count,offLineTagId from "
                        + IndexConstants.getModelTableName(request.getDataSource())
                        + " where processed=0 and "
                        + topicConDSql
                        + " and offLineTagId in ("
                        + tmpModelList
                        + ") group by offLineTagId";
                     logger.info("getModelStatus方法sql：{}", sql);
                     String anydrillAddressModel = DynamicEsSource.getEsSourceByType(request.getDataSource(), "modelAnydrillAddress");
                     PreparedStatement pst = new PreparedStatement(anydrillAddressModel.split(";"));
                     ResultSet rs = pst.executeQuery(sql);
                     List<LinkedHashMap<String, Object>> linkedHashMapList = rs.toList();
                     if (linkedHashMapList.size() > 0) {
                        for (LinkedHashMap<String, Object> data : linkedHashMapList) {
                           ModelStatusResponse modelStatusResponse = new ModelStatusResponse();
                           String modelID = data.get("offLineTagId").toString();
                           int modelStatus = dataMap.containsKey(modelID) ? 2 : -2;
                           modelStatusResponse.setModelId(modelID);
                           modelStatusResponse.setStatus(modelStatus);
                           result.add(modelStatusResponse);
                        }
                     } else {
                        ModelStatusResponse modelStatusResponse = new ModelStatusResponse();
                        modelStatusResponse.setModelId(request.getModelIds());
                        modelStatusResponse.setStatus(2);
                        result.add(modelStatusResponse);
                     }
                  }
               }

               return result;
            }
         } catch (VieAppServiceException var22) {
            logger.error("该专题已被删除", var22);
            throw new VieAppServiceException("该专题已被删除", var22);
         } catch (Exception var23) {
            logger.error("内部服务异常", var23);
            throw new VieAppServiceException("内部服务异常", var23);
         }
      } else {
         logger.error("参数异常");
         throw new VieAppServiceException("参数异常");
      }
   }

   public MarkQueryResponse getTogatherData(TogetherPathRequest request) throws VieAppServiceException {
      MarkQueryResponse response = new MarkQueryResponse();

      try {
         String sortColumn = request.getSortColumn();
         String sortType = request.getSortType();
         String batchId = request.getBatchId();
         Integer dataType = request.getDataType();
         Integer ifLone = request.getIfLone();
         String userId = request.getUserId();
         String systemId = request.getSystemId();
         String togatherId = request.getTogatherId();
         String topicId = request.getTopicId();
         int pageNum = request.getPageNum();
         int pageSize = request.getPageSize();
         TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(topicId);
         if (topicInfo == null) {
            throw new VieAppServiceException("该专题已被删除");
         } else {
            String tableName = request.getDataSource();
            String searchDimension = request.getSearchDimension();
            ColumnInfo columnInfo = this.getTogatherListColumnMaps(searchDimension, tableName);
            TopicTogatherInfo topicTogatherInfo = this.topicTogatherDB.getTogatherInfo(Long.valueOf(togatherId));
            StringBuffer sqlBuffer = new StringBuffer();
            String columns = columnInfo.getColumnsStr().replace("[", "").replace("]", "");
            sqlBuffer.append("select " + columns + " from " + tableName).append(" where processed=0 ");
            String telephonIDS = "";
            if (topicTogatherInfo != null) {
               telephonIDS = topicTogatherInfo.getTelephonIdInfo();
            }

            if (StringUtils.isNotNullAndEmpry(telephonIDS)) {
               sqlBuffer.append(" and id in(" + telephonIDS + ") ");
            }

            if (!request.isExport()) {
               sqlBuffer.append(" and (rownum between " + (pageSize * (pageNum - 1) + 1) + " and " + pageNum * pageSize + ")");
            }

            String dataSql = this.commonData.getDataSql(userId, systemId, request.getDataSource());
            if (!StringUtils.isNullOrEmpry(dataSql)) {
               sqlBuffer.append(" and " + dataSql);
            }

            String multipleSql = MultipleDataConstant.getMultipleDataSql(batchId, ifLone, dataType);
            if (!StringUtils.isNullOrEmpry(multipleSql)) {
               sqlBuffer.append(" and " + multipleSql);
            }

            if ("keyword".equals(sortColumn) || "modelName".equals(sortColumn) || "mark".equals(sortColumn)) {
               sortColumn = "id";
            }

            sqlBuffer.append(" order by " + sortColumn + " " + sortType);
            logger.info("the excute sql is " + sqlBuffer.toString());
            String anydrillAddressMain = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress");
            PreparedStatement pst = new PreparedStatement(anydrillAddressMain.split(";"));
            ResultSet rs = pst.executeQuery(sqlBuffer.toString());
            if (request.isExport() && rs.getTotalCount() > request.getLimitNum()) {
               response.setPassLimit(true);
               return response;
            } else if (rs != null && rs.getTotalCount() != 0L) {
               List<LinkedHashMap<String, Object>> result = rs.toList();
               AuthorizeInfo authorizeInfo = new AuthorizeInfo();
               authorizeInfo.setUserId(request.getUserId());
               authorizeInfo.setDataSource(request.getDataSource());
               HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
               LinkedHashMap<String, DimensionConfig> dimenMap = this.commonData.getDimensionMap(request.getDataSource());
               List<String> durationList = BuildTopicFilterByAnydri.getDurationDimension(dimenMap);
               String modelInfo = "offLineTagId,offLineTagInfo";
               String anydrillAddressModel = DynamicEsSource.getEsSourceByType(request.getDataSource(), "modelAnydrillAddress");
               String[] anydrillAddressModels = anydrillAddressModel.split(";");
               List<DataInfo> list = this.dealModelReturnValue(anydrillAddressModels, result, tableName, modelInfo, modelMap, topicId, durationList);
               if (StringUtils.isNotNullAndEmpry(telephonIDS)) {
                  response.setTotalCount(telephonIDS.split(",").length);
               }

               response.setColumns(columnInfo.getColumnMaps());
               PreviewList previewList = new PreviewList();
               previewList.setPageNum(request.getPageNum());
               previewList.setPageSize(request.getPageSize());
               previewList.setSortColumn(sortColumn);
               previewList.setRows(list);
               response.setPreviewList(previewList);
               return response;
            } else {
               response.setTotalCount(0);
               response.setColumns(columnInfo.getColumnMaps());
               PreviewList previewList = new PreviewList();
               previewList.setPageNum(request.getPageNum());
               previewList.setPageSize(request.getPageSize());
               previewList.setSortColumn(sortColumn);
               previewList.setRows(null);
               response.setPreviewList(previewList);
               return response;
            }
         }
      } catch (VieAppServiceException var37) {
         logger.error("该专题已被删除", var37);
         throw new VieAppServiceException("该专题已被删除", var37);
      } catch (Exception var38) {
         logger.error("内部服务异常", var38);
         throw new VieAppServiceException("内部服务异常", var38);
      }
   }

   private void editOnePath(String pathName, String pathId, String pataParam, String topicId) throws VieAppServiceException {
      TopicPath oldPath = this.topicDB.fetchTopicPathByPathName(pathName, topicId);
      if (oldPath != null && oldPath.getPathId() != Integer.parseInt(pathId)) {
         logger.error("路径 {} 已存在", pathName);
         throw new VieAppServiceException("路径 " + pathName + " 已存在");
      } else {
         TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(topicId);
         if (topicInfo != null) {
            TopicPath topicPath = this.topicDB.fetchTopicPathById(Integer.parseInt(pathId));
            if (topicPath != null) {
               topicPath.setPathName(pathName);
               topicPath.setPathCondition(pataParam);
               topicPath.setTopic(topicInfo);
               this.topicDB.saveTopicPath(topicPath);
            } else {
               logger.error("路径不存在");
               throw new VieAppServiceException("路径不存在");
            }
         } else {
            logger.error("该专题已被删除");
            throw new VieAppServiceException("该专题已被删除");
         }
      }
   }

   private int addPathInfo(String topicId, String pathName, String pathPram) throws VieAppServiceException {
      TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(topicId);
      if (topicInfo != null) {
         TopicPath oldPath = this.topicDB.fetchTopicPathByPathName(pathName, topicId);
         if (oldPath != null) {
            logger.error("路径 {} 已存在", pathName);
            throw new VieAppServiceException("路径 " + pathName + " 已存在");
         } else {
            TopicPath topicPath = new TopicPath();
            topicPath.setPathName(pathName);
            topicPath.setPathCondition(pathPram);
            topicPath.setTopic(topicInfo);
            this.topicDB.saveTopicPath(topicPath);
            return topicPath.getPathId();
         }
      } else {
         logger.error("该专题已被删除");
         throw new VieAppServiceException("该专题已被删除");
      }
   }

   private int editPathInfo(String topicId, String pathName, String pathPram, String pathId) throws VieAppServiceException {
      TopicPath oldPath = this.topicDB.fetchTopicPathByPathName(pathName, topicId);
      if (oldPath != null && oldPath.getPathId() != Integer.parseInt(pathId)) {
         logger.error("路径 {} 已存在", pathName);
         throw new VieAppServiceException("路径 " + pathName + " 已存在");
      } else {
         TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(topicId);
         if (topicInfo != null) {
            TopicPath topicPath = this.topicDB.fetchTopicPathById(Integer.parseInt(pathId));
            if (topicPath != null) {
               topicPath.setPathName(pathName);
               topicPath.setPathCondition(pathPram);
               topicPath.setTopic(topicInfo);
               this.topicDB.saveTopicPath(topicPath);
               return topicPath.getPathId();
            } else {
               logger.error("路径不存在！");
               throw new VieAppServiceException("路径不存在！");
            }
         } else {
            logger.error("该专题已被删除");
            throw new VieAppServiceException("该专题已被删除");
         }
      }
   }

   public List<HashMap<String, Object>> jsonToMap(String jsonString) throws Exception {
      List<HashMap<String, Object>> jsonList = new ArrayList<>();
      if (jsonString != null && !"".equals(jsonString)) {
         ObjectMapper mapper = new ObjectMapper();
         return (List<HashMap<String, Object>>)mapper.readValue(jsonString, List.class);
      } else {
         return jsonList;
      }
   }

   private boolean isContain(List<HashMap<String, Object>> topicCond, List<DimensionAuth> dataCond) {
      boolean isContain = false;

      try {
         if (topicCond != null && topicCond.size() != 0 && dataCond != null && dataCond.size() != 0) {
            HashMap<String, ArrayList<String>> data = new HashMap<>();

            for (DimensionAuth cd : dataCond) {
               data.put(cd.getEnglishName(), (ArrayList<String>)cd.getValue());
            }

            boolean isMulSel = false;
            boolean isContainDataD = false;

            for (HashMap<String, Object> cd : (List<HashMap<String, Object>>)topicCond) {
               String type = cd.get(CommonParams.topicConDType).toString().trim();
               switch (DimType.valueOf(type)) {
                  case mulSel:
                     String filed = cd.get(CommonParams.topicConDKey).toString();
                     List<String> currentList = data.get(filed);
                     if (currentList != null) {
                        List<String> reportC = (ArrayList)cd.get(CommonParams.topicConDValue);
                        reportC.retainAll(currentList);
                        if (reportC.size() > 0) {
                           return true;
                        }

                        isContainDataD = true;
                     }

                     isMulSel = true;
                  case mulEqu:
                  case offLineTagId:
                  case radio:
                  case range:
                  case timeDim:
                  case timeRange:
               }
            }

            isContain = isMulSel ? isContain : true;
            isContain = isContainDataD ? isContain : true;
         } else {
            isContain = true;
         }
      } catch (Exception var13) {
         logger.error(ExceptionUtil.getStackTrace(var13));
      }

      return isContain;
   }

   private ColumnInfo getColumnMaps(String searchDimension) {
      ColumnInfo columnInfo = new ColumnInfo();
      columnInfo.getColumns().add("id");
      columnInfo.getColumns().add("duration");
      if (searchDimension != null && !"".equals(searchDimension)) {
         List list = (List)JSON.parseObject(searchDimension, List.class);
         if (list != null && list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
               JSONObject jsonObject = (JSONObject)list.get(i);
               ColumnMap columnMap = new ColumnMap(jsonObject.getString("columnName"), jsonObject.getString("column"));
               columnInfo.getColumnMaps().add(columnMap);
               logger.info(columnMap.getColumn());
               columnInfo.getColumns().add(columnMap.getColumn());
            }
         }
      }

      columnInfo.getColumnMaps().add(new ColumnMap("模型", "modelName"));
      columnInfo.getColumnMaps().add(new ColumnMap("关键词", "keyword"));
      columnInfo.getColumnMaps().add(new ColumnMap("人工测听标记", "mark"));
      List<String> columns = columnInfo.getColumns();
      StringBuffer columnsStr = new StringBuffer();
      if (null != columns && columns.size() != 0) {
         for (String str : columns) {
            columnsStr.append(str + ",");
         }

         columnInfo.setColumnsStr(columnsStr.subSequence(0, columnsStr.length() - 1).toString());
      }

      return columnInfo;
   }

   private ColumnInfo getColumnMaps2(String searchDimension, String dataSource) {
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
      if (searchDimension != null && !"".equals(searchDimension)) {
         List list = (List)JSON.parseObject(searchDimension, List.class);
         if (list != null && list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
               JSONObject jsonObject = (JSONObject)list.get(i);
               ColumnMap columnMap = new ColumnMap(jsonObject.getString("columnName"), jsonObject.getString("column"));
               columnInfo.getColumnMaps().add(columnMap);
               logger.info(columnMap.getColumn());
               columnInfo.getColumns().add(columnMap.getColumn());
            }
         }
      }

      columnInfo.getColumnMaps().add(new ColumnMap("模型", "modelName"));
      columnInfo.getColumnMaps().add(new ColumnMap("关键词", "keyword"));
      columnInfo.getColumnMaps().add(new ColumnMap("人工测听标记", "mark"));
      List<String> columns = columnInfo.getColumns();
      StringBuffer columnsStr = new StringBuffer();
      if (null != columns && columns.size() != 0) {
         for (String str : columns) {
            columnsStr.append(str + ",");
         }

         columnInfo.setColumnsStr(columnsStr.subSequence(0, columnsStr.length() - 1).toString());
      }

      return columnInfo;
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

   private ColumnInfo getTogatherListColumnMaps(String searchDimension, String dataSource) {
      ColumnInfo columnInfo = new ColumnInfo();
      int insightType = IndexConstants.getInsightType(dataSource);
      if (insightType == 0) {
         columnInfo.getColumnMaps().add(new ColumnMap("录音编号", "id"));
      } else if (insightType == 1) {
         columnInfo.getColumnMaps().add(new ColumnMap("任务号", "id"));
      }

      columnInfo.getColumnMaps().add(new ColumnMap("通话时长", "duration"));
      columnInfo.getColumns().add("id");
      columnInfo.getColumns().add("duration");
      if (searchDimension != null && !"".equals(searchDimension)) {
         List list = (List)JSON.parseObject(searchDimension, List.class);
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
      List<String> columns = columnInfo.getColumns();
      StringBuffer columnsStr = new StringBuffer();
      if (null != columns && columns.size() != 0) {
         for (String str : columns) {
            columnsStr.append(str + ",");
         }

         columnInfo.setColumnsStr(columnsStr.subSequence(0, columnsStr.length() - 1).toString());
      }

      return columnInfo;
   }

   public int getClusterStatus(GetClusterStatusRequest request) throws Exception {
      if (null != request
         && null != request.getPid()
         && !"".equals(request.getPid())
         && null != request.getCid()
         && !"".equals(request.getCid())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())) {
         TopicPath topicPath = this.topicDB.fetchTopicPathById(Integer.parseInt(request.getPid()));
         if (topicPath != null) {
            int topicId = topicPath.getTopic().getTopicId();
            TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(topicId));
            if (topicInfo == null) {
               logger.error("该专题已被删除");
               throw new VieAppServiceException("该专题已被删除");
            }
         }

         int ifLone = request.getIfLone();
         String batchId = request.getBatchId();
         Integer dataType = request.getDataType() == null ? 2 : request.getDataType();
         String dataSQL = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
         int result = 0;

         try {
            int i = this.clusterDB.getClusterStatus2(Long.valueOf(request.getCid()), Long.valueOf(request.getPid()), dataSQL, dataType, ifLone, batchId);
            byte var12;
            switch (i) {
               case 0:
                  var12 = 2;
                  break;
               case 1:
               case 2:
               default:
                  var12 = 0;
                  break;
               case 3:
                  var12 = 1;
                  break;
               case 4:
                  var12 = 3;
                  break;
               case 5:
                  var12 = 4;
                  break;
               case 6:
                  var12 = 5;
            }

            return var12;
         } catch (Exception var9) {
            logger.error("操作失败", var9);
            throw new Exception("操作失败", var9);
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public List<ClusterInfo> getClusterInfoFromPia(GetClusterStatusRequest request) throws VieAppServiceException {
      if (null != request
         && null != request.getPid()
         && !"".equals(request.getPid())
         && null != request.getCid()
         && !"".equals(request.getCid())
         && null != request.getDataSource()
         && !"".equals(request.getDataSource())) {
         try {
            TopicPath topicPath = this.topicDB.fetchTopicPathById(Integer.parseInt(request.getPid()));
            if (topicPath != null) {
               int topicId = topicPath.getTopic().getTopicId();
               TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(topicId));
               if (topicInfo == null) {
                  throw new VieAppServiceException("该专题已被删除");
               }
            }

            int ifLone = request.getIfLone();
            String batchId = request.getBatchId();
            Integer dataType = request.getDataType() == null ? 2 : request.getDataType();
            String dataSQL = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
            String text = this.clusterDB
               .getClusterInfoFromPia(Long.valueOf(request.getCid()), Long.valueOf(request.getPid()), dataSQL, dataType, ifLone, batchId);
            if (StringUtils.isNullOrEmpry(text)) {
               logger.info("没有聚类数据");
            }

            return JSONArray.parseArray(text, ClusterInfo.class);
         } catch (VieAppServiceException var9) {
            logger.error("该专题已被删除", var9);
            throw new VieAppServiceException("该专题已被删除", var9);
         } catch (Exception var10) {
            logger.error("内部异常", var10);
            throw new VieAppServiceException("内部异常", var10);
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public List<ClusterInfo> getClusterInfoFromPath(GetClusterStatusRequest request) throws VieAppServiceException {
      if (null != request && null != request.getCid() && !"".equals(request.getCid())) {
         try {
            TopicPath topicPath = this.topicDB.fetchTopicPathById(Integer.parseInt(request.getPid()));
            if (topicPath != null) {
               int topicId = topicPath.getTopic().getTopicId();
               TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(topicId));
               if (topicInfo == null) {
                  throw new VieAppServiceException("该专题已被删除");
               }
            }

            int ifLone = request.getIfLone();
            String batchId = request.getBatchId();
            Integer dataType = request.getDataType() == null ? 2 : request.getDataType();
            String dataSQL = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
            String text = this.clusterDB.getClusterInfoFromPath(Long.valueOf(request.getCid()), dataSQL, dataType, ifLone, batchId);
            return JSONArray.parseArray(text, ClusterInfo.class);
         } catch (VieAppServiceException var9) {
            logger.error("该专题已被删除", var9);
            throw new VieAppServiceException("该专题已被删除", var9);
         } catch (Exception var10) {
            logger.error("内部异常", var10);
            throw new VieAppServiceException("内部异常", var10);
         }
      } else {
         logger.error("入参不能为空");
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public void setTopicDB(TopicDB topicDB) {
      this.topicDB = topicDB;
   }

   public void setModelDB(ModelDB modelDB) {
      this.modelDB = modelDB;
   }

   public void setTopicMarkDB(TopicMarkDB topicMarkDB) {
      this.topicMarkDB = topicMarkDB;
   }

   public void setCommonData(CommonData commonData) {
      this.commonData = commonData;
   }

   public void setClusterDB(ClusterDB clusterDB) {
      this.clusterDB = clusterDB;
   }

   public void setTopicPathDB(TopicPathDB topicPathDB) {
      this.topicPathDB = topicPathDB;
   }

   public void setTopicTogatherDB(TopicTogatherDB topicTogatherDB) {
      this.topicTogatherDB = topicTogatherDB;
   }

   public void setModelApplyService(ModelApplyService modelApplyService) {
      this.modelApplyService = modelApplyService;
   }

   public void setSparkUtils(SparkUtils sparkUtils) {
      this.sparkUtils = sparkUtils;
   }

   public Integer getLimitNum() {
      return this.limitNum;
   }

   public void setLimitNum(Integer limitNum) {
      this.limitNum = limitNum;
   }

   public NotifyTaskService getNotifyTaskService() {
      return this.notifyTaskService;
   }

   public void setNotifyTaskService(NotifyTaskService notifyTaskService) {
      this.notifyTaskService = notifyTaskService;
   }
}
