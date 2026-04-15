package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.report.ReportConfig;
import com.iflytek.vie.app.pojo.report.ReportGroup;
import com.iflytek.vie.app.provider.common.SqlHelper;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportGroupDB {
   private static final Logger logger = LoggerFactory.getLogger(ReportGroupDB.class);
   private HibernateTemplateUtils hiberTemp;

   public List<ReportGroup> fetchGroupsByIds(List<Long> reportGroupIds, String keyword) throws VieAppServiceException {
      try {
         String sql = SqlHelper.queryAllReportGroupByIds(reportGroupIds, keyword);
         return (List)this.hiberTemp.createSQLQuery(ReportGroup.class, sql, new Object[0]);
      } catch (Exception var4) {
         logger.error("报表组查询fetchGroupsByIds出错", var4);
         throw new VieAppServiceException("报表组查询fetchGroupsByIds出错", var4);
      }
   }

   public List<ReportGroup> fetchReportGroup(List<Long> roleList) throws VieAppServiceException {
      try {
         String hql = "from ReportGroup grp where grp.roleId IN (:roleList) or grp.name='共享组' order by grp.id asc";
         return (List)this.hiberTemp.createQuery(hql, new String[]{"roleList"}, new Object[]{roleList});
      } catch (Exception var3) {
         logger.error("报表组查询fetchReportGroup出错", var3);
         throw new VieAppServiceException("报表组查询fetchReportGroup出错", var3);
      }
   }

   public ReportGroup fetchReportGroupById(int reportGroupId) throws VieAppServiceException {
      try {
         return (ReportGroup)this.hiberTemp.get(ReportGroup.class, reportGroupId);
      } catch (Exception var3) {
         logger.error("报表组查询fetchReportGroupById出错", var3);
         throw new VieAppServiceException("报表组查询fetchReportGroupById出错", var3);
      }
   }

   public List<ReportGroup> fetchReportGroupByRole(String reportGroupName, List<Long> roleIds) throws VieAppServiceException {
      try {
         String hql = "from ReportGroup rc where rc.name = :reportGroupName";
         if (roleIds != null && roleIds.size() > 0) {
            hql = hql + " and rc.roleId IN (:roleIds)";
            return (List)this.hiberTemp.createQuery(hql, new String[]{"reportGroupName", "roleIds"}, new Object[]{reportGroupName, roleIds});
         } else {
            throw new VieAppServiceException("角色集合为空");
         }
      } catch (Exception var4) {
         logger.error("报表组查询fetchReportGroupByRole出错", var4);
         throw new VieAppServiceException("报表组查询fetchReportGroupByRole出错", var4);
      }
   }

   public boolean isMyReportGroup(int reportGroupId, String userName) throws VieAppServiceException {
      try {
         ReportGroup reportGroup = (ReportGroup)this.hiberTemp.get(ReportGroup.class, reportGroupId);
         return reportGroup != null;
      } catch (Exception var4) {
         logger.error("报表组查询isMyReportGroup出错", var4);
         throw new VieAppServiceException("报表组查询isMyReportGroup出错", var4);
      }
   }

   public boolean saveOrUpdate(List<ReportGroup> rgs) throws VieAppServiceException {
      try {
         for (ReportGroup reportGroup : rgs) {
            this.hiberTemp.saveOrUpdate(reportGroup);
            this.hiberTemp.flush();
         }

         return true;
      } catch (Exception var4) {
         logger.error("批量保存报表组saveOrUpdate出错", var4);
         throw new VieAppServiceException("批量保存报表组saveOrUpdate出错", var4);
      }
   }

   public boolean update(ReportGroup rg) throws VieAppServiceException {
      try {
         this.hiberTemp.update(rg);
         this.hiberTemp.flush();
         return true;
      } catch (Exception var3) {
         logger.error("更新报表组update出错", var3);
         throw new VieAppServiceException("更新报表组update出错", var3);
      }
   }

   public boolean deleteReportById(String ids) throws VieAppServiceException {
      try {
         String[] idArrays = ids.split(",");
         List<Integer> idList = new ArrayList<>();

         for (String id : idArrays) {
            idList.add(Integer.parseInt(id));
         }

         String sql = "select rc from ReportConfig rc left join rc.reportGroup rpg where rc.status=1 and rpg.id in (:idList)";
         List<ReportConfig> reportConfigs = (List<ReportConfig>)(List<?>)this.hiberTemp.createQuery(sql, new String[]{"idList"}, new Object[]{idList});
         List<Integer> reportIds = new ArrayList<>();
         if (reportConfigs != null && reportConfigs.size() > 0) {
            for (ReportConfig reportConfig : reportConfigs) {
               reportIds.add(reportConfig.getId());
            }

            this.hiberTemp.deleteAll(reportConfigs);
            sql = "delete from  ReportUsual where reportId in (:idList)";
            this.hiberTemp.bulkUpdate(sql, new String[]{"idList"}, new Object[]{idList});
         }

         sql = "delete from ReportGroup where id in (:idList)";
         this.hiberTemp.bulkUpdate(sql, new String[]{"idList"}, new Object[]{idList});
         return true;
      } catch (Exception var9) {
         logger.error("删除报表组deleteReportById出错", var9);
         throw new VieAppServiceException("删除报表组deleteReportById出错", var9);
      }
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
