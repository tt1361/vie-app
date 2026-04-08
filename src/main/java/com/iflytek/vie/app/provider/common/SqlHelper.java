package com.iflytek.vie.app.provider.common;

import com.alibaba.fastjson.JSON;
import com.iflytek.vie.app.provider.pojo.ModelInfoDto;
import com.iflytek.vie.utils.DBSelectUtil;
import java.io.Reader;
import java.io.Serializable;
import java.sql.Clob;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlHelper implements Serializable {
   private static final Logger logger = LoggerFactory.getLogger(SqlHelper.class);
   private static final long serialVersionUID = 8313654814694681757L;
   public static final Long TASK_STATUS_WAIT_START = 0L;
   public static final Long TASK_STATUS_COMPLETED = 1L;
   public static final Long TASK_STATUS_STOP = 2L;
   public static final Long TASK_STATUS_MANUALTERMINATION = 3L;
   public static final Long TASK_STATUS_DELETE = 4L;
   public static final Long TASK_STATUS_RUNNING = 5L;
   public static final Long TASK_STATUS_NO_RUNNING = 6L;

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
            logger.error("[clobToString]出现异常", var5);
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

   public static String queryAllTopicsCount(String dataSource, String topicName) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append(
         "select count(t.topic_id) as idCount   from TOPIC_INFO t   left join model_group mg on mg.group_id = t.group_id  where t.data_source = :dataSource  and t.topic_status = 0 "
      );
      if (topicName != null && !"".equals(topicName.trim())) {
         if (DBSelectUtil.isMysql()) {
            topicName = topicName.replace("_", "/_");
         } else {
            topicName = topicName.replace("_", "\\_");
         }

         sqlBuffer.append(" AND t.topic_name LIKE :topicName");
         if (topicName.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         } else if (topicName.contains("/")) {
            sqlBuffer.append(" ESCAPE '/'");
         }
      }

      return sqlBuffer.toString();
   }

   public static String queryAllTopics(String dataSource, String topicName) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("select t.topic_id,t.topic_name,mg.group_name,t.data_source,")
         .append("t.update_user_name,t.update_time,t.group_id,t.topic_condition  ")
         .append("  from TOPIC_INFO t  left join model_group mg on mg.group_id = t.group_id")
         .append("  where t.data_source = :dataSource and t.topic_status = 0");
      if (topicName != null && !"".equals(topicName.trim())) {
         topicName = topicName.replace("_", "\\_");
         sqlBuffer.append(" AND t.topic_name LIKE :topicName");
         if (topicName.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      sqlBuffer.append(" ORDER BY t.create_time DESC");
      return sqlBuffer.toString();
   }

   public static String queryAllTopicsCountByName(String dataSource, String topicName) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("select count(t.topic_id) as idCount ")
         .append("  from TOPIC_INFO t   left join model_group mg on mg.group_id = t.group_id")
         .append("  where t.data_source = :dataSource and t.topic_status = 0");
      if (topicName != null && !"".equals(topicName.trim())) {
         sqlBuffer.append(" AND t.topic_name = :topicName");
      }

      return sqlBuffer.toString();
   }

   public static String queryAllOnLineModels(String dataSource, List<Long> groupIds) {
      StringBuffer stringBuffer = new StringBuffer();
      if (groupIds != null && groupIds.size() > 0) {
         groupIds = removeDuplicate(groupIds);
      }

      StringBuffer sb = splicingModelGroupIds(groupIds, "mi.group_id");
      stringBuffer.append(
         "SELECT mi.model_id,mi.model_name,mi.group_id FROM MODEL_INFO mi LEFT JOIN MODEL_TASK_T mt ON mi.model_id=mt.model_id LEFT JOIN TASK_INFO_T ti ON mt.task_id=ti.task_id WHERE ti.run_schema=0 AND mi.dataSource= :dataSource AND mi.model_status!=2 AND ti.task_status=1"
      );
      stringBuffer.append(" AND mi.group_id IN ( " + sb.toString() + " ) ");
      return stringBuffer.toString();
   }

   public static ModelInfoDto objectsToModelInfoDto(Object[] objects) {
      ModelInfoDto modelInfoDto = new ModelInfoDto();
      if (objects == null) {
         return modelInfoDto;
      } else {
         modelInfoDto.setModelId(Long.parseLong(String.valueOf(objects[0])));
         modelInfoDto.setModelName(String.valueOf(objects[1]));
         modelInfoDto.setGroupId(Long.parseLong(String.valueOf(objects[2])));
         modelInfoDto.setUpdateTime(String.valueOf(objects[3]));
         modelInfoDto.setModifierId(objects[4] == null ? 0 : Integer.parseInt(String.valueOf(objects[4])));
         modelInfoDto.setModifierName(String.valueOf(objects[5]));
         modelInfoDto.setDataSource(String.valueOf(objects[6]));
         if (objects[7] instanceof Clob) {
            String s = "";
            if (DBSelectUtil.isSqlServer()) {
               s = (String)objects[7];
            } else {
               s = clobToString((Clob)objects[7]);
            }

            modelInfoDto.setScreeningRule(objects[7] == null ? null : JSON.parseArray(s));
         } else {
            modelInfoDto.setScreeningRule(objects[7] == null ? null : JSON.parseArray(String.valueOf(objects[7])));
         }

         int modelStatuts = -2;
         if (objects[8] != null) {
            modelStatuts = Integer.parseInt(String.valueOf(objects[8]));
         }

         if (modelStatuts == 1) {
            modelStatuts = 2;
         } else if (modelStatuts >= 0 && modelStatuts != 1) {
            modelStatuts = 1;
         }

         modelInfoDto.setModelStatus(modelStatuts);
         return modelInfoDto;
      }
   }

   public static String queryModelInfoByStatus(long modelId) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("SELECT mi.model_id,mi.model_name,mi.group_id,mi.update_time,")
         .append("mi.modifier_id,mi.modifier_name,mi.datasource,mi.filter_rule,")
         .append("task.task_status FROM MODEL_INFO mi ")
         .append(" LEFT JOIN (SELECT mt.model_id,ti.task_status FROM MODEL_TASK_T mt LEFT JOIN TASK_INFO_T ti")
         .append(" ON mt.task_id=ti.task_id WHERE ti.run_schema=0) task ON mi.model_id=task.model_id")
         .append(" WHERE mi.model_id= :modelId AND mi.model_status=2  ");
      return sqlBuffer.toString();
   }

   public static String queryAllReportGroupByIds(List<Long> reportGroupIds, String keyword) {
      StringBuffer sqlBuffer = new StringBuffer();
      if (reportGroupIds != null && reportGroupIds.size() > 0) {
         reportGroupIds = removeDuplicate(reportGroupIds);
      }

      StringBuffer sb = splicingModelGroupIds(reportGroupIds, "t.id");
      sqlBuffer.append("select *  from report_group t   where t.id IN (" + sb.toString() + ") ");
      if (keyword != null && !"".equals(keyword.trim())) {
         keyword = keyword.replace("_", "\\_");
         sqlBuffer.append(" AND t.name LIKE '%" + keyword + "%'");
         if (keyword.contains("\\")) {
            sqlBuffer.append(" ESCAPE '\\'");
         }
      }

      sqlBuffer.append(" ORDER BY t.id DESC");
      return sqlBuffer.toString();
   }

   private static List<Long> removeDuplicate(List<Long> modelGroupIds) {
      LinkedHashSet<Long> set = new LinkedHashSet<>(modelGroupIds.size());
      set.addAll(modelGroupIds);
      modelGroupIds.clear();
      modelGroupIds.addAll(set);
      return modelGroupIds;
   }

   private static StringBuffer splicingModelGroupIds(List<Long> modelGroupIds, String row) {
      StringBuffer sb = new StringBuffer();
      int num = 1;

      for (int i = 0; i < modelGroupIds.size(); i++) {
         if (i == modelGroupIds.size() - 1) {
            sb.append(modelGroupIds.get(i) + "");
         } else if (num == 1000 && i > 0) {
            sb.append(modelGroupIds.get(i) + " ) or " + row + " in ( ");
            num = 1;
         } else {
            sb.append(modelGroupIds.get(i) + ",");
            num++;
         }
      }

      return sb;
   }

   public static String queryModelByID(List<String> modelIds) {
      StringBuffer sqlBuffer = new StringBuffer();
      sqlBuffer.append("select mi.model_id,mi.model_name,mi.group_id,mi.update_time,mi.modifier_id,mi.modifier_name,")
         .append("mi.datasource,mi.filter_rule,task.task_status,mi.is_online  from model_info mi left join ")
         .append("(SELECT mt.model_id, ti.task_status  FROM MODEL_TASK_T mt  LEFT JOIN TASK_INFO_T ti")
         .append(" ON mt.task_id = ti.task_id  WHERE ti.run_schema = 0) task ON mi.model_id=task.model_id ")
         .append(" WHERE mi.model_status!=2 AND mi.model_id in (:modelIds)");
      return sqlBuffer.toString();
   }
}
