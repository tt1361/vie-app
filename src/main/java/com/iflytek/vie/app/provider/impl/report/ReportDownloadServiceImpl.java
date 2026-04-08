package com.iflytek.vie.app.provider.impl.report;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.api.task.NotifyTaskService;
import com.iflytek.vie.app.api.dimension.DimensionService;
import com.iflytek.vie.app.api.model.ModelApplyService;
import com.iflytek.vie.app.api.report.ReportDownloadService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.common.PagerResponse;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.DimensionRequest;
import com.iflytek.vie.app.pojo.report.ReportDeleteRequest;
import com.iflytek.vie.app.pojo.report.ReportDownload;
import com.iflytek.vie.app.pojo.report.ReportDownloadRequest;
import com.iflytek.vie.app.pojo.report.ReportRequest;
import com.iflytek.vie.app.pojo.report.ReportTask;
import com.iflytek.vie.app.pojo.report.ReportTemplate;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.ReportDownloadDB;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.exception.VieDataIntergrationServiceException;
import com.iflytek.vie.pojo.NotifyTaskRequest;
import com.iflytek.vie.utils.StringUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportDownloadServiceImpl implements ReportDownloadService {
   private final Logger logger = LoggerFactory.getLogger(ReportDownloadService.class);
   private ReportDownloadDB reportDownloadDB;
   private ModelDB modelDB;
   private ModelApplyService modelApplyService;
   private CommonData commonData;
   private DimensionService dimensionService;
   protected ObjectMapper mapper = new ObjectMapper();
   private DataSourceInfo dataSourceInfo;
   private NotifyTaskService notifyTaskService;

   public PagerResponse<ReportDownload> queryDownloadReport(ReportDownloadRequest reportDownloadRequest) throws VieAppServiceException {
      PagerResponse<ReportDownload> pagerResponse = new PagerResponse();

      try {
         int totalRows = (int)this.reportDownloadDB
            .fetchReportCountById(
               reportDownloadRequest.getDownloadReportName(),
               reportDownloadRequest.getUserId(),
               reportDownloadRequest.getType(),
               reportDownloadRequest.getDataSource()
            );
         List<ReportDownload> reportDownloads = this.reportDownloadDB
            .fetchReportDownload(
               reportDownloadRequest.getDownloadReportName(),
               reportDownloadRequest.getUserId(),
               reportDownloadRequest.getPageSize(),
               reportDownloadRequest.getPageNum(),
               reportDownloadRequest.getType(),
               reportDownloadRequest.getDataSource()
            );
         pagerResponse.setPageNum(reportDownloadRequest.getPageNum());
         pagerResponse.setTotalRows(totalRows);
         pagerResponse.setRows(reportDownloads);
         pagerResponse.setPageSize(reportDownloadRequest.getPageSize());
         return pagerResponse;
      } catch (Exception var5) {
         this.logger.error("查询下载中/已完成报表列表接口queryDownloadReport出错", var5);
         throw new VieAppServiceException("查询下载中/已完成报表列表接口调用出错", var5);
      }
   }

   public ReportDownload getReportDownloadById(ReportRequest reportRequest) throws VieAppServiceException {
      try {
         return this.reportDownloadDB.fetchReportById(reportRequest.getReportId());
      } catch (Exception var3) {
         this.logger.error("根据下载报表id查询下载报表信息接口getReportDownloadById出错", var3);
         throw new VieAppServiceException("根据下载报表id查询下载报表信息接口出错", var3);
      }
   }

   public int getDownloadReportStatus(ReportRequest reportRequest) throws VieAppServiceException {
      if (reportRequest.getReportId() == 0) {
         this.logger.error("输入参数异常");
         throw new VieAppServiceException("输入参数异常");
      } else {
         try {
            ReportDownload reportDownload = this.reportDownloadDB.fetchReportById(reportRequest.getReportId());
            if (reportDownload == null) {
               this.logger.error("刷新失败");
               throw new VieAppServiceException("刷新失败");
            } else {
               return reportDownload.getProgressRate();
            }
         } catch (Exception var3) {
            this.logger.error("查询下载中/已完成报表进度接口getDownloadReportStatus出错", var3);
            throw new VieAppServiceException("查询下载中/已完成报表进度接口出错", var3);
         }
      }
   }

   public void saveDownloadReport(ReportTemplate reportTemplate) throws VieAppServiceException {
      try {
         List<HashMap<String, Object>> reportParam = null;
         reportParam = (List<HashMap<String, Object>>)this.mapper
            .readValue(reportTemplate.getReportConfig(), new TypeReference<List<HashMap<String, Object>>>() {});
         List<HashMap<String, Object>> reportCondMap = (List<HashMap<String, Object>>)this.mapper
            .readValue(reportTemplate.getReportCondition(), new TypeReference<List<HashMap<String, Object>>>() {});
         HashMap<String, Object> ifLoneMap = new HashMap<>();
         ifLoneMap.put("filed", "ifLone");
         ifLoneMap.put("type", "radio");
         if (reportTemplate.getIfLone() == 1) {
            ifLoneMap.put("isNegate", "0");
            if (StringUtils.isNotNullAndEmpry(reportTemplate.getBatchId())) {
               HashMap<String, Object> batchMap = new HashMap<>();
               batchMap.put("filed", "batchId");
               batchMap.put("type", "mulSel");
               batchMap.put("value", new String[]{reportTemplate.getBatchId()});
               reportCondMap.add(batchMap);
            }
         } else {
            ifLoneMap.put("isNegate", "1");
         }

         ifLoneMap.put("value", 1);
         reportCondMap.add(ifLoneMap);
         if (reportTemplate.getDataType() != null && reportTemplate.getDataType() != 2) {
            HashMap<String, Object> dataTypeMap = new HashMap<>();
            dataTypeMap.put("filed", "dataType");
            dataTypeMap.put("type", "mulSel");
            dataTypeMap.put("value", new Integer[]{reportTemplate.getDataType()});
            reportCondMap.add(dataTypeMap);
         }

         boolean isRepeatTitle = this.checkRepeatTitle(reportParam);
         if (!isRepeatTitle) {
            ReportDownload reportDownload = new ReportDownload();
            reportDownload.setDownloadName(reportTemplate.getReportName());
            reportDownload.setDownloadDate(new Date());
            reportDownload.setCreateUser(reportTemplate.getUserId());
            reportDownload.setProgressRate(1);
            reportDownload.setDataSource(reportTemplate.getDataSource());
            this.reportDownloadDB.saveOrUpReportDownload(reportDownload);
            DimensionRequest request = new DimensionRequest();
            request.setDataSource(reportTemplate.getDataSource());
            LinkedHashMap<String, DimensionConfig> dimensionMap = this.dimensionService.getDimensionMap(request);
            AuthorizeInfo authorizeInfo = new AuthorizeInfo();
            authorizeInfo.setUserId(reportTemplate.getUserId());
            authorizeInfo.setDataSource(reportTemplate.getDataSource());
            HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
            List<String> modelList = new ArrayList<>();
            modelList.addAll(modelMap.keySet());
            HashMap<String, Object> reportCond = new HashMap<>();
            reportCond.put("reportParam", reportParam);
            reportCond.put("measureList", this.commonData.getMeasureList());
            reportCond.put("dimensionMap", dimensionMap);
            reportCond.put("startTime", reportTemplate.getStartTime());
            reportCond.put("endTime", reportTemplate.getEndTime());
            reportCond.put("modelList", modelList);
            reportCond.put("modelMap", modelMap);
            reportCond.put("batchSize", 0);
            reportCond.put("tableName", reportTemplate.getDataSource());
            reportCond.put("reportName", reportTemplate.getReportName());
            reportCond.put("downloadCtx", reportTemplate.getDownloadCtx());
            reportCond.put("reportID", reportDownload.getId());
            reportCond.put("comment", reportTemplate.getComment());
            reportCond.put("reportConfig", reportCondMap);
            reportCond.put("dataSql", this.commonData.getDataSql(reportTemplate.getUserId(), reportTemplate.getSystemId(), reportTemplate.getDataSource()));
            String strReportCond = JSONObject.toJSONString(reportCond);
            ReportTask reportTask = new ReportTask();
            reportTask.setDownloadCondition(strReportCond);
            reportTask.setDownloadID(reportDownload.getId());
            reportTask.setStatus(0);
            this.reportDownloadDB.saveOrUpReportTask(reportTask);

            try {
               NotifyTaskRequest notifyTaskRequest = new NotifyTaskRequest();
               notifyTaskRequest.setTaskType(2);
               notifyTaskRequest.setTaskId(reportDownload.getId());
               notifyTaskRequest.setDataSource(reportTemplate.getDataSource());
               this.notifyTaskService.notifyTask(notifyTaskRequest);
            } catch (VieDataIntergrationServiceException var16) {
               var16.printStackTrace();
            }
         } else {
            this.logger.error("子表名称为空或重复");
            throw new VieAppServiceException("子表名称为空或重复");
         }
      } catch (Exception var17) {
         if (var17 instanceof VieAppServiceException) {
            this.logger.error(var17.getMessage());
            throw new VieAppServiceException(var17.getMessage());
         } else {
            this.logger.error("下载报表出错", var17);
            throw new VieAppServiceException("下载报表接口调用出错", var17);
         }
      }
   }

   public void deleteDownLoadReport(ReportDeleteRequest reportDeleteRequest) throws VieAppServiceException {
      try {
         List<ReportDownload> reportDownloads = this.reportDownloadDB.fetchReportById(reportDeleteRequest.getReportDownloadIds());
         if (!this.reportDownloadDB.deleteReportDownload(reportDeleteRequest.getReportDownloadIds())) {
            this.logger.error("删除下载中/已完成报表出错");
            throw new VieAppServiceException("删除下载中/已完成报表出错");
         } else {
            if (reportDownloads != null) {
               for (int i = 0; i < reportDownloads.size(); i++) {
                  try {
                     if (!StringUtils.isNullOrEmpry(reportDownloads.get(i).getFileDir())) {
                        File f = new File(reportDownloads.get(i).getFileDir());
                        if (f.exists()) {
                           f.delete();
                        }
                     } else {
                        this.logger.error("未找到已下载的报表文件");
                     }
                  } catch (Exception var5) {
                     this.logger.error("删除下载中/已完成报表出错", var5);
                     throw new VieAppServiceException("删除下载中/已完成报表出错", var5);
                  }
               }
            }
         }
      } catch (Exception var6) {
         this.logger.error("删除下载中/已完成报表接口deleteDownLoadReport出错", var6);
         throw new VieAppServiceException("删除下载中/已完成报表接口出错", var6);
      }
   }

   private boolean checkRepeatTitle(List<HashMap<String, Object>> reportParam) throws VieAppServiceException {
      boolean result = false;

      try {
         List<String> temp = new ArrayList<>();

         for (int i = 0; i < reportParam.size(); i++) {
            HashMap<String, Object> cData = reportParam.get(i);
            String ctitle = cData.get(CommonParams.reportTitle).toString();
            if (temp.contains(ctitle) || StringUtils.isNullOrEmpry(ctitle)) {
               result = true;
               break;
            }

            temp.add(ctitle);
         }

         return result;
      } catch (Exception var7) {
         this.logger.error("验证title是否重名出错", var7);
         throw new VieAppServiceException("验证title是否重名接口出错", var7);
      }
   }

   public ReportDownloadDB getReportDownloadDB() {
      return this.reportDownloadDB;
   }

   public void setReportDownloadDB(ReportDownloadDB reportDownloadDB) {
      this.reportDownloadDB = reportDownloadDB;
   }

   public ModelDB getModelDB() {
      return this.modelDB;
   }

   public void setModelDB(ModelDB modelDB) {
      this.modelDB = modelDB;
   }

   public ModelApplyService getModelApplyService() {
      return this.modelApplyService;
   }

   public void setModelApplyService(ModelApplyService modelApplyService) {
      this.modelApplyService = modelApplyService;
   }

   public CommonData getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonData commonData) {
      this.commonData = commonData;
   }

   public DimensionService getDimensionService() {
      return this.dimensionService;
   }

   public void setDimensionService(DimensionService dimensionService) {
      this.dimensionService = dimensionService;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public NotifyTaskService getNotifyTaskService() {
      return this.notifyTaskService;
   }

   public void setNotifyTaskService(NotifyTaskService notifyTaskService) {
      this.notifyTaskService = notifyTaskService;
   }
}
