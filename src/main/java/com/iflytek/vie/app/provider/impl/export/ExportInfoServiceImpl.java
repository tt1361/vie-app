package com.iflytek.vie.app.provider.impl.export;

import com.iflytek.vie.app.api.export.ExportInfoService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.export.ExportInfo;
import com.iflytek.vie.app.pojo.export.ExportSurplusNumRequest;
import com.iflytek.vie.app.pojo.export.ExportUpdateRequest;
import com.iflytek.vie.app.provider.database.ExportInfoDB;
import com.iflytek.vie.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExportInfoServiceImpl implements ExportInfoService {
   private final Logger logger = LoggerFactory.getLogger(ExportInfoServiceImpl.class);
   private ExportInfoDB exportInfoDB;
   private Integer limitNum;

   public Integer exportSurplusNumDaily(ExportSurplusNumRequest request) throws VieAppServiceException {
      if (!StringUtils.isNullOrEmpry(request.getUserId()) && !StringUtils.isNullOrEmpry(request.getSystemId())) {
         String timeStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
         Integer result = null;

         try {
            Integer usedNum = this.exportInfoDB.queryExportNum(request.getSystemId(), timeStr);
            if (null == usedNum) {
               result = this.limitNum;
            } else if (this.limitNum > usedNum) {
               result = this.limitNum - usedNum;
            } else {
               result = 0;
            }

            return result;
         } catch (Exception var5) {
            this.logger.error("查询用户导出文本剩余数量调用exportSurplusNumDaily方法异常", var5);
            throw new VieAppServiceException("查询用户导出文本剩余数量异常", var5);
         }
      } else {
         this.logger.error("ExportSurplusNumDaily接口，请求参数异常");
         throw new VieAppServiceException("ExportSurplusNumDaily接口，请求参数异常");
      }
   }

   public void updateExportInfoNum(ExportUpdateRequest request) throws VieAppServiceException {
      if (!StringUtils.isNullOrEmpry(request.getUserId()) && !StringUtils.isNullOrEmpry(request.getSystemId()) && null != request.getNumber()) {
         String timeStr = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

         try {
            ExportInfo exportInfo = this.exportInfoDB.queryExportInfo(request.getUserId(), request.getSystemId(), timeStr);
            if (null == exportInfo) {
               exportInfo = new ExportInfo(request.getUserId(), request.getSystemId(), request.getNumber(), new Date());
            } else {
               exportInfo.setExportNum(exportInfo.getExportNum() + request.getNumber());
            }

            this.exportInfoDB.updateExpoerInfo(exportInfo);
         } catch (Exception var4) {
            this.logger.error("查询用户导出文本剩余数量调用exportSurplusNumDaily方法异常", var4);
            throw new VieAppServiceException("查询用户导出文本剩余数量异常", var4);
         }
      } else {
         this.logger.error("updateExportInfoNum接口，请求参数异常");
         throw new VieAppServiceException("updateExportInfoNum接口，请求参数异常");
      }
   }

   public ExportInfoDB getExportInfoDB() {
      return this.exportInfoDB;
   }

   public void setExportInfoDB(ExportInfoDB exportInfoDB) {
      this.exportInfoDB = exportInfoDB;
   }

   public Integer getLimitNum() {
      return this.limitNum;
   }

   public void setLimitNum(Integer limitNum) {
      this.limitNum = limitNum;
   }
}
