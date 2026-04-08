package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.batch.BatchDimension;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchDimensionDB {
   private HibernateTemplateUtils hiberTemp;
   private static final Logger logger = LoggerFactory.getLogger(BatchDimensionDB.class);

   public List<BatchDimension> queryDimension(String dataSource) throws VieAppServiceException {
      try {
         String hql = "select bd from BatchDimension bd where bd.dataSource = :dataSource";
         return (List)this.hiberTemp.createQuery(hql, new String[]{"dataSource"}, new Object[]{dataSource});
      } catch (Exception var4) {
         logger.error("查询对应数据源下的维度信息queryDimension方法出错", var4);
         throw new VieAppServiceException("查询对应数据源下的维度信息queryDimension方法出错", var4);
      }
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
