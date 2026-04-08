package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.List;

public class DimensionDB {
   private HibernateTemplateUtils hibernateTemplate;

   public List<DimensionConfig> searchSystemDim(String dataSource) {
      return (List)this.hibernateTemplate.find("FROM DimensionConfig DC WHERE DC.dataSource=?", new Object[]{dataSource});
   }

   public HibernateTemplateUtils getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplateUtils hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
