package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.call.CallFilter;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CallFilterDB {
   private HibernateTemplateUtils hiberTemp;
   private static final Logger logger = LoggerFactory.getLogger(CallFilterDB.class);

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }

   public String getDimensionByUserId(String userId, int type, int listType, String dataSource) {
      String hql = "SELECT t FROM CallFilter t WHERE t.userId =:userId AND t.filterType =:filterType AND delFlag = 0 AND listType = :listType And t.dataSource =:dataSource";

      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         paramNames.add("userId");
         values.add(Integer.parseInt(userId));
         paramNames.add("filterType");
         values.add(type);
         paramNames.add("listType");
         values.add(listType);
         paramNames.add("dataSource");
         values.add(dataSource);
         CallFilter callFilter = (CallFilter)this.hiberTemp.uniqueResult(hql, paramNames, values);
         return callFilter == null ? null : callFilter.getFilter();
      } catch (Exception var9) {
         logger.error("query from DB has a error!", var9);
         return null;
      }
   }

   public List<CallFilter> getFiltersByUserId(String userId, int type, int listType, String dataSource) {
      String hql = "SELECT t FROM CallFilter t WHERE t.dataSource =:dataSource And t.userId =:userId AND t.filterType =:filterType AND t.delFlag = 0 AND listType=:listType order by t.id desc";

      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         paramNames.add("dataSource");
         values.add(dataSource);
         paramNames.add("userId");
         values.add(Integer.parseInt(userId));
         paramNames.add("filterType");
         values.add(type);
         paramNames.add("listType");
         values.add(listType);
         return (List)this.hiberTemp.createQuery(hql, paramNames, values);
      } catch (Exception var9) {
         logger.error("query from DB has a error!", var9);
         return null;
      }
   }

   public CallFilter getCallFilterById(int id) {
      try {
         return (CallFilter)this.hiberTemp.get(CallFilter.class, id);
      } catch (Exception var3) {
         logger.error("query from DB has a error!", var3);
         return null;
      }
   }

   public CallFilter getCallFilterByUserIdAndType(String userId, int type, int listType, String dataSource) {
      String hql = "SELECT t FROM CallFilter t WHERE t.userId =:userId AND t.filterType =:filterType AND t.delFlag = 0 AND listType=:listType And dataSource =:dataSource";

      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         paramNames.add("userId");
         values.add(Integer.parseInt(userId));
         paramNames.add("filterType");
         values.add(type);
         paramNames.add("listType");
         values.add(listType);
         paramNames.add("dataSource");
         values.add(dataSource);
         CallFilter callFilter = (CallFilter)this.hiberTemp.uniqueResult(hql, paramNames, values);
         return callFilter == null ? null : callFilter;
      } catch (Exception var9) {
         logger.error("query from DB has a error!", var9);
         return null;
      }
   }

   public void deleteCallFilterById(int id, String dataSource) {
      String hql = "UPDATE FROM CallFilter t SET t.delFlag = 1 WHERE t.id =:id And t.dataSource =:dataSource";

      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         paramNames.add("id");
         values.add(id);
         paramNames.add("dataSource");
         values.add(dataSource);
         this.hiberTemp.bulkUpdate(hql, paramNames, values);
      } catch (Exception var6) {
         logger.error("query from DB has a error!", var6);
      }
   }

   public void saveCallFilter(String filter, String filterName, String userId, int type, int listType, String dataSource) {
      CallFilter callFilter = new CallFilter();
      callFilter.setUserId(Integer.parseInt(userId));
      callFilter.setFilter(filter);
      callFilter.setFilterName(filterName);
      callFilter.setFilterType(type);
      callFilter.setDelFlag(0);
      callFilter.setCreateTime(new Date());
      callFilter.setListType(listType);
      callFilter.setDataSource(dataSource);

      try {
         this.hiberTemp.save(callFilter);
      } catch (Exception var9) {
         logger.error("error occur when insert into DB!", var9);
      }
   }
}
