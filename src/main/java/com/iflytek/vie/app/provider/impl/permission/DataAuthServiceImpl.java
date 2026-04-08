package com.iflytek.vie.app.provider.impl.permission;

import com.alibaba.fastjson.JSONObject;
import com.iflytek.vie.app.api.permission.DataAuthService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.pojo.permission.ContextRequest;
import com.iflytek.vie.app.pojo.permission.DataResourceAuth;
import com.iflytek.vie.app.provider.common.HttpClientHelper;
import com.iflytek.vie.utils.JSONUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class DataAuthServiceImpl implements DataAuthService {
   private String permissionURL;
   @Autowired
   private Environment env;
   private static final Logger logger = LoggerFactory.getLogger(DataAuthServiceImpl.class);

   public Map<String, DataResourceAuth> getDataAuths(ContextRequest request) throws PermissionServiceException {
      if (null != request && null != request.getContextPath() && !"".equals(request.getContextPath()) && !StringUtils.isNullOrEmpry(request.getDataSource())) {
         StringBuffer url = new StringBuffer();
         String pl = this.env.getProperty("permissionURL");
         logger.info("权限地址======：" + pl);
         url.append(pl + "userAuth/getDataAuths");
         Map<String, String> params = new HashMap<>();

         try {
            params.put("dataSource", request.getDataSource());
            params.put("userId", String.valueOf(request.getUserId()));
            params.put("contextPath", "/" + request.getContextPath());
            params.put("businessCode", request.getDataSource());
            HttpClientHelper helper = new HttpClientHelper();
            String result = helper.get(url.toString(), "UTF-8", params);
            logger.info("调用获取用户拥有的数据权限接口:" + result);
            Map<String, Object> data = JSONUtils.toHashMap(result);
            if (data == null || !(Boolean)data.get("status")) {
               return null;
            } else if (null == data.get("data")) {
               return null;
            } else {
               Map<String, Object> func = JSONUtils.toHashMap(data.get("data").toString());
               Map<String, DataResourceAuth> funcMap = new HashMap<>();

               for (Entry<String, Object> entry : func.entrySet()) {
                  funcMap.put(entry.getKey(), (DataResourceAuth)JSONObject.parseObject(entry.getValue().toString(), DataResourceAuth.class));
               }

               return funcMap;
            }
         } catch (Exception var12) {
            logger.info("调用权限系统userAuth/getDataAuths出现异常");
            throw new PermissionServiceException("服务方异常", var12);
         }
      } else {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      }
   }

   public List<String> getSystemBusiness(String context) throws PermissionServiceException {
      if (StringUtils.isNullOrEmpry(context)) {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      } else {
         StringBuffer url = new StringBuffer();
         url.append(this.permissionURL + "userResource/getSystemAllBusiness");
         Map<String, String> params = new HashMap<>();

         try {
            params.put("contextPath", "/" + context);
            HttpClientHelper helper = new HttpClientHelper();
            String result = helper.get(url.toString(), "UTF-8", params);
            logger.info("调用获取系统下所有业务接口:" + result);
            Map<String, Object> data = JSONUtils.toHashMap(result);
            if (data == null || !(Boolean)data.get("status")) {
               return null;
            } else {
               return null == data.get("data") ? null : (List)data.get("data");
            }
         } catch (Exception var8) {
            logger.info("调用权限系统userResource/getSystemAllBusiness出现异常");
            throw new PermissionServiceException("服务方异常", var8);
         }
      }
   }

   public Map<String, String> getResDimDTOByBusiness(String dataSource) throws PermissionServiceException {
      if (StringUtils.isNullOrEmpry(dataSource)) {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      } else {
         StringBuffer url = new StringBuffer();
         url.append(this.permissionURL + "userResource/getResDimDTOByBusiness");
         Map<String, String> params = new HashMap<>();

         try {
            params.put("businessCode", dataSource);
            HttpClientHelper helper = new HttpClientHelper();
            String result = helper.get(url.toString(), "UTF-8", params);
            logger.info("调用获取业务对应的维度信息:" + result);
            Map<String, Object> data = JSONUtils.toHashMap(result);
            new HashMap();
            if (data == null || !(Boolean)data.get("status")) {
               return null;
            } else {
               return null == data.get("data") ? null : (Map)data.get("data");
            }
         } catch (Exception var8) {
            logger.info("调用权限系统userResource/getResDimDTOByBusiness出现异常");
            throw new PermissionServiceException("服务方异常", var8);
         }
      }
   }

   public void setPermissionURL(String permissionURL) {
      this.permissionURL = permissionURL;
   }
}
