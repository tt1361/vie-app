package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.serial.SerialDetail;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

public class SerialDetailDB {
   private HibernateTemplateUtils hiberTemp;
   private JdbcTemplate jdbcTemplate;
   private static final Logger logger = LoggerFactory.getLogger(SerialDetailDB.class);

   public boolean addSerialDetail(List<SerialDetail> serialDetailList) throws VieAppServiceException {
      if (serialDetailList != null && serialDetailList.size() != 0) {
         long begin = System.currentTimeMillis();
         int count = 0;
         int batchNum = 1000;
         Connection conn = null;
         PreparedStatement pst = null;
         String sql = null;
         if (DBSelectUtil.isSqlServer()) {
            sql = "INSERT INTO serial_detail(ID, BATCH_ID, SERIAL_ID, DIMENSIONS, PROCESS_RESULT, CREATE_TIME) VALUES (NEXT VALUE FOR SERIAL_DETAIL_SEQ, ?, ?, ?, ?, ?)";
         } else {
            sql = "INSERT INTO serial_detail(ID, BATCH_ID, SERIAL_ID, DIMENSIONS, PROCESS_RESULT, CREATE_TIME) VALUES (SERIAL_DETAIL_SEQ.nextval, ?, ?, ?, ?, ?)";
         }

         try {
            conn = this.jdbcTemplate.getDataSource().getConnection();
            conn.setAutoCommit(false);
            pst = conn.prepareStatement(sql);
            SerialDetail serialDetail = null;

            for (int i = 0; i < serialDetailList.size(); i++) {
               serialDetail = serialDetailList.get(i);
               pst.setString(1, serialDetail.getBatchId());
               pst.setString(2, serialDetail.getSerialId());
               pst.setString(3, serialDetail.getDimensions());
               pst.setInt(4, serialDetail.getProcessResult());
               pst.setTimestamp(5, new Timestamp(serialDetail.getCreateTime().getTime()));
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
         } catch (Exception var18) {
            logger.error("导入维度明细addSerialDetail方法出错", var18);
            throw new VieAppServiceException("导入维度明细addSerialDetail方法出错", var18);
         } finally {
            try {
               if (pst != null) {
                  pst.close();
               }

               if (conn != null) {
                  conn.close();
               }
            } catch (SQLException var17) {
               logger.error("addSerialDetail DB has a error!", var17);
            }
         }

         long end = new Date().getTime();
         logger.info("批量插入:" + count + "条，耗时 : " + (end - begin) + "毫秒");
         return true;
      } else {
         return false;
      }
   }

   public int getSerialDetailCount(String batchId) throws VieAppServiceException {
      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         String hql = new String("select count(sd.id) from SerialDetail sd where sd.batchId = :batchId");
         paramNames.add("batchId");
         values.add(batchId);
         Object obj = this.hiberTemp.uniqueResult(hql.toString(), paramNames, values);
         int rows = 0;
         if (obj != null) {
            rows = Integer.parseInt(obj.toString());
         }

         return rows;
      } catch (Exception var7) {
         logger.error("查询维度明细列表querySerialDetail方法出错", var7);
         throw new VieAppServiceException("查询维度明细列表querySerialDetail方法出错", var7);
      }
   }

   public List<SerialDetail> querySerialDetail(String batchId, int pageSize, int pageNum) throws VieAppServiceException {
      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         String hql = new String("select sd from SerialDetail sd where sd.batchId = :batchId order by sd.processResult desc,sd.id asc");
         paramNames.add("batchId");
         values.add(batchId);
         return (List)this.hiberTemp.createQuery(pageNum, pageSize, hql, paramNames, values);
      } catch (Exception var7) {
         logger.error("查询维度明细列表querySerialDetail方法出错", var7);
         throw new VieAppServiceException("查询维度明细列表querySerialDetail方法出错", var7);
      }
   }

   public List<SerialDetail> downloadSerialDetail(String batchId) throws VieAppServiceException {
      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         String hql = new String("select sd from SerialDetail sd where sd.batchId = :batchId");
         paramNames.add("batchId");
         values.add(batchId);
         return (List)this.hiberTemp.createQuery(hql, paramNames, values);
      } catch (Exception var5) {
         logger.error("下载维度更新明细downloadSerialDetail方法出错", var5);
         throw new VieAppServiceException("下载维度更新明细downloadSerialDetail方法出错", var5);
      }
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }

   public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
   }
}
