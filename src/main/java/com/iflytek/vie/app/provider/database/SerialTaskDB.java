package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.serial.SerialTask;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialTaskDB {
   private HibernateTemplateUtils hiberTemp;
   private static final Logger logger = LoggerFactory.getLogger(SerialTaskDB.class);

   public boolean checkBatchId(String batchId) throws VieAppServiceException {
      try {
         String hql = "select st.batchId from SerialTask st where st.batchId = :batchId";
         Object obj = this.hiberTemp.uniqueResult(hql, new String[]{"batchId"}, new Object[]{batchId});
         return obj != null;
      } catch (Exception var4) {
         logger.error("批次号是否重复查询checkBatchId方法出错", var4);
         throw new VieAppServiceException("批次号是否重复查询checkBatchId方法出错", var4);
      }
   }

   public String getBatchId() throws VieAppServiceException {
      try {
         String hql = "select st.batchId from SerialTask st order by st.batchId desc";
         List<String> list = (List<String>)(List<?>)this.hiberTemp.createQuery(1, 1, hql, new Object[0]);
         String batchId = "";
         if (list != null && list.size() > 0) {
            batchId = list.get(0);
         }

         return batchId;
      } catch (Exception var4) {
         logger.error("获得批次号getBatchId方法出错", var4);
         throw new VieAppServiceException("获得批次号checkBatchId方法出错", var4);
      }
   }

   public boolean addSerialTask(SerialTask serialTask) throws VieAppServiceException {
      try {
         this.hiberTemp.saveOrUpdate(serialTask);
         this.hiberTemp.flush();
         return true;
      } catch (Exception var3) {
         logger.error("导入维度明细addSerialTask方法出错", var3);
         throw new VieAppServiceException("导入维度明细addSerialTask方法出错", var3);
      }
   }

   public int getSerialTaskCount() throws VieAppServiceException {
      try {
         String hql = new String("select count(st.batchId) from SerialTask st");
         Object obj = this.hiberTemp.uniqueResult(hql, new Object[0]);
         int rows = 0;
         if (obj != null) {
            rows = Integer.parseInt(obj.toString());
         }

         return rows;
      } catch (Exception var4) {
         logger.error("查询维度明细列表querySerialDetail方法出错", var4);
         throw new VieAppServiceException("查询维度明细列表querySerialDetail方法出错", var4);
      }
   }

   public List<SerialTask> querySerialTask(int pageSize, int pageNum, String sortColumn, String sortType) throws VieAppServiceException {
      StringBuffer sb = new StringBuffer("select st from SerialTask st");
      if (!StringUtils.isNullOrEmpry(sortColumn) && !StringUtils.isNullOrEmpry(sortType)) {
         if (sortColumn.equals("batchId")) {
            sb.append(" order by " + sortColumn + " " + sortType + "");
         } else {
            sb.append(" order by " + sortColumn + " " + sortType + ",st.batchId asc");
         }
      }

      try {
         String hql = sb.toString();
         return (List)this.hiberTemp.createQuery(pageNum, pageSize, hql, new Object[0]);
      } catch (Exception var7) {
         logger.error("查询维度明细列表querySerialDetail方法出错", var7);
         throw new VieAppServiceException("查询维度明细列表querySerialDetail方法出错", var7);
      }
   }

   public int getImportCount() throws VieAppServiceException {
      try {
         int importCount = 0;
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         String createTime = sdf.format(new Date());
         String hql = "select sum(st.total) from SerialTask st where st.createTime >= to_date('"
            + createTime
            + " 00:00:00','yyyy-MM-dd hh24:mi:ss') and st.createTime <= to_date('"
            + createTime
            + " 23:59:59','yyyy-MM-dd hh24:mi:ss')";
         Object obj = this.hiberTemp.uniqueResult(hql, new Object[0]);
         if (obj != null) {
            importCount = Integer.parseInt(obj.toString());
         }

         return importCount;
      } catch (Exception var6) {
         logger.error("得到当天已导入条数调用getImportCount方法出错", var6);
         throw new VieAppServiceException("得到当天已导入条数getImportCount方法出错", var6);
      }
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
