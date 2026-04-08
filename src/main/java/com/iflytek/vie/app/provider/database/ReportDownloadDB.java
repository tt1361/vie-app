package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.report.ReportDownload;
import com.iflytek.vie.app.pojo.report.ReportTask;
import com.iflytek.vie.app.provider.impl.config.ModelCategoryServiceImpl;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportDownloadDB {
   private static final Logger logger = LoggerFactory.getLogger(ModelCategoryServiceImpl.class);
   private HibernateTemplateUtils hiberTemp;

   public List<ReportDownload> fetchReportDownload(String downloadReportName, String userName, int pageSize, int pageNum, int type, String dataSource) {
      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         StringBuffer hql = new StringBuffer("from ReportDownload rd where rd.createUser=:userName and rd.dataSource=:dataSource ");
         paramNames.add("userName");
         values.add(userName);
         paramNames.add("dataSource");
         values.add(dataSource);
         if (!StringUtils.isNullOrEmpry(downloadReportName)) {
            downloadReportName = downloadReportName.replace("_", "\\_");
            if (DBSelectUtil.isOracle()) {
               hql.append(" and rd.downloadName like :downloadReportName escape '\\'");
            } else if (DBSelectUtil.isMysql()) {
               hql.append(" and rd.downloadName like :downloadReportName escape '\\\\'");
            }

            paramNames.add("downloadReportName");
            values.add("%" + downloadReportName + "%");
         }

         if (type == 1) {
            hql.append(" and rd.progressRate = 100 ");
         } else {
            hql.append(" and rd.progressRate < 100 ");
         }

         hql.append(" order by  rd.downloadDate DESC");
         return (List)this.hiberTemp.createQuery(pageNum, pageSize, hql.toString(), paramNames, values);
      } catch (Exception var10) {
         logger.error("下载报表列表【fetchReportDownload】查询出错", var10);
         return null;
      }
   }

   public long fetchReportCountById(String downloadReportName, String userName, int type, String dataSource) {
      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         StringBuffer hql = new StringBuffer("select count(*) from ReportDownload rd where rd.createUser=:userName and rd.dataSource=:dataSource");
         paramNames.add("userName");
         values.add(userName);
         paramNames.add("dataSource");
         values.add(dataSource);
         if (!StringUtils.isNullOrEmpry(downloadReportName)) {
            downloadReportName = downloadReportName.replace("_", "\\_");
            if (DBSelectUtil.isOracle()) {
               hql.append(" and rd.downloadName like :downloadReportName escape '\\' ");
            } else if (DBSelectUtil.isMysql()) {
               hql.append(" and rd.downloadName like :downloadReportName escape '\\\\' ");
            }

            paramNames.add("downloadReportName");
            values.add("%" + downloadReportName + "%");
         }

         if (type == 1) {
            hql.append(" and rd.progressRate = 100");
         } else {
            hql.append(" and rd.progressRate < 100");
         }

         Object obj = this.hiberTemp.uniqueResult(hql.toString(), paramNames, values);
         long rows = 0L;
         if (obj != null) {
            rows = (Long)obj;
         }

         return rows;
      } catch (Exception var11) {
         logger.error("查询报表列表条数【fetchReportCountById】查询出错", var11);
         return 0L;
      }
   }

   public List<ReportDownload> fetchReportById(String ids) {
      List<ReportDownload> reportDownloads = null;

      try {
         if (StringUtils.isNullOrEmpry(ids)) {
            return reportDownloads;
         } else {
            String[] idArrays = ids.split(",");
            if (idArrays != null && idArrays.length > 0) {
               List<Integer> idList = new ArrayList<>();

               for (String id : idArrays) {
                  idList.add(Integer.parseInt(id));
               }

               String hql = " from  ReportDownload rd where rd.id in (:idList)";
               reportDownloads = (List)this.hiberTemp.createQuery(hql, new String[]{"idList"}, new Object[]{idList});
            }

            return reportDownloads;
         }
      } catch (Exception var9) {
         logger.error("查询报表列表【fetchReportById】查询出错", var9);
         return null;
      }
   }

   public boolean deleteReportDownload(String ids) {
      try {
         if (StringUtils.isNullOrEmpry(ids)) {
            return false;
         } else {
            String[] idArrays = ids.split(",");
            if (idArrays != null && idArrays.length > 0) {
               List<Integer> idList = new ArrayList<>();

               for (String id : idArrays) {
                  idList.add(Integer.parseInt(id));
               }

               String[] paramNames = new String[]{"idList"};
               Object[] values = new Object[]{idList};
               String hql = "delete from ReportDownload where id in (:idList)";
               this.hiberTemp.bulkUpdate(hql, paramNames, values);
               hql = "delete from ReportTask rd where rd.id in (:idList)";
               this.hiberTemp.bulkUpdate(hql, paramNames, values);
               return true;
            } else {
               return false;
            }
         }
      } catch (Exception var8) {
         logger.error("删除报表下载【deleteReportDownload】查询出错", var8);
         return false;
      }
   }

   public void saveOrUpReportDownload(ReportDownload reportDownload) {
      this.hiberTemp.saveOrUpdate(reportDownload);
   }

   public void saveOrUpReportTask(ReportTask reportTask) throws VieAppServiceException {
      try {
         this.hiberTemp.saveOrUpdate(reportTask);
         this.hiberTemp.flush();
      } catch (Exception var3) {
         logger.error("保存报表任务【saveOrUpReportTask】查询出错", var3);
         throw new VieAppServiceException("保存报表任务【saveOrUpReportTask】查询出错", var3);
      }
   }

   public ReportDownload fetchReportById(int id) throws VieAppServiceException {
      try {
         return (ReportDownload)this.hiberTemp.get(ReportDownload.class, id);
      } catch (Exception var3) {
         logger.error("根据报表下载id查询报表下载信息【fetchReportById】查询出错", var3);
         throw new VieAppServiceException("根据报表下载id查询报表下载信息【fetchReportById】查询出错", var3);
      }
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
