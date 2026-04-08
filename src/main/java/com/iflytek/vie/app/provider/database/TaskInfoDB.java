package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.TaskInfo;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class TaskInfoDB {
   private HibernateTemplate hiberTemp;

   public void saveTask(TaskInfo taskInfo) {
      this.hiberTemp.save(taskInfo);
   }

   public HibernateTemplate getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplate hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
