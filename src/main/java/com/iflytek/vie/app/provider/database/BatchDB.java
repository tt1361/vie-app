package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.batch.BatchInfo;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchDB {
   private HibernateTemplateUtils hiberTemp;
   private static final Logger logger = LoggerFactory.getLogger(BatchDB.class);

   public boolean checkBatchId(String batchId) throws VieAppServiceException {
      try {
         String hql = "from BatchInfo where batchId = :batchId";
         List<BatchInfo> list = (List<BatchInfo>)(List<?>)this.hiberTemp.createQuery(hql, new String[]{"batchId"}, new Object[]{batchId});
         return list != null && list.size() > 0;
      } catch (Exception var4) {
         logger.error("批次号是否重复查询checkBatchId方法出错", var4);
         throw new VieAppServiceException("批次号是否重复查询checkBatchId方法出错", var4);
      }
   }

   public int saveOrUpdateBatch(BatchInfo batchInfo) throws VieAppServiceException {
      try {
         this.hiberTemp.saveOrUpdate(batchInfo);
         this.hiberTemp.flush();
         return batchInfo.getId();
      } catch (Exception var3) {
         logger.error("数据保存或者更新saveOrUpdateBatch方法出错", var3);
         throw new VieAppServiceException("数据保存或者更新saveOrUpdateBatch方法出错", var3);
      }
   }

   public List<BatchInfo> getBatchList(
      String startDate, String endDate, String batchId, Integer ifLone, int pageSize, int pageNum, List<Integer> idList, Integer dataType
   ) throws VieAppServiceException {
      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         StringBuffer hql = new StringBuffer("select bi from BatchInfo bi where bi.id in :idList");
         paramNames.add("idList");
         values.add(idList);
         if (!StringUtils.isNullOrEmpry(startDate) && !StringUtils.isNullOrEmpry(endDate)) {
            hql.append(
               " and bi.startTime >= to_date('"
                  + startDate
                  + " 00:00:00','yyyy-MM-dd hh24:mi:ss') and bi.startTime <= to_date('"
                  + endDate
                  + " 23:59:59','yyyy-MM-dd hh24:mi:ss')"
            );
         }

         if (!StringUtils.isNullOrEmpry(batchId)) {
            batchId = batchId.replace("_", "\\_");
            hql.append(" and bi.batchId like :batchId");
            paramNames.add("batchId");
            values.add("%" + batchId + "%");
         }

         if (ifLone != null) {
            hql.append(" and bi.ifLone = :ifLone");
            paramNames.add("ifLone");
            values.add(ifLone);
         }

         if (dataType != null && (dataType == 0 || dataType == 1)) {
            hql.append(" and bi.dataType = :dataType");
            paramNames.add("dataType");
            values.add(dataType);
         }

         hql.append(" order by bi.id DESC");
         return (List)this.hiberTemp.createQuery(pageNum, pageSize, hql.toString(), paramNames, values);
      } catch (Exception var12) {
         logger.error("查询数据列表getBatchList方法出错", var12);
         throw new VieAppServiceException("查询数据列表getBatchList方法出错", var12);
      }
   }

   public int getBatchListSize(String startDate, String endDate, String batchId, Integer ifLone, List<Integer> idList) throws VieAppServiceException {
      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         StringBuffer hql = new StringBuffer("select count(bi.id) from BatchInfo bi where bi.id in :idList");
         paramNames.add("idList");
         values.add(idList);
         if (!StringUtils.isNullOrEmpry(startDate) && !StringUtils.isNullOrEmpry(endDate)) {
            hql.append(
               " and bi.startTime >= to_date('"
                  + startDate
                  + " 00:00:00','yyyy-MM-dd hh24:mi:ss') and bi.startTime <= to_date('"
                  + endDate
                  + " 23:59:59','yyyy-MM-dd hh24:mi:ss')"
            );
         }

         if (!StringUtils.isNullOrEmpry(batchId)) {
            batchId = batchId.replace("_", "\\_");
            hql.append(" and bi.batchId like :batchId");
            paramNames.add("batchId");
            values.add("%" + batchId + "%");
         }

         if (ifLone != null && (ifLone == 0 || ifLone == 1)) {
            hql.append(" and bi.ifLone = :ifLone");
            paramNames.add("ifLone");
            values.add(ifLone);
         }

         Object obj = this.hiberTemp.uniqueResult(hql.toString(), paramNames, values);
         int rows = 0;
         if (obj != null) {
            rows = Integer.parseInt(obj.toString());
         }

         return rows;
      } catch (Exception var11) {
         logger.error("查询数据列表getBatchList方法出错", var11);
         throw new VieAppServiceException("查询数据列表getBatchList方法出错", var11);
      }
   }

   public BatchInfo getBatchById(int id) throws VieAppServiceException {
      try {
         String[] params = new String[]{"id"};
         Object[] values = new Object[]{id};
         String hql = "from BatchInfo where id = :id";
         return (BatchInfo)this.hiberTemp.uniqueResult(hql, params, values);
      } catch (Exception var6) {
         logger.error("查询数据信息getBatchById方法出错", var6);
         throw new VieAppServiceException("查询数据信息getBatchList方法出错", var6);
      }
   }

   public boolean deleteBatch(List<Integer> idList) throws VieAppServiceException {
      try {
         String[] paramNames = new String[]{"idList"};
         Object[] values = new Object[]{idList};
         String hql = "delete from BatchInfo bi where bi.id in (:idList)";
         this.hiberTemp.bulkUpdate(hql, paramNames, values);
         return true;
      } catch (Exception var5) {
         logger.error("删除数据deleteBatch方法出错", var5);
         throw new VieAppServiceException("删除数据deleteBatch方法出错", var5);
      }
   }

   public List<BatchInfo> getBatchStatus(List<Integer> idList) throws VieAppServiceException {
      try {
         String[] paramNames = new String[]{"idList"};
         Object[] values = new Object[]{idList};
         String hql = "select bi from BatchInfo bi where bi.id in :idList";
         return (List)this.hiberTemp.createQuery(hql, paramNames, values);
      } catch (Exception var6) {
         logger.error("查询数据状态getBatchStatus方法出错", var6);
         throw new VieAppServiceException("查询数据状态getBatchStatus方法出错", var6);
      }
   }

   public List<Long> getModelIdByBatch(String batchId) throws VieAppServiceException {
      try {
         String[] params = new String[]{"batchId"};
         Object[] values = new Object[]{batchId};
         String hql = "select bi.models from BatchInfo bi where bi.batchId = :batchId";
         Object obj = this.hiberTemp.uniqueResult(hql, params, values);
         String models = "";
         if (obj != null) {
            models = obj.toString();
         }

         String[] idArrays = models.split(",");
         List<Long> idList = new ArrayList<>();

         for (String id : idArrays) {
            idList.add(Long.parseLong(id));
         }

         return idList;
      } catch (Exception var13) {
         logger.error("查询数据状态getBatchStatus方法出错", var13);
         throw new VieAppServiceException("查询数据状态getBatchStatus方法出错", var13);
      }
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
