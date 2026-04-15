package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.datamining.DayClusterDocMap;
import com.iflytek.vie.app.pojo.datamining.DayClusterHotviewMap;
import com.iflytek.vie.utils.DateUtils;
import java.util.Date;
import java.util.List;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class DayClusterDB {
   private final Logger logger = LoggerFactory.getLogger(DayClusterDB.class);
   private HibernateTemplate hibernateTemplate;

   public String getDayCluster(String dataSource, Date date, int isSentence, String dataAuth, String batchId, int ifLone, int dataType) throws Exception {
      Session session = null;
      String dateString = DateUtils.parseDateToString(date, "yyyy-MM-dd");
      String rs = "";
      String yesterday = DateUtils.getDaybeforeOrAfter(dateString, -1);
      Long yesterdayLong = Long.parseLong(yesterday.replace("-", ""));
      StringBuilder sql = new StringBuilder();
      String center = "";
      if (isSentence == 0) {
         sql.append("select new DayClusterHotviewMap(id,createDate,clusterList,clusterDate) ");
         sql.append(" from DayClusterHotviewMap  where dataSource=? and clusterDate=? and ifLone=? and batchId=? and dataType=?");
      } else {
         sql.append("from DayClusterHotviewMap  where dataSource=? and clusterDate=?  and ifLone=? and batchId=? and dataType=?");
      }

      if (dataAuth.length() > 0) {
         if (dataAuth.split(",").length > 1) {
            center = "all";
         } else {
            center = dataAuth.replace(",", "");
         }

         sql.append(" and centerName  = '" + center + "'");

         try {
            List list = (List)this.hibernateTemplate.find(sql.toString(), new Object[]{dataSource, yesterdayLong, ifLone, batchId, dataType});
            DayClusterHotviewMap dayClusterHotviewMap = null;
            if (list.size() > 0) {
               dayClusterHotviewMap = (DayClusterHotviewMap)list.get(0);
               rs = dayClusterHotviewMap.getClusterList();
            } else {
               rs = "[]";
            }

            return rs;
         } catch (Exception var17) {
            this.logger.info("查询聚类数据库失败！");
            throw new ViePlatformServiceException("获取聚类数据失败！", var17);
         }
      } else {
         return "[]";
      }
   }

   public Long getDayClusterId(String selectTime, String center, String dataSource) throws Exception {
      Long dayClusterId = 0L;
      DayClusterHotviewMap dayClusterHotviewMap = null;
      Long timeLong = Long.parseLong(selectTime.replace("-", ""));

      try {
         String sql = "select new DayClusterHotviewMap(id,createDate,clusterList,clusterDate)  from DayClusterHotviewMap  where clusterDate=? and centerName ='"
            + center
            + "' and dataSource ='"
            + dataSource
            + "'";
         List list = (List)this.hibernateTemplate.find(sql.toString(), new Object[]{timeLong});
         if (list != null && list.size() != 0) {
            dayClusterHotviewMap = (DayClusterHotviewMap)list.get(0);
            dayClusterId = dayClusterHotviewMap.getId();
         }

         return dayClusterId;
      } catch (Exception var9) {
         this.logger.info("getDayClusterId=>查询聚类数据库失败！");
         throw new ViePlatformServiceException("getDayClusterId=>查询聚类数据库失败！！", var9);
      }
   }

   public String getCluserTelephoneId(Long dayClusterId, int clusterId) throws Exception {
      String telephoneId = "";

      try {
         String sql = "SELECT new DayClusterDocMap(id,dayClusterId,cluId,docString) from DayClusterDocMap where dayClusterId=? and cluId =?";
         List list = (List)this.hibernateTemplate.find(sql.toString(), new Object[]{dayClusterId, clusterId});
         if (list != null && list.size() != 0) {
            DayClusterDocMap dayClusterDocMap = (DayClusterDocMap)list.get(0);
            telephoneId = dayClusterDocMap.getDocString();
         }

         return telephoneId;
      } catch (Exception var7) {
         this.logger.info("查询聚类流水号失败！");
         throw new ViePlatformServiceException("查询聚类流水号失败！！", var7);
      }
   }

   public HibernateTemplate getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
