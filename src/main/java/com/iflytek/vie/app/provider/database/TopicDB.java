package com.iflytek.vie.app.provider.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.pojo.topic.PathDimension;
import com.iflytek.vie.app.pojo.topic.TopicInfo;
import com.iflytek.vie.app.pojo.topic.TopicOptionLoad;
import com.iflytek.vie.app.pojo.topic.TopicPath;
import com.iflytek.vie.app.pojo.topic.TopicTogatherInfo;
import com.iflytek.vie.app.provider.common.SqlHelper;
import com.iflytek.vie.app.provider.pojo.TopicAllDimension;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.ExceptionUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate4.HibernateCallback;

public class TopicDB {
   private static final Logger logger = LoggerFactory.getLogger(TopicDB.class);
   private HibernateTemplateUtils hiberTemp;

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }

   public int queryAllTopicsCount(String dataSource, String topicName) {
      String sql = SqlHelper.queryAllTopicsCount(dataSource, topicName);
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      paramNames.add("dataSource");
      values.add(dataSource);
      if (!StringUtils.isNullOrEmpry(topicName)) {
         if (DBSelectUtil.isMysql()) {
            topicName = topicName.replace("_", "/_");
         } else {
            topicName = topicName.replace("_", "\\_");
         }

         paramNames.add("topicName");
         values.add("%" + topicName + "%");
      }

      return DBSelectUtil.isMysql()
         ? ((BigInteger)this.hiberTemp.uniqueSQLResult(sql, paramNames, values)).intValue()
         : ((BigDecimal)this.hiberTemp.uniqueSQLResult(sql, paramNames, values)).intValue();
   }

   public List<Object[]> queryAllTopics(String dataSource, int pageNum, int pageSize, String topicName) {
      try {
         String sql = SqlHelper.queryAllTopics(dataSource, topicName);
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         paramNames.add("dataSource");
         values.add(dataSource);
         if (!StringUtils.isNullOrEmpry(topicName)) {
            topicName = topicName.replace("_", "\\_");
            paramNames.add("topicName");
            values.add("%" + topicName + "%");
         }

         return (List)this.hiberTemp.createSQLQuery(pageNum, pageSize, sql, paramNames, values);
      } catch (Exception var9) {
         throw new RuntimeException(ExceptionUtil.getStackTrace(var9));
      }
   }

   public int queryTopicCountByName(String dataSource, String topicName) {
      String sql = SqlHelper.queryAllTopicsCountByName(dataSource, topicName);
      return DBSelectUtil.isMysql()
         ? ((BigInteger)this.hiberTemp.uniqueSQLResult(sql, new String[]{"dataSource", "topicName"}, new Object[]{dataSource, topicName})).intValue()
         : ((BigDecimal)this.hiberTemp.uniqueSQLResult(sql, new String[]{"dataSource", "topicName"}, new Object[]{dataSource, topicName})).intValue();
   }

   public List<TopicPath> queryDimensionInfo(String topicId) {
      try {
         String sql = " SELECT  t FROM  TopicPath t WHERE t.topic.topicId = ?";
         return (List)this.hiberTemp.createQuery(sql, new Object[]{Integer.parseInt(topicId)});
      } catch (Exception var4) {
         logger.info("DB Exception", var4);
         return null;
      }
   }

   public void deleteTopicsByIds(final List<Integer> topicIds) {
      this.hiberTemp.execute(new HibernateCallback<Object>() {
         public Object doInHibernate(Session session) throws HibernateException {
            Query query = session.createQuery("UPDATE TopicInfo SET topicStatus=1 WHERE topicId IN (:ids)").setParameterList("ids", topicIds);
            query.executeUpdate();
            return null;
         }
      });
   }

   public void saveTopicPath(TopicPath topicPath) {
      this.hiberTemp.saveOrUpdate(topicPath);
   }

   public void deleteTopicPath(TopicPath topicPath) {
      this.hiberTemp.delete(topicPath);
   }

   public Integer saveTopic(TopicInfo topicInfo) {
      return (Integer)this.hiberTemp.save(topicInfo);
   }

   public Integer saveMainPath(TopicPath topicPath) {
      int pathId = (Integer)this.hiberTemp.save(topicPath);
      return pathId;
   }

   public void saveDimensionJson(TopicAllDimension topicAllDimension) {
      this.hiberTemp.save(topicAllDimension);
   }

   public boolean deleteDimensions() {
      String sql = "delete from TopicAllDimension";

      try {
         this.hiberTemp.bulkUpdate(sql, new Object[0]);
         return true;
      } catch (Exception var3) {
         logger.error("deleteDimensions DB has a error!", var3);
         return false;
      }
   }

   public List<Object> queryPreDimensionJson() {
      try {
         String sql = "select dimension_json from TOPIC_ALL_DIMENSION ";
         return (List)this.hiberTemp.createSQLQuery(sql, new Object[0]);
      } catch (Exception var3) {
         throw new RuntimeException(ExceptionUtil.getStackTrace(var3));
      }
   }

   public TopicInfo fetchTopicInfoById(String topId) {
      try {
         String hql = "from TopicInfo rc where  rc.topicId = :topId and rc.topicStatus = 0";
         return (TopicInfo)this.hiberTemp.fetchFirstResult(hql, new String[]{"topId"}, new Object[]{Integer.parseInt(topId)});
      } catch (Exception var3) {
         logger.error("fetchTopicInfoById DB has a error!", var3);
         return null;
      }
   }

   public TopicInfo getTopicById(int topicId) {
      try {
         return (TopicInfo)this.hiberTemp.get(TopicInfo.class, topicId);
      } catch (Exception var3) {
         logger.error("getTopicById DB has a error!", var3);
         return null;
      }
   }

   public void updateDimension(
      final String pathId,
      final String callListDimensions,
      final String chartDimensions,
      final String togetherDimensions,
      final String xDimension,
      final String yDimension,
      final TopicInfo topicInfo,
      final int pathStatus
   ) {
      this.hiberTemp
         .execute(
            new HibernateCallback<Object>() {
               public Object doInHibernate(Session session) throws HibernateException {
                  Query query = session.createQuery(
                     "UPDATE TopicPath SET callListDimensions=?,chartDimensions=?,togetherDimensions=?,xDimension=?,yDimension=?,topic=?,pathStatus=?  WHERE pathId =?"
                  );
                  query.setParameter(0, callListDimensions);
                  query.setParameter(1, chartDimensions);
                  query.setParameter(2, togetherDimensions);
                  query.setParameter(3, xDimension);
                  query.setParameter(4, yDimension);
                  query.setParameter(5, topicInfo);
                  query.setParameter(6, pathStatus);
                  query.setParameter(7, Integer.parseInt(pathId));
                  query.executeUpdate();
                  return null;
               }
            }
         );
   }

   public void updateTopicInfo(final String topicId, final String updateTime, final String updateUser) {
      this.hiberTemp.execute(new HibernateCallback<Object>() {
         public Object doInHibernate(Session session) throws HibernateException {
            Query query = session.createQuery("UPDATE TopicInfo SET updateTime=?,updateUserName=?  WHERE topicId=?");
            query.setParameter(0, updateTime);
            query.setParameter(1, updateUser);
            query.setParameter(2, Integer.parseInt(topicId));
            query.executeUpdate();
            return null;
         }
      });
   }

   public void saveTogetherPath(TopicOptionLoad topicOptionLoad) {
      this.hiberTemp.saveOrUpdate(topicOptionLoad);
   }

   public void deletePrePath(TopicPath topicPath) {
      String sql = "delete from TopicOptionLoad where path = ?";

      try {
         this.hiberTemp.createQuery(sql, new Object[]{topicPath});
      } catch (Exception var4) {
         logger.error("delete from TopicOptionLoad DB has a error!", var4);
      }
   }

   public void deletePrePathByPathId(String pathId) {
      String sql = "delete from TopicOptionLoad where path.pathId = :pathId";

      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         paramNames.add("pathId");
         values.add(Integer.parseInt(pathId));
         this.hiberTemp.bulkUpdate(sql, paramNames, values);
      } catch (Exception var5) {
         logger.error("delete from DB has a error!", var5);
      }
   }

   public List<TopicOptionLoad> queryTogetherOptionLoad(String pathId) {
      try {
         String sql = "SELECT  t FROM  TopicOptionLoad  t  WHERE t.path.pathId =:pathId";
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         paramNames.add("pathId");
         values.add(Integer.parseInt(pathId));
         return (List)this.hiberTemp.createQuery(sql, paramNames, values);
      } catch (Exception var5) {
         logger.error("queryTogetherOptionLoad from DB has a error!", var5);
         return null;
      }
   }

   public TopicTogatherInfo queryTogetherById(String togetherId) {
      try {
         try {
            String hql = "from TopicTogatherInfo rc where  rc.togatherId = ? ";
            return (TopicTogatherInfo)this.hiberTemp.fetchFirstResult(hql, new Object[]{Long.parseLong(togetherId)});
         } catch (Exception var3) {
            logger.error("queryTogetherById DB has a error!", var3);
            return null;
         }
      } catch (Exception var4) {
         logger.error("queryTogetherById DB has a error!", var4);
         return null;
      }
   }

   public TopicPath queryPathById(String pathId) {
      try {
         try {
            String hql = "from TopicPath rc where  rc.pathId = ?";
            List<TopicPath> topicPathInfoList = (List<TopicPath>)(List<?>)this.hiberTemp.createQuery(hql, new Object[]{Integer.parseInt(pathId)});
            return topicPathInfoList.size() > 0 ? topicPathInfoList.get(0) : null;
         } catch (Exception var4) {
            logger.error("queryPathById DB has a error!", var4);
            return null;
         }
      } catch (Exception var5) {
         logger.error("queryPathById DB has a error!", var5);
         return null;
      }
   }

   public TopicPath fetchTopicPathById(int pathId) {
      try {
         return (TopicPath)this.hiberTemp.get(TopicPath.class, pathId);
      } catch (Exception var3) {
         logger.error("fetchTopicPathById DB has a error!", var3);
         return null;
      }
   }

   public TopicPath fetchTopicPathByPathName(String pathName, String topicId) {
      try {
         String sql = "from TopicPath rc where  rc.pathName = :pathName and rc.topic.id= :topicId";
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         paramNames.add("pathName");
         paramNames.add("topicId");
         values.add(pathName);
         values.add(Integer.parseInt(topicId));
         return (TopicPath)this.hiberTemp.fetchFirstResult(sql, paramNames, values);
      } catch (Exception var6) {
         logger.error("fetchTopicPathByPathName DB has a error!", var6);
         return null;
      }
   }

   public boolean saveOrUpdateTopicInfo(TopicInfo topicInfo) {
      try {
         this.hiberTemp.saveOrUpdate(topicInfo);
         this.hiberTemp.flush();
         return true;
      } catch (Exception var3) {
         logger.error("saveOrUpdateTopicInfo DB has a error!", var3);
         return false;
      }
   }

   public String getTopicCondition(int topicId) {
      try {
         String hql = "SELECT TF.topicCondition FROM TopicInfo as TF WHERE TF.id=?";
         String conditon = (String)this.hiberTemp.fetchFirstResult(hql, new Object[]{topicId});
         return conditon == null ? "" : conditon;
      } catch (Exception var4) {
         logger.error("getTopicCondition DB has a error!", var4);
         return "";
      }
   }

   public Map<String, Object> getTopicTimeType(Long topicId) {
      Map<String, Object> map = null;

      try {
         String hql = "FROM TopicInfo t WHERE t.topicId = :topicId  ";
         List<TopicInfo> topicInfo = (List<TopicInfo>)(List<?>)this.hiberTemp.createQuery(hql, new String[]{"topicId"}, new Object[]{Integer.parseInt(topicId.toString())});
         if (topicInfo != null && topicInfo.size() > 0) {
            map = this.parseJsonDataToMap(topicInfo.get(0).getTopicCondition().trim());
         }
      } catch (Exception var5) {
         logger.error("getTopicTimeType DB has a error!", var5);
      }

      return map;
   }

   public List<PathDimension> queryAllPathDim(String pathId) {
      return (List)this.hiberTemp.findByNamedQuery("getAllPathDimById", new Object[]{pathId});
   }

   public void deletePathDim(PathDimension pathDimension) {
      this.hiberTemp.delete(pathDimension);
   }

   public PathDimension queryPathDimById(int id) {
      try {
         return (PathDimension)this.hiberTemp.get(PathDimension.class, id);
      } catch (Exception var3) {
         logger.error("queryPathDimById DB has a error!", var3);
         return null;
      }
   }

   public void addPathDim(PathDimension pathDimension) {
      try {
         this.hiberTemp.saveOrUpdate(pathDimension);
      } catch (Exception var3) {
         throw new RuntimeException(ExceptionUtil.getStackTrace(var3));
      }
   }

   public Map<String, Object> parseJsonDataToMap(String data) {
      Map<String, Object> resultMap = null;

      try {
         if (data != null) {
            ObjectMapper mapper = new ObjectMapper();
            List<HashMap<String, Object>> topicConditionList = (List<HashMap<String, Object>>)mapper.readValue(data, List.class);
            if (topicConditionList != null && topicConditionList.size() > 0) {
               Map<String, Object> map = null;

               for (int i = 0; i < topicConditionList.size(); i++) {
                  map = topicConditionList.get(i);
                  if ("timestamp".equals(map.get("key"))) {
                     resultMap = map;
                     break;
                  }
               }
            }
         }
      } catch (Exception var7) {
         logger.error("parseJsonDataToMap DB has a error!", var7);
      }

      return resultMap;
   }
}
