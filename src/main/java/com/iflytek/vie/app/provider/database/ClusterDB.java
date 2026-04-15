package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.topic.TopicOptionLoad;
import com.iflytek.vie.app.pojo.topic.TopicPath;
import com.iflytek.vie.app.provider.pojo.Cluster;
import com.iflytek.vie.app.provider.pojo.ClusterTask;
import com.iflytek.vie.constants.Constants.ClusterStatus;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.ArrayList;
import java.util.List;

public class ClusterDB {
   private HibernateTemplateUtils hibernateTemplate;

   public String getClusterInfoFromPath(Long cid, String dataLimit, Integer dataType, int ifLone, String batchId) throws Exception {
      if (cid != null && cid > 0L) {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         String hql = "select c.chartLeftInfo  from Cluster c where c.togatherId = :cid  and c.dataAuth = :dataLimit and c.ifLone = :ifLone ";
         if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
            hql = hql + " and c.batchId = :batchId";
            paramNames.add("batchId");
            values.add(batchId);
         }

         if (dataType != null && dataType != 2) {
            hql = hql + " and c.dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
         }

         paramNames.add("ifLone");
         values.add(ifLone);
         paramNames.add("cid");
         values.add(cid);
         paramNames.add("dataLimit");
         values.add(dataLimit);
         return (String)this.hibernateTemplate.uniqueResult(hql, paramNames, values);
      } else {
         return "";
      }
   }

   public String getClusterInfoFromPia(Long cid, Long pid, String dataLimit, Integer dataType, int ifLone, String batchId) throws Exception {
      List<String> result = null;
      if (cid != null && cid != 0L) {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         String hql = "select c.chartLeftInfo  from Cluster c where c.parentId = :cid and c.dataAuth = :dataLimit and c.ifLone = :ifLone ";
         if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
            hql = hql + " and c.batchId = :batchId";
            paramNames.add("batchId");
            values.add(batchId);
         }

         if (dataType != null && dataType != 2) {
            hql = hql + " and c.dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
         }

         paramNames.add("ifLone");
         values.add(ifLone);
         paramNames.add("cid");
         values.add(cid);
         paramNames.add("dataLimit");
         values.add(dataLimit);
         result = (List)this.hibernateTemplate.createQuery(hql, paramNames, values);
      } else {
         List<String> paramNamesx = new ArrayList<>();
         List<Object> valuesx = new ArrayList<>();
         String hqlx = "select c.chartLeftInfo  from Cluster c where c.pathId = :pid and c.parentId = 0 and c.dataAuth = :dataLimit and c.ifLone = :ifLone";
         if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
            hqlx = hqlx + " and c.batchId = :batchId";
            paramNamesx.add("batchId");
            valuesx.add(batchId);
         }

         if (dataType != null && dataType != 2) {
            hqlx = hqlx + " and c.dataType = :dataType";
            paramNamesx.add("dataType");
            valuesx.add(dataType);
         }

         hqlx = hqlx + " order by c.togatherId";
         paramNamesx.add("ifLone");
         valuesx.add(ifLone);
         paramNamesx.add("pid");
         valuesx.add(pid);
         paramNamesx.add("dataLimit");
         valuesx.add(dataLimit);
         result = (List)this.hibernateTemplate.createQuery(hqlx, paramNamesx, valuesx);
      }

      return result != null && result.size() > 0 ? result.get(0) : "";
   }

   public String getHotviewById(Long id, Integer dataType, int ifLone, String batchId) throws Exception {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String hql = "select c.charRightInfo from Cluster c where c.togatherId = :id and c.ifLone = :ifLone";
      if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
         hql = hql + " and c.batchId = :batchId";
         paramNames.add("batchId");
         values.add(batchId);
      }

      if (dataType != null && dataType != 2) {
         hql = hql + " and c.dataType = :dataType";
         paramNames.add("dataType");
         values.add(dataType);
      }

      paramNames.add("ifLone");
      values.add(ifLone);
      paramNames.add("id");
      values.add(id);
      String result = (String)this.hibernateTemplate.uniqueResult(hql, paramNames, values);
      return result == null ? "" : result;
   }

   public long createCluster(Long cid, long pid, String dataLimit, Integer dataType, int ifLone, String batchId) throws Exception {
      long taskId = 0L;
      TopicPath tp = (TopicPath)this.hibernateTemplate.get(TopicPath.class, (int)pid);
      ClusterTask c = new ClusterTask();
      c.setDataAuth(dataLimit);
      c.setPathId(pid);
      c.setTogatherId(cid);
      c.setBatchId(batchId);
      c.setDataType(dataType);
      c.setIfLone(ifLone);
      c.setTogatherStatus(ClusterStatus.NOT_STARTED);
      c.setPathContent(tp.getPathCondition());
      this.hibernateTemplate.save(c);
      return c.getTaskId();
   }

   public int getClusterStatus(Long cid, long pid, String dataLimit, boolean isFlushTime, Integer dataType, int ifLone, String batchId) throws Exception {
      int status = 0;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String hql = "select c from ClusterTask c where c.togatherId = :cid and c.pathId = :pid and c.dataAuth = :dataLimit and c.ifLone = :ifLone ";
      if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
         hql = hql + " and c.batchId = :batchId";
         paramNames.add("batchId");
         values.add(batchId);
      }

      if (dataType != null && dataType != 2) {
         hql = hql + " and c.dataType = :dataType";
         paramNames.add("dataType");
         values.add(dataType);
      }

      paramNames.add("ifLone");
      values.add(ifLone);
      paramNames.add("cid");
      values.add(cid);
      paramNames.add("pid");
      values.add(pid);
      paramNames.add("dataLimit");
      values.add(dataLimit);
      List<ClusterTask> list = (List<ClusterTask>)(List<?>)this.hibernateTemplate.createQuery(hql, paramNames, values);
      ClusterTask c = new ClusterTask();
      if (list != null && list.size() > 0) {
         c = list.iterator().next();
         status = c.getTogatherStatus();
      } else {
         status = ClusterStatus.NO_CLUSTER;
      }

      TopicPath tp = (TopicPath)this.hibernateTemplate.get(TopicPath.class, (int)pid);
      if (null == tp) {
         tp = new TopicPath();
      }

      String tpCondition = tp.getPathCondition() == null ? "" : tp.getPathCondition();
      if ((status == 3 || status == 5) && tp != null && !tpCondition.equals(c.getPathContent() == null ? "" : c.getPathContent())) {
         String hql1 = "delete from ClusterTask where pathId = :pid and togatherStatus != 2 and dataAuth = :dataLimit and ifLone = :ifLone";
         if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
            hql1 = hql1 + " and batchId = :batchId";
         }

         if (dataType != null && dataType != 2) {
            hql1 = hql1 + " and dataType = :dataType";
         }

         paramNames.remove("cid");
         values.remove(cid);
         this.hibernateTemplate.bulkUpdate(hql1, paramNames, values);
         this.hibernateTemplate.bulkUpdate("delete from TopicOptionLoad  where path.pathId = ? and dataAuth =? ", new Object[]{(int)pid, dataLimit});
         String hql2 = "delete from TopicTogatherInfo where path.pathId = :pid and dataAuth = :dataLimit and ifLone = :ifLone";
         if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
            hql2 = hql2 + " and batchId = :batchId";
         }

         if (dataType != null && dataType != 2) {
            hql2 = hql2 + " and dataType = :dataType";
         }

         paramNames.remove("pid");
         paramNames.add("pid");
         values.remove(pid);
         values.add((int)pid);
         this.hibernateTemplate.bulkUpdate(hql2, paramNames, values);
         if (isFlushTime) {
            status = ClusterStatus.NO_CLUSTER;
         }
      } else if ((status == 3 || status == 5) && tp != null && tpCondition.equals(c.getPathContent() == null ? "" : c.getPathContent())) {
         String hql1x = "delete from ClusterTask where pathId = :pid and togatherStatus != 2 and dataAuth = :dataLimit  and ifLone = :ifLone ";
         if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
            hql1x = hql1x + " and batchId = :batchId";
         }

         if (dataType != null && dataType != 2) {
            hql1x = hql1x + " and dataType = :dataType";
         }

         paramNames.remove("cid");
         values.remove(cid);
         this.hibernateTemplate.bulkUpdate(hql1x, paramNames, values);
         this.hibernateTemplate.bulkUpdate("delete from TopicOptionLoad  where path.pathId = ? and dataAuth =? ", new Object[]{(int)pid, dataLimit});
         String hql2x = "delete from TopicTogatherInfo where path.pathId = :pid and dataAuth = :dataLimit and ifLone = :ifLone";
         if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
            hql2x = hql2x + " and batchId = :batchId";
         }

         if (dataType != null && dataType != 2) {
            hql2x = hql2x + " and dataType = :dataType";
         }

         paramNames.remove("pid");
         paramNames.add("pid");
         values.remove(pid);
         values.add((int)pid);
         this.hibernateTemplate.bulkUpdate(hql2x, paramNames, values);
         status = ClusterStatus.NO_CLUSTER;
      }

      return status;
   }

   public int getClusterStatus2(Long cid, long pid, String dataLimit, Integer dataType, int ifLone, String batchId) throws Exception {
      int status = 0;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String hql = "select c from ClusterTask c where c.togatherId = :cid and c.pathId = :pid and c.dataAuth = :dataLimit and c.ifLone = :ifLone ";
      if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
         hql = hql + " and c.batchId = :batchId";
         paramNames.add("batchId");
         values.add(batchId);
      }

      if (dataType != null && dataType != 2) {
         hql = hql + " and c.dataType = :dataType";
         paramNames.add("dataType");
         values.add(dataType);
      }

      paramNames.add("ifLone");
      values.add(ifLone);
      paramNames.add("cid");
      values.add(cid);
      paramNames.add("pid");
      values.add(pid);
      paramNames.add("dataLimit");
      values.add(dataLimit);
      List<ClusterTask> list = (List<ClusterTask>)(List<?>)this.hibernateTemplate.createQuery(hql, paramNames, values);
      ClusterTask c = new ClusterTask();
      if (list != null && list.size() > 0) {
         c = list.iterator().next();
         status = c.getTogatherStatus();
      } else {
         status = ClusterStatus.NO_CLUSTER;
      }

      TopicPath tp = (TopicPath)this.hibernateTemplate.get(TopicPath.class, (int)pid);
      if (null == tp) {
         tp = new TopicPath();
      }

      String tpCondition = tp.getPathCondition() == null ? "" : tp.getPathCondition();
      if ((status == 3 || status == 5) && tp != null && !tpCondition.equals(c.getPathContent() == null ? "" : c.getPathContent())) {
      }

      return status;
   }

   public long isCluExist(Long cid, Long pid, String dataLimit, Integer dataType, int ifLone, String batchId) throws Exception {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String hql = "select count(*) from Cluster c where c.pathId= :pid and c.parentId= :cid and c.dataAuth = :dataLimit and c.ifLone = :ifLone ";
      if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
         hql = hql + " and c.batchId = :batchId";
         paramNames.add("batchId");
         values.add(batchId);
      }

      if (dataType != null && dataType != 2) {
         hql = hql + " and c.dataType = :dataType";
         paramNames.add("dataType");
         values.add(dataType);
      }

      paramNames.add("ifLone");
      values.add(ifLone);
      paramNames.add("pid");
      values.add(pid);
      paramNames.add("cid");
      values.add(cid);
      paramNames.add("dataLimit");
      values.add(dataLimit);
      return (Long)this.hibernateTemplate.uniqueResult(hql, paramNames, values);
   }

   public void reCreateClu(Long cid, Long pid, String dataLimit, Integer dataType, int ifLone, String batchId) {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String hql = "select tc from ClusterTask tc where tc.togatherStatus = 4 and tc.pathId = :pid and tc.togatherId = :cid and tc.dataAuth = :dataLimit and tc.ifLone = :ifLone ";
      if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
         hql = hql + " and tc.batchId = :batchId";
         paramNames.add("batchId");
         values.add(batchId);
      }

      if (dataType != null && dataType != 2) {
         hql = hql + " and tc.dataType = :dataType";
         paramNames.add("dataType");
         values.add(dataType);
      }

      paramNames.add("ifLone");
      values.add(ifLone);
      paramNames.add("pid");
      values.add(pid);
      paramNames.add("cid");
      values.add(cid);
      paramNames.add("dataLimit");
      values.add(dataLimit);
      List<ClusterTask> list = (List<ClusterTask>)(List<?>)this.hibernateTemplate.createQuery(hql, paramNames, values);
      if (list.size() > 0) {
         ClusterTask ct = list.get(0);
         this.hibernateTemplate.delete(ct);
      }
   }

   public List<Cluster> queryClusterByPathID(String pathID, String dataLimit) {
      return (List)this.hibernateTemplate.find("from Cluster rc where  rc.pathId= ? and rc.dataAuth =? ", new Object[]{Long.valueOf(pathID), dataLimit});
   }

   public List<ClusterTask> queryClusterTaskByPathID(String pathID, String dataLimit) {
      return (List)this.hibernateTemplate.find("from ClusterTask rc where  rc.pathId= ? and rc.dataAuth = ? ", new Object[]{Long.valueOf(pathID), dataLimit});
   }

   public ClusterTask queryClusterTaskByPathID2(long pid, String dataLimit, Integer dataType, int ifLone, String batchId) {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String hql = "select rc from ClusterTask rc where  rc.pathId= :pid and rc.dataAuth = :dataLimit and rc.togatherStatus = 3 and rc.togatherId = 0 and rc.ifLone = :ifLone ";
      if (ifLone == 1 && batchId != null && !"".equals(batchId)) {
         hql = hql + " and rc.batchId = :batchId";
         paramNames.add("batchId");
         values.add(batchId);
      }

      if (dataType != null && dataType != 2) {
         hql = hql + " and rc.dataType = :dataType";
         paramNames.add("dataType");
         values.add(dataType);
      }

      paramNames.add("ifLone");
      values.add(ifLone);
      paramNames.add("pid");
      values.add(pid);
      paramNames.add("dataLimit");
      values.add(dataLimit);
      List<ClusterTask> tasks = (List<ClusterTask>)(List<?>)this.hibernateTemplate.createQuery(hql, paramNames, values);
      return tasks != null && tasks.size() != 0 ? tasks.get(0) : null;
   }

   public void deleteClusterTask(ClusterTask cluster) {
      this.hibernateTemplate.delete(cluster);
   }

   public void deleteCluster(Cluster cluster) {
      this.hibernateTemplate.delete(cluster);
   }

   public List<TopicOptionLoad> queryTOLByPathID(String pathID, String dataLimit) {
      return (List<TopicOptionLoad>)(List<?>)this.hibernateTemplate
         .find("from TopicOptionLoad rc where  rc.path.pathId= ? and rc.dataAuth = ?", new Object[]{Integer.valueOf(pathID), dataLimit});
   }

   public void deleterTopicOptionLoad(TopicOptionLoad cluster) {
      this.hibernateTemplate.delete(cluster);
   }

   public void setHibernateTemplate(HibernateTemplateUtils hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
