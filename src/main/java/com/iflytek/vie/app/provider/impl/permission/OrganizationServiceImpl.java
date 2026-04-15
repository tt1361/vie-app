package com.iflytek.vie.app.provider.impl.permission;

import com.iflytek.vie.app.api.permission.OrganizationService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.pojo.permission.OrganizationInfo;
import com.iflytek.vie.app.provider.common.HttpHelper;
import com.iflytek.vie.utils.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrganizationServiceImpl implements OrganizationService {
   private String permissionURL;
   private static final Logger logger = LoggerFactory.getLogger(OrganizationServiceImpl.class);

   public OrganizationInfo findOrgByUserId(Long userId) throws PermissionServiceException {
      if (userId == null) {
         logger.info("入参有误");
         throw new PermissionServiceException("入参有误");
      } else {
         StringBuffer url = new StringBuffer();
         url.append(this.permissionURL + "organization/findOrgByUserId?userId=" + userId);

         try {
            HttpHelper helper = new HttpHelper();
            String result = helper.get(url.toString(), "UTF-8");
            return result != null && !"".equals(result) ? (OrganizationInfo)JSONUtils.toBean(result, OrganizationInfo.class) : null;
         } catch (Exception var6) {
            logger.error("organization/findOrgByUserId interface exception");
            throw new PermissionServiceException("服务方异常", var6);
         }
      }
   }

   public void setPermissionURL(String permissionURL) {
      this.permissionURL = permissionURL;
   }
}
