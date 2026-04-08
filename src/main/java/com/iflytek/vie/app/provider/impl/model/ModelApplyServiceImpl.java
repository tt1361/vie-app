package com.iflytek.vie.app.provider.impl.model;

import com.iflytek.vie.app.api.model.ModelApplyService;
import com.iflytek.vie.app.api.permission.DataAuthService;
import com.iflytek.vie.app.api.report.ReportGroupService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.datadrill.DataDetailResponse;
import com.iflytek.vie.app.pojo.model.DataMarkInfo;
import com.iflytek.vie.app.pojo.model.DataRemarkInfo;
import com.iflytek.vie.app.pojo.model.ModelColumnDataRequest;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.app.pojo.model.ModelTableDataRequest;
import com.iflytek.vie.app.pojo.model.ModelUpRequest;
import com.iflytek.vie.app.pojo.model.VoiceCommentRequest;
import com.iflytek.vie.app.pojo.model.VoiceMarkRequest;
import com.iflytek.vie.app.pojo.permission.ContextRequest;
import com.iflytek.vie.app.pojo.permission.DataResourceAuth;
import com.iflytek.vie.app.pojo.permission.DimensionAuth;
import com.iflytek.vie.app.pojo.report.ReportGroupIdRequest;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.DataMarkInfoApplyDB;
import com.iflytek.vie.app.provider.database.DataRemarkApplyDB;
import com.iflytek.vie.app.provider.database.ModelApplyDB;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.ModelGroupDB;
import com.iflytek.vie.app.provider.pojo.ModelStatus;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.anydrill.calculate.set.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelApplyServiceImpl implements ModelApplyService {
   private final Logger logger = LoggerFactory.getLogger(ModelApplyServiceImpl.class);
   private ModelApplyDB modelApplyDB;
   private DataMarkInfoApplyDB dataMarkInfoApplyDB;
   private DataRemarkApplyDB dataRemarkApplyDB;
   private ReportGroupService reportGroupService;
   private ModelDB modelDB;
   private ModelGroupDB modelGroupDB;
   private DataSourceInfo dataSourceInfo;
   private static DataAuthService dataAuthService;
   private ExcuteContext excuteContext;

   public boolean setModelUp(ModelUpRequest modelUpRequest) throws VieAppServiceException {
      boolean flag = false;

      try {
         if (modelUpRequest.getModelId() == null) {
            this.logger.error("模型置顶入参有误");
            throw new VieAppServiceException("模型置顶入参有误");
         } else {
            int upNum = this.modelApplyDB.queryMaxUpNum();
            ModelInfo modelInfo = this.modelApplyDB.queryModelById(modelUpRequest.getModelId());
            if (modelInfo == null) {
               this.logger.info("模型为空");
               return false;
            } else if (!modelUpRequest.isDeleteUp() && modelUpRequest.isDeleteUp()) {
               this.logger.error("模型置顶入参有误");
               throw new VieAppServiceException("模型置顶入参有误");
            } else {
               if (modelUpRequest.isDeleteUp()) {
                  modelInfo.setIsUp(0);
               } else {
                  modelInfo.setIsUp(upNum + 1);
               }

               this.modelApplyDB.saveModel(modelInfo);
               return true;
            }
         }
      } catch (Exception var5) {
         this.logger.error("模型置顶（取消置顶)服务内部错误");
         throw new VieAppServiceException("模型置顶（取消置顶)服务内部错误", var5);
      }
   }

   public boolean addVoiceMark(VoiceMarkRequest voiceMarkRequest) throws VieAppServiceException {
      boolean flag = false;

      try {
         if (voiceMarkRequest.getFragmentId() != null && voiceMarkRequest.getTelId() != null && !"".equals(voiceMarkRequest.getTelId())) {
            if (voiceMarkRequest.getMark() != 1 && voiceMarkRequest.getMark() != -1) {
               this.logger.error("人工测听标注入参有误");
               throw new VieAppServiceException("人工测听标注入参有误");
            } else {
               DataMarkInfo dataMarkInfo = this.dataMarkInfoApplyDB.queryDataMarkByTelId(voiceMarkRequest.getFragmentId(), voiceMarkRequest.getTelId());
               if (dataMarkInfo == null) {
                  dataMarkInfo = new DataMarkInfo();
                  dataMarkInfo.setFragmentId(voiceMarkRequest.getFragmentId());
                  dataMarkInfo.setMark(voiceMarkRequest.getMark());
                  dataMarkInfo.setTelId(voiceMarkRequest.getTelId());
               } else {
                  dataMarkInfo.setMark(voiceMarkRequest.getMark());
               }

               this.dataMarkInfoApplyDB.saveOrUpdate(dataMarkInfo);
               return true;
            }
         } else {
            this.logger.error("人工测听标注入参有误");
            throw new VieAppServiceException("人工测听标注入参有误");
         }
      } catch (Exception var4) {
         this.logger.error("人工测听标注服务内部错误");
         throw new VieAppServiceException("人工测听标注服务内部错误", var4);
      }
   }

   public boolean clearVoiceMark(VoiceMarkRequest voiceMarkRequest) throws VieAppServiceException {
      boolean flag = false;

      try {
         if (voiceMarkRequest.getFragmentId() != null && voiceMarkRequest.getTelId() != null && !"".equals(voiceMarkRequest.getTelId())) {
            List<String> telephonIdList = Arrays.asList(voiceMarkRequest.getTelId().split(","));
            this.dataMarkInfoApplyDB.updateMark(voiceMarkRequest.getFragmentId(), telephonIdList);
            return true;
         } else {
            this.logger.error("取消人工测听标注入参有误");
            throw new VieAppServiceException("取消人工测听标注入参有误");
         }
      } catch (Exception var4) {
         this.logger.error("取消人工测听标注服务内部错误");
         throw new VieAppServiceException("取消人工测听标注服务内部错误", var4);
      }
   }

   public boolean addFragmentVoiceComment(VoiceCommentRequest voiceCommentRequest) throws VieAppServiceException {
      boolean flag = false;

      try {
         if (voiceCommentRequest.getTelId() != null && !"".equals(voiceCommentRequest.getTelId()) && voiceCommentRequest.getFragmentId() != null) {
            DataRemarkInfo dataRemarkInfo = this.dataRemarkApplyDB.queryDataRemarkByTelId(voiceCommentRequest.getFragmentId(), voiceCommentRequest.getTelId());
            if (dataRemarkInfo == null) {
               dataRemarkInfo = new DataRemarkInfo();
               dataRemarkInfo.setFragmentId(voiceCommentRequest.getFragmentId());
               dataRemarkInfo.setRemark(voiceCommentRequest.getComment());
               dataRemarkInfo.setTelId(voiceCommentRequest.getTelId());
            } else {
               dataRemarkInfo.setRemark(voiceCommentRequest.getComment());
            }

            this.dataRemarkApplyDB.saveOrUpdate(dataRemarkInfo);
            return true;
         } else {
            this.logger.error("片段录音备注入参有误");
            throw new VieAppServiceException("片段录音备注入参有误");
         }
      } catch (Exception var4) {
         this.logger.error("片段录音备注服务内部错误");
         throw new VieAppServiceException("片段录音备注服务内部错误", var4);
      }
   }

   public List<ModelInfo> getAllModelByUser(AuthorizeInfo authorizeInfo) throws VieAppServiceException {
      ReportGroupIdRequest reportGroupIdRequest = new ReportGroupIdRequest();
      reportGroupIdRequest.setGroupType(1);
      reportGroupIdRequest.setDataSource(authorizeInfo.getDataSource());
      reportGroupIdRequest.setUserId(Long.valueOf(authorizeInfo.getUserId()));

      try {
         List<Long> groupIds;
         try {
            groupIds = this.reportGroupService.getGroupIdList(reportGroupIdRequest);
         } catch (NumberFormatException var5) {
            this.logger.error("userId转化为Long出错", var5);
            throw new VieAppServiceException("userId转化为Long出错", var5);
         } catch (VieAppServiceException var6) {
            this.logger.error("查询当前用户拥有的模型组出错", var6);
            throw new VieAppServiceException("查询当前用户拥有的模型组出错", var6);
         }

         List<Long> modelGroupIds = this.modelGroupDB.queryAllModelGroupIdsByPermission(groupIds, authorizeInfo.getDataSource());
         return this.modelDB.queryAllModels(authorizeInfo.getDataSource(), ModelStatus.OnLine, modelGroupIds);
      } catch (Exception var7) {
         this.logger.error("获取所有模型接口【getAllModelByUser】出错", var7);
         throw new VieAppServiceException("获取所有模型接口【getAllModelByUser】出错", var7);
      }
   }

   public HashMap<String, String> getAllModelMap(AuthorizeInfo authorizeInfo) throws VieAppServiceException {
      HashMap<String, String> result = new HashMap<>();
      if (authorizeInfo.getUserId() != "" && authorizeInfo.getUserId() != null) {
         if (authorizeInfo.getDataSource() != "" && authorizeInfo.getDataSource() != null) {
            try {
               List<ModelInfo> modelInfoLis = this.getAllModelByUser(authorizeInfo);
               int i = 0;

               for (int length = modelInfoLis.size(); i < length; i++) {
                  ModelInfo currentModel = modelInfoLis.get(i);
                  result.put(currentModel.getModelId() + "", currentModel.getModelName());
               }

               return result;
            } catch (Exception var7) {
               this.logger.error("获取模型id和名字对应map出错", var7);
               throw new VieAppServiceException("获取模型id和名字对应map出错", var7);
            }
         } else {
            throw new VieAppServiceException("dataSource is not right!");
         }
      } else {
         throw new VieAppServiceException("userId is not right!");
      }
   }

   public DataDetailResponse getTableData(ModelTableDataRequest modelTableDataRequest) throws Exception {
      this.logger.info("模型通话列表标准服务getTableData方法响应开始");
      int insightType = IndexConstants.getInsightType(modelTableDataRequest.getDataSource());
      String startTime = modelTableDataRequest.getStartTime();
      String endTime = modelTableDataRequest.getEndTime();
      int pageSize = modelTableDataRequest.getPageSize();
      int pageNum = modelTableDataRequest.getPageNum();
      int dataType = modelTableDataRequest.getDataType();
      String ifLone = modelTableDataRequest.getIfLone();
      String batchId = modelTableDataRequest.getBatchId();
      String userId = modelTableDataRequest.getUserId();
      String systemId = modelTableDataRequest.getSystemId();
      if (StringUtils.isNullOrEmpry(modelTableDataRequest.getModelId())) {
         this.logger.info("modelid 不能为空");
         this.logger.info("模型通话列表标准服务getTableData方法响应成功");
         return new DataDetailResponse();
      } else {
         String[] modelId = modelTableDataRequest.getModelId().split(",");
         DataDetailResponse dataDetailResponse = new DataDetailResponse();
         List<String> searchColumns = modelTableDataRequest.getSearchColumns();
         new ArrayList();
         String sql = "";
         String excuteSql = "";
         String selectSql = "";
         String timeCondSql = "";
         String modelCondSql = "";
         String permissonSql = "";
         String importSql = "";
         String pageSql = "";
         String orderSql = "";
         String tableName = "";
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         Calendar startCalendar = new GregorianCalendar();
         Calendar endCalendar = new GregorianCalendar();
         new Date();
         new Date();

         try {
            Date startDate = sdf.parse(startTime);
            Date enbDate = sdf.parse(endTime);
            startCalendar.setTime(startDate);
            endCalendar.setTime(enbDate);
         } catch (Exception var42) {
            this.logger.error("时间转化出错");
         }

         Long startTimeStamp = startCalendar.getTime().getTime();
         Long endTimeStamp = endCalendar.getTime().getTime();
         timeCondSql = timeCondSql + " timestamp >= " + startTimeStamp + " and timestamp <= " + endTimeStamp;
         if (modelId.length > 1) {
            modelCondSql = modelCondSql + " offLineTagId in(";

            for (int i = 0; i < modelId.length; i++) {
               if (i == modelId.length - 1) {
                  modelCondSql = modelCondSql + modelId[i] + ")";
               } else {
                  modelCondSql = modelCondSql + modelId[i] + ",";
               }
            }
         } else {
            modelCondSql = modelCondSql + " offLineTagId = " + modelId[0];
         }

         for (int ix = 0; ix < searchColumns.size(); ix++) {
            if (ix == searchColumns.size() - 1) {
               selectSql = selectSql + searchColumns.get(ix) + ",offLineTagId,silenceLong,inVadDuration,vadDuration";
            } else {
               if (1 == insightType) {
                  searchColumns.set(0, "id");
               }

               selectSql = selectSql + searchColumns.get(ix) + ",";
            }
         }

         searchColumns.add("offLineTagId");
         searchColumns.add("silenceLong");
         searchColumns.add("inVadDuration");
         searchColumns.add("vadDuration");
         if ("1".equals(ifLone)) {
            importSql = importSql + " and ifLone = 1 and batchId = '" + batchId + "'";
         } else {
            importSql = importSql + " and ifLone != 1 ";
         }

         if (2 == dataType) {
            importSql = importSql + " and (dataType = 0 or dataType = 1) ";
         } else {
            importSql = importSql + " and dataType = " + dataType;
         }

         permissonSql = this.getPermissionSql(userId, systemId, modelTableDataRequest.getDataSource());
         permissonSql = permissonSql + " and processed = 0" + importSql;
         if (StringUtils.isNotNullAndEmpry(modelTableDataRequest.getSortColumn()) && StringUtils.isNotNullAndEmpry(modelTableDataRequest.getSortColumn())) {
            orderSql = orderSql + " order by " + modelTableDataRequest.getSortColumn() + " " + modelTableDataRequest.getSortType();
         }

         tableName = IndexConstants.getModelTableName(modelTableDataRequest.getDataSource());
         pageSql = this.getPageSql(pageNum, pageSize);
         excuteSql = excuteSql
            + "select "
            + selectSql
            + " from "
            + tableName
            + " where "
            + timeCondSql
            + " and "
            + modelCondSql
            + permissonSql
            + " and "
            + pageSql
            + orderSql;
         this.logger.info("getTableData sql start:" + excuteSql);
         Long time_one = System.currentTimeMillis();
         String url = DynamicEsSource.getEsSourceByType(modelTableDataRequest.getDataSource(), "modelAnydrillAddress");
         ResultSet rs = this.excuteContext.executeSearchQuery(url, excuteSql);
         List<LinkedHashMap<String, Object>> resultList = rs.toList();
         Long time_two = System.currentTimeMillis();
         this.logger.info("cost time:" + (time_two - time_one) + "毫秒------------getTableData sql end:" + excuteSql);
         int totalCount = new Long(rs.getTotalCount()).intValue();
         int totalPage;
         int pageNow;
         if (pageSize > 0) {
            totalPage = totalCount % pageSize == 0 ? totalCount / pageSize : totalCount / pageSize + 1;
            pageNow = pageNum;
         } else {
            totalPage = 1;
            pageNow = 1;
            pageSize = totalCount;
         }

         dataDetailResponse.setTotalSize(totalCount);
         dataDetailResponse.setTotalPage(totalPage);
         dataDetailResponse.setPageNow(pageNow);
         dataDetailResponse.setPageSize(pageSize);
         if (totalCount > 0) {
            this.logger.info("模型通话列表标准服务getTableData方法响应成功");
         } else {
            this.logger.info("未查询到相关结果！");
            this.logger.info("模型通话列表标准服务getTableData方法响应成功");
         }

         dataDetailResponse.setValues(resultList);
         return dataDetailResponse;
      }
   }

   public DataDetailResponse getColumnData(ModelColumnDataRequest modelColumnDataRequest) throws Exception {
      this.logger.info("模型通话列表标准服务getColumnData方法响应开始");
      int insightType = IndexConstants.getInsightType(modelColumnDataRequest.getDataSource());
      String startTime = modelColumnDataRequest.getStartTime();
      String endTime = modelColumnDataRequest.getEndTime();
      int pageSize = modelColumnDataRequest.getPageSize();
      int pageNum = modelColumnDataRequest.getPageNum();
      int dataType = modelColumnDataRequest.getDataType();
      String ifLone = modelColumnDataRequest.getIfLone();
      String batchId = modelColumnDataRequest.getBatchId();
      String userId = modelColumnDataRequest.getUserId();
      String systemId = modelColumnDataRequest.getSystemId();
      if (StringUtils.isNullOrEmpry(modelColumnDataRequest.getModelId())) {
         this.logger.info("modelid 不能为空");
         this.logger.info("模型通话列表标准服务getColumnData方法响应成功");
         return new DataDetailResponse();
      } else {
         String[] modelId = modelColumnDataRequest.getModelId().split(",");
         new ArrayList();
         List<String> searchColumns = new ArrayList<>();
         DataDetailResponse dataDetailResponse = new DataDetailResponse();
         String sql = "";
         String excuteSql = "";
         String selectSql = "";
         String timeCondSql = "";
         String modelCondSql = "";
         String importSql = "";
         String permissonSql = "";
         String orderSql = "";
         String groupSql = "";
         String tableName = "";
         String pageSql = "";
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         Calendar startCalendar = new GregorianCalendar();
         Calendar endCalendar = new GregorianCalendar();
         new Date();
         new Date();

         try {
            Date startDate = sdf.parse(startTime);
            Date enbDate = sdf.parse(endTime);
            startCalendar.setTime(startDate);
            endCalendar.setTime(enbDate);
         } catch (Exception var43) {
            this.logger.error("时间转化出错");
         }

         Long startTimeStamp = startCalendar.getTime().getTime();
         Long endTimeStamp = endCalendar.getTime().getTime();
         timeCondSql = timeCondSql + " timestamp >= " + startTimeStamp + " and timestamp <= " + endTimeStamp;
         if (modelId.length > 1) {
            modelCondSql = modelCondSql + " offLineTagId in(";

            for (int i = 0; i < modelId.length; i++) {
               if (i == modelId.length - 1) {
                  modelCondSql = modelCondSql + "'" + modelId[i] + "')";
               } else {
                  modelCondSql = modelCondSql + "'" + modelId[i] + "',";
               }
            }
         } else {
            modelCondSql = modelCondSql + " offLineTagId = " + modelId[0];
         }

         selectSql = selectSql + " count(idCounter) as sumCallNum,year,month,day";
         permissonSql = this.getPermissionSql(userId, systemId, modelColumnDataRequest.getDataSource());
         orderSql = orderSql + " order by year,month,day asc";
         groupSql = groupSql + " group by year,month,day";
         permissonSql = permissonSql + " and processed = 0" + importSql;
         searchColumns.add("sumCallNum");
         searchColumns.add("year");
         searchColumns.add("month");
         searchColumns.add("day");
         if ("1".equals(ifLone)) {
            importSql = importSql + " and ifLone = 1 and batchId = '" + batchId + "'";
         } else {
            importSql = importSql + " and ifLone != 1 ";
         }

         if (2 == dataType) {
            importSql = importSql + " and (dataType = 0 or dataType = 1) ";
         } else {
            importSql = importSql + " and dataType = " + dataType;
         }

         tableName = IndexConstants.getModelTableName(modelColumnDataRequest.getDataSource());
         pageSql = this.getPageSql(pageNum, pageSize);
         excuteSql = excuteSql
            + "select "
            + selectSql
            + " from "
            + tableName
            + " where "
            + timeCondSql
            + " and "
            + modelCondSql
            + permissonSql
            + groupSql
            + " having "
            + pageSql
            + orderSql;
         this.logger.info("getColumnData sql start:" + excuteSql);
         Long time_one = System.currentTimeMillis();
         String url = DynamicEsSource.getEsSourceByType(modelColumnDataRequest.getDataSource(), "modelAnydrillAddress");
         ResultSet rs = this.excuteContext.executeSearchQuery(url, excuteSql);
         List<LinkedHashMap<String, Object>> resultList = rs.toList();
         Long time_two = System.currentTimeMillis();
         this.logger.info("cost time:" + (time_two - time_one) + "毫秒------------getColumnData sql end:" + excuteSql);
         int totalCount = new Long(rs.getTotalCount()).intValue();
         int totalPage;
         int pageNow;
         if (pageSize > 0) {
            totalPage = totalCount % pageSize == 0 ? totalCount / pageSize : totalCount / pageSize + 1;
            pageNow = pageNum;
         } else {
            totalPage = 1;
            pageNow = 1;
            pageSize = totalCount;
         }

         dataDetailResponse.setTotalSize(totalCount);
         dataDetailResponse.setTotalPage(totalPage);
         dataDetailResponse.setPageNow(pageNow);
         dataDetailResponse.setPageSize(pageSize);
         if (totalCount > 0) {
            this.logger.info("模型通话列表标准服务getColumnData方法响应成功");
         } else {
            this.logger.info("未查询到相关结果！");
            this.logger.info("模型通话列表标准服务getColumnData方法响应成功");
         }

         dataDetailResponse.setValues(resultList);
         return dataDetailResponse;
      }
   }

   public String getPermissionSql(String userId, String systemId, String dataSource) throws VieAppServiceException {
      String sql = "";

      try {
         this.logger.info("userId=" + userId);
         this.logger.info("systemId=" + systemId);
         this.logger.info("dataSource=" + dataSource);
         ContextRequest request = new ContextRequest();
         request.setUserId(Long.parseLong(userId));
         request.setContextPath(systemId);
         request.setDataSource(dataSource);
         Map<String, DataResourceAuth> funcMap = dataAuthService.getDataAuths(request);

         for (String ck : funcMap.keySet()) {
            DataResourceAuth dr = funcMap.get(ck);
            List<DimensionAuth> dimensionList = dr.getDataDimension();
            String dimensionName = dimensionList.get(0).getEnglishName();

            for (DimensionAuth da : dimensionList) {
               List<String> dataList = da.getValue();
               if (dataList != null && dataList.size() > 0) {
                  sql = sql + " and (";

                  for (int i = 0; i < dataList.size(); i++) {
                     if (i == dataList.size() - 1) {
                        sql = sql + " " + dimensionName + "='" + dataList.get(i) + "' ";
                     } else {
                        sql = sql + " " + dimensionName + "='" + dataList.get(i) + "' or ";
                     }
                  }

                  sql = sql + ")";
               }
            }
         }

         sql = StringUtils.isNullOrEmpry(sql) ? " and 1=2 " : sql;
      } catch (Exception var16) {
         this.logger.error("获取权限sql异常");
         sql = " and 1=2 ";
         throw new VieAppServiceException("获取权限sql异常1", var16);
      }

      this.logger.info("集成权限拼接sql为:" + sql);
      return sql;
   }

   private String getPageSql(int fetchFrom, int fetchSize) throws Exception {
      String result = "";
      int pageIndex = fetchFrom < 1 ? 1 : fetchFrom;
      int startNum = (pageIndex - 1) * fetchSize + 1;
      int endNum;
      if (-1 == fetchSize) {
         endNum = 999999;
      } else {
         endNum = pageIndex * fetchSize;
      }

      return " rownum between " + startNum + " and " + endNum;
   }

   public ModelApplyDB getModelApplyDB() {
      return this.modelApplyDB;
   }

   public void setModelApplyDB(ModelApplyDB modelApplyDB) {
      this.modelApplyDB = modelApplyDB;
   }

   public DataMarkInfoApplyDB getDataMarkInfoApplyDB() {
      return this.dataMarkInfoApplyDB;
   }

   public void setDataMarkInfoApplyDB(DataMarkInfoApplyDB dataMarkInfoApplyDB) {
      this.dataMarkInfoApplyDB = dataMarkInfoApplyDB;
   }

   public DataRemarkApplyDB getDataRemarkApplyDB() {
      return this.dataRemarkApplyDB;
   }

   public void setDataRemarkApplyDB(DataRemarkApplyDB dataRemarkApplyDB) {
      this.dataRemarkApplyDB = dataRemarkApplyDB;
   }

   public ReportGroupService getReportGroupService() {
      return this.reportGroupService;
   }

   public void setReportGroupService(ReportGroupService reportGroupService) {
      this.reportGroupService = reportGroupService;
   }

   public ModelDB getModelDB() {
      return this.modelDB;
   }

   public void setModelDB(ModelDB modelDB) {
      this.modelDB = modelDB;
   }

   public ModelGroupDB getModelGroupDB() {
      return this.modelGroupDB;
   }

   public void setModelGroupDB(ModelGroupDB modelGroupDB) {
      this.modelGroupDB = modelGroupDB;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public DataAuthService getDataAuthService() {
      return dataAuthService;
   }

   public void setDataAuthService(DataAuthService dataAuthService) {
      ModelApplyServiceImpl.dataAuthService = dataAuthService;
   }

   public ExcuteContext getExcuteContext() {
      return this.excuteContext;
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
   }
}
