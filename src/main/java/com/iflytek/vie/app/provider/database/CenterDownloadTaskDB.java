package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.download.CenterDownloadTask;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CenterDownloadTaskDB {
   private static final Logger logger = LoggerFactory.getLogger(CenterDownloadTaskDB.class);
   private HibernateTemplateUtils hiberTemp;

   public void setHiberTemp(HibernateTemplateUtils hiberTemp) {
      this.hiberTemp = hiberTemp;
   }

   public HibernateTemplateUtils getHiberTemp() {
      return this.hiberTemp;
   }

   public CenterDownloadTask fetchDownloadTask(int id) {
      try {
         return (CenterDownloadTask)this.hiberTemp.get(CenterDownloadTask.class, id);
      } catch (Exception var3) {
         logger.error("根据id获取下载任务【fetchDownloadTask】出错", var3);
         return null;
      }
   }
}
