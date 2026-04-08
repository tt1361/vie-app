package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.ModelTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class ModelTaskDB {
   private HibernateTemplate hiberTemp;
   private final Logger logger = LoggerFactory.getLogger(ModelGroupDBPlatform.class);

   public void saveModelTask(ModelTask modelTask) {
      this.hiberTemp.save(modelTask);
   }

   public HibernateTemplate getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplate hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
