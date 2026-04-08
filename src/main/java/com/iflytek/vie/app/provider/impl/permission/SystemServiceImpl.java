package com.iflytek.vie.app.provider.impl.permission;

import com.iflytek.vie.app.api.permission.SystemService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.pojo.permission.AccountRequest;
import com.iflytek.vie.app.pojo.permission.ResponseStatus;
import com.iflytek.vie.app.pojo.permission.SystemInfo;
import com.iflytek.vie.app.provider.common.HttpHelper;
import com.iflytek.vie.utils.JSONUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemServiceImpl implements SystemService {
   private String permissionURL;
   private static final Logger logger = LoggerFactory.getLogger(SystemServiceImpl.class);

   public List<SystemInfo> getAllSystemInfo() throws PermissionServiceException {
      StringBuffer url = new StringBuffer();
      url.append(this.permissionURL + "systemInfo/findAllSystemInfo");

      try {
         HttpHelper helper = new HttpHelper();
         String result = helper.get(url.toString(), "UTF-8");
         logger.info("查询所有系统信息接口:" + result);
         return JSONUtils.toList(result, SystemInfo.class);
      } catch (Exception var5) {
         logger.info("调用权限系统systemInfo/findAllSystemInfo出现异常");
         throw new PermissionServiceException("服务方异常", var5);
      }
   }

   public ResponseStatus findSystemInfoAuth(AccountRequest request) throws PermissionServiceException {
      ResponseStatus status = new ResponseStatus();
      if (null != request
         && null != request.getAccountName()
         && !"".equals(request.getAccountName())
         && null != request.getContextPath()
         && !"".equals(request.getContextPath())) {
         StringBuffer url = new StringBuffer();
         url.append(this.permissionURL + "systemInfo/findSystemInfoAuth");
         Map<String, String> params = new HashMap<>();
         ObjectMapper mapper = new ObjectMapper();

         try {
            params.put("accountName", String.valueOf(request.getAccountName()));
            params.put("contextPath", "/" + request.getContextPath());
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8", params);
            logger.info("调用根据用户账号和上下文查询是否有系统权限接口:" + result);
            Map<String, Object> data = (Map<String, Object>)mapper.readValue(result, Map.class);
            if (data != null && (Boolean)data.get("status")) {
               status.setStatus(true);
               return status;
            } else {
               status.setStatus(false);
               return status;
            }
         } catch (Exception var9) {
            logger.info("调用权限系统userAuth/getDataAuths出现异常");
            throw new PermissionServiceException("服务方异常", var9);
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
