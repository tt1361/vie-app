package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicHotWordStatDB {
   private static final Logger logger = LoggerFactory.getLogger(TopicHotWordStatDB.class);
   private HibernateTemplateUtils hiberTemp;

   public int getLastTotalCount(String dataTime) {
      int totalCount = 0;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();

      try {
         String hql = "SELECT count(statId) FROM TopicHotWordStat WHERE statDate = :statDate ";
         paramNames.add("dataTime");
         values.add(dataTime);
         Object obj = this.hiberTemp.uniqueResult(hql, paramNames, values);
         if (obj != null) {
            totalCount = Integer.parseInt(String.valueOf(obj));
         }
      } catch (Exception var7) {
         logger.error("getLastTotalCount DB has a error!", var7);
      }

      return totalCount;
   }

   public List<String> getDateList(Long pathId, String dataAuth, Integer dataType, String batchId, int ifLone) {
      List<String> list = null;
      List<Object> values = new ArrayList<>();
      List<String> paramNames = new ArrayList<>();
      String sql = "SELECT t.statdate FROM TOPIC_HOTWORD_STAT t WHERE t.path_id = :pathId AND t.data_auth = :dataAuth";

      try {
         if (dataType == 2) {
            if (ifLone == 0) {
               sql = sql + " AND t.if_lone = :ifLone";
            } else {
               sql = sql + " AND t.if_lone = :ifLone AND t.batch_id = :batchId";
               paramNames.add("batchId");
               values.add(batchId);
            }
         } else if (ifLone == 0) {
            sql = sql + " AND t.if_lone = :ifLone AND t.data_type = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
         } else {
            sql = sql + " AND t.if_lone = :ifLone AND t.data_type = :dataType AND t.batch_id = :batchId";
            paramNames.add("dataType");
            values.add(dataType);
            paramNames.add("batchId");
            values.add(batchId);
         }

         sql = sql + " GROUP BY t.statdate ORDER BY t.statdate DESC";
         paramNames.add("pathId");
         values.add(pathId);
         paramNames.add("dataAuth");
         values.add(dataAuth);
         paramNames.add("ifLone");
         values.add(ifLone);
         logger.info("queryHotWordStat-> getDateList-> dataType:" + dataType + " batchId:" + batchId + " ifLone:" + ifLone + " sql: " + sql);
         return (List)this.hiberTemp.createSQLQuery(sql, paramNames, values);
      } catch (Exception var11) {
         logger.error("getDateList DB has a error!", var11);
         return list;
      }
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
