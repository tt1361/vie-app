package com.iflytek.vie.app.provider.impl.dimension;

import com.iflytek.vie.api.task.NotifyTaskService;
import com.iflytek.vie.app.api.dimension.HotWordService;
import com.iflytek.vie.app.api.permission.DataAuthService;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.dimension.FocusOrExcludeRequest;
import com.iflytek.vie.app.pojo.dimension.HotWordHistoryRequest;
import com.iflytek.vie.app.pojo.dimension.HotWordHistoryResponse;
import com.iflytek.vie.app.pojo.dimension.HotWordRankRequest;
import com.iflytek.vie.app.pojo.dimension.HotWordRankResponse;
import com.iflytek.vie.app.pojo.dimension.HotWordRequest;
import com.iflytek.vie.app.pojo.permission.ContextRequest;
import com.iflytek.vie.app.pojo.permission.DataResourceAuth;
import com.iflytek.vie.app.pojo.permission.DimensionAuth;
import com.iflytek.vie.app.pojo.topic.HotWordTaskRequest;
import com.iflytek.vie.app.pojo.topic.TopicInfo;
import com.iflytek.vie.app.provider.common.CommonDataPlatform;
import com.iflytek.vie.app.provider.database.HotWordStatDB;
import com.iflytek.vie.app.provider.database.TopicDB;
import com.iflytek.vie.pojo.NotifyTaskRequest;
import com.iflytek.vie.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class HotWordServiceImpl implements HotWordService {
   private final Logger logger = LoggerFactory.getLogger(HotWordServiceImpl.class);
   private TopicDB topicDB;
   private HotWordStatDB hotWordStatDb;
   private CommonDataPlatform commonData;
   private DataAuthService dataAuthService;
   private NotifyTaskService notifyTaskService;
   @Autowired
   private Environment env;

   public boolean addFocusHotWordService(FocusOrExcludeRequest focusOrExcludeRequest) throws ViePlatformServiceException {
      boolean flag = false;

      try {
         if (focusOrExcludeRequest.getWord() != null && !focusOrExcludeRequest.getWord().equals("")) {
            if (this.hotWordStatDb.isFocusKwd(focusOrExcludeRequest.getWord(), focusOrExcludeRequest.getAuthInfo().getDataSource())) {
               this.logger.error("白名单已有此词");
               return flag;
            } else {
               this.hotWordStatDb.addHotWord("false", focusOrExcludeRequest.getWord(), focusOrExcludeRequest.getAuthInfo().getDataSource());
               this.logger.info("插入关注热词成功");
               return true;
            }
         } else {
            this.logger.error("添加关注热词（白名单）输入词为空");
            return flag;
         }
      } catch (Exception var4) {
         this.logger.error("添加关注热词（白名单）服务内部错误");
         throw new ViePlatformServiceException("添加关注热词（白名单）服务内部错误", var4);
      }
   }

   public boolean addExcludeHotWordService(FocusOrExcludeRequest focusOrExcludeRequest) throws ViePlatformServiceException {
      boolean flag = false;

      try {
         if (focusOrExcludeRequest.getWord() != null && !focusOrExcludeRequest.getWord().equals("")) {
            if (this.hotWordStatDb.isExcludeKwd(focusOrExcludeRequest.getWord(), focusOrExcludeRequest.getAuthInfo().getDataSource())) {
               this.logger.error("黑名单已有此词");
               return flag;
            } else {
               this.hotWordStatDb.addHotWord("true", focusOrExcludeRequest.getWord(), focusOrExcludeRequest.getAuthInfo().getDataSource());
               this.logger.info("插入排除热词成功");
               return true;
            }
         } else {
            this.logger.error("添加排除热词输入词为空");
            return flag;
         }
      } catch (Exception var4) {
         this.logger.error("添加排除热词服务内部错误");
         throw new ViePlatformServiceException("添加排除热词服务内部错误", var4);
      }
   }

   public List<String> getHotWordService(HotWordRequest hotWordRequest) throws ViePlatformServiceException {
      List<String> wordList = null;

      try {
         wordList = this.hotWordStatDb.getHotWord(hotWordRequest.isBlack(), hotWordRequest.getWord(), hotWordRequest.getAuthInfo().getDataSource());
         this.logger.info("查询关注热词（排除热词）响应成功");
         return wordList;
      } catch (Exception var4) {
         this.logger.error("查询关注热词（排除热词）服务内部错误");
         throw new ViePlatformServiceException("查询关注热词（排除热词）服务内部错误", var4);
      }
   }

   public boolean deleteHotWordService(HotWordRequest hotWordRequest) throws ViePlatformServiceException {
      boolean flag = false;

      try {
         flag = this.hotWordStatDb.deleteHotWord(hotWordRequest.isBlack(), hotWordRequest.getWord(), hotWordRequest.getAuthInfo().getDataSource());
         this.logger.info("删除黑名单（白名单）热词方法响应正常");
         return flag;
      } catch (Exception var4) {
         this.logger.error("删除黑名单（白名单）热词方法响应异常");
         throw new ViePlatformServiceException("删除黑名单（白名单）热词方法响应异常", var4);
      }
   }

   public List<HotWordHistoryResponse> getHistoryHotWordService(HotWordHistoryRequest hotWordHistoryRequest) throws ViePlatformServiceException {
      List<HotWordHistoryResponse> list = null;

      try {
         if (!hotWordHistoryRequest.getWordType().equals("systemKwd") && !hotWordHistoryRequest.getWordType().equals("focusKwd")) {
            this.logger.error("获取上期数据(热词)输入参数有误");
            return null;
         } else if (!hotWordHistoryRequest.getCountType().equals("kwd") && !hotWordHistoryRequest.getCountType().equals("voice")) {
            this.logger.error("获取上期数据(热词)输入参数有误");
            return null;
         } else {
            Integer dataType = hotWordHistoryRequest.getDataType();
            if (dataType == null) {
               dataType = 2;
            }

            String centerSql = this.getCenter(
               hotWordHistoryRequest.getAutInfo().getUserId(),
               hotWordHistoryRequest.getAutInfo().getSystemId(),
               hotWordHistoryRequest.getDataSource(),
               hotWordHistoryRequest.getDataTypeDimension()
            );
            if (hotWordHistoryRequest.getCenterFlag() == 1) {
               centerSql = " centervalue in ('" + hotWordHistoryRequest.getSelectCenter() + "') ";
            }

            this.logger.info("centerSql =" + centerSql);
            return this.hotWordStatDb
               .fetchHotWordByKwd(
                  hotWordHistoryRequest.getKeyWord(),
                  hotWordHistoryRequest.getWordType(),
                  hotWordHistoryRequest.getCountType(),
                  hotWordHistoryRequest.getSelectTime(),
                  centerSql,
                  hotWordHistoryRequest.getIfLone(),
                  hotWordHistoryRequest.getBatchId(),
                  dataType,
                  hotWordHistoryRequest.getDataSource()
               );
         }
      } catch (Exception var5) {
         this.logger.error("获取上期数据(热词)服务内部错误");
         throw new ViePlatformServiceException("获取上期数据(热词)服务内部错误", var5);
      }
   }

   public List<HotWordRankResponse> getHotWordRankService(HotWordRankRequest hotWordRankRequest) throws ViePlatformServiceException {
      long startTime = System.currentTimeMillis();
      List<HotWordRankResponse> list = null;
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

      try {
         Date startDate = sdf.parse(hotWordRankRequest.getStartTime());
         Date endDate = sdf.parse(hotWordRankRequest.getEndTime());
         if (startDate.getTime() > endDate.getTime()) {
            this.logger.error("入参起始时间不能晚于结束时间");
            return null;
         } else if (!hotWordRankRequest.getWordType().equals("systemKwd") && !hotWordRankRequest.getWordType().equals("focusKwd")) {
            this.logger.error("入参wordType输入有误");
            return null;
         } else if (!hotWordRankRequest.getCountType().equals("kwd") && !hotWordRankRequest.getCountType().equals("voice")) {
            this.logger.error("入参countType输入有误");
            return null;
         } else if (!hotWordRankRequest.getOrderType().equals("percent") && !hotWordRankRequest.getOrderType().equals("range")) {
            this.logger.error("入参orderType输入有误");
            return null;
         } else {
            long time1 = System.currentTimeMillis();
            this.logger.info("time1为-------" + (time1 - startTime));
            String centerSql = this.getCenter(
               hotWordRankRequest.getAutInfo().getUserId(),
               hotWordRankRequest.getAutInfo().getSystemId(),
               hotWordRankRequest.getAutInfo().getDataSource(),
               hotWordRankRequest.getDataTypeDimension()
            );
            long time2 = System.currentTimeMillis();
            this.logger.info("time2为-------" + (time2 - startTime));
            if (hotWordRankRequest.getCenterFlag() == 1) {
               centerSql = " centervalue in ('" + hotWordRankRequest.getSelectCenter() + "') ";
            }

            this.logger.info("centerSql =" + centerSql);
            int totalCount = this.hotWordStatDb.getTotalCount(hotWordRankRequest.getStartTime(), hotWordRankRequest.getDataSource());
            long time3 = System.currentTimeMillis();
            this.logger.info("time3为-------" + (time3 - startTime));
            Integer dataType = hotWordRankRequest.getDataType();
            if (dataType == null) {
               dataType = 2;
            }

            list = this.hotWordStatDb
               .fetchHotWord(
                  hotWordRankRequest.getKeyWord(),
                  hotWordRankRequest.getWordType(),
                  hotWordRankRequest.getCountType(),
                  hotWordRankRequest.getOrderType(),
                  totalCount,
                  hotWordRankRequest.getStartTime(),
                  hotWordRankRequest.getEndTime(),
                  hotWordRankRequest.getPageNum(),
                  hotWordRankRequest.getPageSize(),
                  centerSql,
                  hotWordRankRequest.getIfLone(),
                  hotWordRankRequest.getBatchId(),
                  dataType,
                  hotWordRankRequest.getDataSource()
               );
            long time4 = System.currentTimeMillis();
            this.logger.info("time4为-------" + (time4 - startTime));
            return list;
         }
      } catch (Exception var19) {
         this.logger.error("查询热词排名及变化服务内部错误");
         throw new ViePlatformServiceException("查询热词排名及变化服务内部错误", var19);
      }
   }

   public String getCenter(String userId, String systemId, String dataSource, String dataTypeDimension) throws ViePlatformServiceException {
      String sql = "";

      try {
         ContextRequest request = new ContextRequest();
         request.setUserId(Long.parseLong(userId));
         request.setContextPath(systemId);
         request.setDataSource(dataSource);
         Map<String, DataResourceAuth> funcMap = this.dataAuthService.getDataAuths(request);

         for (String ck : funcMap.keySet()) {
            if (ck.equals(dataSource)) {
               DataResourceAuth dr = funcMap.get(ck);
               if (dr.getEnglishName().equals(dataSource)) {
                  for (DimensionAuth da : dr.getDataDimension()) {
                     if (da.getEnglishName().equals(dataTypeDimension)) {
                        List<String> dataList = da.getValue();
                        if (dataList != null && dataList.size() > 0) {
                           String temp = "";

                           for (String data : dataList) {
                              temp = temp + "," + data;
                           }

                           temp = temp.substring(1).replaceAll(",", "','");
                           sql = " centervalue in('" + temp + "') ";
                           break;
                        }
                     }
                  }
               }
            }
         }

         sql = StringUtils.isNullOrEmpry(sql) ? "1=2" : sql;
      } catch (Exception var18) {
         this.logger.error("获取权限sql异常");
         sql = "1=2";
         throw new ViePlatformServiceException("获取权限sql异常1", var18);
      }

      this.logger.info("集成权限拼接sql为:" + sql);
      return sql;
   }

   public long addHotWordTask(HotWordTaskRequest hotWordTaskRequest) throws ViePlatformServiceException {
      long taskId = 0L;
      if (hotWordTaskRequest != null) {
         try {
            if (hotWordTaskRequest.getUserId() == null || "".equals(hotWordTaskRequest.getUserId())) {
               this.logger.error("创建热词分析任务入参userID不能为空");
               throw new ViePlatformServiceException("创建热词分析任务入参userID不能为空");
            } else if (hotWordTaskRequest.getSystemId() == null || "".equals(hotWordTaskRequest.getSystemId())) {
               this.logger.error("创建热词分析任务入参systemId不能为空");
               throw new ViePlatformServiceException("创建热词分析任务入参systemId不能为空");
            } else if (hotWordTaskRequest.getPathId() == null) {
               this.logger.error("创建热词分析任务入参pathId不能为空");
               throw new ViePlatformServiceException("创建热词分析任务入参pathId不能为空");
            } else if (hotWordTaskRequest.getDataSource() != null && !"".equals(hotWordTaskRequest.getDataSource())) {
               TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(hotWordTaskRequest.getTopicId()));
               if (topicInfo == null) {
                  this.logger.error("该专题已被删除");
                  throw new ViePlatformServiceException("该专题已被删除");
               } else {
                  Integer dataType = hotWordTaskRequest.getDataType();
                  String batchId = hotWordTaskRequest.getBatchId();
                  int ifLone = hotWordTaskRequest.getIfLone();
                  if (dataType == null) {
                     dataType = 2;
                  }

                  if (StringUtils.isNullOrEmpry(batchId)) {
                     batchId = "0";
                  }

                  String sql = this.commonData.getDataSql(hotWordTaskRequest.getUserId(), hotWordTaskRequest.getSystemId(), hotWordTaskRequest.getDataSource());
                  taskId = this.hotWordStatDb.createHotWordTask(sql, hotWordTaskRequest.getPathId(), dataType, batchId, ifLone);
                  NotifyTaskRequest notifyTaskRequest = new NotifyTaskRequest();
                  notifyTaskRequest.setDataSource(hotWordTaskRequest.getDataSource());
                  notifyTaskRequest.setTaskId((int)taskId);
                  notifyTaskRequest.setTaskType(0);
                  boolean flag = this.notifyTaskService.notifyTask(notifyTaskRequest);
                  return taskId;
               }
            } else {
               this.logger.error("创建热词分析任务入参dataSource不能为空");
               throw new ViePlatformServiceException("创建热词分析任务入参dataSource不能为空");
            }
         } catch (ViePlatformServiceException var11) {
            this.logger.error("该专题已被删除", var11);
            throw new ViePlatformServiceException("该专题已被删除", var11);
         } catch (Exception var12) {
            this.logger.error("[创建热词分析任务]失败", var12);
            throw new ViePlatformServiceException("服务内部错误", var12);
         }
      } else {
         throw new ViePlatformServiceException("参数传递有误");
      }
   }

   public int getHotWordTaskStatus(HotWordTaskRequest hotWordTaskRequest) throws ViePlatformServiceException {
      int statusId = 0;
      if (hotWordTaskRequest != null) {
         try {
            if (hotWordTaskRequest.getUserId() == null || "".equals(hotWordTaskRequest.getUserId())) {
               this.logger.error("获取热词分析任务状态入参userID不能为空");
               throw new ViePlatformServiceException("获取热词分析任务状态入参userID不能为空");
            } else if (hotWordTaskRequest.getSystemId() == null || "".equals(hotWordTaskRequest.getSystemId())) {
               this.logger.error("获取热词分析任务状态入参systemId不能为空");
               throw new ViePlatformServiceException("获取热词分析任务状态入参systemId不能为空");
            } else if (hotWordTaskRequest.getPathId() == null) {
               this.logger.error("获取热词分析任务状态入参pathId不能为空");
               throw new ViePlatformServiceException("获取热词分析任务状态入参pathId不能为空");
            } else if (hotWordTaskRequest.getDataSource() != null && !"".equals(hotWordTaskRequest.getDataSource())) {
               TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(hotWordTaskRequest.getTopicId()));
               if (topicInfo == null) {
                  this.logger.error("该专题已被删除");
                  throw new ViePlatformServiceException("该专题已被删除");
               } else {
                  Integer dataType = hotWordTaskRequest.getDataType();
                  String batchId = hotWordTaskRequest.getBatchId();
                  int ifLone = hotWordTaskRequest.getIfLone();
                  if (dataType == null) {
                     dataType = 2;
                  }

                  if (StringUtils.isNullOrEmpry(batchId)) {
                     batchId = "0";
                  }

                  String sql = this.commonData.getDataSql(hotWordTaskRequest.getUserId(), hotWordTaskRequest.getSystemId(), hotWordTaskRequest.getDataSource());
                  return this.hotWordStatDb.getHotWordTaskStatus(sql, hotWordTaskRequest.getPathId(), dataType, batchId, ifLone);
               }
            } else {
               this.logger.error("获取热词分析任务状态入参dataSource不能为空");
               throw new ViePlatformServiceException("获取热词分析任务状态入参dataSource不能为空");
            }
         } catch (ViePlatformServiceException var8) {
            this.logger.error("该专题已被删除", var8);
            throw new ViePlatformServiceException("该专题已被删除", var8);
         } catch (Exception var9) {
            this.logger.error("[获取热词分析任务状态]失败", var9);
            throw new ViePlatformServiceException("服务器内部错误", var9);
         }
      } else {
         throw new ViePlatformServiceException("参数传递有误");
      }
   }

   public String getHotWordLastFlushTime(HotWordTaskRequest hotWordTaskRequest) throws ViePlatformServiceException {
      String time = null;
      if (hotWordTaskRequest == null) {
         throw new ViePlatformServiceException("参数传递有误");
      } else {
         try {
            if (hotWordTaskRequest.getUserId() == null || "".equals(hotWordTaskRequest.getUserId())) {
               this.logger.error("获取热词分析上次刷新时间入参userID不能为空");
               throw new ViePlatformServiceException("获取热词分析上次刷新时间入参userID不能为空");
            }

            if (hotWordTaskRequest.getSystemId() == null || "".equals(hotWordTaskRequest.getSystemId())) {
               this.logger.error("获取热词分析上次刷新时间入参systemId不能为空");
               throw new ViePlatformServiceException("获取热词分析上次刷新时间入参systemId不能为空");
            }

            if (hotWordTaskRequest.getPathId() == null) {
               this.logger.error("获取热词分析上次刷新时间入参pathId不能为空");
               throw new ViePlatformServiceException("获取热词分析上次刷新时间入参pathId不能为空");
            }

            if (hotWordTaskRequest.getDataSource() == null || "".equals(hotWordTaskRequest.getDataSource())) {
               this.logger.error("获取热词分析上次刷新时间入参dataSource不能为空");
               throw new ViePlatformServiceException("获取热词分析上次刷新时间入参dataSource不能为空");
            }

            Integer dataType = hotWordTaskRequest.getDataType();
            String batchId = hotWordTaskRequest.getBatchId();
            int ifLone = hotWordTaskRequest.getIfLone();
            if (dataType == null) {
               dataType = 2;
            }

            if (StringUtils.isNullOrEmpry(batchId)) {
               batchId = "0";
            }

            String sql = this.commonData.getDataSql(hotWordTaskRequest.getUserId(), hotWordTaskRequest.getSystemId(), hotWordTaskRequest.getDataSource());
            time = this.hotWordStatDb.getHotWordLastFlushTime(sql, hotWordTaskRequest.getPathId(), dataType, batchId, ifLone);
         } catch (Exception var7) {
            this.logger.error("[获取热词分析上次刷新时间]失败", var7);
         }

         return time;
      }
   }

   public HotWordStatDB getHotWordStatDb() {
      return this.hotWordStatDb;
   }

   public void setHotWordStatDb(HotWordStatDB hotWordStatDb) {
      this.hotWordStatDb = hotWordStatDb;
   }

   public DataAuthService getDataAuthService() {
      return this.dataAuthService;
   }

   public void setDataAuthService(DataAuthService dataAuthService) {
      this.dataAuthService = dataAuthService;
   }

   public CommonDataPlatform getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonDataPlatform commonData) {
      this.commonData = commonData;
   }

   public NotifyTaskService getNotifyTaskService() {
      return this.notifyTaskService;
   }

   public void setNotifyTaskService(NotifyTaskService notifyTaskService) {
      this.notifyTaskService = notifyTaskService;
   }

   public TopicDB getTopicDB() {
      return this.topicDB;
   }

   public void setTopicDB(TopicDB topicDB) {
      this.topicDB = topicDB;
   }
}
