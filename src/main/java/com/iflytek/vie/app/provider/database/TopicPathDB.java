package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.topic.TopicPath;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicPathDB {
   private static final Logger logger = LoggerFactory.getLogger(TopicPathDB.class);
   private HibernateTemplateUtils hibernateTemplate;

   public String getPathCondition(int pathId) {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();

      try {
         String hql = "SELECT TP FROM TopicPath TP WHERE TP.pathId=:pathId";
         paramNames.add("pathId");
         values.add(pathId);
         TopicPath topicPath = (TopicPath)this.hibernateTemplate.fetchFirstResult(hql, paramNames, values);
         return topicPath != null && !"[]".equals(topicPath.getPathCondition()) ? topicPath.getPathCondition() : "";
      } catch (Exception var6) {
         logger.error("getPathCondition DB has a error!", var6);
         return "";
      }
   }

   public TopicPath getPathById(int pathId) {
      try {
         String hql = "SELECT TP FROM TopicPath TP WHERE TP.pathId = :pathId";
         List<TopicPath> topicPathList = (List<TopicPath>)(List<?>)this.hibernateTemplate.createQuery(hql, new String[]{"pathId"}, new Object[]{pathId});
         if (topicPathList != null && topicPathList.size() > 0) {
            return topicPathList.get(0);
         }
      } catch (Exception var4) {
         logger.error("getPathById DB has a error!", var4);
      }

      return null;
   }

   public void setHibernateTemplate(HibernateTemplateUtils hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
