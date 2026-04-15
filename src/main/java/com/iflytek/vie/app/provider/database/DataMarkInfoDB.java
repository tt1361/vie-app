package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.DataMarkInfo;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class DataMarkInfoDB {
   private HibernateTemplate hiberTemp;
   private final Logger logger = LoggerFactory.getLogger(DataMarkInfoDB.class);

   public void deleteByFragmentId(final List<Long> fragmentIds) {
      this.hiberTemp.execute(new HibernateCallback<Object>() {
         public Object doInHibernate(Session session) throws HibernateException {
            session.createQuery("DELETE FROM DataMarkInfo WHERE fragmentId in (:fragmentIds)").setParameterList("fragmentIds", fragmentIds).executeUpdate();
            return null;
         }
      });
   }

   public List<DataMarkInfo> queryAllDataInfos(long fragmentId) {
      return (List)this.hiberTemp.findByNamedQuery("queryAllDataMarks", new Object[]{fragmentId});
   }

   public HibernateTemplate getHiberTemp() {
      return this.hiberTemp;
   }

   public DataMarkInfo queryDataMarkByTelId(long fragmentId, String telId) {
      List<DataMarkInfo> dataMarkInfos = (List<DataMarkInfo>)(List<?>)this.hiberTemp.findByNamedQuery("queryDataMarkByTelId", new Object[]{fragmentId, telId});
      return dataMarkInfos != null && dataMarkInfos.size() != 0 ? dataMarkInfos.get(0) : null;
   }

   public void setHiberTemp(HibernateTemplate hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
