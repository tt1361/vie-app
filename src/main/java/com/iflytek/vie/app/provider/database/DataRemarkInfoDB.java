package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.DataRemarkInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class DataRemarkInfoDB {
   private HibernateTemplate hiberTemp;
   private final Logger logger = LoggerFactory.getLogger(DataRemarkInfoDB.class);

   public List<DataRemarkInfo> queryAllDataRemarks(long fragmentId) {
      return (List)this.hiberTemp.findByNamedQuery("queryAllDataRemarks", new Object[]{fragmentId});
   }

   public HibernateTemplate getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplate hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
