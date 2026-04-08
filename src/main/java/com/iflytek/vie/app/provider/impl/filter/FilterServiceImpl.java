package com.iflytek.vie.app.provider.impl.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.iflytek.vie.app.api.datadrill.FilterAnaylseService;
import com.iflytek.vie.app.api.filter.FilterService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.common.ServiceResponse;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.DimensionValue;
import com.iflytek.vie.app.pojo.filter.FilterInfo;
import com.iflytek.vie.app.pojo.filter.FilterRequest;
import com.iflytek.vie.app.provider.database.FilterInfoDB;
import com.iflytek.vie.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterServiceImpl implements FilterService {
   private ServiceResponse serviceResponse = new ServiceResponse();
   private FilterInfoDB filterInfoDB;
   private final Logger logger = LoggerFactory.getLogger(FilterServiceImpl.class);
   private FilterAnaylseService filterAnaylseService;
   private static final String SQL = "sql";
   private static final String SUCCESS = "success";
   private static final String MESSAGE = "message";
   private static final String IS_MODEL = "isModel";
   private SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

   public ServiceResponse createOrUpdateFilterService(FilterRequest request) throws VieAppServiceException {
      this.serviceResponse = new ServiceResponse();
      FilterInfo filterInfo = new FilterInfo();

      try {
         if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("用户id不能为空");
            return this.serviceResponse;
         }

         String msg = "";
         filterInfo.setFlag(String.valueOf(request.getFlag()));
         if (0 == request.getFlag()) {
            filterInfo.setFilterId(request.getFilterId());
            msg = "删除";
            this.filterInfoDB.updateFlagFilterInfo(filterInfo);
         } else if (1 == request.getFlag()) {
            if (request.getFilterId() > 0L) {
               filterInfo.setFilterId(request.getFilterId());
               msg = "编辑";
            } else {
               filterInfo.setFilterId(0L);
               msg = "新建";
            }

            filterInfo.setFilterName(request.getName());
            filterInfo.setFilterType(request.getFilter());
            filterInfo.setUserId(request.getUserId());
            filterInfo.setDatabase(request.getDataSource());
            filterInfo.setCreateTime(new Date());
            com.iflytek.vie.app.pojo.datadrill.FilterRequest filterRequest = (com.iflytek.vie.app.pojo.datadrill.FilterRequest)JSON.parseObject(
               request.getFilter(), new TypeReference<com.iflytek.vie.app.pojo.datadrill.FilterRequest>() {}, new Feature[0]
            );
            filterRequest.setDataSource(request.getDataSource());
            Map<String, Object> filterMap = this.filterAnaylseService.fetchSqlByFilterDimension(filterRequest);
            if (filterMap != null && (Boolean)filterMap.get("success")) {
               filterInfo.setFilterSql((String)filterMap.get("sql"));
               filterInfo.setContainModel((Integer)filterMap.get("isModel"));
            } else if (filterMap != null && !(Boolean)filterMap.get("success")) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage(filterMap.get("message") != null ? (String)filterMap.get("message") : "");
               this.logger.error((String)filterMap.get("message"));
               return this.serviceResponse;
            }

            this.filterInfoDB.addOrUpdateFilterInfo(filterInfo, request.getDataSource());
         }

         this.serviceResponse.setSuccessful(true);
         this.serviceResponse.setMessage("筛选器" + msg + "成功");
      } catch (Exception var6) {
         this.logger.error("筛选器新增、编辑、删除接口响应异常", var6);
         throw new VieAppServiceException("接口内部异常", var6);
      }

      return this.serviceResponse;
   }

   public ServiceResponse queryFilterService(FilterRequest request) throws VieAppServiceException {
      this.serviceResponse = new ServiceResponse();
      FilterInfo filterInfo = new FilterInfo();

      try {
         if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("用户id不能为空");
            return this.serviceResponse;
         }

         if (request.getFilterId() <= 0L) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("筛选器ID错误");
            return this.serviceResponse;
         }

         if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("数据源为空");
            return this.serviceResponse;
         }

         filterInfo.setFilterId(request.getFilterId());
         FilterInfo filter = this.filterInfoDB.queryFilterInfo(filterInfo);
         this.serviceResponse.setValue(filter);
         this.serviceResponse.setSuccessful(true);
         this.serviceResponse.setMessage("筛选器查询成功");
      } catch (Exception var4) {
         this.logger.error("筛选器查询接口响应异常", var4);
         throw new VieAppServiceException("接口内部异常", var4);
      }

      return this.serviceResponse;
   }

   public ServiceResponse queryAllFilterService(FilterRequest request) throws VieAppServiceException {
      this.serviceResponse = new ServiceResponse();

      try {
         if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("用户id不能为空");
            return this.serviceResponse;
         }

         if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("数据源不能为空");
            return this.serviceResponse;
         }

         List<FilterInfo> filterInfoList = this.filterInfoDB.queryFilterInfo(request.getUserId(), request.getDataSource());
         this.serviceResponse.setValue(filterInfoList);
         this.serviceResponse.setSuccessful(true);
         this.serviceResponse.setMessage("筛选器查询成功");
      } catch (Exception var3) {
         this.serviceResponse.setSuccessful(false);
         this.logger.error("查询所有筛选器接口响应异常", var3);
         throw new VieAppServiceException("接口内部异常", var3);
      }

      return this.serviceResponse;
   }

   public ServiceResponse checkFilterNameService(FilterRequest request) throws VieAppServiceException {
      try {
         if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("用户id不能为空");
            return this.serviceResponse;
         }

         if (request.getFilterId() < 0L) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("筛选器ID错误");
            return this.serviceResponse;
         }

         if (StringUtils.isNullOrEmpry(request.getName())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("筛选器名称不能为空");
            return this.serviceResponse;
         }

         FilterInfo filterInfo = new FilterInfo();
         filterInfo.setFilterId(request.getFilterId());
         filterInfo.setFilterName(request.getName());
         filterInfo.setUserId(request.getUserId());
         filterInfo.setDatabase(request.getDataSource());
         boolean repeat = this.filterInfoDB.checkFilterName(filterInfo);
         if (repeat) {
            this.serviceResponse.setSuccessful(true);
            this.serviceResponse.setMessage("筛选器名称正常");
         } else {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("筛选器名称重复");
         }
      } catch (Exception var4) {
         this.serviceResponse.setSuccessful(false);
         this.logger.error("筛选器名称校验接口响应异常", var4);
         throw new VieAppServiceException("接口内部异常", var4);
      }

      return this.serviceResponse;
   }

   public ServiceResponse queryDimandModelService(FilterRequest request) throws VieAppServiceException {
      this.logger.info("获取所有系统维度接口(筛选器模块)开始响应");
      this.serviceResponse = new ServiceResponse();

      try {
         new ArrayList();
         new ArrayList();
         List<HashMap<String, Object>> resultHm = new ArrayList<>();
         List<DimensionConfig> e = this.filterInfoDB.queryDimandModelService(request.getDataSource(), null);
         if (e == null) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("维度不存在");
            return this.serviceResponse;
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

         this.serviceResponse.setSuccessful(true);
         this.serviceResponse.setMessage("查询成功");
         this.serviceResponse.setValue(resultHm);
      } catch (Exception var10) {
         this.logger.error("获取所有系统维度接口(筛选器模块)响应异常", var10);
         throw new VieAppServiceException("获取所有系统维度接口(筛选器模块)响应异常", var10);
      }

      this.logger.info("获取所有系统维度接口(筛选器模块   )服务响应成功");
      return this.serviceResponse;
   }

   public void setFilterInfoDB(FilterInfoDB filterInfoDB) {
      this.filterInfoDB = filterInfoDB;
   }

   public void setFilterAnaylseService(FilterAnaylseService filterAnaylseService) {
      this.filterAnaylseService = filterAnaylseService;
   }
}
