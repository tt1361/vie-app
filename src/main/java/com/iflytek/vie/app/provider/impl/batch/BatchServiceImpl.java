package com.iflytek.vie.app.provider.impl.batch;

import com.iflytek.vie.app.api.batch.BatchService;
import com.iflytek.vie.app.api.model.ModelService;
import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.batch.BatchDimension;
import com.iflytek.vie.app.pojo.batch.BatchInfo;
import com.iflytek.vie.app.pojo.batch.BatchModelTask;
import com.iflytek.vie.app.pojo.batch.BatchRequest;
import com.iflytek.vie.app.pojo.batch.BatchResponse;
import com.iflytek.vie.app.pojo.common.PagerResponse;
import com.iflytek.vie.app.pojo.common.ServiceResponse;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.app.pojo.model.ModelRequest;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.provider.database.BatchDB;
import com.iflytek.vie.app.provider.database.BatchDimensionDB;
import com.iflytek.vie.app.provider.database.BatchModelDB;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.utils.StringUtils;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchServiceImpl implements BatchService {
   private final Logger logger = LoggerFactory.getLogger(BatchServiceImpl.class);
   private BatchDB batchDB;
   private ModelService modelService;
   private ModelDB modelDB;
   private BatchModelDB batchModelDB;
   private GroupAuthService groupAuthService;
   private BatchDimensionDB batchDimensionlDB;

   public int addBatch(BatchRequest batchRequest) throws VieAppServiceException {
      if (batchRequest.getBatchId().length() > 40) {
         this.logger.error("批次号长度不能大于40");
         throw new VieAppServiceException("批次号长度不能大于40");
      } else if (!StringUtils.isNullOrEmpry(batchRequest.getBatchId())
         && !StringUtils.isNullOrEmpry(batchRequest.getCreateUser())
         && !StringUtils.isNullOrEmpry(batchRequest.getSystemId())
         && !StringUtils.isNullOrEmpry(batchRequest.getUserId())) {
         if (batchRequest.getIfLone() == 1 && StringUtils.isNullOrEmpry(batchRequest.getModels())) {
            this.logger.error("导入独立数据时必须选择模型");
            throw new VieAppServiceException("导入独立数据时必须选择模型");
         } else {
            int id = 0;
            boolean flag = false;

            try {
               flag = this.batchDB.checkBatchId(batchRequest.getBatchId());
            } catch (Exception var7) {
               this.logger.error("查询批次号是否重复方法异常", var7);
               throw new VieAppServiceException("查询批次号是否重复方法异常", var7);
            }

            if (flag) {
               this.logger.error("批次号重复");
               throw new VieAppServiceException("批次号重复");
            } else {
               try {
                  BatchInfo batchInfo = new BatchInfo();
                  batchInfo.setBatchId(batchRequest.getBatchId());
                  batchInfo.setDataType(batchRequest.getDataType());
                  batchInfo.setIfLone(batchRequest.getIfLone());
                  batchInfo.setAnalysisType(batchRequest.getAnalysisType());
                  batchInfo.setImportCount(batchRequest.getImportCount());
                  batchInfo.setCreateUser(batchRequest.getCreateUser());
                  batchInfo.setRemark(batchRequest.getRemark());
                  batchInfo.setModels(batchRequest.getModels());
                  batchInfo.setStartTime(new Date());
                  id = this.batchDB.saveOrUpdateBatch(batchInfo);
                  if (id > 0) {
                     GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
                     groupAuthRequest.setGroupId((long)id);
                     groupAuthRequest.setGroupType(5);
                     groupAuthRequest.setGroupName(batchRequest.getBatchId());
                     groupAuthRequest.setUserId(Long.valueOf(batchRequest.getUserId()));
                     if (!this.groupAuthService.addGroupAuth(groupAuthRequest).isStatus()) {
                        this.logger.error("导入数据时调用权限系统保存组关系失败");
                        throw new VieAppServiceException("导入数据时调用权限系统保存组关系失败");
                     }
                  }

                  return id;
               } catch (Exception var6) {
                  this.logger.error("导入数据调用addBatch接口出错", var6);
                  throw new VieAppServiceException("导入数据异常", var6);
               }
            }
         }
      } else {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      }
   }

   public PagerResponse<BatchInfo> queryBatchList(BatchRequest batchRequest) throws VieAppServiceException {
      PagerResponse<BatchInfo> pagerResponse = new PagerResponse();
      if (!StringUtils.isNullOrEmpry(batchRequest.getSystemId()) && !StringUtils.isNullOrEmpry(batchRequest.getUserId())) {
         String startDate = batchRequest.getStartDate();
         String endDate = batchRequest.getEndDate();
         String batchId = batchRequest.getBatchId();
         Integer ifLone = batchRequest.getIfLone();
         Integer dataType = batchRequest.getDataType();
         int pageSize = batchRequest.getPageSize() == 0 ? 15 : batchRequest.getPageSize();
         int pageNum = batchRequest.getPageNum() == 0 ? 1 : batchRequest.getPageNum();
         int groupType = 5;
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         Date startTime = null;
         Date endTime = null;
         if (!StringUtils.isNullOrEmpry(startDate) && !StringUtils.isNullOrEmpry(endDate)) {
            try {
               startTime = sdf.parse(startDate);
               endTime = sdf.parse(endDate);
            } catch (ParseException var24) {
               this.logger.error("时间类型转换异常");
               throw new VieAppServiceException("时间类型转换异常", var24);
            }

            if (startTime.getTime() > endTime.getTime()) {
               this.logger.info("入参起始时间不能晚于结束时间");
               pagerResponse.setPageNum(pageNum);
               pagerResponse.setPageSize(pageSize);
               pagerResponse.setTotalPages(0);
               pagerResponse.setTotalRows(0L);
               pagerResponse.setRows(null);
               return pagerResponse;
            }
         }

         try {
            List<Long> idList = this.getGroupIdList(groupType, Long.parseLong(batchRequest.getUserId()));
            if (idList != null && idList.size() != 0) {
               List<Integer> intIdList = new ArrayList<>();

               for (Long longId : idList) {
                  intIdList.add(Integer.parseInt(longId.toString()));
               }

               int totalRows = this.batchDB.getBatchListSize(startDate, endDate, batchId, ifLone, intIdList);
               int totalPages = totalRows % pageSize == 0 ? totalRows / pageSize : totalRows / pageSize + 1;
               List<BatchInfo> batchList = this.batchDB.getBatchList(startDate, endDate, batchId, ifLone, pageSize, pageNum, intIdList, dataType);
               if (batchList != null && batchList.size() != 0) {
                  List<BatchModelTask> batchModelTaskList = this.batchModelDB.getBatchModelTask();
                  Set<Integer> idSet = new HashSet<>();
                  BatchModelTask batchModelTask = null;
                  if (batchModelTaskList != null && batchModelTaskList.size() > 0) {
                     for (int i = 0; i < batchModelTaskList.size(); i++) {
                        batchModelTask = batchModelTaskList.get(i);
                        int taskBatchId = (int)batchModelTask.getBatchId();
                        idSet.add(taskBatchId);
                     }
                  }

                  for (BatchInfo batchInfo : batchList) {
                     if (idSet.contains(batchInfo.getId())) {
                        batchInfo.setTaskStatus(1);
                     } else {
                        batchInfo.setTaskStatus(0);
                     }
                  }

                  pagerResponse.setPageNum(pageNum);
                  pagerResponse.setPageSize(pageSize);
                  pagerResponse.setTotalPages(totalPages);
                  pagerResponse.setTotalRows(totalRows);
                  pagerResponse.setRows(batchList);
                  return pagerResponse;
               } else {
                  pagerResponse.setPageNum(pageNum);
                  pagerResponse.setPageSize(pageSize);
                  pagerResponse.setTotalPages(0);
                  pagerResponse.setTotalRows(0L);
                  pagerResponse.setRows(null);
                  return pagerResponse;
               }
            } else {
               pagerResponse.setPageNum(pageNum);
               pagerResponse.setPageSize(pageSize);
               pagerResponse.setTotalPages(0);
               pagerResponse.setTotalRows(0L);
               pagerResponse.setRows(null);
               return pagerResponse;
            }
         } catch (Exception var25) {
            this.logger.error("查询数据列表调用queryBatchList接口出错", var25);
            throw new VieAppServiceException("查询数据列表异常", var25);
         }
      } else {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      }
   }

   public List<BatchInfo> queryIfLoneBatch(BatchRequest batchRequest) throws VieAppServiceException {
      if (batchRequest.getDataType() != null
         && batchRequest.getIfLone() != null
         && !StringUtils.isNullOrEmpry(batchRequest.getUserId())
         && !StringUtils.isNullOrEmpry(batchRequest.getSystemId())) {
         List<BatchInfo> batchList = null;
         List<BatchInfo> successBatchList = new ArrayList<>();

         try {
            List<Long> idList = this.getGroupIdList(5, Long.parseLong(batchRequest.getUserId()));
            if (idList != null && idList.size() != 0) {
               List<Integer> intIdList = new ArrayList<>();

               for (Long longId : idList) {
                  intIdList.add(Integer.parseInt(longId.toString()));
               }

               batchRequest.setPageSize(Integer.MAX_VALUE);
               int pageSize = batchRequest.getPageSize();
               batchList = this.batchDB.getBatchList(null, null, null, batchRequest.getIfLone(), pageSize, 1, intIdList, batchRequest.getDataType());
               if (batchList != null && batchList.size() != 0) {
                  for (BatchInfo batchInfo : batchList) {
                     if (batchInfo.getStatus() == 1) {
                        successBatchList.add(batchInfo);
                     }
                  }

                  return successBatchList;
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } catch (Exception var9) {
            this.logger.error("查询批次数据调用queryIfLoneBatch接口出错", var9);
            throw new VieAppServiceException("查询批次数据异常", var9);
         }
      } else {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      }
   }

   public BatchResponse getBatchById(BatchRequest batchRequest) throws VieAppServiceException {
      if (batchRequest.getId() != 0 && !StringUtils.isNullOrEmpry(batchRequest.getDataSource()) && !StringUtils.isNullOrEmpry(batchRequest.getUserId())) {
         BatchResponse batchResponse = new BatchResponse();

         try {
            BatchInfo batchInfo = this.batchDB.getBatchById(batchRequest.getId());
            if (batchInfo == null) {
               this.logger.error("数据不存在");
               throw new VieAppServiceException("数据不存在");
            } else {
               String models = batchInfo.getModels();
               if (StringUtils.isNullOrEmpry(models)) {
                  batchResponse.setModelList(null);
               } else {
                  String[] modelIdArray = models.split(",");
                  List<Long> modelIdList = new ArrayList<>();

                  for (int i = 0; i < modelIdArray.length; i++) {
                     Long modelId = Long.parseLong(modelIdArray[i]);
                     modelIdList.add(modelId);
                  }

                  ModelRequest modelRequest = new ModelRequest();
                  modelRequest.setUserId(batchRequest.getUserId());
                  modelRequest.setDataSource(batchRequest.getDataSource());
                  modelRequest.setModelGroupId(-1L);
                  modelRequest.setType("online");
                  modelRequest.setSystemId("VIEWEB");
                  Map<String, Object> modelMap = this.modelService.searModelByGroupService(modelRequest);
                  List<HashMap<String, Object>> mapList = (List<HashMap<String, Object>>)modelMap.get("rows");
                  List<ModelInfo> modelInfoList = new ArrayList<>();
                  List<ModelInfo> modelList = new ArrayList<>();
                  if (mapList != null && mapList.size() > 0) {
                     for (HashMap<String, Object> hashMap : (List<HashMap<String, Object>>)mapList) {
                        long modelId = ((BigDecimal)hashMap.get("modelId")).longValue();
                        String modelName = (String)hashMap.get("modelName");
                        ModelInfo modelInfo = new ModelInfo();
                        modelInfo.setModelId(modelId);
                        modelInfo.setModelName(modelName);
                        modelInfoList.add(modelInfo);
                     }

                     for (int i = 0; i < modelInfoList.size(); i++) {
                        for (int j = 0; j < modelIdList.size(); j++) {
                           if (modelInfoList.get(i).getModelId() == modelIdList.get(j)) {
                              modelList.add(modelInfoList.get(i));
                           }
                        }
                     }
                  }

                  batchResponse.setModelList(modelList);
               }

               batchResponse.setId(batchInfo.getId());
               batchResponse.setBatchId(batchInfo.getBatchId());
               batchResponse.setDataType(batchInfo.getDataType());
               batchResponse.setIfLone(batchInfo.getIfLone());
               batchResponse.setAnalysisType(batchInfo.getAnalysisType());
               batchResponse.setStatus(batchInfo.getStatus());
               batchResponse.setStartTime(batchInfo.getStartTime());
               batchResponse.setEndTime(batchInfo.getEndTime());
               batchResponse.setImportCount(batchInfo.getImportCount());
               batchResponse.setHandleCount(batchInfo.getHandleCount());
               batchResponse.setCreateUser(batchInfo.getCreateUser());
               batchResponse.setRemark(batchInfo.getRemark());
               batchResponse.setFailReason(batchInfo.getFailReason());
               return batchResponse;
            }
         } catch (Exception var18) {
            this.logger.error("根据报表id查询报表详细信息getBatchById接口出错", var18);
            throw new VieAppServiceException("根据报表id查询报表详细信息异常", var18);
         }
      } else {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      }
   }

   public void updateBatch(BatchRequest batchRequest) throws VieAppServiceException {
      BatchInfo batchInfo = this.batchDB.getBatchById(batchRequest.getId());
      List<Long> idList = new ArrayList<>();
      idList.add((long)batchRequest.getId());

      for (BatchModelTask batchModelTask : this.batchModelDB.getDeleteBatchModelTask(idList)) {
         if (batchModelTask.getTaskStatus() != 1 && batchModelTask.getTaskStatus() != -1) {
            this.logger.error("数据正在被编辑，存在创建的任务，现在无法编辑");
            throw new VieAppServiceException("数据正在被编辑，存在创建的任务，现在无法编辑");
         }
      }

      if (batchInfo == null) {
         this.logger.error("数据不存在");
         throw new VieAppServiceException("数据不存在");
      } else if (batchInfo.getStatus() == 0) {
         this.logger.error("数据正在被处理，不能编辑");
         throw new VieAppServiceException("数据正在被处理，不能编辑");
      } else if (StringUtils.isNullOrEmpry(batchRequest.getBatchId())) {
         this.logger.error("批次号不能为空");
         throw new VieAppServiceException("批次号不能为空");
      } else if (batchRequest.getBatchId().length() > 40) {
         this.logger.error("批次号长度不能大于40");
         throw new VieAppServiceException("批次号长度不能大于40");
      } else {
         if (!batchRequest.getBatchId().equals(batchInfo.getBatchId())) {
            boolean flag = false;

            try {
               flag = this.batchDB.checkBatchId(batchRequest.getBatchId());
            } catch (Exception var16) {
               this.logger.error("查询批次号是否重复方法异常", var16);
               throw new VieAppServiceException("查询批次号是否重复方法异常", var16);
            }

            if (flag) {
               this.logger.error("批次号重复");
               throw new VieAppServiceException("批次号重复");
            }
         }

         if (batchInfo.getIfLone() == 1 && StringUtils.isNullOrEmpry(batchRequest.getModels())) {
            this.logger.error("编辑独立数据必须上传模型");
            throw new VieAppServiceException("编辑独立数据必须上传模型");
         } else if (StringUtils.isNullOrEmpry(batchRequest.getCreateUser())) {
            this.logger.error("创建人用户名为空");
            throw new VieAppServiceException("创建人用户名为空");
         } else {
            batchInfo.setBatchId(batchRequest.getBatchId());
            batchInfo.setRemark(batchRequest.getRemark());
            batchInfo.setCreateUser(batchRequest.getCreateUser());

            try {
               int n = 0;
               boolean exitOnlineModel1 = false;
               boolean exitOnlineModel2 = false;
               BatchModelTask batchModelTaskx = new BatchModelTask();
               if (batchInfo.getIfLone() == 1) {
                  if (batchInfo.getStatus() == 1) {
                     String[] modelIdArr1 = batchInfo.getModels().split(",");
                     String[] modelIdArr2 = batchRequest.getModels().split(",");

                     for (String modelId : modelIdArr2) {
                        if (!Arrays.asList(modelIdArr1).contains(modelId)) {
                           exitOnlineModel1 = true;
                           exitOnlineModel2 = true;
                           break;
                        }
                     }

                     if (!exitOnlineModel1) {
                        for (String modelIdx : modelIdArr1) {
                           if (!Arrays.asList(modelIdArr2).contains(modelIdx)) {
                              ModelInfo modelInfo = this.modelDB.queryModelById(Long.parseLong(modelIdx));
                              if (modelInfo != null && modelInfo.getModelStatus() == 0) {
                                 exitOnlineModel1 = true;
                                 break;
                              }
                           }
                        }
                     }

                     if (exitOnlineModel1) {
                        batchInfo.setModels(batchRequest.getModels());
                     }

                     n = this.batchDB.saveOrUpdateBatch(batchInfo);
                  } else {
                     if (batchRequest.getDataType() != null) {
                        batchInfo.setDataType(batchRequest.getDataType());
                     }

                     if (batchRequest.getIfLone() != null) {
                        batchInfo.setIfLone(batchRequest.getIfLone());
                     }

                     if (batchRequest.getAnalysisType() != null) {
                        batchInfo.setAnalysisType(batchRequest.getAnalysisType());
                     }

                     batchInfo.setModels(batchRequest.getModels());
                     batchInfo.setStatus(0);
                     batchInfo.setStartTime(new Date());
                     batchInfo.setEndTime(null);
                     batchInfo.setImportCount(batchRequest.getImportCount());
                     batchInfo.setHandleCount(0);
                     batchInfo.setFailReason(null);
                     n = this.batchDB.saveOrUpdateBatch(batchInfo);
                  }
               } else {
                  if (batchInfo.getStatus() == 1) {
                     this.logger.error("导入成功的融入数据不能被编辑");
                     throw new VieAppServiceException("导入成功的融入数据不能被编辑");
                  }

                  if (batchRequest.getDataType() != null) {
                     batchInfo.setDataType(batchRequest.getDataType());
                  }

                  if (batchRequest.getIfLone() != null) {
                     batchInfo.setIfLone(batchRequest.getIfLone());
                  }

                  if (batchRequest.getAnalysisType() != null) {
                     batchInfo.setAnalysisType(batchRequest.getAnalysisType());
                  }

                  batchInfo.setStatus(0);
                  batchInfo.setStartTime(new Date());
                  batchInfo.setEndTime(null);
                  batchInfo.setImportCount(batchRequest.getImportCount());
                  batchInfo.setHandleCount(0);
                  batchInfo.setFailReason(null);
                  n = this.batchDB.saveOrUpdateBatch(batchInfo);
               }

               if (n > 0 && batchInfo.getIfLone() != null && batchInfo.getIfLone() == 1 && batchInfo.getStatus() == 1 && exitOnlineModel2) {
                  batchModelTaskx.setBatchId(batchRequest.getId());
                  batchModelTaskx.setModels(batchRequest.getModels());
                  this.batchModelDB.saveOrUpdateBatchMode(batchModelTaskx);
               }
            } catch (Exception var17) {
               this.logger.error("编辑独立数据updateBatch接口出错", var17);
               throw new VieAppServiceException("编辑独立数据异常", var17);
            }
         }
      }
   }

   public ServiceResponse deleteBatch(BatchRequest batchRequest) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(batchRequest.getIds())) {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      } else {
         ServiceResponse serviceResponse = new ServiceResponse();
         serviceResponse.setSuccessful(true);

         try {
            String[] idArrays = batchRequest.getIds().split(",");
            List<Integer> idList = new ArrayList<>();

            for (String id : idArrays) {
               idList.add(Integer.parseInt(id));
            }

            String msg = null;
            List<BatchInfo> list = this.batchDB.getBatchStatus(idList);
            List<Integer> newIdList = new ArrayList<>();
            serviceResponse.setSuccessful(true);
            if (list != null && list.size() != 0) {
               for (BatchInfo batchInfo : list) {
                  if (batchInfo.getStatus() != 0) {
                     newIdList.add(batchInfo.getId());
                  }
               }

               if (newIdList.size() < idList.size() && StringUtils.isNullOrEmpry(msg)) {
                  msg = "数据中存在正在处理的数据无法删除";
                  serviceResponse.setSuccessful(false);
               }

               newIdList.clear();

               for (BatchInfo batchInfox : list) {
                  if (batchInfox.getStatus() == 1 && batchInfox.getIfLone() == 0) {
                     newIdList.add(batchInfox.getId());
                  }
               }

               if (newIdList != null && newIdList.size() > 0 && StringUtils.isNullOrEmpry(msg)) {
                  msg = "数据中存在处理成功的融入数据无法删除";
                  serviceResponse.setSuccessful(false);
               }

               newIdList.clear();

               for (BatchInfo batchInfoxx : list) {
                  if (batchInfoxx.getIfLone() == 0 && batchInfoxx.getStatus() == 2 || batchInfoxx.getIfLone() == 1 && batchInfoxx.getStatus() != 0) {
                     newIdList.add(batchInfoxx.getId());
                  }
               }
            } else {
               msg = "要删除的数据不存在";
               serviceResponse.setSuccessful(false);
            }

            List<Long> longIdList = new ArrayList<>();
            List<BatchModelTask> batchModelTaskList = null;
            if (newIdList != null && newIdList.size() > 0) {
               for (Integer integer : newIdList) {
                  longIdList.add(Long.parseLong(integer.toString()));
               }

               if (longIdList != null && longIdList.size() > 0) {
                  batchModelTaskList = this.batchModelDB.getDeleteBatchModelTask(longIdList);
               }
            }

            List<Long> deleteIdList = new ArrayList<>();
            if (batchModelTaskList != null && batchModelTaskList.size() > 0) {
               for (int i = 0; i < batchModelTaskList.size(); i++) {
                  BatchModelTask batchModelTask = batchModelTaskList.get(i);
                  int taskStatus = batchModelTask.getTaskStatus();
                  if (taskStatus == 1) {
                     long id = batchModelTask.getBatchId();
                     deleteIdList.add(id);
                  } else {
                     longIdList.remove(batchModelTask.getBatchId());
                     if (StringUtils.isNullOrEmpry(msg)) {
                        msg = "删除对象中包含上线中的数据，需处理完成后才可删除";
                        serviceResponse.setSuccessful(false);
                     }
                  }
               }

               if (deleteIdList != null && deleteIdList.size() > 0) {
                  this.batchModelDB.deleteBatchModelTask(deleteIdList);
               }
            }

            List<Integer> intIdList = new ArrayList<>();
            boolean flag = false;
            if (longIdList != null && longIdList.size() > 0) {
               for (Long longid : longIdList) {
                  int id = Integer.parseInt(longid.toString());
                  intIdList.add(id);
               }

               flag = this.batchDB.deleteBatch(intIdList);
            }

            if (flag) {
               GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
               groupAuthRequest.setGroupType(5);
               groupAuthRequest.setGroupIds(longIdList);
               if (!this.groupAuthService.deleteGroupAuth(groupAuthRequest).isStatus()) {
                  this.logger.error("删除数据时调用权限系统删除组关系失败");
                  throw new VieAppServiceException("删除数据时调用权限系统删除组关系失败");
               }
            }

            serviceResponse.setMessage(msg);
            return serviceResponse;
         } catch (Exception var16) {
            this.logger.error("删除独立数据接口deleteBatch出错", var16);
            throw new VieAppServiceException("删除独立数据异常", var16);
         }
      }
   }

   public List<BatchInfo> getBatchStatus(BatchRequest batchRequest) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(batchRequest.getIds())) {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      } else {
         try {
            String[] idArrays = batchRequest.getIds().split(",");
            List<Integer> idList = new ArrayList<>();

            for (String id : idArrays) {
               idList.add(Integer.parseInt(id));
            }

            return this.batchDB.getBatchStatus(idList);
         } catch (Exception var8) {
            this.logger.error("获取数据状态getBatchStatus接口出错", var8);
            throw new VieAppServiceException("获取数据状态信息异常", var8);
         }
      }
   }

   public List<Long> getModelIdByBatch(BatchRequest batchRequest) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(batchRequest.getBatchId())) {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      } else {
         try {
            return this.batchDB.getModelIdByBatch(batchRequest.getBatchId());
         } catch (Exception var3) {
            this.logger.error("通过批次号查询模型getModelIdByBatch接口出错", var3);
            throw new VieAppServiceException("通过批次号查询模型ID异常", var3);
         }
      }
   }

   public List<BatchDimension> getDimensionByDataSource(String dataSource) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(dataSource)) {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      } else {
         List<BatchDimension> list = null;

         try {
            return this.batchDimensionlDB.queryDimension(dataSource);
         } catch (Exception var4) {
            this.logger.error("通过数据源获取维度信息getDimensionByDataSource接口出错", var4);
            throw new VieAppServiceException("通过数据源获取维度信息异常", var4);
         }
      }
   }

   private List<Long> getGroupIdList(int groupType, long userId) throws VieAppServiceException {
      GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
      groupAuthRequest.setUserId(userId);
      groupAuthRequest.setGroupType(groupType);

      try {
         return this.groupAuthService.queryGroupAuth(groupAuthRequest);
      } catch (Exception var6) {
         this.logger.error("获取组id列表getGroupIdList接口出错", var6);
         throw new VieAppServiceException("获取组id列表异常", var6);
      }
   }

   public void setBatchDB(BatchDB batchDB) {
      this.batchDB = batchDB;
   }

   public void setModelService(ModelService modelService) {
      this.modelService = modelService;
   }

   public void setModelDB(ModelDB modelDB) {
      this.modelDB = modelDB;
   }

   public void setBatchModelDB(BatchModelDB batchModelDB) {
      this.batchModelDB = batchModelDB;
   }

   public void setGroupAuthService(GroupAuthService groupAuthService) {
      this.groupAuthService = groupAuthService;
   }

   public void setBatchDimensionlDB(BatchDimensionDB batchDimensionlDB) {
      this.batchDimensionlDB = batchDimensionlDB;
   }
}
