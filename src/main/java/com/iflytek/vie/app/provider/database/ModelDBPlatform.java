package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.ModelGroup;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.app.pojo.model.ModelOfflineInfo;
import com.iflytek.vie.app.provider.common.SqlHelperPlatform;
import com.iflytek.vie.app.provider.ruleparse.ModelStatus;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.ListUtil;
import com.iflytek.vie.utils.StringUtils;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class ModelDBPlatform {
   private HibernateTemplate hiberTemp;
   private JdbcTemplate jdbcTemplate;
   private ModelGroupDBPlatform modelGroupDB;
   private final Logger logger = LoggerFactory.getLogger(ModelDBPlatform.class);

   public Long saveModelAndGetModelID(ModelInfo modelInfo) throws Exception {
      return (Long)this.hiberTemp.save(modelInfo);
   }

   public ModelInfo queryModelById(Long modelId) {
      List<ModelInfo> modelInfoList = (List<ModelInfo>)(List<?>)this.hiberTemp.findByNamedQuery("QueryModelByIdPlatform", new Object[]{modelId});
      return modelInfoList != null && modelInfoList.size() > 0 ? modelInfoList.get(0) : null;
   }

   public void updateModel(ModelInfo modelInfo) throws Exception {
      this.hiberTemp.update(modelInfo);
   }

   public void deleteModelsByIds(final List<Long> modelIds) {
      this.hiberTemp.execute(new HibernateCallback<Object>() {
         public Object doInHibernate(Session session) throws HibernateException {
            Query query = session.createQuery("UPDATE ModelInfo SET modelStatus=2 WHERE modelId IN (:ids)").setParameterList("ids", modelIds);
            query.executeUpdate();
            return null;
         }
      });
   }

   public List<ModelInfo> queryAllModelByGroup(String dataSource, String modelName, List<Long> groupId) {
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String hql = "SELECT MI FROM ModelInfo MI WHERE MI.modelGroup.groupId IN (:groups)";
         if (!StringUtils.isNullOrEmpry(modelName)) {
            hql = hql + " AND MI.modelName LIKE '% " + modelName + " %' AND ESCAPE '\\'";
         }

         Query query = session.createQuery(hql).setParameterList("groups", groupId);
         return query.list();
      } catch (Exception var12) {
         this.logger.error("查询模型组下模型异常", var12);
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return null;
   }

   public HashMap<Integer, String> queryModelOnline(String modelName, List<Long> groupIds, String dataSource) throws Exception {
      HashMap<Integer, String> allModelList = new HashMap<>();
      Session session = null;

      try {
         StringBuffer sql = new StringBuffer();
         sql.append(
            "SELECT mi.model_id,mi.model_name,mi.group_id  FROM MODEL_INFO mi LEFT JOIN MODEL_TASK_T mt ON mi.model_id=mt.model_id LEFT JOIN TASK_INFO_T ti ON mt.task_id=ti.task_id WHERE ti.run_schema=0 AND mi.dataSource='"
               + dataSource
               + "' AND mi.model_status!=2 AND ti.task_status=1"
         );
         sql.append(" AND mi.group_id " + SqlHelperPlatform.createIn(groupIds, false));
         if (modelName != null && !"".equals(modelName)) {
            sql.append(" AND mi.model_name like '%" + modelName + "%'");
         }

         session = this.hiberTemp.getSessionFactory().openSession();
         List<Object[]> modelList = (List<Object[]>)(List<?>)session.createSQLQuery(sql.toString()).list();
         if (modelList != null && modelList.size() > 0) {
            for (Object[] modelInfo : modelList) {
               allModelList.put(Integer.parseInt(modelInfo[0].toString()), String.valueOf(modelInfo[1]));
            }
         }
      } catch (Exception var13) {
         this.logger.error("根据模型id查找模型名称", var13);
      } finally {
         session.close();
      }

      return allModelList;
   }

   public String queryModelByModelNameLike(String modelName) {
      Session session = null;
      String idsStr = "";

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = "SELECT mi.model_id   FROM MODEL_INFO mi   LEFT JOIN MODEL_TASK_T mt     ON mi.model_id = mt.model_id   LEFT JOIN TASK_INFO_T ti     ON mt.task_id = ti.task_id  WHERE ti.run_schema = 0    AND mi.model_status != 2    AND ti.task_status = 1 AND mi.model_name LIKE :modelName";
         Query query = session.createSQLQuery(sql).setParameter("modelName", "%" + modelName + "%");
         List<Object> data = query.list();
         if (data != null && data.size() > 0) {
            for (Object idstr : data) {
               idsStr = idsStr + String.valueOf(idstr) + ",";
            }

            idsStr = idsStr.substring(0, idsStr.length() - 1);
         }
      } catch (Exception var12) {
         this.logger.error("根据模型组查询模型", var12);
      } finally {
         session.close();
      }

      return idsStr;
   }

   public Object[] queryModelInfoBySingleId(final Long modelId) {
      String sql = "select tit.task_status, t.is_online    from MODEL_INFO t   join MODEL_TASK_T mtt     on t.model_id = mtt.model_id    and t.model_status != 2 and t.model_id=:modelId   join TASK_INFO_T tit     on mtt.task_id = tit.task_id    and tit.run_schema = 0";
      return (Object[])this.hiberTemp
         .execute(
            new HibernateCallback<Object[]>() {
               public Object[] doInHibernate(Session session) throws HibernateException {
                  List<Object[]> objects1 = (List<Object[]>)(List<?>)session.createSQLQuery(
                        "select tit.task_status, t.is_online    from MODEL_INFO t   join MODEL_TASK_T mtt     on t.model_id = mtt.model_id    and t.model_status != 2 and t.model_id=:modelId   join TASK_INFO_T tit     on mtt.task_id = tit.task_id    and tit.run_schema = 0"
                     )
                     .setLong("modelId", modelId)
                     .list();
                  return objects1 != null && objects1.size() > 0 ? objects1.get(0) : null;
               }
            }
         );
   }

   public Map<Long, String> queryAllModels() {
      Map<Long, String> models = new HashMap<>();
      String sql = "SELECT mi.model_id, mi.datasource   FROM MODEL_INFO mi   JOIN (SELECT mt.model_id, mt.task_id, ti.task_status           FROM MODEL_TASK_T mt           JOIN TASK_INFO_T ti             ON mt.task_id = ti.task_id          WHERE ti.run_schema = 0            and ti.task_status = 1) task     ON mi.model_id = task.model_id    and mi.model_status != 2 ";
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         List<Object[]> modelList = (List<Object[]>)(List<?>)session.createSQLQuery(sql).list();
         if (modelList != null && modelList.size() > 0) {
            for (Object[] modelInfo : modelList) {
               models.put(Long.parseLong(modelInfo[0].toString()), String.valueOf(modelInfo[1]));
            }
         }
      } catch (Exception var10) {
         this.logger.error("查询所有已上线模型失败", var10);
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return models;
   }

   public int queryAllModelCountByGroup(String dataSource, String modelName, List<Long> groupId) {
      Session session = null;
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append(
         "SELECT COUNT(distinct(mi.model_id)) as idCount FROM MODEL_INFO mi  LEFT JOIN (SELECT mt.model_id,ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti ON mt.task_id=ti.task_id WHERE ti.run_schema=0) task ON mi.model_id=task.model_id WHERE mi.dataSource='"
            + dataSource
            + "' AND mi.model_status!=2"
      );
      if (modelName != null && !"".equals(modelName.trim())) {
         sqlBuffer.append(" AND mi.model_name LIKE '%" + modelName + "%'");
         if (modelName.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (groupId != null && groupId.size() > 0) {
         sqlBuffer.append(" AND mi.group_id " + createIn(groupId, false));
      }

      sqlBuffer.append(" ORDER BY mi.is_up desc , mi.update_time DESC");

      int var9;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = sqlBuffer.toString();
         Query query = session.createSQLQuery(sql);
         List countList = query.list();
         if (countList == null || countList.size() <= 0) {
            return 0;
         }

         var9 = Integer.parseInt(String.valueOf(countList.get(0)));
      } catch (Exception var13) {
         this.logger.error("查询模型组下模型总数异常", var13);
         return 0;
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return var9;
   }

   public int queryAllModelCountByGroup(String dataSource, String modelName, String textRule, String modifierName, List<Long> groupId, List<Long> modelIds) {
      Session session = null;
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append(
         "SELECT COUNT(distinct(mi.model_id)) as idCount FROM MODEL_INFO mi  LEFT JOIN (SELECT mt.model_id,ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti ON mt.task_id=ti.task_id WHERE ti.run_schema=0) task ON mi.model_id=task.model_id WHERE mi.dataSource='"
            + dataSource
            + "' AND mi.model_status!=2"
      );
      if (modelName != null && !"".equals(modelName.trim())) {
         if (modelName.contains("_")) {
            modelName = modelName.replaceAll("_", "\\\\_");
         }

         sqlBuffer.append(" AND mi.model_name LIKE '%" + modelName + "%'");
         if (modelName.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (textRule != null && !"".equals(textRule.trim())) {
         if (textRule.contains("_")) {
            textRule = textRule.replaceAll("_", "\\\\_");
         }

         sqlBuffer.append(" AND mi.TEXT_RULE LIKE '%" + textRule + "%'");
         if (textRule.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (modifierName != null && !"".equals(modifierName.trim())) {
         if (modifierName.contains("_")) {
            modifierName = modifierName.replaceAll("_", "\\\\_");
         }

         sqlBuffer.append(" AND mi.MODIFIER_NAME LIKE '%" + modifierName + "%'");
         if (modifierName.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (groupId != null && groupId.size() > 0) {
         sqlBuffer.append(" AND mi.group_id " + this.createInSql(groupId, false, "mi.group_id"));
      }

      if (modelIds != null && modelIds.size() > 0) {
         sqlBuffer.append(" AND mi.model_id " + this.createInSql(modelIds, false, "mi.model_id"));
      }

      sqlBuffer.append(" ORDER BY mi.is_up desc , mi.update_time DESC");

      int var12;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = sqlBuffer.toString();
         Query query = session.createSQLQuery(sql);
         List countList = query.list();
         if (countList == null || countList.size() <= 0) {
            return 0;
         }

         var12 = Integer.parseInt(String.valueOf(countList.get(0)));
      } catch (Exception var16) {
         this.logger.error("查询模型组下模型总数异常", var16);
         return 0;
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return var12;
   }

   public String queryConditionByModelID(String modelId) {
      Session session = null;
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("SELECT mi.filter_rule").append(" FROM MODEL_INFO mi ").append(" WHERE  mi.model_id = " + modelId);

      String objStr;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = sqlBuffer.toString();
         if (!DBSelectUtil.isMysql()) {
            List<Clob> list = (List<Clob>)(List<?>)session.createSQLQuery(sql).list();
            objStr = "";

            for (Clob objects : list) {
               if (objects instanceof Clob) {
                  objStr = SqlHelperPlatform.clobToString(objects);
               } else {
                  objStr = objects + "";
               }
            }

            return objStr;
         }

         List<String> list = (List<String>)(List<?>)session.createSQLQuery(sql).list();
         objStr = list != null && !list.isEmpty() ? list.get(0) : "";
      } catch (Exception var12) {
         this.logger.error("获取模型筛选条件异常", var12);
         return null;
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return objStr;
   }

   public List<Object[]> queryAllModelByGroup(
      String dataSource,
      int pageNum,
      int pageSize,
      String modelName,
      String textRule,
      String modifierName,
      List<Long> groupId,
      List<Long> modelIds,
      String orderColumn,
      String orderType
   ) {
      Session session = null;
      int start = (pageNum - 1) * pageSize;
      StringBuffer sqlBuffer = new StringBuffer();
      if (DBSelectUtil.isMysql()) {
         sqlBuffer.append("SELECT mi.model_id,mi.model_name,mi.group_id,mi.update_time,")
            .append("mi.modifier_id,mi.modifier_name,mi.datasource,mi.filter_rule,")
            .append(
               "task.task_status,IFNULL(CAST(mi.model_hitcount AS SIGNED INTEGER), 0) as hitCount,mi.is_up,mi.total_count,mi.model_comment,mi.model_accuracy,mi.is_online,case IFNULL(mi.total_count, '0') "
            )
            .append("  when '0' then '0'")
            .append("  when '' then '0'")
            .append("  else CONCAT(((CAST(mi.model_hitcount AS SIGNED INTEGER) * 100)/CAST(mi.total_count AS SIGNED INTEGER))) end as rate,mi.create_time ")
            .append(" FROM MODEL_INFO mi ")
            .append(" LEFT JOIN (SELECT mt.model_id,ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti")
            .append(" ON mt.task_id=ti.task_id WHERE ti.run_schema=0) task ON mi.model_id=task.model_id")
            .append(" WHERE mi.dataSource='")
            .append(dataSource)
            .append("' AND mi.model_status!=2");
      } else {
         sqlBuffer.append("SELECT mi.model_id,mi.model_name,mi.group_id,mi.update_time,")
            .append("mi.modifier_id,mi.modifier_name,mi.datasource,mi.filter_rule,")
            .append(
               "task.task_status,nvl(to_number(mi.model_hitcount), 0) as hitCount,mi.is_up,mi.total_count,mi.model_comment,mi.model_accuracy,mi.is_online,case nvl(mi.total_count, '0') "
            )
            .append("  when '0' then '0'")
            .append("  when '' then '0'")
            .append("  else to_char((to_number(mi.model_hitcount) * 100)/to_number(mi.total_count) ) end as rate,mi.create_time ")
            .append(" FROM MODEL_INFO mi ")
            .append(" LEFT JOIN (SELECT mt.model_id,ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti")
            .append(" ON mt.task_id=ti.task_id WHERE ti.run_schema=0) task ON mi.model_id=task.model_id")
            .append(" WHERE mi.dataSource='")
            .append(dataSource)
            .append("' AND mi.model_status!=2");
      }

      if (modelName != null && !"".equals(modelName.trim())) {
         if (modelName.contains("_")) {
            modelName = modelName.replaceAll("_", "\\\\_");
         }

         sqlBuffer.append(" AND mi.model_name LIKE '%" + modelName + "%'");
         if (modelName.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (textRule != null && !"".equals(textRule.trim())) {
         if (textRule.contains("_")) {
            textRule = textRule.replaceAll("_", "\\\\_");
         }

         sqlBuffer.append(" AND mi.TEXT_RULE LIKE '%" + textRule + "%'");
         if (textRule.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (modifierName != null && !"".equals(modifierName.trim())) {
         if (modifierName.contains("_")) {
            modifierName = modifierName.replaceAll("_", "\\\\_");
         }

         sqlBuffer.append(" AND mi.MODIFIER_NAME LIKE '%" + modifierName + "%'");
         if (modifierName.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (groupId != null && groupId.size() > 0) {
         sqlBuffer.append(" AND mi.group_id " + this.createInSql(groupId, false, "mi.group_id"));
      }

      if (modelIds != null && modelIds.size() > 0) {
         sqlBuffer.append(" AND mi.model_id " + this.createInSql(modelIds, false, "mi.model_id"));
      }

      sqlBuffer.append(" ORDER BY ").append("mi.is_up desc ");
      if (StringUtils.isNotNullAndEmpry(orderColumn)) {
         sqlBuffer.append(",");
         switch (orderColumn) {
            case "modelStatus":
               sqlBuffer.append("task.task_status ").append(orderType).append(",").append("mi.is_online ").append(orderType);
               break;
            case "modifierName":
               sqlBuffer.append("mi.modifier_name ").append(orderType);
               break;
            case "startTime":
               if (DBSelectUtil.isMysql()) {
                  sqlBuffer.append("task.task_status asc,STR_TO_DATE(mi.extend1,'yyyy-mm-dd hh24:mi:ss') " + orderType);
               } else {
                  sqlBuffer.append("task.task_status asc,to_date(mi.extend1,'yyyy-mm-dd hh24:mi:ss') " + orderType);
               }
               break;
            case "createTime":
               if (DBSelectUtil.isMysql()) {
                  sqlBuffer.append("STR_TO_DATE(mi.create_time,'yyyy-mm-dd hh24:mi:ss') " + orderType);
               } else {
                  sqlBuffer.append("to_date(mi.create_time,'yyyy-mm-dd hh24:mi:ss') " + orderType);
               }
               break;
            case "modelCount":
               sqlBuffer.append("hitCount ").append(orderType);
               break;
            case "modelAccuracy":
               sqlBuffer.append("mi.model_accuracy ").append(orderType);
               break;
            case "checkRate":
               sqlBuffer.append("CAST(rate AS CHAR(100))").append(orderType);
               break;
            default:
               if (DBSelectUtil.isMysql()) {
                  sqlBuffer.append("STR_TO_DATE(mi.update_time,'yyyy-mm-dd hh24:mi:ss') " + orderType);
               } else {
                  sqlBuffer.append("to_date(mi.update_time,'yyyy-mm-dd hh24:mi:ss') " + orderType);
               }
         }
      }

      sqlBuffer.append(",mi.model_id desc");

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = sqlBuffer.toString();
         this.logger.info("(((The query model sql is))): " + sql);
         Query query = session.createSQLQuery(sql).setFirstResult(start).setMaxResults(pageSize);
         return query.list();
      } catch (Exception var20) {
         this.logger.error("查询模型组下模型异常", var20);
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return null;
   }

   public List<Object[]> queryExportByModelinfo(
      String dataSource,
      int pageNum,
      int pageSize,
      String modelName,
      String textRule,
      String modifierName,
      List<Long> groupId,
      List<Long> modelId,
      String orderColumn,
      String orderType
   ) {
      Session session = null;
      int start = (pageNum - 1) * pageSize;
      StringBuffer sqlBuffer = new StringBuffer();
      if (DBSelectUtil.isMysql()) {
         sqlBuffer.append("SELECT mi.model_id,mi.model_name,mi.group_id,mi.update_time,")
            .append("mi.modifier_id,mi.modifier_name,mi.datasource,mi.filter_rule,")
            .append(
               "task.task_status,IFNULL(CAST(mi.model_hitcount AS SIGNED INTEGER), 0) as hitCount,mi.is_up,mi.total_count,mi.model_comment,mi.model_accuracy,mi.is_online,case IFNULL(mi.total_count, '0') "
            )
            .append("  when '0' then '0'")
            .append("  when '' then '0'")
            .append("  else CONCAT((CAST(mi.model_hitcount AS SIGNED INTEGER) * 100)/CAST(mi.total_count AS SIGNED INTEGER) ) end as rate,mi.create_time")
            .append(" FROM MODEL_INFO mi ")
            .append(" LEFT JOIN (SELECT mt.model_id,ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti")
            .append(" ON mt.task_id=ti.task_id WHERE ti.run_schema=0) task ON mi.model_id=task.model_id")
            .append(" WHERE mi.dataSource='")
            .append(dataSource)
            .append("' AND mi.model_status!=2");
      } else {
         sqlBuffer.append("SELECT mi.model_id,mi.model_name,mi.group_id,mi.update_time,")
            .append("mi.modifier_id,mi.modifier_name,mi.datasource,mi.filter_rule,")
            .append(
               "task.task_status,nvl(to_number(mi.model_hitcount), 0) as hitCount,mi.is_up,mi.total_count,mi.model_comment,mi.model_accuracy,mi.is_online,case nvl(mi.total_count, '0') "
            )
            .append("  when '0' then '0'")
            .append("  when '' then '0'")
            .append("  else to_char((to_number(mi.model_hitcount) * 100)/to_number(mi.total_count) ) end as rate,mi.create_time")
            .append(" FROM MODEL_INFO mi ")
            .append(" LEFT JOIN (SELECT mt.model_id,ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti")
            .append(" ON mt.task_id=ti.task_id WHERE ti.run_schema=0) task ON mi.model_id=task.model_id")
            .append(" WHERE mi.dataSource='")
            .append(dataSource)
            .append("' AND mi.model_status!=2");
      }

      if (modelName != null && !"".equals(modelName.trim())) {
         if (modelName.contains("_")) {
            modelName = modelName.replaceAll("_", "\\\\_");
         }

         sqlBuffer.append(" AND mi.model_name LIKE '%" + modelName + "%'");
         if (modelName.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (textRule != null && !"".equals(textRule.trim())) {
         if (textRule.contains("_")) {
            textRule = textRule.replaceAll("_", "\\\\_");
         }

         sqlBuffer.append(" AND mi.TEXT_RULE LIKE '%" + textRule + "%'");
         if (textRule.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (modifierName != null && !"".equals(modifierName.trim())) {
         if (modifierName.contains("_")) {
            modifierName = modifierName.replaceAll("_", "\\\\_");
         }

         sqlBuffer.append(" AND mi.MODIFIER_NAME LIKE '%" + modifierName + "%'");
         if (modifierName.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (groupId != null && groupId.size() > 0) {
         sqlBuffer.append(" AND mi.group_id " + this.createInSql(groupId, false, "mi.group_id"));
      }

      if (modelId != null && modelId.size() > 0) {
         sqlBuffer.append(" AND mi.model_id " + this.createInSql(modelId, false, "mi.group_id"));
      }

      sqlBuffer.append(" ORDER BY ").append("mi.is_up desc ");
      if (StringUtils.isNotNullAndEmpry(orderColumn)) {
         sqlBuffer.append(",");
         switch (orderColumn) {
            case "modelStatus":
               sqlBuffer.append("task.task_status ").append(orderType).append(",").append("mi.is_online ").append(orderType);
               break;
            case "modifierName":
               sqlBuffer.append("mi.modifier_name ").append(orderType);
               break;
            case "startTime":
               if (DBSelectUtil.isMysql()) {
                  sqlBuffer.append("task.task_status asc,STR_TO_DATE(mi.extend1,'yyyy-mm-dd') " + orderType);
               } else {
                  sqlBuffer.append("task.task_status asc,to_date(mi.extend1,'yyyy-mm-dd') " + orderType);
               }
               break;
            case "createTime":
               if (DBSelectUtil.isMysql()) {
                  sqlBuffer.append("STR_TO_DATE(mi.create_time,'yyyy-mm-dd hh24:mi:ss') " + orderType);
               } else {
                  sqlBuffer.append("to_date(mi.create_time,'yyyy-mm-dd hh24:mi:ss') " + orderType);
               }
               break;
            case "modelCount":
               sqlBuffer.append("hitCount ").append(orderType);
               break;
            case "modelAccuracy":
               sqlBuffer.append("mi.model_accuracy ").append(orderType);
               break;
            case "checkRate":
               if (DBSelectUtil.isMysql()) {
                  sqlBuffer.append("cast(rate as char(100)) ").append(orderType);
               } else {
                  sqlBuffer.append("to_number(rate) ").append(orderType);
               }
               break;
            default:
               if (DBSelectUtil.isMysql()) {
                  sqlBuffer.append("STR_TO_DATE(mi.update_time,'yyyy-mm-dd hh24:mi:ss') " + orderType);
               } else {
                  sqlBuffer.append("to_date(mi.update_time,'yyyy-mm-dd hh24:mi:ss') " + orderType);
               }
         }
      }

      sqlBuffer.append(",mi.model_id desc");

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = sqlBuffer.toString();
         Query query = session.createSQLQuery(sql).setFirstResult(start).setMaxResults(pageSize);
         return query.list();
      } catch (Exception var20) {
         this.logger.error("查询模型组下模型异常", var20);
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return null;
   }

   public List<Object[]> queryAllModelByGroup(String dataSource, int pageNum, int pageSize, String modelName, List<Long> groupId) {
      Session session = null;
      int start = (pageNum - 1) * pageSize;
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append(
         "SELECT mi.model_id,mi.model_name,mi.group_id,mi.update_time,mi.modifier_id,mi.modifier_name,mi.datasource,mi.filter_rule,task.task_status,mi.model_hitcount,mi.is_up,mi.total_count,mi.model_comment,mi.model_accuracy,mi.is_online FROM MODEL_INFO mi  LEFT JOIN (SELECT mt.model_id,ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti ON mt.task_id=ti.task_id WHERE ti.run_schema=0) task ON mi.model_id=task.model_id WHERE mi.dataSource='"
            + dataSource
            + "' AND mi.model_status!=2"
      );
      if (modelName != null && !"".equals(modelName.trim())) {
         sqlBuffer.append(" AND mi.model_name LIKE '%" + modelName + "%'");
         if (modelName.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      if (groupId != null && groupId.size() > 0) {
         sqlBuffer.append(" AND mi.group_id " + createIn(groupId, false));
      }

      sqlBuffer.append(" ORDER BY mi.is_up desc , mi.update_time DESC");

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = sqlBuffer.toString();
         Query query = session.createSQLQuery(sql).setFirstResult(start).setMaxResults(pageSize);
         return query.list();
      } catch (Exception var15) {
         this.logger.error("查询模型组下模型异常", var15);
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return null;
   }

   public static String createIn(List list, boolean isString) {
      if (list != null && list.size() != 0) {
         return list.size() == 1 ? " =" + (isString ? "'" + list.get(0) + "'" : list.get(0)) : " in " + combineInCon(list, isString);
      } else {
         return "";
      }
   }

   public String createInSql(List list, boolean isString, String row) {
      if (list != null && list.size() != 0) {
         return list.size() == 1 ? " =" + (isString ? "'" + list.get(0) + "'" : list.get(0)) : this.splicingIdsIn(this.removeDuplicate(list), row).toString();
      } else {
         return "";
      }
   }

   public List<ModelInfo> queryAllModels(String dataSource, ModelStatus modelStatus, List<Long> groupIds) {
      if (groupIds != null && groupIds.size() != 0) {
         final String sql;
         switch (modelStatus) {
            case OffLine:
               sql = SqlHelperPlatform.queryAllOffLineModels(dataSource, groupIds);
               break;
            case OnLine:
               sql = SqlHelperPlatform.queryAllOnLineModels(dataSource, groupIds);
               break;
            case All:
               sql = SqlHelperPlatform.queryAllModelByGroup(dataSource, null, groupIds);
               break;
            default:
               sql = null;
         }

         final Map<Long, ModelGroup> modelGroupMap = this.fillGroupInfo(groupIds, dataSource);
         return (List<ModelInfo>)this.hiberTemp.execute(new HibernateCallback<List<ModelInfo>>() {
            public List<ModelInfo> doInHibernate(Session session) throws HibernateException {
               List<Object[]> list = (List<Object[]>)(List<?>)session.createSQLQuery(sql).list();
               List<ModelInfo> modelInfos = new ArrayList<>();
               if (list != null && list.size() != 0) {
                  for (Object[] objects : list) {
                     ModelInfo modelInfo = new ModelInfo();
                     modelInfo.setModelId(Long.parseLong(String.valueOf(objects[0])));
                     modelInfo.setModelName(String.valueOf(objects[1]));
                     modelInfo.setModelGroup(modelGroupMap.get(Long.parseLong(String.valueOf(objects[2]))));
                     modelInfos.add(modelInfo);
                  }

                  return modelInfos;
               } else {
                  return modelInfos;
               }
            }
         });
      } else {
         return new ArrayList<>();
      }
   }

   public Map<Long, ModelGroup> fillGroupInfo(List<Long> groupIds, String dataSource) {
      Map<Long, ModelGroup> modelGroupMap = new HashMap<>();
      if (groupIds != null && groupIds.size() != 0) {
         for (ModelGroup modelGroup : this.modelGroupDB.queryAllModelGroupsByPermission(groupIds, dataSource)) {
            modelGroupMap.put(modelGroup.getGroupId(), modelGroup);
         }

         return modelGroupMap;
      } else {
         return modelGroupMap;
      }
   }

   public static String combineInCon(List list, boolean isString) {
      if (list != null && list.size() != 0) {
         int i = 0;
         StringBuffer stringBuffer = new StringBuffer("(");

         for (Object object : list) {
            if (i++ != 0) {
               stringBuffer.append(",");
            }

            if (isString) {
               stringBuffer.append("'" + object + "'");
            } else {
               stringBuffer.append(object);
            }
         }

         stringBuffer.append(")");
         return stringBuffer.toString();
      } else {
         return "";
      }
   }

   public List<Object[]> getOnLineModelsByGroup(
      Long groupId, String dataSource, String modelName, List<Long> roleModelIds, ModelGroupDBPlatform modelGroupDB, List<Long> modelIds
   ) {
      Session session = null;

      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         List<Long> groupIds = new ArrayList<>();
         if (groupId != null && groupId != -1L) {
            groupIds.add(groupId);
         } else {
            groupIds.addAll(roleModelIds);
         }

         List<Long> modelGroupIds = modelGroupDB.queryAllModelGroupIdsByPermission(groupIds, dataSource);
         if (modelGroupIds != null && modelGroupIds.size() > 0) {
            modelGroupIds = this.removeDuplicate(modelGroupIds);
         }

         StringBuffer sb = this.splicingModelGroupIds(modelGroupIds, "mg.group_id");
         String sql = "select distinct(m.model_id) as model_id,m.model_name,mg.group_id,mg.group_name from model_info m inner join model_task_t mt on m.model_id=mt.model_id inner join task_info_t tt on mt.task_id=tt.task_id inner join model_group mg on m.group_id=mg.group_id where tt.task_status=1 and m.model_status=0 and mg.deleted=0 and m.datasource = '"
            + dataSource
            + "' and mg.group_id in ("
            + sb.toString()
            + ")";
         if (!StringUtils.isNullOrEmpry(modelName)) {
            if (modelName.contains("_")) {
               modelName = modelName.replace("_", "\\_");
               sql = sql + " and m.model_name like '%" + modelName + "%'";
               sql = sql + " ESCAPE '\\'";
            } else {
               sql = sql + " and m.model_name like '%" + modelName + "%'";
            }
         }

         if (modelIds != null && modelIds.size() > 0) {
            sql = sql + " and m.model_id " + createIn(modelIds, false);
         }

         Query query = session.createSQLQuery(sql);
         return query.list();
      } catch (Exception var18) {
         this.logger.error("查询模型组下上线异常模型", var18);
      } finally {
         session.close();
      }

      return null;
   }

   private List<Long> removeDuplicate(List<Long> modelGroupIds) {
      LinkedHashSet<Long> set = new LinkedHashSet<>(modelGroupIds.size());
      set.addAll(modelGroupIds);
      modelGroupIds.clear();
      modelGroupIds.addAll(set);
      return modelGroupIds;
   }

   private StringBuffer splicingModelGroupIds(List<Long> modelGroupIds, String row) {
      StringBuffer sb = new StringBuffer();
      int inNum = 1;

      for (int i = 0; i < modelGroupIds.size(); i++) {
         if (!StringUtils.isNullOrEmpry(modelGroupIds.get(i) + "")) {
            if (i == modelGroupIds.size() - 1) {
               sb.append(modelGroupIds.get(i) + "");
            } else if (inNum == 1000 && i > 0) {
               sb.append(modelGroupIds.get(i) + " ) or " + row + " in ( ");
               inNum = 1;
            } else {
               sb.append(modelGroupIds.get(i) + ",");
               inNum++;
            }
         }
      }

      return sb;
   }

   private StringBuffer splicingIdsIn(List<Long> modelGroupIds, String row) {
      StringBuffer sb = new StringBuffer();
      sb.append("in (");
      int inNum = 1;

      for (int i = 0; i < modelGroupIds.size(); i++) {
         if (!StringUtils.isNullOrEmpry(modelGroupIds.get(i) + "")) {
            if (i == modelGroupIds.size() - 1) {
               sb.append(modelGroupIds.get(i) + ")");
            } else if (inNum == 1000 && i > 0) {
               sb.append(modelGroupIds.get(i) + " ) or " + row + " in ( ");
               inNum = 1;
            } else {
               sb.append(modelGroupIds.get(i) + ",");
               inNum++;
            }
         }
      }

      return sb;
   }

   public void saveOrUpdateModel(ModelInfo modelInfo) {
      this.hiberTemp.saveOrUpdate(modelInfo);
   }

   public void addofflineinfo(Long id, String time) throws Exception {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("SELECT mi.model_status,mi.datasource,mi.user_id from MODEL_INFO mi where mi.model_id=" + id);
      Session session = null;
      session = this.hiberTemp.getSessionFactory().openSession();
      Query query = session.createSQLQuery(sqlBuffer.toString());

      try {
         Session session1 = this.hiberTemp.getSessionFactory().openSession();
         Transaction tx = session1.beginTransaction();
         List<Objects[]> list1 = query.list();
         ModelOfflineInfo t = new ModelOfflineInfo();
         if (list1 != null && list1.size() != 0) {
            Object[] taskinfo = list1.get(0);
            String str1 = taskinfo[0].toString();
            String str2 = taskinfo[1].toString();
            String str3 = taskinfo[2].toString();
            t.setModel_id(id);
            t.setModel_status(Integer.parseInt(str1));
            t.setDataSource(str2);
            t.setUser_id(Long.parseLong(str3));
            t.setCreate_time(time);
            session1.save(t);
            tx.commit();
            session1.close();
         }
      } catch (Exception var17) {
         this.logger.error("添加下线模型信息异常", var17);
      } finally {
         session.close();
      }
   }

   public void modelOffline(final Long id) {
      this.hiberTemp.execute(new HibernateCallback<Object>() {
         public Object doInHibernate(Session session) throws HibernateException {
            String selectSql = "select ti.task_id,ti.task_name from model_task_t mt inner join task_info_t ti on mt.task_id=ti.task_id where mt.model_id=" + id;
            List<Object[]> lists = (List<Object[]>)(List<?>)session.createSQLQuery(selectSql).list();
            String updateFilterSql = "update ModelInfo set filterRule=null,modelHitCount=null,totalCount=null,isOnline=1,filterId=0 where modelId=" + id;
            session.createQuery(updateFilterSql).executeUpdate();
            if (lists != null && lists.size() > 0) {
               for (Object[] objects : lists) {
                  Long taskId = Long.valueOf(objects[0].toString());
                  if (taskId != null) {
                     if (DBSelectUtil.isMysql()) {
                        String deleteSql = "delete TaskInfo where taskId =" + taskId;
                        session.createQuery(deleteSql).executeUpdate();
                     } else {
                        String deleteSql = "delete task_info_t where task_id = :task_id";
                        session.createSQLQuery(deleteSql).setLong("task_id", taskId).executeUpdate();
                     }
                  }

                  if (id != null && taskId != null) {
                     if (DBSelectUtil.isMysql()) {
                        String delSql = "delete ModelTask  where modelId=" + id + " and taskId=" + taskId;
                        session.createQuery(delSql).executeUpdate();
                     } else {
                        String delSql = "delete model_task_t  where model_id=" + id + " and task_id=" + taskId;
                        session.createSQLQuery(delSql).executeUpdate();
                     }
                  }
               }

               return null;
            } else {
               return null;
            }
         }
      });
   }

   public long saveModel(final ModelInfo modelInfo) {
      return (Long)this.hiberTemp.execute(new HibernateCallback<Long>() {
         public Long doInHibernate(Session session) throws HibernateException {
            return (Long)session.save(modelInfo);
         }
      });
   }

   public Object[] queryModelInfoById(long modelId) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append(
         "SELECT mi.model_id,mi.model_name,mi.group_id,mi.update_time,mi.modifier_id,mi.modifier_name,mi.datasource,mi.filter_rule,task.task_status,mi.model_hitcount,mi.is_up,case when mi.total_count is null then '0' else mi.total_count end total_count,mi.model_comment, mi.model_accuracy,mi.is_online,mi.extend2  FROM MODEL_INFO mi  LEFT JOIN (SELECT mt.model_id,ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti ON mt.task_id=ti.task_id WHERE ti.run_schema=0) task ON mi.model_id=task.model_id WHERE mi.model_id="
            + modelId
            + " AND mi.model_status!=2  "
      );
      Session session = null;

      Object[] var7;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         Query query = session.createSQLQuery(sqlBuffer.toString());
         List<Object[]> list = query.list();
         if (list == null || list.size() == 0) {
            return null;
         }

         var7 = list.get(0);
      } catch (Exception var11) {
         this.logger.error("查询模型信息异常", var11);
         return null;
      } finally {
         session.close();
      }

      return var7;
   }

   public List<HashMap<String, Object>> getModelByModelGroupId(String dataSource, Long groupId, List<String> onlineModelId, Logger log) {
      Session session = null;

      Object var24;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = "";
         if ("".equals(groupId) || "null".equals(groupId)) {
            return null;
         }

         boolean isOracle = true;
         if (DBSelectUtil.isSqlServer()) {
            sql = " SELECT mi.model_id, mi.model_name, cast(round(mi.model_accuracy, 0) as VARCHAR) + '%' as model_Accuracy,mi.filter_rule                FROM MODEL_INFO mi                LEFT JOIN MODEL_TASK_T mt                  ON mi.model_id = mt.model_id                LEFT JOIN TASK_INFO_T ti                  ON mt.task_id = ti.task_id               WHERE ti.run_schema = 0                 AND mi.model_status != 2                 AND ti.task_status = 1                 AND mi.group_id in ("
               + groupId
               + ") "
               + "                AND mi.dataSource='"
               + dataSource
               + "'"
               + " order by model_id desc";
            sql = "WITH TEMPTAB (GROUP_ID, PARENT_ID) AS (SELECT GROUP_ID, PARENT_ID FROM MODEL_GROUP WHERE GROUP_ID ="
               + groupId
               + " UNION ALL SELECT CHILD.GROUP_ID, CHILD.PARENT_ID FROM MODEL_GROUP AS CHILD,TEMPTAB AS SUPER WHERE SUPER.GROUP_ID = CHILD.PARENT_ID) SELECT mi.model_id,mi.model_name,CAST (round(mi.model_accuracy, 0) AS VARCHAR) + '%' AS model_Accuracy,mi.filter_rule "
               + "FROM MODEL_INFO mi LEFT JOIN MODEL_TASK_T mt ON mi.model_id = mt.model_id LEFT JOIN TASK_INFO_T ti ON mt.task_id = ti.task_id "
               + "WHERE ti.run_schema = 0 AND mi.model_status != 2 AND ti.task_status = 1 AND mi.group_id IN (SELECT tp.GROUP_ID FROM TEMPTAB tp) AND mi.dataSource = '"
               + dataSource
               + "' ORDER BY model_id DESC";
            isOracle = false;
         } else if (DBSelectUtil.isMysql()) {
            String rec = " select t.group_id as groupId     from model_group t where FIND_IN_SET(t.group_id, getChildList(" + groupId + "))";
            sql = "SELECT mi.model_id,        mi.model_name,        cast(round(mi.model_accuracy) AS char(100)) + '%' as model_Accuracy,        mi.filter_rule   FROM MODEL_INFO mi   LEFT JOIN MODEL_TASK_T mt     ON mi.model_id = mt.model_id   LEFT JOIN TASK_INFO_T ti     ON mt.task_id = ti.task_id  WHERE ti.run_schema = 0    AND mi.model_status = 0    AND ti.task_status = 1    AND mi.group_id in (select groupTemp.groupId from ("
               + rec
               + ") groupTemp) "
               + "   AND mi.dataSource ='"
               + dataSource
               + "'"
               + " order by model_id desc";
            isOracle = false;
         } else {
            sql = " SELECT mi.model_id, mi.model_name,to_char(round( mi.model_accuracy)) || '%' as model_Accuracy,mi.filter_rule                FROM MODEL_INFO mi                LEFT JOIN MODEL_TASK_T mt                  ON mi.model_id = mt.model_id                LEFT JOIN TASK_INFO_T ti                  ON mt.task_id = ti.task_id               WHERE ti.run_schema = 0                 AND mi.model_status != 2                 AND ti.task_status = 1                 AND mi.group_id in ("
               + groupId
               + ")  "
               + "                AND mi.dataSource='"
               + dataSource
               + "'"
               + " order by model_id desc";
            sql = "with groupTemp as  (select t.group_id as groupId     from model_group t    start with t.group_id ="
               + groupId
               + "  connect by prior t.group_id = t.parent_id) "
               + "SELECT mi.model_id, "
               + "       mi.model_name, "
               + "       to_char(round(mi.model_accuracy)) || '%' as model_Accuracy, "
               + "       mi.filter_rule "
               + "  FROM MODEL_INFO mi "
               + "  LEFT JOIN MODEL_TASK_T mt "
               + "    ON mi.model_id = mt.model_id "
               + "  LEFT JOIN TASK_INFO_T ti "
               + "    ON mt.task_id = ti.task_id "
               + " WHERE ti.run_schema = 0 "
               + "   AND mi.model_status = 0 "
               + "   AND ti.task_status = 1 "
               + "   AND mi.group_id in (select gt.groupId from groupTemp gt) "
               + "   AND mi.dataSource ='"
               + dataSource
               + "'"
               + " order by model_id desc";
         }

         log.info("search model info sql :" + sql);
         List<Object[]> modelList = (List<Object[]>)(List<?>)session.createSQLQuery(sql).list();
         List<HashMap<String, Object>> modelMapList = new ArrayList<>();

         for (Object[] obj : modelList) {
            HashMap<String, Object> modelMap = new HashMap<>();
            modelMap.put("modelId", obj[0]);
            onlineModelId.add(obj[0] + "");
            modelMap.put("modelName", obj[1]);
            modelMap.put("percentage", obj[2]);
            String objStr = "";
            if (isOracle) {
               objStr = SqlHelperPlatform.clobToString((Clob)obj[3]);
            } else {
               objStr = obj[3] + "";
            }

            modelMap.put("filterRule", objStr);
            modelMap.put("modelPer", "");
            modelMapList.add(modelMap);
         }

         if (modelList.size() != 0) {
            return modelMapList;
         }

         var24 = null;
      } catch (Exception var17) {
         throw var17;
      } finally {
         session.close();
      }

      return (List<HashMap<String, Object>>)var24;
   }

   public boolean searchModelConnection(Long modelId) throws Exception {
      Session session = null;

      boolean var6;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = "select t.modelcatagoryid,t.modelid from model_category_releassion t where t.modelid=" + modelId;
         Query query = session.createSQLQuery(sql);
         List<Object[]> result = query.list();
         if (ListUtil.isNullOrEmpty(result)) {
            return false;
         }

         var6 = true;
      } catch (Exception var10) {
         throw var10;
      } finally {
         session.close();
      }

      return var6;
   }

   public boolean deleteModelConnection(Long modelId) throws Exception {
      Session session = null;

      boolean var4;
      try {
         session = this.hiberTemp.getSessionFactory().openSession();
         String sql = "delete from model_category_releassion  where modelid=" + modelId;
         session.createSQLQuery(sql).executeUpdate();
         var4 = true;
      } catch (Exception var8) {
         throw var8;
      } finally {
         session.close();
      }

      return var4;
   }

   public int isHaveSameModelByModelId(String modelName, String dataSource, long modelId) throws Exception {
      try {
         final String sql = "select count(t.model_id) as idCount, '123' as tt   from model_info t  where t.model_name = '"
            + modelName
            + "' "
            + "   and t.datasource = '"
            + dataSource
            + "' "
            + "   and t.model_status != 2 "
            + "   and t.model_id !="
            + modelId;
         List<Object[]> list = (List<Object[]>)this.hiberTemp.execute(new HibernateCallback<List<Object[]>>() {
            public List<Object[]> doInHibernate(Session session) throws HibernateException {
               return (List)session.createSQLQuery(sql).list();
            }
         });
         if (list != null && list.size() >= 1) {
            Object[] objects = list.get(0);
            if (DBSelectUtil.isSqlServer()) {
               Integer bg = (Integer)objects[0];
               return bg;
            } else if (DBSelectUtil.isMysql()) {
               BigInteger bg = (BigInteger)objects[0];
               return bg.intValue();
            } else {
               BigDecimal bg = (BigDecimal)objects[0];
               return bg.intValue();
            }
         } else {
            return 0;
         }
      } catch (Exception var10) {
         this.logger.error("模型是否重名异常", var10);
         throw var10;
      }
   }

   public void deleteModel(ModelInfo modelInfo) {
      this.hiberTemp.delete(modelInfo);
   }

   public int isHaveSameModel(String modelName, String dataSource, long modelId) throws Exception {
      String hql = "SELECT T FROM ModelInfo T WHERE T.modelStatus <> 2 AND T.modelName=? AND T.dataSource=? AND T.modelId <> ?";
      List<ModelInfo> modelInfos = (List<ModelInfo>)(List<?>)this.hiberTemp.find(hql, new Object[]{modelName, dataSource, modelId});
      int count = 0;
      if (!ListUtil.isNullOrEmpty(modelInfos)) {
         count = modelInfos.size();
      }

      return count;
   }

   public ModelGroupDBPlatform getModelGroupDB() {
      return this.modelGroupDB;
   }

   public void setModelGroupDB(ModelGroupDBPlatform modelGroupDB) {
      this.modelGroupDB = modelGroupDB;
   }

   public HibernateTemplate getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplate hiberTemp) {
      this.hiberTemp = hiberTemp;
   }

   public JdbcTemplate getJdbcTemplate() {
      return this.jdbcTemplate;
   }

   public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
   }
}
