package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.topic.TopicTogatherInfo;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicTogatherDB {
   private static final Logger logger = LoggerFactory.getLogger(TopicTogatherDB.class);
   private HibernateTemplateUtils hibernateTemplate;

   public TopicTogatherInfo getTogatherInfo(long togatherId) {
      try {
         String hql = "SELECT TTI FROM TopicTogatherInfo TTI WHERE TTI.togatherId=?";
         return (TopicTogatherInfo)this.hibernateTemplate.fetchFirstResult(hql, new Object[]{togatherId});
      } catch (Exception var4) {
         logger.info("查询异常", var4);
         return null;
      }
   }

   public void setHibernateTemplate(HibernateTemplateUtils hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
