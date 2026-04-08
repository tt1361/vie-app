package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.provider.pojo.StoreFileData;
import com.iflytek.vie.dynamic.DynamicDataSourceContextHolder;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StoreFileDataDB {
   private HibernateTemplateUtils hiberTemp;
   private static final Logger logger = LoggerFactory.getLogger(StoreFileDataDB.class);

   public List<StoreFileData> querySFDataByName(String batchName, String dataSource) throws VieAppServiceException {
      try {
         DynamicDataSourceContextHolder.setDataSourceType(dataSource);
         String[] paramNames = new String[]{"batchName", "dataSource"};
         Object[] values = new Object[]{batchName, dataSource};
         String hql = "select bi from StoreFileData bi where bi.fileName =:batchName and bi.dataSource =:dataSource";
         return (List)this.hiberTemp.createQuery(hql, paramNames, values);
      } catch (Exception var7) {
         logger.error("查询数据状态querySFDataByName方法出错", var7);
         throw new VieAppServiceException("查询数据状态querySFDataByName方法出错", var7);
      }
   }

   public List<StoreFileData> querySFDataByTime(Date fromTime, Date toTime, String dataSource) throws VieAppServiceException {
      try {
         DynamicDataSourceContextHolder.setDataSourceType(dataSource);
         String[] paramNames = new String[]{"fromTime", "toTime", "dataSource"};
         Object[] values = new Object[]{fromTime, toTime, dataSource};
         String hql = "select bi from StoreFileData bi where bi.uploadDate between :fromTime and :toTime and bi.dataSource =:dataSource";
         return (List)this.hiberTemp.createQuery(hql, paramNames, values);
      } catch (Exception var8) {
         logger.error("查询数据状态querySFDataByTime方法出错", var8);
         throw new VieAppServiceException("查询数据状态querySFDataByTime方法出错", var8);
      }
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
