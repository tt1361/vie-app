package com.iflytek.vie.app.provider.impl.dimension;

import com.iflytek.vie.app.api.dimension.CallHotWordService;
import com.iflytek.vie.app.api.dimension.FetchHotWordService;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.dimension.HotWordRankResponse;
import com.iflytek.vie.app.pojo.topic.CallHotWordTaskRequest;
import com.iflytek.vie.app.pojo.topic.CallHotWordTaskResponse;
import com.iflytek.vie.app.provider.common.CommonDataPlatform;
import com.iflytek.vie.app.provider.database.HotWordStatDB;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallHotWordServiceImpl implements CallHotWordService {
   private final Logger logger = LoggerFactory.getLogger(CallHotWordServiceImpl.class);
   private CommonDataPlatform commonData;
   private HotWordStatDB hotWordStatDb;
   private FetchHotWordService fetchHotWordService;

   public CallHotWordTaskResponse addCallHotWordTask(CallHotWordTaskRequest callHotWordTaskRequest) throws ViePlatformServiceException {
      new CallHotWordTaskResponse();
      if (callHotWordTaskRequest != null) {
         try {
            Integer dataType = callHotWordTaskRequest.getDataType();
            String batchId = callHotWordTaskRequest.getBatchId();
            if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getUserId())) {
               this.logger.error("入参userId不能为空");
               throw new ViePlatformServiceException("入参userId不能为空");
            } else if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getSystemId())) {
               this.logger.error("入参systemId不能为空");
               throw new ViePlatformServiceException("入参systemId不能为空");
            } else if (callHotWordTaskRequest.getAnalyzeType() == null) {
               this.logger.error("入参analyzeType不能为空");
               throw new ViePlatformServiceException("入参analyzeType不能为空");
            } else if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getAnalyzeIds())) {
               this.logger.error("入参analyzeIds不能为空");
               throw new ViePlatformServiceException("入参analyzeIds不能为空");
            } else if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getStartTime())) {
               this.logger.error("入参startTime不能为空");
               throw new ViePlatformServiceException("入参startTime不能为空");
            } else if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getEndTime())) {
               this.logger.error("入参endTime不能为空");
               throw new ViePlatformServiceException("入参endTime不能为空");
            } else if (callHotWordTaskRequest.getFlag() == null) {
               this.logger.error("入参flag不能为空");
               throw new ViePlatformServiceException("入参flag不能为空");
            } else {
               if (dataType == null) {
                  dataType = 2;
               }

               if (StringUtils.isNullOrEmpry(batchId)) {
                  batchId = "0";
               }

               Map<String, Object> param = new HashMap<>();
               param.put("analyzeType", callHotWordTaskRequest.getAnalyzeType());
               param.put("analyzeIds", callHotWordTaskRequest.getAnalyzeIds());
               param.put("startTime", callHotWordTaskRequest.getStartTime());
               param.put("endTime", callHotWordTaskRequest.getEndTime());
               param.put("dataType", dataType);
               param.put("batchId", batchId);
               param.put("ifLone", callHotWordTaskRequest.getIfLone());
               param.put("flag", callHotWordTaskRequest.getFlag());
               String sql = this.commonData
                  .getDataSql(callHotWordTaskRequest.getUserId(), callHotWordTaskRequest.getSystemId(), callHotWordTaskRequest.getDataSource());
               return this.hotWordStatDb.createCallHotWordTask(sql, param);
            }
         } catch (Exception var7) {
            this.logger.error("【通过ES的查询条件创建热词任务】失败", var7);
            throw new ViePlatformServiceException("服务器内部错误", var7);
         }
      } else {
         throw new ViePlatformServiceException("参数传递有误");
      }
   }

   public int getCallHotWordTaskStatus(CallHotWordTaskRequest callHotWordTaskRequest) throws ViePlatformServiceException {
      int statusId = 0;
      if (callHotWordTaskRequest != null) {
         try {
            Integer dataType = callHotWordTaskRequest.getDataType();
            if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getUserId())) {
               this.logger.error("入参userId不能为空");
               throw new ViePlatformServiceException("入参userId不能为空");
            } else if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getSystemId())) {
               this.logger.error("入参systemId不能为空");
               throw new ViePlatformServiceException("入参systemId不能为空");
            } else if (callHotWordTaskRequest.getTaskId() == null) {
               this.logger.error("入参taskId不能为空");
               throw new ViePlatformServiceException("入参taskId不能为空");
            } else {
               if (dataType == null) {
                  dataType = 2;
               }

               String sql = this.commonData
                  .getDataSql(callHotWordTaskRequest.getUserId(), callHotWordTaskRequest.getSystemId(), callHotWordTaskRequest.getDataSource());
               return this.hotWordStatDb
                  .getCallHotWordTaskStatus(
                     sql, callHotWordTaskRequest.getTaskId(), dataType, callHotWordTaskRequest.getBatchId(), callHotWordTaskRequest.getIfLone()
                  );
            }
         } catch (Exception var5) {
            this.logger.error("【通过任务id查询任务的执行状态】失败", var5);
            throw new ViePlatformServiceException("服务器内部错误", var5);
         }
      } else {
         throw new ViePlatformServiceException("参数传递有误");
      }
   }

   public List<HotWordRankResponse> getCallHotWordStat(CallHotWordTaskRequest callHotWordTaskRequest) throws ViePlatformServiceException {
      List<HotWordRankResponse> result = new ArrayList<>();
      if (callHotWordTaskRequest == null) {
         throw new ViePlatformServiceException("参数传递有误");
      } else {
         try {
            Long taskId = callHotWordTaskRequest.getTaskId();
            String wordType = callHotWordTaskRequest.getWordType();
            String countType = callHotWordTaskRequest.getCountType();
            String keyWord = callHotWordTaskRequest.getKeyWord();
            Integer pageNum = callHotWordTaskRequest.getPageNum();
            Integer pageSize = callHotWordTaskRequest.getPageSize();
            if (StringUtils.isNullOrEmpry(wordType) || StringUtils.isNullOrEmpry(countType) || taskId == null || pageNum == null || pageSize == null) {
               this.logger.error("入参传递有误");
               throw new ViePlatformServiceException("入参传递有误");
            }

            Map<String, Object> map = new HashMap<>();
            map.put("taskId", taskId);
            map.put("wordType", wordType);
            map.put("countType", countType);
            keyWord = keyWord == null ? "" : keyWord;
            map.put("keyWord", keyWord);
            map.put("pageNum", pageNum);
            map.put("pageSize", pageSize);
            result = this.fetchHotWordService.fetchCallHotWord(map);
         } catch (Exception var10) {
            this.logger.error("[通过任务id查询任务的热词分析结果]失败", var10);
         }

         return result;
      }
   }

   public CallHotWordTaskResponse getCallHotWordTaskAndStatus(CallHotWordTaskRequest callHotWordTaskRequest) throws ViePlatformServiceException {
      new CallHotWordTaskResponse();
      if (callHotWordTaskRequest != null) {
         try {
            Integer dataType = callHotWordTaskRequest.getDataType();
            if (dataType == null) {
               dataType = 2;
            }

            if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getUserId())) {
               this.logger.error("入参userId不能为空");
               throw new ViePlatformServiceException("入参userId不能为空");
            } else if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getSystemId())) {
               this.logger.error("入参systemId不能为空");
               throw new ViePlatformServiceException("入参systemId不能为空");
            } else if (callHotWordTaskRequest.getAnalyzeType() == null) {
               this.logger.error("入参analyzeType不能为空");
               throw new ViePlatformServiceException("入参analyzeType不能为空");
            } else if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getAnalyzeIds())) {
               this.logger.error("入参analyzeIds不能为空");
               throw new ViePlatformServiceException("入参analyzeIds不能为空");
            } else if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getStartTime())) {
               this.logger.error("入参startTime不能为空");
               throw new ViePlatformServiceException("入参startTime不能为空");
            } else if (StringUtils.isNullOrEmpry(callHotWordTaskRequest.getEndTime())) {
               this.logger.error("入参endTime不能为空");
               throw new ViePlatformServiceException("入参endTime不能为空");
            } else {
               Map<String, Object> param = new HashMap<>();
               param.put("analyzeType", callHotWordTaskRequest.getAnalyzeType());
               param.put("analyzeIds", callHotWordTaskRequest.getAnalyzeIds());
               param.put("startTime", callHotWordTaskRequest.getStartTime());
               param.put("endTime", callHotWordTaskRequest.getEndTime());
               param.put("dataType", dataType);
               param.put("batchId", callHotWordTaskRequest.getBatchId());
               param.put("ifLone", callHotWordTaskRequest.getIfLone());
               String sql = this.commonData
                  .getDataSql(callHotWordTaskRequest.getUserId(), callHotWordTaskRequest.getSystemId(), callHotWordTaskRequest.getDataSource());
               return this.hotWordStatDb.getCallHotWordTaskAndStatus(sql, param);
            }
         } catch (Exception var6) {
            this.logger.error("getCallHotWordTaskAndStatus serviceImpl报错", var6);
            throw new ViePlatformServiceException("getCallHotWordTaskAndStatus serviceImpl报错", var6);
         }
      } else {
         throw new ViePlatformServiceException("参数传递有误");
      }
   }

   public CommonDataPlatform getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonDataPlatform commonData) {
      this.commonData = commonData;
   }

   public HotWordStatDB getHotWordStatDb() {
      return this.hotWordStatDb;
   }

   public void setHotWordStatDb(HotWordStatDB hotWordStatDb) {
      this.hotWordStatDb = hotWordStatDb;
   }

   public FetchHotWordService getFetchHotWordService() {
      return this.fetchHotWordService;
   }

   public void setFetchHotWordService(FetchHotWordService fetchHotWordService) {
      this.fetchHotWordService = fetchHotWordService;
   }
}
