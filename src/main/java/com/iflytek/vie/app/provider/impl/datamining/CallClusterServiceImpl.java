package com.iflytek.vie.app.provider.impl.datamining;

import com.alibaba.fastjson.JSONArray;
import com.iflytek.vie.app.api.datamining.CallClusterService;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.datamining.CallClusterTaskRequest;
import com.iflytek.vie.app.pojo.datamining.CallClusterTaskResponse;
import com.iflytek.vie.app.pojo.datamining.ClusterInfo;
import com.iflytek.vie.app.pojo.datamining.TogetherPathRequest;
import com.iflytek.vie.app.pojo.datamining.TogetherPathResponse;
import com.iflytek.vie.app.provider.common.CommonDataPlatform;
import com.iflytek.vie.app.provider.database.CallClusterDB;
import com.iflytek.vie.utils.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallClusterServiceImpl implements CallClusterService {
   private final Logger logger = LoggerFactory.getLogger(CallClusterServiceImpl.class);
   private CommonDataPlatform commonData;
   private CallClusterDB callClusterDB;

   public CallClusterTaskResponse createCallClusterTask(CallClusterTaskRequest request) throws ViePlatformServiceException {
      CallClusterTaskResponse response = new CallClusterTaskResponse();
      int status = 0;
      if (request != null) {
         try {
            Integer dataType = request.getDataType();
            String batchId = request.getBatchId();
            if (StringUtils.isNullOrEmpry(request.getUserId())) {
               this.logger.error("入参userId不能为空");
               throw new ViePlatformServiceException("入参userId不能为空");
            }

            if (StringUtils.isNullOrEmpry(request.getSystemId())) {
               this.logger.error("入参systemId不能为空");
               throw new ViePlatformServiceException("入参systemId不能为空");
            }

            if (request.getAnalyzeType() == null) {
               this.logger.error("入参analyzeType不能为空");
               throw new ViePlatformServiceException("入参analyzeType不能为空");
            }

            if (StringUtils.isNullOrEmpry(request.getAnalyzeIds())) {
               this.logger.error("入参analyzeIds不能为空");
               throw new ViePlatformServiceException("入参analyzeIds不能为空");
            }

            if (StringUtils.isNullOrEmpry(request.getBeginTime())) {
               this.logger.error("入参startTime不能为空");
               throw new ViePlatformServiceException("入参startTime不能为空");
            }

            if (StringUtils.isNullOrEmpry(request.getEndTime())) {
               this.logger.error("入参endTime不能为空");
               throw new ViePlatformServiceException("入参endTime不能为空");
            }

            if (request.getParentId() == null) {
               this.logger.error("入参parentId不能为空");
               throw new ViePlatformServiceException("入参parentId不能为空");
            }

            if (dataType == null) {
               dataType = 2;
            }

            if (StringUtils.isNullOrEmpry(batchId)) {
               batchId = "0";
            }

            Map<String, Object> param = new HashMap<>();
            param.put("analyzeType", request.getAnalyzeType());
            param.put("analyzeIds", request.getAnalyzeIds());
            param.put("beginTime", request.getBeginTime());
            param.put("endTime", request.getEndTime());
            param.put("dataType", dataType);
            param.put("batchId", batchId);
            param.put("ifLone", request.getIfLone());
            if (request.getIfLone() == null) {
               param.put("ifLone", 0);
            }

            param.put("togetherId", request.getParentId());
            String dataAuthSql = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
            this.callClusterDB.reCreateClusterTask(dataAuthSql, param);
            response = this.callClusterDB.getClusterTaskStatus(dataAuthSql, param, response);
            if (request.isFlushTime()) {
               this.callClusterDB.deletePreCluster(dataAuthSql, param);
               response = this.callClusterDB.getClusterTaskStatus(dataAuthSql, param, response);
            }

            if (response.getStatus() <= 0) {
               response = this.callClusterDB.createClusterTask(dataAuthSql, param, response, request.isFlushTime());
            }
         } catch (Exception var8) {
            this.logger.info("获取聚类数据失败！");
            throw new ViePlatformServiceException("获取聚类数据失败！", var8);
         }
      }

      return response;
   }

   public Integer getCallClusterTaskStatus(CallClusterTaskRequest request) throws ViePlatformServiceException {
      int status = 0;
      if (request != null) {
         try {
            Integer dataType = request.getDataType();
            String batchId = request.getBatchId();
            if (StringUtils.isNullOrEmpry(request.getUserId())) {
               this.logger.error("入参userId不能为空");
               throw new ViePlatformServiceException("入参userId不能为空");
            }

            if (StringUtils.isNullOrEmpry(request.getSystemId())) {
               this.logger.error("入参systemId不能为空");
               throw new ViePlatformServiceException("入参systemId不能为空");
            }

            if (request.getAnalyzeType() == null) {
               this.logger.error("入参analyzeType不能为空");
               throw new ViePlatformServiceException("入参analyzeType不能为空");
            }

            if (StringUtils.isNullOrEmpry(request.getAnalyzeIds())) {
               this.logger.error("入参analyzeIds不能为空");
               throw new ViePlatformServiceException("入参analyzeIds不能为空");
            }

            if (StringUtils.isNullOrEmpry(request.getBeginTime())) {
               this.logger.error("入参startTime不能为空");
               throw new ViePlatformServiceException("入参startTime不能为空");
            }

            if (StringUtils.isNullOrEmpry(request.getEndTime())) {
               this.logger.error("入参endTime不能为空");
               throw new ViePlatformServiceException("入参endTime不能为空");
            }

            if (request.getParentId() == null) {
               this.logger.error("入参parentId不能为空");
               throw new ViePlatformServiceException("入参parentId不能为空");
            }

            if (dataType == null) {
               dataType = 2;
            }

            if (StringUtils.isNullOrEmpry(batchId)) {
               batchId = "0";
            }

            Map<String, Object> param = new HashMap<>();
            param.put("analyzeType", request.getAnalyzeType());
            param.put("analyzeIds", request.getAnalyzeIds());
            param.put("beginTime", request.getBeginTime());
            param.put("endTime", request.getEndTime());
            param.put("dataType", dataType);
            param.put("batchId", batchId);
            param.put("ifLone", request.getIfLone());
            if (request.getIfLone() == null) {
               param.put("ifLone", 0);
            }

            param.put("togetherId", request.getParentId());
            String dataAuthSql = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
            status = this.callClusterDB.getClusterTaskStatus(dataAuthSql, param);
         } catch (Exception var7) {
            this.logger.info("获取聚类状态失败！");
            throw new ViePlatformServiceException("获取聚类状态失败！", var7);
         }

         return status;
      } else {
         this.logger.info("来电原因聚类参数为空！");
         throw new ViePlatformServiceException("来电原因聚类参数为空！");
      }
   }

   public List<ClusterInfo> getTogetherByTaskId(Long taskId, Long parentId) throws ViePlatformServiceException {
      if (null != taskId && taskId >= 0L && null != parentId && parentId >= 0L) {
         try {
            String text = this.callClusterDB.getTogetherByTaskId(taskId, parentId);
            if (StringUtils.isNullOrEmpry(text)) {
               text = "[]";
            }

            return JSONArray.parseArray(text, ClusterInfo.class);
         } catch (Exception var4) {
            this.logger.info("通过条件钻取获取聚类饼状图数据失败");
            throw new ViePlatformServiceException("通过条件钻取获取聚类饼状图数据失败", var4);
         }
      } else {
         this.logger.info("入参不能为空或参数不对");
         throw new ViePlatformServiceException("入参不能为空或参数不对");
      }
   }

   public List<ClusterInfo> getTogetherByPath(int togetherId) throws ViePlatformServiceException {
      if (togetherId < 0) {
         this.logger.info("入参参数不对");
         throw new ViePlatformServiceException("入参参数不对");
      } else {
         try {
            String text = this.callClusterDB.getTogetherByPath(togetherId);
            if (StringUtils.isNullOrEmpry(text)) {
               text = "[]";
            }

            return JSONArray.parseArray(text, ClusterInfo.class);
         } catch (Exception var3) {
            this.logger.info("通过点击路径获取聚类饼状图数据失败");
            throw new ViePlatformServiceException("通过点击路径获取聚类饼状图数据失败", var3);
         }
      }
   }

   public String getHotviewByTogetherId(int togetherId) throws ViePlatformServiceException {
      if (togetherId < 0) {
         this.logger.info("入参参数不对");
         throw new ViePlatformServiceException("入参参数不对");
      } else {
         try {
            return this.callClusterDB.getHotviewByTogetherId(togetherId);
         } catch (Exception var3) {
            this.logger.info("获取聚类点击的热点数据失败");
            throw new ViePlatformServiceException("获取聚类点击的热点数据失败", var3);
         }
      }
   }

   public int saveTogetherPath(TogetherPathRequest togetherPathRequest) throws ViePlatformServiceException {
      if (null != togetherPathRequest
         && togetherPathRequest.getTaskId() >= 0
         && togetherPathRequest.getTogetherId() >= 0
         && !StringUtils.isNullOrEmpry(togetherPathRequest.getTogetherIndex())
         && !StringUtils.isNullOrEmpry(togetherPathRequest.getTogetherName())) {
         try {
            return this.callClusterDB.saveTogetherPath(togetherPathRequest);
         } catch (Exception var3) {
            this.logger.info("保存聚类展示的点击路径失败");
            throw new ViePlatformServiceException("保存聚类展示的点击路径失败", var3);
         }
      } else {
         this.logger.info("入参参数不对");
         throw new ViePlatformServiceException("入参参数不对");
      }
   }

   public List<TogetherPathResponse> getTogetherPath(int taskId) throws ViePlatformServiceException {
      if (taskId < 0) {
         this.logger.info("入参参数不对");
         throw new ViePlatformServiceException("入参参数不对");
      } else {
         try {
            return this.callClusterDB.getTogetherPath(taskId);
         } catch (Exception var3) {
            this.logger.info("获取聚类点击的热点数据失败");
            throw new ViePlatformServiceException("获取聚类点击的热点数据失败", var3);
         }
      }
   }

   public Logger getLogger() {
      return this.logger;
   }

   public CommonDataPlatform getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonDataPlatform commonData) {
      this.commonData = commonData;
   }

   public CallClusterDB getCallClusterDB() {
      return this.callClusterDB;
   }

   public void setCallClusterDB(CallClusterDB callClusterDB) {
      this.callClusterDB = callClusterDB;
   }
}
