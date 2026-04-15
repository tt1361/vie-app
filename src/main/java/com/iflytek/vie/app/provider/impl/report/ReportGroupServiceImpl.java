package com.iflytek.vie.app.provider.impl.report;

import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.api.permission.UserService;
import com.iflytek.vie.app.api.report.ReportGroupService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.auth.AuthorizeInfo;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.pojo.permission.RoleInfo;
import com.iflytek.vie.app.pojo.permission.UserInfoRequest;
import com.iflytek.vie.app.pojo.report.ReportGroup;
import com.iflytek.vie.app.pojo.report.ReportGroupIdRequest;
import com.iflytek.vie.app.pojo.report.ReportGroupRequest;
import com.iflytek.vie.app.pojo.report.ReportGroupResponse;
import com.iflytek.vie.app.provider.database.ReportGroupDB;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportGroupServiceImpl implements ReportGroupService {
   private final Logger logger = LoggerFactory.getLogger(ReportGroupServiceImpl.class);
   private GroupAuthService groupAuthService;
   private ReportGroupDB reportGroupDB;
   private UserService userService;

   public List<ReportGroupResponse> queryReportGroup(AuthorizeInfo authorizeInfo) throws VieAppServiceException {
      if (authorizeInfo.getSystemId() != null
         && !"".equals(authorizeInfo.getSystemId())
         && authorizeInfo.getUserId() != null
         && !"".equals(authorizeInfo.getUserId())) {
         List<ReportGroupResponse> reportGroupResponses = new ArrayList<>();

         try {
            ReportGroupIdRequest reportGroupIdRequest = new ReportGroupIdRequest();
            reportGroupIdRequest.setGroupType(2);
            reportGroupIdRequest.setUserId(Long.valueOf(authorizeInfo.getUserId()));
            reportGroupIdRequest.setDataSource(authorizeInfo.getDataSource());
            List<Long> groupIds = this.getGroupIdList(reportGroupIdRequest);
            if (null != groupIds && groupIds.size() > 0) {
               List<ReportGroup> groups = this.reportGroupDB.fetchGroupsByIds(groupIds, null);

               for (int i = 0; i < groups.size(); i++) {
                  ReportGroupResponse reportGroupResponse = new ReportGroupResponse();
                  ReportGroup reportGroup = groups.get(i);
                  reportGroupResponse.setId(reportGroup.getId());
                  reportGroupResponse.setName(reportGroup.getName());
                  reportGroupResponse.setEditFlag(reportGroup.getIsEdit());
                  reportGroupResponses.add(reportGroupResponse);
               }
            }

            return reportGroupResponses;
         } catch (Exception var9) {
            this.logger.error("查询报表组调用queryReportGroup接口出错", var9);
            throw new VieAppServiceException("查询报表组异常", var9);
         }
      } else {
         this.logger.error("输入参数异常");
         throw new VieAppServiceException("输入参数异常");
      }
   }

   public ReportGroup getReportGroupById(ReportGroupRequest reportGroupRequest) throws VieAppServiceException {
      if (reportGroupRequest.getReportGroupId() == 0) {
         this.logger.error("输入参数异常");
         throw new VieAppServiceException("输入参数异常");
      } else {
         try {
            return this.reportGroupDB.fetchReportGroupById(reportGroupRequest.getReportGroupId());
         } catch (Exception var4) {
            this.logger.error("根据报表组id查询报表组信息接口getReportGroupById出错", var4);
            throw new VieAppServiceException("根据报表组id查询报表组出错", var4);
         }
      }
   }

   public void addReportGroup(ReportGroupRequest reportGroupRequest) throws VieAppServiceException {
      if (!StringUtils.isNullOrEmpry(reportGroupRequest.getSystemId())
         && !StringUtils.isNullOrEmpry(reportGroupRequest.getUserId())
         && !StringUtils.isNullOrEmpry(reportGroupRequest.getReportGroupName())
         && !StringUtils.isNullOrEmpry(reportGroupRequest.getDataSource())) {
         List<ReportGroup> groups = null;
         List<Long> roleIds = new ArrayList<>();

         try {
            UserInfoRequest userInfoRequest = new UserInfoRequest();
            userInfoRequest.setAccountName(reportGroupRequest.getUserName());
            userInfoRequest.setDataSource(reportGroupRequest.getDataSource());

            for (RoleInfo roleInfo : this.userService.findRolesByAccountName(userInfoRequest)) {
               roleIds.add(roleInfo.getId());
            }

            groups = this.reportGroupDB.fetchReportGroupByRole(reportGroupRequest.getReportGroupName(), roleIds);
         } catch (Exception var9) {
            this.logger.error("新建报表组接口查询是否已经存在同名报表组出错", var9);
            throw new VieAppServiceException("新建报表组接口查询是否已经存在同名报表组出错", var9);
         }

         if (groups.size() <= 0 && !CommonParams.reportGroupName.equals(reportGroupRequest.getReportGroupName())) {
            try {
               List<ReportGroup> rgs = new ArrayList<>();

               for (Long role : roleIds) {
                  ReportGroup rg = new ReportGroup();
                  rg.setCreateUser(reportGroupRequest.getUserId());
                  rg.setName(reportGroupRequest.getReportGroupName());
                  rg.setIsEdit(1);
                  rg.setRoleId(role);
                  rgs.add(rg);
               }

               if (rgs.size() > 0 && this.reportGroupDB.saveOrUpdate(rgs)) {
                  GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
                  groupAuthRequest.setGroupId((long)rgs.get(0).getId());
                  groupAuthRequest.setGroupType(2);
                  groupAuthRequest.setGroupName(rgs.get(0).getName());
                  groupAuthRequest.setUserId(Long.valueOf(reportGroupRequest.getUserId()));
                  groupAuthRequest.setDataSource(reportGroupRequest.getDataSource());
                  if (!this.groupAuthService.addGroupAuth(groupAuthRequest).isStatus()) {
                     this.logger.error("新增报表组时调用权限系统保存组关系失败");
                     throw new VieAppServiceException("新增报表组时调用权限系统保存组关系失败");
                  }
               }
            } catch (Exception var8) {
               this.logger.error("新建报表组接口addReportGroup出错", var8);
               throw new VieAppServiceException("新建报表组接口出错", var8);
            }
         } else {
            this.logger.error("该报表组名称已经存在");
            throw new VieAppServiceException("该报表组名称已经存在");
         }
      } else {
         this.logger.error("输入参数异常");
         throw new VieAppServiceException("输入参数异常");
      }
   }

   public void deleteReportGroup(ReportGroupRequest reportGroupRequest) throws VieAppServiceException {
      try {
         if (!StringUtils.isNullOrEmpry(reportGroupRequest.getSystemId())
            && !StringUtils.isNullOrEmpry(reportGroupRequest.getUserId())
            && reportGroupRequest.getReportGroupId() != 0
            && !StringUtils.isNullOrEmpry(reportGroupRequest.getDataSource())) {
            ReportGroup reportGroup = this.reportGroupDB.fetchReportGroupById(reportGroupRequest.getReportGroupId());
            if (reportGroup == null) {
               this.logger.error("该报表组已被删除");
               throw new VieAppServiceException("该报表组已被删除");
            } else {
               boolean status = this.reportGroupDB.deleteReportById(String.valueOf(reportGroupRequest.getReportGroupId()));
               if (status) {
                  GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
                  List<Long> groupIds = new ArrayList<>();
                  groupIds.add((long)reportGroup.getId());
                  groupAuthRequest.setGroupType(2);
                  groupAuthRequest.setGroupIds(groupIds);
                  groupAuthRequest.setDataSource(reportGroupRequest.getDataSource());
                  if (!this.groupAuthService.deleteGroupAuth(groupAuthRequest).isStatus()) {
                     this.logger.error("删除报表组时调用权限系统删除组关系失败");
                     throw new VieAppServiceException("删除报表组时调用权限系统删除组关系失败");
                  }
               }
            }
         } else {
            this.logger.error("输入参数异常");
            throw new VieAppServiceException("输入参数异常");
         }
      } catch (Exception var6) {
         this.logger.error("删除报表组接口deleteReportGroup出错", var6);
         throw new VieAppServiceException("删除报表组接口出错", var6);
      }
   }

   public void updateReportGroup(ReportGroupRequest reportGroupRequest) throws VieAppServiceException {
      if (reportGroupRequest.getReportGroupId() != 0
         && !StringUtils.isNullOrEmpry(reportGroupRequest.getReportGroupName())
         && !StringUtils.isNullOrEmpry(reportGroupRequest.getSystemId())
         && !StringUtils.isNullOrEmpry(reportGroupRequest.getUserId())
         && !StringUtils.isNullOrEmpry(reportGroupRequest.getDataSource())) {
         boolean hasCurrentReportId = false;
         List<String> names = new ArrayList<>();
         ReportGroup rg = null;

         try {
            UserInfoRequest request = new UserInfoRequest();
            request.setAccountName(reportGroupRequest.getUserName());
            request.setDataSource(reportGroupRequest.getDataSource());
            List<RoleInfo> roles = this.userService.findRolesByAccountName(request);
            List<Long> list = new ArrayList<>();
            if (null != roles && roles.size() != 0) {
               for (RoleInfo r : roles) {
                  list.add(r.getId());
               }
            }

            List<ReportGroup> totalReportGroup = this.reportGroupDB.fetchReportGroup(list);
            rg = this.reportGroupDB.fetchReportGroupById(reportGroupRequest.getReportGroupId());

            for (ReportGroup reportGroup : totalReportGroup) {
               names.add(reportGroup.getName());
               if (reportGroup.getId() == reportGroupRequest.getReportGroupId() && !hasCurrentReportId) {
                  hasCurrentReportId = true;
               }
            }
         } catch (Exception var12) {
            this.logger.error("编辑报表组接口updateReportGroup出错", var12);
            throw new VieAppServiceException("编辑报表组接口出错", var12);
         }

         if (!hasCurrentReportId) {
            this.logger.error("当前用户没有该报表组的权限");
            throw new VieAppServiceException("当前用户没有该报表组的权限");
         } else if (names.contains(reportGroupRequest.getReportGroupName())) {
            this.logger.error("报表组：{}名称重复！", reportGroupRequest.getReportGroupName());
            throw new VieAppServiceException("报表组：'" + reportGroupRequest.getReportGroupName() + "'名称重复！");
         } else if (rg != null) {
            rg.setName(reportGroupRequest.getReportGroupName());
            if (this.reportGroupDB.update(rg)) {
               GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
               groupAuthRequest.setGroupId((long)rg.getId());
               groupAuthRequest.setGroupType(2);
               groupAuthRequest.setGroupName(rg.getName());
               groupAuthRequest.setUserId(Long.valueOf(reportGroupRequest.getUserId()));
               groupAuthRequest.setDataSource(reportGroupRequest.getDataSource());

               try {
                  if (!this.groupAuthService.updateGroupAuth(groupAuthRequest).isStatus()) {
                     this.logger.error("编辑报表组时调用权限系统保存组关系失败");
                     throw new VieAppServiceException("编辑报表组时调用权限系统保存组关系失败");
                  }
               } catch (Exception var11) {
                  this.logger.error(var11.getMessage());
                  throw new VieAppServiceException(var11.getMessage());
               }
            }
         } else {
            this.logger.error("编辑的报表组不存在");
            throw new VieAppServiceException("编辑的报表组不存在");
         }
      } else {
         this.logger.error("输入参数异常");
         throw new VieAppServiceException("输入参数异常");
      }
   }

   public List<Long> getGroupIdList(ReportGroupIdRequest reportGroupIdRequest) throws VieAppServiceException {
      GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
      groupAuthRequest.setUserId(reportGroupIdRequest.getUserId());
      groupAuthRequest.setGroupType(reportGroupIdRequest.getGroupType());
      groupAuthRequest.setDataSource(reportGroupIdRequest.getDataSource());

      try {
         return this.groupAuthService.queryGroupAuth(groupAuthRequest);
      } catch (Exception var4) {
         this.logger.error("获取组id列表出错", var4);
         throw new VieAppServiceException("获取组id列表出错", var4);
      }
   }

   public GroupAuthService getGroupAuthService() {
      return this.groupAuthService;
   }

   public void setGroupAuthService(GroupAuthService groupAuthService) {
      this.groupAuthService = groupAuthService;
   }

   public ReportGroupDB getReportGroupDB() {
      return this.reportGroupDB;
   }

   public void setReportGroupDB(ReportGroupDB reportGroupDB) {
      this.reportGroupDB = reportGroupDB;
   }

   public UserService getUserService() {
      return this.userService;
   }

   public void setUserService(UserService userService) {
      this.userService = userService;
   }
}
