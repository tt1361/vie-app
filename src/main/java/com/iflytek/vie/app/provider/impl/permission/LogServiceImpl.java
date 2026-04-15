package com.iflytek.vie.app.provider.impl.permission;

import com.iflytek.vie.app.api.permission.LogService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.pojo.permission.LoginLog;
import com.iflytek.vie.app.pojo.permission.OperationLog;
import com.iflytek.vie.app.pojo.permission.ResponseStatus;
import com.iflytek.vie.app.pojo.permission.UpdateLoginLog;
import com.iflytek.vie.app.provider.common.HttpHelper;
import java.io.IOException;
import java.net.URLEncoder;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogServiceImpl implements LogService {
   private String permissionURL;
   private static final Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);

   public ResponseStatus saveOperationLog(OperationLog log) throws PermissionServiceException {
      ResponseStatus status = new ResponseStatus();
      if (null != log
         && log.getMenuName() != null
         && !"".equals(log.getMenuName())
         && log.getOperateStatus() != null
         && !"".equals(log.getOperateStatus().toString())
         && log.getOperateAccount() != null
         && !"".equals(log.getOperateAccount())) {
         ObjectMapper mapper = new ObjectMapper();

         try {
            String sendJson = mapper.writeValueAsString(log);
            String url = this.permissionURL + "log/saveOperationLog/" + URLEncoder.encode(sendJson, "UTF-8");
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8");
            if (null != result && "success".equals(result)) {
               status.setStatus(true);
               return status;
            }
         } catch (JsonGenerationException var8) {
            logger.error("错误原因", var8);
         } catch (JsonMappingException var9) {
            logger.error("错误原因", var9);
         } catch (IOException var10) {
            logger.error("错误原因", var10);
         }

         status.setStatus(false);
         return status;
      } else {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      }
   }

   public ResponseStatus saveLoginLog(LoginLog log) throws PermissionServiceException {
      ResponseStatus status = new ResponseStatus();
      if (null != log
         && log.getAccountName() != null
         && !"".equals(log.getAccountName())
         && log.getLoginTime() != null
         && log.getReverse1() != null
         && log.getLoginStatus() != null) {
         ObjectMapper mapper = new ObjectMapper();

         try {
            String sendJson = mapper.writeValueAsString(log);
            String url = this.permissionURL + "log/saveLoginLog/" + URLEncoder.encode(sendJson, "UTF-8");
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8");
            if (null != result && "success".equals(result)) {
               status.setStatus(true);
               return status;
            }
         } catch (JsonGenerationException var8) {
            logger.error("错误原因", var8);
         } catch (JsonMappingException var9) {
            logger.error("错误原因", var9);
         } catch (IOException var10) {
            logger.error("错误原因", var10);
         }

         status.setStatus(false);
         return status;
      } else {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      }
   }

   public ResponseStatus updateLoginLog(UpdateLoginLog log) throws PermissionServiceException {
      ResponseStatus status = new ResponseStatus();
      if (null != log && log.getSessionId() != null && !"".equals(log.getSessionId()) && log.getLogoutTime() != null) {
         try {
            String url = this.permissionURL
               + "log/updateLoginLog/"
               + URLEncoder.encode(log.getSessionId(), "UTF-8")
               + "/"
               + URLEncoder.encode(String.valueOf(log.getLogoutTime()), "UTF-8");
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8");
            if (null != result && "success".equals(result)) {
               status.setStatus(true);
               return status;
            }
         } catch (IOException var6) {
            logger.error("错误原因", var6);
         }

         status.setStatus(false);
         return status;
      } else {
         logger.info("入参不正确");
         throw new PermissionServiceException("入参不正确");
      }
   }

   public void setPermissionURL(String permissionURL) {
      this.permissionURL = permissionURL;
   }
}
