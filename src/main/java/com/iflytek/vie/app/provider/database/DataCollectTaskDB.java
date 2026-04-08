package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.DataCollectTask;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class DataCollectTaskDB {
   private HibernateTemplate hibernateTemplate;

   public long saveCollectTask(DataCollectTask dataCollectTask) {
      return (Long)this.hibernateTemplate.save(dataCollectTask);
   }

   public HibernateTemplate getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
