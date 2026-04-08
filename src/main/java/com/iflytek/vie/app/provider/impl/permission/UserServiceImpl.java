package com.iflytek.vie.app.provider.impl.permission;

import com.alibaba.fastjson.JSONObject;
import com.iflytek.vie.app.api.permission.UserService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.pojo.permission.DataSourceResponse;
import com.iflytek.vie.app.pojo.permission.LoginRequest;
import com.iflytek.vie.app.pojo.permission.ResponseStatus;
import com.iflytek.vie.app.pojo.permission.RoleInfo;
import com.iflytek.vie.app.pojo.permission.UpdatePwdRequest;
import com.iflytek.vie.app.pojo.permission.UserBasicInfo;
import com.iflytek.vie.app.pojo.permission.UserInfoRequest;
import com.iflytek.vie.app.provider.common.HttpClientHelper;
import com.iflytek.vie.app.provider.common.HttpHelper;
import com.iflytek.vie.app.provider.common.MD5Util;
import com.iflytek.vie.utils.JSONUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class UserServiceImpl implements UserService {
   private String permissionURL;
   @Autowired
   private Environment env;
   private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

   public ResponseStatus updatePwd(UpdatePwdRequest request) throws PermissionServiceException {
      ResponseStatus status = new ResponseStatus();
      if (request != null
         && request.getUserId() != 0L
         && request.getNewPassword() != null
         && !request.getNewPassword().equals("")
         && request.getOldPassword() != null
         && !request.getOldPassword().equals("")) {
         String newPassword = request.getNewPassword();
         String oldPassword = request.getOldPassword();
         if (newPassword.length() > 20 || newPassword.length() < 6 || oldPassword.length() > 20 || oldPassword.length() < 6) {
            logger.info("密码不能少于6位且不能大于20位");
            throw new PermissionServiceException("密码不能少于6位且不能大于20位");
         } else if (newPassword.indexOf(" ") < 0 && oldPassword.indexOf(" ") < 0) {
            StringBuffer url = new StringBuffer();
            url.append(this.permissionURL + "user/updatePwd");
            Map<String, String> params = new HashMap<>();
            ObjectMapper mapper = new ObjectMapper();

            try {
               params.put("userId", String.valueOf(request.getUserId()));
               params.put("newPassword", MD5Util.md5(newPassword));
               params.put("oldPassword", MD5Util.md5(oldPassword));
               HttpHelper helper = new HttpHelper();
               String result = helper.get(url.toString(), "UTF-8", params);
               logger.info("调用修改密码接口:" + result);
               Map<String, Object> data = (Map<String, Object>)mapper.readValue(result, Map.class);
               if (data != null && (Boolean)data.get("status")) {
                  status.setStatus(true);
                  return status;
               } else {
                  status.setStatus(false);
                  return status;
               }
            } catch (Exception var11) {
               status.setStatus(false);
               return status;
            }
         } else {
            logger.info("密码不能有空格");
            throw new PermissionServiceException("密码不能有空格");
         }
      } else {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      }
   }

   public List<RoleInfo> findRolesByAccountName(UserInfoRequest request) throws PermissionServiceException {
      List<RoleInfo> roleList = new ArrayList<>();
      if (request != null && request.getAccountName() != null && !"".equals(request.getAccountName()) && !StringUtils.isNullOrEmpry(request.getDataSource())) {
         StringBuffer url = new StringBuffer();
         url.append(
            this.permissionURL + "user/findRoleInfosByAccountName?accountName=" + request.getAccountName().trim() + "&businessCode=" + request.getDataSource()
         );

         try {
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8");
            logger.info("查询用户下的角色信息接口:" + result);
            Map<String, Object> data = JSONUtils.toHashMap(result);
            if (data != null && (Boolean)data.get("status")) {
               roleList = JSONUtils.toList(data.get("data").toString(), RoleInfo.class);
            }

            return roleList;
         } catch (Exception var7) {
            logger.info("调用权限系统user/findRoleInfosByAccountName出现异常");
            throw new PermissionServiceException("服务方异常", var7);
         }
      } else {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      }
   }

   public Map<Integer, Map<String, String>> queryAllUserInfo(String dataSource) throws PermissionServiceException {
      Map<Integer, Map<String, String>> resultMap = null;
      if (StringUtils.isNullOrEmpry(dataSource)) {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      } else {
         StringBuffer url = new StringBuffer();
         url.append(this.permissionURL + "user/queryAllUserInfo?businessCode=" + dataSource);

         try {
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8");
            logger.info("获取所有用户信息接口:" + result);
            return (Map<Integer, Map<String, String>>)JSONObject.parseObject(result, HashMap.class);
         } catch (Exception var6) {
            logger.error("调用权限系统user/queryAllUserInfo获取所有用户信息返回数据解析时出错");
            throw new PermissionServiceException("服务方异常", var6);
         }
      }
   }

   public UserBasicInfo checkLogin(LoginRequest request) throws PermissionServiceException {
      if (null != request
         && null != request.getAccountName()
         && !"".equals(request.getAccountName())
         && null != request.getPassword()
         && !"".equals(request.getPassword())) {
         StringBuffer url = new StringBuffer();

         try {
            String b = this.env.getProperty("permissionURL");
            url.append(this.permissionURL + "user/checkLogin/" + request.getAccountName() + "/" + request.getPassword());
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8");
            logger.info("查询用户下的角色信息接口:" + result);
            Map<String, Object> data = JSONUtils.toHashMap(result);
            if (data != null && (Boolean)data.get("status")) {
               Map<String, Object> data1 = JSONUtils.toHashMap(data.get("data"));
               data1.remove("org");
               logger.info("查询用户下的角色信息接口data1信息:" + data1);
               return (UserBasicInfo)JSONUtils.toBean(data1, UserBasicInfo.class);
            } else {
               return null;
            }
         } catch (Exception var9) {
            logger.error("调用权限系统user/checkLogin接口出错");
            throw new PermissionServiceException("服务方异常", var9);
         }
      } else {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      }
   }

   public List<DataSourceResponse> getUserAllBusiness(String contextPath, String userId) throws PermissionServiceException {
      if (!StringUtils.isNullOrEmpry(contextPath) && !StringUtils.isNullOrEmpry(userId)) {
         StringBuffer url = new StringBuffer();
         url.append(this.permissionURL + "userResource/getUserAllBusiness");
         Map<String, String> params = new HashMap<>();

         try {
            params.put("userId", userId);
            params.put("contextPath", "/" + contextPath);
            HttpClientHelper helper = new HttpClientHelper();
            String result = helper.get(url.toString(), "UTF-8", params);
            logger.info("调用获取用户在系统下拥有业务接口:" + result);
            Map<String, Object> data = JSONUtils.toHashMap(result);
            if (data == null || !(Boolean)data.get("status")) {
               return null;
            } else {
               return null == data.get("data") ? null : JSONUtils.toList(data.get("data"), DataSourceResponse.class);
            }
         } catch (Exception var9) {
            logger.info("调用权限系统userResource/getUserAllBusiness出现异常");
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
