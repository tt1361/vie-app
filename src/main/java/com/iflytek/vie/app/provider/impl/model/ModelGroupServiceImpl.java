package com.iflytek.vie.app.provider.impl.model;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.model.ModelGroupService;
import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.ServiceResponse;
import com.iflytek.vie.app.pojo.common.TreeJson;
import com.iflytek.vie.app.pojo.model.ModelGroup;
import com.iflytek.vie.app.pojo.model.ModelGroupRequest;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.pojo.permission.ResponseStatus;
import com.iflytek.vie.app.provider.common.CommonDataPlatform;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.ModelDBPlatform;
import com.iflytek.vie.app.provider.database.ModelGroupDBPlatform;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.ListUtil;
import com.iflytek.vie.utils.StringUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.anydrill.calculate.set.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelGroupServiceImpl implements ModelGroupService {
   private final Logger logger = LoggerFactory.getLogger(ModelGroupServiceImpl.class);
   private ModelGroupDBPlatform modelGroupDB;
   private ModelDBPlatform modelDB;
   private ObjectMapper mapper = new ObjectMapper();
   private GroupAuthService groupAuthService;
   private CommonDataPlatform commonData;
   private String dataTypeDimension;
   private DataSourceInfo dataSourceInfo;
   private ExcuteContext excuteContext;
   private ServiceResponse serviceResponse;

   public ServiceResponse addModelGroupService(ModelGroupRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start add modelGroup");
         this.serviceResponse = new ServiceResponse();
         String specialStr = "[`~!@#$%^&*()=|{}':;',\\[\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]";
         Pattern p = Pattern.compile(specialStr);
         ModelGroup modelGroup = request.getModelGroup();
         if (modelGroup == null) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型组ID为空");
            return this.serviceResponse;
         } else if (StringUtils.isNullOrEmpry(modelGroup.getGroupName())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型组名称为空");
            return this.serviceResponse;
         } else {
            Matcher m = p.matcher(modelGroup.getGroupName());
            if (m.find()) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("模型组名称包含特殊符号");
               return this.serviceResponse;
            } else {
               this.logger.info("deal modelGroup condition done");
               modelGroup.setIsDisplay(1);
               modelGroup.setApplicationId(1L);
               Long parentId = modelGroup.getParentId();
               if (parentId == null) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("父模型组为空");
                  return this.serviceResponse;
               } else {
                  new ModelGroup();
                  int treeCode = 1;
                  if (parentId != 0L) {
                     this.logger.info("search parent group");
                     ModelGroup parentModelGroup = this.modelGroupDB.queryModelGroupByID(parentId);
                     this.logger.info("search parent group done");
                     if (parentModelGroup == null) {
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("父模型组不存在");
                        return this.serviceResponse;
                     }

                     treeCode = parentModelGroup.getTreeCodeIndex();
                     if (treeCode == 4) {
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("最多只能建四层模型组");
                        return this.serviceResponse;
                     }

                     treeCode++;
                  }

                  Long modelGroupId = null;
                  if (modelGroup == null) {
                     this.serviceResponse.setSuccessful(false);
                     this.serviceResponse.setMessage("传入的模型组为空");
                     return this.serviceResponse;
                  } else {
                     this.logger.info("start search user model group ");
                     new ArrayList();

                     List groupIds;
                     try {
                        GroupAuthRequest authRequest = new GroupAuthRequest();
                        authRequest.setUserId(Long.parseLong(request.getUserId()));
                        authRequest.setGroupType(1);
                        authRequest.setDataSource(request.getDataSource());
                        groupIds = this.groupAuthService.queryGroupAuth(authRequest);
                     } catch (Exception var15) {
                        this.logger.error("获取权限系统错误", var15);
                        throw new ViePlatformServiceException("获取权限系统错误", var15);
                     }

                     this.logger.info("search user model group done");
                     List<Long> allIds = this.modelGroupDB.getModelGroupIdById(groupIds, request.getDataSource());
                     if (!ListUtil.isNullOrEmpty(allIds)) {
                        this.logger.info("judge model group is have the same name");
                        boolean have = this.modelGroupDB.isSameModelGroup(modelGroup.getGroupName(), modelGroup.getParentId(), allIds, request.getDataSource());
                        this.logger.info("judge model group is have the same name done");
                        if (have) {
                           this.logger.info("have the same modelGroupName");
                           this.serviceResponse.setSuccessful(false);
                           this.serviceResponse.setMessage("存在相同的模型组名");
                           return this.serviceResponse;
                        }
                     }

                     modelGroup.setTreeCodeIndex(treeCode);
                     modelGroup.setDataSource(request.getDataSource());
                     this.logger.info("add model group");
                     modelGroupId = this.modelGroupDB.addModelGroup(modelGroup);
                     this.logger.info("add model group done");
                     if (modelGroupId == null) {
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("模型组保存失败");
                        return this.serviceResponse;
                     } else {
                        if (modelGroup.getParentId() == 0L) {
                           this.logger.info("add the modelGroup to permission");

                           try {
                              GroupAuthRequest authRequest = new GroupAuthRequest();
                              authRequest.setUserId(Long.parseLong(request.getUserId()));
                              authRequest.setGroupType(1);
                              authRequest.setGroupId(modelGroupId);
                              authRequest.setGroupName(modelGroup.getGroupName());
                              authRequest.setDataSource(request.getDataSource());
                              ResponseStatus responseStatus = this.groupAuthService.addGroupAuth(authRequest);
                              if (responseStatus.isStatus()) {
                                 this.logger.info("add group to permission success");
                              } else {
                                 this.logger.info("add group to permission false");
                              }
                           } catch (Exception var14) {
                              this.logger.error("add group to permission error");
                              throw new ViePlatformServiceException("添加模型组到权限系统错误", var14);
                           }

                           this.logger.info("add modelGroup to permission done");
                        }

                        this.logger.info("success add modelGroup");
                        this.serviceResponse.setSuccessful(true);
                        this.serviceResponse.setMessage("保存成功");
                        this.serviceResponse.setValue(modelGroupId);
                        return this.serviceResponse;
                     }
                  }
               }
            }
         }
      } catch (Exception var16) {
         this.logger.error("添加模型组错误", var16);
         throw new ViePlatformServiceException("添加模型组错误", var16);
      }
   }

   public ServiceResponse deleteModelGroupService(ModelGroupRequest request) throws ViePlatformServiceException {
      this.serviceResponse = new ServiceResponse();
      this.logger.info("start delete modelGroup");

      try {
         Long modelGroupId = request.getModelGroupId();
         if (modelGroupId == null) {
            this.logger.error("the modelGroupId is null");
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型组ID为空");
            return this.serviceResponse;
         } else {
            new ArrayList();
            this.logger.info("start search user model group");

            List modelGroupIds;
            try {
               GroupAuthRequest authRequest = new GroupAuthRequest();
               authRequest.setUserId(Long.parseLong(request.getUserId()));
               authRequest.setGroupType(1);
               authRequest.setDataSource(request.getDataSource());
               modelGroupIds = this.groupAuthService.queryGroupAuth(authRequest);
            } catch (Exception var10) {
               this.logger.error("获取权限系统错误", var10);
               throw new ViePlatformServiceException("获取权限系统错误", var10);
            }

            this.logger.info("search user model group done");
            if (ListUtil.isNullOrEmpty(modelGroupIds)) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("该角色没有该模型组的权限");
               return this.serviceResponse;
            } else {
               this.logger.info("the group size is " + modelGroupIds.size() + " and the groups is " + this.mapper.writeValueAsString(modelGroupIds));
               List<Long> allIds = this.modelGroupDB.getModelGroupIdById(modelGroupIds, request.getDataSource());
               if (!allIds.contains(modelGroupId)) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("该角色没有该模型组的权限");
                  return this.serviceResponse;
               } else {
                  ModelGroup modelGroup = this.modelGroupDB.queryModelGroupByID(modelGroupId);
                  if (modelGroup == null) {
                     this.serviceResponse.setSuccessful(false);
                     this.serviceResponse.setMessage("模型组不存在");
                     return this.serviceResponse;
                  } else {
                     this.logger.info("judge is delete");
                     if (this.modelGroupDB.queryModelOnlineByGroupId(modelGroupId, request.getDataSource())) {
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("已选模型组中有已上线模型,不可删除");
                        return this.serviceResponse;
                     } else {
                        if (modelGroup.getParentId() == 0L) {
                           this.logger.info("delete modelGroup from permission");

                           try {
                              GroupAuthRequest authRequest = new GroupAuthRequest();
                              List<Long> groupIds = new ArrayList<>();
                              groupIds.add(modelGroupId);
                              authRequest.setUserId(Long.parseLong(request.getUserId()));
                              authRequest.setGroupType(1);
                              authRequest.setGroupIds(groupIds);
                              authRequest.setDataSource(request.getDataSource());
                              ResponseStatus responseStatus = this.groupAuthService.deleteGroupAuth(authRequest);
                              if (responseStatus.isStatus()) {
                                 this.logger.info("delete group from permission success");
                              } else {
                                 this.logger.info("delete group from permission false");
                              }
                           } catch (Exception var9) {
                              this.logger.error("delete group from permission error");
                              throw new ViePlatformServiceException("从权限系统中删除模型组错误", var9);
                           }

                           this.logger.info("delete modelGroup from permission done");
                        }

                        this.logger.info("start delete modelGroup");
                        boolean isDelete = this.modelGroupDB.deleteModelGroup2(modelGroupId, request.getDataSource());
                        if (isDelete) {
                           this.serviceResponse.setSuccessful(true);
                           this.serviceResponse.setMessage("删除成功");
                           this.logger.info("success delete modelGroup.");
                        } else {
                           this.serviceResponse.setSuccessful(false);
                           this.serviceResponse.setMessage("系统中其他模块有使用，不可删除");
                           this.logger.info("delete modelGroup failure.");
                        }

                        this.logger.info("delete modelGroup success");
                        return this.serviceResponse;
                     }
                  }
               }
            }
         }
      } catch (Exception var11) {
         this.logger.info("删除模型组错误", var11);
         throw new ViePlatformServiceException("删除模型组错误", var11);
      }
   }

   public List<TreeJson> searchModelGroupService(ModelGroupRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search modelGroup");
         new ArrayList();
         this.logger.info("search modelGroup from permission");

         List e;
         try {
            GroupAuthRequest authRequest = new GroupAuthRequest();
            authRequest.setUserId(Long.parseLong(request.getUserId()));
            authRequest.setGroupType(1);
            authRequest.setDataSource(request.getDataSource());
            e = this.groupAuthService.queryGroupAuth(authRequest);
         } catch (Exception var11) {
            this.logger.error("get the modelGroupIds from permission error");
            this.logger.error("获取前线系统错误", var11);
            throw new ViePlatformServiceException("获取权限系统错误", var11);
         }

         this.logger.info("search modelGroup from permission done");
         List<TreeJson> treeJsonList = new ArrayList<>();
         TreeJson test = new TreeJson();
         test.setId(0);
         test.setPid(-1);
         test.setText("全部");
         treeJsonList.add(test);
         List<TreeJson> list = TreeJson.formatTree(treeJsonList);
         if (e == null) {
            this.logger.error("get the modelGroupIds from permission is null");
            return list;
         } else {
            List<Long> allIds = this.modelGroupDB.getModelGroupIdById(e, request.getDataSource());
            if (ListUtil.isNullOrEmpty(allIds)) {
               this.logger.info("模型组为空");
               return list;
            } else {
               this.logger.info("the groupids from permission is " + JSON.toJSONString(e));
               String modelGroupName = request.getModelGroupName();
               List<Object[]> modelGroupList = this.modelGroupDB.searchModelGroups(allIds, modelGroupName, request.getDataSource());
               this.logger.info("deal modelGroup to tree");
               if (null != modelGroupList && modelGroupList.size() > 0) {
                  for (int i = 0; i < modelGroupList.size(); i++) {
                     TreeJson treeJson = new TreeJson();
                     treeJson.setId(Integer.parseInt(modelGroupList.get(i)[0].toString()));
                     treeJson.setPid(Integer.parseInt(modelGroupList.get(i)[2].toString()));
                     treeJson.setText(modelGroupList.get(i)[1].toString());
                     treeJson.setTreeNum(Integer.parseInt(modelGroupList.get(i)[3].toString()));
                     treeJsonList.add(treeJson);
                  }
               }

               list = TreeJson.formatTree(treeJsonList);
               this.logger.info("success search modelGroup");
               return list;
            }
         }
      } catch (Exception var12) {
         throw new ViePlatformServiceException("查询模型组错误", var12);
      }
   }

   public ServiceResponse updateModelGroupService(ModelGroupRequest request) throws ViePlatformServiceException {
      this.logger.info("start update modelGroup");
      this.serviceResponse = new ServiceResponse();
      ModelGroup modelGroup = request.getModelGroup();
      this.logger.info("get modelGoup from DB");
      ModelGroup group = this.modelGroupDB.queryModelGroupByID(modelGroup.getGroupId());
      this.logger.info("get modelGroup from DB done");
      if (group == null) {
         this.serviceResponse.setSuccessful(false);
         this.serviceResponse.setMessage("模型组不存在");
         return this.serviceResponse;
      } else {
         Long parentId = modelGroup.getParentId();
         if (parentId == modelGroup.getGroupId()) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("父模型组不能和子模型组相同");
            return this.serviceResponse;
         } else {
            if (parentId != 0L) {
               this.logger.info("get modelGoup parent from DB");
               ModelGroup mg = this.modelGroupDB.queryModelGroupByID(parentId);
               this.logger.info("get modelGroup parent from DB done");
               if (mg == null) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("父模型组不存在");
                  return this.serviceResponse;
               }
            }

            this.logger.info("search modelGroup from permission");
            new ArrayList();

            List var14;
            try {
               GroupAuthRequest authRequest = new GroupAuthRequest();
               authRequest.setUserId(Long.parseLong(request.getUserId()));
               authRequest.setGroupType(1);
               authRequest.setDataSource(request.getDataSource());
               var14 = this.groupAuthService.queryGroupAuth(authRequest);
            } catch (Exception var12) {
               this.logger.error("get the groupIds from permission error");
               this.logger.error("获取权限系统错误", var12);
               throw new ViePlatformServiceException("获取权限系统错误", var12);
            }

            this.logger.info("search modelGroup from permission done");
            if (var14 == null) {
               this.logger.error("get the groupIds from permission is null");
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("模型组权限为空");
               return this.serviceResponse;
            } else {
               this.logger.info("the groupids from permission is " + JSON.toJSONString(var14));
               List<Long> allIds = this.modelGroupDB.getModelGroupIdById(var14, request.getDataSource());
               if (ListUtil.isNullOrEmpty(allIds)) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("模型组为空");
                  return this.serviceResponse;
               } else {
                  this.logger.info("search modelGroup hava the same name");
                  boolean have = this.modelGroupDB.isSameModelGroup(modelGroup.getGroupName(), modelGroup.getParentId(), allIds, request.getDataSource());
                  if (have) {
                     this.logger.info("hava the same modelGroupName");
                     this.serviceResponse.setSuccessful(false);
                     this.serviceResponse.setMessage("存在相同的模型组");
                     return this.serviceResponse;
                  } else {
                     this.logger.info("search modelGroup from DB");
                     ModelGroup mg = this.modelGroupDB.queryModelGroupByID(modelGroup.getGroupId());
                     mg.setParentId(modelGroup.getParentId());
                     mg.setGroupName(modelGroup.getGroupName());
                     this.logger.info("update modelGroup");
                     boolean isUpdate = this.modelGroupDB.saveOrUpdateModelGroup(mg);
                     if (isUpdate && mg.getParentId() == 0L) {
                        this.logger.info("update permission modelGroup");

                        try {
                           GroupAuthRequest authRequest = new GroupAuthRequest();
                           authRequest.setUserId(Long.parseLong(request.getUserId()));
                           authRequest.setGroupType(1);
                           authRequest.setGroupName(mg.getGroupName());
                           authRequest.setGroupId(mg.getGroupId());
                           authRequest.setDataSource(request.getDataSource());
                           ResponseStatus responseStatus = this.groupAuthService.updateGroupAuth(authRequest);
                           if (responseStatus.isStatus()) {
                              this.logger.info("update group from permission success");
                           }
                        } catch (Exception var13) {
                           this.logger.error("update group from permission error");
                           throw new ViePlatformServiceException("更新权限系统错误", var13);
                        }

                        this.logger.info("update permission modelGroup done");
                     }

                     this.logger.info("success update modelGroup");
                     this.serviceResponse.setSuccessful(true);
                     this.serviceResponse.setMessage("更新模型组成功");
                     return this.serviceResponse;
                  }
               }
            }
         }
      }
   }

   public ServiceResponse searchAllModelGroupsService(ModelGroupRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search all model groups");
         this.serviceResponse = new ServiceResponse();
         new ArrayList();

         List e;
         try {
            this.logger.info("search groupids from permission");
            GroupAuthRequest authRequest = new GroupAuthRequest();
            authRequest.setUserId(Long.parseLong(request.getUserId()));
            authRequest.setGroupType(1);
            authRequest.setDataSource(request.getDataSource());
            e = this.groupAuthService.queryGroupAuth(authRequest);
         } catch (Exception var4) {
            this.logger.error("get the groupIds from permission error");
            this.logger.error("获取权限系统错误", var4);
            throw new ViePlatformServiceException("获取权限系统错误", var4);
         }

         this.logger.info("search groupids from permission done");
         if (ListUtil.isNullOrEmpty(e)) {
            this.logger.error("get the groupIds from permission is null");
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型组权限为空");
            return this.serviceResponse;
         } else {
            List<ModelGroup> modelGroups = this.modelGroupDB.queryGroupsByPermission(e, request.getDataSource());
            if (modelGroups != null && modelGroups.size() != 0) {
               this.serviceResponse.setSuccessful(true);
               this.serviceResponse.setMessage("查询成功");
               this.serviceResponse.setValue(modelGroups);
               this.logger.info("success search all model groups");
               return this.serviceResponse;
            } else {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("查询结果为空");
               return this.serviceResponse;
            }
         }
      } catch (Exception var5) {
         this.logger.error("查询所有的模型组异常", var5);
         throw new ViePlatformServiceException("查询所有的模型组异常", var5);
      }
   }

   public ServiceResponse searchOnlineModelGroupsService(ModelGroupRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search online modelGroup");
         this.serviceResponse = new ServiceResponse();
         new ArrayList();

         List e;
         try {
            this.logger.info("search groupids from permission");
            GroupAuthRequest authRequest = new GroupAuthRequest();
            authRequest.setUserId(Long.parseLong(request.getUserId()));
            authRequest.setGroupType(1);
            authRequest.setDataSource(request.getDataSource());
            e = this.groupAuthService.queryGroupAuth(authRequest);
         } catch (Exception var5) {
            this.logger.error("get the groupIds from permission error");
            this.logger.error("获取权限系统错误", var5);
            throw new ViePlatformServiceException("获取权限系统错误", var5);
         }

         this.logger.info("search groupids from permission done");
         if (ListUtil.isNullOrEmpty(e)) {
            this.logger.error("get the groupIds from permission is null");
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型组权限为空");
            return this.serviceResponse;
         } else {
            List<Long> modelGroupIds = this.modelGroupDB.queryAllModelGroupIdsByPermission(e, request.getDataSource());
            if (ListUtil.isNullOrEmpty(modelGroupIds)) {
               this.logger.info("模型组为空");
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("模型组为空");
               return this.serviceResponse;
            } else {
               List<HashMap<String, Object>> data = this.modelGroupDB.findAllOnlineModelGroup(modelGroupIds, request.getDataSource());
               this.serviceResponse.setValue(this.dealData(data));
               this.serviceResponse.setSuccessful(true);
               this.serviceResponse.setMessage("查询成功");
               return this.serviceResponse;
            }
         }
      } catch (Exception var6) {
         this.logger.info("获取所有上线模型组错误", var6);
         throw new ViePlatformServiceException("获取所有上线模型组错误", var6);
      }
   }

   public ServiceResponse searchModelByGroupId(ModelGroupRequest request) throws ViePlatformServiceException {
      this.logger.info("####### Enter searchModelByGroupId() ");
      this.logger.info("The dataSource is: " + request.getDataSource());
      String centerDimension = request.getDataTypeDimension();
      String indexAnydrillModelAddress = DynamicEsSource.getEsSourceByType(request.getDataSource(), "modelAnydrillAddress");
      this.logger.info("modelAnydrillAddress is: " + indexAnydrillModelAddress);
      ExcuteContext indexContext = new ExcuteContext();

      try {
         this.logger.info("start search online modelGroup");
         this.serviceResponse = new ServiceResponse();
         String dataSQL = this.commonData.getDataSql(request.getUserId(), "VIEWEB", request.getDataSource());
         if (dataSQL != null && !dataSQL.equals("")) {
            dataSQL = " and " + dataSQL;
         }

         String mid = "";
         String sql = "";
         String accuracyPer = "";
         String modelStrings = "";
         int centerFlag = request.getCenterFlag();
         this.logger.debug("start time :" + request.getStartTime() + ", end time :" + request.getEndTime());
         String startTime = request.getStartTime();
         String endTime = request.getEndTime();
         String selectCenter = request.getSelectCenter();
         String tableName = request.getDataSource();
         List<String> onlineModelId = new ArrayList<>();
         this.logger.info("database name is: " + tableName);
         List<HashMap<String, Object>> modelListMap = this.modelDB.getModelByModelGroupId(tableName, request.getModelGroupId(), onlineModelId, this.logger);
         if (modelListMap != null && modelListMap.size() != 0) {
            List<HashMap<String, Object>> modelListMapTemp = new ArrayList<>();
            List<LinkedHashMap<String, Object>> modelAccuracyList = new ArrayList<>();
            HashMap<String, String> modelAccuracyMap = new HashMap<>();
            StringBuffer stringBuffer = new StringBuffer();

            for (String mMap : onlineModelId) {
               stringBuffer.append(mMap).append(",");
            }

            modelStrings = stringBuffer.substring(0, stringBuffer.length() - 1).toString();
            SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Long start = System.currentTimeMillis();
            Long eTime = sdf1.parse(endTime).getTime();
            Long sTime = sdf1.parse(startTime).getTime();
            String indexTableName = IndexConstants.getModelTableName(tableName);
            String timeSql = "timestamp between " + sTime + " and " + eTime;
            sql = "select offLineTagId,count(idCounter) as accuracyNum  from "
               + indexTableName
               + " where "
               + timeSql
               + " and  offLineTagId in ("
               + modelStrings
               + ") "
               + dataSQL;
            String batchSql = " and " + MultipleDataConstant.getMultipleDataSql(request.getBatchId(), request.getIfLone(), request.getDataType());
            sql = sql + batchSql;
            StringBuffer sqlBuffer = new StringBuffer(sql);
            if (request.getCenterFlag() == 1) {
               sqlBuffer.append(" and  " + centerDimension + " in  ('" + request.getSelectCenter() + "') ");
            }

            sqlBuffer.append(" group  by offLineTagId");
            sql = sqlBuffer.toString();
            this.logger.info("(((############### query model ES sql is:))) " + sql);
            ResultSet result = indexContext.executeSearchQuery(indexAnydrillModelAddress, sql);
            this.logger.info("模型检出占比查询索引耗时：" + (System.currentTimeMillis() - start));
            if (result != null) {
               modelAccuracyList = result.toList();
            }

            for (LinkedHashMap<String, Object> modelAccuracy : modelAccuracyList) {
               modelAccuracyMap.put(modelAccuracy.get("offLineTagId").toString(), modelAccuracy.get("accuracyNum").toString());
            }

            String countSql = this.dealWithRuleStringNew(timeSql, tableName) + dataSQL;
            countSql = countSql + " and " + MultipleDataConstant.getMultipleDataSql(request.getBatchId(), request.getIfLone(), request.getDataType());
            StringBuffer sqlBuffer1 = new StringBuffer(countSql);
            if (centerFlag == 1) {
               sqlBuffer1.append(" and  " + centerDimension + " in  ('" + selectCenter + "') ");
            }

            countSql = sqlBuffer1.toString();
            this.logger.info("************" + countSql);
            Long start1 = System.currentTimeMillis();
            String url = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress");
            ResultSet rs = this.excuteContext.executeSearchQuery(url, countSql);
            this.logger.info("模型检出占比查询索引耗时：" + (System.currentTimeMillis() - start));
            this.logger.info("模型" + mid + "检出占比查询索引耗时：" + (System.currentTimeMillis() - start1));
            String allNum = "0";
            if (rs != null && rs.toList().size() > 0) {
               allNum = ((LinkedHashMap)rs.toList().get(0)).get("accuracyNum").toString().replace(".0", "");
            }

            for (HashMap<String, Object> modelMap : (List<HashMap<String, Object>>)modelListMap) {
               mid = modelMap.get("modelId").toString();
               String modelNum = "0";
               if (modelAccuracyMap.size() != 0 && modelAccuracyMap.keySet().contains(mid)) {
                  modelNum = modelAccuracyMap.get(mid);
               }

               accuracyPer = modelNum.replace(".0", "") + "(" + allNum + ")";
               modelMap.put("modelPer", accuracyPer);
               modelMap.remove("filterRule");
               modelListMapTemp.add(modelMap);
            }

            if (modelListMapTemp.size() == 0) {
               this.serviceResponse.setMessage("模型信息为空!");
               this.serviceResponse.setSuccessful(true);
            } else {
               this.serviceResponse.setValue(modelListMapTemp);
               this.serviceResponse.setSuccessful(true);
            }

            return this.serviceResponse;
         } else {
            this.serviceResponse.setMessage("模型信息为空!");
            this.serviceResponse.setSuccessful(true);
            return this.serviceResponse;
         }
      } catch (Exception var39) {
         this.logger.info("根据模型组和模型时间查询模型错误", var39);
         throw new ViePlatformServiceException("根据模型组和模型时间查询模型错误", var39);
      }
   }

   public List<HashMap<String, Object>> jsonToMap(String filterRule) throws Exception {
      List<HashMap<String, Object>> ruleList = new ArrayList<>();
      new ArrayList();
      if (filterRule.equals("")) {
         return ruleList;
      } else {
         ObjectMapper mapper = new ObjectMapper();

         for (HashMap<String, Object> map : (List<HashMap<String, Object>>)mapper.readValue(filterRule, List.class)) {
            HashMap<String, Object> rulemap = new HashMap<>();
            if (!map.get("key").equals("timestamp")) {
               rulemap.put("key", map.get("key"));
               rulemap.put("value", (ArrayList)map.get("value"));
               rulemap.put("type", map.get("type"));
            }

            ruleList.add(rulemap);
         }

         return ruleList;
      }
   }

   public String dealWithRuleStringNew(String dateString, String tableName) {
      return "select count(idCounter) as accuracyNum  from " + tableName + " where " + dateString + " and  processed = 0 ";
   }

   public String dealWithRuleString(List<HashMap<String, Object>> ruleList, String dateString, String tableName) {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMdd");
      Date date = null;

      try {
         date = sdf.parse(dateString);
      } catch (ParseException var19) {
         var19.printStackTrace();
      }

      date.setDate(date.getDate() - 1);
      String dimDay = sdf1.format(date);
      String msql = "select count(dimDay) as accuracyNum  from " + tableName + " where  dimDay = '" + dimDay + "'" + "  and  processed = 0  ";
      new ArrayList();
      new ArrayList();
      int min = 0;
      int max = 0;

      for (HashMap<String, Object> ruleMap : (List<HashMap<String, Object>>)ruleList) {
         ArrayList<String> rule = (ArrayList)ruleMap.get("value");
         if (ruleMap.get("key") != null && ruleMap.get("value") != null) {
            if (ruleMap.get("type").equals("range")) {
               String[] range = new String[2];

               for (int i = 0; i < rule.size(); i++) {
                  range = ((String)rule.get(i)).toString().split("~");
                  Double range0 = Double.parseDouble(range[0]) * 1000.0 - 500.0;
                  Double range1 = Double.parseDouble(range[1]) * 1000.0 + 499.0;
                  if (rule.size() == 1) {
                     msql = msql + "  and (" + ruleMap.get("key") + " between " + range0 + " and " + range1 + "  )  ";
                     break;
                  }

                  if (i == 0) {
                     msql = msql + "  and (" + ruleMap.get("key") + " between " + range0 + " and " + range1 + "  or  ";
                  } else {
                     if (i == rule.size() - 1) {
                        msql = msql + ruleMap.get("key") + " between " + range0 + " and " + range1 + ")";
                        break;
                     }

                     msql = msql + ruleMap.get("key") + " between " + range0 + " and " + range1 + "  or  ";
                  }
               }
            } else {
               String ruleString = "";
               int i = 0;

               while (true) {
                  if (i < rule.size()) {
                     if (i != rule.size() - 1) {
                        ruleString = ruleString + "'" + (String)rule.get(i) + "'" + ",";
                        i++;
                        continue;
                     }

                     ruleString = ruleString + "'" + (String)rule.get(i) + "'";
                  }

                  msql = msql + " and " + " " + ruleMap.get("key") + " " + "in (" + ruleString + ")";
                  break;
               }
            }
         }
      }

      return msql;
   }

   private List<HashMap<String, Object>> dealData(List<HashMap<String, Object>> data) {
      List<HashMap<String, Object>> result = new ArrayList<>();
      String lastModelGroupID = "";
      HashMap<String, Object> lastDate = new HashMap<>();
      List<HashMap<String, String>> tmpModelList = new ArrayList<>();

      for (HashMap<String, Object> current : (List<HashMap<String, Object>>)data) {
         String modelGroupID = current.get("group_id").toString();
         if (!lastModelGroupID.equals(modelGroupID) && !"".equals(lastModelGroupID)) {
            HashMap<String, Object> currentDate = new HashMap<>();
            currentDate.put("modelGroup", lastDate.get("group_name"));
            currentDate.put("modelGroupId", lastDate.get("group_id"));
            currentDate.put("value", tmpModelList);
            tmpModelList = new ArrayList<>();
            HashMap<String, String> cmDate = new HashMap<>();
            cmDate.put("key", current.get("model_id").toString());
            cmDate.put("value", current.get("model_name").toString());
            if (current.get("text_rule") != null) {
               cmDate.put("ruleText", current.get("text_rule").toString());
            } else {
               cmDate.put("ruleText", "");
            }

            tmpModelList.add(cmDate);
            result.add(currentDate);
         } else {
            String ruleText = current.get("text_rule") == null ? "" : current.get("text_rule").toString();
            HashMap<String, String> cmDate = new HashMap<>();
            cmDate.put("key", current.get("model_id").toString());
            cmDate.put("value", current.get("model_name").toString());
            cmDate.put("ruleText", ruleText);
            tmpModelList.add(cmDate);
         }

         lastDate = current;
         lastModelGroupID = modelGroupID;
      }

      if (tmpModelList.size() > 0) {
         HashMap<String, Object> currentDate = new HashMap<>();
         currentDate.put("modelGroup", lastDate.get("group_name"));
         currentDate.put("modelGroupId", lastDate.get("group_id"));
         currentDate.put("value", tmpModelList);
         new ArrayList();
         result.add(currentDate);
      }

      return result;
   }

   public ModelGroupDBPlatform getModelGroupDB() {
      return this.modelGroupDB;
   }

   public void setModelGroupDB(ModelGroupDBPlatform modelGroupDB) {
      this.modelGroupDB = modelGroupDB;
   }

   public GroupAuthService getGroupAuthService() {
      return this.groupAuthService;
   }

   public void setGroupAuthService(GroupAuthService groupAuthService) {
      this.groupAuthService = groupAuthService;
   }

   public String getDataTypeDimension() {
      return this.dataTypeDimension;
   }

   public void setDataTypeDimension(String dataTypeDimension) {
      this.dataTypeDimension = dataTypeDimension;
   }

   public CommonDataPlatform getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonDataPlatform commonData) {
      this.commonData = commonData;
   }

   public ModelDBPlatform getModelDB() {
      return this.modelDB;
   }

   public void setModelDB(ModelDBPlatform modelDB) {
      this.modelDB = modelDB;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public ExcuteContext getExcuteContext() {
      return this.excuteContext;
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
   }
}
