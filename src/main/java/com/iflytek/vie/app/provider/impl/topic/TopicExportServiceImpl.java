package com.iflytek.vie.app.provider.impl.topic;

import com.iflytek.vie.app.api.topic.FunnelService;
import com.iflytek.vie.app.api.topic.TopicExportService;
import com.iflytek.vie.app.api.topic.TopicHotWordTaskService;
import com.iflytek.vie.app.api.topic.TopicService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.topic.BaseExportRequest;
import com.iflytek.vie.app.pojo.topic.ExportFunnelRequest;
import com.iflytek.vie.app.pojo.topic.ExportFunnelResponse;
import com.iflytek.vie.app.pojo.topic.FunnelCallRequest;
import com.iflytek.vie.app.pojo.topic.FunnelCallResponse;
import com.iflytek.vie.app.pojo.topic.FunnelTableResponse;
import com.iflytek.vie.app.pojo.topic.GetPathDataRequest;
import com.iflytek.vie.app.pojo.topic.GetPathDataResponse;
import com.iflytek.vie.app.pojo.topic.HotWordRankResponse;
import com.iflytek.vie.app.pojo.topic.HotWordTaskRequest;
import com.iflytek.vie.app.pojo.topic.MarkQueryResponse;
import com.iflytek.vie.app.pojo.topic.TogetherPathRequest;
import com.iflytek.vie.app.pojo.topic.TopicExportResponse;
import com.iflytek.vie.app.pojo.topic.TopicInfo;
import com.iflytek.vie.app.pojo.topic.TopicPath;
import com.iflytek.vie.app.pojo.topicgroup.ColumnMap;
import com.iflytek.vie.app.pojo.topicgroup.DataInfo;
import com.iflytek.vie.app.provider.database.TopicDB;
import com.iflytek.vie.app.provider.database.TopicPathDB;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.collections.CollectionUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicExportServiceImpl implements TopicExportService {
   private FunnelService funnelService;
   private TopicHotWordTaskService topicHotWordTaskService;
   private static final Logger logger = LoggerFactory.getLogger(TopicExportServiceImpl.class);
   private TopicDB topicDB;
   private TopicPathDB topicPathDB;
   private TopicService topicService;
   private Integer limitNum;

   public ExportFunnelResponse exportFunnel(ExportFunnelRequest params) throws VieAppServiceException {
      if (params.getTopicId() != null && params.getPathId() != null && !StringUtils.isNullOrEmpry(params.getInputTime())) {
         ExportFunnelResponse exportResponse = new ExportFunnelResponse();
         exportResponse.setLimitNum(this.limitNum);

         try {
            FunnelCallRequest funnelCallRequest = new FunnelCallRequest();
            if (params.getAllPathFlag() != null) {
               funnelCallRequest.setAllPathFlag(params.getAllPathFlag());
            }

            funnelCallRequest.setBatchId(params.getBatchId());
            List<HashMap<String, Object>> conditionList = null;
            if (!StringUtils.isNullOrEmpry(params.getCondition())) {
               funnelCallRequest.setCondition(params.getCondition());
               String[] conList = params.getCondition().split("and;");
               String conStr = conList[conList.length - 1];
               conditionList = this.jsonToMap(conStr);
            }

            exportResponse.setConditionList(conditionList);
            funnelCallRequest.setDataSource(params.getDataSource());
            funnelCallRequest.setDataType(params.getDataType());
            funnelCallRequest.setPathId(params.getPathId());
            funnelCallRequest.setTopicId(params.getTopicId());
            funnelCallRequest.setIfLone(params.getIfLone());
            if (params.getIsNumData() != null) {
               funnelCallRequest.setIsNumData(params.getIsNumData());
            }

            if (params.getPageNum() != null) {
               funnelCallRequest.setPageNum(params.getPageNum());
            }

            if (params.getPageSize() != null) {
               funnelCallRequest.setPageSize(params.getPageSize());
            }

            funnelCallRequest.setSearchDimension(params.getSearchDimension());
            funnelCallRequest.setSortColumn(params.getSortColumn());
            funnelCallRequest.setSortType(params.getSortType());
            funnelCallRequest.setSystemId(params.getSystemId());
            funnelCallRequest.setUserId(params.getUserId());
            funnelCallRequest.setPageNum(1);
            funnelCallRequest.setPageSize(this.limitNum);
            Map<String, Object> pathMap = this.getExportDataOfPath(
               String.valueOf(params.getTopicId()), params.getInputTime(), String.valueOf(params.getPathId()), params.getPathDim()
            );
            boolean isPathChange = Boolean.valueOf(pathMap.get("isPathChange").toString());
            if (isPathChange) {
               throw new VieAppServiceException("专题路径信息发生变化，请保存专题路径信息后导出！");
            } else {
               exportResponse.setPathMap(pathMap);
               FunnelTableResponse funnelTableResponse = null;
               if (params.getAllPathFlag() != null) {
                  funnelTableResponse = this.funnelService.getFunnelTable(funnelCallRequest);
               }

               exportResponse.setFunnelTableResponse(funnelTableResponse);
               FunnelCallResponse funnelCallResponse = null;
               if (params.getIsNumData() != null) {
                  funnelCallResponse = this.funnelService.getFunnelList(funnelCallRequest);
               }

               exportResponse.setFunnelCallResponse(funnelCallResponse);
               return exportResponse;
            }
         } catch (Exception var9) {
            logger.error("导出漏斗dubbo接口出错", var9);
            return exportResponse;
         }
      } else {
         throw new VieAppServiceException("入参不能为空");
      }
   }

   public TopicExportResponse getExportDataOfCluster(TogetherPathRequest request) throws VieAppServiceException {
      TopicExportResponse response = new TopicExportResponse();

      try {
         request.setExport(true);
         request.setLimitNum(this.limitNum);
         response.setLimitNum(this.limitNum);
         MarkQueryResponse result = this.topicService.getTogatherData(request);
         this.getExportDataOfCall(
            response,
            request.getTopicId(),
            request.getInputTime(),
            result.getPreviewList().getRows(),
            result.getColumns(),
            request.getPathId(),
            request.getPathDim(),
            "聚类分析"
         );
         if (result.isPassLimit()) {
            response.setPassLimit(true);
            return response;
         } else {
            return response;
         }
      } catch (VieAppServiceException var4) {
         logger.error("获取导出聚类分析数据getExportDataOfCluster接口出错", var4);
         throw new VieAppServiceException("获取导出聚类分析数据异常", var4);
      }
   }

   public Map<String, Object> getExportDataOfPath(String topicId, String inputTime, String pathId, String pathDim) throws VieAppServiceException {
      Map<String, Object> path = new HashMap<>();

      try {
         TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(topicId);
         if (topicInfo == null) {
            logger.error("获取专题信息为空");
            throw new VieAppServiceException("获取专题信息为空");
         } else {
            TopicPath topicPath = this.topicPathDB.getPathById(Integer.parseInt(pathId));
            Map<String, Object> pathMap = new HashMap<>();
            pathMap.put("pathType", pathDim);
            pathMap.put("pathName", topicPath.getPathName());
            List<Map<String, Object>> pahtDataLst = new ArrayList<>();
            pahtDataLst.add(pathMap);
            path.put("topicName", topicInfo.getTopicName());
            path.put("topicPath", inputTime);
            path.put("pathData", pahtDataLst);
            path.put("isPathChange", false);
            return path;
         }
      } catch (Exception var10) {
         logger.error("获取导出专题路径信息getExportDataOfPath方法内部异常", var10);
         throw new VieAppServiceException("获取导出专题路径信息异常", var10);
      }
   }

   private boolean comparePath(int pathId, String pathDim) throws VieAppServiceException {
      boolean isPathChange = false;
      TopicPath topicPath = this.topicPathDB.getPathById(pathId);
      if (topicPath == null) {
         logger.error("获取专题路径信息为空");
         throw new VieAppServiceException("获取专题路径信息为空");
      } else {
         List<HashMap<String, Object>> pathCond = null;
         StringBuffer pathData = new StringBuffer();
         StringBuffer typeStr = null;
         String pathStr = "";

         try {
            pathCond = this.jsonToMap(topicPath.getPathCondition());
            if (pathCond.size() > 0 && pathCond != null) {
               for (HashMap<String, Object> map : (List<HashMap<String, Object>>)pathCond) {
                  typeStr = new StringBuffer();
                  if (map.containsKey("name")) {
                     if (map.get("isNegate").toString().equals("1")) {
                        typeStr.append("!");
                     }

                     typeStr.append(map.get("name").toString());
                     typeStr.append("[");
                     typeStr.append(map.get("count").toString());
                     typeStr.append("]");
                     pathData.append(typeStr).append("/");
                  }
               }
            }

            pathStr = pathData.length() > 0 ? pathData.deleteCharAt(pathData.lastIndexOf("/")).toString() : "";
            if (!pathDim.equals(pathStr)) {
               isPathChange = true;
            }

            return isPathChange;
         } catch (Exception var11) {
            logger.error("比较专题路径信息是否发生变化comparePath方法内部异常", var11);
            throw new VieAppServiceException("比较专题路径信息异常", var11);
         }
      }
   }

   private void getExportDataOfCall(
      TopicExportResponse response,
      String topicId,
      String inputTime,
      List<DataInfo> dataInfo,
      List<ColumnMap> columnList,
      String pathId,
      String pathDim,
      String modulName
   ) throws VieAppServiceException {
      try {
         response.setTopicPathMap(this.getExportDataOfPath(topicId, inputTime, pathId, pathDim));
         response.setColumnList(columnList);
         response.setPassLimit(false);
         List<Map<String, Object>> dataList = new ArrayList<>();
         List<Map<String, Object>> list = null;
         LinkedHashMap<String, Object> dataMap = null;
         if (dataInfo != null && dataInfo.size() > 0) {
            for (DataInfo data : dataInfo) {
               String keyWord = "";

               for (Map<String, Object> map : data.getKeywordInfos()) {
                  keyWord = keyWord + map.get("word").toString() + " ";
               }

               dataMap = data.getDataMaps();
               Map<String, Object> resultMap = new HashMap<>();
               resultMap.put("keyword", keyWord.length() > 0 ? keyWord.substring(0, keyWord.length() - 1) : "");
               resultMap.put("modelName", dataMap.get("modelName"));
               resultMap.put("remark", dataMap.get("remark"));

               for (ColumnMap columnMap : columnList) {
                  if (!columnMap.getColumn().equals("keyword")
                     && !columnMap.getColumn().equals("modelName")
                     && !columnMap.getColumn().equals("remark")
                     && !columnMap.getColumn().equals("mark")) {
                     resultMap.put(columnMap.getColumn(), dataMap.get(columnMap.getColumn()));
                  }
               }

               dataList.add(resultMap);
            }

            response.setDataList(dataList);
         }
      } catch (Exception var18) {
         logger.error("获取导出专题路径信息getExportDataOfCall方法内部异常", var18);
         throw new VieAppServiceException("获取导出专题路径信息异常", var18);
      }
   }

   private List<ColumnMap> getNewColumnList(List<ColumnMap> columnList) {
      Set<String> set = new HashSet<>();
      List<ColumnMap> newColumnList = new ArrayList<>();

      for (ColumnMap cp : columnList) {
         if (set.add(cp.getColumnName())) {
            newColumnList.add(cp);
         }
      }

      Collections.sort(newColumnList, new Comparator<ColumnMap>() {
         public int compare(ColumnMap o1, ColumnMap o2) {
            int map1value = o1.getColumn().length();
            int map2value = o2.getColumn().length();
            return map1value - map2value;
         }
      });
      return newColumnList;
   }

   private List<HashMap<String, Object>> jsonToMap(String jsonString) throws Exception {
      List<HashMap<String, Object>> jsonList = new ArrayList<>();
      if (jsonString != null && !"".equals(jsonString)) {
         ObjectMapper mapper = new ObjectMapper();
         return (List<HashMap<String, Object>>)mapper.readValue(jsonString, List.class);
      } else {
         return jsonList;
      }
   }

   public TopicExportResponse exportTopicBaseData(BaseExportRequest request) throws VieAppServiceException {
      GetPathDataRequest getPathDataRequest = new GetPathDataRequest();
      getPathDataRequest.setBatchId(request.getBatchId());
      getPathDataRequest.setDataSource(request.getDataSource());
      getPathDataRequest.setDataType(request.getDataType());
      getPathDataRequest.setIfLone(request.getIfLone());
      getPathDataRequest.setPathId(request.getPathId());
      getPathDataRequest.setSearchDimension(request.getSearchDimension());
      getPathDataRequest.setSortColumn(request.getSortColumn());
      getPathDataRequest.setSortType(request.getSortType());
      getPathDataRequest.setSystemId(request.getSystemId());
      getPathDataRequest.setTopicId(request.getTopicId());
      getPathDataRequest.setUserId(request.getUserId());
      getPathDataRequest.setExport(true);
      getPathDataRequest.setLimitNum(this.limitNum);
      TopicExportResponse response = new TopicExportResponse();

      try {
         GetPathDataResponse getPathDataResponse = this.topicService.getPathData(getPathDataRequest);
         String moduleName = "基础分析";

         for (ColumnMap map : getPathDataResponse.getColumns()) {
            if (map.getColumn().equals("mark")) {
               map.setColumn("remark");
            }
         }

         this.getExportDataOfCall(
            response,
            request.getTopicId(),
            request.getInputTime(),
            getPathDataResponse.getPreviewList().getRows(),
            getPathDataResponse.getColumns(),
            request.getPathId(),
            request.getPathDim(),
            moduleName
         );
         if (getPathDataResponse.isExceedsLimit()) {
            response.setPassLimit(true);
         }

         return response;
      } catch (Exception var8) {
         logger.error("获取导出基础分析数据exportTopicBaseData接口出错", var8);
         throw new VieAppServiceException("获取导出基础分析数据异常", var8);
      }
   }

   public Map<String, Object> queryHotWordStatForExport(HotWordTaskRequest hotWordTaskRequest) throws VieAppServiceException {
      Map<String, Object> map = new ConcurrentHashMap<>();
      String[] types = new String[]{"focusKwd", "systemKwd"};
      String[] kwTypes = new String[]{"voice", "kwd"};

      for (int i = 0; i < types.length; i++) {
         for (int j = 0; j < kwTypes.length; j++) {
            hotWordTaskRequest.setType(types[i]);
            hotWordTaskRequest.setKwType(kwTypes[j]);
            List<HotWordRankResponse> result = this.topicHotWordTaskService.queryHotWordStat(hotWordTaskRequest);
            if (CollectionUtils.isNotEmpty(result) && result.size() > this.limitNum) {
               logger.error("获取导出专题-热词信息异常，导出数量为" + result.size() + "，超出" + this.limitNum);
               throw new VieAppServiceException("获取导出专题-热词信息异常，导出数量为" + result.size() + "，超出" + this.limitNum);
            }

            map.put(types[i] + "/" + kwTypes[j], result);
         }
      }

      if (hotWordTaskRequest.getTopicId() != null && hotWordTaskRequest.getPathId() != null) {
         Map<String, Object> pathMap = this.getExportDataOfPath(
            hotWordTaskRequest.getTopicId().toString(),
            hotWordTaskRequest.getInputTime(),
            hotWordTaskRequest.getPathId().toString(),
            hotWordTaskRequest.getPathDim()
         );
         map.put("topicPath", pathMap);
      }

      return map;
   }

   public TopicDB getTopicDB() {
      return this.topicDB;
   }

   public void setTopicDB(TopicDB topicDB) {
      this.topicDB = topicDB;
   }

   public void setTopicService(TopicService topicService) {
      this.topicService = topicService;
   }

   public void setFunnelService(FunnelService funnelService) {
      this.funnelService = funnelService;
   }

   public void setLimitNum(Integer limitNum) {
      this.limitNum = limitNum;
   }

   public TopicHotWordTaskService getTopicHotWordTaskService() {
      return this.topicHotWordTaskService;
   }

   public void setTopicHotWordTaskService(TopicHotWordTaskService topicHotWordTaskService) {
      this.topicHotWordTaskService = topicHotWordTaskService;
   }

   public TopicService getTopicService() {
      return this.topicService;
   }

   public Integer getLimitNum() {
      return this.limitNum;
   }

   public FunnelService getFunnelService() {
      return this.funnelService;
   }

   public TopicPathDB getTopicPathDB() {
      return this.topicPathDB;
   }

   public void setTopicPathDB(TopicPathDB topicPathDB) {
      this.topicPathDB = topicPathDB;
   }
}
