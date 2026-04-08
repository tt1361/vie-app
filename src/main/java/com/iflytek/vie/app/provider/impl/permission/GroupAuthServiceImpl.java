package com.iflytek.vie.app.provider.impl.permission;

import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.pojo.permission.ResponseStatus;
import com.iflytek.vie.app.provider.common.HttpClientHelper;
import com.iflytek.vie.app.provider.common.HttpHelper;
import com.iflytek.vie.utils.JSONUtils;
import com.iflytek.vie.utils.StringUtils;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupAuthServiceImpl implements GroupAuthService {
   private String permissionURL;
   private static final Logger logger = LoggerFactory.getLogger(GroupAuthServiceImpl.class);

   public List<Long> queryGroupAuth(GroupAuthRequest groupAuthRequest) throws PermissionServiceException {
      logger.info("开始调用获取模型组、报表组、质检项、业务项权限接口【queryGroupAuth】");
      List<Long> groupList = new ArrayList<>();
      if (groupAuthRequest != null
         && groupAuthRequest.getUserId() != 0L
         && groupAuthRequest.getGroupType() != 0
         && !StringUtils.isNullOrEmpry(groupAuthRequest.getDataSource())) {
         StringBuffer url = new StringBuffer();
         url.append(this.permissionURL + "userAuth/getGroupAuthByType");
         Map<String, String> params = new HashMap<>();

         try {
            logger.info(
               "获取模型组、报表组、质检项、业务项权限接口【queryGroupAuth】参数：userId="
                  + String.valueOf(groupAuthRequest.getUserId())
                  + ", groupType="
                  + groupAuthRequest.getGroupType()
                  + ", businessCode="
                  + groupAuthRequest.getDataSource()
            );
            params.put("userId", String.valueOf(groupAuthRequest.getUserId()));
            params.put("groupType", String.valueOf(groupAuthRequest.getGroupType()));
            params.put("businessCode", groupAuthRequest.getDataSource());
            HttpClientHelper helper = new HttpClientHelper();
            String result = helper.get(url.toString(), "UTF-8", params);
            logger.info("调用获取模型组、报表组、质检项、业务项权限接口返回数据:" + result);
            Map<String, Object> data = JSONUtils.toHashMap(result);
            if (data != null && (Boolean)data.get("status")) {
               groupList = JSONUtils.toList(data.get("data").toString(), Long.class);
            }

            return groupList;
         } catch (Exception var8) {
            logger.error("调用权限系统userAuth/queryGroupAuth返回数据解析时出错", var8);
            throw new PermissionServiceException("服务方异常", var8);
         }
      } else {
         throw new PermissionServiceException("输入参数异常");
      }
   }

   public ResponseStatus addGroupAuth(GroupAuthRequest request) throws PermissionServiceException {
      ResponseStatus status = new ResponseStatus();
      if (request == null
         || request.getUserId() == 0L
         || request.getGroupId() == null
         || "".equals(request.getGroupId().toString())
         || request.getGroupName() == null
         || "".equals(request.getGroupName())
         || request.getGroupType() == 0
         || StringUtils.isNullOrEmpry(request.getDataSource())) {
         throw new PermissionServiceException("输入参数异常");
      } else if (String.valueOf(request.getGroupId()).length() > 20) {
         throw new PermissionServiceException("groupId长度不能大于20");
      } else {
         StringBuffer url = new StringBuffer();
         url.append(this.permissionURL + "userAuth/addGroupAuths");
         Map<String, String> params = new HashMap<>();
         ObjectMapper mapper = new ObjectMapper();

         try {
            params.put("userId", String.valueOf(request.getUserId()));
            params.put("groupName", URLEncoder.encode(request.getGroupName(), "UTF-8"));
            params.put("groupId", String.valueOf(request.getGroupId()));
            params.put("groupType", String.valueOf(request.getGroupType()));
            params.put("businessCode", request.getDataSource());
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8", params);
            logger.info("调用新增模型组、报表组、质检项、业务项权限接口:" + result);
            Map<String, Object> data = (Map<String, Object>)mapper.readValue(result, Map.class);
            if (data != null && (Boolean)data.get("status")) {
               status.setStatus(true);
               return status;
            } else {
               status.setStatus(false);
               return status;
            }
         } catch (Exception var9) {
            logger.error("新增数据异常", var9);
            throw new PermissionServiceException("新增数据异常", var9);
         }
      }
   }

   public ResponseStatus updateGroupAuth(GroupAuthRequest request) throws PermissionServiceException {
      ResponseStatus status = new ResponseStatus();
      if (request != null
         && request.getGroupType() != 0
         && request.getGroupId() != null
         && !"".equals(request.getGroupId().toString())
         && request.getGroupName() != null
         && !"".equals(request.getGroupName())
         && !StringUtils.isNullOrEmpry(request.getDataSource())) {
         if (String.valueOf(request.getGroupId()).length() > 20) {
            throw new PermissionServiceException("groupId长度不能大于20");
         } else {
            StringBuffer url = new StringBuffer();
            url.append(this.permissionURL + "userAuth/updateGroupAuths");
            Map<String, String> params = new HashMap<>();
            ObjectMapper mapper = new ObjectMapper();

            try {
               params.put("userId", String.valueOf(request.getUserId()));
               params.put("groupName", URLEncoder.encode(request.getGroupName(), "UTF-8"));
               params.put("groupId", String.valueOf(request.getGroupId()));
               params.put("groupType", String.valueOf(request.getGroupType()));
               params.put("businessCode", request.getDataSource());
               HttpHelper helper = new HttpHelper();
               String result = helper.get(url.toString(), "UTF-8", params);
               logger.info("调用更新模型组、报表组、质检项、业务项权限接口:" + result);
               Map<String, Object> data = (Map<String, Object>)mapper.readValue(result, Map.class);
               if (data != null && (Boolean)data.get("status")) {
                  status.setStatus(true);
                  return status;
               } else {
                  status.setStatus(false);
                  return status;
               }
            } catch (Exception var9) {
               logger.error("更新数据异常", var9);
               throw new PermissionServiceException("更新数据异常", var9);
            }
         }
      } else {
         throw new PermissionServiceException("输入参数异常");
      }
   }

   public ResponseStatus deleteGroupAuth(GroupAuthRequest request) throws PermissionServiceException {
      ResponseStatus status = new ResponseStatus();
      if (request != null
         && request.getGroupType() != 0
         && request.getGroupIds() != null
         && request.getGroupIds().size() >= 0
         && !StringUtils.isNullOrEmpry(request.getDataSource())) {
         StringBuffer url = new StringBuffer();
         url.append(this.permissionURL + "userAuth/delGroupAuths");
         Map<String, String> params = new HashMap<>();
         ObjectMapper mapper = new ObjectMapper();

         try {
            StringBuffer groupIds = new StringBuffer();

            for (int i = 0; i < request.getGroupIds().size(); i++) {
               groupIds.append(String.valueOf(request.getGroupIds().get(i)));
               if (i < request.getGroupIds().size() - 1) {
                  groupIds.append(",");
               }
            }

            params.put("groupIds", groupIds.toString());
            params.put("groupType", String.valueOf(request.getGroupType()));
            params.put("businessCode", request.getDataSource());
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8", params);
            logger.info("调用删除模型组、报表组、质检项、业务项权限接口:" + result);
            Map<String, Object> data = (Map<String, Object>)mapper.readValue(result, Map.class);
            if (data != null && (Boolean)data.get("status")) {
               status.setStatus(true);
               return status;
            } else {
               status.setStatus(false);
               return status;
            }
         } catch (Exception var10) {
            logger.error("删除数据异常", var10);
            throw new PermissionServiceException("删除数据异常", var10);
         }
      } else {
         throw new PermissionServiceException("输入参数异常");
      }
   }

   public void setPermissionURL(String permissionURL) {
      this.permissionURL = permissionURL;
   }
}
