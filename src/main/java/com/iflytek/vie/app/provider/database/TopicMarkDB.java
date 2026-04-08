package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.provider.pojo.TopicMarkInfo;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicMarkDB {
   private HibernateTemplateUtils hiberTemp;
   private static final Logger logger = LoggerFactory.getLogger(TopicMarkDB.class);

   public Integer saveMark(TopicMarkInfo topicMarkInfo) {
      this.hiberTemp.saveOrUpdate(topicMarkInfo);
      return 0;
   }

   public TopicMarkInfo getMarkInfo(String telephoneId, int topicId) {
      try {
         String hql = "SELECT TM FROM TopicMarkInfo TM WHERE TM.telephoneId=? AND TM.topic.topicId=?";
         return (TopicMarkInfo)this.hiberTemp.fetchFirstResult(hql, new Object[]{telephoneId, topicId});
      } catch (Exception var4) {
         logger.error("getMarkInfo DB has a error!", var4);
         return null;
      }
   }

   public Map<String, TopicMarkInfo> getMarkInfo(List<String> telephoneIds, int topicId) {
      Map<String, TopicMarkInfo> map = new HashMap<>();
      telephoneIds = this.removeDuplicate(telephoneIds);
      StringBuffer sb = this.splicingIds(telephoneIds, "TM.telephoneId");
      String hql = "SELECT TM FROM TopicMarkInfo TM WHERE TM.telephoneId IN (" + sb.toString() + ") AND TM.topic.topicId = :topicId ";

      try {
         List<TopicMarkInfo> topicMarkInfoList = (List<TopicMarkInfo>)(List<?>)this.hiberTemp.createQuery(hql, new String[]{"topicId"}, new Object[]{topicId});
         if (topicMarkInfoList != null && topicMarkInfoList.size() > 0) {
            TopicMarkInfo topicMarkInfo = null;

            for (int i = 0; i < topicMarkInfoList.size(); i++) {
               topicMarkInfo = topicMarkInfoList.get(i);
               map.put(topicMarkInfo.getTelephoneId(), topicMarkInfo);
            }
         }

         return map;
      } catch (Exception var9) {
         logger.error("getMarkInfo DB has a error!", var9);
         return null;
      }
   }

   public List<TopicMarkInfo> getMarkInfoList(List<String> telephoneIds, int topicId) {
      try {
         if (telephoneIds != null && telephoneIds.size() != 0) {
            telephoneIds = this.removeDuplicate(telephoneIds);
            StringBuffer sb = this.splicingIds(telephoneIds, "TM.telephoneId");
            String hql = "SELECT TM FROM TopicMarkInfo TM WHERE TM.telephoneId IN (" + sb.toString() + ") AND TM.topic.topicId=:topicId";
            return (List)this.hiberTemp.createQuery(hql, new String[]{"topicId"}, new Object[]{topicId});
         } else {
            return null;
         }
      } catch (Exception var6) {
         logger.error("getMarkInfo DB has a error!", var6);
         return null;
      }
   }

   public boolean deleteMarkInfo(String telephonId, int topicId) {
      try {
         TopicMarkInfo topicMarkInfo = this.getMarkInfo(telephonId, topicId);
         this.hiberTemp.delete(topicMarkInfo);
         return true;
      } catch (Exception var4) {
         logger.error("deleteMarkInfo DB has a error!", var4);
         return false;
      }
   }

   public TopicMarkInfo getMarkInfo(int markId) {
      try {
         String hql = "SELECT TM FROM TopicMarkInfo TM WHERE TM.id=?";
         List<TopicMarkInfo> topicMarkInfoList = (List<TopicMarkInfo>)(List<?>)this.hiberTemp.createQuery(hql, new Object[]{markId});
         if (topicMarkInfoList != null && topicMarkInfoList.size() > 0) {
            return topicMarkInfoList.get(0);
         }
      } catch (Exception var4) {
         logger.error("getMarkInfo DB has a error!", var4);
      }

      return null;
   }

   public boolean deleteMarkInfo(List<Integer> markIdList) {
      boolean flag = true;

      try {
         String hql = "";
         if (DBSelectUtil.isMysql()) {
            hql = "DELETE TopicMarkInfo WHERE id IN (:markIds)";
         } else {
            hql = "DELETE TopicMarkInfo tmi WHERE tmi.id IN (:markIds)";
         }

         int code = this.hiberTemp.bulkUpdate(hql, new String[]{"markIds"}, new Object[]{markIdList});
         if (code == 0) {
            flag = false;
         }
      } catch (Exception var5) {
         logger.error("deleteMarkInfo DB has a error!", var5);
      }

      return flag;
   }

   public List<TopicMarkInfo> getMarkByTopic(int topicId, String sort) {
      try {
         String hql = "";
         if (!StringUtils.isNullOrEmpry(sort)) {
            hql = "SELECT TM FROM TopicMarkInfo TM WHERE TM.topic.topicId=? ORDER BY TM.mark " + sort;
         } else {
            hql = "SELECT TM FROM TopicMarkInfo TM WHERE TM.topic.topicId=?";
         }

         return (List)this.hiberTemp.createQuery(hql, new Object[]{topicId});
      } catch (Exception var5) {
         logger.error("getMarkByTopic DB has a error!", var5);
         return null;
      }
   }

   private List<String> removeDuplicate(List<String> ids) {
      LinkedHashSet<String> set = new LinkedHashSet<>(ids.size());
      set.addAll(ids);
      ids.clear();
      ids.addAll(set);
      return ids;
   }

   private StringBuffer splicingIds(List<String> ids, String row) {
      StringBuffer sb = new StringBuffer();
      int num = 1;

      for (int i = 0; i < ids.size(); i++) {
         if (i == ids.size() - 1) {
            sb.append("'" + ids.get(i) + "'");
         } else if (num == 1000 && i > 0) {
            sb.append("'" + ids.get(i) + "' ) or " + row + " in ( ");
            num = 1;
         } else {
            sb.append("'" + ids.get(i) + "',");
            num++;
         }
      }

      return sb;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
