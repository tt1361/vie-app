package com.iflytek.vie.app.provider.impl.datadrill;

import com.iflytek.vie.app.api.datadrill.DataDrillService;
import com.iflytek.vie.app.api.dimension.DimensionService;
import com.iflytek.vie.app.api.model.ModelService;
import com.iflytek.vie.app.api.permission.DataAuthService;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.datadrill.DataDetailRequest;
import com.iflytek.vie.app.pojo.datadrill.DataDetailResponse;
import com.iflytek.vie.app.pojo.dimension.AllDimensionRequest;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.DimensionRequest;
import com.iflytek.vie.app.pojo.dimension.DimensionResponse;
import com.iflytek.vie.app.pojo.permission.ContextRequest;
import com.iflytek.vie.app.pojo.permission.DataResourceAuth;
import com.iflytek.vie.app.provider.common.CommonInfoPlatform;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.engine.VoiceDetail;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.anydrill.calculate.set.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataDrillServiceImpl implements DataDrillService {
   private static final Logger logger = LoggerFactory.getLogger(DataDrillServiceImpl.class);
   private ExcuteContext excuteContext;
   private ExcuteContext excuteContextModel;
   private DimensionService dimensionService;
   private ModelService modelService;
   private DataSourceInfo dataSourceInfo;
   private DataAuthService dataAuthService;

   public DataDetailResponse queryVoiceList(DataDetailRequest request) throws ViePlatformServiceException {
      Date d1 = new Date();
      String guid = UUID.randomUUID() + "";
      DataDetailResponse dataDetailResponse = new DataDetailResponse();

      try {
         String systemID = request.getSystemId();
         String userId = request.getUserId();
         AuthorizeInfo autInfo = new AuthorizeInfo();
         autInfo.setSystemId(systemID);
         autInfo.setUserId(userId);
         List<DimensionConfig> dimensionConfigList = this.dimensionService.getAllDimensionService(new AllDimensionRequest(request.getDataSource(), autInfo));
         if (dimensionConfigList != null && dimensionConfigList.size() != 0) {
            Date d2 = new Date();
            logger.debug(guid + "获取维度耗时：" + this.getTimeDifference(d1, d2));
            DimensionRequest dimensionRequest = new DimensionRequest();
            dimensionRequest.setDataSource(request.getDataSource());
            DimensionResponse dimensionResponse = this.dimensionService.searchPersonalDimensionService(dimensionRequest);
            if (dimensionConfigList != null && dimensionResponse.isSuccessful()) {
               Date d3 = new Date();
               logger.debug(guid + "获取自定义维度耗时：" + this.getTimeDifference(d2, d3));
               this.addCustomerDimension(dimensionConfigList, dimensionResponse);
               DataResourceAuth dataResourceAuth = null;
               if (!userId.trim().equals("-1")) {
                  ContextRequest cr = new ContextRequest();
                  cr.setContextPath(request.getSystemId());
                  cr.setUserId(Long.parseLong(userId));
                  cr.setDataSource(request.getDataSource());
                  Map<String, DataResourceAuth> dataMap = this.dataAuthService.getDataAuths(cr);
                  if (dataMap != null) {
                     dataResourceAuth = dataMap.get(request.getDataSource());
                  }

                  if (dataMap == null || dataResourceAuth == null) {
                     throw new ViePlatformServiceException("明细数据查询接口错误:数据权限获取失败！");
                  }
               } else {
                  dataResourceAuth = new DataResourceAuth();
                  dataResourceAuth.setDataDimension(new ArrayList());
               }

               Date d4 = new Date();
               logger.debug(guid + "获取自定义维度耗时：" + this.getTimeDifference(d3, d4));
               HashMap<String, DimensionConfig> dimMap = CommonInfoPlatform.getDimensinMap(dimensionConfigList);
               this.dealAnyDrillData(request, dataDetailResponse, dimMap, dataResourceAuth, guid);
               Date d5 = new Date();
               logger.debug(guid + "明细接口总耗时：" + this.getTimeDifference(d1, d5));
               return dataDetailResponse;
            } else {
               String emsg = dimensionResponse.getMessage();
               throw new ViePlatformServiceException("明细数据查询接口错误:自定义维度获取失败！" + emsg);
            }
         } else {
            throw new ViePlatformServiceException("明细数据查询接口错误:维度获取失败！");
         }
      } catch (Exception var17) {
         logger.error("明细数据查询接口错误", var17);
         return new DataDetailResponse();
      }
   }

   private double getTimeDifference(Date start, Date end) {
      return (end.getTime() - start.getTime()) / 1000.0;
   }

   private void addCustomerDimension(List<DimensionConfig> dimensionConfigList, DimensionResponse dimensionResponse) {
      for (HashMap<String, String> data : dimensionResponse.getValue() == null ? new ArrayList<HashMap<String, String>>() : (List<HashMap<String, String>>)dimensionResponse.getValue()) {
         String dimensionAnotherName = data.get(CommonParams.dimensionAnotherName);
         String dimensionDataType = data.get(CommonParams.dimensionDataType);
         String dimensionType = data.get(CommonParams.dimensionType);
         DimensionConfig dimensionConfig = new DimensionConfig();
         dimensionConfig.setAnalysis(1);
         dimensionConfig.setAvailable(1);
         dimensionConfig.setIndexField(dimensionAnotherName);
         dimensionConfig.setDataType(dimensionDataType);
         dimensionConfig.setShowType(dimensionType);
         dimensionConfig.setFlag(0);
         dimensionConfigList.add(dimensionConfig);
      }
   }

   private void dealAnyDrillData(
      DataDetailRequest request, DataDetailResponse dataDetailResponse, HashMap<String, DimensionConfig> dimMap, DataResourceAuth dataResourceAuth, String guid
   ) throws Exception {
      String tableName = request.getDataSource();
      if (StringUtils.isNullOrEmpry(tableName)) {
         throw new Exception("数据源" + request.getDataSource() + "不存在！");
      } else {
         Date d1 = new Date();
         VoiceDetail vd = new VoiceDetail(tableName, request, dimMap, dataResourceAuth);
         String sql = vd.getSql(request.getUserId());
         Date d2 = new Date();
         logger.debug(guid + "明细接口 sql拼接耗时：" + this.getTimeDifference(d1, d2));

         try {
            logger.info("queryVoiceList sql start:" + sql);
            new ResultSet();
            ResultSet ex;
            if (vd.getTableName().contains("model")) {
               logger.info("need search model es !");
               String URL = DynamicEsSource.getEsSourceByType(tableName, "modelAnydrillAddress");
               ExcuteContext esModel = new ExcuteContext();
               ex = esModel.executeSearchQuery(URL, sql);
            } else {
               String URL = DynamicEsSource.getEsSourceByType(tableName, "mainAnydrillAddress");
               ExcuteContext esMain = new ExcuteContext();
               ex = esMain.executeSearchQuery(URL, sql);
            }

            logger.info("queryVoiceList sql finish:" + sql);
            Date d3 = new Date();
            logger.debug(guid + "明细接口 anydrill查询耗时：" + this.getTimeDifference(d2, d3));
            List<LinkedHashMap<String, Object>> linkedHashMapList = ex.toList();
            if (vd.isIsFormat()) {
               String systemID = request.getSystemId();
               String userId = request.getUserId();
               String dataSource = request.getDataSource();
               List<String> modelList = DataFormat.getMoldelList(userId, systemID, dataSource, this.modelService);
               linkedHashMapList = DataFormat.formatData(linkedHashMapList, modelList);
            }

            int totalCount = new Long(ex.getTotalCount()).intValue();
            int pageSize = request.getFetchSize();
            int totalPage = totalCount / pageSize;
            totalPage = totalCount % pageSize == 0 ? totalPage : totalPage + 1;
            dataDetailResponse.setValues(linkedHashMapList);
            dataDetailResponse.setPageNow(request.getFetchFrom());
            dataDetailResponse.setPageSize(pageSize);
            dataDetailResponse.setTotalSize(totalCount);
            dataDetailResponse.setTotalPage(totalPage);
            Date d4 = new Date();
            logger.debug(guid + "明细接口 数据格式化耗时：" + this.getTimeDifference(d3, d4));
         } catch (Exception var18) {
            logger.error("anydrill error", var18);
            throw new Exception("明细数据查询接口错误", var18);
         }
      }
   }

   public ExcuteContext getExcuteContext() {
      return this.excuteContext;
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
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

   public DataAuthService getDataAuthService() {
      return this.dataAuthService;
   }

   public void setDataAuthService(DataAuthService dataAuthService) {
      this.dataAuthService = dataAuthService;
   }

   public ModelService getModelService() {
      return this.modelService;
   }

   public void setModelService(ModelService modelService) {
      this.modelService = modelService;
   }

   public ExcuteContext getExcuteContextModel() {
      return this.excuteContextModel;
   }

   public void setExcuteContextModel(ExcuteContext excuteContextModel) {
      this.excuteContextModel = excuteContextModel;
   }
}
