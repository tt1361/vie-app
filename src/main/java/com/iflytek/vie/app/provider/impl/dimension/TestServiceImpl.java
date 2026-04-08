package com.iflytek.vie.app.provider.impl.dimension;

import com.iflytek.vie.app.pojo.dimension.TestService;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.List;
import java.util.Map;

public class TestServiceImpl implements TestService {
   private HibernateTemplateUtils hibernateTemplate;

   public List<Map<String, Object>> test() {
      return (List<Map<String, Object>>)(List<?>)this.hibernateTemplate.createSQLQuery("SELECT * from DIMENSION_PERSONAL ", new Object[0]);
   }

   public HibernateTemplateUtils getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplateUtils hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
