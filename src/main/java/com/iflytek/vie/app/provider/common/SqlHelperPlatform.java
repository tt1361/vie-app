package com.iflytek.vie.app.provider.common;

import java.io.Reader;
import java.io.Serializable;
import java.sql.Clob;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlHelperPlatform implements Serializable {
   private static final Logger logger = LoggerFactory.getLogger(SqlHelperPlatform.class);
   public static final Long TASK_STATUS_WAIT_START = 0L;
   public static final Long TASK_STATUS_COMPLETED = 1L;
   public static final Long TASK_STATUS_STOP = 2L;
   public static final Long TASK_STATUS_MANUALTERMINATION = 3L;
   public static final Long TASK_STATUS_DELETE = 4L;
   public static final Long TASK_STATUS_RUNNING = 5L;
   public static final Long TASK_STATUS_NO_RUNNING = 6L;
   private static final long serialVersionUID = -6068183668281927007L;

   public static String clobToString(Clob obj) {
      String s1 = "";
      char[] ac = new char[200];
      if (obj != null) {
         try {
            Reader reader = obj.getCharacterStream();

            int i;
            while ((i = reader.read(ac, 0, 200)) != -1) {
               s1 = s1 + new String(ac, 0, i);
            }

            reader.close();
         } catch (Exception var5) {
            logger.error("method clobToString exception");
         }
      }

      return s1;
   }

   public static String createIn(List list, boolean isString) {
      if (list != null && list.size() != 0) {
         return list.size() == 1 ? " =" + (isString ? "'" + list.get(0) + "'" : list.get(0)) : " IN " + combineInCon(list, isString);
      } else {
         return "";
      }
   }

   public static String queryAllOnLineModels(String dataSource, List<Long> groupIds) {
      StringBuffer stringBuffer = new StringBuffer();
      stringBuffer.append(
         "SELECT mi.model_id,mi.model_name,mi.group_id  FROM MODEL_INFO mi LEFT JOIN MODEL_TASK_T mt ON mi.model_id=mt.model_id LEFT JOIN TASK_INFO_T ti ON mt.task_id=ti.task_id WHERE ti.run_schema=0 AND mi.dataSource='"
            + dataSource
            + "' AND mi.model_status!=2 AND ti.task_status=1"
      );
      stringBuffer.append(" AND mi.group_id " + createIn(groupIds, false));
      return stringBuffer.toString();
   }

   public static String queryAllOffLineModels(String dataSource, List<Long> groupIds) {
      StringBuffer stringBuffer = new StringBuffer();
      String inGroupIds = createIn(groupIds, false);
      stringBuffer.append(
         "SELECT mi.model_id,mi.model_name,mi.group_id FROM MODEL_INFO mi WHERE mi.group_id "
            + inGroupIds
            + " mi.model_id NOT IN "
            + "(SELECT mi.model_id FROM MODEL_INFO mi"
            + " LEFT JOIN MODEL_TASK_T mt ON mi.model_id=mt.model_id"
            + " WHERE mi.dataSource='"
            + dataSource
            + "' AND mi.model_status!=2 AND mi.group_id "
            + inGroupIds
            + ")"
      );
      return stringBuffer.toString();
   }

   public static String queryAllModelByGroup(String dataSource, String modelName, List<Long> groupId) {
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

      if (null != groupId && groupId.size() > 0) {
         sqlBuffer.append(" AND mi.group_id " + createIn(groupId, false));
      }

      sqlBuffer.append(" ORDER BY mi.is_up desc , mi.update_time DESC");
      System.out.println("sql is:" + sqlBuffer);
      return sqlBuffer.toString();
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
}
