package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.model.DataMarkInfo;
import java.util.List;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class DataMarkInfoApplyDB {
   private static final Logger logger = LoggerFactory.getLogger(DataMarkInfoApplyDB.class);
   private HibernateTemplate hibernateTemplate;

   public DataMarkInfo queryDataMarkByTelId(long fragmentId, String telId) throws ViePlatformServiceException {
      try {
         List<DataMarkInfo> dataMarkInfos = (List<DataMarkInfo>)(List<?>)this.hibernateTemplate.findByNamedQuery("queryDataMarkByTelId", new Object[]{fragmentId, telId});
         return dataMarkInfos != null && dataMarkInfos.size() != 0 ? dataMarkInfos.get(0) : null;
      } catch (Exception var5) {
         logger.error("根据id查找标注录音db异常");
         throw new ViePlatformServiceException("根据id查找标注录音db异常", var5);
      }
   }

   public void saveOrUpdate(DataMarkInfo dataMarkInfo) throws ViePlatformServiceException {
      try {
         this.hibernateTemplate.saveOrUpdate(dataMarkInfo);
      } catch (Exception var3) {
         logger.error("保存标注信息db异常");
         throw new ViePlatformServiceException("保存标注信息db异常", var3);
      }
   }

   public void updateMark(long fragmentId, List<String> telephonIdList) throws ViePlatformServiceException {
      Session session = null;

      try {
         session = this.hibernateTemplate.getSessionFactory().openSession();
         session.beginTransaction();
         String sql = "UPDATE DataMarkInfo SET mark=0 WHERE fragmentId=? AND telId in (:telephonIds)";
         logger.info("==========" + sql);
         session.createQuery(sql).setParameter(0, fragmentId).setParameterList("telephonIds", telephonIdList).executeUpdate();
         session.getTransaction().commit();
      } catch (Exception var9) {
         logger.error("更新标记db异常");
         throw new ViePlatformServiceException("更新标记db异常", var9);
      } finally {
         session.close();
      }
   }

   public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
