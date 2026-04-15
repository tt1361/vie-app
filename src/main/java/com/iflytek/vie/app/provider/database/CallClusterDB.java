package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.datamining.CallClusterTask;
import com.iflytek.vie.app.pojo.datamining.CallClusterTaskResponse;
import com.iflytek.vie.app.pojo.datamining.TogetherPath;
import com.iflytek.vie.app.pojo.datamining.TogetherPathRequest;
import com.iflytek.vie.app.pojo.datamining.TogetherPathResponse;
import com.iflytek.vie.app.pojo.datamining.TogetherResult;
import com.iflytek.vie.constants.Constants.TaskStatus;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallClusterDB {
   private static final Logger logger = LoggerFactory.getLogger(HotWordStatDB.class);
   private HibernateTemplateUtils hibernateTemplate;

   public List<CallClusterTask> getClusterStatus(String dataAuth, Map<String, Object> param) throws ViePlatformServiceException {
      String hql = "";
      int analyzeType = Integer.parseInt(String.valueOf(param.get("analyzeType")));
      String analyzeIds = String.valueOf(param.get("analyzeIds"));
      String beginTime = String.valueOf(param.get("beginTime"));
      String endTime = String.valueOf(param.get("endTime"));
      Integer dataType = Integer.parseInt(String.valueOf(param.get("dataType")));
      String batchId = String.valueOf(param.get("batchId"));
      int ifLone = Integer.parseInt(String.valueOf(param.get("ifLone")));
      long togetherId = Long.parseLong(String.valueOf(param.get("togetherId")));
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      new ArrayList();

      try {
         if (dataType == 2) {
            if (ifLone == 0) {
               hql = "FROM CallClusterTask WHERE togetherId = :togetherId and analyzeType = :analyzeType AND analyzeIds = :analyzeIds AND beginTime = :beginTime AND endTime = :endTime AND dataAuth = :dataAuth AND ifLone = :ifLone";
            } else {
               hql = "FROM CallClusterTask WHERE togetherId = :togetherId and analyzeType = :analyzeType AND analyzeIds = :analyzeIds AND beginTime = :beginTime AND endTime = :endTime AND dataAuth = :dataAuth AND ifLone = :ifLone AND batchId = :batchId";
               paramNames.add("batchId");
               values.add(batchId);
            }
         } else if (ifLone == 0) {
            hql = "FROM CallClusterTask WHERE togetherId = :togetherId and analyzeType = :analyzeType AND analyzeIds = :analyzeIds AND beginTime = :beginTime AND endTime = :endTime AND dataAuth = :dataAuth AND ifLone = :ifLone AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
         } else {
            hql = "FROM CallClusterTask WHERE togetherId = :togetherId and analyzeType = :analyzeType AND analyzeIds = :analyzeIds AND beginTime = :beginTime AND endTime = :endTime AND dataAuth = :dataAuth AND ifLone = :ifLone AND batchId = :batchId AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
            paramNames.add("batchId");
            values.add(batchId);
         }

         paramNames.add("togetherId");
         values.add(togetherId);
         paramNames.add("analyzeType");
         values.add(analyzeType);
         paramNames.add("analyzeIds");
         values.add(analyzeIds);
         paramNames.add("beginTime");
         values.add(beginTime);
         paramNames.add("endTime");
         values.add(endTime);
         paramNames.add("dataAuth");
         values.add(dataAuth);
         paramNames.add("ifLone");
         values.add(ifLone);
         return (List)this.hibernateTemplate.createQuery(hql, paramNames, values);
      } catch (Exception var17) {
         logger.info("查询来电原因聚类数据失败！");
         throw new ViePlatformServiceException("查询来电原因聚类数据失败！", var17);
      }
   }

   public CallClusterTaskResponse getClusterTaskStatus(String dataAuth, Map<String, Object> param, CallClusterTaskResponse response) {
      int status = 0;
      long taskId = 0L;
      String updateTime = "";

      try {
         List<CallClusterTask> callClusterTaskList = this.getClusterStatus(dataAuth, param);
         if (callClusterTaskList != null && callClusterTaskList.size() > 0) {
            status = callClusterTaskList.get(0).getTaskStatus();
            taskId = callClusterTaskList.get(0).getTaskId();
            updateTime = callClusterTaskList.get(0).getUpdateTime();
         }

         response.setStatus(status);
         response.setTaskId(taskId);
         response.setUpdateTime(updateTime);
      } catch (ViePlatformServiceException var10) {
         var10.printStackTrace();
      }

      return response;
   }

   public int getClusterTaskStatus(String dataAuth, Map<String, Object> param) {
      int status = 0;

      try {
         List<CallClusterTask> callClusterTaskList = this.getClusterStatus(dataAuth, param);
         if (callClusterTaskList != null && callClusterTaskList.size() > 0) {
            status = callClusterTaskList.get(0).getTaskStatus();
         }
      } catch (ViePlatformServiceException var6) {
         var6.printStackTrace();
      }

      return status;
   }

   public void reCreateClusterTask(String dataAuth, Map<String, Object> param) throws ViePlatformServiceException {
      try {
         List<CallClusterTask> callClusterTaskList = this.getClusterStatus(dataAuth, param);
         if (callClusterTaskList != null && callClusterTaskList.size() > 0 && callClusterTaskList.get(0).getTaskStatus() == TaskStatus.FAILED) {
            this.hibernateTemplate.delete(callClusterTaskList.get(0));
         }
      } catch (Exception var4) {
         logger.info("查询来电原因聚类数据失败！");
         throw new ViePlatformServiceException("查询来电原因聚类数据失败！", var4);
      }
   }

   public CallClusterTaskResponse createClusterTask(String dataAuth, Map<String, Object> param, CallClusterTaskResponse response, boolean isFlushTime) throws ViePlatformServiceException {
      CallClusterTask callClusterTask = new CallClusterTask();
      int analyzeType = Integer.parseInt(String.valueOf(param.get("analyzeType")));
      String analyzeIds = String.valueOf(param.get("analyzeIds"));
      String beginTime = String.valueOf(param.get("beginTime"));
      String endTime = String.valueOf(param.get("endTime"));
      Integer dataType = Integer.parseInt(String.valueOf(param.get("dataType")));
      String batchId = String.valueOf(param.get("batchId"));
      int ifLone = Integer.parseInt(String.valueOf(param.get("ifLone")));
      long togetherId = Long.parseLong(String.valueOf(param.get("togetherId")));

      try {
         callClusterTask.setAnalyzeType(analyzeType);
         callClusterTask.setAnalyzeIds(analyzeIds);
         callClusterTask.setBeginTime(beginTime);
         callClusterTask.setEndTime(endTime);
         callClusterTask.setDataType(dataType);
         callClusterTask.setBatchId(batchId);
         callClusterTask.setUpdateTime(DateUtils.parseDateToString(new Date(), "yyyy.MM.dd HH:mm:ss"));
         callClusterTask.setIfLone(ifLone);
         callClusterTask.setTogetherId(togetherId);
         if (isFlushTime) {
            callClusterTask.setTogetherId(0L);
         }

         callClusterTask.setTaskStatus(TaskStatus.NOT_STARTED);
         callClusterTask.setDataAuth(dataAuth);
         this.hibernateTemplate.save(callClusterTask);
         response.setStatus(TaskStatus.NOT_STARTED);
         response.setTaskId(callClusterTask.getTaskId());
         response.setUpdateTime(callClusterTask.getUpdateTime());
         return response;
      } catch (Exception var16) {
         logger.info("查询来电原因聚类数据失败！");
         throw new ViePlatformServiceException("查询来电原因聚类数据失败！", var16);
      }
   }

   public String getTogetherByTaskId(Long taskId, Long parentId) throws Exception {
      String hql = "";
      new ArrayList();
      List result;
      if (parentId == 0L) {
         result = this.hibernateTemplate
            .find("select t.chartLeftInfo  from TogetherResult t where t.taskId = ?  and t.parentId =0 order by t.togetherId asc", new Object[]{taskId});
      } else {
         result = this.hibernateTemplate
            .find("select t.chartLeftInfo  from TogetherResult t where  t.parentId =? order by t.togetherId asc", new Object[]{parentId});
      }

      return null != result && result.size() > 0 ? (String)result.get(0) : "";
   }

   public String getTogetherByPath(int togetherId) throws Exception {
      List<String> result = (List<String>)(List<?>)this.hibernateTemplate.find("select t.chartLeftInfo  from TogetherResult t where t.togetherId=?", new Object[]{togetherId});
      return null != result && result.size() > 0 ? result.get(0) : "";
   }

   public String getHotviewByTogetherId(int togetherId) throws Exception {
      List<String> result = (List<String>)(List<?>)this.hibernateTemplate.find("select t.chartRightInfo  from TogetherResult t where t.togetherId=?", new Object[]{togetherId});
      return null != result && result.size() > 0 ? result.get(0) : "";
   }

   public int saveTogetherPath(TogetherPathRequest togetherPathRequest) throws Exception {
      TogetherPath togetherPath = new TogetherPath();
      new TogetherPath();
      TogetherPath tp = this.getPrePath(togetherPathRequest.getTaskId(), togetherPathRequest.getTogetherId());
      if (tp == null) {
         togetherPath.setTaskId(togetherPathRequest.getTaskId());
         togetherPath.setTogetherId(togetherPathRequest.getTogetherId());
         togetherPath.setLoadIndex(togetherPathRequest.getTogetherIndex());
         togetherPath.setLoadName(togetherPathRequest.getTogetherName());
         this.hibernateTemplate.save(togetherPath);
         return togetherPath.getLoadId();
      } else {
         return tp.getLoadId();
      }
   }

   public TogetherPath getPrePath(int taskId, int togetherId) {
      TogetherPath tp = null;
      List<TogetherPath> tasks = (List<TogetherPath>)(List<?>)this.hibernateTemplate
         .find("from TogetherPath tp where  tp.taskId= ? and tp.togetherId=?", new Object[]{Long.parseLong(String.valueOf(taskId)), togetherId});
      if (null != tasks && tasks.size() > 0) {
         tp = tasks.get(0);
      }

      return tp;
   }

   public List<TogetherPathResponse> getTogetherPath(int taskId) throws Exception {
      List<TogetherPath> tasks = (List<TogetherPath>)(List<?>)this.hibernateTemplate.find("from TogetherPath tp where  tp.taskId= ?", new Object[]{Long.parseLong(String.valueOf(taskId))});
      List<TogetherPathResponse> tpList = new ArrayList<>();
      if (null != tasks && tasks.size() > 0) {
         for (TogetherPath tp : tasks) {
            tpList.add(new TogetherPathResponse(tp.getTogetherId(), tp.getLoadIndex(), tp.getLoadName()));
         }
      }

      return tpList;
   }

   public void deletePreCluster(long taskId) {
      this.hibernateTemplate.bulkUpdate("delete from CallClusterTask cct where cct.taskId = ?", new Object[]{taskId});
      this.hibernateTemplate.bulkUpdate("delete from TogetherPath tp where tp.taskId = ?", new Object[]{taskId});
      this.hibernateTemplate.bulkUpdate("delete from TogetherResult cr where cr.taskId = ?", new Object[]{taskId});
   }

   public void deletePreCluster(String dataAuth, Map<String, Object> param) {
      new ArrayList();
      new ArrayList();
      new ArrayList();
      long taskId = 0L;
      long togetherId = 0L;

      try {
         List<CallClusterTask> callClusterTaskList = this.getClusterStatus(dataAuth, param);
         if (callClusterTaskList != null && callClusterTaskList.size() > 0) {
            CallClusterTask callClusterTask = (CallClusterTask)callClusterTaskList.get(0);
            taskId = callClusterTask.getTaskId();
            if (callClusterTask.getTogetherId() == 0L) {
               List<TogetherResult> togetherResultList = (List<TogetherResult>)(List<?>)this.hibernateTemplate
                  .find("from TogetherResult t where t.taskId = ?  and t.parentId =0", new Object[]{taskId});
               this.hibernateTemplate.bulkUpdate("delete from CallClusterTask cct where cct.taskId =?", new Object[]{taskId});
               this.hibernateTemplate.bulkUpdate("delete from TogetherResult cr where cr.taskId = ?", new Object[]{taskId});

               for (TogetherResult togetherResult : togetherResultList) {
                  this.hibernateTemplate
                     .bulkUpdate(
                        "delete from CallClusterTask cct where cct.togetherId=?", new Object[]{Long.parseLong(String.valueOf(togetherResult.getTogetherId()))}
                     );
                  this.hibernateTemplate.bulkUpdate("delete from TogetherPath tp where tp.taskId = ?", new Object[]{taskId});
                  this.hibernateTemplate
                     .bulkUpdate(
                        "delete from TogetherResult cr where cr.parentId=?", new Object[]{Long.parseLong(String.valueOf(togetherResult.getTogetherId()))}
                     );
               }
            } else {
               List<Long> taskIdList = (List<Long>)(List<?>)this.hibernateTemplate
                  .find(
                     "select t.taskId from TogetherResult t where t.togetherId =?",
                     new Object[]{Integer.parseInt(String.valueOf(callClusterTask.getTogetherId()))}
                  );
               taskId = (Long)taskIdList.get(0);
               List<TogetherResult> var14 = (List<TogetherResult>)(List<?>)this.hibernateTemplate.find("from TogetherResult t where t.taskId = ?  and t.parentId =0", new Object[]{taskId});
               this.hibernateTemplate.bulkUpdate("delete from CallClusterTask cct where cct.taskId =?", new Object[]{taskId});
               this.hibernateTemplate.bulkUpdate("delete from TogetherResult cr where cr.taskId = ?", new Object[]{taskId});

               for (TogetherResult togetherResult : var14) {
                  this.hibernateTemplate
                     .bulkUpdate(
                        "delete from CallClusterTask cct where cct.togetherId=?", new Object[]{Long.parseLong(String.valueOf(togetherResult.getTogetherId()))}
                     );
                  this.hibernateTemplate.bulkUpdate("delete from TogetherPath tp where tp.taskId = ?", new Object[]{taskId});
                  this.hibernateTemplate
                     .bulkUpdate(
                        "delete from TogetherResult cr where cr.parentId=?", new Object[]{Long.parseLong(String.valueOf(togetherResult.getTogetherId()))}
                     );
               }
            }
         }
      } catch (ViePlatformServiceException var13) {
         var13.printStackTrace();
      }
   }

   public static Logger getLogger() {
      return logger;
   }

   public HibernateTemplateUtils getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplateUtils hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
