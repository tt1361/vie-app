package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.filter.FilterInfo;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterInfoDB {
   private HibernateTemplateUtils hibernateTemplate;
   private static final Logger logger = LoggerFactory.getLogger(FilterInfoDB.class);

   public void addOrUpdateFilterInfo(FilterInfo filterInfo, String dataSource) throws VieAppServiceException {
      int insightType = IndexConstants.getInsightType(dataSource);
      String isTask = 1 == insightType ? "1" : "0";

      try {
         filterInfo.setIsTask(isTask);
         this.hibernateTemplate.saveOrUpdate(filterInfo);
      } catch (Exception var6) {
         logger.error("添加筛选器信息  db类异常1");
         throw new VieAppServiceException("添加筛选器信息  db类异常1", var6);
      }
   }

   public void updateFlagFilterInfo(FilterInfo filterInfo) throws VieAppServiceException {
      try {
         String updateSql = "update FilterInfo t set flag=? where filterId=?";
         this.hibernateTemplate.bulkUpdate(updateSql, new Object[]{filterInfo.getFlag(), filterInfo.getFilterId()});
      } catch (Exception var3) {
         logger.error("添加筛选器信息  db类异常1");
         throw new VieAppServiceException("添加筛选器信息  db类异常1", var3);
      }
   }

   public void editFilterInfo(FilterInfo filterInfo, String dataSource) throws VieAppServiceException {
      int insightType = IndexConstants.getInsightType(dataSource);
      String isTask = 1 == insightType ? "1" : "0";

      try {
         filterInfo.setIsTask(isTask);
         this.hibernateTemplate.update(filterInfo);
      } catch (Exception var6) {
         logger.error("编辑筛选器信息  db类异常1");
         throw new VieAppServiceException("编辑筛选器信息  db类异常1", var6);
      }
   }

   public boolean deleteFilterInfo(FilterInfo filterInfo) throws VieAppServiceException {
      Session session = null;
      boolean flag = false;

      try {
         session = this.getHibernateTemplate().getSessionFactory().openSession();
         session.beginTransaction();
         String sql = "delete from FILTER_INFO where FILTER_ID = " + filterInfo.getFilterId();
         session.createSQLQuery(sql).executeUpdate();
         session.getTransaction().commit();
         logger.info(sql);
         logger.info("删除筛选器信息db类服务正常");
         flag = true;
      } catch (Exception var8) {
         logger.error("删除筛选器信息db类服务异常");
         throw new VieAppServiceException("删除筛选器信息db类服务异常", var8);
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return flag;
   }

   public List<FilterInfo> queryFilterInfo(String userId, String dataSource) throws VieAppServiceException {
      List<FilterInfo> filterInfoList = null;

      try {
         String hql = "FROM FilterInfo t WHERE userId =? and flag = '1' and database = ?";
         filterInfoList = (List)this.hibernateTemplate.find(hql, new Object[]{userId, dataSource});
         logger.info("query:查询筛选器信息db类服务正常");
         return filterInfoList;
      } catch (Exception var5) {
         logger.error("query:查询筛选器信息db类服务异常");
         throw new VieAppServiceException("query:查询筛选器信息db类服务异常", var5);
      }
   }

   public FilterInfo queryFilterInfo(FilterInfo filterInfo) throws VieAppServiceException {
      FilterInfo filter = null;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();

      try {
         String sql = "select t FROM FilterInfo t where filterId =:filterId and flag = '1' ";
         paramNames.add("filterId");
         values.add(filterInfo.getFilterId());
         filter = (FilterInfo)this.hibernateTemplate.uniqueResult(sql, paramNames, values);
         logger.info(sql);
         logger.info("query:查询筛选器信息db类服务正常");
         return filter;
      } catch (Exception var6) {
         logger.error("query:查询筛选器信息db类服务异常");
         throw new VieAppServiceException("query:查询筛选器信息db类服务异常", var6);
      }
   }

   public FilterInfo queryFilterInfoById(Integer filterId) throws VieAppServiceException {
      FilterInfo filter = null;

      try {
         filter = (FilterInfo)this.hibernateTemplate.get(FilterInfo.class, filterId.longValue());
         logger.info("query:查询筛选器信息db类服务正常");
         return filter;
      } catch (Exception var4) {
         logger.error("query:查询筛选器信息db类服务异常");
         throw new VieAppServiceException("query:查询筛选器信息db类服务异常", var4);
      }
   }

   public List queryDimandModelService(String dataSource, String keyword) throws VieAppServiceException {
      int insightType = IndexConstants.getInsightType(dataSource);
      String isTask = 1 == insightType ? "1" : "0";
      new ArrayList();
      List<DimensionConfig> resultList = new ArrayList<>();

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

            List<DimensionConfig> list = (List<DimensionConfig>)(List<?>)this.hibernateTemplate.find(sql, new Object[]{dataSource});
            if (list.size() > 0 && null != list) {
               for (int i = 0; i < list.size(); i++) {
                  DimensionConfig dc = (DimensionConfig)list.get(i);
                  if (1 == insightType && !dc.getIndexField().equals("voiceId")) {
                     resultList.add(dc);
                  } else if (0 == insightType && !dc.getIndexField().equals("taskId")) {
                     resultList.add(dc);
                  }
               }
            }
         }

         return resultList;
      } catch (Exception var10) {
         logger.error("获取全部维度取值db类(模糊查询)异常", var10);
         throw new VieAppServiceException("获取全部维度取值db类（模糊查询）异常", var10);
      }
   }

   public boolean checkFilterName(FilterInfo filterInfo) throws VieAppServiceException {
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      boolean repeat = false;

      try {
         String sql = "select count(t.filterId) FROM FilterInfo t where filterName =:filterName and userId =:userId and flag = '1'and database =:dataSource ";
         if (filterInfo.getFilterId() > 0L) {
            sql = sql + " and filterId not in :idList ";
            List<Long> idList = new ArrayList<>();
            paramNames.add("idList");
            idList.add(filterInfo.getFilterId());
            values.add(idList);
         }

         paramNames.add("filterName");
         values.add(filterInfo.getFilterName());
         paramNames.add("userId");
         values.add(filterInfo.getUserId());
         paramNames.add("dataSource");
         values.add(filterInfo.getDatabase());
         Object obj = this.hibernateTemplate.uniqueResult(sql, paramNames, values);
         int rows = 0;
         if (obj != null) {
            rows = Integer.parseInt(obj.toString());
         }

         if (rows == 0) {
            repeat = true;
         }

         logger.info("checkFilterName:查询筛选器信息db类服务正常");
         logger.info(sql);
         return repeat;
      } catch (Exception var8) {
         logger.error("checkFilterName:查询筛选器信息db类服务异常");
         throw new VieAppServiceException("checkFilterName:查询筛选器信息db类服务异常", var8);
      }
   }

   public HibernateTemplateUtils getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplateUtils hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
