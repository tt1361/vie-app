package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.download.CenterDownload;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CenterDownloadDB {
   private static final Logger logger = LoggerFactory.getLogger(CenterDownloadDB.class);
   private HibernateTemplateUtils hiberTemp;

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public CenterDownload fetchDownload(int id) {
      try {
         return (CenterDownload)this.hiberTemp.get(CenterDownload.class, id);
      } catch (Exception var3) {
         logger.error("根据id获取下载文件【fetchDownload】出错", var3);
         return null;
      }
   }
}
