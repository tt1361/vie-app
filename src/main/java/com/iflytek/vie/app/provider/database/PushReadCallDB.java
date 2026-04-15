package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.topicgroup.PushReadCall;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class PushReadCallDB {
   private static final Logger logger = LoggerFactory.getLogger(PushReadCallDB.class);
   private HibernateTemplateUtils hiberTemp;
   private JdbcTemplate jdbcTemplate;

   public List<String> getCallIdList(
      int modelOrGroup, Long id, Long parentGroupId, Date beginTime, Date endTime, Map<Long, String> groupModelMap, int ifLone, String batchId, int dataType
   ) {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String sql = "select t.call_id from push_read_call t where t.deleted= 0 and t.call_time between :beginTime and :endTime ";
      if (modelOrGroup == 0) {
         sql = sql + " and t.group_id = :parentGroupId  and t.model_id = :id ";
         paramNames.add("parentGroupId");
         values.add(parentGroupId);
         paramNames.add("id");
         values.add(id);
      } else {
         StringBuffer sbf = new StringBuffer();
         sbf.append(" and (");

         for (Entry<Long, String> group : groupModelMap.entrySet()) {
            sbf.append("(t.group_id = ").append(group.getKey()).append(" and t.model_id in (").append(group.getValue());
            sbf.append(")) or ");
         }

         sql = sql + sbf.substring(0, sbf.length() - 3) + ") ";
      }

      sql = sql + " and t.if_lone = :ifLone and t.data_type = :dataType and t.batch_id = :batchId ";

      try {
         paramNames.add("beginTime");
         values.add(beginTime);
         paramNames.add("endTime");
         values.add(endTime);
         paramNames.add("ifLone");
         values.add(ifLone);
         paramNames.add("dataType");
         values.add(dataType);
         paramNames.add("batchId");
         values.add(batchId);
         return (List)this.hiberTemp.createSQLQuery(sql, paramNames, values);
      } catch (Exception var16) {
         logger.error("getCallIdList DB has a error!", var16);
         return null;
      }
   }

   public long getCallIdCount(
      int modelOrGroup, Long id, Long parentGroupId, Date beginTime, Date endTime, Map<Long, String> groupModelMap, int ifLone, String batchId, int dataType
   ) {
      long totalNum = 0L;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String sql = "select count(1) from push_read_call t where t.deleted= 0 and t.call_time between :beginTime and :endTime ";
      if (modelOrGroup == 0) {
         sql = sql + " and t.group_id = :parentGroupId  and t.model_id = :id ";
         paramNames.add("parentGroupId");
         values.add(parentGroupId);
         paramNames.add("id");
         values.add(id);
      } else {
         StringBuffer sbf = new StringBuffer();
         sbf.append(" and (");

         for (Entry<Long, String> group : groupModelMap.entrySet()) {
            sbf.append("(t.group_id = ").append(group.getKey()).append(" and t.model_id in (").append(group.getValue());
            sbf.append(")) or ");
         }

         sql = sql + sbf.substring(0, sbf.length() - 3) + ") ";
      }

      sql = sql + " and t.if_lone = :ifLone and t.data_type = :dataType and t.batch_id = :batchId ";

      try {
         paramNames.add("beginTime");
         values.add(beginTime);
         paramNames.add("endTime");
         values.add(endTime);
         paramNames.add("ifLone");
         values.add(ifLone);
         paramNames.add("dataType");
         values.add(dataType);
         paramNames.add("batchId");
         values.add(batchId);
         Object obj = this.hiberTemp.uniqueSQLResult(sql, paramNames, values);
         if (obj != null) {
            totalNum = Long.parseLong(String.valueOf(obj));
         }

         return totalNum;
      } catch (Exception var18) {
         logger.error("getCallIdCount DB has a error!", var18);
         return totalNum;
      }
   }

   public void savePushReadCalls(List<PushReadCall> readCalls) {
      if (readCalls != null && readCalls.size() != 0) {
         long begin = System.currentTimeMillis();
         int count = 0;
         int batchNum = 1000;
         Connection conn = null;
         PreparedStatement pst = null;
         String sql = null;
         if (DBSelectUtil.isSqlServer()) {
            sql = "INSERT INTO push_read_call(ID, GROUP_ID, MODEL_ID, CALL_ID, CALL_TIME, DELETED, REMARK, data_type, batch_id, if_lone) VALUES (NEXT VALUE FOR PUSH_READ_CALL_SEQ, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
         } else {
            sql = "INSERT INTO push_read_call(ID, GROUP_ID, MODEL_ID, CALL_ID, CALL_TIME, DELETED, REMARK, data_type, batch_id, if_lone) VALUES (PUSH_READ_CALL_SEQ.nextval, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
         }

         try {
            conn = this.jdbcTemplate.getDataSource().getConnection();
            conn.setAutoCommit(false);
            pst = conn.prepareStatement(sql);
            PushReadCall pushReadCall = null;

            for (int i = 0; i < readCalls.size(); i++) {
               pushReadCall = readCalls.get(i);
               pst.setLong(1, pushReadCall.getGroupId());
               pst.setLong(2, pushReadCall.getModelId());
               pst.setString(3, pushReadCall.getCallId());
               pst.setDate(4, new java.sql.Date(pushReadCall.getCallTime().getTime()));
               pst.setInt(5, pushReadCall.getDeleted());
               pst.setString(6, pushReadCall.getRemark());
               pst.setInt(7, pushReadCall.getDataType());
               pst.setString(8, pushReadCall.getBatchId());
               pst.setInt(9, pushReadCall.getIfLone());
               pst.addBatch();
               if (++count % batchNum == 0) {
                  pst.executeBatch();
                  conn.commit();
               }
            }

            if (count % batchNum != 0) {
               pst.executeBatch();
               conn.commit();
            }
         } catch (Exception var19) {
            logger.error("savePushReadCalls DB has a error!", var19);
         } finally {
            try {
               if (pst != null) {
                  pst.close();
               }

               if (conn != null) {
                  conn.close();
               }
            } catch (SQLException var18) {
               logger.error("savePushReadCalls DB has a error!", var18);
            }
         }

         long end = new Date().getTime();
         logger.info("批量插入:" + count + "条，耗时 : " + (end - begin) + "毫秒");
      }
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }

   public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
   }
}
