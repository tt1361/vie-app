package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.DimensionPersonal;
import com.iflytek.vie.app.pojo.dimension.DimensionPersonalClick;
import com.iflytek.vie.app.pojo.dimension.DimensionPersonalTask;
import com.iflytek.vie.app.pojo.dimension.DimensionPersonalValue;
import com.iflytek.vie.app.pojo.dimension.DimensionTask;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.dynamic.DynamicDataSourceContextHolder;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.ListUtil;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class DimensionDBPlatform {
   private final Logger logger = LoggerFactory.getLogger(DimensionDBPlatform.class);
   private HibernateTemplate hibernateTemplate;
   private JdbcTemplate jdbcTemplate;
   private HibernateTemplateUtils hibernateTemplateUtils;

   public List<DimensionConfig> getDimListByDataSource(final String dataSource) throws ViePlatformServiceException {
      List<DimensionConfig> list = new ArrayList<>();

      try {
         if (dataSource != null && !"".equals(dataSource)) {
            String sql = "select d from DimensionConfig d left join fetch d.valueList c where d.dataSource = ?";
            list = (List<DimensionConfig>)this.getHibernateTemplate().execute(new HibernateCallback<List<DimensionConfig>>() {
               public List<DimensionConfig> doInHibernate(Session session) throws HibernateException {
                  Query query = session.createQuery("select d from DimensionConfig d left join fetch d.valueList c where d.dataSource = ?");
                  query.setString(0, dataSource);
                  return query.list();
               }
            });
         }

         return list;
      } catch (Exception var4) {
         this.logger.error("获取全部维度取值db类异常");
         throw new ViePlatformServiceException("获取全部维度取值db类异常", var4);
      }
   }

   public List<DimensionConfig> searchDim(String dataSource, String keyword) throws ViePlatformServiceException {
      List<DimensionConfig> list = new ArrayList<>();
      int insightType = IndexConstants.getInsightType(dataSource);

      try {
         if (dataSource != null && !"".equals(dataSource)) {
            String sql = "";
            if (0 == insightType) {
               if (StringUtils.isNullOrEmpry(keyword)) {
                  sql = "select d from DimensionConfig d where d.dataSource = ? and INDEX_FIELD!= 'taskId'";
               } else {
                  if (keyword.contains("_")) {
                     keyword = keyword.replace("_", "\\_");
                  }

                  sql = "select d from DimensionConfig d where d.dataSource = ? and INDEX_FIELD!= 'taskId' and d.showName like '%" + keyword + "%'escape '\\'";
               }
            } else if (1 == insightType) {
               if (StringUtils.isNullOrEmpry(keyword)) {
                  sql = "select d from DimensionConfig d where d.dataSource = ?";
               } else {
                  if (keyword.contains("_")) {
                     keyword = keyword.replace("_", "\\_");
                  }

                  sql = "select d from DimensionConfig d where d.dataSource = ?  and d.showName like '%" + keyword + "%'escape '\\'";
               }
            }

            list = (List)this.hibernateTemplate.find(sql, new Object[]{dataSource});
         }

         return list;
      } catch (Exception var6) {
         this.logger.error("获取全部维度取值db类(模糊查询)异常", var6);
         throw new ViePlatformServiceException("获取全部维度取值db类（模糊查询）异常", var6);
      }
   }

   public List<DimensionConfig> searchAllDim(String dataSource) throws ViePlatformServiceException {
      List<DimensionConfig> list = new ArrayList<>();

      try {
         DynamicDataSourceContextHolder.setDataSourceType(dataSource);
         if (!StringUtils.isNullOrEmpry(dataSource)) {
            String sql = "";
            sql = "select d from DimensionConfig d where d.dataSource = ?";
            list = (List)this.hibernateTemplate.find(sql, new Object[]{dataSource});
         }
      } catch (Exception var4) {
         this.logger.error("method searchAllDim exception");
      }

      return list;
   }

   public List<DimensionConfig> searchSystemDim(String dataSource, String keyword) throws ViePlatformServiceException {
      List<DimensionConfig> list = new ArrayList<>();

      try {
         if (!StringUtils.isNullOrEmpry(dataSource)) {
            String sql = "";
            if (StringUtils.isNullOrEmpry(keyword)) {
               sql = "select d from DimensionConfig d where d.dataSource = ?";
            } else {
               sql = "select d from DimensionConfig d where d.dataSource = ? and d.showName like '%" + keyword + "%'";
            }

            sql = sql + " and d.indexField!='timeFormat' ";
            list = (List)this.hibernateTemplate.find(sql, new Object[]{dataSource});
         }
      } catch (Exception var5) {
         this.logger.error("method searchSystemDim exception");
      }

      return list;
   }

   public DimensionConfig getValuesOfDim(String dataSource, String key) throws ViePlatformServiceException {
      DimensionConfig dimensionConfig = new DimensionConfig();

      try {
         String hql = "select d from DimensionConfig d where d.dataSource = ? and d.indexField = ?";
         List<DimensionConfig> dcList = (List<DimensionConfig>)(List<?>)this.hibernateTemplate.find(hql, new Object[]{dataSource, key});
         if (!ListUtil.isNullOrEmpty(dcList)) {
            dimensionConfig = dcList.get(0);
         }

         return dimensionConfig;
      } catch (Exception var6) {
         this.logger.error("根据数据源和key值获取维度值db类异常");
         throw new ViePlatformServiceException("根据数据源和key值获取维度值db类异常", var6);
      }
   }

   public List<DimensionPersonal> searchAllDP(String dimensionName, String dataSource) throws Exception {
      String hql = "";
      if (StringUtils.isNullOrEmpry(dimensionName)) {
         hql = "SELECT DP FROM DimensionPersonal DP left join fetch DP.dimensionValueSet DPV WHERE DP.dimensionState=0 AND DP.dataSource=? ORDER BY DP.id desc ";
      } else {
         dimensionName = dimensionName.replace("_", "\\_");
         hql = "SELECT DP FROM DimensionPersonal DP left join fetch DP.dimensionValueSet DPV WHERE DP.dimensionName LIKE '%"
            + dimensionName
            + "%' ESCAPE '\\' AND DP.dimensionState=0 AND DP.dataSource=? ORDER  BY DP.id desc";
      }

      return (List)this.hibernateTemplate.find(hql, new Object[]{dataSource});
   }

   public List<DimensionPersonal> searchAllDimensionPersonal(String dimensionName, String dataSource) throws Exception {
      String hql = "";
      if (StringUtils.isNullOrEmpry(dimensionName)) {
         hql = "SELECT DP FROM DimensionPersonal DP WHERE DP.dimensionState=0 AND DP.dataSource=? ORDER BY DP.id desc ";
      } else {
         dimensionName = dimensionName.replace("_", "\\_");
         hql = "SELECT DP FROM DimensionPersonal DP  WHERE DP.dimensionName LIKE '%"
            + dimensionName
            + "%' ESCAPE '\\' AND DP.dimensionState=0 AND DP.dataSource=? ORDER  BY DP.id desc";
      }

      return (List)this.hibernateTemplate.find(hql, new Object[]{dataSource});
   }

   public List<DimensionConfig> getSystemDimensions(String dimensionName, String dataSource) throws Exception {
      String hql = "SELECT DC FROM DimensionConfig DC WHERE DC.dataSource='" + dataSource + "'";
      if (!StringUtils.isNullOrEmpry(dimensionName)) {
         dimensionName = dimensionName.replace("_", "\\_");
         hql = "SELECT DC FROM DimensionConfig DC WHERE DC.dataSource='" + dataSource + "' AND DC.showName LIKE '%" + dimensionName + "%'";
         if (dimensionName.contains("\\")) {
            hql = hql + " ESCAPE '\\'";
         }
      }

      return (List)this.hibernateTemplate.find(hql, new Object[0]);
   }

   public boolean isHaveSameName(String name, long dimensionId, String dataSource) throws Exception {
      String hql = "SELECT DC FROM DimensionConfig DC WHERE DC.showName=? AND DC.id !=? AND DC.dataSource=?";
      List<DimensionConfig> list = (List<DimensionConfig>)(List<?>)this.hibernateTemplate.find(hql, new Object[]{name, dimensionId, dataSource});
      return !ListUtil.isNullOrEmpty(list);
   }

   public boolean isEqualPersonalDimension(String name, String dataSource) throws Exception {
      String hql = "SELECT DP FROM DimensionPersonal DP WHERE DP.dimensionName=? AND DP.dataSource=? AND DP.dimensionState=0";
      List<DimensionPersonal> list = (List<DimensionPersonal>)(List<?>)this.hibernateTemplate.find(hql, new Object[]{name, dataSource});
      return !ListUtil.isNullOrEmpty(list);
   }

   public DimensionConfig getSystemDimensionById(long dimensionId, String dataSource) throws Exception {
      String hql = "SELECT DC FROM DimensionConfig DC WHERE DC.id=? AND DC.dataSource=?";
      List<DimensionConfig> list = (List<DimensionConfig>)(List<?>)this.hibernateTemplate.find(hql, new Object[]{dimensionId, dataSource});
      return ListUtil.isNullOrEmpty(list) ? null : list.get(0);
   }

   public void updateSystemDimension(DimensionConfig dc) throws Exception {
      this.hibernateTemplate.update(dc);
   }

   public List<DimensionConfig> getSystemDimensionByField(String field, String dataSource) {
      String hql = "SELECT DC FROM DimensionConfig DC WHERE DC.dataSource=? AND DC.indexField=?";
      return (List)this.hibernateTemplate.find(hql, new Object[]{dataSource, field});
   }

   public List<DimensionPersonal> getDimensionByAnoterName(List<String> anotherName) {
      String hql = "SELECT DP FROM DimensionPersonal DP WHERE DP.dimensionAnotherName in (:names)";
      Session session = null;
      List<DimensionPersonal> dpList = new ArrayList<>();

      try {
         session = this.hibernateTemplate.getSessionFactory().openSession();
         Query query = session.createQuery(hql).setParameterList("names", anotherName);
         dpList = query.list();
      } catch (Exception var9) {
         this.logger.error("根据名称查询自定义维度错误", var9);
      } finally {
         session.clear();
         session.close();
      }

      return dpList;
   }

   public List<DimensionPersonalClick> getRoidDimension(List<Long> roids, String tableName) throws Exception {
      StringBuffer roidStr = new StringBuffer();

      for (int i = 0; i < roids.size(); i++) {
         if (i == roids.size() - 1) {
            roidStr.append(roids.get(i));
         } else {
            roidStr.append(roids.get(i) + ",");
         }
      }

      String sql = "select t.dimension_another_name,t.role_id,dp.dimension_id,dp.dimension_name,dp.dimension_state,dp.datasource from dimension_personal_click t inner join dimension_personal dp on t.dimension_another_name=dp.dimension_another_name where t.role_id in ("
         + roidStr
         + ") and dp.dataSource = '"
         + tableName
         + "' and dp.dimension_state=0";
      List<Map<String, Object>> resultMapList = this.jdbcTemplate.queryForList(sql);
      List<DimensionPersonalClick> dpcList = new ArrayList<>();

      for (Map<String, Object> map : (List<Map<String, Object>>)resultMapList) {
         DimensionPersonalClick dpc = new DimensionPersonalClick();
         dpc.setDimensionAnoterName(String.valueOf(map.get("dimension_another_name")));
         dpc.setRoleId(Long.parseLong(String.valueOf(map.get("role_id"))));
         dpcList.add(dpc);
      }

      return dpcList;
   }

   public List<DimensionPersonalClick> getRoidDimension(List<Long> roids) throws Exception {
      String hql = "SELECT DPC FROM DimensionPersonalClick DPC WHERE DPC.roleId in (:roid)";
      Session session = null;
      List<DimensionPersonalClick> dpcList = new ArrayList<>();

      try {
         session = this.hibernateTemplate.getSessionFactory().openSession();
         Query query = session.createQuery(hql).setParameterList("roid", roids);
         dpcList = query.list();
      } catch (Exception var9) {
         this.logger.error("method getRoidDimension exception");
      } finally {
         session.clear();
         session.close();
      }

      return dpcList;
   }

   public void deleteRoidDimension(List<Long> roids) throws Exception {
      for (DimensionPersonalClick dpc : this.getRoidDimension(roids)) {
         this.hibernateTemplate.delete(dpc);
      }
   }

   public boolean isHaveSaveNamePersonal(String name, long id, String dataSource) throws Exception {
      String hql = "SELECT DP FROM DimensionPersonal DP WHERE DP.dimensionName=? AND DP.id != ? AND DP.dataSource=? AND DP.dimensionState=0";
      List<DimensionPersonal> list = (List<DimensionPersonal>)(List<?>)this.hibernateTemplate.find(hql, new Object[]{name, id, dataSource});
      return !ListUtil.isNullOrEmpty(list);
   }

   public DimensionPersonal searchPersonalDimension(long diemsnionId, String dataSource) throws Exception {
      String hql = "SELECT DP FROM DimensionPersonalValue DPV RIGHT JOIN DPV.dimensionPersonal DP  WHERE DP.dimensionId=? AND DP.dataSource=? AND DP.dimensionState=0 ORDER BY DPV.showIndex asc";
      List<DimensionPersonal> list = (List<DimensionPersonal>)(List<?>)this.hibernateTemplate.find(hql, new Object[]{diemsnionId, dataSource});
      return ListUtil.isNullOrEmpty(list) ? null : list.get(0);
   }

   public void updateDimensionPersonal(DimensionPersonal dp) throws Exception {
      this.hibernateTemplate.update(dp);
   }

   public void deleteDimensionPersonal(DimensionPersonal dp) throws Exception {
      this.hibernateTemplate.delete(dp);
   }

   public void saveDimensionValue(List<DimensionPersonalValue> dpvList) throws Exception {
      for (DimensionPersonalValue dpv : dpvList) {
         this.hibernateTemplate.save(dpv);
      }
   }

   public void deleteDimensionValue(long parentId) throws Exception {
      String hql = "DELETE FROM DimensionPersonalValue WHERE dimensionPersonal.dimensionId=?";
      this.hibernateTemplate.bulkUpdate(hql, new Object[]{parentId});
   }

   public DimensionPersonal addDimensionPersonal(DimensionPersonal dp) throws Exception {
      this.hibernateTemplate.save(dp);
      return dp;
   }

   public Map<String, Object> getAllDimensionTask(int pageNum, int pageSize) throws Exception {
      String hql = "SELECT COUNT(*) FROM DimensionTask DT ORDER BY DT.createTime DESC ";
      List<Long> count = (List<Long>)(List<?>)this.hibernateTemplate.find(hql, new Object[0]);
      Map<String, Object> result = new HashMap<>();
      DetachedCriteria criteria = DetachedCriteria.forClass(DimensionTask.class);
      criteria.addOrder(Order.desc("createTime"));
      criteria.addOrder(Order.desc("taskId"));
      int start = (pageNum - 1) * pageSize;
      List<DimensionTask> list = (List<DimensionTask>)(List<?>)this.hibernateTemplate.findByCriteria(criteria, start, pageSize);
      result.put("totalSize", count.get(0));
      result.put("tasks", list);
      return result;
   }

   public Map<String, Object> getAllPersonalTasks(long taskId, int pageNum, int pageSize) throws Exception {
      String hql = "SELECT DPT FROM DimensionPersonalTask DPT WHERE DPT.dimensionTask.id=? AND DPT.state=?";
      Session session = null;
      Map<String, Object> result = new HashMap<>();

      try {
         session = this.hibernateTemplate.getSessionFactory().openSession();
         Query query = session.createQuery(hql).setParameter(0, taskId).setParameter(1, -1);
         List<DimensionPersonalTask> dptList = query.list();
         int start = (pageNum - 1) * pageSize;
         query = session.createQuery(hql).setParameter(0, taskId).setParameter(1, -1).setFirstResult(start).setMaxResults(pageSize);
         List<DimensionPersonalTask> dptLists = query.list();
         int count = 0;
         if (!ListUtil.isNullOrEmpty(dptList)) {
            count = dptList.size();
         }

         result.put("totalSize", count);
         result.put("tasks", dptLists);
      } catch (Exception var16) {
         this.logger.error("method getAllPersonalTasks exception");
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return result;
   }

   public List<DimensionPersonal> selectDimensionByType(String type, String name, String dataSource) throws Exception {
      String hql = "";
      if (!StringUtils.isNullOrEmpry(name)) {
         name = name.replace("_", "/_");
         hql = "SELECT  DP FROM DimensionPersonalValue DPV INNER JOIN DPV.dimensionPersonal DP WHERE DP.dimensionType=? AND DP.dataSource=? AND DP.dimensionState=0 AND DP.dimensionName LIKE '%"
            + name
            + "%' ESCAPE '/' ORDER BY DP.id desc,DPV.showIndex asc";
      } else {
         hql = "SELECT  DP FROM DimensionPersonalValue DPV INNER JOIN DPV.dimensionPersonal DP WHERE DP.dimensionType=? AND DP.dataSource=? AND DP.dimensionState=0 ORDER BY DP.id desc,DPV.showIndex asc";
      }

      return (List)this.hibernateTemplate.find(hql, new Object[]{type, dataSource});
   }

   public DimensionPersonal getPersonalDimensionById(long dimensionId) throws Exception {
      String hql = "SELECT DP FROM DimensionPersonalValue DPV RIGHT JOIN DPV.dimensionPersonal DP  WHERE DP.dimensionId=? AND DP.dimensionState=0 ORDER BY DPV.showIndex asc";
      List<DimensionPersonal> list = (List<DimensionPersonal>)(List<?>)this.hibernateTemplate.find(hql, new Object[]{dimensionId});
      return ListUtil.isNullOrEmpty(list) ? null : list.get(0);
   }

   public void saveDimensionClick(List<DimensionPersonalClick> dpcList) throws Exception {
      for (DimensionPersonalClick dpc : dpcList) {
         this.hibernateTemplate.saveOrUpdate(dpc);
      }
   }

   public DimensionPersonalValue selectValueById(long valueId) throws Exception {
      String hql = "SELECT DPV FROM DimensionPersonalValue DPV WHERE DPV.id=?";
      List<DimensionPersonalValue> list = (List<DimensionPersonalValue>)(List<?>)this.hibernateTemplate.find(hql, new Object[]{valueId});
      return ListUtil.isNullOrEmpty(list) ? null : list.get(0);
   }

   public boolean isHaveSaveName(String name, long id, String dataSource) throws Exception {
      String hql = "SELECT DP FROM DimensionPersonal DP WHERE DP.dimensionName=? AND DP.id != ? AND DP.dimensionState=0 AND DP.dataSource=?";
      List<DimensionPersonal> list = (List<DimensionPersonal>)(List<?>)this.hibernateTemplate.find(hql, new Object[]{name, id, dataSource});
      return !ListUtil.isNullOrEmpty(list);
   }

   public DimensionPersonal getpersonDimensionByName(String dimensionName, String dataSource) throws Exception {
      String hql = "SELECT DP FROM DimensionPersonal DP WHERE DP.dimensionName=? AND DP.dimensionState=0 AND DP.dataSource=?";
      List<DimensionPersonal> list = (List<DimensionPersonal>)(List<?>)this.hibernateTemplate.find(hql, new Object[]{dimensionName, dataSource});
      return ListUtil.isNullOrEmpty(list) ? null : list.get(0);
   }

   public DimensionTask saveDimensionTask(DimensionTask dt) {
      this.hibernateTemplate.save(dt);
      return dt;
   }

   public void savePersonTaskList(List<DimensionPersonalTask> dptList) throws Exception {
      for (DimensionPersonalTask dpt : dptList) {
         this.hibernateTemplate.saveOrUpdate(dpt);
      }
   }

   public boolean updatePersonalDimension(DimensionPersonal dp) throws Exception {
      this.hibernateTemplate.update(dp);
      return true;
   }

   public HibernateTemplate getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }

   public JdbcTemplate getJdbcTemplate() {
      return this.jdbcTemplate;
   }

   public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
   }

   public HibernateTemplateUtils getHibernateTemplateUtils() {
      return this.hibernateTemplateUtils;
   }

   public void setHibernateTemplateUtils(HibernateTemplateUtils hibernateTemplateUtils) {
      this.hibernateTemplateUtils = hibernateTemplateUtils;
   }
}
