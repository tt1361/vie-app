package com.iflytek.vie.app.provider.impl.config;

import com.iflytek.vie.app.api.config.ModelCategoryService;
import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.api.permission.UserService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.common.PagerResponse;
import com.iflytek.vie.app.pojo.common.ServiceResponse;
import com.iflytek.vie.app.pojo.config.ModelCategory;
import com.iflytek.vie.app.pojo.config.ModelCategoryRelation;
import com.iflytek.vie.app.pojo.config.ModelCategoryRequest;
import com.iflytek.vie.app.pojo.config.ModelCategoryResponse;
import com.iflytek.vie.app.pojo.config.ModelCategoryVo;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.pojo.permission.ResponseStatus;
import com.iflytek.vie.app.provider.database.ModelCategoryDB;
import com.iflytek.vie.utils.BeanUtilsExtends;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelCategoryServiceImpl implements ModelCategoryService {
   private final Logger logger = LoggerFactory.getLogger(ModelCategoryServiceImpl.class);
   private ModelCategoryDB modelCategoryDB;
   private GroupAuthService groupAuthService;
   private UserService userService;

   public ServiceResponse addModelCategoryService(ModelCategoryRequest modelCategoryRequest) throws VieAppServiceException {
      ServiceResponse serviceResponse = new ServiceResponse();

      try {
         if (modelCategoryRequest != null) {
            List<String> keyList = new ArrayList<>();
            keyList.add("modelCategoryType");
            keyList.add("modelCategoryName");
            keyList.add("userId");
            keyList.add("status");
            String checkResult = this.checkRequestParamNull(modelCategoryRequest, keyList);
            if (checkResult != null) {
               serviceResponse.setSuccessful(false);
               serviceResponse.setMessage("参数(" + checkResult + ")为空");
               return serviceResponse;
            } else if (this.checkModelCategoryNameExist(modelCategoryRequest)) {
               serviceResponse.setSuccessful(false);
               serviceResponse.setMessage("名称已存在");
               return serviceResponse;
            } else {
               ModelCategory modelCategory = new ModelCategory();
               modelCategory.setModelCategoryType(modelCategoryRequest.getModelCategoryType());
               modelCategory.setModelCategoryName(modelCategoryRequest.getModelCategoryName());
               modelCategory.setUserId(Long.parseLong(modelCategoryRequest.getUserId()));
               modelCategory.setCreatedate(new Date());
               modelCategory.setStatus(modelCategoryRequest.getStatus());
               modelCategory.setDescription(modelCategoryRequest.getDescription());
               Long modelCatagoryId = this.modelCategoryDB.saveModelCategory(modelCategory);
               if (modelCatagoryId == null) {
                  serviceResponse.setSuccessful(false);
                  serviceResponse.setMessage("保存失败");
                  return serviceResponse;
               } else {
                  if (!StringUtils.isNullOrEmpry(modelCategoryRequest.getModelIds())) {
                     String[] modelIds = modelCategoryRequest.getModelIds().split(",");
                     if (modelIds != null && modelIds.length > 0) {
                        for (int i = 0; i < modelIds.length; i++) {
                           if (!StringUtils.isNullOrEmpry(modelIds[i])) {
                              ModelCategoryRelation modelCategoryRelation = new ModelCategoryRelation();
                              modelCategoryRelation.setModelCatagoryId(modelCatagoryId);
                              modelCategoryRelation.setModelId(Long.parseLong(modelIds[i]));
                              this.modelCategoryDB.saveModelCategoryRelation(modelCategoryRelation);
                           }
                        }
                     }
                  }

                  this.syncModelCategoryToPermission(modelCategory, 1);
                  serviceResponse.setSuccessful(true);
                  serviceResponse.setValue(modelCatagoryId);
                  return serviceResponse;
               }
            }
         } else {
            serviceResponse.setSuccessful(false);
            serviceResponse.setMessage("参数传递有误");
            return serviceResponse;
         }
      } catch (Exception var10) {
         serviceResponse.setSuccessful(false);
         serviceResponse.setMessage("服务内存错误");
         this.logger.error("addModelCategoryService service has a error!", var10);
         return serviceResponse;
      }
   }

   private void syncModelCategoryToPermission(ModelCategory modelCategory, int type) throws PermissionServiceException {
      GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
      groupAuthRequest.setUserId(modelCategory.getUserId());
      if (modelCategory.getModelCategoryType() == 1) {
         groupAuthRequest.setGroupType(3);
      } else {
         groupAuthRequest.setGroupType(4);
      }

      groupAuthRequest.setGroupId(modelCategory.getId());
      groupAuthRequest.setGroupName(modelCategory.getModelCategoryName());
      ResponseStatus resp = null;
      if (type == 1) {
         resp = this.groupAuthService.addGroupAuth(groupAuthRequest);
      } else if (type == 2) {
         resp = this.groupAuthService.updateGroupAuth(groupAuthRequest);
      }

      if (resp == null || !resp.isStatus()) {
         throw new PermissionServiceException();
      }
   }

   private boolean checkModelCategoryNameExist(ModelCategoryRequest modelCategoryRequest) {
      return this.modelCategoryDB.checkModelCategoryNameExist(modelCategoryRequest);
   }

   public ServiceResponse deleteModelCategoryService(ModelCategoryRequest modelCategoryRequest) throws VieAppServiceException {
      ServiceResponse serviceResponse = new ServiceResponse();

      try {
         if (modelCategoryRequest != null) {
            List<Long> ids = modelCategoryRequest.getIds();
            Integer modelCategoryType = modelCategoryRequest.getModelCategoryType();
            if (ids != null && ids.size() > 0 && modelCategoryType != null) {
               this.modelCategoryDB.deleteModelCategory(ids);
               this.modelCategoryDB.deleteModelCategoryRelation(ids);
               this.deleteModelCategoryToPermission(ids, modelCategoryType);
               serviceResponse.setSuccessful(true);
               return serviceResponse;
            } else {
               serviceResponse.setSuccessful(false);
               serviceResponse.setMessage("参数传递有误");
               return serviceResponse;
            }
         } else {
            serviceResponse.setSuccessful(false);
            serviceResponse.setMessage("参数传递有误");
            return serviceResponse;
         }
      } catch (Exception var5) {
         serviceResponse.setSuccessful(false);
         serviceResponse.setMessage("服务内部错误");
         this.logger.error("deleteModelCategoryService service has a error!", var5);
         return serviceResponse;
      }
   }

   private void deleteModelCategoryToPermission(List<Long> ids, Integer modelCategoryType) throws PermissionServiceException {
      GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
      if (modelCategoryType == 1) {
         groupAuthRequest.setGroupType(3);
      } else {
         groupAuthRequest.setGroupType(4);
      }

      groupAuthRequest.setGroupIds(ids);
      ResponseStatus resp = this.groupAuthService.deleteGroupAuth(groupAuthRequest);
      if (!resp.isStatus()) {
         throw new PermissionServiceException();
      }
   }

   public ServiceResponse updateModelCategoryService(ModelCategoryRequest modelCategoryRequest) throws VieAppServiceException {
      ServiceResponse serviceResponse = new ServiceResponse();

      try {
         if (modelCategoryRequest != null) {
            List<String> keyList = new ArrayList<>();
            keyList.add("id");
            keyList.add("modelCategoryName");
            keyList.add("status");
            String checkResult = this.checkRequestParamNull(modelCategoryRequest, keyList);
            if (checkResult != null) {
               serviceResponse.setSuccessful(false);
               serviceResponse.setMessage("参数(" + checkResult + ")为空");
               return serviceResponse;
            } else if (this.checkModelCategoryNameExist(modelCategoryRequest)) {
               serviceResponse.setSuccessful(false);
               serviceResponse.setMessage("名称已存在");
               return serviceResponse;
            } else {
               ModelCategory modelCategory = this.modelCategoryDB.getModelCategoryById(modelCategoryRequest.getId());
               if (modelCategory == null) {
                  serviceResponse.setSuccessful(false);
                  serviceResponse.setMessage("信息不存在");
                  return serviceResponse;
               } else {
                  modelCategory.setModelCategoryName(modelCategoryRequest.getModelCategoryName());
                  modelCategory.setCreatedate(new Date());
                  modelCategory.setStatus(modelCategoryRequest.getStatus());
                  modelCategory.setDescription(modelCategoryRequest.getDescription());
                  modelCategory.setCreatedate(new Date());
                  this.modelCategoryDB.deleteModelCategoryRelation(modelCategoryRequest.getId());
                  if (!StringUtils.isNullOrEmpry(modelCategoryRequest.getModelIds())) {
                     String[] modelIds = modelCategoryRequest.getModelIds().split(",");
                     if (modelIds != null && modelIds.length > 0) {
                        for (int i = 0; i < modelIds.length; i++) {
                           if (!StringUtils.isNullOrEmpry(modelIds[i])) {
                              ModelCategoryRelation modelCategoryRelation = new ModelCategoryRelation();
                              modelCategoryRelation.setModelCatagoryId(modelCategoryRequest.getId());
                              modelCategoryRelation.setModelId(Long.parseLong(modelIds[i]));
                              this.modelCategoryDB.saveModelCategoryRelation(modelCategoryRelation);
                           }
                        }
                     }
                  }

                  this.syncModelCategoryToPermission(modelCategory, 2);
                  serviceResponse.setSuccessful(true);
                  return serviceResponse;
               }
            }
         } else {
            serviceResponse.setSuccessful(false);
            serviceResponse.setMessage("参数传递有误");
            return serviceResponse;
         }
      } catch (Exception var9) {
         serviceResponse.setSuccessful(false);
         serviceResponse.setMessage("服务内部错误");
         this.logger.error("updateModelCategoryService service has a error!", var9);
         return serviceResponse;
      }
   }

   public ServiceResponse getModelCategoryByIdService(Long id) {
      ServiceResponse serviceResponse = new ServiceResponse();

      try {
         if (id != null) {
            ModelCategory modelCategory = this.modelCategoryDB.getModelCategoryById(id);
            if (modelCategory != null) {
               ModelCategoryResponse modelCategoryResponse = new ModelCategoryResponse();
               ModelCategoryVo modelCategoryVo = new ModelCategoryVo();
               BeanUtilsExtends.copyProperties(modelCategory, modelCategoryVo);
               modelCategoryResponse.setModelCategoryVo(modelCategoryVo);
               List<Map<String, String>> modelList = this.modelCategoryDB.queryRelateModelList(id);
               modelCategoryResponse.setModelList(modelList);
               serviceResponse.setSuccessful(true);
               serviceResponse.setValue(modelCategoryResponse);
               return serviceResponse;
            } else {
               serviceResponse.setSuccessful(false);
               serviceResponse.setMessage("信息不存在");
               return serviceResponse;
            }
         } else {
            serviceResponse.setSuccessful(false);
            serviceResponse.setMessage("参数传递有误");
            return serviceResponse;
         }
      } catch (Exception var7) {
         serviceResponse.setSuccessful(false);
         serviceResponse.setMessage("服务内部错误");
         this.logger.error("getModelCategoryByIdService service has a error!", var7);
         return serviceResponse;
      }
   }

   public ServiceResponse queryModelCategoryListService(ModelCategoryRequest modelCategoryRequest) throws VieAppServiceException {
      ServiceResponse serviceResponse = new ServiceResponse();
      PagerResponse<ModelCategoryVo> pagerResponse = new PagerResponse();

      try {
         if (modelCategoryRequest != null) {
            List<String> keyList = new ArrayList<>();
            keyList.add("modelCategoryType");
            keyList.add("userId");
            String checkResult = this.checkRequestParamNull(modelCategoryRequest, keyList);
            if (checkResult != null) {
               serviceResponse.setSuccessful(false);
               serviceResponse.setMessage("参数(" + checkResult + ")为空");
               return serviceResponse;
            } else {
               List<Long> idList = this.getIdList(modelCategoryRequest);
               if (idList != null && idList.size() != 0) {
                  int totalCount = this.modelCategoryDB.queryModelCategoryTotalCount(modelCategoryRequest, idList);
                  if (totalCount > 0) {
                     List<ModelCategoryVo> resultList = new ArrayList<>();
                     List<ModelCategory> list = this.modelCategoryDB.queryModelCategoryBeanList(modelCategoryRequest, idList);
                     List<Long> categoryIds = this.getCategoryIds(list);
                     if (categoryIds != null && categoryIds.size() > 0) {
                        Map<Long, String> modelMap = this.modelCategoryDB.queryModelMap(categoryIds);
                        ModelCategory modelCategory = null;
                        Map<Integer, Map<String, String>> usermap = this.userService.queryAllUserInfo(modelCategoryRequest.getDataSource());
                        Map<String, String> usertemp = null;

                        for (int i = 0; i < list.size(); i++) {
                           modelCategory = list.get(i);
                           ModelCategoryVo modelCategoryVo = new ModelCategoryVo();
                           modelCategoryVo.setId(modelCategory.getId());
                           modelCategoryVo.setCreatedate(modelCategory.getCreatedate());
                           modelCategoryVo.setModelCategoryName(modelCategory.getModelCategoryName());
                           modelCategoryVo.setStatus(modelCategory.getStatus());
                           modelCategoryVo.setUserId(modelCategory.getUserId());
                           if (modelMap.containsKey(modelCategory.getId())) {
                              modelCategoryVo.setModuleNames(modelMap.get(modelCategory.getId()));
                           } else {
                              modelCategoryVo.setModuleNames("");
                           }

                           usertemp = usermap.get(modelCategoryVo.getUserId().intValue());
                           if (usertemp != null) {
                              modelCategoryVo.setCreateName(usertemp.get("accountName"));
                              modelCategoryVo.setRoleName(usertemp.get("roleName"));
                           }

                           resultList.add(modelCategoryVo);
                        }

                        this.setPagerResponse(pagerResponse, resultList, modelCategoryRequest, totalCount);
                        serviceResponse.setSuccessful(true);
                        serviceResponse.setValue(pagerResponse);
                        return serviceResponse;
                     } else {
                        serviceResponse.setSuccessful(false);
                        serviceResponse.setMessage("服务内部错误");
                        return serviceResponse;
                     }
                  } else {
                     serviceResponse.setSuccessful(true);
                     serviceResponse.setValue(pagerResponse);
                     return serviceResponse;
                  }
               } else {
                  serviceResponse.setSuccessful(true);
                  serviceResponse.setValue(pagerResponse);
                  return serviceResponse;
               }
            }
         } else {
            serviceResponse.setSuccessful(false);
            serviceResponse.setMessage("参数传递有误");
            return serviceResponse;
         }
      } catch (Exception var17) {
         serviceResponse.setSuccessful(false);
         serviceResponse.setMessage("服务内部错误");
         this.logger.error("queryModelCategoryListService service has a error!", var17);
         return serviceResponse;
      }
   }

   private List<Long> getCategoryIds(List<ModelCategory> list) {
      List<Long> result = new ArrayList<>();
      if (list != null && list.size() > 0) {
         ModelCategory modelCategory = null;

         for (int i = 0; i < list.size(); i++) {
            modelCategory = list.get(i);
            result.add(modelCategory.getId());
         }
      }

      return result;
   }

   private List<Long> getIdList(ModelCategoryRequest modelCategoryRequest) throws VieAppServiceException {
      try {
         GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
         groupAuthRequest.setUserId(Long.parseLong(modelCategoryRequest.getUserId()));
         if (modelCategoryRequest.getModelCategoryType() == 1) {
            groupAuthRequest.setGroupType(3);
         } else {
            groupAuthRequest.setGroupType(4);
         }

         return this.groupAuthService.queryGroupAuth(groupAuthRequest);
      } catch (Exception var3) {
         this.logger.error("获取组id列表出错", var3);
         throw new VieAppServiceException("获取组id列表出错", var3);
      }
   }

   private void setPagerResponse(
      PagerResponse<ModelCategoryVo> pagerResponse, List<ModelCategoryVo> resultList, ModelCategoryRequest modelCategoryRequest, int totalCount
   ) {
      int totalPage = totalCount % modelCategoryRequest.getPageSize() == 0
         ? totalCount / modelCategoryRequest.getPageSize()
         : totalCount / modelCategoryRequest.getPageSize() + 1;
      pagerResponse.setTotalPages(totalPage);
      pagerResponse.setTotalRows(totalCount);
      pagerResponse.setPageNum(modelCategoryRequest.getPageNum());
      pagerResponse.setPageSize(modelCategoryRequest.getPageSize());
      pagerResponse.setRows(resultList);
   }

   public ServiceResponse updateModelCategoryStatusService(ModelCategoryRequest modelCategoryRequest) throws VieAppServiceException {
      ServiceResponse serviceResponse = new ServiceResponse();

      try {
         if (modelCategoryRequest != null) {
            List<Long> ids = modelCategoryRequest.getIds();
            Integer status = modelCategoryRequest.getStatus();
            if (ids != null && ids.size() > 0 && status != null) {
               this.modelCategoryDB.updateModelCategoryStatus(ids, status);
               serviceResponse.setSuccessful(true);
               return serviceResponse;
            } else {
               serviceResponse.setSuccessful(false);
               serviceResponse.setMessage("参数传递有误");
               return serviceResponse;
            }
         } else {
            serviceResponse.setSuccessful(false);
            serviceResponse.setMessage("参数传递有误");
            return serviceResponse;
         }
      } catch (Exception var5) {
         serviceResponse.setSuccessful(false);
         serviceResponse.setMessage("服务内部错误");
         this.logger.error("updateModelCategoryStatusService service has a error!", var5);
         return serviceResponse;
      }
   }

   public ServiceResponse queryModelCategoryByName(String modelCategoryName) throws VieAppServiceException {
      ServiceResponse serviceResponse = new ServiceResponse();
      if (StringUtils.isNullOrEmpry(modelCategoryName)) {
         serviceResponse.setSuccessful(false);
         serviceResponse.setMessage("参数(" + modelCategoryName + ")为空");
         return serviceResponse;
      } else {
         try {
            ModelCategory modelCategory = this.modelCategoryDB.queryModelCategoryByName(modelCategoryName);
            serviceResponse.setSuccessful(true);
            serviceResponse.setValue(modelCategory);
            return serviceResponse;
         } catch (Exception var4) {
            serviceResponse.setSuccessful(false);
            serviceResponse.setMessage("服务内部错误");
            this.logger.error("queryModelCategoryByName service has a error!", var4);
            return serviceResponse;
         }
      }
   }

   private String checkRequestParamNull(ModelCategoryRequest modelCategoryRequest, List<String> list) {
      if (list != null && list.size() > 0) {
         String key = null;

         for (int i = 0; i < list.size(); i++) {
            key = list.get(i);
            if ("modelCategoryType".equals(key)) {
               if (modelCategoryRequest.getModelCategoryType() == null) {
                  return key;
               }
            } else if ("modelCategoryName".equals(key)) {
               if (StringUtils.isNullOrEmpry(modelCategoryRequest.getModelCategoryName())) {
                  return key;
               }
            } else if ("userId".equals(key)) {
               if (StringUtils.isNullOrEmpry(modelCategoryRequest.getUserId())) {
                  return key;
               }
            } else if ("status".equals(key)) {
               if (modelCategoryRequest.getStatus() == null) {
                  return key;
               }
            } else if ("id".equals(key) && modelCategoryRequest.getId() == null) {
               return key;
            }
         }
      }

      return null;
   }

   public ModelCategoryDB getModelCategoryDB() {
      return this.modelCategoryDB;
   }

   public void setModelCategoryDB(ModelCategoryDB modelCategoryDB) {
      this.modelCategoryDB = modelCategoryDB;
   }

   public GroupAuthService getGroupAuthService() {
      return this.groupAuthService;
   }

   public void setGroupAuthService(GroupAuthService groupAuthService) {
      this.groupAuthService = groupAuthService;
   }

   public UserService getUserService() {
      return this.userService;
   }

   public void setUserService(UserService userService) {
      this.userService = userService;
   }
}
