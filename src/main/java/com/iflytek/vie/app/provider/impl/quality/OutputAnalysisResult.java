package com.iflytek.vie.app.provider.impl.quality;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.quality.OutputAnalysisResultService;
import com.iflytek.vie.app.pojo.quality.OutputAnalysisData;
import com.iflytek.vie.app.provider.database.StoreFileDataDB;
import com.iflytek.vie.app.provider.pojo.StoreFileData;
import com.iflytek.vie.dynamic.DynamicDataSourceContextHolder;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OutputAnalysisResult implements OutputAnalysisResultService {
   private final Logger logger = LoggerFactory.getLogger(OutputAnalysisResult.class);
   ObjectMapper mapper = new ObjectMapper();
   @Autowired
   private Environment env;
   @Autowired
   private StoreFileDataDB storeFileDataDB;
   public static final String BATCHNAME = "batchName";
   public static final String TIMERANGGE = "timeRange";
   public static SimpleDateFormat simDateFormat = new SimpleDateFormat("yyyyMMdd");
   public static SimpleDateFormat simpleDateFormatDay = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

   @RequestMapping("/outputAnalysisResult")
   public String outputAnalysisResult(OutputAnalysisData outputAnalysisData) {
      String resultMsg = "";

      try {
         this.logger.info("outputAnalysisResult service start !");
         resultMsg = this.checkJson(outputAnalysisData);
         this.logger.info("resultMsg is  " + resultMsg);
         if (!resultMsg.equals("pass")) {
            return resultMsg;
         }

         String http = "http://" + this.env.getProperty("file_url") + "/";
         this.logger.info("fdfs download url is " + http);
         String requestType = outputAnalysisData.getRequestType() == null ? "" : outputAnalysisData.getRequestType();
         String typeValue = outputAnalysisData.getTypeValue() == null ? "" : outputAnalysisData.getTypeValue();
         String dataSource = outputAnalysisData.getDataSource() == null ? "" : outputAnalysisData.getDataSource();
         String fromTime = String.valueOf(outputAnalysisData.getFromTime());
         String toTime = String.valueOf(outputAnalysisData.getToTime());
         this.logger
            .info(
               "requestType : "
                  + requestType
                  + ", typeValue : "
                  + typeValue
                  + ", dataSource :"
                  + dataSource
                  + ", fromTime : "
                  + fromTime
                  + ", toTime : "
                  + toTime
            );
         List<StoreFileData> data = new ArrayList<>();
         if (!requestType.equals("batchName") && requestType != "batchName") {
            if (requestType.equals("timeRange") || requestType == "timeRange") {
               this.logger.debug("requestType is timeRange");
               Date beginTime = null;
               Date endTime = null;

               try {
                  beginTime = simpleDateFormatDay.parse(DateUtils.getDaybeforeOrAfter(fromTime, 0, simDateFormat, simpleDateFormatDay));
                  endTime = simpleDateFormatDay.parse(DateUtils.getDaybeforeOrAfter(toTime, 0, simDateFormat, simpleDateFormatDay));
                  this.logger.info("beginTime is " + beginTime + ",endTime is " + endTime);
                  data = this.storeFileDataDB.querySFDataByTime(beginTime, endTime, dataSource);
               } catch (Exception var14) {
                  this.logger.error("querySFDataByTime error");
               }
            }
         } else {
            this.logger.debug("requestType is batchName");
            data = this.storeFileDataDB.querySFDataByName(typeValue, dataSource);
         }

         this.logger.debug("request data size is " + data.size());
         List<HashMap<String, String>> fileList = new ArrayList<>();
         if (data.size() > 0) {
            for (StoreFileData sfd : data) {
               HashMap<String, String> file = new HashMap<>();
               file.put("dataTime", sfd.getUploadDate().toString());
               file.put("fileName", sfd.getFileName().toString());
               file.put("fileUrl", http + sfd.getStorePath().toString());
               file.put("batchName", sfd.getFileName().toString());
               file.put("status", this.getStatusName(sfd.getStatus()));
               fileList.add(file);
            }
         }

         Hashtable<String, Object> hm = new Hashtable<>();
         hm.put("requestType", requestType);
         hm.put("typeValue", typeValue);
         hm.put("fromTime", fromTime);
         hm.put("toTime", toTime);
         hm.put("dataSource", dataSource);
         hm.put("fileList", fileList);
         resultMsg = this.mapper.writeValueAsString(hm);
         this.logger.debug("outputAnalysisResult :" + resultMsg);
         this.logger.info("outputAnalysisResult service end !");
      } catch (Exception var15) {
         this.logger.error("get outputAnalysisResult  is error", var15);
      }

      return resultMsg;
   }

   private String getStatusName(int status) {
      String statusName = "";
      if (status == 7) {
         statusName = "处理中";
      } else if (status == 8) {
         statusName = "处理完成";
      } else if (status == 9) {
         statusName = "处理失败";
      }

      return statusName;
   }

   private String checkJson(OutputAnalysisData data) {
      String msg = "pass";
      String requestType = data.getRequestType();
      String dataSource = data.getDataSource();
      if (!DynamicDataSourceContextHolder.containsDataSource(dataSource)) {
         this.logger.warn("dataSource ：" + dataSource + " can't be use ");
         return "error :[dataSource is not exist]";
      } else if (StringUtils.isNullOrEmpry(dataSource)) {
         return "error :[dataSource is null]";
      } else if (StringUtils.isNullOrEmpry(requestType)) {
         return "error :[requestType is null]";
      } else {
         if (requestType.equals("batchName")) {
            String typeValue = data.getTypeValue();
            if (StringUtils.isNullOrEmpry(typeValue)) {
               return "error :[typeValue is null]";
            }
         } else {
            if (!requestType.equals("timeRange")) {
               return "error :[requestType error " + requestType + " , requestType is \"batchName\" or \"timeRange\" ]";
            }

            String fromTime = data.getFromTime();
            String toTime = data.getToTime();
            if (StringUtils.isNullOrEmpry(fromTime) || StringUtils.isNullOrEmpry(toTime)) {
               return "error :[Time is null]";
            }

            if (fromTime.length() != 8 || toTime.length() != 8) {
               return "error :[Time format error , timeFormat like \"yyyyMMdd\"]";
            }

            try {
               simDateFormat.parse(fromTime);
               simDateFormat.parse(toTime);
            } catch (Exception var9) {
               return "error :[Time format error , timeFormat like \"yyyyMMdd\"]";
            }

            try {
               Long.parseLong(fromTime);
               Long.parseLong(toTime);
            } catch (Exception var8) {
               return "error :[Time is not long ]";
            }
         }

         return msg;
      }
   }
}
