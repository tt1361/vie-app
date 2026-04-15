package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.provider.common.SqlHelper;
import com.iflytek.vie.dynamic.DynamicDataSourceContextHolder;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.StringUtils;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class ModelInfoDBPlatform {
   private final Logger logger = LoggerFactory.getLogger(ModelInfoDBPlatform.class);
   private HibernateTemplate hiberTemp;
   private JdbcTemplate jdbcTemplate;

   public long queryModelId(String dataSource, String modelName) {
      long modelId = -99L;
      DynamicDataSourceContextHolder.setDataSourceType(dataSource);
      String sql = "select t.modelId from ModelInfo t where t.dataSource = '" + dataSource + "' and t.modelName = '" + modelName + "'";
      List<Long> resultList = (List<Long>)(List<?>)this.hiberTemp.find(sql, new Object[0]);
      if (resultList.size() > 0) {
         modelId = resultList.get(0);
      }

      return modelId;
   }

   public HashMap<Integer, String> queryModelOnline(String dataSource, int isLoad) {
      HashMap<Integer, String> allModelList = new HashMap<>();
      Session session = null;

      try {
         StringBuffer sql = new StringBuffer();
         sql.append(
            "SELECT mi.model_id,mi.model_name FROM MODEL_INFO mi LEFT JOIN MODEL_TASK_T mt ON mi.model_id=mt.model_id LEFT JOIN TASK_INFO_T ti ON mt.task_id=ti.task_id WHERE ti.run_schema=0 AND mi.dataSource='"
               + dataSource
               + "' AND mi.model_status!=2 AND ti.task_status=1"
         );
         session = this.hiberTemp.getSessionFactory().openSession();
         List<Object[]> modelList = (List<Object[]>)(List<?>)session.createSQLQuery(sql.toString()).list();
         if (modelList != null && modelList.size() > 0) {
            for (Object[] modelInfo : modelList) {
               allModelList.put(Integer.parseInt(modelInfo[0].toString()), String.valueOf(modelInfo[1]));
            }
         }
      } catch (Exception var12) {
         this.logger.error("method queryModelOnline exception");
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return allModelList;
   }

   public Map<Long, Map<String, Object>> getModelByGroupId(String dataSource, List<Long> groupIds, int isLoad) {
      try {
         if (groupIds != null && groupIds.size() > 0) {
            groupIds = this.removeDuplicate(groupIds);
         }

         StringBuffer groupId = this.splicingModelGroupIds(groupIds, "mg.group_id");
         String sql = "";
         if (isLoad == 0) {
            sql = "SELECT  m.model_id,m.model_name,m.model_type,mg.group_name FROM model_info m  INNER JOIN model_group mg ON m.group_id=mg.group_id INNER JOIN MODEL_TASK_T MT ON M.MODEL_ID=MT.MODEL_ID INNER JOIN TASK_INFO_T TT ON TT.TASK_ID=MT.TASK_ID WHERE m.datasource= '"
               + dataSource
               + "' AND m.model_status!=2 AND TT.TASK_STATUS=1";
         } else {
            if (groupIds == null || groupIds.isEmpty()) {
               return null;
            }

            if (DBSelectUtil.isMysql()) {
               StringBuffer sb = new StringBuffer();

               for (int i = 0; i < groupIds.size(); i++) {
                  String rec = "SELECT group_id from model_group where find_in_set(group_id,getChildList(" + groupIds.get(i) + "))";
                  List<Map<String, Object>> one = this.jdbcTemplate.queryForList(rec);
                  if (one != null && one.size() > 0) {
                     for (Map map : one) {
                        sb.append(map.get("group_id") + ",");
                     }
                  }
               }

               String groups = sb.substring(0, sb.length() - 1);
               sql = "SELECT m.model_id, m.model_name, m.model_type, mg.group_name   FROM model_info m  INNER JOIN model_group mg     ON m.group_id = mg.group_id  INNER JOIN MODEL_TASK_T MT     ON M.MODEL_ID = MT.MODEL_ID  INNER JOIN TASK_INFO_T TT     ON TT.TASK_ID = MT.TASK_ID  WHERE m.datasource = '"
                  + dataSource
                  + "' "
                  + "   AND m.model_status = 0 "
                  + "   AND (mg.group_id IN ("
                  + groups
                  + ") OR "
                  + "       mg.application_id = -1) "
                  + "   AND TT.TASK_STATUS = 1 "
                  + "UNION ALL "
                  + "SELECT m.model_id, m.model_name, m.model_type, mg.group_name "
                  + "  FROM model_info m "
                  + " INNER JOIN model_group mg "
                  + "    ON m.group_id = mg.group_id "
                  + " INNER JOIN MODEL_TASK_T MT "
                  + "    ON M.MODEL_ID = MT.MODEL_ID "
                  + " INNER JOIN TASK_INFO_T TT "
                  + "    ON TT.TASK_ID = MT.TASK_ID "
                  + " WHERE m.datasource = '"
                  + dataSource
                  + "' AND MG.APPLICATION_ID = -1 AND "
                  + " M.MODEL_STATUS = 0 AND TT.TASK_STATUS = 1";
            } else {
               sql = "with groupTemp as (SELECT mg.group_id groupId     from model_group mg    start with mg.group_id IN ("
                  + groupId.toString()
                  + ")"
                  + "  connect by prior mg.group_id = mg.parent_id "
                  + "   order by mg.group_id) "
                  + "SELECT m.model_id, m.model_name, m.model_type, mg.group_name "
                  + "  FROM model_info m "
                  + " INNER JOIN model_group mg "
                  + "    ON m.group_id = mg.group_id "
                  + " INNER JOIN MODEL_TASK_T MT "
                  + "    ON M.MODEL_ID = MT.MODEL_ID "
                  + " INNER JOIN TASK_INFO_T TT "
                  + "    ON TT.TASK_ID = MT.TASK_ID "
                  + " WHERE m.datasource = '"
                  + dataSource
                  + "' "
                  + "   AND m.model_status = 0 "
                  + "   AND (mg.group_id IN (select gt.groupId from groupTemp gt) OR "
                  + "       mg.application_id = -1) "
                  + "   AND TT.TASK_STATUS = 1 "
                  + "UNION ALL "
                  + "SELECT m.model_id, m.model_name, m.model_type, mg.group_name "
                  + "  FROM model_info m "
                  + " INNER JOIN model_group mg "
                  + "    ON m.group_id = mg.group_id "
                  + " INNER JOIN MODEL_TASK_T MT "
                  + "    ON M.MODEL_ID = MT.MODEL_ID "
                  + " INNER JOIN TASK_INFO_T TT "
                  + "    ON TT.TASK_ID = MT.TASK_ID "
                  + " WHERE m.datasource = '"
                  + dataSource
                  + "' AND MG.APPLICATION_ID = -1 AND "
                  + " M.MODEL_STATUS = 0 AND TT.TASK_STATUS = 1";
            }
         }

         System.out.println(sql);
         Map<Long, Map<String, Object>> idMap = new HashMap<>();

         for (Object[] objects : this.jdbcTemplate.query(sql, new RowMapper<Object[]>() {
            public Object[] mapRow(ResultSet resultSet, int ix) throws SQLException {
               return new Object[]{resultSet.getObject(1), resultSet.getObject(2), resultSet.getObject(3), resultSet.getObject(4)};
            }
         })) {
            Long modelId = Long.parseLong(String.valueOf(objects[0]));
            if (!idMap.containsKey(modelId)) {
               Map<String, Object> modelMap = new HashMap<>();
               modelMap.put("modelId", objects[0]);
               modelMap.put("modelName", objects[1]);
               modelMap.put("modelType", objects[2]);
               modelMap.put("modelGroupName", objects[3]);
               idMap.put(modelId, modelMap);
            }
         }

         return idMap;
      } catch (Exception var12) {
         this.logger.error("method getModelByGroupId exception");
         return null;
      }
   }

   public List<Map<String, Object>> queryEditModelByGroupId(int isLoad, String dataSource, List<Long> groupIds) {
      String sql = "";
      if (groupIds != null && groupIds.size() > 0) {
         groupIds = this.removeDuplicate(groupIds);
      }

      StringBuffer groupId = this.splicingModelGroupIds(groupIds, "mg.group_id");

      try {
         if (isLoad == 0) {
            sql = "SELECT mi.model_id, mi.model_name, mi.text_rule  FROM MODEL_INFO mi  LEFT JOIN (SELECT mt.model_id, ti.task_status               FROM MODEL_TASK_T mt               LEFT JOIN TASK_INFO_T ti                 ON mt.task_id = ti.task_id              WHERE ti.run_schema = 0) task    ON mi.model_id = task.model_id WHERE mi.dataSource = '"
               + dataSource
               + "' "
               + "   AND mi.model_status != 2"
               + " AND (task.task_status IS NULL OR task.task_status BETWEEN 2 AND 4)"
               + " AND mi.text_rule IS NOT NULL";
         } else {
            if (groupIds == null || groupIds.isEmpty()) {
               return null;
            }

            if (DBSelectUtil.isMysql()) {
               StringBuffer sb = new StringBuffer();

               for (int i = 0; i < groupIds.size(); i++) {
                  String rec = "SELECT group_id from model_group where find_in_set(group_id,getChildList(" + groupIds.get(i) + ")) ORDER BY group_id";
                  List<Map<String, Object>> one = this.jdbcTemplate.queryForList(rec);
                  if (one != null && one.size() > 0) {
                     for (Map map : one) {
                        sb.append(map.get("group_id") + ",");
                     }
                  }
               }

               String groups = sb.substring(0, sb.length() - 1);
               sql = "SELECT mi.model_id, mi.model_name, mi.text_rule  FROM MODEL_INFO mi  LEFT JOIN (SELECT mt.model_id, ti.task_status               FROM MODEL_TASK_T mt               LEFT JOIN TASK_INFO_T ti                 ON mt.task_id = ti.task_id              WHERE ti.run_schema = 0) task    ON mi.model_id = task.model_id WHERE mi.dataSource = '"
                  + dataSource
                  + "' "
                  + "   AND mi.model_status != 2"
                  + "   AND mi.group_id IN ("
                  + groups
                  + ")"
                  + " AND (task.task_status IS NULL OR task.task_status BETWEEN 2 AND 4)"
                  + " AND mi.text_rule IS NOT NULL";
            } else {
               sql = "WITH groupTemp AS (SELECT mg.group_id groupId    FROM model_group mg   START WITH mg.group_id IN ("
                  + groupId.toString()
                  + ")"
                  + "  CONNECT BY PRIOR mg.group_id = mg.parent_id"
                  + "   ORDER BY mg.group_id)"
                  + "SELECT mi.model_id, mi.model_name, mi.text_rule"
                  + "  FROM MODEL_INFO mi"
                  + "  LEFT JOIN (SELECT mt.model_id, ti.task_status"
                  + "               FROM MODEL_TASK_T mt"
                  + "               LEFT JOIN TASK_INFO_T ti"
                  + "                 ON mt.task_id = ti.task_id"
                  + "              WHERE ti.run_schema = 0) task"
                  + "    ON mi.model_id = task.model_id"
                  + " WHERE mi.dataSource = '"
                  + dataSource
                  + "' "
                  + "   AND mi.model_status != 2"
                  + "   AND mi.group_id IN (SELECT groupId FROM groupTemp)"
                  + " AND (task.task_status IS NULL OR task.task_status BETWEEN 2 AND 4)"
                  + " AND mi.text_rule IS NOT NULL";
            }
         }

         final String finalSql = sql;
         List<Map<String, Object>> idMap = new ArrayList<>();

         for (Object[] objects : (List<Object[]>)(List<?>)this.hiberTemp.execute(new HibernateCallback<List<Object[]>>() {
            public List<Object[]> doInHibernate(Session session) throws HibernateException {
               return (List<Object[]>)(List<?>)session.createSQLQuery(finalSql).list();
            }
         })) {
            Long modelId = Long.parseLong(String.valueOf(objects[0]));
            Map<String, Object> modelMap = new HashMap<>();
            modelMap.put("modelId", objects[0]);
            modelMap.put("modelName", objects[1]);
            String objStr = "";
            if (objects[2] instanceof Clob) {
               objStr = SqlHelper.clobToString((Clob)objects[2]);
            } else {
               objStr = objects[2] + "";
            }

            modelMap.put("textRule", objStr);
            idMap.add(modelMap);
         }

         return idMap;
      } catch (Exception var14) {
         this.logger.error("method queryEditModelByGroupId exception");
         return null;
      }
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
