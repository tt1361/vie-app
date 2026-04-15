package com.iflytek.vie.app.provider.impl.dimension;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.dimension.DimensionService;
import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.api.permission.UserService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.dimension.AllDimensionRequest;
import com.iflytek.vie.app.pojo.dimension.Constant;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.DimensionDTO;
import com.iflytek.vie.app.pojo.dimension.DimensionPersonal;
import com.iflytek.vie.app.pojo.dimension.DimensionPersonalClick;
import com.iflytek.vie.app.pojo.dimension.DimensionPersonalTask;
import com.iflytek.vie.app.pojo.dimension.DimensionPersonalValue;
import com.iflytek.vie.app.pojo.dimension.DimensionRequest;
import com.iflytek.vie.app.pojo.dimension.DimensionResponse;
import com.iflytek.vie.app.pojo.dimension.DimensionTask;
import com.iflytek.vie.app.pojo.dimension.DimensionValue;
import com.iflytek.vie.app.pojo.dimension.KeyDimensionRequest;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.pojo.permission.RoleInfo;
import com.iflytek.vie.app.pojo.permission.UserInfoRequest;
import com.iflytek.vie.app.provider.common.CommonDataPlatform;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.DimensionDBPlatform;
import com.iflytek.vie.app.provider.database.ModelDBPlatform;
import com.iflytek.vie.app.provider.database.ModelGroupDBPlatform;
import com.iflytek.vie.app.provider.ruleparse.ModelDTO;
import com.iflytek.vie.app.provider.ruleparse.ModelStatus;
import com.iflytek.vie.app.provider.ruleparse.SpecDimValue;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.ListUtil;
import com.iflytek.vie.utils.LoadCommonConfig;
import com.iflytek.vie.utils.StringUtils;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DimensionServiceImpl implements DimensionService {
   private final Logger logger = LoggerFactory.getLogger(DimensionServiceImpl.class);
   private DimensionDBPlatform dimensionDb;
   protected ExcuteContext excuteContext;
   private DimensionResponse dimensionResponse = new DimensionResponse();
   private DataSourceInfo dataSourceInfo;
   private CommonDataPlatform commonData;
   protected ObjectMapper mapper = new ObjectMapper();
   public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   private UserService userService;
   private ModelDBPlatform modelDB;
   private ModelGroupDBPlatform modelGroupDB;
   private GroupAuthService groupService;

   public List<DimensionConfig> getAllDimensionService(AllDimensionRequest allDimensionRequest) throws ViePlatformServiceException {
      new ArrayList();
      this.logger.info("开始获取所有系统维度集合服务");

      try {
         List<DimensionConfig> list = this.dimensionDb.getDimListByDataSource(allDimensionRequest.getDataSource());
         this.logger.info("获取所有系统维度集合服务响应成功");
         return list;
      } catch (Exception var4) {
         this.logger.error("获取所有系统维度集合服务内部错误");
         throw new ViePlatformServiceException("获取所有系统维度集合服务内部错误", var4);
      }
   }

   public DimensionResponse getAllDimService(AllDimensionRequest allDimensionRequest) throws ViePlatformServiceException {
      this.logger.info("getAllDimService  start！");
      this.dimensionResponse = new DimensionResponse();
      new ArrayList();

      try {
         List<DimensionConfig> list = this.dimensionDb.getDimListByDataSource(allDimensionRequest.getDataSource());
         new ArrayList();
         List<HashMap<String, Object>> resultHm = new ArrayList<>();
         if (list == null) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度不存在");
            this.logger.info("获取所有系统维度集合服务响应成功");
            return this.dimensionResponse;
         }

         for (int i = 0; i < list.size(); i++) {
            HashMap<String, Object> resultDimensionconfig = new HashMap<>();
            resultDimensionconfig.put("id", ((DimensionConfig)list.get(i)).getId());
            resultDimensionconfig.put("showName", ((DimensionConfig)list.get(i)).getShowName());
            resultDimensionconfig.put("indexField", ((DimensionConfig)list.get(i)).getIndexField());
            resultDimensionconfig.put("dataSource", ((DimensionConfig)list.get(i)).getDataSource());
            resultDimensionconfig.put("onlineType", ((DimensionConfig)list.get(i)).getOnlineType());
            resultDimensionconfig.put("show", ((DimensionConfig)list.get(i)).getShow());
            resultDimensionconfig.put("dataType", ((DimensionConfig)list.get(i)).getDataType());
            resultDimensionconfig.put("analysis", ((DimensionConfig)list.get(i)).getAnalysis());
            resultDimensionconfig.put("available", ((DimensionConfig)list.get(i)).getAvailable());
            resultDimensionconfig.put("showType", ((DimensionConfig)list.get(i)).getShowType());
            resultDimensionconfig.put("taskType", ((DimensionConfig)list.get(i)).getTaskType());
            resultDimensionconfig.put("genValue", ((DimensionConfig)list.get(i)).getGenValue());
            resultDimensionconfig.put("creator", ((DimensionConfig)list.get(i)).getCreator());
            resultDimensionconfig.put("modifyStatus", ((DimensionConfig)list.get(i)).getModifyStatus());
            List<DimensionValue> dimensionValueList = new ArrayList<>();
            dimensionValueList.addAll(((DimensionConfig)list.get(i)).getValueList());
            resultDimensionconfig.put("valueList", dimensionValueList);
            resultHm.add(resultDimensionconfig);
         }

         this.dimensionResponse.setSuccessful(true);
         this.dimensionResponse.setMessage("查询成功");
         this.dimensionResponse.setValue(resultHm);
         this.logger.info("获取所有系统维度集合服务响应成功");
      } catch (Exception var8) {
         this.logger.error("获取所有系统维度集合服务内部错误");
         throw new ViePlatformServiceException("获取所有系统维度集合服务内部错误", var8);
      }

      return this.dimensionResponse;
   }

   public DimensionResponse getTopicDimensionService(AllDimensionRequest allDimensionRequest) throws ViePlatformServiceException {
      this.logger.info("获取所有系统维度接口(专题模块)开始响应");
      this.dimensionResponse = new DimensionResponse();

      try {
         new ArrayList();
         new ArrayList();
         List<HashMap<String, Object>> resultHm = new ArrayList<>();
         List<DimensionConfig> e = this.dimensionDb.searchDim(allDimensionRequest.getDataSource(), allDimensionRequest.getKeyWord());
         if (e == null) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度不存在");
            return this.dimensionResponse;
         }

         for (int i = 0; i < e.size(); i++) {
            HashMap<String, Object> resultDimensionconfig = new HashMap<>();
            resultDimensionconfig.put("dataType", ((DimensionConfig)e.get(i)).getDataType());
            resultDimensionconfig.put("key", ((DimensionConfig)e.get(i)).getIndexField());
            resultDimensionconfig.put("name", ((DimensionConfig)e.get(i)).getShowName());
            resultDimensionconfig.put("type", ((DimensionConfig)e.get(i)).getShowType());
            resultDimensionconfig.put("analysis", ((DimensionConfig)e.get(i)).getAnalysis());
            resultDimensionconfig.put("flag", ((DimensionConfig)e.get(i)).getFlag());
            List<String> dimensionValueList = new ArrayList<>();

            for (DimensionValue dimensionValue : ((DimensionConfig)e.get(i)).getValueList()) {
               dimensionValueList.add(dimensionValue.getDimValue());
            }

            resultDimensionconfig.put("valueList", dimensionValueList);
            resultHm.add(resultDimensionconfig);
         }

         this.dimensionResponse.setSuccessful(true);
         this.dimensionResponse.setMessage("查询成功");
         this.dimensionResponse.setValue(resultHm);
      } catch (Exception var10) {
         this.logger.error("获取所有系统维度接口(专题模块)响应异常", var10);
         throw new ViePlatformServiceException("获取所有系统维度接口(专题模块)响应异常", var10);
      }

      this.logger.info("获取所有系统维度接口(专题模块)服务响应成功");
      return this.dimensionResponse;
   }

   public DimensionConfig getOneDimensionService(KeyDimensionRequest keyDimensionRequest) throws ViePlatformServiceException {
      new DimensionConfig();

      try {
         DimensionConfig dimensionConfig = this.dimensionDb.getValuesOfDim(keyDimensionRequest.getDataSource(), keyDimensionRequest.getKey());
         this.logger.info("根据key获取系统维度接口服务响应成功");
         return dimensionConfig;
      } catch (Exception var4) {
         this.logger.error("根据key获取系统维度接口服务内部错误");
         throw new ViePlatformServiceException("根据key获取系统维度接口服务内部错误", var4);
      }
   }

   public LinkedHashMap<String, DimensionConfig> getDimensionMap(DimensionRequest request) throws ViePlatformServiceException {
      LinkedHashMap<String, DimensionConfig> configMap = new LinkedHashMap<>();

      try {
         String dataSource = request.getDataSource();
         if (StringUtils.isNullOrEmpry(dataSource)) {
            return null;
         }

         String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
         new ExcuteContext();
         List<DimensionConfig> dimensionConfigList = this.dimensionDb.getDimListByDataSource(dataSource);
         List<DimensionPersonal> dpList = this.dimensionDb.searchAllDP("", dataSource);

         for (DimensionConfig dimensionConfig : dimensionConfigList) {
            configMap.put(dimensionConfig.getIndexField(), dimensionConfig);
         }

         for (DimensionPersonal dp : dpList) {
            DimensionConfig dc = new DimensionConfig();
            dc.setIndexField(dp.getDimensionAnotherName());
            dc.setShowType("mulSel");
            dc.setFlag(0);
            dc.setShowName(dp.getDimensionName());
            configMap.put(dp.getDimensionAnotherName(), dc);
         }
      } catch (Exception var11) {
         var11.printStackTrace();
      }

      return configMap;
   }

   public DimensionConfig getValuesOfDim(String dataSource, String filed) throws ViePlatformServiceException {
      return this.dimensionDb.getValuesOfDim(dataSource, filed);
   }

   public List<DimensionConfig> getSystemDimensionByField(String name, String dataSource) {
      return this.dimensionDb.getSystemDimensionByField(name, dataSource);
   }

   public List<DimensionPersonal> getDimensionByAnoterName(List<String> nameList) {
      return this.dimensionDb.getDimensionByAnoterName(nameList);
   }

   public DimensionResponse searchAllDimensionsService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search all dimensions");
         this.dimensionResponse = new DimensionResponse();
         if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("数据源为空");
            return this.dimensionResponse;
         } else if (StringUtils.isNullOrEmpry(request.getSystemId())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("系统名称为空");
            return this.dimensionResponse;
         } else if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("用户ID为空");
            return this.dimensionResponse;
         } else {
            List<DimensionConfig> dimensionConfigList = this.dimensionDb.getSystemDimensions(request.getDimensionName(), request.getDataSource());
            String dataLimit = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
            List<HashMap<String, Object>> result = new ArrayList<>();

            for (DimensionConfig dc : dimensionConfigList) {
               HashMap<String, Object> hm = new HashMap<>();
               hm.put("id", dc.getId());
               hm.put("showName", dc.getShowName());
               hm.put("showCode", dc.getIndexField());
               hm.put("showType", dc.getShowType());
               hm.put("dataType", dc.getDataType());
               List<String> value = new ArrayList<>();

               for (DimensionValue dv : dc.getValueList()) {
                  if (dataLimit.contains(dc.getIndexField()) && dataLimit.contains(dv.getDimValue())) {
                     value.add(dv.getDimValue());
                  } else if (!dataLimit.contains(dc.getIndexField())) {
                     value.add(dv.getDimValue());
                  }
               }

               hm.put("shouValue", value);
               result.add(hm);
            }

            this.dimensionResponse.setSuccessful(true);
            this.dimensionResponse.setMessage("查询成功");
            this.dimensionResponse.setValue(result);
            this.logger.info("success search all dimensions");
            return this.dimensionResponse;
         }
      } catch (Exception var11) {
         throw new ViePlatformServiceException("查询所有系统维度异常", var11);
      }
   }

   public DimensionResponse updateSystemDimensionService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start update system dimension");
         if (request.getDimensionId() == null) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度ID不存在");
            return this.dimensionResponse;
         } else if (StringUtils.isNullOrEmpry(request.getDataSource()) || "null".equals(request.getDataSource())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("数据源不存在");
            return this.dimensionResponse;
         } else if (!StringUtils.isNullOrEmpry(request.getDimensionName()) && !"null".equals(request.getDimensionName())) {
            String regEx = "[`~@$%^*={}':;',\\[\\].<>/?~！!&@￥……*（）——{}【】‘；：”“'。，、？]";
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(request.getDimensionName());
            if (!StringUtils.isNullOrEmpry(request.getDimensionName()) && !m.find() && request.getDimensionName().length() <= 20) {
               String tableName = request.getDataSource();
               this.dimensionResponse = new DimensionResponse();
               boolean isHaveSameName = this.dimensionDb.isHaveSameName(request.getDimensionName(), request.getDimensionId(), request.getDataSource());
               if (isHaveSameName) {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("已存在相同名称的维度");
                  return this.dimensionResponse;
               } else {
                  boolean isEqualPersonal = this.dimensionDb.isEqualPersonalDimension(request.getDimensionName(), tableName);
                  if (isEqualPersonal) {
                     this.dimensionResponse.setSuccessful(false);
                     this.dimensionResponse.setMessage("已存在相同名称的自定义维度");
                     return this.dimensionResponse;
                  } else {
                     DimensionConfig dimensionConfig = this.dimensionDb.getSystemDimensionById(request.getDimensionId(), request.getDataSource());
                     if (dimensionConfig == null) {
                        this.dimensionResponse.setSuccessful(false);
                        this.dimensionResponse.setMessage("更新的维度不存在");
                        return this.dimensionResponse;
                     } else {
                        dimensionConfig.setShowName(request.getDimensionName());
                        dimensionConfig.setModifyStatus(1);
                        this.dimensionDb.updateSystemDimension(dimensionConfig);
                        this.dimensionResponse.setSuccessful(true);
                        this.dimensionResponse.setMessage("更新成功");
                        this.logger.info("success update system dimension");
                        return this.dimensionResponse;
                     }
                  }
               }
            } else {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("维度名称包含特殊字符或长度大于20");
               return this.dimensionResponse;
            }
         } else {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度名称不存在");
            return this.dimensionResponse;
         }
      } catch (Exception var9) {
         this.logger.error("更新系统维度错误", var9);
         throw new ViePlatformServiceException("更新系统维度错误", var9);
      }
   }

   public DimensionResponse searchPersonalDimensionByIdService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search personal dimension value");
         this.dimensionResponse = new DimensionResponse();
         if (request.getDimensionId() == null) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度ID为空");
            return this.dimensionResponse;
         } else if (!StringUtils.isNullOrEmpry(request.getDataSource()) && !"null".equals(request.getDataSource())) {
            String dataSourceName = request.getDataSource();
            String tableName = "";

            try {
               tableName = dataSourceName;
            } catch (Exception var11) {
               this.logger.error("数据源获取错误", var11);
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("数据源获取错误");
               return this.dimensionResponse;
            }

            DimensionPersonal dp = this.dimensionDb.searchPersonalDimension(request.getDimensionId(), tableName);
            if (dp == null) {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("维度不存在");
               return this.dimensionResponse;
            } else {
               HashMap<String, Object> resultHm = new HashMap<>();
               List<HashMap<String, Object>> hashMapList = new ArrayList<>();
               resultHm.put("dimensionId", dp.getDimensionId());
               resultHm.put("dimensionName", dp.getDimensionName());
               resultHm.put("dimensionType", dp.getDimensionType());
               Set<DimensionPersonalValue> dps = dp.getDimensionValueSet();
               if (dps != null && dps.size() >= 1) {
                  for (DimensionPersonalValue dpv : dps) {
                     HashMap<String, Object> hm2 = new HashMap<>();
                     hm2.put("dimensionName", dpv.getDimensionName());
                     hm2.put("dimensionId", dpv.getId());
                     hm2.put("dimensionColor", dpv.getDimensionColor());
                     hm2.put("showIndex", dpv.getShowIndex());
                     hashMapList.add(hm2);
                  }

                  resultHm.put("dimensionValues", hashMapList);
               } else {
                  resultHm.put("dimensionValues", hashMapList);
               }

               this.dimensionResponse.setSuccessful(true);
               this.dimensionResponse.setMessage("查询成功");
               this.dimensionResponse.setValue(resultHm);
               this.logger.info("success search personal dimension value");
               return this.dimensionResponse;
            }
         } else {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("数据源不存在");
            return this.dimensionResponse;
         }
      } catch (Exception var12) {
         this.logger.info("查询自定义维度值错误", var12);
         throw new ViePlatformServiceException("查询自定义维度值错误", var12);
      }
   }

   public DimensionResponse searchPersonalDimensionService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search personal dimension");
         if (!StringUtils.isNullOrEmpry(request.getDataSource()) && !"null".equals(request.getDataSource())) {
            String tableName = "";

            try {
               tableName = request.getDataSource();
            } catch (Exception var12) {
               this.logger.error("维度获取错误", var12);
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("维度获取错误");
               return this.dimensionResponse;
            }

            this.dimensionResponse = new DimensionResponse();
            List<DimensionPersonal> dpList = this.dimensionDb.searchAllDimensionPersonal(request.getDimensionName(), tableName);
            List<HashMap<String, Object>> resultList = new ArrayList<>();

            for (DimensionPersonal dp : dpList) {
               HashMap<String, Object> resultHm = new HashMap<>();
               List<String> dimensionValues = new ArrayList<>();
               Set<DimensionPersonalValue> dps = dp.getDimensionValueSet();
               if (dps != null && dps.size() > 0) {
                  for (DimensionPersonalValue dpv : dps) {
                     dimensionValues.add(dpv.getDimensionName());
                  }
               }

               resultHm.put("dimensionId", dp.getDimensionId());
               resultHm.put("dimensionName", dp.getDimensionName());
               resultHm.put("dimensionAnotherName", dp.getDimensionAnotherName());
               resultHm.put("dimensionType", dp.getDimensionType());
               resultHm.put("dimensionValues", dimensionValues);
               resultHm.put("dimensionDataType", "string");
               resultHm.put("analysis", 1);
               resultList.add(resultHm);
            }

            this.dimensionResponse.setSuccessful(true);
            this.dimensionResponse.setMessage("查询成功");
            this.dimensionResponse.setValue(resultList);
            this.logger.info("success search personal dimension");
            return this.dimensionResponse;
         } else {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("数据源为空");
            return this.dimensionResponse;
         }
      } catch (Exception var13) {
         this.logger.info("查询自定义维度异常", var13);
         throw new ViePlatformServiceException("查询自定义维度异常", var13);
      }
   }

   public DimensionResponse updatePersonalDimensionService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start add personalDimension");
         this.dimensionResponse = new DimensionResponse();
         if (request.getDimensionId() == null) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度ID为空");
            return this.dimensionResponse;
         } else if (StringUtils.isNullOrEmpry(request.getDimensionName()) || "null".equals(request.getDimensionName())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度名称为空");
            return this.dimensionResponse;
         } else if (!StringUtils.isNullOrEmpry(request.getDataSource()) && !"null".equals(request.getDataSource())) {
            String tableName = "";

            try {
               tableName = request.getDataSource();
            } catch (Exception var14) {
               this.logger.error("维度获取错误", var14);
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("维度获取错误");
               return this.dimensionResponse;
            }

            boolean isSame = this.dimensionDb.isHaveSaveNamePersonal(request.getDimensionName(), request.getDimensionId(), tableName);
            if (isSame) {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("已存在相同名称的自定义维度");
               return this.dimensionResponse;
            } else {
               String dataSourceName = request.getDataSource();
               boolean isEqualSystemName = this.dimensionDb.isHaveSameName(request.getDimensionName(), -1L, dataSourceName);
               if (isEqualSystemName) {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("已存在相同名称的系统维度");
                  return this.dimensionResponse;
               } else {
                  String regEx = "[`~@$%^*={}':;',\\[\\].<>/?~！!&@￥……*（）——{}【】‘；：”“'。，、？]";
                  Pattern p = Pattern.compile(regEx);
                  Matcher m = p.matcher(request.getDimensionName());
                  if (!StringUtils.isNullOrEmpry(request.getDimensionName()) && !m.find() && request.getDimensionName().length() <= 20) {
                     this.logger.info("search pd");
                     DimensionPersonal dp = this.dimensionDb.searchPersonalDimension(request.getDimensionId(), tableName);
                     if (dp == null) {
                        this.dimensionResponse.setSuccessful(false);
                        this.dimensionResponse.setMessage("更新的维度不存在");
                        return this.dimensionResponse;
                     } else {
                        dp.setDimensionName(request.getDimensionName());
                        dp.setDimensionType(request.getDimensionType());
                        dp.setUpdateTime(this.sdf.format(new Date()));
                        this.logger.info("update pd");
                        this.dimensionDb.updateDimensionPersonal(dp);
                        this.logger.info("delete pd value");
                        this.dimensionDb.deleteDimensionValue(dp.getDimensionId());
                        List<DimensionPersonalValue> dimensionValues = request.dimensionPersonalValues;
                        List<DimensionPersonalValue> valueList = new ArrayList<>();

                        for (DimensionPersonalValue dpv : dimensionValues) {
                           dpv.setDimensionPersonal(dp);
                           valueList.add(dpv);
                        }

                        this.logger.info("save pd value");
                        this.dimensionDb.saveDimensionValue(valueList);
                        this.dimensionResponse.setSuccessful(true);
                        this.dimensionResponse.setMessage("更新成功");
                        return this.dimensionResponse;
                     }
                  } else {
                     this.dimensionResponse.setSuccessful(false);
                     this.dimensionResponse.setMessage("维度名称包含特殊字符或长度大于20");
                     return this.dimensionResponse;
                  }
               }
            }
         } else {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("数据源为空");
            return this.dimensionResponse;
         }
      } catch (Exception var15) {
         this.logger.info("更新自定义维度异常", var15);
         throw new ViePlatformServiceException("更新自定义维度异常", var15);
      }
   }

   public DimensionResponse deletePersonalDimensionService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start delete personal dimension");
         if (request.getDimensionId() == null) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度ID为空");
            return this.dimensionResponse;
         } else if (!StringUtils.isNullOrEmpry(request.getDataSource()) && !"null".equals(request.getDataSource())) {
            String dataSourceName = request.getDataSource();
            String tableName = "";

            try {
               tableName = dataSourceName;
            } catch (Exception var5) {
               this.logger.error("数据源获取错误", var5);
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("数据源获取错误");
               return this.dimensionResponse;
            }

            DimensionPersonal dp = this.dimensionDb.searchPersonalDimension(request.getDimensionId(), tableName);
            if (dp == null) {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("待删除的维度不存在");
               return this.dimensionResponse;
            } else {
               dp.setDimensionState(1);
               this.dimensionDb.updateDimensionPersonal(dp);
               this.dimensionResponse.setSuccessful(true);
               this.dimensionResponse.setMessage("删除成功");
               this.logger.info("success delete personal dimension");
               return this.dimensionResponse;
            }
         } else {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("数据源为空");
            return this.dimensionResponse;
         }
      } catch (Exception var6) {
         this.logger.info("删除自定义维度错误", var6);
         throw new ViePlatformServiceException("删除自定义维度错误", var6);
      }
   }

   public DimensionResponse addPersonalDimensionService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start add personalDimension");
         this.dimensionResponse = new DimensionResponse();
         if (StringUtils.isNullOrEmpry(request.getDimensionName())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度名称为空");
            return this.dimensionResponse;
         } else if (StringUtils.isNullOrEmpry(request.getDimensionType())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度类型为空");
            return this.dimensionResponse;
         } else if (!StringUtils.isNullOrEmpry(request.getDataSource()) && !"null".equals(request.getDataSource())) {
            String dataSourceName = request.getDataSource();
            String tableName = "";

            try {
               tableName = dataSourceName;
            } catch (Exception var19) {
               this.logger.error("维度获取错误", var19);
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("维度获取错误");
               return this.dimensionResponse;
            }

            boolean isSame = this.dimensionDb.isHaveSaveNamePersonal(request.getDimensionName(), 0L, tableName);
            if (isSame) {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("已存在相同名称的自定义维度");
               return this.dimensionResponse;
            } else {
               String regEx = "[`~@$%^*={}':;',\\[\\].<>/?~！!&@￥……*（）——{}【】‘；：”“'。，、？]";
               Pattern p = Pattern.compile(regEx);
               Matcher m = p.matcher(request.getDimensionName());
               if (!StringUtils.isNullOrEmpry(request.getDimensionName()) && !m.find() && request.getDimensionName().length() <= 20) {
                  boolean isEqualSystemName = this.dimensionDb.isHaveSameName(request.getDimensionName(), -1L, dataSourceName);
                  if (isEqualSystemName) {
                     this.dimensionResponse.setSuccessful(false);
                     this.dimensionResponse.setMessage("已存在相同名称的系统维度");
                     return this.dimensionResponse;
                  } else {
                     DimensionPersonal dp = new DimensionPersonal();
                     dp.setDimensionName(request.getDimensionName());
                     dp.setDimensionType(request.getDimensionType());
                     dp.setCreateTime(this.sdf.format(new Date()));
                     dp.setUpdateTime(this.sdf.format(new Date()));
                     dp.setDataSource(tableName);
                     DimensionPersonal dp2 = this.dimensionDb.addDimensionPersonal(dp);
                     if (dp2.getDimensionId() == 0L) {
                        this.dimensionResponse.setSuccessful(false);
                        this.dimensionResponse.setMessage("添加失败");
                        return this.dimensionResponse;
                     } else {
                        this.logger.info("save dimension success " + dp2.getDimensionId());
                        String dimensionField = "dimension" + dp2.getDimensionId();
                        dp2.setDimensionAnotherName(dimensionField);
                        this.dimensionDb.updateDimensionPersonal(dp2);
                        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + dimensionField + " string index 'not_analyzed' store 'yes'";
                        this.logger.info("the add sql is:" + sql);
                        String url = DynamicEsSource.getEsSourceByType(dataSourceName, "mainAnydrillAddress");
                        boolean isADD = this.getExcuteContext().executeDDL(url, sql);
                        if (!isADD) {
                           this.dimensionResponse.setSuccessful(false);
                           this.dimensionResponse.setMessage("添加索引失败");
                           this.dimensionDb.deleteDimensionPersonal(dp2);
                           return this.dimensionResponse;
                        } else {
                           List<DimensionPersonalValue> dimensionValues = request.getDimensionPersonalValues();
                           List<DimensionPersonalValue> valueList = new ArrayList<>();

                           for (DimensionPersonalValue dpv : dimensionValues) {
                              dpv.setDimensionPersonal(dp2);
                              valueList.add(dpv);
                           }

                           this.dimensionDb.saveDimensionValue(valueList);
                           this.dimensionResponse.setSuccessful(true);
                           this.dimensionResponse.setMessage("保存成功");
                           this.dimensionResponse.setValue(dp2.getDimensionId());
                           this.logger.info("success save dimension");
                           return this.dimensionResponse;
                        }
                     }
                  }
               } else {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("维度名称包含特殊字符或长度大于20");
                  return this.dimensionResponse;
               }
            }
         } else {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("数据源为空");
            return this.dimensionResponse;
         }
      } catch (Exception var20) {
         this.logger.error("添加自定义维度错误", var20);
         throw new ViePlatformServiceException("添加自定义维度错误", var20);
      }
   }

   public DimensionResponse importPersonalDimensionService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start import personal dimension");
         this.dimensionResponse = new DimensionResponse();
         if (request.getImportFile() == null) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("导入文件为空");
            return this.dimensionResponse;
         } else if (!StringUtils.isNullOrEmpry(request.getDataSource()) && !"null".equals(request.getDataSource())) {
            List<HashMap<String, Object>> excelList = this.getExcelToList(request.getImportFile());
            String nameSpecialStr = "[`~!@#$%^&*()=|{}':;',[].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]";
            String valueSpecialStr = "[`~!@#$%^&*()=|{}':;',[]<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]";
            String tableName = "";

            try {
               tableName = request.getDataSource();
            } catch (Exception var12) {
               this.logger.error("维度获取错误", var12);
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("维度获取错误");
               return this.dimensionResponse;
            }

            try {
               String condition = this.getTotalSql(excelList, request.getDataSource());
               PreparedStatement pst = new PreparedStatement(this.getExcuteContext().parseHost(LoadCommonConfig.get("mainAnydrillAddress")));
               String sql = "select sum(idCounter) as hitCount from " + tableName + " where " + condition;
               ResultSet rs = pst.executeQuery(sql);
               double count = 0.0;

               while (rs.hasRow()) {
                  rs.next();
                  if (rs.getValue("hitCount") != null && !"".equals(rs.getValue("hitCount"))) {
                     count = Double.parseDouble(rs.getValue("hitCount").toString());
                  }
               }

               if (count > 500000.0) {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("导入的数据大于50万");
                  return this.dimensionResponse;
               }
            } catch (Exception var14) {
               this.logger.error("条件错误", var14);
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("条件错误");
               return this.dimensionResponse;
            }

            List<HashMap<String, Object>> resultList = this.dealExcelList(excelList, request.getDataSource());
            String isHaveSpecial = this.isHaveSpecialStr(nameSpecialStr, valueSpecialStr, resultList);
            if (!StringUtils.isNotNullAndEmpry(isHaveSpecial)) {
               this.logger.info("the condition is true");
               String userName = "";

               try {
                  Map<Integer, Map<String, String>> usersInfo = this.userService.queryAllUserInfo(request.getDataSource());
                  this.logger.info("all the users info is : " + JSON.toJSONString(usersInfo));
                  Long userid = Long.parseLong(request.getUserId());
                  Map<String, String> userInfo = usersInfo.get(userid.intValue());
                  if (userInfo != null) {
                     userName = userInfo.get("accountName");
                  }
               } catch (Exception var13) {
                  this.logger.info("get userinfo from permission error");
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("从权限系统查询用户信息错误");
                  return this.dimensionResponse;
               }

               this.logger.info("success get userinfo from permission");
               String message = this.saveDimension(resultList, tableName, request.getImportFile(), userName, request.getDataSource());
               if ("false".equals(message)) {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("存储到数据库失败");
                  return this.dimensionResponse;
               } else if ("same".equals(message)) {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("已存在相同的维度名称");
                  return this.dimensionResponse;
               } else if (message.contains("系统维度")) {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage(message);
                  return this.dimensionResponse;
               } else {
                  this.dimensionResponse.setSuccessful(true);
                  this.logger.info("success import personal dimension");
                  this.dimensionResponse.setMessage("导入成功");
                  return this.dimensionResponse;
               }
            } else if (!isHaveSpecial.contains("有重复值") && !isHaveSpecial.contains("超过20") && !isHaveSpecial.contains("为空")) {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("第" + (int)Double.parseDouble(isHaveSpecial) + "行存在特殊字符");
               return this.dimensionResponse;
            } else {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage(isHaveSpecial);
               return this.dimensionResponse;
            }
         } else {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("数据源为空");
            return this.dimensionResponse;
         }
      } catch (ViePlatformServiceException var15) {
         this.logger.info(var15.getMessage(), var15);
         throw var15;
      } catch (Exception var16) {
         this.logger.info("导入自定义维度异常", var16);
         throw new ViePlatformServiceException("导入自定义维度异常", var16);
      }
   }

   public DimensionResponse searchDimensionTaskService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search task");
         this.dimensionResponse = new DimensionResponse();
         if (request.getPageNum() < 1) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("页码错误");
            return this.dimensionResponse;
         } else {
            Map<String, Object> result = this.dimensionDb.getAllDimensionTask(request.getPageNum(), request.getPageSize());
            long count = (Long)result.get("totalSize");
            int totalCount = (int)count;
            int totalPage = totalCount % request.getPageSize() == 0 ? totalCount / request.getPageSize() : totalCount / request.getPageSize() + 1;
            List<DimensionTask> dimensionTasks = (List<DimensionTask>)result.get("tasks");
            List<HashMap<String, Object>> resultList = new ArrayList<>();
            HashMap<String, Object> resultHm = new HashMap<>();

            for (DimensionTask dt : dimensionTasks) {
               HashMap<String, Object> hm = new HashMap<>();
               hm.put("taskState", dt.getTaskState());
               hm.put("taskTime", dt.getCreateTime());
               hm.put("taskUser", dt.getCreateName());
               hm.put("taskMessage", dt.getMessage());
               hm.put("taskId", dt.getTaskId());
               resultList.add(hm);
            }

            resultHm.put("totalPages", totalPage);
            resultHm.put("totalRows", totalCount);
            resultHm.put("pageNum", request.getPageNum());
            resultHm.put("pageSize", request.getPageSize());
            resultHm.put("rows", resultList);
            this.dimensionResponse.setSuccessful(true);
            this.dimensionResponse.setMessage("查询成功");
            this.dimensionResponse.setValue(resultHm);
            this.logger.info("success search dimensionTask");
            return this.dimensionResponse;
         }
      } catch (Exception var13) {
         this.logger.info("查询导入维度任务异常", var13);
         throw new ViePlatformServiceException("导入自定义维度异常", var13);
      }
   }

   public DimensionResponse searchDimensionMessageService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search message");
         this.dimensionResponse = new DimensionResponse();
         if (request.getDimensionTaskId() == null) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度任务ID为空");
            return this.dimensionResponse;
         } else if (StringUtils.isNullOrEmpry(request.getSystemId()) || "null".equals(request.getSystemId())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("页面ID为空");
            return this.dimensionResponse;
         } else if (!StringUtils.isNullOrEmpry(request.getUserId()) && !"null".equals(request.getUserId())) {
            if (request.getPageNum() >= 1 && request.getPageSize() >= 1) {
               Map<String, Object> result = this.dimensionDb.getAllPersonalTasks(request.getDimensionTaskId(), request.getPageNum(), request.getPageSize());
               int totalCount = (Integer)result.get("totalSize");
               int totalPage = totalCount % request.getPageSize() == 0 ? totalCount / request.getPageSize() : totalCount / request.getPageSize() + 1;
               List<DimensionPersonalTask> dptList = (List<DimensionPersonalTask>)result.get("tasks");
               HashMap<String, Object> resutlHm = new HashMap<>();
               List<String> messageList = new ArrayList<>();

               for (DimensionPersonalTask dpt : dptList) {
                  String message = "序号" + (int)Double.parseDouble(dpt.getNum()) + "配置内容出错";
                  messageList.add(message);
               }

               resutlHm.put("pageNum", request.getPageNum());
               resutlHm.put("pageSize", request.getPageSize());
               resutlHm.put("totalPages", totalPage);
               resutlHm.put("totalRows", totalCount);
               resutlHm.put("rows", messageList);
               this.dimensionResponse.setSuccessful(true);
               this.dimensionResponse.setMessage("查询成功");
               this.dimensionResponse.setValue(resutlHm);
               return this.dimensionResponse;
            } else {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("页码或每页数量不在合理范围内");
               return this.dimensionResponse;
            }
         } else {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("用户ID为空");
            return this.dimensionResponse;
         }
      } catch (Exception var11) {
         this.logger.info("查询导入任务的错误信息异常", var11);
         throw new ViePlatformServiceException("导入自定义维度异常", var11);
      }
   }

   public DimensionResponse searchMulDimensionService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search meiju dimension");
         this.dimensionResponse = new DimensionResponse();
         if (!StringUtils.isNullOrEmpry(request.getDimensionType()) && !"null".equals(request.getDimensionType())) {
            if (!StringUtils.isNullOrEmpry(request.getDataSource()) && !"null".equals(request.getDataSource())) {
               String tableName = "";

               try {
                  tableName = request.getDataSource();
               } catch (Exception var14) {
                  this.logger.error("数据源获取错误", var14);
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("数据源获取错误");
                  return this.dimensionResponse;
               }

               List<DimensionPersonal> dpList = this.dimensionDb.selectDimensionByType(request.getDimensionType(), request.getDimensionName(), tableName);
               if (ListUtil.isNullOrEmpty(dpList)) {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("查询维度为空");
                  return this.dimensionResponse;
               } else {
                  List<HashMap<String, Object>> resultList = new ArrayList<>();
                  List<DimensionPersonal> list = new ArrayList<>();

                  for (DimensionPersonal dp : dpList) {
                     if (!list.contains(dp)) {
                        HashMap<String, Object> hm = new HashMap<>();
                        hm.put("dimensionName", dp.getDimensionName());
                        hm.put("dimensionId", dp.getDimensionId());
                        hm.put("dimensionAnother", dp.getDimensionAnotherName());
                        List<HashMap<String, Object>> valueList = new ArrayList<>();

                        for (DimensionPersonalValue dpv : dp.getDimensionValueSet()) {
                           HashMap<String, Object> hm2 = new HashMap<>();
                           hm2.put("valueName", dpv.getDimensionName());
                           hm2.put("valueId", dpv.getId());
                           hm2.put("valueColor", dpv.getDimensionColor());
                           hm2.put("valueIndex", dpv.getShowIndex());
                           valueList.add(hm2);
                        }

                        Collections.sort(valueList, new Comparator<HashMap<String, Object>>() {
                           public int compare(HashMap<String, Object> o1, HashMap<String, Object> o2) {
                              return Integer.compare(((Number)o1.get("valueIndex")).intValue(), ((Number)o2.get("valueIndex")).intValue());
                           }
                        });
                        hm.put("dimensionValue", valueList);
                        resultList.add(hm);
                        list.add(dp);
                     }
                  }

                  this.dimensionResponse.setSuccessful(true);
                  this.dimensionResponse.setMessage("查询成功");
                  this.dimensionResponse.setValue(resultList);
                  return this.dimensionResponse;
               }
            } else {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("数据源为空");
               return this.dimensionResponse;
            }
         } else {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度类型为空");
            return this.dimensionResponse;
         }
      } catch (Exception var15) {
         this.logger.info("查询所有的枚举型自定义维度异常", var15);
         throw new ViePlatformServiceException("导入自定义维度异常", var15);
      }
   }

   public DimensionResponse addDimensionToEsService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start add dimension to es");
         if (StringUtils.isNullOrEmpry(request.getTelephoneId()) || "null".equals(request.getTelephoneId())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("用户ID为空");
            return this.dimensionResponse;
         } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("数据源为空");
            return this.dimensionResponse;
         } else if (request.getDimensionId() == null) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度ID为空");
            return this.dimensionResponse;
         } else if (request.getDimensionValueId() == null) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("维度值ID为空");
            return this.dimensionResponse;
         } else {
            this.dimensionResponse = new DimensionResponse();
            DimensionPersonal dp = this.dimensionDb.getPersonalDimensionById(request.getDimensionId());
            if (dp == null) {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("维度不存在");
               return this.dimensionResponse;
            } else {
               DimensionPersonalValue dpv = this.dimensionDb.selectValueById(request.getDimensionValueId());
               if (dpv == null) {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("维度值不存在");
                  return this.dimensionResponse;
               } else {
                  String dimAnotherName = dp.getDimensionAnotherName();
                  String valueName = dpv.getDimensionName();
                  String tableName = request.getDataSource();
                  String sql = "update " + tableName + " set " + dimAnotherName + "=? where id=?";
                  Object[] valueObject = new Object[2];
                  if (request.getIsDelete() == 1) {
                     valueObject[0] = "";
                  } else {
                     if (request.getIsDelete() != 0) {
                        this.dimensionResponse.setSuccessful(false);
                        this.dimensionResponse.setMessage("操作类型不在合理范围内");
                        return this.dimensionResponse;
                     }

                     valueObject[0] = valueName;
                  }

                  valueObject[1] = request.getTelephoneId();
                  List<Object[]> list = new ArrayList<>();
                  list.add(valueObject);
                  String url = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress");
                  int num = this.getExcuteContext().executeDDLQuery(url, sql, list);
                  if (num == 0) {
                     this.dimensionResponse.setSuccessful(false);
                     this.dimensionResponse.setMessage("添加索引失败");
                     return this.dimensionResponse;
                  } else if (request.getIsDelete() == 1) {
                     this.logger.info("success delete dimension to es");
                     this.dimensionResponse.setSuccessful(true);
                     this.dimensionResponse.setMessage("删除索引维度成功");
                     return this.dimensionResponse;
                  } else {
                     this.logger.info("success add dimension to es");
                     this.dimensionResponse.setSuccessful(true);
                     this.dimensionResponse.setMessage("添加索引维度成功");
                     return this.dimensionResponse;
                  }
               }
            }
         }
      } catch (Exception var12) {
         this.logger.info("添加维度到索引异常", var12);
         throw new ViePlatformServiceException("导入自定义维度异常", var12);
      }
   }

   public DimensionResponse saveSelectDimensionService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start save select dimension");
         if (!StringUtils.isNullOrEmpry(request.getUserId()) && !"null".equals(request.getUserId())) {
            if (StringUtils.isNullOrEmpry(request.getDataSource())) {
               this.dimensionResponse.setSuccessful(false);
               this.dimensionResponse.setMessage("数据源为空");
               return this.dimensionResponse;
            } else {
               this.dimensionResponse = new DimensionResponse();
               String userName = "";
               List<Long> roidList = new ArrayList<>();

               try {
                  Map<Integer, Map<String, String>> usersInfo = this.userService.queryAllUserInfo(request.getDataSource());
                  this.logger.info("all the users info is : " + JSON.toJSONString(usersInfo));
                  int userid = Integer.parseInt(request.getUserId());
                  Map<String, String> userInfo = usersInfo.get(userid);
                  if (userInfo != null) {
                     userName = userInfo.get("accountName");
                  }

                  if (StringUtils.isNullOrEmpry(userName) || "null".equals(userName)) {
                     this.dimensionResponse.setSuccessful(false);
                     this.dimensionResponse.setMessage("用户信息查询为空");
                     return this.dimensionResponse;
                  }

                  UserInfoRequest userInfoRequest = new UserInfoRequest();
                  userInfoRequest.setAccountName(userName);
                  List<RoleInfo> roleInfoList = this.userService.findRolesByAccountName(userInfoRequest);
                  if (ListUtil.isNullOrEmpty(roleInfoList)) {
                     this.dimensionResponse.setSuccessful(false);
                     this.dimensionResponse.setMessage("获取用户角色列表为空");
                     return this.dimensionResponse;
                  }

                  for (RoleInfo ri : roleInfoList) {
                     roidList.add(ri.getId());
                  }
               } catch (Exception var11) {
                  this.logger.error("get userinfo from permission error", var11);
                  throw new ViePlatformServiceException("获取用户信息失败", var11);
               }

               if (ListUtil.isNullOrEmpty(request.getDimensionIds())) {
                  this.logger.info("the dimensionIds is null");
                  this.dimensionDb.deleteRoidDimension(roidList);
                  this.logger.info("success delete select dimension");
                  this.dimensionResponse.setSuccessful(true);
                  this.dimensionResponse.setMessage("保存成功");
               }

               List<Long> dimensionIds = request.getDimensionIds();
               List<DimensionPersonalClick> dpcList = new ArrayList<>();

               for (int i = 0; i < dimensionIds.size(); i++) {
                  DimensionPersonal dp = this.dimensionDb.getPersonalDimensionById(dimensionIds.get(i));
                  if (dp == null) {
                     this.dimensionResponse.setSuccessful(false);
                     this.dimensionResponse.setMessage("维度不存在");
                     return this.dimensionResponse;
                  }

                  for (int j = 0; j < roidList.size(); j++) {
                     DimensionPersonalClick dpc = new DimensionPersonalClick();
                     dpc.setDimensionAnoterName(dp.getDimensionAnotherName());
                     dpc.setRoleId(roidList.get(j));
                     dpcList.add(dpc);
                  }
               }

               this.dimensionDb.deleteRoidDimension(roidList);
               this.dimensionDb.saveDimensionClick(dpcList);
               this.logger.info("success save select dimension");
               this.dimensionResponse.setSuccessful(true);
               this.dimensionResponse.setMessage("保存成功");
               return this.dimensionResponse;
            }
         } else {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("用户ID为空");
            return this.dimensionResponse;
         }
      } catch (Exception var12) {
         this.logger.info("保存选择的维度异常", var12);
         throw new ViePlatformServiceException("保存选择的维度异常", var12);
      }
   }

   public DimensionResponse searchTelephoneDimensionService(DimensionRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search telephon dimension");
         this.dimensionResponse = new DimensionResponse();
         if (StringUtils.isNullOrEmpry(request.getTelephoneId())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("电话信息为空");
            return this.dimensionResponse;
         } else if (StringUtils.isNullOrEmpry(request.getUserId()) || "null".equals(request.getUserId())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("用户ID为空");
            return this.dimensionResponse;
         } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.dimensionResponse.setSuccessful(false);
            this.dimensionResponse.setMessage("数据源为空");
            return this.dimensionResponse;
         } else {
            this.dimensionResponse = new DimensionResponse();
            String userName = "";
            List<Long> roidList = new ArrayList<>();

            try {
               Map<Integer, Map<String, String>> usersInfo = this.userService.queryAllUserInfo(request.getDataSource());
               this.logger.info("all the users info is : " + JSON.toJSONString(usersInfo));
               int userid = Integer.parseInt(request.getUserId());
               Map<String, String> userInfo = usersInfo.get(userid);
               if (userInfo != null) {
                  userName = userInfo.get("accountName");
               }

               if (StringUtils.isNullOrEmpry(userName) || "null".equals(userName)) {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("用户信息查询为空");
                  return this.dimensionResponse;
               }

               UserInfoRequest userInfoRequest = new UserInfoRequest();
               userInfoRequest.setAccountName(userName);
               userInfoRequest.setDataSource(request.getDataSource());
               List<RoleInfo> roleInfoList = this.userService.findRolesByAccountName(userInfoRequest);
               if (ListUtil.isNullOrEmpty(roleInfoList)) {
                  this.dimensionResponse.setSuccessful(false);
                  this.dimensionResponse.setMessage("获取用户角色列表为空");
                  return this.dimensionResponse;
               }

               for (RoleInfo ri : roleInfoList) {
                  roidList.add(ri.getId());
               }
            } catch (Exception var26) {
               this.logger.error("get userinfo from permission error", var26);
               throw new ViePlatformServiceException("获取用户信息失败", var26);
            }

            String tableName = request.getDataSource();
            List<HashMap<String, Object>> resultList = new ArrayList<>();
            List<DimensionPersonalClick> dpcList = this.dimensionDb.getRoidDimension(roidList, tableName);
            if (ListUtil.isNullOrEmpty(dpcList)) {
               this.dimensionResponse.setSuccessful(true);
               this.dimensionResponse.setMessage("查询成功");
               this.dimensionResponse.setValue(resultList);
               this.logger.info("success search telephon dimension");
               return this.dimensionResponse;
            } else {
               String nameStr = "";
               List<String> nameList = new ArrayList<>();

               for (int i = 0; i < dpcList.size(); i++) {
                  if (!nameList.contains(dpcList.get(i).getDimensionAnoterName())) {
                     nameList.add(dpcList.get(i).getDimensionAnoterName());
                  }
               }

               for (int j = 0; j < nameList.size(); j++) {
                  if (j == nameList.size() - 1) {
                     nameStr = nameStr + nameList.get(j);
                  } else {
                     nameStr = nameStr + nameList.get(j) + ",";
                  }
               }

               if (!ListUtil.isNullOrEmpty(nameList)) {
                  List<DimensionPersonal> dpList = this.dimensionDb.getDimensionByAnoterName(nameList);
                  String[] anydrillHost = this.getExcuteContext().parseHost(LoadCommonConfig.get("mainAnydrillAddress"));
                  PreparedStatement pst = new PreparedStatement(anydrillHost);
                  String sql = "select " + nameStr + " from " + tableName + " where id='" + request.getTelephoneId() + "'";
                  ResultSet rs = pst.executeQuery(sql);
                  List<LinkedHashMap<String, Object>> dataMaps = rs.toList();
                  LinkedHashMap<String, Object> lh = dataMaps.get(0);

                  for (int ix = 0; ix < nameList.size(); ix++) {
                     String value = String.valueOf(lh.get(nameList.get(ix)));
                     if (!StringUtils.isNullOrEmpry(value) & !"null".equals(value)) {
                        for (DimensionPersonal dp : dpList) {
                           if (dp.getDimensionAnotherName().equals(nameList.get(ix))) {
                              HashMap<String, Object> hm = new HashMap<>();
                              List<HashMap<String, Object>> valueList = new ArrayList<>();
                              hm.put("dimensionName", dp.getDimensionName());
                              hm.put("dimensionId", dp.getDimensionId());

                              for (DimensionPersonalValue dpv : dp.getDimensionValueSet()) {
                                 HashMap<String, Object> hm2 = new HashMap<>();
                                 hm2.put("valueName", dpv.getDimensionName());
                                 hm2.put("valueId", dpv.getId());
                                 if (dpv.getDimensionName().equals(value)) {
                                    hm2.put("selected", 1);
                                 } else {
                                    hm2.put("selected", 0);
                                 }

                                 hm2.put("valueIndex", dpv.getShowIndex());
                                 hm2.put("valueColor", dpv.getDimensionColor());
                                 valueList.add(hm2);
                              }

                              Collections.sort(valueList, new Comparator<HashMap<String, Object>>() {
                                 public int compare(HashMap<String, Object> o1, HashMap<String, Object> o2) {
                                    return Integer.compare(((Number)o1.get("valueIndex")).intValue(), ((Number)o2.get("valueIndex")).intValue());
                                 }
                              });
                              hm.put("dimensionValue", valueList);
                              resultList.add(hm);
                           }
                        }
                     }
                  }
               }

               this.dimensionResponse.setSuccessful(true);
               this.dimensionResponse.setMessage("查询成功");
               this.dimensionResponse.setValue(resultList);
               this.logger.info("success search telephon dimension");
               return this.dimensionResponse;
            }
         }
      } catch (Exception var27) {
         this.logger.info("查询单个录音的自定义维度异常", var27);
         throw new ViePlatformServiceException("查询单个录音的自定义维度异常", var27);
      }
   }

   public DimensionResponse fetchAllDimensionService(DimensionRequest request) throws ViePlatformServiceException {
      this.logger.info("start fetchAllDimensionService dimension");
      if (StringUtils.isNullOrEmpry(request.getDataSource())) {
         this.logger.info("fetchAllDimensionService dataSource:" + request.getDataSource());
         throw new ViePlatformServiceException("数据源dataSource不能为空");
      } else if (StringUtils.isNullOrEmpry(request.getUserId()) || "null".equals(request.getUserId())) {
         this.logger.info("fetchAllDimensionService userId:" + request.getUserId());
         throw new ViePlatformServiceException("用户id不能为空");
      } else if (!StringUtils.isNullOrEmpry(request.getSystemId()) && !"null".equals(request.getSystemId())) {
         this.dimensionResponse = new DimensionResponse();

         try {
            String tableName = request.getDataSource();
            String keyword = StringUtils.isNullOrEmpry(request.getKeyword()) ? "" : request.getKeyword();
            String ds = request.getDataSource();
            this.logger.info("fetchAllDimensionService keyword:" + keyword);
            List<DimensionConfig> list = this.dimensionDb.searchSystemDim(ds, keyword);
            List<DimensionPersonal> dpList = this.dimensionDb.searchAllDP(keyword, tableName);
            HashMap<String, Set<String>> hs = this.commonData.getDataMapAuth(request.getUserId(), request.getSystemId(), request.getDataSource());
            this.dimensionResponse.setValue(this.convertDimensionDTOList(list, dpList, 0, hs, request.getUserId(), tableName, request.getDataTypeDimension()));
            this.dimensionResponse.setSuccessful(true);
            this.dimensionResponse.setMessage("查询成功");
            this.logger.info("success search fetchAllDimensionService dimension");
            return this.dimensionResponse;
         } catch (Exception var8) {
            this.logger.info("获取所有维度信息异常", var8);
            throw new ViePlatformServiceException("获取所有维度信息异常", var8);
         }
      } else {
         this.logger.info("fetchAllDimensionService systemId:" + request.getSystemId());
         throw new ViePlatformServiceException("系统systemId不能为空");
      }
   }

   private List<DimensionDTO> convertDimensionDTOList(
      List<DimensionConfig> list,
      List<DimensionPersonal> dpList,
      int isReport,
      HashMap<String, Set<String>> ss,
      String userId,
      String tableName,
      String dataTypeDimension
   ) throws PermissionServiceException {
      List<DimensionDTO> dimensionDTOs = new ArrayList<>();
      if (isReport == 1) {
         DimensionDTO configTime = new DimensionDTO();
         DimensionDTO model = new DimensionDTO();
         configTime.setType(DimType.timeDim.getName());
         configTime.setDataType("string");
         configTime.setKey("timeDim");
         configTime.setName("时间");
         List<Object> specDimValueList = new ArrayList<>();

         for (Entry<String, String> entry : Constant.dimTime.entrySet()) {
            SpecDimValue specDimValue = new SpecDimValue();
            specDimValue.setKey(entry.getKey());
            specDimValue.setValue(entry.getValue());
            specDimValueList.add(specDimValue);
         }

         configTime.setValue(specDimValueList);
         dimensionDTOs.add(configTime);
         model.setType(DimType.offLineTagId.getName());
         model.setKey(DimType.offLineTagId.getName());
         model.setName("模型");
         model.setDataType("string");
         new ArrayList();
         specDimValueList = this.getModelInfo(userId, tableName);
         model.setValue(specDimValueList);
         dimensionDTOs.add(model);
         DimensionDTO kwd = new DimensionDTO();
         kwd.setType(DimType.mulEqu.getName());
         kwd.setKey(CommonParams.keyword);
         kwd.setName("关键词");
         kwd.setDataType("string");
         dimensionDTOs.add(kwd);
      }

      if (isReport == 2) {
         DimensionDTO model = new DimensionDTO();
         List<Object> specDimValueList = new ArrayList<>();

         for (Entry<String, String> entry : Constant.dimTime.entrySet()) {
            SpecDimValue specDimValue = new SpecDimValue();
            specDimValue.setKey(entry.getKey());
            specDimValue.setValue(entry.getValue());
            specDimValueList.add(specDimValue);
         }

         model.setType(DimType.offLineTagId.getName());
         model.setKey(DimType.offLineTagId.getName());
         model.setName("模型");
         model.setDataType("string");
         new ArrayList();
         specDimValueList = this.getModelInfo(userId, tableName);
         model.setValue(specDimValueList);
         dimensionDTOs.add(model);
      }

      if (isReport == 3) {
         DimensionDTO model = new DimensionDTO();
         List<Object> specDimValueList = new ArrayList<>();

         for (Entry<String, String> entry : Constant.dimTime.entrySet()) {
            SpecDimValue specDimValue = new SpecDimValue();
            specDimValue.setKey(entry.getKey());
            specDimValue.setValue(entry.getValue());
            specDimValueList.add(specDimValue);
         }

         model.setType(DimType.offLineTagId.getName());
         model.setKey(DimType.offLineTagId.getName());
         model.setName("模型");
         model.setDataType("string");
         new ArrayList();
         specDimValueList = this.getModelInfo(userId, tableName);
         model.setValue(specDimValueList);
         dimensionDTOs.add(model);
      }

      for (DimensionConfig dimensionConfig : list) {
         DimensionDTO dimensionDTO = this.getDimensionDTO(dimensionConfig, ss, dataTypeDimension);
         dimensionDTOs.add(dimensionDTO);
      }

      for (DimensionPersonal dp : dpList) {
         DimensionDTO dimensionDTO = this.getDimensionDTO(dp, ss, dataTypeDimension);
         dimensionDTOs.add(dimensionDTO);
      }

      int flag = -1;
      if (isReport == 2 || isReport == 0) {
         for (int index = 0; index < dimensionDTOs.size(); index++) {
            if ("timeFormat".equals(dimensionDTOs.get(index).getKey())) {
               flag = index;
               break;
            }
         }

         if (flag != -1) {
            dimensionDTOs.remove(flag);
         }
      }

      return dimensionDTOs;
   }

   private DimensionDTO getDimensionDTO(DimensionConfig dimensionConfig, HashMap<String, Set<String>> hm, String dataTypeDimension) {
      List<Object> values = new ArrayList<>();
      DimensionDTO dimensionDTO = new DimensionDTO();
      dimensionDTO.setDataType(dimensionConfig.getDataType());
      dimensionDTO.setKey(dimensionConfig.getIndexField());
      dimensionDTO.setName(dimensionConfig.getShowName());
      dimensionDTO.setType(dimensionConfig.getShowType());
      Set<DimensionValue> dimensionValues = dimensionConfig.getValueList();
      String[] dts = dataTypeDimension.split(",");

      for (String dt : dts) {
         if (hm.containsKey(dimensionConfig.getIndexField()) && hm.containsKey(dt)) {
            Set<DimensionValue> dvs = new HashSet<>();

            for (String s : hm.get(dimensionConfig.getIndexField())) {
               DimensionValue dv = new DimensionValue();
               dv.setDimensionId(dimensionConfig.getId());
               dv.setDimValue(s);
               dvs.add(dv);
            }

            dimensionValues.clear();
            dimensionValues.addAll(dvs);
         } else if (dimensionConfig.getIndexField().equals(dt)) {
            dimensionValues.clear();
         }
      }

      for (DimensionValue dv : dimensionValues) {
         values.add(dv.getDimValue());
      }

      dimensionDTO.setValue(values);
      return dimensionDTO;
   }

   private DimensionDTO getDimensionDTO(DimensionPersonal dp, HashMap<String, Set<String>> hm, String dataTypeDimension) {
      List<Object> values = new ArrayList<>();
      DimensionDTO dimensionDTO = new DimensionDTO();
      dimensionDTO.setDataType("string");
      dimensionDTO.setKey(dp.getDimensionAnotherName());
      dimensionDTO.setName(dp.getDimensionName());
      dimensionDTO.setType(dp.getDimensionType());
      Set<DimensionPersonalValue> dpv = dp.getDimensionValueSet();
      String[] dts = dataTypeDimension.split(",");

      for (DimensionPersonalValue dv : dpv) {
         values.add(dv.getDimensionName());
      }

      dimensionDTO.setValue(values);
      return dimensionDTO;
   }

   public List<Object> getModelInfo(String userId, String dataSource) throws PermissionServiceException {
      ArrayList<Object> modelDTOList = new ArrayList<>();
      GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
      groupAuthRequest.setUserId(Long.parseLong(userId));
      groupAuthRequest.setGroupType(1);
      List<Long> groupId = this.groupService.queryGroupAuth(groupAuthRequest);
      List<Long> modelGroupIds = new ArrayList<>();

      try {
         modelGroupIds = this.modelGroupDB.queryAllModelGroupIdsByPermission(groupId, dataSource);
      } catch (Exception var19) {
         this.logger.error("error", var19);
      }

      if (modelGroupIds != null && modelGroupIds.size() != 0) {
         List<ModelInfo> modelInfoList = this.modelDB.queryAllModels(dataSource, ModelStatus.OnLine, modelGroupIds);
         HashMap<Long, HashMap<String, List<ModelInfo>>> hashMap = new HashMap<>();

         for (ModelInfo modelInfo : modelInfoList) {
            long modelGroupId = modelInfo.getModelGroup().getGroupId();
            if (hashMap.containsKey(modelGroupId)) {
               HashMap<String, List<ModelInfo>> storedGroupHash = hashMap.get(modelGroupId);
               List<ModelInfo> storedList = storedGroupHash.get(modelInfo.getModelGroup().getGroupName());
               storedList.add(modelInfo);
            } else {
               List<ModelInfo> unStoredList = new ArrayList<>();
               unStoredList.add(modelInfo);
               HashMap<String, List<ModelInfo>> groupHash = new HashMap<>();
               groupHash.put(modelInfo.getModelGroup().getGroupName(), unStoredList);
               hashMap.put(modelGroupId, groupHash);
            }
         }

         for (Entry<Long, HashMap<String, List<ModelInfo>>> entry : hashMap.entrySet()) {
            ModelDTO modelDTO = new ModelDTO();
            modelDTO.setModelGroupId(entry.getKey());
            HashMap<String, List<ModelInfo>> tmp = entry.getValue();

            for (Entry<String, List<ModelInfo>> entryTmp : tmp.entrySet()) {
               modelDTO.setModelGroup(entryTmp.getKey());
               List<SpecDimValue> specDimValues = new ArrayList<>();

               for (ModelInfo m : entryTmp.getValue()) {
                  SpecDimValue specDimValue = new SpecDimValue();
                  specDimValue.setKey(m.getModelId() + "");
                  specDimValue.setValue(m.getModelName());
                  specDimValues.add(specDimValue);
               }

               modelDTO.setValue(specDimValues);
            }

            if (modelDTO.getValue().size() > 0) {
               modelDTOList.add(modelDTO);
            }
         }
      }

      return modelDTOList;
   }

   private String getTotalSql(List<HashMap<String, Object>> excelList, String dataSource) throws Exception {
      String condition = "";
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

      for (int i = 0; i < excelList.size(); i++) {
         HashMap<String, Object> hm = excelList.get(i);
         String con = hm.get("condition").toString();
         String[] conArr = null;
         boolean has = true;
         if (con.contains("|")) {
            has = false;
         }

         if (has) {
            conArr = con.split("&");
         } else {
            conArr = con.split("|");
         }

         String dimension = "";
         String[] dArr = null;
         String cond = "";

         for (int j = 0; j < conArr.length; j++) {
            if (conArr[j].contains("timestamp")) {
               dArr = conArr[j].split(">=|<=|<|>|=");
               Date date = sdf.parse(dArr[1].trim());
               if (conArr[j].contains(">=")) {
                  cond = cond + this.getStr(j, conArr.length, dArr[0] + " >= " + date.getTime(), has);
               } else if (conArr[j].contains(">")) {
                  Calendar ca = Calendar.getInstance();
                  ca.setTime(date);
                  ca.add(6, 1);
                  cond = cond + this.getStr(j, conArr.length, dArr[0] + " > " + ca.getTimeInMillis(), has);
               } else if (conArr[j].contains("<=")) {
                  Calendar ca = Calendar.getInstance();
                  ca.setTime(date);
                  ca.add(6, 1);
                  cond = cond + this.getStr(j, conArr.length, dArr[0] + " < " + ca.getTimeInMillis(), has);
               } else if (conArr[j].contains("<")) {
                  cond = cond + this.getStr(j, conArr.length, dArr[0] + " < " + date.getTime(), has);
               } else if (conArr[j].contains("=")) {
                  Calendar ca = Calendar.getInstance();
                  ca.setTime(date);
                  ca.add(6, 1);
                  cond = cond + this.getStr(j, conArr.length, dArr[0] + " >= " + date.getTime(), has);
                  cond = cond + this.getStr(j, conArr.length, dArr[0] + " < " + ca.getTimeInMillis(), has);
               }
            } else {
               dArr = conArr[j].split(">=|<=|<|>|=");
               DimensionConfig dc = this.dimensionDb.getValuesOfDim(dataSource, dArr[0].trim());
               String type = dc.getDataType();
               if (type == null) {
                  type = "string";
               }

               String str = "";
               if ("string".equals(type)) {
                  str = this.dealString(dArr[1].trim());
                  cond = cond + this.getStr(j, conArr.length, dArr[0] + " in (" + str + ")", has);
               } else {
                  str = this.dealInteger(conArr[j]);
                  cond = cond + this.getStr(j, conArr.length, str, has);
               }
            }
         }

         excelList.get(i).put("trueCondition", cond);
         condition = condition + this.getStr(i, excelList.size(), "(" + cond + ")", false);
      }

      return condition;
   }

   private String getStr(int len, int arrLen, String str, boolean has) {
      String ss = "";
      if (len == arrLen - 1) {
         ss = str;
      } else if (has) {
         ss = str + " and ";
      } else {
         ss = str + " or ";
      }

      return ss;
   }

   private List<HashMap<String, Object>> getExcelToList(File file) throws Exception {
      FileInputStream inputStream = new FileInputStream(file);
      XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
      XSSFSheet sheet = workbook.getSheetAt(0);
      int rowSize = 0;
      XSSFRow row = null;
      XSSFCell cell = null;
      List<HashMap<String, Object>> excelList = new ArrayList<>();

      for (int i = 1; i < sheet.getLastRowNum() + 1; i++) {
         row = sheet.getRow(i);
         if (row != null) {
            int tempRowSize = row.getLastCellNum() + 1;
            if (tempRowSize > rowSize) {
               rowSize = tempRowSize;
            }

            HashMap<String, Object> cellHm = new HashMap<>();

            for (int j = 0; j < 4; j++) {
               cell = row.getCell(j);
               if (cell == null) {
                  throw new ViePlatformServiceException("第" + ++i + "行,第" + ++j + "列不能为空");
               }

               switch (j) {
                  case 0:
                     if (cell.getCellType() == 0) {
                        cellHm.put("num", cell.getNumericCellValue());
                     } else {
                        cellHm.put("num", cell.getStringCellValue());
                     }
                     break;
                  case 1:
                     cellHm.put("condition", cell.getStringCellValue());
                     break;
                  case 2:
                     cellHm.put("dimensionName", cell.getStringCellValue());
                     break;
                  case 3:
                     if (cell.getCellType() == 0) {
                        cellHm.put("dimensionValueName", cell.getNumericCellValue());
                     } else {
                        cellHm.put("dimensionValueName", cell.getStringCellValue());
                     }
               }
            }

            cellHm.put("index", row.getRowNum());
            excelList.add(cellHm);
         }
      }

      return excelList;
   }

   private List<HashMap<String, Object>> dealExcelList(List<HashMap<String, Object>> excelList, String dataSource) {
      List<String> strList = new ArrayList<>();
      List<HashMap<String, Object>> resultList = new ArrayList<>();

      for (int i = 0; i < excelList.size(); i++) {
         HashMap<String, Object> hm = excelList.get(i);
         HashMap<String, Object> hm2 = new HashMap<>();
         String anotherName = dataSource + new Date().getTime();
         String dimensionName = hm.get("dimensionName").toString();
         if (strList.contains(dimensionName)) {
            excelList.get(i).put("dimensionAnotherName", anotherName);
         } else {
            strList.add(dimensionName);
            excelList.get(i).put("dimensionAnotherName", anotherName);
            hm2.put("dimensionName", dimensionName);
            hm2.put("dimensionAnotherName", anotherName);
            hm2.put("num", hm.get("num"));
            List<HashMap<String, Object>> valueList = new ArrayList<>();

            for (int j = 0; j < excelList.size(); j++) {
               HashMap<String, Object> hm3 = new HashMap<>();
               HashMap<String, Object> hm4 = excelList.get(j);
               String dimensionName2 = hm4.get("dimensionName").toString();
               if (dimensionName.equals(dimensionName2)) {
                  hm3.put("dimensionName", hm4.get("dimensionValueName").toString());
                  hm3.put("showIndex", hm4.get("index").toString());
                  hm3.put("num", hm4.get("num"));
                  hm3.put("condition", hm4.get("condition"));
                  hm3.put("trueCondition", hm4.get("trueCondition"));
                  valueList.add(hm3);
               }
            }

            hm2.put("dimensionValue", valueList);
            resultList.add(hm2);
         }
      }

      return resultList;
   }

   private String dealString(String str) {
      String[] strArr = str.split(",");
      String result = "";

      for (int i = 0; i < strArr.length; i++) {
         if (i == strArr.length - 1) {
            result = result + "'" + strArr[i] + "'";
         } else {
            result = result + "'" + strArr[i] + "',";
         }
      }

      return result;
   }

   private String dealInteger(String str) {
      str = StringUtils.removeBlank(str);
      String[] strArr = str.split(">=|<=|>|<|=");
      String result = "";
      int num = Integer.parseInt(strArr[1].trim()) * 1000;
      if (str.contains(">=")) {
         if (num == 0) {
            result = strArr[0] + ">=" + (num - 500);
         } else {
            result = strArr[0] + ">=" + (num - 500);
         }
      } else if (str.contains(">")) {
         if (num == 0) {
            result = strArr[0] + ">" + 500;
         } else {
            result = strArr[0] + ">" + (num - 500);
         }
      } else if (str.contains("<=")) {
         if (num == 0) {
            result = strArr[0] + "<" + 500;
         } else {
            result = strArr[0] + "<" + (num + 500);
         }
      } else if (str.contains("<")) {
         if (num == 0) {
            result = strArr[0] + "<" + 500;
         } else {
            result = strArr[0] + "<" + (num + 500);
         }
      } else if (num == 0) {
         result = strArr[0] + " > " + 0 + " and " + strArr[0] + " < " + 500;
      } else {
         result = strArr[0] + " >= " + (num - 500) + " and " + strArr[0] + " < " + (num + 500);
      }

      return result;
   }

   private String isHaveSpecialStr(String dimensionStr, String valueStr, List<HashMap<String, Object>> list) throws Exception {
      String result = "";

      for (HashMap<String, Object> hm : (List<HashMap<String, Object>>)list) {
         String dimensionName = String.valueOf(hm.get("dimensionName"));
         if (StringUtils.isNullOrEmpry(dimensionName)) {
            return "第" + (int)Double.parseDouble(String.valueOf(hm.get("num"))) + "行,维度名称为空";
         }

         if (dimensionName.getBytes("GBK").length > 20) {
            return "第" + (int)Double.parseDouble(String.valueOf(hm.get("num"))) + "行,维度名称超过20个字符";
         }

         String[] strArr = dimensionStr.split("");
         String[] strArr2 = valueStr.split("");

         for (String str : strArr) {
            if (!StringUtils.isNullOrEmpry(str) && dimensionName.contains(str)) {
               return hm.get("num").toString();
            }
         }

         List<HashMap<String, Object>> valueList = (List<HashMap<String, Object>>)hm.get("dimensionValue");
         List<String> dValue = new ArrayList<>();

         for (HashMap<String, Object> hm2 : (List<HashMap<String, Object>>)valueList) {
            String valueName = String.valueOf(hm2.get("dimensionName"));
            if (StringUtils.isNullOrEmpry(valueName)) {
               return "第" + (int)Double.parseDouble(String.valueOf(hm.get("num"))) + "行,维度值为空";
            }

            if (valueName.getBytes("GBK").length > 20) {
               return "第" + (int)Double.parseDouble(String.valueOf(hm.get("num"))) + "行,枚举项名称超过20个字符";
            }

            if (dValue.contains(valueName)) {
               return "第" + (int)Double.parseDouble(String.valueOf(hm2.get("num"))) + "行,有重复值" + valueName;
            }

            dValue.add(valueName);

            for (String strx : strArr2) {
               if (!StringUtils.isNullOrEmpry(strx) && valueName.contains(strx)) {
                  return String.valueOf(hm2.get("num"));
               }
            }
         }
      }

      return result;
   }

   private String saveDimension(List<HashMap<String, Object>> resultList, String tableName, File importFile, String userName, String dataSource) throws Exception {
      try {
         String dimensionField = "";
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

         for (int i = 0; i < resultList.size(); i++) {
            HashMap<String, Object> hm = resultList.get(i);
            DimensionPersonal dp = new DimensionPersonal();
            boolean isHaveSame = this.dimensionDb.isHaveSaveName(hm.get("dimensionName").toString(), 0L, tableName);
            boolean isEqualSystemName = this.dimensionDb.isHaveSameName(hm.get("dimensionName").toString(), -1L, dataSource);
            if (isEqualSystemName) {
               return hm.get("dimensionName").toString() + " 存在相同名称的系统维度";
            }

            if (isHaveSame) {
               dp = this.dimensionDb.getpersonDimensionByName(hm.get("dimensionName").toString(), tableName);
               dimensionField = dp.getDimensionAnotherName();
               Set<DimensionPersonalValue> dpvSet = dp.getDimensionValueSet();
               if (dpvSet != null) {
                  String fileName = importFile.getName();
                  DimensionTask dimensionTask = new DimensionTask();
                  dimensionTask.setTaskName(fileName);
                  dimensionTask.setTaskState(0);
                  dimensionTask.setCreateName(userName);
                  dimensionTask.setCreateTime(sdf.format(new Date()));
                  DimensionTask dt = this.dimensionDb.saveDimensionTask(dimensionTask);
                  List<HashMap<String, Object>> valueList = (List<HashMap<String, Object>>)hm.get("dimensionValue");
                  List<DimensionPersonalValue> dpvList = new ArrayList<>();
                  List<DimensionPersonalTask> dptList = new ArrayList<>();

                  for (int j = 0; j < valueList.size(); j++) {
                     HashMap<String, Object> hmValue = valueList.get(j);
                     Iterator<DimensionPersonalValue> iterator = dpvSet.iterator();
                     boolean isHave = false;

                     while (iterator.hasNext()) {
                        DimensionPersonalValue dpv = iterator.next();
                        if (dpv.getDimensionName().equals(hmValue.get("dimensionName").toString())) {
                           isHave = true;
                        }
                     }

                     if (!isHave) {
                        DimensionPersonalValue dpv = new DimensionPersonalValue();
                        dpv.setDimensionName(hmValue.get("dimensionName").toString());
                        dpv.setDimensionColor("red");
                        dpv.setShowIndex(Integer.parseInt(hmValue.get("showIndex").toString()));
                        dpv.setDimensionPersonal(dp);
                        dpvList.add(dpv);
                        DimensionPersonalTask dpt = new DimensionPersonalTask();
                        dpt.setTaskName(UUID.randomUUID().toString().replace("-", ""));
                        dpt.setDimensionAnotherName(dimensionField);
                        dpt.setDimensionValueName(hmValue.get("dimensionName").toString());
                        dpt.setOriginalCondition(hmValue.get("condition").toString());
                        dpt.setDimensionName(hm.get("dimensionName").toString());
                        dpt.setCondition(hmValue.get("trueCondition").toString());
                        dpt.setCreateTime(sdf.format(new Date()));
                        dpt.setNum(String.valueOf(hmValue.get("num")));
                        dpt.setDimensionTask(dt);
                        dptList.add(dpt);
                     } else {
                        DimensionPersonalTask dpt = new DimensionPersonalTask();
                        dpt.setTaskName(UUID.randomUUID().toString().replace("-", ""));
                        dpt.setDimensionAnotherName(dimensionField);
                        dpt.setDimensionValueName(hmValue.get("dimensionName").toString());
                        dpt.setOriginalCondition(hmValue.get("condition").toString());
                        dpt.setDimensionName(hm.get("dimensionName").toString());
                        dpt.setCondition(hmValue.get("trueCondition").toString());
                        dpt.setCreateTime(sdf.format(new Date()));
                        dpt.setNum(String.valueOf(hmValue.get("num")));
                        dpt.setDimensionTask(dt);
                        dptList.add(dpt);
                     }
                  }

                  this.dimensionDb.saveDimensionValue(dpvList);
                  this.dimensionDb.savePersonTaskList(dptList);
               }
            } else {
               dp.setDimensionName(hm.get("dimensionName").toString());
               dp.setDimensionType("mulSel");
               dp.setCreateTime(sdf.format(new Date()));
               dp.setDimensionState(0);
               dp.setDataSource(tableName);
               DimensionPersonal dp2 = this.dimensionDb.addDimensionPersonal(dp);
               dimensionField = "dimension" + dp2.getDimensionId();
               dp2.setDimensionAnotherName(dimensionField);
               this.dimensionDb.updatePersonalDimension(dp2);
               String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + dimensionField + " string index 'not_analyzed' store 'yes'";
               String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
               boolean isADD = this.getExcuteContext().executeDDL(url, sql);
               if (!isADD) {
                  this.logger.error("添加索引失败");
                  this.dimensionDb.deleteDimensionPersonal(dp2);
                  return "false";
               }

               String fileName = importFile.getName();
               DimensionTask dimensionTask = new DimensionTask();
               dimensionTask.setTaskName(fileName);
               dimensionTask.setTaskState(0);
               dimensionTask.setCreateName(userName);
               dimensionTask.setCreateTime(sdf.format(new Date()));
               DimensionTask dt = this.dimensionDb.saveDimensionTask(dimensionTask);
               List<HashMap<String, Object>> valueList = (List<HashMap<String, Object>>)hm.get("dimensionValue");
               List<DimensionPersonalValue> dpvList = new ArrayList<>();
               List<DimensionPersonalTask> dptList = new ArrayList<>();

               for (int j = 0; j < valueList.size(); j++) {
                  HashMap<String, Object> hmValue = valueList.get(j);
                  DimensionPersonalValue dpv = new DimensionPersonalValue();
                  dpv.setDimensionName(hmValue.get("dimensionName").toString());
                  dpv.setDimensionColor("red");
                  dpv.setShowIndex(Integer.parseInt(hmValue.get("showIndex").toString()));
                  dpv.setDimensionPersonal(dp2);
                  dpvList.add(dpv);
                  DimensionPersonalTask dpt = new DimensionPersonalTask();
                  dpt.setTaskName(UUID.randomUUID().toString().replace("-", ""));
                  dpt.setDimensionAnotherName(dimensionField);
                  dpt.setDimensionValueName(hmValue.get("dimensionName").toString());
                  dpt.setOriginalCondition(hmValue.get("condition").toString());
                  dpt.setDimensionName(hm.get("dimensionName").toString());
                  dpt.setCondition(hmValue.get("trueCondition").toString());
                  dpt.setCreateTime(sdf.format(new Date()));
                  dpt.setNum(String.valueOf(hmValue.get("num")));
                  dpt.setDimensionTask(dt);
                  dptList.add(dpt);
               }

               this.dimensionDb.saveDimensionValue(dpvList);
               this.dimensionDb.savePersonTaskList(dptList);
            }
         }

         return dimensionField;
      } catch (Exception var27) {
         var27.printStackTrace();
         return "false";
      }
   }

   public DimensionDBPlatform getDimensionDb() {
      return this.dimensionDb;
   }

   public void setDimensionDb(DimensionDBPlatform dimensionDb) {
      this.dimensionDb = dimensionDb;
   }

   public DimensionResponse getDimensionResponse() {
      return this.dimensionResponse;
   }

   public void setDimensionResponse(DimensionResponse dimensionResponse) {
      this.dimensionResponse = dimensionResponse;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public ModelDBPlatform getModelDB() {
      return this.modelDB;
   }

   public void setModelDB(ModelDBPlatform modelDB) {
      this.modelDB = modelDB;
   }

   public ExcuteContext getExcuteContext() {
      return this.excuteContext;
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
   }

   public CommonDataPlatform getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonDataPlatform commonData) {
      this.commonData = commonData;
   }

   public UserService getUserService() {
      return this.userService;
   }

   public void setUserService(UserService userService) {
      this.userService = userService;
   }

   public ModelGroupDBPlatform getModelGroupDB() {
      return this.modelGroupDB;
   }

   public void setModelGroupDB(ModelGroupDBPlatform modelGroupDB) {
      this.modelGroupDB = modelGroupDB;
   }

   public GroupAuthService getGroupService() {
      return this.groupService;
   }

   public void setGroupService(GroupAuthService groupService) {
      this.groupService = groupService;
   }
}
