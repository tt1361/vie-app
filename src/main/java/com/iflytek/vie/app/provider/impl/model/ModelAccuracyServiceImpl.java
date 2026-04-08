package com.iflytek.vie.app.provider.impl.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.datadrill.FilterAnaylseService;
import com.iflytek.vie.app.api.model.ModelAccuracyService;
import com.iflytek.vie.app.api.model.ModelService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.datadrill.FilterRequest;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.model.DataMarkInfo;
import com.iflytek.vie.app.pojo.model.DataRemarkInfo;
import com.iflytek.vie.app.pojo.model.ModelFragment;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.app.pojo.model.ModelRequest;
import com.iflytek.vie.app.pojo.model.ModelResponse;
import com.iflytek.vie.app.pojo.topic.TopicInfo;
import com.iflytek.vie.app.pojo.topicgroup.ColumnMap;
import com.iflytek.vie.app.provider.common.ColumnInfo;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.DataMarkInfoDB;
import com.iflytek.vie.app.provider.database.DataRemarkInfoDB;
import com.iflytek.vie.app.provider.database.DimensionDB;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.TopicDB;
import com.iflytek.vie.app.provider.ruleparse.DataInfo;
import com.iflytek.vie.app.provider.ruleparse.Filter;
import com.iflytek.vie.app.provider.ruleparse.FilterParser;
import com.iflytek.vie.app.provider.ruleparse.MarkDataProcess;
import com.iflytek.vie.app.provider.ruleparse.MarkKeyWordProcess;
import com.iflytek.vie.app.provider.ruleparse.ModelParser;
import com.iflytek.vie.app.provider.ruleparse.ModelPreviewMethods;
import com.iflytek.vie.app.provider.ruleparse.PagerHelper;
import com.iflytek.vie.app.provider.ruleparse.ParserResult;
import com.iflytek.vie.app.provider.ruleparse.ResultTable;
import com.iflytek.vie.app.provider.ruleparse.RuleCheckPlatform;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.parse.TranslatePlatform;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExceptionUtil;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.ListUtil;
import com.iflytek.vie.utils.StringUtils;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelAccuracyServiceImpl implements ModelAccuracyService {
   private final Logger logger = LoggerFactory.getLogger(ModelAccuracyServiceImpl.class);
   private ObjectMapper mapper = new ObjectMapper();
   private ModelResponse response = new ModelResponse();
   private DataMarkInfoDB dataMarkInfoDB;
   private long fragmentId;
   private ModelDB modelDB;
   private DataRemarkInfoDB dataRemarkInfoDB;
   private ExcuteContext excuteContext;
   private Map<String, DataMarkInfo> idDataInfoMap = new HashMap<>();
   private CommonData commonData;
   private DataSourceInfo dataSourceInfo;
   private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
   private ModelService modelService;
   private FilterAnaylseService filterAnaylseService;
   private TopicDB topicDB;
   private DecimalFormat df = new DecimalFormat("0.00");
   private DimensionDB dimensionDB;

   public ModelResponse markPreviewService(ModelRequest request) throws VieAppServiceException {
      try {
         this.response = new ModelResponse();
         if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.response.setIsSuccess(false);
            this.response.setMessage("用户信息为空");
            return this.response;
         } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.response.setIsSuccess(false);
            this.response.setMessage("数据源为空");
            return this.response;
         } else {
            String checkResult = this.checkDimension(request.getModelDimension());
            if (!"true".equals(checkResult)) {
               this.response.setIsSuccess(false);
               this.response.setMessage(checkResult);
               return this.response;
            } else if (request.getSearchType() != 0 && request.getSearchType() != 1) {
               this.response.setIsSuccess(false);
               this.response.setMessage("搜索方式不在有效值内");
               return this.response;
            } else if (request.getChannel() != 0 && request.getChannel() != 1 && request.getChannel() != 2) {
               this.response.setIsSuccess(false);
               this.response.setMessage("声道不在有效值内");
               return this.response;
            } else if (request.getModelId() == null) {
               this.response.setIsSuccess(false);
               this.response.setMessage("模型ID为空");
               return this.response;
            } else {
               boolean isOrderSearchType = false;
               String modelFragmentRelation = request.getModelFragmentRelation();
               if (null != modelFragmentRelation && modelFragmentRelation.contains("@")) {
                  isOrderSearchType = true;
               }

               String tableName = request.getDataSource();
               ColumnInfo columnInfo = this.getColumnMaps(request.getColumns());
               Map<String, Object> retMap = new HashMap<>();
               String[] anyDrillHosts = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress").split(";");
               List<DimensionConfig> dimensionConfigs = this.dimensionDB.searchSystemDim(request.getDataSource());
               FilterParser filterParser = this.getFilterParser(request.getModelDimension(), dimensionConfigs);
               boolean isOnlySilenceRule = this.checkSilenceFragments(request);
               boolean isFragment = false;
               ModelParser modelParser = null;
               int channel = 2;
               List<ModelFragment> modelFragments = this.getModelFragmentList(request);
               if (!isOnlySilenceRule) {
                  List<ModelFragment> modelFragmentList = this.getModelFragmentList(request);
                  if (request.getModelFragments() == null || "".equals(request.getModelFragments())) {
                     isFragment = true;
                  }

                  modelFragmentList.add(request.getModelFragment());
                  Set<Integer> channels = new HashSet<>();
                  String result = this.checkFragments(modelFragmentList, channels, request.getChoose());
                  if (!"true".equals(result)) {
                     this.response.setIsSuccess(false);
                     this.response.setMessage("片段校验不通过 : " + result);
                     return this.response;
                  }

                  if (channels.size() == 1) {
                     channel = channels.iterator().next();
                  }

                  modelParser = this.getModelParser(modelFragmentList, isFragment, request);
                  ParserResult parserResult = modelParser.parse();
                  if (parserResult.getRet() >= 0) {
                     this.response.setIsSuccess(false);
                     this.response.setMessage("输入的规则语法错误，错误信息:" + parserResult.getMsg());
                     return this.response;
                  }

                  if (!isFragment) {
                     parserResult = modelParser.parseReplace();
                     if (parserResult.getRet() >= 0) {
                        this.response.setIsSuccess(false);
                        this.response.setMessage("输入的规则语法错误，错误信息:" + parserResult.getMsg());
                        return this.response;
                     }

                     if (isOrderSearchType) {
                        RuleCheckPlatform.checkNoOperator(modelParser.getInputRules());
                     }
                  }
               } else {
                  modelParser = this.getSilenceModelParser(request);
               }

               List<ModelFragment> mfList = new ArrayList<>();

               for (ModelFragment mf : modelFragments) {
                  if (mf.getRuleType() == 2) {
                     this.fragmentId = mf.getFragmentId();
                  } else {
                     mfList.add(mf);
                  }
               }

               Map<String, List<String>> dataInfoMarksMap = this.getMarkIds();
               retMap.put("columns", columnInfo.getColumnMaps());
               if (dataInfoMarksMap.get("ids") != null && dataInfoMarksMap.get("ids").size() != 0) {
                  boolean isSilenceSearchType = false;
                  TranslatePlatform translate = new TranslatePlatform();
                  String filter = this.dealJyTagContent(request.getSilenceRule(), isFragment);
                  if (!StringUtils.isNullOrEmpry(filter)) {
                     if (!filter.contains("</filters>")) {
                        this.response.setIsSuccess(false);
                        this.response.setMessage(filter);
                        return this.response;
                     }

                     translate.translate(filter);
                     isSilenceSearchType = true;
                  }

                  String channelRule = "";
                  if (isOrderSearchType) {
                     channelRule = this.getChannelRuleParser(mfList);
                  }

                  String textFilter = StringUtils.isNullOrEmpry(translate.getTextFilterCondition()) ? "" : translate.getTextFilterCondition();
                  ModelPreviewMethods mpm = new ModelPreviewMethods();
                  String paseRule = "";
                  if (mfList.size() == 0) {
                     paseRule = "";
                  } else {
                     paseRule = mpm.getPaseRule(false, mfList, null, modelParser.getInputRules(), 2, request.getSearchType());
                  }

                  String dataLimit = this.createDataLimit(request);
                  if ("error".equals(dataLimit)) {
                     return this.response;
                  } else {
                     dataLimit = dataLimit + " and ifLone != 1 ";
                     String objectRule = translate.getObjecntRule();
                     String textColumn = translate.getTextColumn();
                     String quaCondition = translate.getQuaCondition();
                     MarkDataProcess markDataProcess = new MarkDataProcess(
                        this.getRule(modelParser, isFragment, request),
                        channel,
                        filterParser.getFilters(),
                        tableName,
                        1000000,
                        "timestamp",
                        "desc",
                        columnInfo.getColumnsStr(),
                        dataInfoMarksMap.get("yIds"),
                        dataInfoMarksMap.get("nIds"),
                        dataInfoMarksMap.get("ids"),
                        anyDrillHosts,
                        request.getSearchType(),
                        paseRule,
                        objectRule,
                        textColumn,
                        quaCondition,
                        dimensionConfigs,
                        dataLimit,
                        textFilter,
                        channelRule
                     );
                     if (!isSilenceSearchType && !isOrderSearchType) {
                        markDataProcess.process(request.getDataSource());
                     } else {
                        markDataProcess.processSilence(request.getDataSource());
                     }

                     String rule = "";
                     if (request.getModelId() != -1L && !isFragment) {
                        ModelInfo modelInfo = this.modelDB.queryModelById(request.getModelId());
                        modelInfo.setModelAccuracy(markDataProcess.getPercent());
                        this.modelDB.saveModel(modelInfo);
                     }

                     if (isFragment) {
                        rule = modelParser.getInputRules();
                     } else {
                        rule = modelParser.getCombineRule();
                     }

                     MarkKeyWordProcess markKeyWordProcess = new MarkKeyWordProcess(
                        rule,
                        channel,
                        filterParser.getFilters(),
                        tableName,
                        markDataProcess.getDataInfos(),
                        this.fragmentId,
                        anyDrillHosts,
                        markDataProcess.getSilenceKeywordMap(),
                        channelRule
                     );
                     markKeyWordProcess.process(request.getDataSource());
                     ResultTable resultTable = new ResultTable();
                     int hitSize = dataInfoMarksMap.get("ids").size();
                     List<DataInfo> subDataInfos = PagerHelper.subList(markDataProcess.getDataInfos(), request.getPageNum(), request.getPageSize());

                     for (DataInfo dataInfo : subDataInfos) {
                        if (this.idDataInfoMap.containsKey(dataInfo.getId())) {
                           dataInfo.setDataMarkId(this.idDataInfoMap.get(dataInfo.getId()).getId());
                        }
                     }

                     double percent = markDataProcess.getPercent();
                     percent = Double.parseDouble(this.df.format(percent));
                     this.fillPreviewRemarkData(subDataInfos);
                     PagerHelper.setPagerInfo(
                        resultTable,
                        hitSize,
                        PagerHelper.caculateTotalNum(request.getPageSize(), hitSize),
                        subDataInfos,
                        request.getPageNum(),
                        request.getPageSize()
                     );
                     this.fillCountInfo(
                        retMap, percent, markDataProcess.getyExist(), markDataProcess.getnExist(), markDataProcess.getyNoExist(), markDataProcess.getnNoExist()
                     );
                     retMap.put("previewList", resultTable);
                     this.response.setIsSuccess(true);
                     this.response.setValue(retMap);
                     return this.response;
                  }
               } else {
                  this.fillCountInfo(retMap, 0.0, 0, 0, 0, 0);
                  ResultTable resultTable = new ResultTable();
                  resultTable.setTotalPages(0);
                  resultTable.setTotalRows(0);
                  resultTable.setPageNum(0);
                  resultTable.setPageSize(request.getPageSize());
                  resultTable.setRows(new ArrayList());
                  retMap.put("previewList", resultTable);
                  this.response.setIsSuccess(true);
                  this.response.setValue(retMap);
                  return this.response;
               }
            }
         }
      } catch (Exception var37) {
         this.logger.error("标记预览错误", var37);
         throw new VieAppServiceException("接口内部错误", var37);
      }
   }

   private String getChannelRuleParser(List<ModelFragment> modelFragmentList) {
      String channelRule = "";

      for (ModelFragment mf : modelFragmentList) {
         channelRule = channelRule + mf.getChannel() + ";";
      }

      if (!"".equals(channelRule)) {
         channelRule = channelRule.substring(0, channelRule.length() - 1);
      }

      return channelRule;
   }

   private String createDataLimit(ModelRequest request) {
      String dataLimit = "";

      try {
         dataLimit = this.commonData.getDataSql(request.getUserId(), "VIEWEB", request.getDataSource());
         Map<String, Object> filterMap = null;
         if (request.getFilterRuleId() != null && request.getFilterRuleId() != 0) {
            filterMap = this.filterAnaylseService.fetchSqlByFilterId(request.getFilterRuleId());
         } else if (StringUtils.isNotNullAndEmpry(request.getFilter()) && !"{}".equals(request.getFilter())) {
            FilterRequest filterRequest = (FilterRequest)JSON.parseObject(request.getFilter(), new TypeReference<FilterRequest>() {}, new Feature[0]);
            filterRequest.setDataSource(request.getDataSource());
            filterMap = this.filterAnaylseService.fetchSqlByFilterDimension(filterRequest);
         }

         String filterSql = "";
         if (filterMap != null && (Boolean)filterMap.get("success")) {
            filterSql = (String)filterMap.get("sql");
         } else if (filterMap != null && !(Boolean)filterMap.get("success")) {
            this.response.setIsSuccess(false);
            this.response.setMessage((String)filterMap.get("message"));
            this.logger.info("failed preview model");
            return "error";
         }

         dataLimit = dataLimit + (StringUtils.isNotNullAndEmpry(filterSql) ? " and " + filterSql + " " : "");
      } catch (ViePlatformServiceException var5) {
         this.logger.error("failed get dataLimit sql,msg:[" + ExceptionUtil.getStackTrace(var5) + "]");
      }

      return dataLimit;
   }

   private boolean checkSilenceFragments(ModelRequest request) {
      return StringUtils.isNullOrEmpry(request.getModelFragments()) && request.getModelFragment().getRuleType() == 1
         ? false
         : request.getModelFragments() == null
            || "".equals(request.getModelFragments())
            || "[]".equals(request.getModelFragments())
            || StringUtils.isNullOrEmpry(request.getModelFragment().getFragmentContent());
   }

   private ModelParser getSilenceModelParser(ModelRequest request) {
      ModelParser modelParser = new ModelParser(null, request.getModelFragment().getFragmentContent(), null);
      modelParser.setReplace(false);
      this.fragmentId = request.getModelFragment().getFragmentId();
      return modelParser;
   }

   public ModelResponse getMarkCount(ModelRequest request) throws VieAppServiceException {
      try {
         this.response = new ModelResponse();
         int totalNum = 0;
         if (request == null) {
            this.response.setIsSuccess(false);
            this.response.setMessage("参数为空");
            return this.response;
         } else {
            List<DataMarkInfo> dataMarkInfoList = this.dataMarkInfoDB.queryAllDataInfos(request.getFragmentId());
            if (dataMarkInfoList != null) {
               totalNum = dataMarkInfoList.size();
            }

            this.response.setIsSuccess(true);
            this.response.setMessage("查询成功");
            this.response.setValue(totalNum);
            return this.response;
         }
      } catch (Exception var4) {
         this.logger.error("接口内部错误", var4);
         throw new VieAppServiceException("接口内部错误", var4);
      }
   }

   public ModelResponse getTelephoneMarkState(ModelRequest request) throws VieAppServiceException {
      try {
         this.response = new ModelResponse();
         int markState = 0;
         if (request == null) {
            this.response.setIsSuccess(false);
            this.response.setMessage("参数为空");
            return this.response;
         } else if (request.getFragmentId() == null) {
            this.response.setIsSuccess(false);
            this.response.setMessage("fragment id is null");
            return this.response;
         } else if (StringUtils.isNullOrEmpry(request.getTelephonId())) {
            this.response.setIsSuccess(false);
            this.response.setMessage("telephone id is null");
            return this.response;
         } else {
            DataMarkInfo markInfo = this.dataMarkInfoDB.queryDataMarkByTelId(request.getFragmentId(), request.getTelephonId());
            if (markInfo != null) {
               markState = markInfo.getMark();
            }

            this.response.setIsSuccess(true);
            this.response.setMessage("查询成功");
            this.response.setValue(markState);
            return this.response;
         }
      } catch (Exception var4) {
         this.logger.error("内部异常", var4);
         throw new VieAppServiceException("内部异常", var4);
      }
   }

   public ModelResponse getOnLineModelsService(ModelRequest request) throws VieAppServiceException {
      try {
         this.response = new ModelResponse();
         if (request.getModelGroupId() == null) {
            this.response.setIsSuccess(false);
            this.response.setMessage("模型组ID为空");
            return this.response;
         } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.response.setIsSuccess(false);
            this.response.setMessage("数据源为空");
            return this.response;
         } else {
            Map<String, Object> modelMap = null;

            try {
               ModelRequest modelRequest = new ModelRequest();
               modelRequest.setModelGroupId(request.getModelGroupId());
               modelRequest.setUserId(request.getUserId());
               modelRequest.setDataSource(request.getDataSource());
               modelRequest.setModelName(request.getModelName());
               modelRequest.setType("online");
               modelRequest.setSystemId("VIEWEB");
               modelRequest.setBatchId(request.getBatchId());
               modelRequest.setIfLone(request.getIfLone());
               modelRequest.setDataType(request.getDataType());
               modelMap = this.modelService.searModelByGroupService(modelRequest);
            } catch (Exception var20) {
               this.logger.error("获取已上线模型基础服务错误", var20);
               throw new VieAppServiceException("获取上线模型错误", var20);
            }

            List<HashMap<String, Object>> modelListMap = new ArrayList<>();
            if (modelMap.isEmpty()) {
               this.response.setIsSuccess(true);
               this.response.setMessage("查询成功");
               this.response.setValue(modelListMap);
               return this.response;
            } else {
               modelListMap = (List<HashMap<String, Object>>)modelMap.get("rows");
               if (modelListMap == null) {
                  this.response.setIsSuccess(true);
                  this.response.setMessage("查询成功");
                  this.response.setValue(new ArrayList());
                  return this.response;
               } else {
                  String modelIds = "";

                  for (int i = 0; i < modelListMap.size(); i++) {
                     HashMap<String, Object> hm = modelListMap.get(i);
                     if (i == modelListMap.size() - 1) {
                        modelIds = modelIds + hm.get("modelId");
                     } else {
                        modelIds = modelIds + hm.get("modelId") + ",";
                     }
                  }

                  if (StringUtils.isNullOrEmpry(modelIds)) {
                     this.response.setIsSuccess(true);
                     this.response.setMessage("查询成功");
                     this.response.setValue(modelListMap);
                     return this.response;
                  } else {
                     for (HashMap<String, Object> hmModel : (List<HashMap<String, Object>>)modelListMap) {
                        hmModel.put("count", 0);
                     }

                     TopicInfo topicInfo = this.topicDB.fetchTopicInfoById(String.valueOf(request.getTopicId()));
                     if (topicInfo == null) {
                        this.response.setIsSuccess(false);
                        this.response.setMessage("专题为空");
                        return this.response;
                     } else {
                        String topicCondition = topicInfo.getTopicCondition();
                        String dataSql = this.commonData.getDataSql(request.getUserId(), request.getSystemId(), request.getDataSource());
                        String tableName = IndexConstants.getModelTableName(request.getDataSource());
                        String con = this.getTopicCondition(topicCondition);
                        String sql = "select offLineTagId as modelId,sum(idCounter) as result from "
                           + tableName
                           + " where offLineTagId in ("
                           + modelIds
                           + ") and "
                           + con
                           + " and "
                           + dataSql
                           + " and "
                           + MultipleDataConstant.getMultipleDataSql(request.getBatchId(), request.getIfLone(), request.getDataType())
                           + " group by offLineTagId";
                        this.logger.info("the model sql is: {}", sql);
                        String[] anydrillHost = DynamicEsSource.getEsSourceByType(request.getDataSource(), "modelAnydrillAddress").split(";");
                        PreparedStatement pst = new PreparedStatement(anydrillHost);
                        ResultSet rs = pst.executeQuery(sql);
                        List<LinkedHashMap<String, Object>> dataMaps = rs.toList();
                        DecimalFormat df = new DecimalFormat("0");

                        for (LinkedHashMap<String, Object> map : dataMaps) {
                           for (HashMap<String, Object> hm : (List<HashMap<String, Object>>)modelListMap) {
                              if (map.get("modelId").toString().equals(hm.get("modelId").toString())) {
                                 hm.put("count", Integer.parseInt(df.format(map.get("result"))));
                              }
                           }
                        }

                        this.response.setIsSuccess(true);
                        this.response.setMessage("查询成功");
                        this.response.setValue(modelListMap);
                        return this.response;
                     }
                  }
               }
            }
         }
      } catch (Exception var21) {
         this.logger.error("获取上线模型错误", var21);
         throw new VieAppServiceException("获取上线模型错误", var21);
      }
   }

   private String getTopicCondition(String topicCondition) throws Exception {
      String result = "";
      List<HashMap<String, Object>> conList = (List<HashMap<String, Object>>)this.mapper.readValue(topicCondition, List.class);
      HashMap<String, Object> hm = conList.get(0);
      Integer type = (Integer)hm.get("timeType");
      Integer value = (Integer)hm.get("timeValue");
      if (type != null && value != null && type == 2) {
         Date date = new Date();
         Calendar past = Calendar.getInstance();
         past.setTime(date);
         past.add(6, value + 1);
         past.set(11, 0);
         past.set(12, 0);
         past.set(13, 0);
         past.set(14, 0);
         Calendar now = Calendar.getInstance();
         now.setTime(date);
         now.add(6, 1);
         now.set(11, 0);
         now.set(12, 0);
         now.set(13, 0);
         now.set(14, 0);
         result = "timestamp > " + past.getTimeInMillis() + " and timestamp < " + now.getTimeInMillis();
      } else {
         FilterParser filterParser = new FilterParser(topicCondition);
         filterParser.parserFilters();
         List<Filter> filters = filterParser.getFilters();
         int count = 0;
         StringBuffer conditionBuffer = new StringBuffer();

         for (int i = 0; i < filters.size(); i++) {
            Filter filter = filters.get(i);
            String condition = filter.toSqlCondition();
            if (condition != null && !"".equals(condition)) {
               if (count++ != 0) {
                  conditionBuffer.append(" and ");
               }

               conditionBuffer.append("(" + condition + ")");
            }
         }

         result = conditionBuffer.toString();
      }

      return result;
   }

   private String checkFragments(List<ModelFragment> fragments, Set<Integer> channels, String choose) {
      List<Long> ids = new ArrayList<>();
      List<Integer> nums = new ArrayList<>();
      String regEx = "[`~@$%^*={}':;',\\[\\].<>/?~！@￥……*（）——{}【】‘；：”“'。，、？]";
      Pattern p = Pattern.compile(regEx);
      String message = "";

      for (ModelFragment mf : fragments) {
         if (StringUtils.isNullOrEmpry(mf.getFragmentContent()) || "null".equals(mf.getFragmentContent())) {
            return "片段内容不能为空";
         }

         Matcher m = p.matcher(mf.getFragmentContent());
         if (m.find() && !"true".equals(choose)) {
            return "片段不能存在特殊符号";
         }

         if (mf.getRuleType() != 1 && mf.getRuleType() != 2) {
            return "片段类型错误";
         }

         if (mf.getRuleType() != 2 && mf.getFragmentNum() <= 0) {
            return "片段序号错误";
         }

         if (mf.getChannel() != 0 && mf.getChannel() != 1 && mf.getChannel() != 2) {
            return "片段声道不在合理范围内";
         }

         channels.add(mf.getChannel());
         ids.add(mf.getFragmentId());
         nums.add(mf.getFragmentNum());
      }

      return "true";
   }

   private String checkDimension(String dimensionCondition) {
      String checkResult = "";

      try {
         for (HashMap<String, Object> hm : (List<HashMap<String, Object>>)this.mapper.readValue(dimensionCondition, List.class)) {
            String type = String.valueOf(hm.get("type"));
            if (!"mulEqu".equals(type)) {
               if ("range".equals(type)) {
                  for (String rangeStr : (List<String>)hm.get("value")) {
                     String[] rangeStrArr = rangeStr.split("~");
                     if (rangeStrArr.length != 2) {
                        return "区间校验不通过";
                     }

                     try {
                        int start = Integer.parseInt(rangeStrArr[0]);
                        int end = Integer.parseInt(rangeStrArr[1]);
                        if (start > end) {
                           return "区间开始值不能大于结束值";
                        }
                     } catch (Exception var14) {
                        return "区间只能输入数字";
                     }
                  }
               } else if ("timeRange".equals(type)) {
                  List<String> timeList = (List<String>)hm.get("value");
                  if (ListUtil.isNullOrEmpty(timeList) || timeList.size() < 2) {
                     return "时间范围不对";
                  }

                  String start = timeList.get(0);
                  String end = timeList.get(1);

                  Date startTime;
                  Date endTime;
                  try {
                     startTime = this.sdf.parse(start);
                     endTime = this.sdf.parse(end);
                  } catch (Exception var13) {
                     return "时间格式不对";
                  }

                  if (startTime.getTime() > endTime.getTime()) {
                     return "开始时间大于结束时间";
                  }
               } else if (!"mulSel".equals(type)) {
                  return "条件校验不通过，条件类型不在合理的范围内";
               }
            }
         }

         checkResult = "true";
      } catch (Exception var15) {
         this.logger.error("校验出错", var15);
      }

      return checkResult;
   }

   private void fillPreviewRemarkData(List<DataInfo> dataInfos) {
      if (dataInfos != null && dataInfos.size() != 0) {
         List<DataRemarkInfo> dataRemarkInfos = this.dataRemarkInfoDB.queryAllDataRemarks(this.fragmentId);
         Map<String, DataRemarkInfo> map = new HashMap<>();
         if (dataRemarkInfos != null && dataRemarkInfos.size() != 0) {
            for (DataRemarkInfo dataRemarkInfo : dataRemarkInfos) {
               map.put(dataRemarkInfo.getTelId(), dataRemarkInfo);
            }
         }

         for (DataInfo dataInfo : dataInfos) {
            dataInfo.setFragmentId(this.fragmentId);
            if (map.containsKey(dataInfo.getId())) {
               dataInfo.setDataRemarkId(map.get(dataInfo.getId()).getId());
               dataInfo.setRemarked(1);
               dataInfo.setRemark(map.get(dataInfo.getId()).getRemark());
            }
         }
      }
   }

   private String dealJyTagContent(String tagContentRule, boolean isFragment) {
      if (!isFragment && !StringUtils.isNullOrEmpry(tagContentRule)) {
         StringBuffer filter = new StringBuffer();

         try {
            List<HashMap<String, Object>> list = (List<HashMap<String, Object>>)this.mapper.readValue(tagContentRule, List.class);
            if (list != null && list.size() > 0) {
               List<String> idToTagPropertyType = new ArrayList<>();
               List<String> idToRelativeList = new ArrayList<>();
               filter.append("<filters name=\" \">");

               for (HashMap<String, Object> dataMap : (List<HashMap<String, Object>>)list) {
                  String filterType = dataMap.get("dimensionCode").toString();
                  String filterId = dataMap.get("id").toString();
                  filter.append(String.format("<filter type=\"%s\" id=\"%s\">", filterType, filterId));

                  for (HashMap<String, String> value : (List<HashMap<String, String>>)dataMap.get("value")) {
                     String propCode = value.get("propertyCode").toString();
                     if (TagPropType.AvgSpeed == TagPropType.getType(propCode)) {
                        idToTagPropertyType.add(filterId);
                     }

                     String operationCode = value.get("operationCode").toString();
                     String val = value.get("value").toString();
                     String relative = value.get("relativeobject") == null ? "" : value.get("relativeobject").toString();
                     if (StringUtils.isNullOrEmpry(relative)) {
                        filter.append("<rule type=\"" + propCode + "\" operator=\"" + operationCode + "\" value=\"" + val + "\"></rule>");
                     } else {
                        filter.append(
                           "<rule type=\""
                              + propCode
                              + "\" operator=\""
                              + operationCode
                              + "\" value=\""
                              + val
                              + "\" relativeobject=\""
                              + relative
                              + "\"></rule>"
                        );
                        idToRelativeList.add(relative);
                     }
                  }

                  filter.append("</filter>");
               }

               filter.append("</filters>");
               if (idToRelativeList.size() > 0 && idToTagPropertyType.size() > 0) {
                  for (int i = 0; i < idToRelativeList.size(); i++) {
                     if (idToTagPropertyType.contains(idToRelativeList.get(i))) {
                        return "平均语速不可选为其他属性规则相对位置规则的对象";
                     }
                  }
               }
            }
         } catch (IOException var18) {
            this.logger.error("处理静音标签错误", var18);
            return "处理静音标签错误";
         }

         return filter.toString();
      } else {
         return "";
      }
   }

   private String getRule(ModelParser modelParser, boolean isFragment, ModelRequest request) {
      return isFragment ? request.getModelFragment().getFragmentContent() : modelParser.getCombineRule();
   }

   private void fillCountInfo(Map<String, Object> retMap, double accuracy, int yExist, int nExist, int yNoExist, int nNoExist) {
      retMap.put("accuracy", accuracy);
      retMap.put("yExist", yExist);
      retMap.put("nExist", nExist);
      retMap.put("yNoExist", yNoExist);
      retMap.put("nNoExist", nNoExist);
   }

   private Map<String, List<String>> getMarkIds() {
      List<DataMarkInfo> markInfos = this.dataMarkInfoDB.queryAllDataInfos(this.fragmentId);
      List<String> ids = new ArrayList<>();
      List<String> yIds = new ArrayList<>();
      List<String> nIds = new ArrayList<>();
      Map<String, List<String>> markIdsMap = new HashMap<>();
      markIdsMap.put("ids", ids);
      markIdsMap.put("yIds", yIds);
      markIdsMap.put("nIds", nIds);
      if (markInfos != null && markInfos.size() != 0) {
         for (DataMarkInfo dataMarkInfo : markInfos) {
            ids.add(dataMarkInfo.getTelId());
            this.idDataInfoMap.put(dataMarkInfo.getTelId(), dataMarkInfo);
            if (dataMarkInfo.getMark() == -1) {
               nIds.add(dataMarkInfo.getTelId());
            } else if (dataMarkInfo.getMark() == 1) {
               yIds.add(dataMarkInfo.getTelId());
            }
         }

         return markIdsMap;
      } else {
         return markIdsMap;
      }
   }

   private ColumnInfo getColumnMaps(String columns) {
      ColumnInfo columnInfo = new ColumnInfo();
      columnInfo.getColumns().add("id");
      columnInfo.getColumns().add("duration");
      if (columns != null && !"".equals(columns)) {
         List list = (List)JSON.parseObject(columns, List.class);
         if (list != null && list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
               JSONObject jsonObject = (JSONObject)list.get(i);
               ColumnMap columnMap = new ColumnMap(jsonObject.getString("columnName"), jsonObject.getString("column"));
               columnInfo.getColumnMaps().add(columnMap);
               columnInfo.getColumns().add(columnMap.getColumn());
            }
         }
      }

      columnInfo.getColumnMaps().add(new ColumnMap("匹配规则", "keyword"));
      columnInfo.getColumnMaps().add(new ColumnMap("人工测听标记", "mark"));
      columnInfo.getColumnMaps().add(new ColumnMap("备注", "remark"));
      columnInfo.setColumnsStr(columnInfo.getColumns().toString());
      return columnInfo;
   }

   private FilterParser getFilterParser(String modelDimensions, List<DimensionConfig> dimensionConfigs) throws ParseException {
      modelDimensions = this.filterTime(modelDimensions, dimensionConfigs);
      FilterParser filterParser = new FilterParser(modelDimensions);
      filterParser.parserFilters();
      return filterParser;
   }

   private String filterTime(String modelDimensions, List<DimensionConfig> dimensionConfigs) {
      ObjectMapper mapper = new ObjectMapper();
      List<String> timeDimension = new ArrayList<>();
      if (dimensionConfigs != null && dimensionConfigs.size() > 0) {
         for (int i = 0; i < dimensionConfigs.size(); i++) {
            if (dimensionConfigs.get(i).getFlag() == 1) {
               timeDimension.add(dimensionConfigs.get(i).getIndexField());
            }
         }
      }

      try {
         List<HashMap<String, Object>> dimensionList = (List<HashMap<String, Object>>)mapper.readValue(modelDimensions, List.class);
         List<HashMap<String, Object>> dl = new ArrayList<>();

         for (int ix = 0; ix < dimensionList.size(); ix++) {
            HashMap<String, Object> hs = dimensionList.get(ix);
            String key = hs.get("key").toString();
            if (!timeDimension.contains(key)) {
               dl.add(ix, hs);
            } else {
               List<String> valueList = (List<String>)hs.get("value");
               List<String> resultList = new ArrayList<>();

               for (int j = 0; j < valueList.size(); j++) {
                  String[] arr = valueList.get(j).split("~");
                  if (arr.length > 1) {
                     String st = Integer.parseInt(arr[0]) * 1000 - 501 + "";
                     String le = Integer.parseInt(arr[1]) * 1000 + 499 + "";
                     resultList.add(st + "~" + le);
                  }
               }

               hs.put("value", resultList);
               dl.add(ix, hs);
            }
         }

         return mapper.writeValueAsString(dl);
      } catch (Exception var16) {
         this.logger.error("更改时间错误", var16);
         return "";
      }
   }

   private List<ModelFragment> getModelFragmentList(ModelRequest request) {
      List<ModelFragment> modelFragmentList = new ArrayList<>();
      JSONArray jsonArray = JSONArray.parseArray(request.getModelFragments());
      if (jsonArray != null && jsonArray.size() != 0) {
         for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject)jsonArray.get(i);
            ModelFragment modelFragment = new ModelFragment();
            modelFragment.setModelId(request.getModelId());
            modelFragment.setIsTag(Integer.parseInt(String.valueOf(jsonObject.get("isTag"))));
            modelFragment.setRuleType(Integer.parseInt(String.valueOf(jsonObject.get("ruleType"))));
            modelFragment.setFragmentNum(Integer.parseInt(String.valueOf(jsonObject.get("fragmentNum"))));
            modelFragment.setFragmentId(Long.parseLong(String.valueOf(jsonObject.get("fragmentId"))));
            modelFragment.setFragmentContent(String.valueOf(jsonObject.get("fragmentContent")));
            modelFragment.setChannel(Integer.parseInt(String.valueOf(jsonObject.get("channel"))));
            modelFragment.setTagContent(String.valueOf(jsonObject.get("tagContent")));
            modelFragmentList.add(modelFragment);
         }

         return modelFragmentList;
      } else {
         return modelFragmentList;
      }
   }

   private ModelParser getModelParser(List<ModelFragment> modelFragmentList, boolean isFragment, ModelRequest request) {
      ModelParser modelParser;
      if (!isFragment) {
         ModelFragment mf = request.getModelFragment();
         this.fragmentId = mf.getFragmentId();
         modelParser = new ModelParser(null, mf.getFragmentContent(), this.getModelFragmentHm(modelFragmentList));
      } else {
         modelParser = new ModelParser(null, request.getModelFragment().getFragmentContent(), null);
         modelParser.setReplace(false);
         this.fragmentId = request.getModelFragment().getFragmentId();
      }

      return modelParser;
   }

   private HashMap<Integer, String> getModelFragmentHm(List<ModelFragment> modelFragmentList) {
      HashMap<Integer, String> hm = new HashMap<>();

      for (int i = 0; i < modelFragmentList.size(); i++) {
         int fn = modelFragmentList.get(i).getFragmentNum();
         String tr = modelFragmentList.get(i).getFragmentContent();
         hm.put(fn, tr);
      }

      return hm;
   }

   public Map<String, DataMarkInfo> getIdDataInfoMap() {
      return this.idDataInfoMap;
   }

   public void setIdDataInfoMap(Map<String, DataMarkInfo> idDataInfoMap) {
      this.idDataInfoMap = idDataInfoMap;
   }

   public long getFragmentId() {
      return this.fragmentId;
   }

   public void setFragmentId(long fragmentId) {
      this.fragmentId = fragmentId;
   }

   public ModelDB getModelDB() {
      return this.modelDB;
   }

   public void setModelDB(ModelDB modelDB) {
      this.modelDB = modelDB;
   }

   public DataRemarkInfoDB getDataRemarkInfoDB() {
      return this.dataRemarkInfoDB;
   }

   public void setDataRemarkInfoDB(DataRemarkInfoDB dataRemarkInfoDB) {
      this.dataRemarkInfoDB = dataRemarkInfoDB;
   }

   public ExcuteContext getExcuteContext() {
      return this.excuteContext;
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
   }

   public DataMarkInfoDB getDataMarkInfoDB() {
      return this.dataMarkInfoDB;
   }

   public void setDataMarkInfoDB(DataMarkInfoDB dataMarkInfoDB) {
      this.dataMarkInfoDB = dataMarkInfoDB;
   }

   public CommonData getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonData commonData) {
      this.commonData = commonData;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public ModelService getModelService() {
      return this.modelService;
   }

   public void setModelService(ModelService modelService) {
      this.modelService = modelService;
   }

   public TopicDB getTopicDB() {
      return this.topicDB;
   }

   public void setTopicDB(TopicDB topicDB) {
      this.topicDB = topicDB;
   }

   public DimensionDB getDimensionDB() {
      return this.dimensionDB;
   }

   public void setDimensionDB(DimensionDB dimensionDB) {
      this.dimensionDB = dimensionDB;
   }

   public void setFilterAnaylseService(FilterAnaylseService filterAnaylseService) {
      this.filterAnaylseService = filterAnaylseService;
   }
}
