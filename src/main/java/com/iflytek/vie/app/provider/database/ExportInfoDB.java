package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.export.ExportInfo;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportInfoDB {
   private HibernateTemplateUtils hiberTemp;
   private static final Logger logger = LoggerFactory.getLogger(ExportInfoDB.class);

   public Integer queryExportNum(String systemId, String timeStr) throws VieAppServiceException {
      String hql = "from ExportInfo ei where systemId = :systemId and ei.updateTime >= to_date('"
         + timeStr
         + " 00:00:00','yyyy-MM-dd hh24:mi:ss') and ei.updateTime <= to_date('"
         + timeStr
         + " 23:59:59','yyyy-MM-dd hh24:mi:ss')";

      try {
         List<ExportInfo> list = (List<ExportInfo>)(List<?>)this.hiberTemp.createQuery(hql, new String[]{"systemId"}, new Object[]{systemId});
         if (null != list && list.size() > 0) {
            int temp = 0;

            for (int i = 0; i < list.size(); i++) {
               temp += list.get(i).getExportNum();
            }

            return new Integer(temp);
         } else {
            return null;
         }
      } catch (Exception var7) {
         logger.error("查询用户每日导出文本数量方法queryExportNum出错", var7);
         throw new VieAppServiceException("查询用户每日导出文本数量方法queryExportNum出错", var7);
      }
   }

   public void updateExpoerInfo(ExportInfo exportInfo) throws VieAppServiceException {
      try {
         this.hiberTemp.saveOrUpdate(exportInfo);
         this.hiberTemp.flush();
      } catch (Exception var3) {
         logger.error("保存或更新文本导出记录方法updateExpoerInfo出错", var3);
         throw new VieAppServiceException("保存或更新文本导出记录方法updateExpoerInfo出错", var3);
      }
   }

   public ExportInfo queryExportInfo(String userId, String systemId, String timeStr) throws VieAppServiceException {
      String hql = "from ExportInfo ei where ei.userId = :userId and systemId = :systemId and ei.updateTime >= to_date('"
         + timeStr
         + " 00:00:00','yyyy-MM-dd hh24:mi:ss') and ei.updateTime <= to_date('"
         + timeStr
         + " 23:59:59','yyyy-MM-dd hh24:mi:ss')";

      try {
         List<ExportInfo> list = (List<ExportInfo>)(List<?>)this.hiberTemp.createQuery(hql, new String[]{"userId", "systemId"}, new Object[]{userId, systemId});
         return null != list && list.size() > 0 ? list.get(0) : null;
      } catch (Exception var6) {
         logger.error("查询文本导出记录方法queryExportInfo出错", var6);
         throw new VieAppServiceException("查询文本导出记录方法queryExportInfo出错", var6);
      }
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }
}
