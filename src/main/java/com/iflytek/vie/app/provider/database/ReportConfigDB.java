package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.report.ReportConfig;
import com.iflytek.vie.app.pojo.report.ReportUsual;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportConfigDB {
   private HibernateTemplateUtils hiberTemp;
   private static final Logger logger = LoggerFactory.getLogger(ReportConfigDB.class);

   public List<ReportConfig> fetchReportByGroup(int reportGroupId, int pageSize, int pageNum, String reportName, String userName) throws VieAppServiceException {
      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         StringBuffer hql = new StringBuffer("select rc  from ReportConfig rc");
         if (reportGroupId != -1) {
            hql.append(" left join rc.reportGroup rpg where rc.status=1 and rpg.id = :reportGroupId");
            paramNames.add("reportGroupId");
            values.add(reportGroupId);
            if (!StringUtils.isNullOrEmpry(reportName)) {
               reportName = reportName.replace("_", "\\_");
               if (DBSelectUtil.isOracle()) {
                  hql.append(" and rc.name like :reportName escape '\\' ");
               } else if (DBSelectUtil.isMysql()) {
                  hql.append(" and rc.name like :reportName escape '\\\\' ");
               }

               paramNames.add("reportName");
               values.add("%" + reportName + "%");
            }
         } else {
            hql.append(" where rc.status=1 and rc.createUser = :userName");
            paramNames.add("userName");
            values.add(userName);
            if (!StringUtils.isNullOrEmpry(reportName)) {
               reportName = reportName.replace("_", "\\_");
               if (DBSelectUtil.isOracle()) {
                  hql.append(" and rc.name like :reportName escape '\\' ");
               } else if (DBSelectUtil.isMysql()) {
                  hql.append(" and rc.name like :reportName escape '\\\\' ");
               }

               paramNames.add("reportName");
               values.add("%" + reportName + "%");
            }
         }

         hql.append(" order by rc.isUp DESC, rc.id DESC");
         return (List)this.hiberTemp.createQuery(pageNum, pageSize, hql.toString(), paramNames, values);
      } catch (Exception var9) {
         logger.error("DB查询报表列表fetchReportByGroup方法出错", var9);
         throw new VieAppServiceException("DB查询报表列表fetchReportByGroup方法出错", var9);
      }
   }

   public List<ReportConfig> fetchReportByGroups(int reportGroupId, int pageSize, int pageNum, String reportName, List<Integer> reportGroupIds) throws VieAppServiceException {
      try {
         List<Integer> list = null;
         if (reportGroupId == -1) {
            list = reportGroupIds;
         } else {
            list = new ArrayList<>();
            list.add(reportGroupId);
         }

         if (list == null && list.size() == 0) {
            return null;
         } else {
            list = this.removeDuplicate(list);
            StringBuffer sb = this.splicingModelGroupIds(list, "rpg.id");
            List<String> paramNames = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            StringBuffer hql = new StringBuffer(
               "select rc from ReportConfig rc left join rc.reportGroup rpg where rc.status=1 and rpg.id in ( " + sb.toString() + " ) "
            );
            if (!StringUtils.isNullOrEmpry(reportName)) {
               reportName = reportName.replace("_", "\\_");
               if (DBSelectUtil.isOracle()) {
                  hql.append(" and rc.name like :reportName escape '\\' ");
               } else if (DBSelectUtil.isMysql()) {
                  hql.append(" and rc.name like :reportName escape '\\\\'");
               }

               paramNames.add("reportName");
               values.add("%" + reportName + "%");
            }

            hql.append(" order by rc.isUp desc, rc.id desc");
            return (List)this.hiberTemp.createQuery(pageNum, pageSize, hql.toString(), paramNames, values);
         }
      } catch (Exception var11) {
         logger.error("DB查询报表列表fetchReportByGroups方法出错", var11);
         throw new VieAppServiceException("DB查询报表列表fetchReportByGroups方法出错", var11);
      }
   }

   public List<ReportConfig> fetchReportByGroups(List<Integer> reportGroupIds) throws VieAppServiceException {
      try {
         if (reportGroupIds == null && reportGroupIds.size() == 0) {
            return null;
         } else {
            reportGroupIds = this.removeDuplicate(reportGroupIds);
            StringBuffer sb = this.splicingModelGroupIds(reportGroupIds, "rpg.id");
            StringBuffer hql = new StringBuffer(
               "select rc from ReportConfig rc left join rc.reportGroup rpg where rc.status=1 and rpg.id in ( " + sb.toString() + " ) "
            );
            hql.append(" order by rc.isUp desc, rc.id desc");
            return (List)this.hiberTemp.createQuery(hql.toString(), new Object[0]);
         }
      } catch (Exception var4) {
         logger.error("DB查询报表列表fetchReportByGroups方法出错", var4);
         throw new VieAppServiceException("DB查询报表列表fetchReportByGroups方法出错", var4);
      }
   }

   public List<ReportConfig> fetchReportById(String reportId) {
      try {
         String[] reportIds = reportId.split(",");
         List<Integer> reportIdList = new ArrayList<>();

         for (int i = 0; i < reportIds.length; i++) {
            if (reportIds[i] != null) {
               reportIdList.add(Integer.parseInt(reportIds[i]));
            }
         }

         String hql = " from ReportConfig rc where rc.status=1 and rc.id in (:reportIdList)";
         return (List)this.hiberTemp.createQuery(hql, new String[]{"reportIdList"}, new Object[]{reportIdList});
      } catch (Exception var5) {
         logger.error("DB查询报表列表fetchReportById 1个参数 方法出错", var5);
         return null;
      }
   }

   public boolean fetchReportById(String reportId, String createUser, String reportGroupName) {
      try {
         String hql = "from ReportConfig rc where rc.status=1 and rc.id = :reportId and rc.createUser = :createUser";
         List<ReportConfig> reportConfigs = (List<ReportConfig>)(List<?>)this.hiberTemp.createQuery(hql, new String[]{"reportId", "createUser"}, new Object[]{reportId, createUser});
         if (reportConfigs.size() > 0) {
            return true;
         } else {
            hql = "select rc  from ReportConfig rc left join rc.reportGroup rpg where rc.status=1 and rc.id = :reportId and rpg.name = :reportGroupName";
            reportConfigs = (List)this.hiberTemp.createQuery(hql, new String[]{"reportId", "reportGroupName"}, new Object[]{reportId, reportGroupName});
            return reportConfigs.size() > 0;
         }
      } catch (Exception var6) {
         logger.error("DB查询报表列表fetchReportById 3个参数 方法出错", var6);
         return false;
      }
   }

   public boolean fetchReportById(String reportId, String createUser) {
      try {
         if (StringUtils.isNullOrEmpry(reportId)) {
            return true;
         } else {
            String[] idArrays = reportId.split(",");
            if (idArrays != null && idArrays.length > 0) {
               List<Integer> idList = new ArrayList<>();

               for (String id : idArrays) {
                  idList.add(Integer.parseInt(id));
               }

               String[] paramNames = new String[]{"idList"};
               Object[] values = new Object[]{idList};
               String hql = "from ReportConfig rc where rc.status=1 and rc.id in (:idList)";
               List<ReportConfig> reportConfigs = (List<ReportConfig>)(List<?>)this.hiberTemp.createQuery(hql, paramNames, values);
               if (reportConfigs.size() == reportId.split(",").length) {
                  return true;
               }
            }

            return false;
         }
      } catch (Exception var9) {
         logger.error("DB查询报表列表fetchReportById 2个参数方法出错", var9);
         return false;
      }
   }

   public ReportConfig fetchReportByName(String reportName, String createUser) {
      try {
         String hql = "select T from ReportConfig as T where T.status=1 and T.name= :reportName and T.createUser=:createUser  ORDER BY T.id DESC";
         return (ReportConfig)this.hiberTemp.fetchFirstResult(hql, new String[]{"reportName", "createUser"}, new Object[]{reportName, createUser});
      } catch (Exception var4) {
         logger.error("DB查询报表列表fetchReportByName 2个参数  方法出错", var4);
         return null;
      }
   }

   public ReportConfig fetchReportByName(String reportName, int updateId, String createUser) {
      try {
         String hql = "select T from ReportConfig as T where T.status=1 and T.id! = :id and T.name= :reportName and T.createUser = :createUser ORDER BY T.id DESC";
         return (ReportConfig)this.hiberTemp
            .fetchFirstResult(hql, new String[]{"id", "reportName", "createUser"}, new Object[]{updateId, reportName, createUser});
      } catch (Exception var5) {
         logger.error("DB查询报表列表fetchReportByName 3个参数  方法出错", var5);
         return null;
      }
   }

   public ReportConfig getReportById(int reportId) {
      try {
         String hql = "from ReportConfig as T where T.status=1 and T.id= :reportId";
         String[] params = new String[]{"reportId"};
         Object[] values = new Object[]{reportId};
         return (ReportConfig)this.hiberTemp.uniqueResult(hql, params, values);
      } catch (Exception var5) {
         logger.error("DB根据reportId查询报表 getReportById 方法出错", var5);
         return null;
      }
   }

   public long fetchReportCountById(int reportGroupId, String reportName, String userName) throws VieAppServiceException {
      try {
         List<String> paramNames = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         StringBuffer hql = new StringBuffer("select count(*) from ReportConfig rc");
         if (reportGroupId == -1) {
            hql.append(" where rc.createUser= :userName ");
            paramNames.add("userName");
            values.add(userName);
         } else {
            hql.append(" left join rc.reportGroup rpg where rpg.id= :reportGroupId");
         }

         hql.append(" and rc.status=1 ");
         if (!StringUtils.isNullOrEmpry(reportName)) {
            reportName = reportName.replace("_", "\\_");
            if (DBSelectUtil.isOracle()) {
               hql.append(" and rc.name like :reportName escape '\\'");
            } else if (DBSelectUtil.isMysql()) {
               hql.append(" and rc.name like :reportName escape '\\\\'");
            }
         }

         Object obj = this.hiberTemp.uniqueResult(hql.toString(), new Object[0]);
         long rows = 0L;
         if (obj != null) {
            rows = (Long)obj;
         }

         return rows;
      } catch (Exception var10) {
         logger.error("DB查询fetchReportCountById方法出错", var10);
         throw new VieAppServiceException("DB查询fetchReportCountById方法出错", var10);
      }
   }

   public long fetchReportCountByIds(int reportGroupId, String reportName, List<Integer> reportGroupIds) throws VieAppServiceException {
      try {
         List<Integer> list = null;
         if (reportGroupId == -1) {
            list = reportGroupIds;
         } else {
            list = new ArrayList<>();
            list.add(reportGroupId);
         }

         if (list != null && list.size() > 0) {
            list = this.removeDuplicate(list);
         }

         StringBuffer sb = this.splicingModelGroupIds(list, "rpg.id");
         List<String> params = new ArrayList<>();
         List<Object> values = new ArrayList<>();
         StringBuffer hql = new StringBuffer(
            "select count(*) from ReportConfig rc left join rc.reportGroup rpg where rc.status=1 and rpg.id IN ( " + sb.toString() + " )"
         );
         if (!StringUtils.isNullOrEmpry(reportName)) {
            reportName = reportName.replace("_", "\\_");
            if (DBSelectUtil.isOracle()) {
               hql.append(" and rc.name like :reportName escape '\\'");
            } else if (DBSelectUtil.isMysql()) {
               hql.append(" and rc.name like :reportName escape '\\\\'");
            }

            params.add("reportName");
            values.add("%" + reportName + "%");
         }

         Object obj = this.hiberTemp.uniqueResult(hql.toString(), params, values);
         long rows = 0L;
         if (obj != null) {
            rows = (Long)obj;
         }

         return rows;
      } catch (Exception var12) {
         logger.error("DB查询fetchReportCountByIds方法出错", var12);
         throw new VieAppServiceException("DB查询fetchReportCountByIds方法出错", var12);
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

   public int fetchReportUpCountById(int reportGroupId, String userName) {
      try {
         String hql = "select max(rc.isUp) from ReportConfig rc left join rc.reportGroup rpg where rc.status=1 and rc.createUser=:userName and  rc.isUp >=1";
         Object obj = this.hiberTemp.uniqueResult(hql, new String[]{"userName"}, new Object[]{userName});
         int rows = 0;
         if (obj != null) {
            rows = Integer.parseInt(String.valueOf(obj));
         }

         return rows;
      } catch (Exception var6) {
         logger.error("查询报表列表已置顶最大值出错", var6);
         return 0;
      }
   }

   public void updateReportconfig(int total, int reportId) throws VieAppServiceException {
      try {
         String hql = "update ReportConfig set isUp=:total where id = :reportId";
         String[] paramNames = new String[]{"total", "reportId"};
         Object[] values = new Object[]{total, reportId};
         this.hiberTemp.bulkUpdate(hql, paramNames, values);
      } catch (Exception var6) {
         logger.error("报表置顶/取消置顶【updateReportconfig】出错", var6);
         throw new VieAppServiceException("报表置顶/取消置顶【updateReportconfig】出错", var6);
      }
   }

   public boolean shareReport(List<ReportConfig> rcs) {
      try {
         this.hiberTemp.saveOrUpdate(rcs);
         this.hiberTemp.flush();
         return true;
      } catch (Exception var3) {
         logger.error("报表分享/取消分享出错", var3);
         return false;
      }
   }

   public boolean isUsualReport(int reportId, String userName) throws VieAppServiceException {
      try {
         String hql = "select count(*) from ReportUsual rc where rc.reportId = :reportId and rc.createUser = :createUser";
         String[] paramNames = new String[]{"reportId", "createUser"};
         Object[] values = new Object[]{reportId, userName};
         Object obj = this.hiberTemp.uniqueResult(hql, paramNames, values);
         return obj != null && (Long)obj > 0L;
      } catch (Exception var7) {
         logger.error("判断报表是否常用【isUsualReport】出错", var7);
         throw new VieAppServiceException("判断报表是否常用【isUsualReport】出错", var7);
      }
   }

   public List<Integer> queryUsualReport(String userId) throws VieAppServiceException {
      try {
         String hql = "select rc.reportId from ReportUsual rc where rc.createUser = :createUser";
         String[] paramNames = new String[]{"createUser"};
         Object[] values = new Object[]{userId};
         return (List)this.hiberTemp.createQuery(hql, paramNames, values);
      } catch (Exception var5) {
         logger.error("判断报表是否常用【isUsualReport】出错", var5);
         throw new VieAppServiceException("判断报表是否常用【isUsualReport】出错", var5);
      }
   }

   public boolean usualReport(String ids, int type, String userName) throws VieAppServiceException {
      try {
         if (type == 1) {
            if (StringUtils.isNullOrEmpry(ids)) {
               return true;
            }

            String[] idArrays = ids.split(",");
            if (idArrays != null && idArrays.length > 0) {
               List<Integer> idList = new ArrayList<>();

               for (String id : idArrays) {
                  idList.add(Integer.parseInt(id));
               }

               String[] paramNames = new String[]{"idList"};
               Object[] values = new Object[]{idList};
               String hql = "delete ReportUsual where createUser=:userName and reportId in (:idList)";
               this.hiberTemp.bulkUpdate(hql, paramNames, values);
            }
         } else {
            List<ReportUsual> rus = new ArrayList<>();
            String[] idArr = ids.split(",");

            for (int i = 0; i < idArr.length; i++) {
               ReportUsual ru = new ReportUsual();
               ru.setCreateUser(userName);
               ru.setReportId(Integer.parseInt(idArr[i]));
               rus.add(ru);
            }

            this.hiberTemp.saveOrUpdate(rus);
            this.hiberTemp.flush();
            this.hiberTemp.clear();
         }

         return true;
      } catch (Exception var10) {
         logger.error("报表常用/取消常用【usualReport】出错", var10);
         throw new VieAppServiceException("报表常用/取消常用【usualReport】出错", var10);
      }
   }

   public boolean usualReportLog(String ids, int type, String userName) throws VieAppServiceException {
      try {
         if (type == 1) {
            if (StringUtils.isNullOrEmpry(ids)) {
               return true;
            }

            String[] idArrays = ids.split(",");
            if (idArrays != null && idArrays.length > 0) {
               List<Integer> idList = new ArrayList<>();

               for (String id : idArrays) {
                  idList.add(Integer.parseInt(id));
               }

               String[] paramNames = new String[]{"idList", "userName"};
               Object[] values = new Object[]{idList, userName};
               String hql = "delete ReportUsual where createUser=:userName and reportId in (:idList)";
               this.hiberTemp.bulkUpdate(hql, paramNames, values);
            }
         } else {
            String[] idArr = ids.split(",");

            for (int i = 0; i < idArr.length; i++) {
               if (!this.isUsualReport(Integer.parseInt(idArr[i]), userName)) {
                  ReportUsual ru = new ReportUsual();
                  ru.setCreateUser(userName);
                  ru.setReportId(Integer.parseInt(idArr[i]));
                  this.hiberTemp.saveOrUpdate(ru);
                  this.hiberTemp.flush();
                  this.hiberTemp.clear();
               }
            }
         }

         return true;
      } catch (Exception var10) {
         logger.error("报表常用/取消常用【usualReportLog】出错", var10);
         throw new VieAppServiceException("报表常用/取消常用【usualReportLog】出错", var10);
      }
   }

   public boolean deleteUsualReport(String ids, String userName) throws VieAppServiceException {
      try {
         if (StringUtils.isNullOrEmpry(ids)) {
            return true;
         } else {
            String[] idArrays = ids.split(",");
            if (idArrays != null && idArrays.length > 0) {
               List<Integer> idList = new ArrayList<>();

               for (String id : idArrays) {
                  idList.add(Integer.parseInt(id));
               }

               String[] paramNames = new String[]{"idList"};
               Object[] values = new Object[]{idList};
               String hql = "delete ReportUsual  where createUser != :userName and reportId in (:idList)";
               this.hiberTemp.bulkUpdate(hql, paramNames, values);
            }

            return true;
         }
      } catch (Exception var9) {
         logger.error("报表删除常用【deleteUsualReport】出错", var9);
         throw new VieAppServiceException("报表删除常用【deleteUsualReport】出错", var9);
      }
   }

   public boolean deleteReportById(String ids) throws VieAppServiceException {
      try {
         if (StringUtils.isNullOrEmpry(ids)) {
            return true;
         } else {
            String[] idArrays = ids.split(",");
            if (idArrays != null && idArrays.length > 0) {
               List<Integer> idList = new ArrayList<>();

               for (String id : idArrays) {
                  idList.add(Integer.parseInt(id));
               }

               String[] paramNames = new String[]{"idList"};
               Object[] values = new Object[]{idList};
               String hql = "delete from ReportConfig rc where rc.id in(:idList)";
               this.hiberTemp.bulkUpdate(hql, paramNames, values);
               hql = "delete from  ReportUsual where  reportId in (:idList)";
               this.hiberTemp.bulkUpdate(hql, paramNames, values);
            }

            return true;
         }
      } catch (Exception var8) {
         logger.error("删除报表【deleteReportById】出错", var8);
         throw new VieAppServiceException("删除报表【deleteUsualReport】出错", var8);
      }
   }

   public boolean saveOrUpdateReportConfig(ReportConfig reportConfig) {
      try {
         this.hiberTemp.saveOrUpdate(reportConfig);
         this.hiberTemp.flush();
         return true;
      } catch (Exception var3) {
         logger.error("报表保存或者更新【saveOrUpdateReportConfig】出错", var3);
         return false;
      }
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
