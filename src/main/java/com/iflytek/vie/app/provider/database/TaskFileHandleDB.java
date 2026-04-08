package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.api.datahandle.TaskfileDetailInfo;
import com.iflytek.vie.app.api.datahandle.TaskfileHandleInfo;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;

public class TaskFileHandleDB {
   private HibernateTemplateUtils hiberTemp;
   private static Logger logger = Logger.getLogger(TaskFileHandleDB.class);

   public TaskfileHandleInfo getHandleInfoByTime(Date time, String dataSource) {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();

      try {
         String hql = "select t from TaskfileHandleInfo t where t.handleTime=:time And t.dataSource =:dataSource";
         paramNames.add("time");
         values.add(time);
         paramNames.add("dataSource");
         values.add(dataSource);
         return (TaskfileHandleInfo)this.hiberTemp.uniqueResult(hql, paramNames, values);
      } catch (Exception var6) {
         logger.error("查询数据库TASKFILE_HANDLE_INFO失败:" + var6.getMessage());
         throw var6;
      }
   }

   public int getTaskfileDetailInfoCount(Date time, String dataSource) {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      Integer count = 0;
      if (time != null) {
         try {
            String hql = "select count(t.id) from TaskfileDetailInfo t where t.createTime = :time and t.dataSource = :dataSource and status = 1";
            paramNames.add("time");
            paramNames.add("dataSource");
            values.add(time);
            values.add(dataSource);
            Object result = this.hiberTemp.fetchFirstResult(hql, paramNames, values);
            if (result != null) {
               count = Integer.parseInt(result.toString());
            }
         } catch (Exception var8) {
            logger.error("查询数据处理失败总量失败:" + var8.getMessage());
            throw var8;
         }
      }

      return count == null ? 0 : count;
   }

   public List<TaskfileDetailInfo> getTaskfileDetailInfoList(int pageNum, int pageSize, Date time, String dataSource) {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      List<TaskfileDetailInfo> infoList = null;
      if (time != null) {
         try {
            String hql = "select t from TaskfileDetailInfo t where t.createTime = :time and t.dataSource = :dataSource and status = 1 order by t.callTime desc";
            paramNames.add("time");
            paramNames.add("dataSource");
            values.add(time);
            values.add(dataSource);
            infoList = (List)this.hiberTemp.createQuery(pageNum, pageSize, hql, paramNames, values);
         } catch (Exception var9) {
            logger.error("查询数据库TASKFILE_DETAIL_INFO失败:" + var9.getMessage());
            throw var9;
         }
      }

      return infoList;
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
