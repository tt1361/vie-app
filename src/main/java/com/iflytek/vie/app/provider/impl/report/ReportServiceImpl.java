package com.iflytek.vie.app.provider.impl.report;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.report.ReportGroupService;
import com.iflytek.vie.app.api.report.ReportService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.common.PagerResponse;
import com.iflytek.vie.app.pojo.permission.DimensionAuth;
import com.iflytek.vie.app.pojo.report.ReportConfig;
import com.iflytek.vie.app.pojo.report.ReportDeleteRequest;
import com.iflytek.vie.app.pojo.report.ReportGroup;
import com.iflytek.vie.app.pojo.report.ReportGroupIdRequest;
import com.iflytek.vie.app.pojo.report.ReportRequest;
import com.iflytek.vie.app.pojo.report.ReportSimpleRequest;
import com.iflytek.vie.app.pojo.report.ReportTopRequest;
import com.iflytek.vie.app.pojo.report.ReportUsualRequest;
import com.iflytek.vie.app.provider.common.AnyDrillParse;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.common.ReportType;
import com.iflytek.vie.app.provider.common.TableColType;
import com.iflytek.vie.app.provider.database.CommonReportDB;
import com.iflytek.vie.app.provider.database.ReportConfigDB;
import com.iflytek.vie.app.provider.database.ReportGroupDB;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportServiceImpl implements ReportService {
   private final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);
   protected ObjectMapper mapper = new ObjectMapper();
   private ReportGroupService reportGroupService;
   private CommonReportDB commonReportDB;
   private ReportGroupDB reportGroupDB;
   private ReportConfigDB reportConfigDB;
   private CommonData commonData;

   public PagerResponse<HashMap<String, Object>> queryCommonReport(ReportSimpleRequest reportSimpleRequest) throws VieAppServiceException {
      if (reportSimpleRequest.getSystemId() != null
         && !"".equals(reportSimpleRequest.getSystemId())
         && reportSimpleRequest.getUserId() != null
         && !"".equals(reportSimpleRequest.getUserId())) {
         PagerResponse<HashMap<String, Object>> pagerResponse = new PagerResponse();

         try {
            ReportGroupIdRequest reportGroupIdRequest = new ReportGroupIdRequest();
            reportGroupIdRequest.setGroupType(2);
            reportGroupIdRequest.setUserId(Long.valueOf(reportSimpleRequest.getUserId()));
            reportGroupIdRequest.setDataSource(reportSimpleRequest.getDataSource());
            List<Long> groupIds = this.reportGroupService.getGroupIdList(reportGroupIdRequest);
            if (null != groupIds && groupIds.size() > 0) {
               List<Integer> grouoIdsInt = new ArrayList<>();

               for (Long groupId : groupIds) {
                  grouoIdsInt.add(groupId.intValue());
               }

               List<Integer> reportList = this.commonReportDB.fetchReportByGroup(grouoIdsInt);
               List<Integer> rcList = this.commonReportDB.getCommonReportByIds(reportList);
               int totalRows = 0;
               String reportName;
               if (reportSimpleRequest.getReportName() != null && !reportSimpleRequest.getReportName().trim().isEmpty()) {
                  reportName = reportSimpleRequest.getReportName();
               } else {
                  reportName = null;
               }

               List<HashMap<String, Object>> list = new ArrayList<>();
               if (rcList != null && rcList.size() > 0) {
                  List<ReportConfig> reportConfigList = this.commonReportDB
                     .getCommonReportByPage(reportSimpleRequest.getPageSize(), reportSimpleRequest.getPageNum(), reportName, rcList);
                  totalRows = (int)this.commonReportDB.getCountCommonReport(reportName, rcList);

                  for (ReportConfig reportConfig : reportConfigList) {
                     HashMap<String, Object> map = new HashMap<>();
                     map.put("reportId", reportConfig.getId());
                     map.put("reportName", reportConfig.getName());
                     Set<ReportGroup> reportGroup = reportConfig.getReportGroup();
                     Iterator<ReportGroup> iterator = reportGroup.iterator();
                     String groupName = "";
                     int groupId = -1;
                     if (reportGroup.size() > 0) {
                        while (iterator.hasNext()) {
                           ReportGroup rg = iterator.next();
                           if (reportGroup.size() > 1) {
                              if (!rg.getName().equals("reportGroupName")) {
                                 groupName = rg.getName() + "(" + reportSimpleRequest.getUserName() + ")";
                                 groupId = rg.getId();
                              }
                           } else {
                              groupName = rg.getName() + "(" + reportSimpleRequest.getUserName() + ")";
                              groupId = rg.getId();
                           }
                        }

                        map.put("reportGroupId", groupId);
                        map.put("reportGroupName", groupName);
                        list.add(map);
                     }
                  }
               }

               pagerResponse.setPageNum(reportSimpleRequest.getPageNum());
               pagerResponse.setPageSize(reportSimpleRequest.getPageSize());
               pagerResponse.setTotalRows(totalRows);
               pagerResponse.setRows(list);
            }

            return pagerResponse;
         } catch (Exception var20) {
            this.logger.error("查询报表组调用queryCommonReport接口出错", var20);
            throw new VieAppServiceException("查询报表组异常", var20);
         }
      } else {
         this.logger.error("输入参数异常");
         throw new VieAppServiceException("输入参数异常");
      }
   }

   public void deleteCommonReport(ReportDeleteRequest reportDeleteRequest) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(reportDeleteRequest.getReportIds())) {
         this.logger.error("输入参数异常");
         throw new VieAppServiceException("输入参数异常");
      } else {
         try {
            this.commonReportDB.deleteCommonReport(reportDeleteRequest.getReportIds());
         } catch (Exception var3) {
            this.logger.error("删除常用报表deleteCommonReport接口出错", var3);
            throw new VieAppServiceException("删除常用报表", var3);
         }
      }
   }

   public PagerResponse<ReportConfig> queryReportList(ReportSimpleRequest reportSimpleRequest) throws VieAppServiceException {
      if (reportSimpleRequest.getSystemId() != null
         && !"".equals(reportSimpleRequest.getSystemId())
         && reportSimpleRequest.getUserId() != null
         && !"".equals(reportSimpleRequest.getUserId())) {
         PagerResponse<ReportConfig> pagerResponse = new PagerResponse();

         try {
            ReportGroupIdRequest reportGroupIdRequest = new ReportGroupIdRequest();
            reportGroupIdRequest.setGroupType(2);
            reportGroupIdRequest.setUserId(Long.valueOf(reportSimpleRequest.getUserId()));
            reportGroupIdRequest.setDataSource(reportSimpleRequest.getDataSource());
            List<Long> groupIds = this.reportGroupService.getGroupIdList(reportGroupIdRequest);
            if (null != groupIds && groupIds.size() != 0) {
               List<Integer> reportGroupIds = new ArrayList<>();

               for (Long groupId : groupIds) {
                  reportGroupIds.add(groupId.intValue());
               }

               if (this.reportGroupDB.isMyReportGroup(reportSimpleRequest.getReportGroupId(), reportSimpleRequest.getUserId())
                  || reportSimpleRequest.getReportGroupId() == -1) {
                  int totalRows = (int)this.reportConfigDB
                     .fetchReportCountByIds(reportSimpleRequest.getReportGroupId(), reportSimpleRequest.getReportName(), reportGroupIds);
                  List<ReportConfig> reportConfigs = this.reportConfigDB
                     .fetchReportByGroups(
                        reportSimpleRequest.getReportGroupId(),
                        reportSimpleRequest.getPageSize(),
                        reportSimpleRequest.getPageNum(),
                        reportSimpleRequest.getReportName(),
                        reportGroupIds
                     );
                  List<Integer> reportIds = this.reportConfigDB.queryUsualReport(reportSimpleRequest.getUserId());
                  if (null != reportConfigs && reportConfigs.size() > 0) {
                     for (ReportConfig reportConfig : reportConfigs) {
                        int isUp = reportConfig.getIsUp();
                        byte var15;
                        if (isUp > 0) {
                           var15 = 1;
                        } else {
                           var15 = 0;
                        }

                        reportConfig.setIsUp(var15);
                        if (reportIds.contains(reportConfig.getId())) {
                           reportConfig.setIsGeneral(1);
                        } else {
                           reportConfig.setIsGeneral(0);
                        }
                     }
                  }

                  pagerResponse.setPageNum(reportSimpleRequest.getPageNum());
                  pagerResponse.setPageSize(reportSimpleRequest.getPageSize());
                  pagerResponse.setTotalRows(totalRows);
                  pagerResponse.setRows(reportConfigs);
               }

               return pagerResponse;
            } else {
               return pagerResponse;
            }
         } catch (Exception var12) {
            this.logger.error("分页获取所有报表列表queryReportList接口出错", var12);
            throw new VieAppServiceException("分页获取所有报表列表", var12);
         }
      } else {
         this.logger.error("输入参数异常");
         throw new VieAppServiceException("输入参数异常");
      }
   }

   public String getReportById(ReportRequest reportRequest) throws VieAppServiceException {
      try {
         ReportConfig reportConfig = this.reportConfigDB.getReportById(reportRequest.getReportId());
         if (reportConfig == null) {
            this.logger.error("报表不存在");
            throw new VieAppServiceException("报表不存在");
         } else {
            String reportTemplate = reportConfig.getReportTemplate();
            HashMap<String, Object> reportParam = (HashMap<String, Object>)this.mapper
               .readValue(reportTemplate, new TypeReference<HashMap<String, Object>>() {});
            List<LinkedHashMap<String, Object>> reportConfigMap = (List<LinkedHashMap<String, Object>>)reportParam.get("reportCondition");
            boolean isContain = this.isContain(
               reportConfigMap,
               this.commonData.getDataMap(Long.parseLong(reportRequest.getUserId()), reportRequest.getSystemId(), reportRequest.getDataSource())
            );
            if (!isContain) {
               this.logger.error("数据权限已修改！该报表不能使用");
               throw new VieAppServiceException("数据权限已修改！该报表不能使用");
            } else {
               return reportTemplate;
            }
         }
      } catch (Exception var7) {
         this.logger.error("根据报表id查询报表详细信息getReportById接口出错", var7);
         throw new VieAppServiceException("根据报表id查询报表详细信息", var7);
      }
   }

   private boolean isContain(List<LinkedHashMap<String, Object>> reportCond, List<DimensionAuth> dataCond) throws VieAppServiceException {
      boolean isContain = false;

      try {
         if (reportCond != null && reportCond.size() != 0 && dataCond != null && dataCond.size() != 0) {
            HashMap<String, List<String>> data = new HashMap<>();

            for (DimensionAuth dimensionAuth : dataCond) {
               data.put(dimensionAuth.getEnglishName(), dimensionAuth.getValue());
            }

            boolean isMulSel = false;
            boolean isContainDataD = false;

            for (LinkedHashMap<String, Object> cd : reportCond) {
               String type = cd.get(CommonParams.conditionType).toString().trim();
               switch (DimType.valueOf(type)) {
                  case mulSel:
                     String filed = cd.get(CommonParams.conditionFiled).toString();
                     List<String> currentList = data.get(filed);
                     if (currentList != null) {
                        List<String> reportC = (ArrayList)cd.get(CommonParams.conditionValue);
                        reportC.retainAll(currentList);
                        if (reportC.size() > 0) {
                           return true;
                        }

                        isContainDataD = true;
                     }

                     isMulSel = true;
               }
            }

            isContain = isMulSel ? isContain : true;
            isContain = isContainDataD ? isContain : true;
         } else {
            isContain = true;
         }

         return isContain;
      } catch (Exception var13) {
         this.logger.error("检查是否包含数据权限出错", var13);
         throw new VieAppServiceException("检查是否包含数据权限出错", var13);
      }
   }

   public int saveReport(ReportRequest reportRequest) throws VieAppServiceException {
      try {
         String errorMsg = this.checkParams(reportRequest);
         int reportId = reportRequest.getReportId();
         if (StringUtils.isNullOrEmpry(errorMsg)) {
            if (reportRequest.getReportId() != -1) {
               this.upadteReportInfo(reportRequest);
            } else {
               reportId = this.addReportInfo(reportRequest);
            }

            return reportId;
         } else {
            this.logger.error(errorMsg);
            throw new VieAppServiceException(errorMsg);
         }
      } catch (Exception var4) {
         if (var4 instanceof VieAppServiceException) {
            this.logger.error(var4.getMessage());
            throw new VieAppServiceException(var4.getMessage());
         } else {
            this.logger.error("保存报表saveReport接口出错", var4);
            throw new VieAppServiceException("保存报表出错", var4);
         }
      }
   }

   private int upadteReportInfo(ReportRequest reportRequest) throws VieAppServiceException {
      try {
         if (!StringUtils.isNullOrEmpry(reportRequest.getReportName())) {
            ReportConfig reportConfig = this.reportConfigDB.getReportById(reportRequest.getReportId());
            if (reportConfig != null) {
               boolean checkReport = true;
               if (!reportRequest.getReportName().equals(reportConfig.getName())) {
                  ReportSimpleRequest reportSimpleRequest = new ReportSimpleRequest();
                  reportSimpleRequest.setUserId(reportRequest.getUserId());
                  reportSimpleRequest.setDataSource(reportRequest.getDataSource());
                  reportSimpleRequest.setReportName(reportRequest.getReportName());
                  checkReport = this.checkReportName(reportSimpleRequest);
               }

               if (checkReport) {
                  ReportGroup reportGroup = this.reportGroupDB.fetchReportGroupById(reportRequest.getReportGroupId());
                  if (reportGroup != null) {
                     reportConfig.setUpdateUser(reportRequest.getCreateUser());
                     reportConfig.setName(reportRequest.getReportName());
                     this.dealReportGroup(reportGroup, reportConfig);
                     List<HashMap<String, Object>> reportParam = null;
                     reportParam = (List<HashMap<String, Object>>)this.mapper
                        .readValue(reportRequest.getReportConfig(), new TypeReference<List<HashMap<String, Object>>>() {});
                     List<HashMap<String, Object>> reportCond = new ArrayList<>();
                     if (!StringUtils.isNullOrEmpry(reportRequest.getReportCondition())) {
                        reportCond = (List<HashMap<String, Object>>)this.mapper
                           .readValue(reportRequest.getReportCondition(), new TypeReference<List<HashMap<String, Object>>>() {});
                     }

                     HashMap<String, Object> reportTem = new HashMap<>();
                     reportTem.put("startTime", reportRequest.getStartTime());
                     reportTem.put("endTime", reportRequest.getEndTime());
                     reportTem.put("timeType", reportRequest.getTimeType());
                     reportTem.put("timeValue", reportRequest.getTimeValue());
                     reportTem.put("reportGroupID", reportRequest.getReportGroupId());
                     reportTem.put("reportName", reportRequest.getReportName());
                     reportTem.put("comment", reportRequest.getComment());
                     reportTem.put("reportConfig", reportParam);
                     reportTem.put("reportCondition", reportCond);
                     reportConfig.setReportTemplate(this.mapper.writeValueAsString(reportTem));
                     reportConfig.setRemark(reportRequest.getComment());
                     reportConfig.setUpdateTime(new Date());
                     this.reportConfigDB.saveOrUpdateReportConfig(reportConfig);
                     return reportConfig.getId();
                  } else {
                     this.logger.error("报表组不存在！");
                     throw new VieAppServiceException("报表组不存在！");
                  }
               } else {
                  this.logger.error("报表名称{}已存在！", reportRequest.getReportName());
                  throw new VieAppServiceException("报表名称" + reportRequest.getReportName() + "已存在！");
               }
            } else {
               this.logger.error("报表{}不存在！", reportRequest.getReportName());
               throw new VieAppServiceException("报表" + reportRequest.getReportName() + "不存在！");
            }
         } else {
            this.logger.error("报表名称不能为空！");
            throw new VieAppServiceException("报表名称不能为空！");
         }
      } catch (Exception var8) {
         if (var8 instanceof VieAppServiceException) {
            this.logger.error(var8.getMessage());
            throw new VieAppServiceException(var8.getMessage());
         } else {
            this.logger.error("更新报表出错", var8);
            throw new VieAppServiceException("更新报表出错", var8);
         }
      }
   }

   private void dealReportGroup(ReportGroup reportGroup, ReportConfig reportConfig) {
      Set<ReportGroup> reportGroupSet = new HashSet<>();
      reportGroupSet.add(reportGroup);
      Set<ReportGroup> rgSet = reportConfig.getReportGroup();
      ReportGroup shareGroup = null;

      for (ReportGroup current : rgSet) {
         if (CommonParams.reportGroupName.equals(current.getName())) {
            shareGroup = current;
         }
      }

      if (shareGroup != null) {
         reportGroupSet.add(shareGroup);
      }

      reportConfig.setReportGroup(reportGroupSet);
   }

   private int addReportInfo(ReportRequest reportRequest) throws VieAppServiceException {
      try {
         if (!StringUtils.isNullOrEmpry(reportRequest.getReportName())) {
            ReportConfig reportConfig = new ReportConfig();
            ReportSimpleRequest reportSimpleRequest = new ReportSimpleRequest();
            reportSimpleRequest.setUserId(reportRequest.getUserId());
            reportSimpleRequest.setDataSource(reportRequest.getDataSource());
            reportSimpleRequest.setReportName(reportRequest.getReportName());
            boolean checkReport = this.checkReportName(reportSimpleRequest);
            if (checkReport) {
               ReportGroup reportGroup = this.reportGroupDB.fetchReportGroupById(reportRequest.getReportGroupId());
               if (reportGroup != null) {
                  reportConfig.setCreateUser(reportRequest.getCreateUser());
                  reportConfig.setName(reportRequest.getReportName());
                  Set<ReportGroup> reportGroupSet = new HashSet<>();
                  reportGroupSet.add(reportGroup);
                  reportConfig.setReportGroup(reportGroupSet);
                  List<HashMap<String, Object>> reportParam = null;
                  reportParam = (List<HashMap<String, Object>>)this.mapper
                     .readValue(reportRequest.getReportConfig(), new TypeReference<List<HashMap<String, Object>>>() {});
                  List<HashMap<String, Object>> reportCond = new ArrayList<>();
                  if (!StringUtils.isNullOrEmpry(reportRequest.getReportCondition())) {
                     reportCond = (List<HashMap<String, Object>>)this.mapper
                        .readValue(reportRequest.getReportCondition(), new TypeReference<List<HashMap<String, Object>>>() {});
                  }

                  HashMap<String, Object> reportTem = new HashMap<>();
                  reportTem.put("startTime", reportRequest.getStartTime());
                  reportTem.put("endTime", reportRequest.getEndTime());
                  reportTem.put("timeType", reportRequest.getTimeType());
                  reportTem.put("timeValue", reportRequest.getTimeValue());
                  reportTem.put("reportGroupID", reportRequest.getReportGroupId());
                  reportTem.put("reportName", reportRequest.getReportName());
                  reportTem.put("comment", reportRequest.getComment());
                  reportTem.put("reportConfig", reportParam);
                  reportTem.put("reportCondition", reportCond);
                  reportConfig.setReportTemplate(this.mapper.writeValueAsString(reportTem));
                  reportConfig.setCreateTime(new Date());
                  reportConfig.setRemark(reportRequest.getComment());
                  this.reportConfigDB.saveOrUpdateReportConfig(reportConfig);
                  return reportConfig.getId();
               } else {
                  this.logger.error("报表组不存在！");
                  throw new VieAppServiceException("报表组不存在！");
               }
            } else {
               this.logger.error("报表名称{}已存在！", reportRequest.getReportName());
               throw new VieAppServiceException("报表名称" + reportRequest.getReportName() + "已存在！");
            }
         } else {
            this.logger.error("报表名称不能为空！");
            throw new VieAppServiceException("报表名称不能为空！");
         }
      } catch (Exception var10) {
         if (var10 instanceof VieAppServiceException) {
            this.logger.error(var10.getMessage());
            throw new VieAppServiceException(var10.getMessage());
         } else {
            this.logger.error("新增报表出错", var10);
            throw new VieAppServiceException("新增报表出错", var10);
         }
      }
   }

   private String checkParams(ReportRequest reportRequest) throws VieAppServiceException {
      StringBuffer errormsg = new StringBuffer("");
      boolean isPass = true;

      try {
         List<HashMap<String, Object>> reportParam = (List<HashMap<String, Object>>)this.mapper
            .readValue(reportRequest.getReportConfig(), new TypeReference<List<HashMap<String, Object>>>() {});
         AnyDrillParse anyDrillParse = new AnyDrillParse();
         anyDrillParse.setMeasureList(this.commonData.getMeasureList());

         for (int i = 0; i < reportParam.size(); i++) {
            HashMap<String, Object> currentParam = reportParam.get(i);
            Object reportType = currentParam.get(CommonParams.reportType);
            if (reportType != null) {
               switch (ReportType.valueOf(reportType.toString())) {
                  case table:
                     isPass = this.checkTabCol((List<HashMap<String, String>>)currentParam.get(CommonParams.tabCol), anyDrillParse);
                     if (!isPass) {
                        errormsg.append(anyDrillParse.erroeMsg);
                     }
                     break;
                  case lineColumChart:
                     isPass = this.checkLineCol(
                        (List<HashMap<String, String>>)currentParam.get(CommonParams.lineColMainAxis),
                        (List<HashMap<String, String>>)currentParam.get(CommonParams.lineColSecondaryAxis),
                        anyDrillParse
                     );
                     if (!isPass) {
                        errormsg.append(anyDrillParse.erroeMsg);
                     }
                     break;
                  case pieChart:
                     isPass = this.checkPieCol(currentParam, anyDrillParse);
                     if (!isPass) {
                        errormsg.append(anyDrillParse.erroeMsg);
                     }
               }
            }
         }
      } catch (Exception var9) {
         this.logger.error(var9.getMessage());
         throw new VieAppServiceException(var9);
      }

      return errormsg.toString();
   }

   private boolean checkTabCol(List<HashMap<String, String>> current, AnyDrillParse anyDrillParse) throws VieAppServiceException {
      boolean isPass = true;

      try {
         for (HashMap<String, String> cd : (List<HashMap<String, String>>)current) {
            String type = cd.get(CommonParams.tabColType);
            if (TableColType.computer.toString().equals(type)) {
               String exp = cd.get(CommonParams.tabColExp);
               if (StringUtils.isNullOrEmpry(anyDrillParse.switchComputerExpress(exp))) {
                  isPass = false;
                  break;
               }
            }
         }

         return isPass;
      } catch (Exception var8) {
         this.logger.error(var8.getMessage());
         throw new VieAppServiceException(var8);
      }
   }

   private boolean checkLineCol(List<HashMap<String, String>> mainAxis, List<HashMap<String, String>> secondAxis, AnyDrillParse anyDrillParse) throws VieAppServiceException {
      boolean isPass = true;

      try {
         for (HashMap<String, String> cd : (List<HashMap<String, String>>)mainAxis) {
            String type = cd.get(CommonParams.tabColType);
            if (TableColType.computer.toString().equals(type)) {
               String exp = cd.get(CommonParams.tabColExp);
               if (StringUtils.isNullOrEmpry(anyDrillParse.switchComputerExpress(exp))) {
                  isPass = false;
                  break;
               }
            }
         }

         for (HashMap<String, String> cdx : (List<HashMap<String, String>>)secondAxis) {
            String type = cdx.get(CommonParams.tabColType);
            if (TableColType.computer.toString().equals(type)) {
               String exp = cdx.get(CommonParams.lineColExp);
               if (StringUtils.isNullOrEmpry(anyDrillParse.switchComputerExpress(exp))) {
                  isPass = false;
                  break;
               }
            }
         }

         return isPass;
      } catch (Exception var9) {
         this.logger.error(var9.getMessage());
         throw new VieAppServiceException(var9);
      }
   }

   private boolean checkPieCol(HashMap<String, Object> data, AnyDrillParse anyDrillParse) throws VieAppServiceException {
      boolean isPass = true;

      try {
         String type = data.get(CommonParams.measureOrcomputer).toString();
         if (TableColType.computer.toString().equals(type)) {
            String exp = data.get(CommonParams.pieMExp).toString();
            if (StringUtils.isNullOrEmpry(anyDrillParse.switchComputerExpress(exp))) {
               isPass = false;
            }
         }

         return isPass;
      } catch (Exception var6) {
         this.logger.error("验证饼图列出错", var6);
         throw new VieAppServiceException(var6);
      }
   }

   public void setUsualReport(ReportUsualRequest reportUsualRequest) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(reportUsualRequest.getReportIds())) {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      } else {
         try {
            if (!this.reportConfigDB.fetchReportById(reportUsualRequest.getReportIds(), reportUsualRequest.getUserId())) {
               this.logger.error("选择的操作报表已经不存在，请刷新页面");
               throw new VieAppServiceException("选择的操作报表已经不存在，请刷新页面");
            } else if (!this.reportConfigDB.usualReportLog(reportUsualRequest.getReportIds(), reportUsualRequest.getType(), reportUsualRequest.getUserId())) {
               this.logger.error("操作失败");
               throw new VieAppServiceException("操作失败");
            }
         } catch (Exception var3) {
            this.logger.error("添加/取消常用报表接口setUsualReport出错", var3);
            throw new VieAppServiceException("添加/取消常用报表接口出错", var3);
         }
      }
   }

   public void deleteReport(ReportDeleteRequest reportDeleteRequest) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(reportDeleteRequest.getId())) {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      } else {
         try {
            List<ReportConfig> list = this.reportConfigDB.fetchReportById(reportDeleteRequest.getId());
            if (list == null || list.size() < 1) {
               this.logger.error("该报表已不存在，请刷新页面后重新操作");
               throw new VieAppServiceException("该报表已不存在，请刷新页面后重新操作");
            } else if (!this.reportConfigDB.deleteReportById(String.valueOf(reportDeleteRequest.getId()))) {
               this.logger.error("删除失败");
               throw new VieAppServiceException("删除失败");
            }
         } catch (Exception var3) {
            this.logger.error("删除报表接口deleteReport出错", var3);
            throw new VieAppServiceException("删除报表接口出错", var3);
         }
      }
   }

   public void setUpReport(ReportTopRequest reportTopRequest) throws VieAppServiceException {
      try {
         int count = 0;
         if (reportTopRequest.getType() == 1) {
            count = this.reportConfigDB.fetchReportUpCountById(reportTopRequest.getReportId(), reportTopRequest.getUserId()) + 1;
         }

         this.reportConfigDB.updateReportconfig(count, reportTopRequest.getReportId());
      } catch (Exception var3) {
         this.logger.error("验证报表名称是否已存在接口setUpReport出错", var3);
         throw new VieAppServiceException("验证报表名称是否已存在接口出错", var3);
      }
   }

   public boolean checkReportName(ReportSimpleRequest reportSimpleRequest) throws VieAppServiceException {
      try {
         ReportGroupIdRequest reportGroupIdRequest = new ReportGroupIdRequest();
         reportGroupIdRequest.setGroupType(2);
         reportGroupIdRequest.setUserId(Long.valueOf(reportSimpleRequest.getUserId()));
         reportGroupIdRequest.setDataSource(reportSimpleRequest.getDataSource());
         List<Long> groupIds = this.reportGroupService.getGroupIdList(reportGroupIdRequest);
         List<Integer> reportGroupIds = new ArrayList<>();

         for (Long groupId : groupIds) {
            reportGroupIds.add(groupId.intValue());
         }

         for (ReportConfig reportConfig : this.reportConfigDB.fetchReportByGroups(reportGroupIds)) {
            if (reportConfig.getName().equals(reportSimpleRequest.getReportName())) {
               return false;
            }
         }

         return true;
      } catch (Exception var8) {
         this.logger.error("验证报表名称是否已存在接口checkReportName出错", var8);
         throw new VieAppServiceException("验证报表名称是否已存在接口出错", var8);
      }
   }

   public void checkExpress(String expression) throws VieAppServiceException {
      try {
         if (this.checkExp(expression)) {
            AnyDrillParse anyDrillParse = new AnyDrillParse();
            anyDrillParse.setMeasureList(this.commonData.getMeasureList());
            anyDrillParse.switchComputerExpress(expression);
            if (!StringUtils.isNullOrEmpry(anyDrillParse.erroeMsg)) {
               this.logger.error(anyDrillParse.erroeMsg);
               throw new VieAppServiceException(anyDrillParse.erroeMsg);
            }
         } else {
            this.logger.error("表达式错误：除零");
            throw new VieAppServiceException("表达式错误：除零");
         }
      } catch (Exception var3) {
         this.logger.error("验证报表显示字段计算项表达式接口checkExpress出错", var3);
         throw new VieAppServiceException("验证报表显示字段计算项表达式接口出错", var3);
      }
   }

   private boolean checkExp(String exp) {
      boolean isPass = false;
      if (exp.indexOf("/0") == -1) {
         isPass = true;
      }

      return isPass;
   }

   public String getComputerField() throws VieAppServiceException {
      return "c" + new Date().getTime();
   }

   public List<HashMap<String, String>> getMeasureList() throws VieAppServiceException {
      try {
         return this.commonData.getMeasureList();
      } catch (Exception var2) {
         this.logger.error("查询所有指标出错", var2);
         throw new VieAppServiceException("查询所有指标出错。", var2);
      }
   }

   public ObjectMapper getMapper() {
      return this.mapper;
   }

   public void setMapper(ObjectMapper mapper) {
      this.mapper = mapper;
   }

   public ReportGroupService getReportGroupService() {
      return this.reportGroupService;
   }

   public void setReportGroupService(ReportGroupService reportGroupService) {
      this.reportGroupService = reportGroupService;
   }

   public CommonReportDB getCommonReportDB() {
      return this.commonReportDB;
   }

   public void setCommonReportDB(CommonReportDB commonReportDB) {
      this.commonReportDB = commonReportDB;
   }

   public ReportGroupDB getReportGroupDB() {
      return this.reportGroupDB;
   }

   public void setReportGroupDB(ReportGroupDB reportGroupDB) {
      this.reportGroupDB = reportGroupDB;
   }

   public ReportConfigDB getReportConfigDB() {
      return this.reportConfigDB;
   }

   public void setReportConfigDB(ReportConfigDB reportConfigDB) {
      this.reportConfigDB = reportConfigDB;
   }

   public CommonData getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonData commonData) {
      this.commonData = commonData;
   }
}
