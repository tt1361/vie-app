package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.batch.BatchModelTask;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchModelDB {
   private HibernateTemplateUtils hiberTemp;
   private static final Logger logger = LoggerFactory.getLogger(BatchModelDB.class);

   public long saveOrUpdateBatchMode(BatchModelTask batchModelTask) throws VieAppServiceException {
      try {
         this.hiberTemp.saveOrUpdate(batchModelTask);
         this.hiberTemp.flush();
         return batchModelTask.getId();
      } catch (Exception var3) {
         logger.error("数据模型回溯任务保存或者更新saveOrUpdateBatchMode方法出错", var3);
         throw new VieAppServiceException("数据模型回溯任务保存或者更新saveOrUpdateBatchMode异常", var3);
      }
   }

   public List<BatchModelTask> getBatchModelTask() throws VieAppServiceException {
      try {
         String hql = "select bmt from BatchModelTask bmt where bmt.taskStatus != 1";
         return (List)this.hiberTemp.createQuery(hql, new Object[0]);
      } catch (Exception var3) {
         logger.error("查询数据模型回溯任务getTaskStatusById方法出错", var3);
         throw new VieAppServiceException("查询数据模型回溯任务getTaskStatusById异常", var3);
      }
   }

   public List<BatchModelTask> getDeleteBatchModelTask(List<Long> idList) throws VieAppServiceException {
      try {
         String[] paramNames = new String[]{"idList"};
         Object[] values = new Object[]{idList};
         String hql = "select bmt from BatchModelTask bmt where bmt.batchId in (:idList)";
         return (List)this.hiberTemp.createQuery(hql, paramNames, values);
      } catch (Exception var6) {
         logger.error("查询数据模型回溯任务getTaskStatusById方法出错", var6);
         throw new VieAppServiceException("查询数据模型回溯任务getTaskStatusById异常", var6);
      }
   }

   public void deleteBatchModelTask(List<Long> idList) throws VieAppServiceException {
      try {
         String[] paramNames = new String[]{"idList"};
         Object[] values = new Object[]{idList};
         String hql = "delete from BatchModelTask bmt where bmt.batchId in (:idList)";
         this.hiberTemp.bulkUpdate(hql, paramNames, values);
      } catch (Exception var5) {
         logger.error("删除数据模型回溯任务deleteBatchModelTask方法出错", var5);
         throw new VieAppServiceException("删除数据模型回溯任务deleteBatchModelTask异常", var5);
      }
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
