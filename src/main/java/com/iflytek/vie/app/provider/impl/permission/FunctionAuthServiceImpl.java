package com.iflytek.vie.app.provider.impl.permission;

import com.alibaba.fastjson.JSONObject;
import com.iflytek.vie.app.api.permission.FunctionAuthService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.pojo.permission.ContextRequest;
import com.iflytek.vie.app.pojo.permission.FunctionAuthRequest;
import com.iflytek.vie.app.pojo.permission.FunctionAuthResponse;
import com.iflytek.vie.app.provider.common.HttpClientHelper;
import com.iflytek.vie.app.provider.common.HttpHelper;
import com.iflytek.vie.utils.JSONUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class FunctionAuthServiceImpl implements FunctionAuthService {
   private String permissionURL;
   @Autowired
   private Environment env;
   private static final Logger logger = LoggerFactory.getLogger(FunctionAuthServiceImpl.class);

   public List<FunctionAuthResponse> getFunctionResources(FunctionAuthRequest request) throws PermissionServiceException {
      if (null != request && null != request.getContextPath() && !"".equals(request.getContextPath()) && !StringUtils.isNullOrEmpry(request.getDataSource())) {
         StringBuffer url = new StringBuffer();
         url.append(this.permissionURL + "userResource/getFunctionResources");
         Map<String, String> params = new HashMap<>();

         try {
            params.put("userId", String.valueOf(request.getUserId()));
            params.put("contextPath", "/" + request.getContextPath());
            params.put("resourcePId", String.valueOf(request.getResourcePId()));
            params.put("businessCode", request.getDataSource());
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8", params);
            logger.info("调用获取用户拥有的一级菜单接口:" + result);
            Map<String, Object> data = JSONUtils.toHashMap(result);
            if (data == null || !(Boolean)data.get("status")) {
               return null;
            } else {
               return null == data.get("data") ? null : JSONUtils.toList(data.get("data"), FunctionAuthResponse.class);
            }
         } catch (Exception var8) {
            logger.info("调用权限系统userResource/getFunctionResources出现异常");
            throw new PermissionServiceException("服务方异常", var8);
         }
      } else {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      }
   }

   public Map<String, FunctionAuthResponse> getFuncAuths(ContextRequest request) throws PermissionServiceException {
      if (null != request && null != request.getContextPath() && !"".equals(request.getContextPath()) && !StringUtils.isNullOrEmpry(request.getDataSource())) {
         StringBuffer url = new StringBuffer();
         url.append(this.env.getProperty("permissionURL") + "userAuth/getFuncAuths");
         logger.info("权限地址=====" + url);
         Map<String, String> params = new HashMap<>();

         try {
            params.put("userId", String.valueOf(request.getUserId()));
            params.put("contextPath", "/" + request.getContextPath());
            params.put("businessCode", request.getDataSource());
            HttpClientHelper helper = new HttpClientHelper();
            String result = helper.get(url.toString(), "UTF-8", params);
            logger.info("调用获取用户拥有的一级菜单接口:" + result);
            return (LinkedHashMap)JSONObject.parseObject(result, LinkedHashMap.class);
         } catch (Exception var7) {
            logger.info("调用权限系统userAuth/getFuncAuths出现异常");
            throw new PermissionServiceException("服务方异常", var7);
         }
      } else {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      }
   }

   public void setPermissionURL(String permissionURL) {
      this.permissionURL = permissionURL;
   }
}
