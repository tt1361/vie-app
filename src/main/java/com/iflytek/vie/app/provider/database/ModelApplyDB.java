package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.utils.DBSelectUtil;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class ModelApplyDB {
   private static final Logger logger = LoggerFactory.getLogger(ModelApplyDB.class);
   private HibernateTemplate hibernateTemplate;

   public Integer queryMaxUpNum() throws ViePlatformServiceException {
      try {
         Session session = this.getHibernateTemplate().getSessionFactory().getCurrentSession();
         String sql = "select max(t.is_up) as upnum,'123' as ttt from model_info t";
         Query query = session.createSQLQuery(sql);
         List<Object[]> list = query.list();
         if (list != null && list.size() > 0) {
            Object[] objects = list.get(0);
            if (DBSelectUtil.isMysql()) {
               return Integer.parseInt(String.valueOf(objects[0]));
            }

            BigDecimal bd = (BigDecimal)objects[0];
            return bd.intValue();
         }
      } catch (Exception var7) {
         logger.error("查询现有的up的最大值db异常");
         throw new ViePlatformServiceException("查询现有的up的最大值db异常", var7);
      }

      return 0;
   }

   public ModelInfo queryModelById(Long modelId) throws ViePlatformServiceException {
      try {
         List<ModelInfo> modelInfoList = (List<ModelInfo>)(List<?>)this.getHibernateTemplate().findByNamedQuery("QueryModelById", new Object[]{modelId});
         return modelInfoList != null && modelInfoList.size() > 0 ? modelInfoList.get(0) : null;
      } catch (Exception var3) {
         logger.error("按照模型Id查找模型db异常");
         throw new ViePlatformServiceException("按照模型Id查找模型db异常", var3);
      }
   }

   public void saveModel(ModelInfo modelInfo) throws ViePlatformServiceException {
      try {
         this.getHibernateTemplate().saveOrUpdate(modelInfo);
      } catch (Exception var3) {
         logger.error("保存模型db异常");
         throw new ViePlatformServiceException("保存模型db异常", var3);
      }
   }

   public HibernateTemplate getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
