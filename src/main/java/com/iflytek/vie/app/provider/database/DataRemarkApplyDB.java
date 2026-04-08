package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.DataRemarkInfo;
import java.util.List;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class DataRemarkApplyDB {
   private HibernateTemplate hibernateTemplate;

   public DataRemarkInfo queryDataRemarkByTelId(long fragmentId, String telId) {
      List<DataRemarkInfo> dataRemarkInfos = (List<DataRemarkInfo>)(List<?>)this.hibernateTemplate.findByNamedQuery("queryDataRemarkByTelId", new Object[]{fragmentId, telId});
      return dataRemarkInfos != null && dataRemarkInfos.size() != 0 ? dataRemarkInfos.get(0) : null;
   }

   public void saveOrUpdate(DataRemarkInfo dataRemarkInfo) {
      this.hibernateTemplate.saveOrUpdate(dataRemarkInfo);
   }

   public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
