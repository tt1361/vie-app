package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.report.ReportConfig;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonReportDB {
   private static final Logger logger = LoggerFactory.getLogger(CommonReportDB.class);
   private HibernateTemplateUtils hiberTemp;

   public List<ReportConfig> getCommonReportByPage(int pageSize, int pageNum, String reportName, List<Integer> idList) {
      if (StringUtils.isNullOrEmpry(reportName)) {
         try {
            String hql = "SELECT T from ReportConfig T where T.id IN (:ids) ORDER BY T.id DESC";
            String[] params = new String[]{"ids"};
            Object[] values = new Object[]{idList};
            return (List)this.hiberTemp.createQuery(pageNum, pageSize, hql, params, values);
         } catch (Exception var8) {
            logger.error("分页查询常用报表【getCommonReportByPage】出错", var8);
            return null;
         }
      } else {
         try {
            reportName = reportName.replace("_", "\\_");
            String hql = "";
            if (DBSelectUtil.isOracle()) {
               hql = "SELECT T from ReportConfig T where T.id IN (:ids) and T.name like :reportName escape '\\' ORDER BY T.id DESC";
            } else if (DBSelectUtil.isMysql()) {
               hql = "SELECT T from ReportConfig T where T.id IN (:ids) and T.name like :reportName escape '\\\\' ORDER BY T.id DESC";
            }

            String[] params = new String[]{"ids", "reportName"};
            Object[] values = new Object[]{idList, "%" + reportName + "%"};
            return (List)this.hiberTemp.createQuery(pageNum, pageSize, hql, params, values);
         } catch (Exception var9) {
            logger.error("根据报表名称分页查询常用报表【getCommonReportByPage】出错", var9);
            return null;
         }
      }
   }

   public long getCountCommonReport(String reportName, List<Integer> idList) {
      try {
         long count = 0L;
         Object obj = null;
         if (reportName != null && !reportName.trim().isEmpty()) {
            reportName = reportName.replace("_", "\\_");
            String hql = "";
            if (DBSelectUtil.isOracle()) {
               hql = "select count(T.id) from ReportConfig as T where T.id IN (:ids) and T.name like :reportName escape '\\' ORDER BY T.id DESC";
            } else if (DBSelectUtil.isMysql()) {
               hql = "select count(T.id) from ReportConfig as T where T.id IN (:ids) and T.name like :reportName escape '\\\\' ORDER BY T.id DESC";
            }

            String[] params = new String[]{"ids", "reportName"};
            Object[] values = new Object[]{idList, "%" + reportName + "%"};
            obj = this.hiberTemp.uniqueResult(hql, params, values);
         } else {
            String hql = "select count(T.id) from ReportConfig as T where T.id IN (:ids)";
            String[] params = new String[]{"ids"};
            Object[] values = new Object[]{idList};
            obj = this.hiberTemp.uniqueResult(hql, params, values);
         }

         if (obj != null) {
            count = (Long)obj;
         }

         return count;
      } catch (Exception var9) {
         logger.error("查询常用报表总量【getCountCommonReport】出错", var9);
         return 0L;
      }
   }

   public boolean deleteCommonReport(String reportIds) {
      try {
         if (StringUtils.isNullOrEmpry(reportIds)) {
            return true;
         } else {
            String[] idArrays = reportIds.split(",");
            if (idArrays != null && idArrays.length > 0) {
               List<Integer> idList = new ArrayList<>();

               for (String id : idArrays) {
                  idList.add(Integer.parseInt(id));
               }

               String[] paramNames = new String[]{"idList"};
               Object[] values = new Object[]{idList};
               String hql = "delete from ReportUsual where reportId in (:idList)";
               this.hiberTemp.bulkUpdate(hql, paramNames, values);
            }

            return true;
         }
      } catch (Exception var8) {
         logger.error("移除常用报表，转成普通报表【deleteCommonReport】出错", var8);
         return false;
      }
   }

   public List<Integer> getReportCommonReportIds(String userName) throws Exception {
      String hql = "select rc.reportId from ReportUsual as rc where rc.createUser=:userName";
      return (List)this.hiberTemp.createQuery(hql, new String[]{"userName"}, new Object[]{userName});
   }

   public List<Integer> getCommonReportByIds(List<Integer> reportIds) throws Exception {
      try {
         if (null != reportIds && reportIds.size() > 0) {
            reportIds = this.removeDuplicate(reportIds);
            StringBuffer sb = this.splicingModelGroupIds(reportIds, "rc.reportId");
            String hql = "select distinct(rc.reportId) from ReportUsual rc where rc.reportId IN (" + sb.toString() + ")";
            return (List)this.hiberTemp.createQuery(hql, new Object[0]);
         } else {
            return null;
         }
      } catch (Exception var4) {
         logger.error("查询常用报表【getCommonReportByIds】出错", var4);
         return null;
      }
   }

   public List<Integer> fetchReportByGroup(List<Integer> reportGroupIds) {
      try {
         if (null != reportGroupIds && reportGroupIds.size() > 0) {
            reportGroupIds = this.removeDuplicate(reportGroupIds);
            StringBuffer sb = this.splicingModelGroupIds(reportGroupIds, "rpg.id");
            String hql = "select rc.id  from ReportConfig rc left join rc.reportGroup rpg where rc.status=1  and rpg.id IN ("
               + sb.toString()
               + ") "
               + " order by rc.isUp DESC, rc.id DESC";
            return (List)this.hiberTemp.createQuery(hql, new Object[0]);
         } else {
            return null;
         }
      } catch (Exception var4) {
         logger.error("根据报表组id查询报表信息【fetchReportByGroup】出错", var4);
         return null;
      }
   }

   private List<Integer> removeDuplicate(List<Integer> modelGroupIds) {
      LinkedHashSet<Integer> set = new LinkedHashSet<>(modelGroupIds.size());
      set.addAll(modelGroupIds);
      modelGroupIds.clear();
      modelGroupIds.addAll(set);
      return modelGroupIds;
   }

   private StringBuffer splicingModelGroupIds(List<Integer> modelGroupIds, String row) {
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

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
