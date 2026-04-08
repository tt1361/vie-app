package com.iflytek.vie.app.provider.impl.custom;

import com.alibaba.fastjson.JSON;
import com.iflytek.vie.app.api.custom.CustomHomePageService;
import com.iflytek.vie.app.api.model.ModelApplyService;
import com.iflytek.vie.app.api.permission.DataAuthService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.custom.CallTrendDimBeanDTO;
import com.iflytek.vie.app.pojo.custom.CallTrendDimRequest;
import com.iflytek.vie.app.pojo.custom.CallTrendDimResponse;
import com.iflytek.vie.app.pojo.custom.CustomHomePage;
import com.iflytek.vie.app.pojo.custom.CustomHomePageRequest;
import com.iflytek.vie.app.pojo.custom.HomePageRelateModule;
import com.iflytek.vie.app.pojo.custom.HomeTableDataRequest;
import com.iflytek.vie.app.pojo.custom.PageRelateModuleRequest;
import com.iflytek.vie.app.pojo.custom.PageRelateModuleResponse;
import com.iflytek.vie.app.pojo.datadrill.DataDetailResponse;
import com.iflytek.vie.app.pojo.permission.ContextRequest;
import com.iflytek.vie.app.pojo.permission.DataResourceAuth;
import com.iflytek.vie.app.pojo.permission.DimensionAuth;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.CustomHomePageDB;
import com.iflytek.vie.app.provider.impl.model.ModelApplyServiceImpl;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.ExceptionUtil;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.LoadCommonConfig;
import com.iflytek.vie.utils.SparkUtils;
import com.iflytek.vie.utils.StringUtils;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.sf.json.JSONArray;
import org.anydrill.calculate.set.ResultSet;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class CustomHomePageServiceImpl implements CustomHomePageService {
   private final Logger logger = LoggerFactory.getLogger(CustomHomePageServiceImpl.class);
   private CustomHomePageDB customHomePageDB;
   private String dimPro;
   private String dimProValue;
   private String dimName;
   private DataSourceInfo dataSourceInfo;
   private CommonData commonData;
   @Autowired
   private Environment env;
   private ExcuteContext excuteContext;
   private static final int DEFAULT_MAX_NUM = 10;
   private SparkUtils sparkUtils;
   private ModelApplyService modelApplyService;
   private DataAuthService dataAuthService;

   public ModelApplyService getModelApplyService() {
      return this.modelApplyService;
   }

   public void setModelApplyService(ModelApplyService modelApplyService) {
      this.modelApplyService = modelApplyService;
   }

   public SparkUtils getSparkUtils() {
      return this.sparkUtils;
   }

   public void setSparkUtils(SparkUtils sparkUtils) {
      this.sparkUtils = sparkUtils;
   }

   public DataAuthService getDataAuthService() {
      return this.dataAuthService;
   }

   public void setDataAuthService(DataAuthService dataAuthService) {
      this.dataAuthService = dataAuthService;
   }

   public Long saveCustomHomePage(CustomHomePageRequest customHomePageRequest) throws VieAppServiceException {
      Long pageId = null;
      if (customHomePageRequest != null) {
         try {
            int sortOrder = -1;
            List<String> keyList = new ArrayList<>();
            keyList.add("pageName");
            keyList.add("userId");
            String checkResult = this.checkRequestParamNull(customHomePageRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               String userId = customHomePageRequest.getUserId();
               String pageName = customHomePageRequest.getPageName();
               boolean nameExist = this.customHomePageDB.checkPageNameExist(userId, pageName, null, customHomePageRequest.getDataSource());
               if (nameExist) {
                  this.logger.error("首页名称重复");
                  throw new VieAppServiceException("首页名称重复！");
               } else {
                  int maxNum = customHomePageRequest.getMaxNum() != null ? customHomePageRequest.getMaxNum() : 10;
                  boolean overLimit = this.customHomePageDB.checkPageNumOverLimit(maxNum, userId, customHomePageRequest.getDataSource());
                  if (overLimit) {
                     this.logger.error("最多可创建{}个概览，已超最大限制", maxNum);
                     throw new VieAppServiceException("最多可创建" + maxNum + "个概览，已超最大限制！");
                  } else {
                     CustomHomePage homePage = new CustomHomePage();
                     homePage.setPageName(pageName);
                     homePage.setCreateUser(userId);
                     homePage.setCreateTime(new Date());
                     homePage.setSortOrder(sortOrder);
                     homePage.setDataSource(customHomePageRequest.getDataSource());
                     pageId = this.customHomePageDB.saveCustomHomePage(homePage);
                     if (pageId == null) {
                        this.logger.error("保存失败");
                        throw new VieAppServiceException("保存失败");
                     } else {
                        return pageId;
                     }
                  }
               }
            }
         } catch (VieAppServiceException var12) {
            throw new VieAppServiceException(var12.getMessage());
         } catch (Exception var13) {
            this.logger.error("saveCustomHomePage service has a error!", var13);
            throw new VieAppServiceException("服务内部错误", var13);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public boolean updateCustomHomePage(CustomHomePageRequest customHomePageRequest) throws VieAppServiceException {
      boolean flag = true;
      if (customHomePageRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("pageName");
            keyList.add("userId");
            keyList.add("pageId");
            String checkResult = this.checkRequestParamNull(customHomePageRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               String userId = customHomePageRequest.getUserId();
               String pageName = customHomePageRequest.getPageName();
               Long pageId = customHomePageRequest.getPageId();
               boolean nameExist = this.customHomePageDB.checkPageNameExist(userId, pageName, pageId, customHomePageRequest.getDataSource());
               if (nameExist) {
                  this.logger.error("首页名称重复！");
                  throw new VieAppServiceException("首页名称重复！");
               } else {
                  CustomHomePage homePage = new CustomHomePage();
                  homePage.setDataSource(customHomePageRequest.getDataSource());
                  homePage.setPageName(pageName);
                  homePage.setId(pageId);
                  return this.customHomePageDB.updateCustomHomePage(homePage);
               }
            }
         } catch (VieAppServiceException var10) {
            this.logger.error(var10.getMessage());
            throw new VieAppServiceException(var10.getMessage());
         } catch (Exception var11) {
            this.logger.error("updateCustomHomePage service has a error!", var11);
            throw new VieAppServiceException("服务内部错误", var11);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public boolean deleteCustomHomePage(CustomHomePageRequest request) throws VieAppServiceException {
      boolean flag = true;
      if (request != null && request.getPageId() != null) {
         try {
            return this.customHomePageDB.deleteCustomHomePage(request.getPageId());
         } catch (Exception var4) {
            this.logger.error("deleteCustomHomePage service has a error!", var4);
            throw new VieAppServiceException("服务内部错误", var4);
         }
      } else {
         this.logger.error("参数(pageId)为空");
         throw new VieAppServiceException("参数(pageId)为空");
      }
   }

   public List<CustomHomePage> queryCustomHomePageList(CustomHomePageRequest customHomePageRequest) throws VieAppServiceException {
      List<CustomHomePage> pageList = null;
      if (customHomePageRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("userId");
            String checkResult = this.checkRequestParamNull(customHomePageRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               String userId = customHomePageRequest.getUserId();
               pageList = this.customHomePageDB.queryCustomHomePageList(userId, customHomePageRequest.getDataSource());
               if (pageList == null) {
                  this.logger.error("服务内部错误");
                  throw new VieAppServiceException("服务内部错误");
               } else {
                  return pageList;
               }
            }
         } catch (VieAppServiceException var6) {
            this.logger.error(var6.getMessage());
            throw new VieAppServiceException(var6.getMessage());
         } catch (Exception var7) {
            this.logger.error("queryCustomHomePageList service has a error!", var7);
            throw new VieAppServiceException("服务内部错误", var7);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public Long savePageRelateModule(PageRelateModuleRequest pageRelateModuleRequest) throws VieAppServiceException {
      Long id = null;
      if (pageRelateModuleRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("userId");
            keyList.add("pageId");
            keyList.add("moduleName");
            keyList.add("moduleType");
            keyList.add("contentType");
            String checkResult = this.checkRequestParamNull(pageRelateModuleRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               String userId = pageRelateModuleRequest.getUserId();
               String moduleName = pageRelateModuleRequest.getModuleName();
               Long pageId = pageRelateModuleRequest.getPageId();
               boolean nameExist = this.customHomePageDB.checkModuleNameExist(userId, moduleName, null, pageId);
               if (nameExist) {
                  this.logger.error("名称重复！");
                  throw new VieAppServiceException("名称重复！");
               } else {
                  int maxNum = pageRelateModuleRequest.getMaxNum() != null ? pageRelateModuleRequest.getMaxNum() : 10;
                  boolean overLimit = this.customHomePageDB.checkPageModuleOverLimit(maxNum, pageId, null);
                  if (overLimit) {
                     this.logger.error("每个首页最多可绑定{}个图表，已超最大限制！", maxNum);
                     throw new VieAppServiceException("每个首页最多可绑定" + maxNum + "个图表，已超最大限制！");
                  } else {
                     HomePageRelateModule pageRelateModule = new HomePageRelateModule();
                     pageRelateModule.setCreateUser(userId);
                     this.setPageRelateModuleInfo(pageRelateModule, pageRelateModuleRequest);
                     if (pageRelateModule.getModuleType() == 4) {
                        if (pageRelateModule.getModuleId() == null || pageRelateModule.getModuleId() == 0L) {
                           this.logger.error("模块ID为空");
                           throw new VieAppServiceException("参数模块ID为空");
                        }

                        if (this.customHomePageDB.checkIsNotExist(pageRelateModule.getModuleId())) {
                           return null;
                        }
                     }

                     if (!this.customHomePageDB.savePageRelateModule(pageRelateModule)) {
                        this.logger.error("保存失败");
                        throw new VieAppServiceException("保存失败");
                     } else {
                        return pageRelateModule.getId();
                     }
                  }
               }
            }
         } catch (VieAppServiceException var12) {
            this.logger.error(var12.getMessage());
            throw new VieAppServiceException(var12.getMessage());
         } catch (Exception var13) {
            this.logger.error("savePageRelateModule service has a error!", var13);
            throw new VieAppServiceException("服务内部错误", var13);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public boolean updatePageRelateModule(PageRelateModuleRequest pageRelateModuleRequest) throws VieAppServiceException {
      boolean flag = true;
      if (pageRelateModuleRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("id");
            keyList.add("userId");
            keyList.add("pageId");
            keyList.add("moduleName");
            String checkResult = this.checkRequestParamNull(pageRelateModuleRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               Long id = pageRelateModuleRequest.getId();
               Long pageId = pageRelateModuleRequest.getPageId();
               String userId = pageRelateModuleRequest.getUserId();
               String moduleName = pageRelateModuleRequest.getModuleName();
               boolean nameExist = this.customHomePageDB.checkModuleNameExist(userId, moduleName, id, pageId);
               if (nameExist) {
                  this.logger.error("名称重复！");
                  throw new VieAppServiceException("名称重复！");
               } else {
                  int maxNum = pageRelateModuleRequest.getMaxNum() != null ? pageRelateModuleRequest.getMaxNum() : 10;
                  boolean overLimit = this.customHomePageDB.checkPageModuleOverLimit(maxNum, pageId, id);
                  if (overLimit) {
                     this.logger.error("每个首页最多可绑定{}个图表，已超最大限制！", maxNum);
                     throw new VieAppServiceException("每个首页最多可绑定" + maxNum + "个图表，已超最大限制！");
                  } else {
                     HomePageRelateModule pageRelateModule = new HomePageRelateModule();
                     pageRelateModule.setId(id);
                     pageRelateModule.setModuleName(moduleName);
                     pageRelateModule.setPageId(pageRelateModuleRequest.getPageId());
                     pageRelateModule.setContentParam(pageRelateModuleRequest.getContentParam());
                     pageRelateModule.setContentType(pageRelateModuleRequest.getContentType());
                     return this.customHomePageDB.updatePageRelateModule(pageRelateModule);
                  }
               }
            }
         } catch (VieAppServiceException var13) {
            this.logger.error(var13.getMessage());
            throw new VieAppServiceException(var13.getMessage());
         } catch (Exception var14) {
            this.logger.error("updatePageRelateModule service has a error!", var14);
            throw new VieAppServiceException("服务内部错误", var14);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public boolean deletePageRelateModule(HomePageRelateModule homePageRelateModule) throws VieAppServiceException {
      boolean flag = true;
      if (homePageRelateModule != null && homePageRelateModule.getId() != null) {
         try {
            return this.customHomePageDB.deletePageRelateModule(homePageRelateModule.getId());
         } catch (Exception var4) {
            this.logger.error("deletePageRelateModule service has a error!", var4);
            throw new VieAppServiceException("服务内部错误", var4);
         }
      } else {
         this.logger.error("参数(id)为空");
         throw new VieAppServiceException("参数(id)为空");
      }
   }

   public boolean deleteModuleFromHomePage(PageRelateModuleRequest pageRelateModuleRequest) throws VieAppServiceException {
      boolean flag = true;
      if (pageRelateModuleRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("moduleType");
            keyList.add("moduleIds");
            String checkResult = this.checkRequestParamNull(pageRelateModuleRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               return this.customHomePageDB.deleteModuleFromHomePage(pageRelateModuleRequest);
            }
         } catch (VieAppServiceException var5) {
            this.logger.error(var5.getMessage());
            throw new VieAppServiceException(var5.getMessage());
         } catch (Exception var6) {
            this.logger.error("deleteModuleFromHomePage service has a error!", var6);
            throw new VieAppServiceException("服务内部错误", var6);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public PageRelateModuleResponse queryPageRelateModuleList(CustomHomePageRequest request) throws VieAppServiceException {
      PageRelateModuleResponse result = new PageRelateModuleResponse();
      if (request != null && request.getPageId() != null) {
         try {
            CustomHomePage pageInfo = this.customHomePageDB.queryCustomPageById(request.getPageId());
            List<HomePageRelateModule> moduleList = this.customHomePageDB.queryPageRelateModuleList(request.getPageId());
            if (moduleList != null && pageInfo != null) {
               result.setPageInfo(pageInfo);
               result.setModuleList(moduleList);
               return result;
            } else {
               this.logger.error("服务内部错误");
               throw new VieAppServiceException("服务内部错误");
            }
         } catch (VieAppServiceException var5) {
            this.logger.error(var5.getMessage());
            throw new VieAppServiceException(var5.getMessage());
         } catch (Exception var6) {
            this.logger.error("queryPageRelateModuleList service has a error!", var6);
            throw new VieAppServiceException("服务内部错误", var6);
         }
      } else {
         this.logger.error("参数(pageId)为空");
         throw new VieAppServiceException("参数(pageId)为空");
      }
   }

   public HomePageRelateModule getModuleInfoById(HomePageRelateModule request) throws VieAppServiceException {
      HomePageRelateModule homePageRelateModule = null;
      if (request != null && request.getId() != null) {
         try {
            return this.customHomePageDB.queryModuleInfoById(request.getId());
         } catch (Exception var4) {
            this.logger.error("getModuleInfoById service has a error!", var4);
            throw new VieAppServiceException("服务内部错误", var4);
         }
      } else {
         this.logger.error("参数(id)为空");
         throw new VieAppServiceException("参数(id)为空");
      }
   }

   public Long queryPageIdByModuleInfo(PageRelateModuleRequest pageRelateModuleRequest) throws VieAppServiceException {
      Long pageId = null;
      if (pageRelateModuleRequest != null) {
         try {
            List<String> keyList = new ArrayList<>();
            keyList.add("userId");
            keyList.add("moduleType");
            keyList.add("moduleId");
            String checkResult = this.checkRequestParamNull(pageRelateModuleRequest, keyList);
            if (checkResult != null) {
               this.logger.error("参数({})为空", checkResult);
               throw new VieAppServiceException("参数(" + checkResult + ")为空");
            } else {
               return this.customHomePageDB.queryModuleIfSendHomePage(pageRelateModuleRequest);
            }
         } catch (VieAppServiceException var5) {
            this.logger.error(var5.getMessage());
            throw new VieAppServiceException(var5.getMessage());
         } catch (Exception var6) {
            this.logger.error("queryPageIdByModuleInfo service has a error!", var6);
            throw new VieAppServiceException("服务内部错误", var6);
         }
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public CallTrendDimBeanDTO getCallTrendDimDTO() throws VieAppServiceException {
      CallTrendDimBeanDTO ctd = new CallTrendDimBeanDTO();
      if (StringUtils.isNullOrEmpry(this.env.getProperty("dim"))) {
         this.logger.error("dim参数为空!");
         throw new VieAppServiceException("dim参数为空!");
      } else if (StringUtils.isNullOrEmpry(this.env.getProperty("dimName"))) {
         this.logger.error("dimName参数为空!");
         throw new VieAppServiceException("dimName参数为空!");
      } else if (StringUtils.isNullOrEmpry(this.env.getProperty("dimValue"))) {
         this.logger.error("dimValue参数为空!");
         throw new VieAppServiceException("dimValue参数为空!");
      } else {
         try {
            ctd.setDimid(0L);
            ctd.setDimIndexField(this.env.getProperty("dim"));
            ctd.setDimName(this.env.getProperty("dimName"));
            List<String> dimValue = new ArrayList<>();
            String[] values = this.env.getProperty("dimValue").split(",");

            for (String v : values) {
               dimValue.add(v);
            }

            ctd.setDimValue(dimValue);
            return ctd;
         } catch (Exception var8) {
            this.logger.error("服务内部错误!", var8);
            throw new VieAppServiceException("服务内部错误!", var8);
         }
      }
   }

   public CallTrendDimResponse getCallTimeTrendByDim(CallTrendDimRequest request) throws VieAppServiceException {
      CallTrendDimResponse response = new CallTrendDimResponse();
      Date date = null;
      date = DateUtils.parseStringToDate(request.getSelectTime());
      long dateTimp = date.getTime();
      String sql = "";
      String startTime = "";
      String endTime = "";
      String dateTemp = "";
      StringBuilder wh = new StringBuilder();
      String centerIndex = request.getDataTypeDimension();
      this.logger.info("dataTypeDimension====" + centerIndex);
      String durationSql = "";
      String durationIndex = "";
      String timeType = request.getTimeType();
      int flag = request.getFlag();
      int durationType = request.getDurationType();
      int centerDimsion = request.getCenterDimsion();
      String selectCenter = request.getSelectCenter();
      String dimIndexField = request.getDimIndexField();
      String dimValue = request.getDimValue();
      if (centerDimsion != 0 && centerDimsion != 1 && centerDimsion != 2) {
         this.logger.error("参数centerDimsion不合法");
         throw new VieAppServiceException("参数centerDimsion不合法");
      } else if ((centerDimsion == 1 || centerDimsion == 2) && StringUtils.isNullOrEmpry(selectCenter)) {
         this.logger.error("centerDimsion不为0,参数selectCenter值不能为空");
         throw new VieAppServiceException("centerDimsion不为0,参数selectCenter值不能为空");
      } else if (!"day".equals(timeType) && !"hour".equals(timeType)) {
         this.logger.error("timeType参数timeType值只支持day或hour");
         throw new VieAppServiceException("timeType参数timeType值只支持day或hour");
      } else {
         if (durationType != -1) {
            switch (durationType) {
               case 0:
                  durationSql = " avg(silenceLong) as silenttime, ";
                  durationIndex = "silenttime";
                  break;
               case 1:
                  durationSql = " avg(vadDuration) as validtime, ";
                  durationIndex = "validtime";
                  break;
               case 2:
                  durationSql = " avg(duration) as timelen ,";
                  durationIndex = "timelen";
            }
         }

         try {
            String authSql = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
            if (!StringUtils.isNullOrEmpry(authSql)) {
               authSql = " and " + authSql;
            }

            String batchSql = " and " + MultipleDataConstant.getMultipleDataSql(request.getBatchId(), request.getIfLone(), request.getDataType());
            authSql = authSql + batchSql;
            String tableName = request.getDataSource();
            if ("day".equals(timeType)) {
               SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
               startTime = sdf.format(new Date(dateTimp - 604800000L));
               endTime = sdf.format(new Date(dateTimp - 86400000L));
               long startTemp = sdf.parse(startTime).getTime();
               long endTemp = sdf.parse(endTime).getTime() + 86399000L;
               dateTemp = sdf.format(date);
               if (flag == 0) {
                  switch (centerDimsion) {
                     case 0:
                        sql = "select dimDay as calltime , avg(duration) as timelen , avg(silenceLong) as silenttime , avg(vadDuration) as validtime  from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0 "
                           + authSql
                           + "  group by dimDay order by dimDay asc";
                        break;
                     case 1:
                        sql = "select dimDay as calltime , avg(duration) as timelen , avg(silenceLong) as silenttime , avg(vadDuration) as validtime  from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0 and "
                           + centerIndex
                           + " in  ('"
                           + selectCenter
                           + "') group by dimDay order by dimDay asc";
                        break;
                     case 2:
                        sql = "select dimDay as calltime,"
                           + durationSql
                           + centerIndex
                           + " from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0 "
                           + authSql
                           + " group by dimDay,"
                           + centerIndex
                           + " order by dimDay asc";
                  }
               } else {
                  wh.append(" " + dimIndexField + " = ");
                  wh.append("'");
                  wh.append(dimValue);
                  wh.append("'");
                  switch (centerDimsion) {
                     case 0:
                        sql = "select dimDay as calltime , avg(duration) as timelen , avg(silenceLong) as silenttime , avg(vadDuration) as validtime  from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0 and "
                           + wh.toString()
                           + " "
                           + authSql
                           + "  group by dimDay order by dimDay asc";
                        break;
                     case 1:
                        sql = "select dimDay as calltime , avg(duration) as timelen , avg(silenceLong) as silenttime , avg(vadDuration) as validtime  from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0  and "
                           + wh.toString()
                           + " and "
                           + centerIndex
                           + " in  ('"
                           + selectCenter
                           + "') group by dimDay order by dimDay asc";
                        break;
                     case 2:
                        sql = "select dimDay as calltime,"
                           + durationSql
                           + centerIndex
                           + " from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0 and "
                           + wh.toString()
                           + "  "
                           + authSql
                           + "  group by dimDay,"
                           + centerIndex
                           + " order by dimDay asc";
                  }
               }
            }

            if ("hour".equals(timeType)) {
               SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
               startTime = sdf.format(new Date(dateTimp - 86400000L)) + "00";
               SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHH");
               long startTemp = sdf1.parse(startTime).getTime();
               long endTemp = startTemp + 86400000L - 1000L;
               dateTemp = sdf.format(new Date(dateTimp)) + "00";
               if (flag == 0) {
                  switch (centerDimsion) {
                     case 0:
                        sql = "select dimHour as calltime , avg(duration) as timelen , avg(silenceLong) as silenttime , avg(vadDuration) as validtime  from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0 "
                           + authSql
                           + "  group by dimHour order by dimHour asc";
                        break;
                     case 1:
                        sql = "select dimHour as calltime , avg(duration) as timelen , avg(silenceLong) as silenttime , avg(vadDuration) as validtime  from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0 and "
                           + centerIndex
                           + " in  ('"
                           + selectCenter
                           + "') group by dimHour order by dimHour asc";
                        break;
                     case 2:
                        sql = "select dimHour as calltime,"
                           + durationSql
                           + centerIndex
                           + " from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0 "
                           + authSql
                           + " group by dimHour,"
                           + centerIndex
                           + " order by dimHour asc";
                  }
               } else {
                  wh.append(" " + dimIndexField + " = ");
                  wh.append("'");
                  wh.append(dimValue);
                  wh.append("'");
                  switch (centerDimsion) {
                     case 0:
                        sql = "select dimHour as calltime , avg(duration) as timelen , avg(silenceLong) as silenttime , avg(vadDuration) as validtime  from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0 and "
                           + wh.toString()
                           + " "
                           + authSql
                           + "  group by dimHour order by dimHour asc";
                        break;
                     case 1:
                        sql = "select dimHour as calltime , avg(duration) as timelen , avg(silenceLong) as silenttime , avg(vadDuration) as validtime  from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0  and "
                           + wh.toString()
                           + " and "
                           + centerIndex
                           + " in  ('"
                           + selectCenter
                           + "') group by dimHour order by dimHour asc";
                        break;
                     case 2:
                        sql = "select dimHour as calltime,"
                           + durationSql
                           + centerIndex
                           + " from "
                           + tableName
                           + " where timestamp between "
                           + startTemp
                           + " and "
                           + endTemp
                           + " and processed = 0 and "
                           + wh.toString()
                           + " "
                           + authSql
                           + " group by dimHour,"
                           + centerIndex
                           + " order by dimHour asc";
                  }
               }
            }

            LinkedHashMap<String, Object> trueData = this.getCallTimeTrend(
               sql,
               startTime,
               dateTemp,
               centerDimsion,
               durationIndex,
               centerIndex,
               timeType,
               Long.parseLong(request.getUserId()),
               request.getDataSource(),
               request.getDataTypeDimension()
            );
            response.setCallTrendDimMap(trueData);
            return response;
         } catch (Exception var30) {
            this.logger.error("服务内部出错!", var30);
            throw new VieAppServiceException("服务内部出错!", var30);
         }
      }
   }

   private LinkedHashMap<String, Object> getCallTimeTrend(
      String sql,
      String stime,
      String timetmp,
      int centerDimsion,
      String durationIndex,
      String centerIndex,
      String timeType,
      long userId,
      String dataSource,
      String dataTypeDimension
   ) {
      this.logger.info(sql);
      this.logger.info("{}执行sql", new Date().getTime());
      String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
      ResultSet rs = this.excuteContext.executeSearchQuery(url, sql);
      if (rs == null) {
         return new LinkedHashMap<>();
      } else {
         List<LinkedHashMap<String, Object>> linkedHashMapList = rs.toList();
         if (centerDimsion != 2) {
            if ("hour".equals(timeType)) {
               return this.dealListDataOfHour(linkedHashMapList, stime, timetmp);
            }

            if ("day".equals(timeType)) {
               return this.dealListDataOfDay(linkedHashMapList, stime, timetmp);
            }
         } else {
            if ("hour".equals(timeType)) {
               return this.dealListDataOfHour(linkedHashMapList, stime, timetmp, centerIndex, durationIndex, userId, dataSource, dataTypeDimension);
            }

            if ("day".equals(timeType)) {
               return this.dealListDataOfDay(linkedHashMapList, stime, timetmp, centerIndex, durationIndex, userId, dataSource, dataTypeDimension);
            }
         }

         return new LinkedHashMap<>();
      }
   }

   private LinkedHashMap<String, Object> dealListDataOfHour(List<LinkedHashMap<String, Object>> data, String startTime, String endTime) {
      try {
         LinkedHashMap<String, Object> result = new LinkedHashMap<>();
         List<HashMap<String, Object>> listval = new ArrayList<>();
         Date startDate = new SimpleDateFormat("yyyyMMddHH").parse(startTime);
         Date endDate = new SimpleDateFormat("yyyyMMddHH").parse(endTime);
         HashMap<String, Object> callMap = new HashMap<>();
         HashMap<String, Object> silentMap = new HashMap<>();
         HashMap<String, Object> validMap = new HashMap<>();
         List<String> list = new ArrayList<>();
         List<Double> timeList = new ArrayList<>();
         List<Double> silentList = new ArrayList<>();
         List<Double> validList = new ArrayList<>();
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(startDate);
         String startTemp = "";
         this.logger.info("{}处理开始", new Date().getTime());

         for (; calendar.getTime().before(endDate); calendar.add(10, 1)) {
            String calenTemp = new SimpleDateFormat("yyyyMMddHH").format(calendar.getTime());

            for (LinkedHashMap<String, Object> current : data) {
               String callTimeFlag = (String)current.get("calltime");
               double timeLenFlag = new BigDecimal((Double)current.get("timelen") / 1000.0).setScale(2, 4).doubleValue();
               double silentTimeFlag = new BigDecimal((Double)current.get("silenttime") / 1000.0).setScale(2, 4).doubleValue();
               double validTimeFlag = new BigDecimal((Double)current.get("validtime") / 1000.0).setScale(2, 4).doubleValue();
               if (callTimeFlag.equals(calenTemp)) {
                  list.add(this.dateFormatOfHour(calendar.getTime()));
                  timeList.add(timeLenFlag);
                  silentList.add(silentTimeFlag);
                  validList.add(validTimeFlag);
                  startTemp = calenTemp;
                  break;
               }
            }

            if (!startTemp.equals(calenTemp)) {
               list.add(this.dateFormatOfHour(calendar.getTime()));
               timeList.add(0.0);
               silentList.add(0.0);
               validList.add(0.0);
            }
         }

         callMap.put("type", "line");
         callMap.put("name", "通话时长");
         callMap.put("data", timeList);
         silentMap.put("name", "静音时长");
         silentMap.put("data", silentList);
         validMap.put("name", "有效时长");
         validMap.put("data", validList);
         listval.add(callMap);
         listval.add(validMap);
         listval.add(silentMap);
         result.put("xData", list);
         result.put("yData", listval);
         this.logger.info("{}返回结果", new Date().getTime());
         return result;
      } catch (Exception var27) {
         this.logger.error(ExceptionUtil.getStackTrace(var27));
         return null;
      }
   }

   private LinkedHashMap<String, Object> dealListDataOfDay(List<LinkedHashMap<String, Object>> data, String startTime, String endTime) {
      try {
         LinkedHashMap<String, Object> result = new LinkedHashMap<>();
         List<HashMap<String, Object>> listval = new ArrayList<>();
         Date startDate = new SimpleDateFormat("yyyyMMdd").parse(startTime);
         Date endDate = new SimpleDateFormat("yyyyMMdd").parse(endTime);
         HashMap<String, Object> callMap = new HashMap<>();
         HashMap<String, Object> silentMap = new HashMap<>();
         HashMap<String, Object> validMap = new HashMap<>();
         List<String> list = new ArrayList<>();
         List<Double> timeList = new ArrayList<>();
         List<Double> silentList = new ArrayList<>();
         List<Double> validList = new ArrayList<>();
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(startDate);
         String startTemp = "";
         this.logger.info("{}处理开始", new Date().getTime());

         for (; calendar.getTime().before(endDate); calendar.add(5, 1)) {
            String calenTemp = new SimpleDateFormat("yyyyMMdd").format(calendar.getTime());

            for (LinkedHashMap<String, Object> current : data) {
               String callTimeFlag = (String)current.get("calltime");
               double timeLenFlag = new BigDecimal((Double)current.get("timelen") / 1000.0).setScale(2, 4).doubleValue();
               double silentTimeFlag = new BigDecimal((Double)current.get("silenttime") / 1000.0).setScale(2, 4).doubleValue();
               double validTimeFlag = new BigDecimal((Double)current.get("validtime") / 1000.0).setScale(2, 4).doubleValue();
               if (callTimeFlag.equals(calenTemp)) {
                  list.add(this.dateFormatOfDay(calendar.getTime()));
                  timeList.add(timeLenFlag);
                  silentList.add(silentTimeFlag);
                  validList.add(validTimeFlag);
                  startTemp = calenTemp;
                  break;
               }
            }

            if (!startTemp.equals(calenTemp)) {
               list.add(this.dateFormatOfDay(calendar.getTime()));
               timeList.add(0.0);
               silentList.add(0.0);
               validList.add(0.0);
            }
         }

         callMap.put("type", "line");
         callMap.put("name", "通话时长");
         callMap.put("data", timeList);
         silentMap.put("name", "静音时长");
         silentMap.put("data", silentList);
         validMap.put("name", "有效时长");
         validMap.put("data", validList);
         listval.add(callMap);
         listval.add(validMap);
         listval.add(silentMap);
         result.put("xData", list);
         result.put("yData", listval);
         this.logger.info("{}返回结果", new Date().getTime());
         return result;
      } catch (Exception var27) {
         this.logger.error(ExceptionUtil.getStackTrace(var27));
         return null;
      }
   }

   private LinkedHashMap<String, Object> dealListDataOfHour(
      List<LinkedHashMap<String, Object>> data,
      String startTime,
      String endTime,
      String centerIndex,
      String durationIndex,
      long userId,
      String dataSource,
      String dataTypeDimension
   ) {
      try {
         String durationCh = "";
         switch (durationIndex) {
            case "timelen":
               durationCh = "通话时长";
               break;
            case "silenttime":
               durationCh = "静音时长";
               break;
            case "validtime":
               durationCh = "有效时长";
         }

         LinkedHashMap<String, Object> result = new LinkedHashMap<>();
         List<HashMap<String, Object>> yDataList = new ArrayList<>();
         Date startDate = new SimpleDateFormat("yyyyMMddHH").parse(startTime);
         Date endDate = new SimpleDateFormat("yyyyMMddHH").parse(endTime);
         List<String> xDataList = new ArrayList<>();
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(startDate);
         this.logger.info("{}处理开始", new Date().getTime());
         String[] centers = this.getCenters(userId, dataSource, dataTypeDimension);

         for (String center : centers) {
            String startTemp = "";
            calendar.setTime(startDate);
            xDataList.clear();
            HashMap<String, Object> durationMap = new HashMap<>();

            List<Double> durationList;
            for (durationList = new ArrayList<>(); calendar.getTime().before(endDate); calendar.add(10, 1)) {
               String calenTemp = new SimpleDateFormat("yyyyMMddHH").format(calendar.getTime());

               for (LinkedHashMap<String, Object> current : data) {
                  String callTimeFlag = (String)current.get("calltime");
                  String centerFlag = (String)current.get(centerIndex);
                  double durationFlag = new BigDecimal((Double)current.get(durationIndex) / 1000.0).setScale(2, 4).doubleValue();
                  if (callTimeFlag.equals(calenTemp) && centerFlag.equals(center)) {
                     xDataList.add(this.dateFormatOfHour(calendar.getTime()));
                     durationList.add(durationFlag);
                     startTemp = calenTemp;
                     break;
                  }
               }

               if (!startTemp.equals(calenTemp)) {
                  xDataList.add(this.dateFormatOfHour(calendar.getTime()));
                  durationList.add(0.0);
               }
            }

            durationMap.put("type", "line");
            durationMap.put("name", durationCh);
            durationMap.put("data", durationList);
            durationMap.put("center", center);
            yDataList.add(durationMap);
         }

         result.put("xData", xDataList);
         result.put("yData", yDataList);
         this.logger.info("{}返回结果", new Date().getTime());
         return result;
      } catch (Exception var32) {
         this.logger.error(ExceptionUtil.getStackTrace(var32));
         return null;
      }
   }

   private LinkedHashMap<String, Object> dealListDataOfDay(
      List<LinkedHashMap<String, Object>> data,
      String startTime,
      String endTime,
      String centerIndex,
      String durationIndex,
      long userId,
      String dataSource,
      String dataTypeDimension
   ) {
      try {
         String durationCh = "";
         switch (durationIndex) {
            case "timelen":
               durationCh = "通话时长";
               break;
            case "silenttime":
               durationCh = "静音时长";
               break;
            case "validtime":
               durationCh = "有效时长";
         }

         LinkedHashMap<String, Object> result = new LinkedHashMap<>();
         List<HashMap<String, Object>> yDataList = new ArrayList<>();
         Date startDate = new SimpleDateFormat("yyyyMMdd").parse(startTime);
         Date endDate = new SimpleDateFormat("yyyyMMdd").parse(endTime);
         List<String> xDataList = new ArrayList<>();
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(startDate);
         this.logger.info("{}处理开始", new Date().getTime());
         String[] centers = this.getCenters(userId, dataSource, dataTypeDimension);

         for (String center : centers) {
            xDataList.clear();
            String startTemp = "";
            calendar.setTime(startDate);
            HashMap<String, Object> durationMap = new HashMap<>();

            List<Double> durationList;
            for (durationList = new ArrayList<>(); calendar.getTime().before(endDate); calendar.add(5, 1)) {
               String calenTemp = new SimpleDateFormat("yyyyMMdd").format(calendar.getTime());

               for (LinkedHashMap<String, Object> current : data) {
                  String callTimeFlag = (String)current.get("calltime");
                  String centerFlag = (String)current.get(centerIndex);
                  double durationFlag = new BigDecimal((Double)current.get(durationIndex) / 1000.0).setScale(2, 4).doubleValue();
                  if (callTimeFlag.equals(calenTemp) && centerFlag.equals(center)) {
                     xDataList.add(this.dateFormatOfDay(calendar.getTime()));
                     durationList.add(durationFlag);
                     startTemp = calenTemp;
                     break;
                  }
               }

               if (!startTemp.equals(calenTemp)) {
                  xDataList.add(this.dateFormatOfDay(calendar.getTime()));
                  durationList.add(0.0);
               }
            }

            durationMap.put("type", "line");
            durationMap.put("name", durationCh);
            durationMap.put("data", durationList);
            durationMap.put("center", center);
            yDataList.add(durationMap);
         }

         result.put("xData", xDataList);
         result.put("yData", yDataList);
         this.logger.info("{}返回结果", new Date().getTime());
         return result;
      } catch (Exception var32) {
         this.logger.error(ExceptionUtil.getStackTrace(var32));
         return null;
      }
   }

   public String[] getCenters(long userId, String dataSource, String dataTypeDimension) {
      List<DimensionAuth> dataAuth = this.commonData.getDataMap(userId, "VIEWEB", dataSource);
      String[] centers = null;
      StringBuilder sb = new StringBuilder();
      if (dataAuth != null && dataAuth.size() > 0) {
         for (DimensionAuth dimAuth : dataAuth) {
            if (dimAuth.getEnglishName().equals(dataTypeDimension)) {
               for (String center : dimAuth.getValue()) {
                  sb.append(center + ",");
               }
            }
         }
      }

      return sb.toString().split(",");
   }

   private String dateFormatOfHour(Date startTime) {
      return startTime.getHours() + ":00";
   }

   private String dateFormatOfDay(Date startTime) {
      return startTime.getMonth() + 1 + "月" + startTime.getDate() + "日";
   }

   private String checkRequestParamNull(CustomHomePageRequest customHomePageRequest, List<String> list) {
      if (list != null && list.size() > 0) {
         String key = null;

         for (int i = 0; i < list.size(); i++) {
            key = list.get(i);
            if ("userId".equals(key)) {
               if (StringUtils.isNullOrEmpry(customHomePageRequest.getUserId())) {
                  return key;
               }
            } else if ("pageId".equals(key)) {
               if (customHomePageRequest.getPageId() == null) {
                  return key;
               }
            } else if ("pageName".equals(key)) {
               if (StringUtils.isNullOrEmpry(customHomePageRequest.getPageName())) {
                  return key;
               }
            } else if ("maxNum".equals(key) && customHomePageRequest.getMaxNum() == null) {
               return key;
            }
         }
      }

      return null;
   }

   private String checkRequestParamNull(PageRelateModuleRequest pageRelateModuleRequest, List<String> list) {
      if (list != null && list.size() > 0) {
         String key = null;

         for (int i = 0; i < list.size(); i++) {
            key = list.get(i);
            if ("id".equals(key)) {
               if (pageRelateModuleRequest.getId() == null) {
                  return key;
               }
            } else if ("userId".equals(key)) {
               if (StringUtils.isNullOrEmpry(pageRelateModuleRequest.getUserId())) {
                  return key;
               }
            } else if ("pageId".equals(key)) {
               if (pageRelateModuleRequest.getPageId() == null) {
                  return key;
               }
            } else if ("moduleName".equals(key)) {
               if (StringUtils.isNullOrEmpry(pageRelateModuleRequest.getModuleName())) {
                  return key;
               }
            } else if ("moduleType".equals(key)) {
               if (pageRelateModuleRequest.getModuleType() == null) {
                  return key;
               }
            } else if ("contentType".equals(key)) {
               if (pageRelateModuleRequest.getContentType() == null) {
                  return key;
               }
            } else if ("moduleId".equals(key)) {
               if (pageRelateModuleRequest.getModuleId() == null) {
                  return key;
               }
            } else if ("moduleIds".equals(key)) {
               if (StringUtils.isNullOrEmpry(pageRelateModuleRequest.getModuleIds())) {
                  return key;
               }
            } else if ("moduleDetailInfo".equals(key)) {
               if (StringUtils.isNullOrEmpry(pageRelateModuleRequest.getModuleDetailInfo())) {
                  return key;
               }
            } else if ("maxNum".equals(key) && pageRelateModuleRequest.getMaxNum() == null) {
               return key;
            }
         }
      }

      return null;
   }

   private void setPageRelateModuleInfo(HomePageRelateModule pageRelateModule, PageRelateModuleRequest pageRelateModuleRequest) {
      pageRelateModule.setModuleName(pageRelateModuleRequest.getModuleName());
      pageRelateModule.setPageId(pageRelateModuleRequest.getPageId());
      pageRelateModule.setModuleType(pageRelateModuleRequest.getModuleType());
      pageRelateModule.setModuleId(pageRelateModuleRequest.getModuleId());
      pageRelateModule.setModuleDetailInfo(pageRelateModuleRequest.getModuleDetailInfo());
      pageRelateModule.setContentType(pageRelateModuleRequest.getContentType());
      pageRelateModule.setContentParam(pageRelateModuleRequest.getContentParam());
      pageRelateModule.setDimensions(pageRelateModuleRequest.getDimensions());
      pageRelateModule.setPicturesShow(pageRelateModuleRequest.getPicturesShow());
      pageRelateModule.setCreateTime(new Date());
      pageRelateModule.setSortOrder(0);
   }

   public DataDetailResponse getTableData1(HomeTableDataRequest homeTableDataRequest) throws Exception {
      this.logger.info("聚类钻取标准服务getTableData方法开始响应");
      if (!StringUtils.isNullOrEmpry(homeTableDataRequest.getDimDay()) && !StringUtils.isNullOrEmpry(homeTableDataRequest.getVoiceId())) {
         String[] dimDay = homeTableDataRequest.getDimDay().split(",");
         String[] voiceId = homeTableDataRequest.getVoiceId().split(",");
         int pageSize = homeTableDataRequest.getPageSize();
         int pageNum = homeTableDataRequest.getPageNum();
         int dataType = homeTableDataRequest.getDataType();
         String ifLone = homeTableDataRequest.getIfLone();
         String batchId = homeTableDataRequest.getBatchId();
         String userId = homeTableDataRequest.getUserId();
         String systemId = homeTableDataRequest.getSystemId();
         DataDetailResponse dataDetailResponse = new DataDetailResponse();
         List<String> searchColumns = homeTableDataRequest.getSearchColumns();
         List<LinkedHashMap<String, Object>> resultList = new ArrayList<>();
         String sql = "";
         String selectSql = "";
         String timeCondSql = "";
         String voiceIdCondSql = "";
         String importSql = "";
         String permissonSql = "";
         String orderSql = "";
         String tableName = "";
         String excuteSql = "";
         if (dimDay.length > 1) {
            timeCondSql = timeCondSql + " (";

            for (int i = 0; i < dimDay.length; i++) {
               if (i == dimDay.length - 1) {
                  timeCondSql = timeCondSql + " dimDay = '" + dimDay[i].replace("-", "") + "')";
               } else {
                  timeCondSql = timeCondSql + "dimDay = '" + dimDay[i].replace("-", "") + "' or";
               }
            }

            timeCondSql = timeCondSql + ")";
         } else {
            timeCondSql = timeCondSql + " dimDay = '" + dimDay[0] + "'";
         }

         if (voiceId.length > 1) {
            voiceIdCondSql = voiceIdCondSql + "(";

            for (int ix = 0; ix < voiceId.length; ix++) {
               if (ix == voiceId.length - 1) {
                  voiceIdCondSql = voiceIdCondSql + " id = '" + voiceId[ix] + "')";
               } else {
                  voiceIdCondSql = voiceIdCondSql + " id = '" + voiceId[ix] + "' or ";
               }
            }
         } else {
            voiceIdCondSql = voiceIdCondSql + " id = '" + voiceId[0] + "'";
         }

         for (int ixx = 0; ixx < searchColumns.size(); ixx++) {
            if (ixx == searchColumns.size() - 1) {
               selectSql = selectSql + searchColumns.get(ixx) + " as " + searchColumns.get(ixx);
            } else {
               selectSql = selectSql + searchColumns.get(ixx) + " as " + searchColumns.get(ixx) + ",";
            }
         }

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

         ModelApplyServiceImpl modelApplyService = new ModelApplyServiceImpl();
         permissonSql = modelApplyService.getPermissionSql(userId, systemId, homeTableDataRequest.getDataSource());
         permissonSql = permissonSql + " and processed = 0" + importSql;
         if (StringUtils.isNotNullAndEmpry(homeTableDataRequest.getSortColumn()) && StringUtils.isNotNullAndEmpry(homeTableDataRequest.getSortColumn())) {
            orderSql = orderSql + " order by " + homeTableDataRequest.getSortColumn() + " " + homeTableDataRequest.getSortType();
         }

         tableName = homeTableDataRequest.getDataSource();
         sql = sql + "select " + selectSql + " from " + tableName + " where " + timeCondSql + " and " + voiceIdCondSql + permissonSql + orderSql;
         excuteSql = excuteSql + "select *,row_number() over(" + orderSql + ") as rownum from (" + sql + ")";
         SparkUtils sparkUtils = new SparkUtils();
         sparkUtils.init();
         Map<String, String> optMain = new HashMap<>();
         optMain.put("pushdown", "true");
         optMain.put("es.nodes", LoadCommonConfig.get("modelEsAddress").trim().replace(";", ","));
         sparkUtils.getSqlContext()
            .read()
            .format("org.elasticsearch.spark.sql")
            .options(optMain)
            .load(tableName + "_*/" + tableName)
            .createOrReplaceTempView(tableName);
         this.logger.info("getHomePageTableData sql start:" + excuteSql);
         Long startTime = System.currentTimeMillis();
         Dataset<Row> dataset = sparkUtils.getSqlContext().sql(excuteSql);
         Long endTime = System.currentTimeMillis();
         this.logger.info("cost time:" + (endTime - startTime) + "毫秒------------getColumnData sql end:" + excuteSql);
         List<Row> resultRow = null;
         int totalCount = new Long(dataset.count()).intValue();
         int totalPage;
         int pageNow;
         if (pageSize > 0) {
            totalPage = totalCount % pageSize == 0 ? totalCount % pageSize : totalCount % pageSize + 1;
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
         if (totalCount <= 0) {
            this.logger.info("未查询到相关结果！");
            dataDetailResponse.setValues(resultList);
            this.logger.info("聚类钻取标准服务getTableData方法响应成功");
            return dataDetailResponse;
         } else {
            if (pageNum > 0 && pageSize > 0) {
               int rownumStart = pageSize * (pageNum - 1) + 1;
               int rownumEnd = pageSize * pageNum;
               resultRow = dataset.filter("rownum >= " + rownumStart + " and rownum <= " + rownumEnd).collectAsList();
            } else {
               resultRow = dataset.collectAsList();
            }

            if (resultRow.size() > 0) {
               for (int ixxx = 0; ixxx < resultRow.size(); ixxx++) {
                  LinkedHashMap<String, Object> lhm = new LinkedHashMap<>();

                  for (int j = 0; j < searchColumns.size(); j++) {
                     lhm.put(searchColumns.get(j), resultRow.get(ixxx).get(j));
                  }

                  resultList.add(lhm);
               }
            } else {
               resultList = null;
            }

            dataDetailResponse.setValues(resultList);
            this.logger.info("聚类钻取标准服务getTableData方法响应成功");
            return dataDetailResponse;
         }
      } else {
         this.logger.info("入参有误，过滤条件不能为空");
         return new DataDetailResponse();
      }
   }

   public DataDetailResponse getTableData2(HomeTableDataRequest homeTableDataRequest) throws Exception {
      this.logger.info("聚类钻取标准服务getTableData方法开始响应");
      if (!StringUtils.isNullOrEmpry(homeTableDataRequest.getDimDay()) && !StringUtils.isNullOrEmpry(homeTableDataRequest.getVoiceId())) {
         String[] dimDay = homeTableDataRequest.getDimDay().split(",");
         String voiceIds = homeTableDataRequest.getVoiceId();
         int pageSize = homeTableDataRequest.getPageSize();
         int pageNum = homeTableDataRequest.getPageNum();
         int dataType = homeTableDataRequest.getDataType();
         String ifLone = homeTableDataRequest.getIfLone();
         String batchId = homeTableDataRequest.getBatchId();
         String userId = homeTableDataRequest.getUserId();
         String systemId = homeTableDataRequest.getSystemId();
         DataDetailResponse dataDetailResponse = new DataDetailResponse();
         List<String> searchColumns = homeTableDataRequest.getSearchColumns();
         List<LinkedHashMap<String, Object>> resultList = new ArrayList<>();
         new ArrayList();
         String sql = "";
         String selectSql = "";
         String timeCondSql = "";
         String voiceIdCondSql = "";
         String importSql = "";
         String permissonSql = "";
         String orderSql = "";
         String tableName = "";
         String excuteSql = "";
         if (dimDay.length > 1) {
            timeCondSql = timeCondSql + " (";

            for (int i = 0; i < dimDay.length; i++) {
               if (i == dimDay.length - 1) {
                  timeCondSql = timeCondSql + " dimDay = '" + dimDay[i].replace("-", "") + "')";
               } else {
                  timeCondSql = timeCondSql + "dimDay = '" + dimDay[i].replace("-", "") + "' or";
               }
            }

            timeCondSql = timeCondSql + ")";
         } else {
            timeCondSql = timeCondSql + " dimDay = '" + dimDay[0].replace("-", "") + "'";
         }

         voiceIdCondSql = " id in (" + voiceIds + ")";
         List<String> searchList = new ArrayList<>();
         List<String> modelSearchList = new ArrayList<>();

         for (int k = 0; k < searchColumns.size(); k++) {
            if (!searchColumns.get(k).equals("offLineTagId")
               && !searchColumns.get(k).equals("offLineTagName")
               && !searchColumns.get(k).equals("offLineTagInfo")) {
               searchList.add(searchColumns.get(k));
            } else {
               modelSearchList.add(searchColumns.get(k));
            }
         }

         String normalAlias = "";
         String modelAlias = "";

         for (int ix = 0; ix < searchList.size(); ix++) {
            if (ix == searchList.size() - 1) {
               selectSql = selectSql + searchList.get(ix) + " as " + searchList.get(ix);
            } else {
               selectSql = selectSql + searchList.get(ix) + " as " + searchList.get(ix) + ",";
            }

            normalAlias = normalAlias + "a." + searchList.get(ix) + ",";
         }

         String modelSelectSql = "";

         for (int ix = 0; ix < modelSearchList.size(); ix++) {
            if (ix == modelSearchList.size() - 1) {
               modelSelectSql = modelSelectSql + modelSearchList.get(ix) + " as " + modelSearchList.get(ix);
               modelAlias = modelAlias + "b." + modelSearchList.get(ix);
            } else {
               modelSelectSql = modelSelectSql + modelSearchList.get(ix) + " as " + modelSearchList.get(ix) + ",";
               modelAlias = modelAlias + "b." + modelSearchList.get(ix) + ",";
            }
         }

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

         ModelApplyServiceImpl modelApplyService = new ModelApplyServiceImpl();
         permissonSql = modelApplyService.getPermissionSql(userId, systemId, homeTableDataRequest.getDataSource());
         permissonSql = permissonSql + " and processed = 0" + importSql;
         if (StringUtils.isNotNullAndEmpry(homeTableDataRequest.getSortColumn()) && StringUtils.isNotNullAndEmpry(homeTableDataRequest.getSortColumn())) {
            orderSql = orderSql + " order by " + homeTableDataRequest.getSortColumn() + " " + homeTableDataRequest.getSortType();
         }

         tableName = homeTableDataRequest.getDataSource();
         String modelTableName = IndexConstants.getModelTableName(tableName);
         Map<String, String> optMain = new HashMap<>();
         Map<String, String> optModel = new HashMap<>();
         optMain.put("pushdown", "true");
         optMain.put("es.nodes", LoadCommonConfig.get("mainEsAddress").trim().replace(";", ","));
         optModel.put("pushdown", "true");
         optModel.put("es.nodes", LoadCommonConfig.get("modelEsAddress").trim().replace(";", ","));
         this.sparkUtils
            .getSqlContext()
            .read()
            .format("org.elasticsearch.spark.sql")
            .options(optMain)
            .load(tableName + "_*/" + tableName)
            .createOrReplaceTempView(tableName);
         this.sparkUtils
            .getSqlContext()
            .read()
            .format("org.elasticsearch.spark.sql")
            .options(optModel)
            .load(modelTableName + "_*/" + modelTableName)
            .createOrReplaceTempView(modelTableName);
         String pageSql = this.getPageSql(pageNum, pageSize);
         sql = "select "
            + normalAlias
            + modelAlias
            + "  from (select id,"
            + selectSql
            + "  from  "
            + tableName
            + ") a left join (select id,"
            + modelSelectSql
            + " from "
            + modelTableName
            + ") b"
            + " on a.id=b.id ";
         excuteSql = excuteSql + "select *,row_number() over(" + orderSql + ") as rownum from (" + sql + ")";
         Long startTime = System.currentTimeMillis();
         Dataset<Row> dataset = this.sparkUtils.getSqlContext().sql(excuteSql).cache();
         Long endTime = System.currentTimeMillis();
         this.logger.info("cost time:" + (endTime - startTime) + "毫秒------------getColumnData sql end:" + excuteSql);
         int totalCount = new Long(dataset.count()).intValue();
         int totalPage;
         int pageNow;
         if (pageSize > 0) {
            totalPage = totalCount % pageSize == 0 ? totalCount % pageSize : totalCount % pageSize + 1;
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
         if (totalCount <= 0) {
            this.logger.info("未查询到相关结果！");
            dataDetailResponse.setValues(resultList);
            this.logger.info("聚类钻取标准服务getTableData方法响应成功");
            return dataDetailResponse;
         } else {
            List resultRow;
            if (pageNum > 0 && pageSize > 0) {
               int rownumStart = pageSize * (pageNum - 1) + 1;
               int rownumEnd = pageSize * pageNum;
               resultRow = dataset.filter("rownum >= " + rownumStart + " and rownum <= " + rownumEnd).collectAsList();
            } else {
               resultRow = dataset.collectAsList();
            }

            if (resultRow.size() > 0) {
               for (int ixx = 0; ixx < resultRow.size(); ixx++) {
                  LinkedHashMap<String, Object> lhm = new LinkedHashMap<>();

                  for (int j = 0; j < searchColumns.size(); j++) {
                     lhm.put(searchColumns.get(j), ((Row)resultRow.get(ixx)).get(j));
                  }

                  resultList.add(lhm);
               }
            } else {
               resultList = null;
            }

            dataDetailResponse.setValues(resultList);
            this.logger.info("聚类钻取标准服务getTableData方法响应成功");
            return dataDetailResponse;
         }
      } else {
         this.logger.info("入参有误，过滤条件不能为空");
         return new DataDetailResponse();
      }
   }

   private String getPageSql(int fetchFrom, int fetchSize) throws Exception {
      String result = "";
      int pageIndex = fetchFrom < 1 ? 1 : fetchFrom;
      int startNum = (pageIndex - 1) * fetchSize + 1;
      int endNum = pageIndex * fetchSize;
      return " rownum between " + startNum + " and " + endNum;
   }

   public DataDetailResponse getTableData(HomeTableDataRequest homeTableDataRequest) throws Exception {
      DataDetailResponse dataDetailResponse = new DataDetailResponse();

      try {
         this.logger.info("聚类钻取标准服务getTableData方法开始响应");
         if (!StringUtils.isNullOrEmpry(homeTableDataRequest.getDimDay()) && !StringUtils.isNullOrEmpry(homeTableDataRequest.getVoiceId())) {
            String[] dimDay = homeTableDataRequest.getDimDay().split(",");
            String voiceIds = homeTableDataRequest.getVoiceId();
            int pageSize = homeTableDataRequest.getPageSize();
            int pageNum = homeTableDataRequest.getPageNum();
            int dataType = homeTableDataRequest.getDataType();
            String ifLone = homeTableDataRequest.getIfLone();
            String batchId = homeTableDataRequest.getBatchId();
            String userId = homeTableDataRequest.getUserId();
            String systemId = homeTableDataRequest.getSystemId();
            List<String> searchColumns = homeTableDataRequest.getSearchColumns();
            new ArrayList();
            new ArrayList();
            new ArrayList();
            List<LinkedHashMap<String, LinkedHashMap<String, Object>>> resultMap = new ArrayList<>();
            new ArrayList();
            String sql = "";
            String selectSql = "";
            String timeCondSql = "";
            String voiceIdCondSql = "";
            String importSql = "";
            String permissonSql = "";
            String orderSql = "";
            String tableName = "";
            String excuteSql = "";
            if (dimDay.length > 1) {
               timeCondSql = timeCondSql + " (";

               for (int i = 0; i < dimDay.length; i++) {
                  if (i == dimDay.length - 1) {
                     timeCondSql = timeCondSql + " dimDay = '" + dimDay[i].replace("-", "") + "')";
                  } else {
                     timeCondSql = timeCondSql + "dimDay = '" + dimDay[i].replace("-", "") + "' or";
                  }
               }

               timeCondSql = timeCondSql + ")";
            } else {
               timeCondSql = timeCondSql + " dimDay = '" + dimDay[0].replace("-", "") + "'";
            }

            voiceIdCondSql = " id in (" + voiceIds + ")";
            List<String> searchList = new ArrayList<>();
            List<String> modelSearchList = new ArrayList<>();

            for (int k = 0; k < searchColumns.size(); k++) {
               if (!searchColumns.get(k).equals("offLineTagId")
                  && !searchColumns.get(k).equals("offLineTagName")
                  && !searchColumns.get(k).equals("offLineTagInfo")) {
                  searchList.add(searchColumns.get(k));
               } else {
                  modelSearchList.add(searchColumns.get(k));
               }
            }

            for (int ix = 0; ix < searchList.size(); ix++) {
               if (ix == searchList.size() - 1) {
                  selectSql = selectSql + searchList.get(ix) + " as " + searchList.get(ix);
               } else {
                  selectSql = selectSql + searchList.get(ix) + " as " + searchList.get(ix) + ",";
               }
            }

            String modelSelectSql = "";

            for (int ixx = 0; ixx < modelSearchList.size(); ixx++) {
               if (ixx == modelSearchList.size() - 1) {
                  modelSelectSql = modelSelectSql + modelSearchList.get(ixx) + " as " + modelSearchList.get(ixx);
               } else {
                  modelSelectSql = modelSelectSql + modelSearchList.get(ixx) + " as " + modelSearchList.get(ixx) + ",";
               }
            }

            if ("1".equals(ifLone)) {
               importSql = importSql + " and ifLone = 1 and batchId = '" + batchId + "'";
            } else {
               importSql = importSql + " and ifLone != 1 ";
            }

            if (2 == dataType) {
               importSql = importSql + " and (dataType = 0 or dataType = 2) ";
            } else {
               importSql = importSql + " and dataType = " + dataType;
            }

            permissonSql = this.getPermissionSql(userId, systemId, homeTableDataRequest.getDataSource());
            permissonSql = permissonSql + " and processed = 0" + importSql;
            if (StringUtils.isNotNullAndEmpry(homeTableDataRequest.getSortColumn()) && StringUtils.isNotNullAndEmpry(homeTableDataRequest.getSortColumn())) {
               orderSql = orderSql + " order by " + homeTableDataRequest.getSortColumn() + " " + homeTableDataRequest.getSortType();
            }

            tableName = homeTableDataRequest.getDataSource();
            String modelTableName = IndexConstants.getModelTableName(tableName);
            String pageSql = this.getPageSql(pageNum, pageSize);
            sql = sql
               + "select id,"
               + selectSql
               + " from "
               + tableName
               + " where "
               + timeCondSql
               + " and "
               + voiceIdCondSql
               + permissonSql
               + " and "
               + pageSql
               + orderSql;
            this.logger.info("聚类钻取列表查询主索引sql=>" + sql);
            String url = DynamicEsSource.getEsSourceByType(tableName, "mainAnydrillAddress");
            ExcuteContext ec = new ExcuteContext();
            ResultSet rs = ec.executeSearchQuery(url, sql);
            List<LinkedHashMap<String, Object>> resultList = rs.toList();
            String pageVoiceIds = "";

            for (LinkedHashMap<String, Object> rsMap : resultList) {
               LinkedHashMap<String, LinkedHashMap<String, Object>> tempMap = new LinkedHashMap<>();
               tempMap.put(String.valueOf(rsMap.get("id")), rsMap);
               pageVoiceIds = pageVoiceIds + "'" + rsMap.get("id") + "',";
               resultMap.add(tempMap);
            }

            if (StringUtils.isNotNullAndEmpry(pageVoiceIds)) {
               pageVoiceIds = pageVoiceIds.substring(0, pageVoiceIds.length() - 1);
            }

            AuthorizeInfo authorizeInfo = new AuthorizeInfo();
            authorizeInfo.setDataSource(homeTableDataRequest.getDataSource());
            authorizeInfo.setUserId(homeTableDataRequest.getUserId());
            HashMap<String, String> modelMap = this.modelApplyService.getAllModelMap(authorizeInfo);
            String modelSql = "select id," + modelSelectSql + "  from " + modelTableName + " where id in (" + pageVoiceIds + ")";
            this.logger.info("聚类钻取列表查询模型索引sql=>" + modelSql);
            String modelUrl = DynamicEsSource.getEsSourceByType(tableName, "modelAnydrillAddress");
            ExcuteContext ecModel = new ExcuteContext();
            ResultSet rsModel = ecModel.executeSearchQuery(modelUrl, modelSql);
            List<LinkedHashMap<String, Object>> modelResultList = rsModel.toList();
            Iterator<LinkedHashMap<String, Object>> it = modelResultList.iterator();

            while (it.hasNext()) {
               LinkedHashMap<String, Object> optionMap = it.next();
               if (!modelMap.containsKey(String.valueOf(optionMap.get("offLineTagId")))) {
                  it.remove();
               }
            }

            for (LinkedHashMap<String, Object> rsModelMap : this.dealModelInfo(modelResultList)) {
               for (LinkedHashMap<String, LinkedHashMap<String, Object>> rsMap : resultMap) {
                  if (rsMap.containsKey(String.valueOf(rsModelMap.get("id")))) {
                     rsMap.get(String.valueOf(rsModelMap.get("id"))).putAll(rsModelMap);
                  }
               }
            }

            List<LinkedHashMap<String, Object>> totalList = new ArrayList<>();

            for (LinkedHashMap<String, LinkedHashMap<String, Object>> rsMap2 : resultMap) {
               totalList.addAll(rsMap2.values());
            }

            int totalCount = voiceIds.split(",").length;
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
            List<LinkedHashMap<String, Object>> endList = new ArrayList<>();
            if (totalList.size() > 0) {
               for (int ixxx = 0; ixxx < totalList.size(); ixxx++) {
                  LinkedHashMap<String, Object> lhm = new LinkedHashMap<>();

                  for (int j = 0; j < searchColumns.size(); j++) {
                     lhm.put(searchColumns.get(j), totalList.get(ixxx).get(searchColumns.get(j)));
                  }

                  endList.add(lhm);
               }
            } else {
               endList = null;
            }

            dataDetailResponse.setValues(endList);
            this.logger.info("聚类钻取标准服务getTableData方法响应成功");
            return dataDetailResponse;
         } else {
            this.logger.info("入参有误，过滤条件不能为空");
            return new DataDetailResponse();
         }
      } catch (Exception var53) {
         var53.printStackTrace();
         this.logger.info("聚类钻取标准服务getTableData方法响应异常");
         return dataDetailResponse;
      }
   }

   public List<LinkedHashMap<String, Object>> dealModelInfo(List<LinkedHashMap<String, Object>> values) {
      LinkedHashMap<String, LinkedHashMap<String, Object>> modelInfoMap = new LinkedHashMap<>();
      String id = "";
      List<Map<String, Object>> preList = new ArrayList<>();
      List<Map<String, Object>> thisList = new ArrayList<>();

      for (LinkedHashMap<String, Object> map : values) {
         if (map.containsKey("id")) {
            id = String.valueOf(map.get("id"));
         }

         if (modelInfoMap.containsKey(id)) {
            LinkedHashMap<String, Object> modelInfo = modelInfoMap.get(id);
            if (modelInfo.get("offLineTagInfo") != null && map.get("offLineTagInfo") != null) {
               preList = JSONArray.fromObject(modelInfo.get("offLineTagInfo"));
               thisList = JSONArray.fromObject(String.valueOf(map.get("offLineTagInfo")));
            }

            preList.addAll(thisList);
            modelInfo.put("offLineTagInfo", JSON.toJSONString(preList));
            modelInfo.put("offLineTagName", map.get("offLineTagName") + "," + modelInfo.get("offLineTagName"));
            modelInfo.put("offLineTagId", map.get("offLineTagId") + "," + modelInfo.get("offLineTagId"));
            modelInfoMap.put(id, modelInfo);
         } else {
            modelInfoMap.put(id, map);
         }

         map.put("offLineTagId", modelInfoMap.get(id).get("offLineTagId"));
         map.put("offLineTagName", modelInfoMap.get(id).get("offLineTagName"));
         map.put("offLineTagInfo", modelInfoMap.get(id).get("offLineTagInfo"));
      }

      List<LinkedHashMap<String, Object>> newValues = new ArrayList<>();

      for (Entry<String, LinkedHashMap<String, Object>> entry : modelInfoMap.entrySet()) {
         newValues.add(entry.getValue());
      }

      return newValues;
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
         Map<String, DataResourceAuth> funcMap = this.dataAuthService.getDataAuths(request);

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

   public CustomHomePageDB getCustomHomePageDB() {
      return this.customHomePageDB;
   }

   public void setCustomHomePageDB(CustomHomePageDB customHomePageDB) {
      this.customHomePageDB = customHomePageDB;
   }

   public String getDimPro() {
      return this.dimPro;
   }

   public void setDimPro(String dimPro) {
      this.dimPro = dimPro;
   }

   public String getDimProValue() {
      return this.dimProValue;
   }

   public void setDimProValue(String dimProValue) {
      this.dimProValue = dimProValue;
   }

   public String getDimName() {
      return this.dimName;
   }

   public void setDimName(String dimName) {
      this.dimName = dimName;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public CommonData getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonData commonData) {
      this.commonData = commonData;
   }

   public ExcuteContext getExcuteContext() {
      return this.excuteContext;
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
   }
}
