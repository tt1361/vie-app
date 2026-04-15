package com.iflytek.vie.app.provider.impl.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.datadrill.FilterAnaylseService;
import com.iflytek.vie.app.api.model.ModelService;
import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.api.permission.UserService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.ServiceResponse;
import com.iflytek.vie.app.pojo.datadrill.FilterRequest;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.filter.FilterInfo;
import com.iflytek.vie.app.pojo.model.DataMarkInfo;
import com.iflytek.vie.app.pojo.model.DataRemarkInfo;
import com.iflytek.vie.app.pojo.model.ModelDetailDTO;
import com.iflytek.vie.app.pojo.model.ModelFragment;
import com.iflytek.vie.app.pojo.model.ModelGroup;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.app.pojo.model.ModelRequest;
import com.iflytek.vie.app.pojo.model.ModelTagRequest;
import com.iflytek.vie.app.pojo.model.ModelTask;
import com.iflytek.vie.app.pojo.model.ResultTable;
import com.iflytek.vie.app.pojo.model.TagDimension;
import com.iflytek.vie.app.pojo.model.TagDimensionProperty;
import com.iflytek.vie.app.pojo.model.TagPropertyOperation;
import com.iflytek.vie.app.pojo.model.TaskInfo;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.provider.common.ColumnInfoPlatform;
import com.iflytek.vie.app.provider.common.ColumnMapPlatform;
import com.iflytek.vie.app.provider.common.CommonDataPlatform;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.common.SqlHelperPlatform;
import com.iflytek.vie.app.provider.database.BatchDB;
import com.iflytek.vie.app.provider.database.DataCollectTaskDB;
import com.iflytek.vie.app.provider.database.DataMarkInfoDB;
import com.iflytek.vie.app.provider.database.DataRemarkInfoDB;
import com.iflytek.vie.app.provider.database.DimensionDBPlatform;
import com.iflytek.vie.app.provider.database.FilterInfoDB;
import com.iflytek.vie.app.provider.database.ModelDBPlatform;
import com.iflytek.vie.app.provider.database.ModelFragmentDB;
import com.iflytek.vie.app.provider.database.ModelGroupDBPlatform;
import com.iflytek.vie.app.provider.database.ModelInfoDBPlatform;
import com.iflytek.vie.app.provider.database.ModelTagDB;
import com.iflytek.vie.app.provider.database.ModelTaskDB;
import com.iflytek.vie.app.provider.database.TaskInfoDB;
import com.iflytek.vie.app.provider.ruleparse.DataInfoPlatform;
import com.iflytek.vie.app.provider.ruleparse.FilterParserPlatform;
import com.iflytek.vie.app.provider.ruleparse.ModelParserPlatform;
import com.iflytek.vie.app.provider.ruleparse.ModelPreviewMethods;
import com.iflytek.vie.app.provider.ruleparse.ModelRunException;
import com.iflytek.vie.app.provider.ruleparse.ParserResultPlatform;
import com.iflytek.vie.app.provider.ruleparse.PreviewProcessPlatform;
import com.iflytek.vie.app.provider.ruleparse.RuleCheckPlatform;
import com.iflytek.vie.app.provider.ruleparse.SearchHitCountProcess;
import com.iflytek.vie.app.provider.ruleparse.SilenceTagsCache;
import com.iflytek.vie.app.provider.ruleparse.TaskInfoStatus;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.parse.TagRuleHandler;
import com.iflytek.vie.app.provider.ruleparse.parse.TranslatePlatform;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.MultipleDataConstant;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.ExceptionUtil;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.ListUtil;
import com.iflytek.vie.utils.StringUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Clob;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.ddl.model.TableInfoResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelServiceImpl implements ModelService {
   private ModelDBPlatform modelDB;
   private ModelFragmentDB fragmentDB;
   private ModelGroupDBPlatform modelGroupDB;
   private DataMarkInfoDB dataMarkInfoDB;
   private DataRemarkInfoDB dataRemarkInfoDB;
   private TaskInfoDB taskInfoDB;
   private ModelTaskDB modelTaskDB;
   private CommonDataPlatform commonData;
   public ModelTagDB modelTagDB;
   private DataCollectTaskDB collectTaskDB;
   private BatchDB batchInfoDB;
   private FilterAnaylseService filterAnaylseService;
   private FilterInfoDB filterInfoDB;
   private static final String MESSAGE = "message";
   private static final String SQL = "sql";
   private static final String SUCCESS = "success";
   private static final String IS_MODEL = "isModel";
   private String dataTypeDimension;
   private final Logger logger = LoggerFactory.getLogger(ModelServiceImpl.class);
   private ServiceResponse serviceResponse = new ServiceResponse();
   private ObjectMapper mapper = new ObjectMapper();
   private ExcuteContext excuteContext;
   private GroupAuthService groupAuthService;
   private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
   private SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   private UserService userService;
   private DataSourceInfo dataSourceInfo;
   private DecimalFormat df = new DecimalFormat("0.00");
   private DecimalFormat df1 = new DecimalFormat("0.0");
   private DimensionDBPlatform dimensionDB;
   private ModelInfoDBPlatform modelInfoDB;
   private static final long AVG_TIME = 5L;

   public ServiceResponse addModelService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.serviceResponse = new ServiceResponse();
         ModelInfo modelInfo = request.getModelInfo();
         this.logger.info("start add model");
         if (modelInfo.getChannel() != 0 && modelInfo.getChannel() != 1 && modelInfo.getChannel() != 2) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型声道错误");
            return this.serviceResponse;
         } else {
            float accuracy = modelInfo.getModelAccuracy();
            if (accuracy < 0.0F || accuracy > 100.0F) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("模型准确率不在合理范围内");
               return this.serviceResponse;
            } else if (request.getModelGroupId() == null) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("模型组为空");
               return this.serviceResponse;
            } else {
               ModelGroup group = this.modelGroupDB.queryModelGroupByID(request.getModelGroupId());
               if (group == null) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("模型组不存在");
                  return this.serviceResponse;
               } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("数据源不存在");
                  return this.serviceResponse;
               } else {
                  String tableName = request.getDataSource();
                  boolean isHave = this.isHaveSameModel(modelInfo.getModelName(), tableName, modelInfo.getModelId(), group.getGroupId(), 0);
                  if (isHave) {
                     this.logger.info("hava the same modelname");
                     this.serviceResponse.setSuccessful(false);
                     this.serviceResponse.setMessage("已存在相同的模型名称");
                     return this.serviceResponse;
                  } else if (StringUtils.isNullOrEmpry(request.getUserId())) {
                     this.logger.info("the userid is null or empty");
                     this.serviceResponse.setSuccessful(false);
                     this.serviceResponse.setMessage("用户不存在");
                     return this.serviceResponse;
                  } else {
                     ModelFragment mf = request.getModelFragment();
                     if (this.isModelFragment(request.getModelFragments()) && StringUtils.isNullOrEmpry(request.getSilenceRule())) {
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("模型关系不能为空");
                        return this.serviceResponse;
                     } else if (mf.getRuleType() != 2) {
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("片段组合关系不存在");
                        return this.serviceResponse;
                     } else {
                        this.logger.info("the condition is true");
                        String userName = "";

                        try {
                           Map<Integer, Map<String, String>> usersInfo = this.userService.queryAllUserInfo(request.getDataSource());
                           this.logger.info("all the users info is : " + JSON.toJSONString(usersInfo));
                           Integer userid = Integer.parseInt(request.getUserId());
                           Map<String, String> userInfo = usersInfo.get(userid);
                           if (userInfo != null) {
                              userName = userInfo.get("accountName");
                           }
                        } catch (Exception var24) {
                           this.logger.info("get userinfo from permission error");
                        }

                        this.logger.info("success get userinfo from permission");
                        if (!StringUtils.isNullOrEmpry(userName) && !"null".equals(userName)) {
                           String dataSql = this.commonData.getDataSql(request.getUserId(), "VIEWEB", request.getDataSource());
                           List<ModelFragment> modelFragmentList = new ArrayList<>();
                           this.logger.info("start deal jingyin");
                           String tagRule = this.dealJyTagContent(request.getSilenceRule(), false);
                           if (null != tagRule && !"".equals(tagRule) && !tagRule.contains("</filters>")) {
                              this.serviceResponse.setSuccessful(false);
                              this.serviceResponse.setMessage(tagRule);
                              return this.serviceResponse;
                           } else {
                              this.logger.info("deal jingyin done");
                              if (!this.isModelFragment(request.getModelFragments())) {
                                 List<HashMap<String, Object>> list = (List<HashMap<String, Object>>)this.mapper
                                    .readValue(request.getModelFragments(), List.class);
                                 boolean isHaveStructuredRule = StringUtils.isNullOrEmpry(mf.getFragmentContent()) && list.size() > 0;

                                 for (HashMap<String, Object> hm : (List<HashMap<String, Object>>)list) {
                                    ModelFragment mft = new ModelFragment();
                                    mft.setChannel(Integer.parseInt(String.valueOf(hm.get("channel"))));
                                    mft.setTagContent(this.mapper.writeValueAsString(hm.get("tagContent")));
                                    mft.setIsTag(Integer.parseInt(String.valueOf(hm.get("isTag"))));
                                    mft.setFragmentContent(String.valueOf(hm.get("fragmentContent")));
                                    mft.setFragmentId(Long.parseLong(String.valueOf(hm.get("fragmentId"))));
                                    mft.setFragmentNum(Integer.parseInt(String.valueOf(hm.get("fragmentNum"))));
                                    mft.setRuleType(Integer.parseInt(String.valueOf(hm.get("ruleType"))));
                                    mft.setRemark(String.valueOf(hm.get("remark")));
                                    mft.setTagText(String.valueOf(hm.get("tagText")));
                                    mft.setTagVersion(String.valueOf(hm.get("tagVersion")));
                                    mft.setModelId(modelInfo.getModelId());
                                    modelFragmentList.add(mft);
                                 }

                                 modelFragmentList.add(mf);
                                 if (!isHaveStructuredRule) {
                                    HashMap<Integer, String> hm = this.getModelFragmentHm(modelFragmentList);
                                    this.logger.info("start check model ruel");
                                    ModelParserPlatform mp = new ModelParserPlatform(null, mf.getFragmentContent(), hm);
                                    ParserResultPlatform ps = mp.parse();
                                    ParserResultPlatform parserResult = mp.parseReplace();
                                    if (parserResult.getRet() == 0) {
                                       this.logger.error("模型标签错误");
                                       this.serviceResponse.setSuccessful(false);
                                       this.serviceResponse.setMessage("模型标签错误");
                                       return this.serviceResponse;
                                    }

                                    this.logger.info("start check model fragment");
                                    String result = this.checkFragments(modelFragmentList, request.getChoose());
                                    if (!"true".equals(result)) {
                                       this.serviceResponse.setSuccessful(false);
                                       this.serviceResponse.setMessage(result);
                                       return this.serviceResponse;
                                    }

                                    this.logger.info("success deal model condition");
                                    if (ps.getRet() == -1) {
                                       this.logger.info("start get limit sql ");
                                       this.logger.info("get limit sql done");
                                       String rule = mp.getCombineRule();
                                       this.logger.info("get modelGroup done");
                                       Date date = new Date();
                                       modelInfo.setTextRule(rule);
                                       modelInfo.setModelGroup(group);
                                       modelInfo.setSilenceRule(tagRule);
                                       modelInfo.setSilenceText(request.getSilenceText());
                                       modelInfo.setDataSource(tableName);
                                       modelInfo.setCreateTime(this.sdf2.format(date));
                                       modelInfo.setUpdateTime(this.sdf2.format(date));
                                       modelInfo.setUserName(userName);
                                       modelInfo.setModifierName(userName);
                                       modelInfo.setModifierId(Long.parseLong(request.getUserId()));
                                       modelInfo.setInputRule(mf.getFragmentContent());
                                       modelInfo.setDataLimit(dataSql);
                                       modelInfo.setModelStatus(0);
                                       modelInfo.setModelComment(modelInfo.getModelComment());
                                       this.logger.info("add model");
                                       long modelId = this.modelDB.saveModelAndGetModelID(modelInfo);
                                       this.logger.info("add model done");
                                       List<ModelFragment> mfListNew = this.rebuildModelFragmentList(modelId, modelFragmentList, request.getPriviewId());
                                       this.logger.info("add modelFragments");
                                       this.fragmentDB.saveList(mfListNew);
                                       this.logger.info("success add model : " + modelId);
                                       this.serviceResponse.setSuccessful(true);
                                       this.serviceResponse.setValue(modelId);
                                    }
                                 } else {
                                    this.logger.info("start get limit sql ");
                                    String rule = "";
                                    this.logger.info("start deal jingyin");
                                    Date date = new Date();
                                    modelInfo.setTextRule(rule);
                                    modelInfo.setModelGroup(group);
                                    modelInfo.setSilenceRule(tagRule);
                                    modelInfo.setSilenceText(request.getSilenceText());
                                    modelInfo.setDataSource(tableName);
                                    modelInfo.setCreateTime(this.sdf2.format(date));
                                    modelInfo.setUpdateTime(this.sdf2.format(date));
                                    modelInfo.setUserName(userName);
                                    modelInfo.setModifierName(userName);
                                    modelInfo.setModifierId(Long.parseLong(request.getUserId()));
                                    modelInfo.setInputRule(mf.getFragmentContent());
                                    modelInfo.setDataLimit(dataSql);
                                    modelInfo.setModelStatus(0);
                                    modelInfo.setModelComment(modelInfo.getModelComment());
                                    this.logger.info("add model");
                                    long modelId = this.modelDB.saveModelAndGetModelID(modelInfo);
                                    this.logger.info("add model done");
                                    List<ModelFragment> mfListNew = this.rebuildModelFragmentList(modelId, modelFragmentList, request.getPriviewId());
                                    this.logger.info("add modelFragments");
                                    this.fragmentDB.saveList(mfListNew);
                                    this.logger.info("success add model : " + modelId);
                                    this.serviceResponse.setSuccessful(true);
                                    this.serviceResponse.setValue(modelId);
                                 }
                              } else {
                                 modelFragmentList.add(mf);
                                 String resultx = this.checkSilenceFragments(modelFragmentList);
                                 if (!"true".equals(resultx)) {
                                    this.serviceResponse.setSuccessful(false);
                                    this.serviceResponse.setMessage(resultx);
                                    return this.serviceResponse;
                                 }

                                 Date date = new Date();
                                 modelInfo.setTextRule("");
                                 modelInfo.setModelGroup(group);
                                 modelInfo.setSilenceRule(tagRule);
                                 modelInfo.setSilenceText(request.getSilenceText());
                                 modelInfo.setDataSource(tableName);
                                 modelInfo.setCreateTime(this.sdf2.format(date));
                                 modelInfo.setUpdateTime(this.sdf2.format(date));
                                 modelInfo.setUserName(userName);
                                 modelInfo.setModifierName(userName);
                                 modelInfo.setModifierId(Long.parseLong(request.getUserId()));
                                 modelInfo.setInputRule(mf.getFragmentContent());
                                 modelInfo.setDataLimit(dataSql);
                                 modelInfo.setModelStatus(0);
                                 modelInfo.setModelComment(modelInfo.getModelComment());
                                 this.logger.info("add model");
                                 long modelId = this.modelDB.saveModelAndGetModelID(modelInfo);
                                 List<ModelFragment> mfListNew = this.rebuildModelFragmentList(modelId, modelFragmentList, request.getPriviewId());
                                 this.logger.info("add modelFragments");
                                 this.fragmentDB.saveList(mfListNew);
                                 this.logger.info("success add model : " + modelId);
                                 this.serviceResponse.setSuccessful(true);
                                 this.serviceResponse.setValue(modelId);
                              }

                              return this.serviceResponse;
                           }
                        } else {
                           this.serviceResponse.setSuccessful(false);
                           this.serviceResponse.setMessage("用户信息查询为空");
                           return this.serviceResponse;
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var25) {
         this.logger.error("添加模型错误", var25);
         throw new ViePlatformServiceException("服务接口内部错误", var25);
      }
   }

   public ServiceResponse isOrderModel(ModelRequest request) throws ViePlatformServiceException {
      try {
         if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("用户id不能为空");
            return this.serviceResponse;
         }

         Long modelId = request.getModelId();
         Object[] modelObject = this.modelDB.queryModelInfoById(modelId);
         if (modelObject == null) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型不存在");
            return this.serviceResponse;
         }

         HashMap<String, Object> model = this.objectArrToMap(modelObject, "all");
         String isOrderModel = String.valueOf(model.get("isOrderModel"));
         this.serviceResponse.setSuccessful(true);
         this.serviceResponse.setValue(isOrderModel);
      } catch (Exception var6) {
         this.logger.error("查询模型顺序字段错误", var6);
         throw new ViePlatformServiceException("服务接口内部错误", var6);
      }

      return this.serviceResponse;
   }

   public ServiceResponse updateModelService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.serviceResponse = new ServiceResponse();
         this.logger.info("start update model");
         String regEx = "[`~@$%^*={}':;',\\[\\].<>/?~！@￥……*（）——{}【】‘；：”“'。，、？]";
         Pattern p = Pattern.compile(regEx);
         if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("用户id不能为空");
            return this.serviceResponse;
         } else {
            ModelInfo modelInfo = request.getModelInfo();
            Matcher m = p.matcher(modelInfo.getModelName());
            if (StringUtils.isNullOrEmpry(modelInfo.getModelName()) || m.find() || modelInfo.getModelName().length() > 100) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("模型名称不符合规则");
               return this.serviceResponse;
            } else if (modelInfo.getModelComment() != null && modelInfo.getModelComment().length() > 20) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("模型备注不能大于20字符");
               return this.serviceResponse;
            } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("数据源为空");
               return this.serviceResponse;
            } else {
               String tableName = request.getDataSource();
               if (modelInfo.getModelAccuracy() < 0.0F || modelInfo.getModelAccuracy() > 100.0F) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("模型准确率不在合理范围内");
                  return this.serviceResponse;
               } else if (request.getModelGroupId() == null) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("模型组ID为空");
                  return this.serviceResponse;
               } else {
                  ModelGroup group = this.modelGroupDB.queryModelGroupByID(request.getModelGroupId());
                  if (group == null) {
                     this.serviceResponse.setSuccessful(false);
                     this.serviceResponse.setMessage("模型组不存在");
                     return this.serviceResponse;
                  } else {
                     Object[] modelObject = this.modelDB.queryModelInfoById(modelInfo.getModelId());
                     if (modelObject == null) {
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("更新的模型不存在");
                        return this.serviceResponse;
                     } else {
                        HashMap<String, Object> model = this.objectArrToMap(modelObject, "all");
                        int modelStatus = Integer.parseInt(String.valueOf(model.get("modelStatus")));
                        if (modelStatus != 0 && modelStatus != -1) {
                           this.logger.info("test hava the same name");
                           boolean isHave = this.isHaveSameModel(modelInfo.getModelName(), tableName, modelInfo.getModelId(), request.getModelGroupId(), 0);
                           if (isHave) {
                              this.logger.info("已存在相同的模型名称");
                              this.serviceResponse.setSuccessful(false);
                              this.serviceResponse.setMessage("已存在相同的模型名称");
                              return this.serviceResponse;
                           } else {
                              ModelFragment mf = request.getModelFragment();
                              if (mf.getRuleType() == 1) {
                                 this.serviceResponse.setSuccessful(false);
                                 this.serviceResponse.setMessage("片段组合规则不存在");
                                 return this.serviceResponse;
                              } else {
                                 mf.setModelId(modelInfo.getModelId());
                                 List<ModelFragment> modelFragmentList = new ArrayList<>();
                                 String rule = "";
                                 String inputRule = "";
                                 this.logger.info("zl fragment list");
                                 if (this.isModelFragment(request.getModelFragments())) {
                                    modelFragmentList.add(mf);
                                 } else {
                                    List<HashMap<String, Object>> list = (List<HashMap<String, Object>>)this.mapper
                                       .readValue(request.getModelFragments(), List.class);
                                    boolean isHaveStructuredRule = StringUtils.isNullOrEmpry(mf.getFragmentContent()) && list.size() > 0;

                                    for (HashMap<String, Object> hm : (List<HashMap<String, Object>>)list) {
                                       ModelFragment mft = new ModelFragment();
                                       mft.setChannel(Integer.parseInt(String.valueOf(hm.get("channel"))));
                                       int channel = Integer.parseInt(String.valueOf(hm.get("channel")));
                                       if (channel != 0 && channel != 1 && channel != 2) {
                                          this.serviceResponse.setSuccessful(false);
                                          this.serviceResponse.setMessage("片段声道不在合理范围内");
                                          return this.serviceResponse;
                                       }

                                       mft.setTagContent(this.mapper.writeValueAsString(hm.get("tagContent")));
                                       mft.setIsTag(Integer.parseInt(String.valueOf(hm.get("isTag"))));
                                       mft.setFragmentContent(String.valueOf(hm.get("fragmentContent")));
                                       mft.setFragmentId(Long.parseLong(String.valueOf(hm.get("fragmentId"))));
                                       mft.setFragmentNum(Integer.parseInt(String.valueOf(hm.get("fragmentNum"))));
                                       mft.setRuleType(Integer.parseInt(String.valueOf(hm.get("ruleType"))));
                                       int ruleType = Integer.parseInt(String.valueOf(hm.get("ruleType")));
                                       if (ruleType != 1 && ruleType != 2) {
                                          this.serviceResponse.setSuccessful(false);
                                          this.serviceResponse.setMessage("片段类型不在合理范围内");
                                          return this.serviceResponse;
                                       }

                                       mft.setRemark(String.valueOf(hm.get("remark")));
                                       mft.setTagText(String.valueOf(hm.get("tagText")));
                                       mft.setTagVersion(String.valueOf(hm.get("tagVersion")));
                                       mft.setModelId(modelInfo.getModelId());
                                       modelFragmentList.add(mft);
                                    }

                                    modelFragmentList.add(mf);
                                    if (!isHaveStructuredRule) {
                                       String message = this.checkFragments(modelFragmentList, request.getChoose());
                                       if (!"true".equals(message)) {
                                          this.serviceResponse.setSuccessful(false);
                                          this.serviceResponse.setMessage(message);
                                          return this.serviceResponse;
                                       }

                                       HashMap<Integer, String> hm = this.getModelFragmentHm(modelFragmentList);
                                       ModelParserPlatform mp = new ModelParserPlatform(null, mf.getFragmentContent(), hm);
                                       ParserResultPlatform ps = mp.parse();
                                       ParserResultPlatform parserResult = mp.parseReplace();
                                       if (parserResult.getRet() == 0 || ps.getRet() != -1) {
                                          this.logger.error("标签规则错误");
                                          this.serviceResponse.setSuccessful(false);
                                          this.serviceResponse.setMessage("标签规则错误");
                                          return this.serviceResponse;
                                       }

                                       rule = mp.getCombineRule();
                                       inputRule = mf.getFragmentContent();
                                    } else {
                                       rule = "";
                                       inputRule = "";
                                    }
                                 }

                                 this.logger.info("deal fragment list");
                                 List<Long> oldFramentIds = this.fragmentDB.queryAllFragmentId(request.getModelInfo().getModelId());
                                 List<Long> newFragmentIds = this.getFragmentId(modelFragmentList);
                                 List<Long> deleteFragmentIds = this.getDeleteFramentIds(oldFramentIds, newFragmentIds);
                                 if (!ListUtil.isNullOrEmpty(deleteFragmentIds)) {
                                    this.fragmentDB.deleteFragments(deleteFragmentIds);
                                 }

                                 List<ModelFragment> fragments = this.fragmentDB.getFragmentByIds(newFragmentIds);
                                 String oldTagText = "";

                                 for (ModelFragment mf2 : fragments) {
                                    oldTagText = oldTagText + mf2.getTagContent();
                                 }

                                 String newTagText = "";

                                 for (ModelFragment mf2 : modelFragmentList) {
                                    newTagText = newTagText + mf2.getTagContent();
                                 }

                                 List<ModelFragment> modelFragments = this.getUpdateFragments(fragments, modelFragmentList);
                                 this.logger.info("save fragment list");
                                 this.fragmentDB.saveList(modelFragments);
                                 this.fragmentDB.deleteModelFragmentAnd(-1L, request.getPriviewId());
                                 ModelInfo modelInfoNew = this.modelDB.queryModelById(request.getModelInfo().getModelId());
                                 if (modelInfoNew == null) {
                                    this.serviceResponse.setSuccessful(false);
                                    this.serviceResponse.setMessage("模型ID对应的模型不存在");
                                    return this.serviceResponse;
                                 } else {
                                    if (modelInfoNew.getModelGroup().getGroupId() != request.getModelGroupId()) {
                                       modelInfoNew.setModelGroup(group);
                                    }

                                    int isChange = 0;
                                    String oldSilRule = modelInfoNew.getSilenceRule();
                                    String oldSilText = modelInfoNew.getSilenceText();
                                    String newSilRule = this.dealJyTagContent(request.getSilenceRule(), false);
                                    if (null != newSilRule && !"".equals(newSilRule) && !newSilRule.contains("</filters>")) {
                                       this.serviceResponse.setSuccessful(false);
                                       this.serviceResponse.setMessage(newSilRule);
                                       return this.serviceResponse;
                                    } else {
                                       if (rule.equals(modelInfoNew.getTextRule())
                                          && newTagText.equals(oldTagText)
                                          && (oldSilRule == null || newSilRule == null || oldSilRule.equals(newSilRule))
                                          && (request.getSilenceText() == null || oldSilText == null || request.getSilenceText().equals(oldSilText))) {
                                          isChange = modelInfoNew.getHasChange();
                                       } else {
                                          this.logger.info("the model condition has change");
                                          isChange = 1;
                                       }

                                       SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                       Date date = new Date();
                                       String userName = "";

                                       try {
                                          Map<Integer, Map<String, String>> usersInfo = this.userService.queryAllUserInfo(request.getDataSource());
                                          this.logger.info("all the users info is : " + JSON.toJSONString(usersInfo));
                                          int userid = Integer.parseInt(request.getUserId());
                                          Map<String, String> userInfo = usersInfo.get(userid);
                                          if (userInfo != null) {
                                             userName = userInfo.get("accountName");
                                          }
                                       } catch (Exception var34) {
                                          this.logger.info("get userinfo from permission error");
                                       }

                                       String dataSql = this.commonData.getDataSql(request.getUserId(), "VIEWEB", request.getDataSource());
                                       modelInfoNew.setModelName(request.getModelInfo().getModelName());
                                       modelInfoNew.setChannel(request.getModelInfo().getChannel());
                                       modelInfoNew.setModelComment(request.getModelInfo().getModelComment());
                                       modelInfoNew.setPageType(request.getModelInfo().getPageType());
                                       modelInfoNew.setTextRule(rule);
                                       modelInfoNew.setDataSource(tableName);
                                       modelInfoNew.setUpdateTime(sdf.format(date));
                                       modelInfoNew.setHasChange(isChange);
                                       modelInfoNew.setSilenceRule(newSilRule);
                                       modelInfoNew.setSilenceText(request.getSilenceText());
                                       modelInfoNew.setUpdateTime(sdf.format(date));
                                       modelInfoNew.setModifierName(userName);
                                       modelInfoNew.setModelAccuracy(request.getModelInfo().getModelAccuracy());
                                       modelInfoNew.setInputRule(inputRule);
                                       modelInfoNew.setDataLimit(dataSql);
                                       modelInfoNew.setModelStatus(0);
                                       this.logger.info("update model");
                                       this.modelDB.updateModel(modelInfoNew);
                                       this.logger.info("success update model : " + modelInfoNew.getModelId());
                                       this.serviceResponse.setSuccessful(true);
                                       this.serviceResponse.setMessage("更新成功");
                                       return this.serviceResponse;
                                    }
                                 }
                              }
                           }
                        } else {
                           this.serviceResponse.setSuccessful(false);
                           this.serviceResponse.setMessage("已上线或上线中的模型不允许编辑");
                           return this.serviceResponse;
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var35) {
         throw new ViePlatformServiceException("接口内部异常", var35);
      }
   }

   public ServiceResponse deleteModelsService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start delete models");
         this.serviceResponse = new ServiceResponse();
         if (request.getModelIds() == null) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型ID为空");
            return this.serviceResponse;
         } else {
            List<String> theNotModelNme = new ArrayList<>();

            for (Long modelId : request.getModelIds()) {
               Object[] modelObject = this.modelDB.queryModelInfoById(modelId);
               if (modelObject == null) {
                  this.logger.info("the model not exits or has been deleted");
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("要删除的模型不存在或已被删除");
                  return this.serviceResponse;
               }

               HashMap<String, Object> hm = this.objectArrToMap(modelObject, "all");
               int modelStatus = Integer.parseInt(String.valueOf(hm.get("modelStatus")));
               if (modelStatus == TaskInfoStatus.GoLineTask.getTaskStatus() || modelStatus == TaskInfoStatus.OnLineTask.getTaskStatus()) {
                  this.logger.info("the model not exits or has deleted");
                  theNotModelNme.add(String.valueOf(hm.get("modelName")));
               }
            }

            if (!ListUtil.isNullOrEmpty(theNotModelNme)) {
               String name = "";

               for (int i = 0; i < theNotModelNme.size(); i++) {
                  if (i == theNotModelNme.size() - 1) {
                     name = name + theNotModelNme.get(i);
                  } else {
                     name = name + theNotModelNme.get(i) + "、";
                  }
               }

               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("当前模型" + name + "不能进行删除操作");
               return this.serviceResponse;
            } else {
               this.logger.info("start delete model");
               List<Long> fragmentIds = new ArrayList<>();

               for (int ix = 0; ix < request.getModelIds().size(); ix++) {
                  fragmentIds.addAll(this.fragmentDB.queryAllFragmentId((Long)request.getModelIds().get(ix)));
               }

               if (fragmentIds.size() != 0) {
                  this.dataMarkInfoDB.deleteByFragmentId(fragmentIds);
               }

               for (Long lon : request.getModelIds()) {
                  this.fragmentDB.deleteModelFragment(lon, -10000L);
               }

               this.modelDB.deleteModelsByIds(request.getModelIds());
               this.logger.info("success delete model");
               this.serviceResponse.setSuccessful(true);
               this.serviceResponse.setMessage("删除成功");
               return this.serviceResponse;
            }
         }
      } catch (Exception var8) {
         this.logger.info("删除模型异常", var8);
         throw new ViePlatformServiceException("接口内部错误", var8);
      }
   }

   public Map<String, Object> searExportByModelId(ModelRequest request, List<Long> modelIds) throws ViePlatformServiceException {
      try {
         this.logger.info("start search model by group");
         this.serviceResponse = new ServiceResponse();
         HashMap<String, Object> result = new HashMap<>();
         new ArrayList();
         if (request.getModelGroupId() == null) {
            this.logger.info("模型组ID为空");
            return result;
         } else {
            this.logger.info("start get user modelGroup");

            List permissionGroupIds;
            try {
               if (StringUtils.isNullOrEmpry(request.getUserId()) || !request.getUserId().matches("[0-9]+")) {
                  this.logger.info("the userID error");
                  return result;
               }

               GroupAuthRequest authRequest = new GroupAuthRequest();
               authRequest.setUserId(Long.parseLong(request.getUserId()));
               authRequest.setGroupType(1);
               authRequest.setDataSource(request.getDataSource());
               permissionGroupIds = this.groupAuthService.queryGroupAuth(authRequest);
            } catch (Exception var19) {
               this.logger.info("获取权限系统错误", var19);
               throw new ViePlatformServiceException("获取权限系统错误", var19);
            }

            this.logger.info("get user modelGroup done");
            if (ListUtil.isNullOrEmpty(permissionGroupIds)) {
               this.logger.info("模型组权限为空");
               return result;
            } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
               this.logger.info("数据源为空");
               result.put("message", "数据源为空");
               return result;
            } else {
               String tableName = request.getDataSource();
               new ArrayList();
               boolean isLoneData = false;
               List<Long> batchIds = null;
               if (request.getIfLone() != null && request.getIfLone() == 1 && StringUtils.isNotNullAndEmpry(request.getBatchId())) {
                  batchIds = this.batchInfoDB.getModelIdByBatch(request.getBatchId());
                  isLoneData = true;
               }

               if (!"all".equals(request.getType())) {
                  if ("online".equals(request.getType())) {
                     this.logger.info("start search online model by group");
                     List<Long> groupIdList = new ArrayList<>();
                     if (request.getModelGroupId() != null && request.getModelGroupId() != -1L) {
                        groupIdList.add(request.getModelGroupId());
                     } else {
                        this.logger.info("get all the groupIDs by parent groupIds");
                        groupIdList = this.modelGroupDB.getModelGroupIdById(permissionGroupIds, request.getDataSource());
                        if (ListUtil.isNullOrEmpty(groupIdList)) {
                           this.logger.info("the modelIds is null");
                           return result;
                        }
                     }

                     this.logger.info("get model width detail");
                     List<Object[]> list = this.modelDB
                        .getOnLineModelsByGroup(request.getModelGroupId(), tableName, request.getModelName(), groupIdList, this.modelGroupDB, batchIds);
                     List<HashMap<String, Object>> mapList = new ArrayList<>();
                     if (ListUtil.isNullOrEmpty(list)) {
                        result.put("rows", mapList);
                        return result;
                     }

                     this.logger.info("deal model to map");

                     for (Object[] ob : list) {
                        HashMap<String, Object> hm = this.objectArrToMap(ob, "online");
                        mapList.add(hm);
                     }

                     if (mapList != null && !mapList.isEmpty()) {
                        for (int i = 0; i < mapList.size(); i++) {
                           String modelId = String.valueOf(mapList.get(i).get("modelId"));
                           int modelStatus = Integer.parseInt(String.valueOf(mapList.get(i).get("modelStatus")));
                           String modelCount = String.valueOf(mapList.get(i).get("modelCount"));
                           String number = this.getOnlineProgress(modelId, modelStatus, request.getDataSource(), modelCount);
                           mapList.get(i).put("onlineProgress", number);
                        }
                     }

                     result.put("rows", mapList);
                  }
               } else {
                  this.logger.info("start search all model by group");
                  int totalNum = 0;
                  List groupIds;
                  if (request.getModelGroupId() != null && request.getModelGroupId() != -1L) {
                     ModelGroup group = this.modelGroupDB.queryModelGroupByID(request.getModelGroupId());
                     if (group == null) {
                        this.logger.info("模型组为空");
                        return result;
                     }

                     this.logger.info("get the groupIds by parent");
                     groupIds = this.modelGroupDB.getModelGroupIdById(request.getModelGroupId(), request.getDataSource());
                     this.logger.info("get model total num");
                     if (!isLoneData) {
                        totalNum = this.modelDB
                           .queryAllModelCountByGroup(tableName, request.getModelName(), request.getTextRule(), request.getModifierName(), groupIds, null);
                     } else {
                        totalNum = this.modelDB
                           .queryAllModelCountByGroup(tableName, request.getModelName(), request.getTextRule(), request.getModifierName(), groupIds, batchIds);
                     }
                  } else {
                     new ArrayList();
                     if (null == permissionGroupIds || permissionGroupIds.size() == 0) {
                        return result;
                     }

                     this.logger.info("get the all groupIds from permission");
                     groupIds = this.modelGroupDB.queryAllModelGroupIdsByPermission(permissionGroupIds, request.getDataSource());
                     if (ListUtil.isNullOrEmpty(groupIds)) {
                        this.logger.info("无模型组");
                        return result;
                     }

                     this.logger.info("get model total num");
                     if (!isLoneData) {
                        totalNum = this.modelDB
                           .queryAllModelCountByGroup(tableName, request.getModelName(), request.getTextRule(), request.getModifierName(), groupIds, null);
                     } else {
                        totalNum = this.modelDB
                           .queryAllModelCountByGroup(tableName, request.getModelName(), request.getTextRule(), request.getModifierName(), groupIds, batchIds);
                     }
                  }

                  int totalPage = totalNum % request.getPageSize() == 0 ? totalNum / request.getPageSize() : totalNum / request.getPageSize() + 1;
                  this.logger.info("get model width detail");
                  String orderType = StringUtils.isNullOrEmpry(request.getOrderType()) ? "desc" : request.getOrderType();
                  List<Object[]> listx = this.modelDB
                     .queryExportByModelinfo(
                        tableName,
                        request.getPageNum(),
                        request.getPageSize(),
                        request.getModelName(),
                        request.getTextRule(),
                        request.getModifierName(),
                        groupIds,
                        modelIds,
                        request.getOrderColumn(),
                        orderType
                     );
                  List<HashMap<String, Object>> mapListx = new ArrayList<>();
                  this.logger.info("deal model to map");

                  for (Object[] ob : listx) {
                     HashMap<String, Object> hm = this.objectArrToMap(ob, "all");
                     mapListx.add(hm);
                  }

                  if (mapListx != null && !mapListx.isEmpty()) {
                     for (int i = 0; i < mapListx.size(); i++) {
                        String modelId = String.valueOf(mapListx.get(i).get("modelId"));
                        int modelStatus = Integer.parseInt(String.valueOf(mapListx.get(i).get("modelStatus")));
                        String modelCount = String.valueOf(mapListx.get(i).get("modelCount"));
                        String number = this.getOnlineProgress(modelId, modelStatus, request.getDataSource(), modelCount);
                        mapListx.get(i).put("onlineProgress", number);
                     }
                  }

                  result.put("pageNum", request.getPageNum());
                  result.put("pageSize", request.getPageSize());
                  result.put("totalRows", totalNum);
                  result.put("totalPages", totalPage);
                  result.put("rows", mapListx);
               }

               this.logger.info("success search model by group");
               return result;
            }
         }
      } catch (Exception var20) {
         this.logger.info("根据模型id获取报表导出内容异常", var20);
         throw new ViePlatformServiceException("内部错误", var20);
      }
   }

   private String getOnlineProgress(String modelId, int modelStatus, String dataSource, String modelCount) throws Exception {
      String number;
      if (modelStatus == 0) {
         long num = 0L;
         String url = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
         ExcuteContext excuteContext = new ExcuteContext();
         String table = dataSource + "model_" + modelId;
         String tableSql = "select " + table + " from TABLE_INFO";
         TableInfoResultSet trs = excuteContext.executeQueryFieldName(url, tableSql);
         if (trs != null && trs.getTolCount() > 0) {
            String sql = "select count(idCounter) as result from " + table + " where processed=0";
            ResultSet rs = excuteContext.executeSearchQuery(url, sql);
            if (rs != null && rs.toList() != null && !rs.toList().isEmpty()) {
               List<LinkedHashMap<String, Object>> linkedHashMapList = rs.toList();
               String a = String.valueOf(linkedHashMapList.get(0).get("result"));
               if (!"null".equals(a) && a.contains(".")) {
                  a = a.substring(0, a.indexOf("."));
               }

               if (!"null".equals(a)) {
                  num = Integer.valueOf(a).intValue();
               }
            }
         }

         if (!"null".equals(modelCount) && !"".equals(modelCount) && !"0".equals(modelCount)) {
            number = this.df1.format((float)num / this.df1.parse(modelCount).floatValue() * 100.0F) + "%";
         } else {
            number = this.df1.format(0L) + "%";
         }
      } else if (modelStatus == -1) {
         number = this.df1.format(100L) + "%";
      } else {
         number = this.df1.format(0L) + "%";
      }

      return number;
   }

   public Map<String, Object> searConditionByModelId(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search conition by modelId");
         this.serviceResponse = new ServiceResponse();
         String modelId = request.getModelId().toString();
         HashMap<String, Object> result = new HashMap<>();
         String list = this.modelDB.queryConditionByModelID(modelId);
         result.put("filter_rule", JSON.parseArray(String.valueOf(list)));
         if (JSON.parseArray(String.valueOf(list)) == null) {
            result.put("hascdt", "0");
         } else if (JSON.parseArray(String.valueOf(list)).size() > 1) {
            result.put("hascdt", "1");
         } else {
            result.put("hascdt", "0");
         }

         return result;
      } catch (Exception var5) {
         this.logger.info("根据模型id获取模型筛选条件异常", var5);
         throw new ViePlatformServiceException("内部错误", var5);
      }
   }

   public Map<String, Object> searModelByGroupService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("######## start search model by group");
         this.logger.info("dataSource is: " + request.getDataSource());
         this.serviceResponse = new ServiceResponse();
         HashMap<String, Object> result = new HashMap<>();
         new ArrayList();
         if (request.getModelGroupId() == null) {
            this.logger.info("模型组ID为空");
            return result;
         } else {
            this.logger.info("start get user modelGroup");

            List permissionGroupIds;
            try {
               if (StringUtils.isNullOrEmpry(request.getUserId()) || !request.getUserId().matches("[0-9]+")) {
                  this.logger.info("the userID error");
                  return result;
               }

               GroupAuthRequest authRequest = new GroupAuthRequest();
               authRequest.setUserId(Long.parseLong(request.getUserId()));
               authRequest.setGroupType(1);
               authRequest.setDataSource(request.getDataSource());
               permissionGroupIds = this.groupAuthService.queryGroupAuth(authRequest);
            } catch (Exception var18) {
               this.logger.info("获取权限系统错误", var18);
               throw new ViePlatformServiceException("获取权限系统错误", var18);
            }

            this.logger.info("get user modelGroup done");
            if (ListUtil.isNullOrEmpty(permissionGroupIds)) {
               this.logger.info("模型组权限为空");
               return result;
            } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
               this.logger.info("数据源为空");
               result.put("message", "数据源为空");
               return result;
            } else {
               String tableName = request.getDataSource();
               new ArrayList();
               boolean isLoneData = false;
               List<Long> batchIds = null;
               if (request.getIfLone() != null && request.getIfLone() == 1 && StringUtils.isNotNullAndEmpry(request.getBatchId())) {
                  batchIds = this.batchInfoDB.getModelIdByBatch(request.getBatchId());
                  isLoneData = true;
               }

               if (!"all".equals(request.getType())) {
                  if ("online".equals(request.getType())) {
                     this.logger.info("start search online model by group");
                     List<Long> groupIdList = new ArrayList<>();
                     if (request.getModelGroupId() != null && request.getModelGroupId() != -1L) {
                        groupIdList.add(request.getModelGroupId());
                     } else {
                        this.logger.info("get all the groupIDs by parent groupIds");
                        groupIdList = this.modelGroupDB.getModelGroupIdById(permissionGroupIds, request.getDataSource());
                        if (ListUtil.isNullOrEmpty(groupIdList)) {
                           this.logger.info("the modelIds is null");
                           return result;
                        }
                     }

                     this.logger.info("get model width detail");
                     List<Object[]> list = this.modelDB
                        .getOnLineModelsByGroup(request.getModelGroupId(), tableName, request.getModelName(), groupIdList, this.modelGroupDB, batchIds);
                     List<HashMap<String, Object>> mapList = new ArrayList<>();
                     if (ListUtil.isNullOrEmpty(list)) {
                        result.put("rows", mapList);
                        return result;
                     }

                     this.logger.info("deal model to map");

                     for (Object[] ob : list) {
                        HashMap<String, Object> hm = this.objectArrToMap(ob, "online");
                        mapList.add(hm);
                     }

                     result.put("rows", mapList);
                  }
               } else {
                  this.logger.info("start search all model by group");
                  int totalNum = 0;
                  List groupIds;
                  if (request.getModelGroupId() != null && request.getModelGroupId() != -1L) {
                     ModelGroup group = this.modelGroupDB.queryModelGroupByID(request.getModelGroupId());
                     if (group == null) {
                        this.logger.info("模型组为空");
                        return result;
                     }

                     this.logger.info("get the groupIds by parent");
                     groupIds = this.modelGroupDB.getModelGroupIdById(request.getModelGroupId(), request.getDataSource());
                     this.logger.info("get model total num");
                     if (!isLoneData) {
                        totalNum = this.modelDB
                           .queryAllModelCountByGroup(tableName, request.getModelName(), request.getTextRule(), request.getModifierName(), groupIds, null);
                     } else {
                        totalNum = this.modelDB
                           .queryAllModelCountByGroup(tableName, request.getModelName(), request.getTextRule(), request.getModifierName(), groupIds, batchIds);
                     }
                  } else {
                     new ArrayList();
                     if (null == permissionGroupIds || permissionGroupIds.size() == 0) {
                        return result;
                     }

                     this.logger.info("get the all groupIds from permission");
                     groupIds = this.modelGroupDB.queryAllModelGroupIdsByPermission(permissionGroupIds, request.getDataSource());
                     if (ListUtil.isNullOrEmpty(groupIds)) {
                        this.logger.info("无模型组");
                        return result;
                     }

                     this.logger.info("get model total num");
                     if (!isLoneData) {
                        totalNum = this.modelDB
                           .queryAllModelCountByGroup(tableName, request.getModelName(), request.getTextRule(), request.getModifierName(), groupIds, null);
                     } else {
                        totalNum = this.modelDB
                           .queryAllModelCountByGroup(tableName, request.getModelName(), request.getTextRule(), request.getModifierName(), groupIds, batchIds);
                     }
                  }

                  int totalPage = totalNum % request.getPageSize() == 0 ? totalNum / request.getPageSize() : totalNum / request.getPageSize() + 1;
                  this.logger.info("get model width detail");
                  String orderType = StringUtils.isNullOrEmpry(request.getOrderType()) ? "desc" : request.getOrderType();
                  List<Object[]> list = this.modelDB
                     .queryAllModelByGroup(
                        tableName,
                        request.getPageNum(),
                        request.getPageSize(),
                        request.getModelName(),
                        request.getTextRule(),
                        request.getModifierName(),
                        groupIds,
                        batchIds,
                        request.getOrderColumn(),
                        orderType
                     );
                  List<HashMap<String, Object>> mapList = new ArrayList<>();
                  this.logger.info("deal model to map");

                  for (Object[] ob : list) {
                     HashMap<String, Object> hm = this.objectArrToMap(ob, "all");
                     mapList.add(hm);
                  }

                  if (mapList != null && !mapList.isEmpty()) {
                     for (int i = 0; i < mapList.size(); i++) {
                        String modelId = String.valueOf(mapList.get(i).get("modelId"));
                        int modelStatus = Integer.parseInt(String.valueOf(mapList.get(i).get("modelStatus")));
                        String modelCount = String.valueOf(mapList.get(i).get("modelCount"));
                        String number = this.getOnlineProgress(modelId, modelStatus, request.getDataSource(), modelCount);
                        mapList.get(i).put("onlineProgress", number);
                     }
                  }

                  result.put("pageNum", request.getPageNum());
                  result.put("pageSize", request.getPageSize());
                  result.put("totalRows", totalNum);
                  result.put("totalPages", totalPage);
                  result.put("rows", mapList);
               }

               this.logger.info("success search model by group");
               return result;
            }
         }
      } catch (Exception var19) {
         this.logger.info("根据模型组查询模型异常", var19);
         throw new ViePlatformServiceException("内部错误", var19);
      }
   }

   private HashMap<String, Object> objectArrToMap(Object[] objArr, String type) {
      HashMap<String, Object> hm = new HashMap<>();
      if ("all".equals(type)) {
         hm.put("modelId", objArr[0]);
         hm.put("modelName", objArr[1]);
         hm.put("updateTime", objArr[3]);
         hm.put("modifierName", objArr[5]);
         hm.put("dataSource", objArr[6]);
         if (objArr.length > 16 && objArr[16] != null) {
            hm.put("createTime", objArr[16]);
         }

         if (objArr[7] instanceof Clob) {
            String s = "";
            s = SqlHelperPlatform.clobToString((Clob)objArr[7]);
            hm.put("screeningRule", objArr[7] == null ? null : JSON.parseArray(String.valueOf(s)));
         } else {
            hm.put("screeningRule", objArr[7] == null ? null : JSON.parseArray(String.valueOf(objArr[7])));
         }

         int modelStatuts = TaskInfoStatus.OptimizeTask.getTaskStatus();
         if (objArr[8] != null) {
            modelStatuts = Integer.parseInt(String.valueOf(objArr[8]));
         } else if (objArr[14] != null) {
            modelStatuts = TaskInfoStatus.getStatusByObj(objArr[14]);
         }

         if (modelStatuts == 1) {
            modelStatuts = TaskInfoStatus.GoLineTask.getTaskStatus();
         } else if (modelStatuts == 0 || modelStatuts == 5) {
            modelStatuts = TaskInfoStatus.OnLineTask.getTaskStatus();
         } else if (modelStatuts > 1 && modelStatuts != 5) {
            hm.put("errorReson", TaskInfoStatus.getTaskStatus(modelStatuts).getErrorReason());
            modelStatuts = TaskInfoStatus.OnLineFail.getTaskStatus();
         }

         hm.put("modelStatus", modelStatuts);
         String modelCount = String.valueOf(objArr[9]);
         String totalCount = String.valueOf(objArr[11]);
         if (StringUtils.isNullOrEmpry(modelCount) || "null".equals(modelCount)) {
            modelCount = "-";
         }

         hm.put("modelCount", modelCount);
         hm.put("count", totalCount);
         int isUp = Integer.valueOf(String.valueOf(objArr[10]));
         hm.put("isUp", isUp);
         String modelComment = String.valueOf(objArr[12]);
         if (StringUtils.isNullOrEmpry(modelComment) || "null".equals(modelComment)) {
            modelComment = "";
         }

         hm.put("modelComment", modelComment);
         float a = Float.valueOf(objArr[13].toString());
         float percentNumber = Math.round(a * 100.0F) / 100.0F;
         modelCount = modelCount == "-" ? "0" : modelCount;
         totalCount = totalCount != "" && totalCount != null && totalCount != "null" ? totalCount : "0";
         double percentcheckRate = 0.0;
         if (!"0".equals(totalCount)) {
            percentcheckRate = Double.valueOf(modelCount) / Double.valueOf(totalCount) * 100.0;
         }

         double percent = percentNumber;
         percent = Double.parseDouble(this.df.format(percent));
         percentcheckRate = Double.parseDouble(this.df.format(percentcheckRate));
         hm.put("modelAccuracy", percent + "%");
         hm.put("checkRate", percentcheckRate + "%");
      } else if ("online".equals(type)) {
         hm.put("modelId", objArr[0]);
         hm.put("modelName", objArr[1]);
         hm.put("modelGroupId", objArr[2]);
         hm.put("modelGroupName", objArr[3]);
      }

      if (objArr.length > 15) {
         hm.put("isOrderModel", objArr[15]);
      }

      return hm;
   }

   public ServiceResponse searchModelInfoService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search modelInfo");
         this.serviceResponse = new ServiceResponse();
         if (request.getModelId() != null && !"".equals(request.getModelId())) {
            ModelInfo modelInfo = this.modelDB.queryModelById(request.getModelId());
            if (modelInfo != null && modelInfo.getModelStatus() != 2) {
               List<ModelFragment> modelFragmentList = this.fragmentDB.queryAllFragment(request.getModelId());
               ModelDetailDTO modelDetailDTO = new ModelDetailDTO();
               this.fillModelBaseInfo(modelDetailDTO, modelInfo);
               this.fillModelFragments(modelDetailDTO, modelFragmentList);
               this.serviceResponse.setSuccessful(true);
               this.serviceResponse.setValue(modelDetailDTO);
               this.logger.info("success search modelinfo");
               return this.serviceResponse;
            } else {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("查询的模型不存在");
               return this.serviceResponse;
            }
         } else {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("传入的模型为空");
            return this.serviceResponse;
         }
      } catch (Exception var5) {
         this.logger.error("查询模型信息异常", var5);
         throw new ViePlatformServiceException("接口内部错误", var5);
      }
   }

   public ServiceResponse searchAllCategoryModelList(ModelRequest request) throws ViePlatformServiceException {
      this.logger.info("start searchAllCategoryModelList method");
      String userId = request.getUserId();
      String dataSource = request.getDataSource();
      ServiceResponse response = null;
      if (StringUtils.isNullOrEmpry(userId)) {
         response = new ServiceResponse();
         response.setSuccessful(false);
         response.setMessage("userId不能为空");
         return response;
      } else if (StringUtils.isNullOrEmpry(dataSource)) {
         response = new ServiceResponse();
         response.setSuccessful(false);
         response.setMessage("dataSource不能为空");
         return response;
      } else {
         new ArrayList();

         try {
            GroupAuthRequest authRequest = new GroupAuthRequest();
            authRequest.setUserId(Long.parseLong(request.getUserId()));
            authRequest.setGroupType(1);
            authRequest.setDataSource(request.getDataSource());
            List<Long> permissionGroupIds = this.groupAuthService.queryGroupAuth(authRequest);
            List<String[]> resultMap = this.modelGroupDB.queryAllCategoryModelList(permissionGroupIds, dataSource);
            List<HashMap<String, String>> categoryMap = new ArrayList<>();

            for (int i = 0; i < resultMap.size(); i++) {
               HashMap<String, String> eachMap = new HashMap<>();
               String[] eachStr = resultMap.get(i);
               eachMap.put("modelId", eachStr[0]);
               eachMap.put("modelName", eachStr[1]);
               eachMap.put("categoryType", eachStr[2]);
               categoryMap.add(eachMap);
            }

            response = new ServiceResponse();
            response.setSuccessful(true);
            response.setValue(categoryMap);
            return response;
         } catch (PermissionServiceException var12) {
            this.logger.info("获取所有业务质检项失败", var12);
            throw new ViePlatformServiceException("接口内部错误", var12);
         } catch (Exception var13) {
            this.logger.info("获取数据源失败", var13);
            throw new ViePlatformServiceException("获取数据源失败", var13);
         }
      }
   }

   public ServiceResponse onlineModelService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.serviceResponse = new ServiceResponse();
         this.logger.info("start online model");
         ModelInfo modelInfo = this.modelDB.queryModelById(request.getModelId());
         if (modelInfo == null) {
            this.logger.info("the online model not exits");
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("上线的模型不存在");
            return this.serviceResponse;
         } else {
            Object[] modelInfoObject = this.modelDB.queryModelInfoById(request.getModelId());
            if (modelInfoObject == null) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("上线的模型不存在");
               return this.serviceResponse;
            } else {
               HashMap<String, Object> hm = this.objectArrToMap(modelInfoObject, "all");
               int modelStatus = Integer.parseInt(String.valueOf(hm.get("modelStatus")));
               if (modelStatus != TaskInfoStatus.OnLineTask.getTaskStatus() && modelStatus != TaskInfoStatus.GoLineTask.getTaskStatus()) {
                  this.modelDB.modelOffline(modelInfo.getModelId());
                  modelInfo.setIsPass(request.getIsPass());
                  String checkResult = this.checkOnlineCondition(request.getCondition());
                  if (!"true".equals(checkResult)) {
                     this.logger.info("the condition has error");
                     this.serviceResponse.setSuccessful(false);
                     this.serviceResponse.setMessage(checkResult);
                     return this.serviceResponse;
                  } else {
                     String dataSql = this.commonData.getDataSql(request.getUserId(), "VIEWEB", request.getDataSource());
                     if (request.getFilterRuleId() != null && request.getFilterRuleId() > 0) {
                        FilterInfo filterInfo = this.filterInfoDB.queryFilterInfoById(request.getFilterRuleId());
                        if (filterInfo == null || "0".equals(filterInfo.getFlag())) {
                           this.logger.info("the condition has error");
                           this.serviceResponse.setSuccessful(false);
                           this.serviceResponse.setMessage("模型引用的筛选器已不存在!");
                           return this.serviceResponse;
                        }

                        if (filterInfo.getContainModel() == 1) {
                           this.logger.info("the condition has error");
                           this.serviceResponse.setSuccessful(false);
                           this.serviceResponse.setMessage("模型上线时，模型维度不可用，请修改筛选条件!");
                           return this.serviceResponse;
                        }

                        if (StringUtils.isNotNullAndEmpry(dataSql)) {
                           dataSql = dataSql + (StringUtils.isNotNullAndEmpry(filterInfo.getFilterSql()) ? " and " + filterInfo.getFilterSql() + " " : "");
                        } else if (StringUtils.isNullOrEmpry(dataSql) && StringUtils.isNotNullAndEmpry(filterInfo.getFilterSql())) {
                           dataSql = filterInfo.getFilterSql() + " ";
                        }

                        modelInfo.setFilterId(request.getFilterRuleId());
                     } else {
                        modelInfo.setFilterId(0);
                     }

                     modelInfo.setDataLimit(dataSql);
                     String onlineTime = this.getOnlineTimeByCondition(request.getCondition());
                     modelInfo.setExtend1(onlineTime);
                     FilterParserPlatform newFilterParser = new FilterParserPlatform(request.getCondition());

                     try {
                        newFilterParser.parserFiltersOnline();
                     } catch (Exception var15) {
                        this.logger.error("规则验证错误");
                     }

                     if (!newFilterParser.isRet()) {
                        System.out.println("");
                     }

                     boolean hasChange = false;
                     if (modelInfo.getHasChange() == 1) {
                        this.logger.info("the model has changed");
                        modelInfo.setModelStatus(2);
                        this.modelDB.deleteModel(modelInfo);
                        modelInfo.setModelCondition(request.getCondition());
                        modelInfo.setFilterRule(request.getCondition());
                        Long modelId = modelInfo.getModelId();
                        modelInfo.setHasChange(0);
                        modelInfo.setModelUptonow(1);
                        modelInfo.setModelStatus(0);
                        long newModelId = this.modelDB.saveModel(modelInfo);
                        this.logger.info("the model online new modelId:" + newModelId);
                        this.fragmentDB.updateModelFragment(modelId, newModelId);
                        List<Long> modelIdList = new ArrayList<>();
                        modelIdList.add(modelId);
                        this.modelDB.deleteModelsByIds(modelIdList);
                        hasChange = true;
                     } else if (modelInfo.getModelCondition() != null && !"".equals(modelInfo.getModelCondition())) {
                        FilterParserPlatform oldFilterParser = new FilterParserPlatform(modelInfo.getModelCondition());
                        oldFilterParser.parserFilters();
                        if (!StringUtils.isNullOrEmpry(modelInfo.getModelCondition())
                           || FilterParserPlatform.filterChange(oldFilterParser.getFilters(), newFilterParser.getFilters())) {
                           this.logger.info("the model online condition changed");
                           modelInfo.setModelStatus(2);
                           this.modelDB.deleteModel(modelInfo);
                           modelInfo.setModelCondition(request.getCondition());
                           modelInfo.setFilterRule(request.getCondition());
                           Long modelId = modelInfo.getModelId();
                           modelInfo.setHasChange(0);
                           modelInfo.setModelUptonow(1);
                           modelInfo.setModelStatus(0);
                           long newModelId = this.modelDB.saveModel(modelInfo);
                           this.fragmentDB.updateModelFragment(modelId, newModelId);
                           hasChange = true;
                        }
                     } else {
                        modelInfo.setModelStatus(2);
                        long modelId = modelInfo.getModelId();
                        this.modelDB.deleteModel(modelInfo);
                        modelInfo.setModelCondition(request.getCondition());
                        modelInfo.setFilterRule(request.getCondition());
                        modelInfo.setHasChange(0);
                        modelInfo.setModelUptonow(1);
                        modelInfo.setModelStatus(0);
                        long newModelId = this.modelDB.saveModel(modelInfo);
                        this.logger.info("the model online new modelId:" + newModelId);
                        this.fragmentDB.updateModelFragment(modelId, newModelId);
                        hasChange = true;
                     }

                     if (!hasChange) {
                        if (newFilterParser.isUpToNow()) {
                           modelInfo.setModelUptonow(1);
                        }

                        this.modelDB.saveOrUpdateModel(modelInfo);
                     }

                     this.logger.info("save model online task");
                     this.logger.info("save uptonow task");
                     TaskInfo autoTask = this.createTaskInfo(
                        modelInfo.getModelId(), modelInfo.getDataSource(), modelInfo.getUserName(), 1L, newFilterParser.getFilterStr()
                     );
                     this.taskInfoDB.saveTask(autoTask);
                     ModelTask autoModelTask = new ModelTask();
                     autoModelTask.setModelId(modelInfo.getModelId());
                     autoModelTask.setTaskId(autoTask.getTaskId());
                     this.modelTaskDB.saveModelTask(autoModelTask);
                     autoTask = this.createTaskInfo(
                        modelInfo.getModelId(), modelInfo.getDataSource(), modelInfo.getUserName(), 0L, newFilterParser.getFilterStr()
                     );
                     this.taskInfoDB.saveTask(autoTask);
                     autoModelTask = new ModelTask();
                     autoModelTask.setModelId(modelInfo.getModelId());
                     autoModelTask.setTaskId(autoTask.getTaskId());
                     this.logger.info("save model task");
                     this.modelTaskDB.saveModelTask(autoModelTask);
                     this.logger.info("success online model : " + modelInfo.getModelId());
                     this.serviceResponse.setSuccessful(true);
                     this.serviceResponse.setMessage("上线成功");
                     return this.serviceResponse;
                  }
               } else {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("已上线或正在上线的模型不允许上线");
                  return this.serviceResponse;
               }
            }
         }
      } catch (Exception var16) {
         this.logger.error("上线异常", var16);
         throw new ViePlatformServiceException("内部错误", var16);
      }
   }

   public ServiceResponse saveCondition(ModelRequest request, Long modelId) throws ViePlatformServiceException {
      try {
         this.serviceResponse = new ServiceResponse();
         this.logger.info("start online model");
         ModelInfo modelInfo = this.modelDB.queryModelById(modelId);
         FilterParserPlatform newFilterParser = new FilterParserPlatform(request.getCondition());

         try {
            newFilterParser.parserFiltersOnline();
         } catch (Exception var8) {
            this.logger.error("规则验证错误");
         }

         if (!newFilterParser.isRet()) {
            System.out.println("");
         }

         boolean hasChange = false;
         String choose = request.getChoose();
         if ("true".equals(choose)) {
            modelInfo.setExtend2("1");
         } else {
            modelInfo.setExtend2("0");
         }

         if (modelInfo.getHasChange() == 1) {
            this.logger.info("the model has changed");
            modelInfo.setModelCondition(request.getCondition());
            modelInfo.setFilterRule(request.getCondition());
            this.modelDB.saveOrUpdateModel(modelInfo);
            hasChange = true;
         } else if (modelInfo.getModelCondition() != null && !"".equals(modelInfo.getModelCondition())) {
            FilterParserPlatform oldFilterParser = new FilterParserPlatform(modelInfo.getModelCondition());
            oldFilterParser.parserFilters();
            if (!StringUtils.isNullOrEmpry(modelInfo.getModelCondition())
               || FilterParserPlatform.filterChange(oldFilterParser.getFilters(), newFilterParser.getFilters())) {
               this.logger.info("the model online condition changed");
               modelInfo.setModelCondition(request.getCondition());
               modelInfo.setFilterRule(request.getCondition());
               this.modelDB.saveOrUpdateModel(modelInfo);
               hasChange = true;
            }
         } else {
            modelInfo.setModelCondition(request.getCondition());
            modelInfo.setFilterRule(request.getCondition());
         }

         if (!hasChange) {
            if (newFilterParser.isUpToNow()) {
               modelInfo.setModelUptonow(1);
            }

            this.modelDB.saveOrUpdateModel(modelInfo);
         }

         this.logger.info("success save model : " + modelInfo.getModelId());
         this.serviceResponse.setSuccessful(true);
         this.serviceResponse.setValue(modelId);
         this.serviceResponse.setMessage("保存成功");
         return this.serviceResponse;
      } catch (Exception var9) {
         this.logger.error("保存异常", var9);
         throw new ViePlatformServiceException("内部错误", var9);
      }
   }

   private String getOnlineTimeByCondition(String condition) {
      try {
         List<HashMap<String, Object>> conditionList = (List<HashMap<String, Object>>)this.mapper
            .readValue(condition, new TypeReference<List<HashMap<String, Object>>>() {});

         for (int i = 0; i < conditionList.size(); i++) {
            String key = conditionList.get(i).get("key") + "";
            if ("timestamp".equals(key)) {
               List<String> objs = (List<String>)conditionList.get(i).get("value");
               return objs.get(0);
            }
         }
      } catch (IOException var6) {
         this.logger.error("上线条件解析错误", var6);
      }

      return null;
   }

   public ServiceResponse offlineModelService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start offline model");
         this.serviceResponse = new ServiceResponse();
         if (ListUtil.isNullOrEmpty(request.getModelIds())) {
            this.logger.info("the modelIds is null or empty");
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型ID为空");
            return this.serviceResponse;
         } else {
            List<Long> modelIds = new ArrayList<>();
            List<String> modelNameList = new ArrayList<>();

            for (Long modelId : request.getModelIds()) {
               Object[] modelInfo = this.modelDB.queryModelInfoById(modelId);
               if (modelInfo == null) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("下线的模型不存在");
                  return this.serviceResponse;
               }

               HashMap<String, Object> hm = this.objectArrToMap(modelInfo, "all");
               int modelStatus = Integer.parseInt(String.valueOf(hm.get("modelStatus")));
               if (modelStatus == TaskInfoStatus.OnLineTask.getTaskStatus()) {
                  modelNameList.add(String.valueOf(hm.get("modelName")));
               }

               if (modelStatus != TaskInfoStatus.OptimizeTask.getTaskStatus() && modelStatus != TaskInfoStatus.OffLineTask.getTaskStatus()) {
                  modelIds.add(modelId);
               }
            }

            if (!ListUtil.isNullOrEmpty(modelNameList)) {
               String name = "";

               for (int i = 0; i < modelNameList.size(); i++) {
                  if (i == modelNameList.size() - 1) {
                     name = name + modelNameList.get(i);
                  } else {
                     name = name + modelNameList.get(i) + "、";
                  }
               }

               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("当前模型" + name + "不能进行下线操作");
               return this.serviceResponse;
            } else {
               for (Long modelId : modelIds) {
                  this.logger.info("offline model : " + modelId);
                  this.modelDB.modelOffline(modelId);
                  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                  String time = sdf.format(new Date());
                  this.modelDB.addofflineinfo(modelId, time);
                  this.modelDB.deleteModelConnection(modelId);
               }

               this.serviceResponse.setSuccessful(true);
               this.serviceResponse.setMessage("下线成功");
               return this.serviceResponse;
            }
         }
      } catch (Exception var9) {
         this.logger.error("下线异常", var9);
         throw new ViePlatformServiceException("接口内部错误", var9);
      }
   }

   public HashMap<String, Object> getModelStatusService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start get model status");
         Object[] objects = this.modelDB.queryModelInfoById(request.getModelId());
         this.logger.info("get model status");
         int modelStatuts = TaskInfoStatus.OptimizeTask.getTaskStatus();
         HashMap<String, Object> hm = new HashMap<>();
         if (objects[8] != null) {
            modelStatuts = Integer.parseInt(String.valueOf(objects[8]));
         } else if (objects[14] != null) {
            modelStatuts = TaskInfoStatus.getStatusByObj(objects[14]);
         }

         if (modelStatuts == 1) {
            modelStatuts = TaskInfoStatus.GoLineTask.getTaskStatus();
         } else if (modelStatuts == 0 || modelStatuts == 5) {
            modelStatuts = TaskInfoStatus.OnLineTask.getTaskStatus();
         } else if (modelStatuts > 1 && modelStatuts != 5) {
            hm.put("errorReason", TaskInfoStatus.getTaskStatus(modelStatuts).getErrorReason());
            modelStatuts = TaskInfoStatus.OnLineFail.getTaskStatus();
         }

         this.logger.info("get model hitcount");
         String modelCount = String.valueOf(objects[9]);
         String totalCount = String.valueOf(objects[11]);
         if (StringUtils.isNullOrEmpry(totalCount) || "null".equals(totalCount)) {
            totalCount = "0";
         }

         if (StringUtils.isNullOrEmpry(modelCount) || "null".equals(modelCount)) {
            modelCount = "0";
         }

         String onlineProgress = this.getOnlineProgress(String.valueOf(request.getModelId()), modelStatuts, request.getDataSource(), modelCount);
         String checkRate = "0.0";
         if (!"0".equals(modelCount) && !"0".equals(totalCount) && !"0".equals(totalCount)) {
            checkRate = this.df.format(Double.valueOf(modelCount) / Double.valueOf(totalCount) * 100.0);
         }

         hm.put("modelStatus", modelStatuts);
         hm.put("onlineProgress", onlineProgress);
         hm.put("modelCount", modelCount);
         hm.put("checkRate", checkRate + "%");
         this.logger.info("success get model status");
         return hm;
      } catch (Exception var10) {
         this.logger.error("查询模型状态异常", var10);
         throw new ViePlatformServiceException("接口内部错误", var10);
      }
   }

   public ServiceResponse getModelOnlineProgress(ModelRequest request) throws ViePlatformServiceException {
      ServiceResponse serviceResponse = new ServiceResponse();
      JSONObject resObj = new JSONObject();
      String dataSource = request.getDataSource();
      Long modelId = request.getModelId();

      try {
         String tableName = "";
         String andrillModel = DynamicEsSource.getEsSourceByType(request.getDataSource(), "modelAnydrillAddress");
         String[] andrillModeUrlPort = andrillModel.split(":");
         ExcuteContext excuteContext1 = new ExcuteContext();
         tableName = request.getDataSource();
         String sql = "select count(idCounter) as result from " + tableName;
         ResultSet rs = excuteContext1.executeSearchQuery(andrillModel, sql);
         List<LinkedHashMap<String, Object>> linkedHashMapList = rs.toList();
         long onlineCount = ((Number)linkedHashMapList.get(0).get("result")).longValue();
         ModelInfo modelInfo = this.modelDB.queryModelById(modelId);
         String modelHitCountStr = modelInfo.getModelHitCount();
         BigDecimal bg = new BigDecimal(0.0);
         double totalCount;
         if (StringUtils.isNullOrEmpry(modelHitCountStr)) {
            totalCount = 0.0;
            resObj.put("percent", 0.0);
         } else {
            totalCount = Double.parseDouble(modelHitCountStr);
            bg = new BigDecimal(onlineCount / totalCount).setScale(2, RoundingMode.UP);
            resObj.put("percent", bg.doubleValue());
         }

         resObj.put("remainTime", bg.doubleValue() > 0.99 ? 0L : this.buildRemainingTime(bg.doubleValue(), modelInfo.getUpdateTime(), onlineCount, totalCount));
         serviceResponse.setSuccessful(true);
         serviceResponse.setMessage("查询成功");
         serviceResponse.setValue(resObj);
      } catch (Exception var20) {
         serviceResponse.setSuccessful(false);
         serviceResponse.setMessage(var20.getMessage());
         resObj.put("percent", 0.0);
         resObj.put("remainTime", 0.0);
         serviceResponse.setValue(resObj);
      }

      return serviceResponse;
   }

   private long buildRemainingTime(double completePercent, String updateTimeStr, long onlineCount, double totalCount) {
      try {
         if (onlineCount >= totalCount || totalCount == 0.0) {
            return 0L;
         } else if (completePercent <= 0.1) {
            return (long)(totalCount / 10000.0) * 5L;
         } else {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            long startTime = simpleDateFormat.parse(updateTimeStr).getTime();
            long remainTime = (long)((DateUtils.getCurrentTime() - startTime) * (totalCount - onlineCount) * 1.0 / onlineCount / 1000.0);
            return remainTime < 0L ? -1L : remainTime;
         }
      } catch (Exception var13) {
         this.logger.error(var13.getMessage());
         return 0L;
      }
   }

   public ServiceResponse modelPreviewService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.serviceResponse = new ServiceResponse();
         if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("用户为空");
            return this.serviceResponse;
         } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("数据源为空");
            return this.serviceResponse;
         } else {
            String tableName = request.getDataSource();
            this.logger.info("start preview model");
            ColumnInfoPlatform columnInfo = this.getColumnMaps(request.getColumns());
            long fragmentId = 0L;
            String fragmentContent = "";
            List<ModelFragment> modelFragments = request.getFragments();
            List<ModelFragment> mfOrderList = new ArrayList<>();
            List<ModelFragment> mfList = new ArrayList<>();
            if (request.getSearchType() != 0 && request.getSearchType() != 1) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("搜索方式不在合理范围内");
               return this.serviceResponse;
            } else {
               String checkResult = this.checkDimension(request.getModelDimension());
               if (!"true".equals(checkResult)) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage(checkResult);
                  return this.serviceResponse;
               } else {
                  for (ModelFragment mf : modelFragments) {
                     mfOrderList.add(mf);
                     if (mf.getRuleType() == 2) {
                        fragmentContent = mf.getFragmentContent();
                        fragmentId = mf.getFragmentId();
                     } else {
                        mfList.add(mf);
                        if (mf.getChannel() != 0 && mf.getChannel() != 1 && mf.getChannel() != 2) {
                           this.serviceResponse.setSuccessful(false);
                           this.serviceResponse.setMessage("片段声道不在合理范围内");
                           return this.serviceResponse;
                        }

                        if (mf.getRuleType() != 1) {
                           this.serviceResponse.setSuccessful(false);
                           this.serviceResponse.setMessage("片段类型不在合理范围内");
                           return this.serviceResponse;
                        }
                     }
                  }

                  boolean isHave = modelFragments.size() != 0;
                  if (null != mfList && null != fragmentContent && fragmentContent.contains("@")) {
                     for (ModelFragment mfx : mfList) {
                        if (mfx.getFragmentContent().contains("!") && (mfx.getFragmentContent().contains("|") || mfx.getFragmentContent().contains("&"))) {
                           RuleCheckPlatform.checkNotRule(mfx.getFragmentContent());
                        }
                     }
                  }

                  List<DimensionConfig> dimensionConfigs = this.dimensionDB.searchSystemDim(request.getDataSource(), null);
                  FilterParserPlatform filterParser = this.getFilterParser(request.getModelDimension(), dimensionConfigs);
                  String[] anyDrillHosts = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress").split(";");
                  ModelParserPlatform modelParser;
                  if (null != fragmentContent && fragmentContent.contains("@")) {
                     modelParser = this.getModelParser(mfOrderList, false, fragmentContent);
                  } else {
                     modelParser = this.getModelParser(mfList, false, fragmentContent);
                  }

                  String channelRule = "";
                  if (null != fragmentContent && fragmentContent.contains("@")) {
                     channelRule = this.getChannelRuleParser(mfList);
                  }

                  if (isHave) {
                     ParserResultPlatform ps = modelParser.parse();
                     if (ps.getRet() >= 0) {
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("模型规则验证不通过," + ps.getMsg());
                        return this.serviceResponse;
                     }

                     ps = modelParser.parseReplace();
                     if (ps.getRet() >= 0) {
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("模型规则验证不通过," + ps.getMsg());
                        return this.serviceResponse;
                     }
                  }

                  ModelPreviewMethods mpm = new ModelPreviewMethods();
                  TranslatePlatform translate = new TranslatePlatform();
                  String silenceContent = request.getSilenceRule();
                  String filter = this.dealJyTagContent(silenceContent, false);
                  if (null != filter && !"".equals(filter) && !filter.contains("</filters>")) {
                     this.serviceResponse.setSuccessful(false);
                     this.serviceResponse.setMessage(filter);
                     return this.serviceResponse;
                  } else {
                     if (!StringUtils.isNullOrEmpry(filter)) {
                        translate.translate(filter);
                     }

                     String textFilter = StringUtils.isNullOrEmpry(translate.getTextFilterCondition()) ? "" : translate.getTextFilterCondition();
                     String paseRule = "";
                     if (mfList.size() == 0) {
                        paseRule = "";
                     } else {
                        paseRule = mpm.getPaseRule(false, mfList, null, modelParser.getInputRules(), 2, request.getSearchType());
                     }

                     String dataLimit = this.createDataLimit(request);
                     if ("error".equals(dataLimit)) {
                        return this.serviceResponse;
                     } else {
                        dataLimit = dataLimit + " and ifLone != 1 ";
                        String objectRule = translate.getObjecntRule();
                        String textColumn = translate.getTextColumn();
                        String quaCondition = translate.getQuaCondition();
                        PreviewProcessPlatform previewProcess = new PreviewProcessPlatform(
                           modelParser.getCombineRule(),
                           2,
                           filterParser.getFilters(),
                           tableName,
                           request.getPageNum(),
                           request.getPageSize(),
                           "timestamp",
                           "desc",
                           columnInfo.getColumnsStr().replace("[", "").replace("]", ""),
                           anyDrillHosts,
                           paseRule,
                           dataLimit,
                           request.getSearchType(),
                           objectRule,
                           textColumn,
                           quaCondition,
                           textFilter,
                           dimensionConfigs,
                           channelRule
                        );
                        if (!StringUtils.isNullOrEmpry(filter) || null != fragmentContent && fragmentContent.contains("@")) {
                           if (request.getIsExport() == 1) {
                              previewProcess.processSilenceExport(request.getDataSource());
                           } else {
                              previewProcess.processSilence(request.getDataSource());
                           }
                        } else {
                           previewProcess.process(request.getDataSource());
                        }

                        HashMap<String, Object> retMap = new HashMap<>();
                        ResultTable resultTable = new ResultTable();
                        retMap.put("totalCount", previewProcess.getDimCount());
                        retMap.put("columns", columnInfo.getColumnMaps());
                        int totalCount = previewProcess.getCount();
                        int totalPages = totalCount % request.getPageSize() == 0 ? totalCount / request.getPageSize() : totalCount / request.getPageSize() + 1;
                        this.fillPreviewData(previewProcess.getDataInfos(), fragmentId);
                        this.fillPreviewRemarkData(previewProcess.getDataInfos(), fragmentId);
                        resultTable.setPageNum(request.getPageNum());
                        resultTable.setPageSize(request.getPageSize());
                        resultTable.setTotalRows(totalCount);
                        resultTable.setTotalPages(totalPages);
                        resultTable.setRows(previewProcess.getDataInfos());
                        retMap.put("previewList", resultTable);
                        this.serviceResponse.setSuccessful(true);
                        this.serviceResponse.setValue(retMap);
                        this.logger.info("success preview model");
                        return this.serviceResponse;
                     }
                  }
               }
            }
         }
      } catch (Exception var32) {
         this.logger.error("模型预览异常", var32);
         if (StringUtils.isNotNullAndEmpry(var32.getMessage())) {
            throw new ViePlatformServiceException(var32.getMessage(), var32);
         } else {
            throw new ViePlatformServiceException("接口内部错误", var32);
         }
      }
   }

   private String createDataLimit(ModelRequest request) {
      String dataLimit = "";

      try {
         dataLimit = this.commonData.getDataSql(request.getUserId(), "VIEWEB", request.getDataSource());
         Map<String, Object> filterMap = null;
         if (request.getFilterRuleId() != null && request.getFilterRuleId() != 0) {
            filterMap = this.filterAnaylseService.fetchSqlByFilterId(request.getFilterRuleId());
         } else if (StringUtils.isNotNullAndEmpry(request.getFilter()) && !"{}".equals(request.getFilter())) {
            FilterRequest filterRequest = (FilterRequest)JSON.parseObject(
               request.getFilter(), new com.alibaba.fastjson.TypeReference<FilterRequest>() {}, new Feature[0]
            );
            filterRequest.setDataSource(request.getDataSource());
            filterMap = this.filterAnaylseService.fetchSqlByFilterDimension(filterRequest);
         }

         String filterSql = "";
         if (filterMap != null && (Boolean)filterMap.get("success")) {
            filterSql = (String)filterMap.get("sql");
         } else if (filterMap != null && !(Boolean)filterMap.get("success")) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage((String)filterMap.get("message"));
            this.logger.info("failed preview model");
            return "error";
         }

         dataLimit = dataLimit + (StringUtils.isNotNullAndEmpry(filterSql) ? " and " + filterSql + " " : "");
      } catch (ViePlatformServiceException var5) {
         this.logger.error("failed get dataLimit sql,msg:[" + ExceptionUtil.getStackTrace(var5) + "]");
      }

      return dataLimit;
   }

   public ServiceResponse fragmentPreviewService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.serviceResponse = new ServiceResponse();
         this.logger.info("start preview fragment");
         if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("用户为空");
            return this.serviceResponse;
         } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("数据源为空");
            return this.serviceResponse;
         } else {
            String tableName = request.getDataSource();
            ColumnInfoPlatform columnInfo = this.getColumnMaps(request.getColumns());
            ModelFragment modelFragment = request.getModelFragment();
            if (request.getSearchType() != 1 && request.getSearchType() != 0) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("搜索方式不在合理范围内");
               return this.serviceResponse;
            } else if (modelFragment == null) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("片段信息不能为空");
               return this.serviceResponse;
            } else if (modelFragment.getChannel() != 0 && modelFragment.getChannel() != 1 && modelFragment.getChannel() != 2) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("声道不在合理范围内");
               return this.serviceResponse;
            } else if (modelFragment.getRuleType() != 1) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("片段类型不在合理范围内");
               return this.serviceResponse;
            } else {
               String modelDimension = request.getModelDimension();
               String checkResult = this.checkDimension(modelDimension);
               if (!"true".equals(checkResult)) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage(checkResult);
                  return this.serviceResponse;
               } else {
                  long fragmentId = modelFragment.getFragmentId();
                  String fragmentContent = modelFragment.getFragmentContent();
                  List<ModelFragment> fragments = new ArrayList<>();
                  fragments.add(modelFragment);
                  String[] anyDrillHosts = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress").split(";");
                  List<DimensionConfig> dimensionConfigs = this.dimensionDB.searchSystemDim(request.getDataSource(), null);
                  FilterParserPlatform filterParser = this.getFilterParser(modelDimension, dimensionConfigs);
                  ModelParserPlatform modelParser = this.getModelParser(fragments, true, fragmentContent);
                  ParserResultPlatform parserResult = modelParser.parse();
                  if (parserResult.getRet() >= 0) {
                     this.serviceResponse.setSuccessful(false);
                     this.serviceResponse.setMessage("规则校验错误");
                     return this.serviceResponse;
                  } else {
                     ModelPreviewMethods mpm = new ModelPreviewMethods();
                     String paseRule = mpm.getPaseRule(
                        true, fragments, fragmentContent, modelParser.getInputRules(), modelFragment.getChannel(), request.getSearchType()
                     );
                     String dataLimit = this.createDataLimit(request);
                     if ("error".equals(dataLimit)) {
                        return this.serviceResponse;
                     } else {
                        dataLimit = dataLimit + " and ifLone != 1 ";
                        PreviewProcessPlatform previewProcess = new PreviewProcessPlatform(
                           fragmentContent,
                           modelFragment.getChannel(),
                           filterParser.getFilters(),
                           tableName,
                           request.getPageNum(),
                           request.getPageSize(),
                           "timestamp",
                           "desc",
                           columnInfo.getColumnsStr().replace("[", "").replace("]", ""),
                           anyDrillHosts,
                           paseRule,
                           dataLimit,
                           request.getSearchType(),
                           "",
                           null,
                           null,
                           null,
                           dimensionConfigs,
                           ""
                        );
                        previewProcess.processFragment(request.getDataSource());
                        HashMap<String, Object> retMap = new HashMap<>();
                        ResultTable resultTable = new ResultTable();
                        retMap.put("totalCount", previewProcess.getDimCount());
                        retMap.put("columns", columnInfo.getColumnMaps());
                        int totalCount = previewProcess.getCount();
                        int totalPages = totalCount % request.getPageSize() == 0 ? totalCount / request.getPageSize() : totalCount / request.getPageSize() + 1;
                        this.fillPreviewData(previewProcess.getDataInfos(), fragmentId);
                        this.fillPreviewRemarkData(previewProcess.getDataInfos(), fragmentId);
                        resultTable.setPageNum(request.getPageNum());
                        resultTable.setPageSize(request.getPageSize());
                        resultTable.setTotalRows(totalCount);
                        resultTable.setTotalPages(totalPages);
                        resultTable.setRows(previewProcess.getDataInfos());
                        retMap.put("previewList", resultTable);
                        this.serviceResponse.setSuccessful(true);
                        this.serviceResponse.setValue(retMap);
                        this.logger.info("success preview fragment");
                        return this.serviceResponse;
                     }
                  }
               }
            }
         }
      } catch (Exception var24) {
         this.logger.error("片段预览错误", var24);
         throw new ViePlatformServiceException("接口内部错误", var24);
      }
   }

   public ServiceResponse fragmentCountService(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.serviceResponse = new ServiceResponse();
         this.logger.info("start get fragment hitcount");
         if (StringUtils.isNullOrEmpry(request.getUserId())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("用户ID不能为空");
            return this.serviceResponse;
         } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("数据源为空");
            return this.serviceResponse;
         } else {
            String tableName = request.getDataSource();

            try {
               long userId = Long.parseLong(request.getUserId());
            } catch (Exception var17) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("用户ID类型错误");
               return this.serviceResponse;
            }

            List<Map<String, Long>> result = new ArrayList<>();
            if (request.getModelFragments() != null) {
               List<DimensionConfig> dimensionConfigs = this.dimensionDB.searchSystemDim(request.getDataSource(), null);
               FilterParserPlatform filterParser = this.getFilterParser(request.getModelDimension(), dimensionConfigs);
               List<Map<String, Object>> list = (List<Map<String, Object>>)JSON.parseObject(request.getModelFragments(), List.class);
               if (list != null && list.size() == 0) {
               }

               List<Map<String, Object>> list2 = new ArrayList<>();
               if (null != list && 0 < list.size()) {
                  for (Map<String, Object> fragmentMap : (List<Map<String, Object>>)list) {
                     String textRule = String.valueOf(fragmentMap.get("fragmentContent"));
                     new ModelParserPlatform(null, textRule, null);
                     Long fragmentId = Long.parseLong(fragmentMap.get("fragmentId").toString());
                     ModelFragment fragment = this.fragmentDB.searchModelFragmentById(fragmentId);
                     if (fragment == null) {
                        this.serviceResponse.setSuccessful(false);
                        this.serviceResponse.setMessage("片段不存在");
                        return this.serviceResponse;
                     }

                     ModelFragment mf = new ModelFragment();
                     String ruleText = "";
                     if (Integer.parseInt(fragmentMap.get("isTag").toString()) == 1) {
                        mf.setFragmentContent(fragmentMap.get("fragmentContent").toString());
                        mf.setFragmentId(Integer.parseInt(fragmentMap.get("fragmentId").toString()));
                        mf.setIsTag(Integer.parseInt(fragmentMap.get("isTag").toString()));
                        mf.setFragmentNum(Integer.parseInt(fragmentMap.get("fragmentNum").toString()));
                        mf.setChannel(Integer.parseInt(fragmentMap.get("channel").toString()));
                        mf.setTagContent(fragmentMap.get("tagContent").toString());
                        ruleText = new ModelPreviewMethods().getParseTagContent(mf);
                     }

                     fragmentMap.put("otherRuleText", ruleText);
                     list2.add(fragmentMap);
                  }
               }

               String[] anyDrillHosts = DynamicEsSource.getEsSourceByType(request.getDataSource(), "mainAnydrillAddress").split(";");
               String dataLimit = this.createDataLimit(request);
               if ("error".equals(dataLimit)) {
                  return this.serviceResponse;
               }

               dataLimit = dataLimit + " and ifLone != 1 ";
               SearchHitCountProcess searchHitCountProcess = new SearchHitCountProcess(
                  anyDrillHosts, list2, tableName, 2, filterParser.getFilters(), 300000L, dataLimit, request.getSearchType()
               );

               try {
                  searchHitCountProcess.process(request.getDataSource());
               } catch (ModelRunException var16) {
                  this.logger.error("fragments hit count timeout!");
               }

               result = searchHitCountProcess.getRet();
            }

            this.serviceResponse.setSuccessful(true);
            this.serviceResponse.setMessage("查询成功");
            this.serviceResponse.setValue(result);
            this.logger.info("success get fragment count");
            return this.serviceResponse;
         }
      } catch (Exception var18) {
         this.logger.error("获取片段命中数错误", var18);
         throw new ViePlatformServiceException("接口内部错误", var18);
      }
   }

   public ServiceResponse searchModelInfoList(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start searchModelInfoList");
         this.serviceResponse = new ServiceResponse();
         List<Long> modelIds = request.getModelIds();
         if (ListUtil.isNullOrEmpty(modelIds)) {
            this.logger.info("modelids is null or empty");
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型ID为空");
            return this.serviceResponse;
         } else {
            Map<Long, Map<String, Object>> resultMap = new HashMap<>();
            List<String> modelNameList = new ArrayList<>();

            for (Long modelId : modelIds) {
               List<ModelFragment> modelFragments = this.fragmentDB.queryAllFragment(modelId);
               ModelInfo modelInfo = this.modelDB.queryModelById(modelId);
               if (modelInfo == null) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("模型不存在" + modelId);
                  return this.serviceResponse;
               }

               if (modelInfo.getModelStatus() != 0) {
                  modelNameList.add(modelInfo.getModelName());
               }

               Object[] objArr = this.modelDB.queryModelInfoBySingleId(modelId);
               int modelStatuts = TaskInfoStatus.OptimizeTask.getTaskStatus();
               if (objArr != null) {
                  modelStatuts = Integer.parseInt(String.valueOf(objArr[0]));
                  if (modelStatuts == 1) {
                     modelStatuts = TaskInfoStatus.GoLineTask.getTaskStatus();
                  } else if (modelStatuts == 0 || modelStatuts == 5) {
                     modelStatuts = TaskInfoStatus.OnLineTask.getTaskStatus();
                  } else if (modelStatuts > 1 && modelStatuts != 5) {
                     modelStatuts = TaskInfoStatus.OnLineFail.getTaskStatus();
                  }
               }

               modelInfo.setModelStatus(modelStatuts);
               ModelGroup modelGroup = modelInfo.getModelGroup();
               modelInfo.setModelGroup(null);
               modelGroup.setModelInfoSet(null);
               Map<String, Object> map = new HashMap<>();
               map.put("modelFragments", modelFragments);
               map.put("modelInfo", modelInfo);
               map.put("modelGroup", modelGroup);
               resultMap.put(modelId, map);
            }

            if (!ListUtil.isNullOrEmpty(modelNameList)) {
               String name = "";

               for (int i = 0; i < modelNameList.size(); i++) {
                  if (i == modelNameList.size() - 1) {
                     name = name + modelNameList.get(i);
                  } else {
                     name = name + modelNameList.get(i) + "、";
                  }
               }

               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("当前模型" + name + "不能进行下线操作");
               return this.serviceResponse;
            } else {
               this.serviceResponse.setSuccessful(true);
               this.serviceResponse.setMessage("查询成功");
               this.serviceResponse.setValue(resultMap);
               this.logger.info("success searchModelInfoList");
               return this.serviceResponse;
            }
         }
      } catch (Exception var13) {
         throw new ViePlatformServiceException("接口内部异常", var13);
      }
   }

   public ServiceResponse searchModelCountByName(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search model count by name");
         this.serviceResponse = new ServiceResponse();
         if (StringUtils.isNullOrEmpry(request.getModelName())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型名称为空");
            return this.serviceResponse;
         } else if (StringUtils.isNullOrEmpry(request.getDataSource())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("数据源为空");
            return this.serviceResponse;
         } else if (request.getModelId() == null) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型ID为空");
            return this.serviceResponse;
         } else {
            String tableName = request.getDataSource();
            int count = this.modelDB.isHaveSameModel(request.getModelName(), tableName, request.getModelId());
            this.logger.info("success search model count by name");
            if (count > 0) {
               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("已存在相同的模型名称");
               this.serviceResponse.setValue(count);
               return this.serviceResponse;
            } else {
               this.serviceResponse.setSuccessful(true);
               this.serviceResponse.setMessage("该模型名称可以使用");
               return this.serviceResponse;
            }
         }
      } catch (Exception var4) {
         throw new ViePlatformServiceException("根据模型名称查询数量错误", var4);
      }
   }

   public ServiceResponse searchTagDimension(ModelRequest request) throws ViePlatformServiceException {
      try {
         this.logger.info("start search tagdimension");
         this.serviceResponse = new ServiceResponse();
         if (request.getSearchType() != 0 && request.getSearchType() != 1) {
            throw new ViePlatformServiceException("searchType的值不对");
         } else {
            List<TagDimension> tagDimensions = SilenceTagsCache.tagDimensions.get(request.getSearchType());
            List<HashMap<String, Object>> resultList = new ArrayList<>();

            for (TagDimension td : tagDimensions) {
               HashMap<String, Object> hm = new HashMap<>();
               hm.put("dimensionId", td.getDimensionId());
               hm.put("dimensionCode", td.getDimensionCode());
               hm.put("dimensionName", td.getDimensionName());
               resultList.add(hm);
            }

            this.serviceResponse.setSuccessful(true);
            this.serviceResponse.setMessage("查询成功");
            this.serviceResponse.setValue(resultList);
            this.logger.info("success search tagdimension");
            return this.serviceResponse;
         }
      } catch (Exception var7) {
         this.logger.error("查询标签错误", var7);
         throw new ViePlatformServiceException("查询标签错误", var7);
      }
   }

   public ServiceResponse searchTagProperty(ModelTagRequest tagRequest) throws ViePlatformServiceException {
      try {
         this.logger.info("start search tagproperty");
         this.serviceResponse = new ServiceResponse();
         if (tagRequest.getDimensionId() == null) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("标签ID为空");
            return this.serviceResponse;
         } else if (StringUtils.isNullOrEmpry(tagRequest.getDimensionCode())) {
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("标签CODE为空");
            return this.serviceResponse;
         } else {
            List<TagDimensionProperty> properties = SilenceTagsCache.tagProperties.get(tagRequest.getDimensionId());
            List<HashMap<String, Object>> resultList = new ArrayList<>();

            for (TagDimensionProperty tdp : properties) {
               HashMap<String, Object> hm = new HashMap<>();
               hm.put("propertyId", tdp.getPropertyId());
               hm.put("propertyCode", tdp.getPropertyCode());
               hm.put("propertyName", tdp.getPropertyName());
               hm.put("isDepend", tdp.getIsDepend());
               hm.put("flag", tdp.getFlag());
               resultList.add(hm);
            }

            this.serviceResponse.setSuccessful(true);
            this.serviceResponse.setMessage("查询成功");
            this.serviceResponse.setValue(resultList);
            this.logger.info("success search tagproperty");
            return this.serviceResponse;
         }
      } catch (Exception var7) {
         this.logger.error("查询标签错误", var7);
         throw new ViePlatformServiceException("查询标签错误", var7);
      }
   }

   public ServiceResponse searchTagOperation(ModelTagRequest tagRequest) throws ViePlatformServiceException {
      try {
         this.logger.info("start search tagoperation");
         this.serviceResponse = new ServiceResponse();
         List<TagPropertyOperation> operations = SilenceTagsCache.tagOperations.get(tagRequest.getPropertyId());
         List<HashMap<String, Object>> resultList = new ArrayList<>();

         for (TagPropertyOperation tpo : operations) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("operationId", tpo.getOperationId());
            hm.put("operationCode", tpo.getOperationCode());
            hm.put("operationName", tpo.getOperationName());
            hm.put("type", tpo.getType());
            resultList.add(hm);
         }

         this.serviceResponse.setSuccessful(true);
         this.serviceResponse.setMessage("查询成功");
         this.serviceResponse.setValue(resultList);
         this.logger.info("success search tagopeartion");
         return this.serviceResponse;
      } catch (Exception var7) {
         this.logger.error("查询标签错误", var7);
         throw new ViePlatformServiceException("查询标签错误", var7);
      }
   }

   public ServiceResponse searchCheckModel(ModelRequest request) throws ViePlatformServiceException {
      this.logger.info("start search check model");

      try {
         this.serviceResponse = new ServiceResponse();
         if (ListUtil.isNullOrEmpty(request.getModelIds())) {
            this.logger.info("the modelIds is null or empty");
            this.serviceResponse.setSuccessful(false);
            this.serviceResponse.setMessage("模型ID为空");
            return this.serviceResponse;
         } else {
            List<Long> modelIds = new ArrayList<>();
            List<String> modelNameList = new ArrayList<>();
            List<String> connectionModelNames = new ArrayList<>();

            for (Long modelId : request.getModelIds()) {
               Object[] modelInfo = this.modelDB.queryModelInfoById(modelId);
               if (modelInfo == null) {
                  this.serviceResponse.setSuccessful(false);
                  this.serviceResponse.setMessage("下线的模型不存在");
                  return this.serviceResponse;
               }

               boolean isHave = this.modelDB.searchModelConnection(modelId);
               HashMap<String, Object> hm = this.objectArrToMap(modelInfo, "all");
               if (isHave) {
                  connectionModelNames.add(String.valueOf(hm.get("modelName")));
               }

               int modelStatus = Integer.parseInt(String.valueOf(hm.get("modelStatus")));
               if (modelStatus == 0) {
                  modelNameList.add(String.valueOf(hm.get("modelName")));
               }

               modelIds.add(modelId);
            }

            if (!ListUtil.isNullOrEmpty(connectionModelNames)) {
               String name = "";

               for (int i = 0; i < connectionModelNames.size(); i++) {
                  if (i == connectionModelNames.size() - 1) {
                     name = name + connectionModelNames.get(i);
                  } else {
                     name = name + connectionModelNames.get(i) + "、";
                  }
               }

               this.serviceResponse.setSuccessful(false);
               this.serviceResponse.setMessage("以下模型 " + name + " 于质检项或业务项关联");
               return this.serviceResponse;
            } else {
               this.serviceResponse.setSuccessful(true);
               this.serviceResponse.setMessage("可以正常操作");
               return this.serviceResponse;
            }
         }
      } catch (Exception var11) {
         throw new ViePlatformServiceException("查询模型操作异常", var11);
      }
   }

   public ServiceResponse getModelAccuracy(ModelRequest request) throws ViePlatformServiceException {
      String centerDimension = request.getDataTypeDimension();
      this.logger.info("start getModelAccuracy method");
      String indexAnydrillModelAddress = DynamicEsSource.getEsSourceByType(request.getDataSource(), "modelAnydrillAddress");
      ExcuteContext indexContext = new ExcuteContext();

      try {
         this.serviceResponse = new ServiceResponse();
         String dataSQL = this.commonData.getDataSql(request.getUserId(), "VIEWEB", request.getDataSource());
         if (dataSQL != null && !dataSQL.equals("")) {
            dataSQL = " and " + dataSQL;
         }

         String sql = "";
         String tableName = request.getDataSource();
         String indexTableName = IndexConstants.getModelTableName(tableName);
         this.logger.debug("start time :" + request.getStartTime() + ", end time :" + request.getEndTime());
         String startTimeStr = request.getStartTime();
         String endTimeStr = request.getEndTime();
         String selectCenter = request.getSelectCenter();
         int centerFlag = request.getCenterFlag();
         Long modelId = request.getModelId();
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         Long endTime = sdf.parse(endTimeStr).getTime();
         Long startTime = sdf.parse(startTimeStr).getTime();
         sql = "select count(idCounter) as accuracyNum , dimDay from "
            + indexTableName
            + " where  offLineTagId in ("
            + modelId
            + ")"
            + " and timestamp between "
            + startTime
            + " and "
            + endTime
            + " and  processed = 0 "
            + dataSQL;
         String batchSql = " and " + MultipleDataConstant.getMultipleDataSql(request.getBatchId(), request.getIfLone(), request.getDataType());
         sql = sql + batchSql;
         StringBuffer sqlBuffer = new StringBuffer(sql);
         if (centerFlag == 1) {
            sqlBuffer.append(" and " + centerDimension + " in ('" + selectCenter + "') ");
         }

         sqlBuffer.append(" group by dimDay order by dimDay desc ");
         sql = sqlBuffer.toString();
         this.logger.info("(((### ModelServiceImpl.getModelAccuracy() query model ES sql is))): " + sql);
         this.logger.info("(((### model ES address is))): " + indexAnydrillModelAddress);
         ResultSet rs = indexContext.executeSearchQuery(indexAnydrillModelAddress, sql);
         List<LinkedHashMap<String, Object>> linkedHashMapList = new ArrayList<>();
         if (rs != null) {
            linkedHashMapList = rs.toList();
         }

         LinkedHashMap<String, Object> resultMap = this.dealTimeMethod(linkedHashMapList, startTimeStr, endTimeStr);
         this.logger.info("resultMap:" + resultMap);
         this.serviceResponse.setValue(resultMap);
         this.serviceResponse.setSuccessful(true);
      } catch (Exception var22) {
         throw new ViePlatformServiceException("查询模型操作异常", var22);
      }

      return this.serviceResponse;
   }

   public ServiceResponse getOfflineTagInfo(ModelRequest request) throws ViePlatformServiceException {
      this.logger.info("start getOfflineTagInfo method");
      String indexAnydrillModelAddress = DynamicEsSource.getEsSourceByType(request.getDataSource(), "modelAnydrillAddress");
      ExcuteContext indexContext = new ExcuteContext();

      try {
         this.serviceResponse = new ServiceResponse();
         String voiceId = request.getVoiceId();
         String sql = "";
         String importSql = "";
         String permissonSql = "";
         String tableName = request.getDataSource();
         String indexTableName = IndexConstants.getModelTableName(tableName);
         permissonSql = permissonSql + " and processed = 0" + importSql;
         sql = "select offLineTagId from " + indexTableName + " where voiceId = '" + voiceId + "'" + permissonSql + " order by voiceId asc";
         this.logger.info("getOfflineTagInfo sql start:" + sql);
         ResultSet rs = indexContext.executeSearchQuery(indexAnydrillModelAddress, sql);
         List<LinkedHashMap<String, Object>> linkedList = new ArrayList<>();
         if (rs != null) {
            linkedList = rs.toList();
         }

         this.serviceResponse.setValue(linkedList);
         this.serviceResponse.setSuccessful(true);
      } catch (Exception var12) {
         throw new ViePlatformServiceException("查询OfflineTag操作异常(getOfflineTagInfo)", var12);
      }

      return this.serviceResponse;
   }

   public ServiceResponse searchSilenceProperties(ModelRequest request) throws ViePlatformServiceException {
      this.logger.info("start searchSilenceProperties method");

      try {
         this.serviceResponse = new ServiceResponse();
         List<TagDimensionProperty> properties = this.modelTagDB.getPropertys(0L, null, true);
         HashMap<String, HashMap<String, Object>> resultMap = new HashMap<>();

         for (TagDimensionProperty tdp : properties) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("propertyId", tdp.getPropertyId());
            hm.put("propertyCode", tdp.getPropertyCode());
            hm.put("propertyName", tdp.getPropertyName());
            hm.put("isDepend", tdp.getIsDepend());
            hm.put("flag", tdp.getFlag());
            resultMap.put(tdp.getPropertyName(), hm);
         }

         this.serviceResponse.setSuccessful(true);
         this.serviceResponse.setMessage("查询成功");
         this.serviceResponse.setValue(resultMap);
         this.logger.info("success search tagproperty");
         return this.serviceResponse;
      } catch (Exception var7) {
         this.logger.error("查询静音属性错误", var7);
         throw new ViePlatformServiceException("查询静音属性错误", var7);
      }
   }

   public ServiceResponse searchSilenceOperation(ModelRequest request) throws ViePlatformServiceException {
      this.logger.info("start searchSilenceOperation method");

      try {
         this.serviceResponse = new ServiceResponse();
         List<TagPropertyOperation> operations = this.modelTagDB.getOperations(0L, null, true);
         HashMap<String, HashMap<String, Object>> resultMap = new HashMap<>();

         for (TagPropertyOperation tpo : operations) {
            HashMap<String, Object> hm = new HashMap<>();
            hm.put("operationId", tpo.getOperationId());
            hm.put("operationCode", tpo.getOperationCode());
            hm.put("operationName", tpo.getOperationName());
            hm.put("type", tpo.getType());
            resultMap.put(tpo.getOperationName(), hm);
         }

         this.serviceResponse.setSuccessful(true);
         this.serviceResponse.setMessage("查询成功");
         this.serviceResponse.setValue(resultMap);
         this.logger.info("success search tagopeartion");
         return this.serviceResponse;
      } catch (Exception var7) {
         this.logger.error("查询静音逻辑关系错误", var7);
         throw new ViePlatformServiceException("查询静音逻辑关系错误", var7);
      }
   }

   public LinkedHashMap<String, Object> dealTimeMethod(List<LinkedHashMap<String, Object>> data, String startTime, String endTime) {
      List<String> dayList = new ArrayList<>();
      List<Integer> accuracyList = new ArrayList<>();
      LinkedHashMap<String, Object> xyDataMap = new LinkedHashMap<>();
      LinkedHashMap<String, Object> accuracyMap = new LinkedHashMap<>();
      List<LinkedHashMap<String, Object>> accuracyMapList = new ArrayList<>();

      try {
         Date startDate = this.sdf2.parse(startTime);
         Date endDate = this.sdf2.parse(endTime);
         int days = DateUtils.daysOfTwo(endDate, startDate);
         int d = days / 12 + 1;
         Calendar calendar = Calendar.getInstance();
         calendar.setTime(startDate);
         if (data.size() == 0) {
            while (calendar.getTime().before(endDate)) {
               dayList.add(this.dayFormatMethod(calendar.getTime()));
               accuracyList.add(0);
               calendar.add(5, d);
            }
         }

         for (; calendar.getTime().before(endDate); calendar.add(5, d)) {
            String time = new SimpleDateFormat("yyyyMMdd").format(calendar.getTime());
            int i = 0;

            for (LinkedHashMap<String, Object> dataMap : data) {
               if (dataMap.values().contains(time)) {
                  dayList.add(this.dayFormatMethod(calendar.getTime()));
                  accuracyList.add(Integer.parseInt(dataMap.get("accuracyNum").toString().replace(".0", "")));
                  break;
               }

               if (++i == data.size()) {
                  dayList.add(this.dayFormatMethod(calendar.getTime()));
                  accuracyList.add(0);
               }
            }
         }

         xyDataMap.put("xData", dayList);
         accuracyMap.put("name", "检出数");
         accuracyMap.put("data", accuracyList);
         accuracyMapList.add(accuracyMap);
         xyDataMap.put("yData", accuracyMapList);
      } catch (ParseException var18) {
         var18.printStackTrace();
      }

      return xyDataMap;
   }

   public String dayFormatMethod(Date time) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(time);
      return calendar.get(1) + "年" + (calendar.get(2) + 1) + "月" + calendar.get(5) + "日";
   }

   private String checkFragments(List<ModelFragment> fragments, String choose) {
      List<Long> ids = new ArrayList<>();
      List<Integer> nums = new ArrayList<>();
      String regEx = "[`~@$%^*={}':;',\\[\\].<>/?~！@￥……*（）——{}【】‘；：”“'。，、？]";
      Pattern p = Pattern.compile(regEx);
      String message = "";

      for (ModelFragment mf : fragments) {
         ModelFragment fragment = this.fragmentDB.searchModelFragmentById(mf.getFragmentId());
         if (fragment == null) {
            return "片段不存在";
         }

         if (ids.contains(mf.getFragmentId())) {
            return "存在相同的片段ID";
         }

         if (nums.contains(mf.getFragmentNum())) {
            return "存在相同的片段序号";
         }

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

         if (!StringUtils.isNullOrEmpry(mf.getRemark()) && mf.getRemark().length() > 20) {
            return "片段备注长度不能大于20";
         }

         if (mf.getChannel() != 0 && mf.getChannel() != 1 && mf.getChannel() != 2) {
            return "片段声道不在合理范围内";
         }

         ids.add(mf.getFragmentId());
         nums.add(mf.getFragmentNum());
      }

      return "true";
   }

   private String checkSilenceFragments(List<ModelFragment> fragments) {
      List<Long> ids = new ArrayList<>();
      String message = "";

      for (ModelFragment mf : fragments) {
         ModelFragment fragment = this.fragmentDB.searchModelFragmentById(mf.getFragmentId());
         if (fragment == null) {
            return "片段不存在";
         }

         if (ids.contains(mf.getFragmentId())) {
            return "存在相同的片段ID";
         }

         if (mf.getRuleType() != 1 && mf.getRuleType() != 2) {
            return "片段类型错误";
         }

         ids.add(mf.getFragmentId());
      }

      return "true";
   }

   private String checkOnlineCondition(String onlineCondition) {
      String checkResult = "";

      try {
         for (HashMap<String, Object> hm : (List<HashMap<String, Object>>)this.mapper.readValue(onlineCondition, List.class)) {
            String type = String.valueOf(hm.get("type"));
            if ("timeRange".equals(type)) {
               Integer uptoNow = (Integer)hm.get("uptonow");
               if (uptoNow == null || uptoNow != 0 && uptoNow != 1) {
                  return "是否至今为空或不在合理范围内";
               }

               List<String> timeList = (List<String>)hm.get("value");
               if (uptoNow == 1) {
                  String start = timeList.get(0);
                  if (StringUtils.isNullOrEmpry(start)) {
                     return "开始时间为空";
                  }

                  try {
                     Date startTime = this.sdf.parse(start);
                  } catch (Exception var15) {
                     return "开始时间格式不对";
                  }
               } else {
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
                  } catch (Exception var14) {
                     return "时间格式不对";
                  }

                  if (startTime.getTime() > endTime.getTime()) {
                     return "开始时间大于结束时间";
                  }
               }
            }
         }
      } catch (Exception var16) {
         return "条件校验不通过，条件类型不在合理的范围内";
      }

      return "true";
   }

   private String checkDimension(String dimensionCondition) {
      String checkResult = "";

      try {
         for (HashMap<String, Object> hm : (List<HashMap<String, Object>>)this.mapper.readValue(dimensionCondition, List.class)) {
            String type = String.valueOf(hm.get("type"));
            String value = String.valueOf(hm.get("value"));
            String dataType = String.valueOf(hm.get("dataType"));
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
                     } catch (Exception var16) {
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
                  } catch (Exception var15) {
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
      } catch (Exception var17) {
         this.logger.error("校验出错", var17);
      }

      return checkResult;
   }

   private void fillModelBaseInfo(ModelDetailDTO modelDetailDTO, ModelInfo modelInfo) {
      modelDetailDTO.setModelId(modelInfo.getModelId());
      modelDetailDTO.setModelName(modelInfo.getModelName());
      modelDetailDTO.setModelComment(modelInfo.getModelComment());
      modelDetailDTO.setGroupId(modelInfo.getModelGroup().getGroupId());
      modelDetailDTO.setGroupName(modelInfo.getModelGroup().getGroupName());
      modelDetailDTO.setModelDimensions(modelInfo.getModelCondition());
      modelDetailDTO.setSilenceText(modelInfo.getSilenceText());
      modelDetailDTO.setChannel(modelInfo.getChannel());
      modelDetailDTO.setModelAccuracy(modelInfo.getModelAccuracy());
      modelDetailDTO.setFilterId(modelInfo.getFilterId() != null ? modelInfo.getFilterId() : 0);
      String tagRule = modelInfo.getSilenceRule();
      if (!StringUtils.isNullOrEmpry(tagRule)) {
         TagRuleHandler handler = new TagRuleHandler(tagRule);
         List<LinkedHashMap<String, Object>> lhm = handler.getFilterMap();

         try {
            String silenceContent = this.mapper.writeValueAsString(lhm);
            modelDetailDTO.setSilenceRule(silenceContent);
         } catch (JsonProcessingException var7) {
            var7.printStackTrace();
         }
      }
   }

   private void fillModelFragments(ModelDetailDTO modelDetailDTO, List<ModelFragment> modelFragmentList) {
      if (modelFragmentList != null && modelFragmentList.size() != 0) {
         List<ModelFragment> finalModelFragments = new ArrayList<>();

         for (ModelFragment modelFragment : modelFragmentList) {
            if (modelFragment.getRuleType() == 2) {
               modelDetailDTO.setModelFragmentRelation(modelFragment);
            } else {
               if (modelFragment.getRemark() == null) {
                  modelFragment.setRemark("");
               }

               finalModelFragments.add(modelFragment);
            }
         }

         modelDetailDTO.setModelFragments(finalModelFragments);
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

   private boolean isHaveSameModel(String modelName, String dataSource, long modelId, long modelGroupId, int topicId) {
      int count = 0;

      try {
         count = this.modelDB.isHaveSameModelByModelId(modelName, dataSource, modelId);
      } catch (Exception var10) {
         return true;
      }

      return count != 0;
   }

   private void fillPreviewData(List<DataInfoPlatform> dataInfos, long fragmentId) {
      if (dataInfos != null && dataInfos.size() != 0) {
         List<DataMarkInfo> dataMarkInfos = this.dataMarkInfoDB.queryAllDataInfos(fragmentId);
         Map<String, DataMarkInfo> map = new HashMap<>();
         if (dataMarkInfos != null && dataMarkInfos.size() != 0) {
            for (DataMarkInfo dataMarkInfo : dataMarkInfos) {
               map.put(dataMarkInfo.getTelId(), dataMarkInfo);
            }
         }

         for (DataInfoPlatform dataInfo : dataInfos) {
            dataInfo.setFragmentId(fragmentId);
            if (map.containsKey(dataInfo.getId())) {
               dataInfo.setDataMarkId(map.get(dataInfo.getId()).getId());
               dataInfo.setMark(map.get(dataInfo.getId()).getMark());
            }
         }
      }
   }

   private void fillPreviewRemarkData(List<DataInfoPlatform> dataInfos, long fragmentId) {
      if (dataInfos != null && dataInfos.size() != 0) {
         List<DataRemarkInfo> dataRemarkInfos = this.dataRemarkInfoDB.queryAllDataRemarks(fragmentId);
         Map<String, DataRemarkInfo> map = new HashMap<>();
         if (dataRemarkInfos != null && dataRemarkInfos.size() != 0) {
            for (DataRemarkInfo dataRemarkInfo : dataRemarkInfos) {
               map.put(dataRemarkInfo.getTelId(), dataRemarkInfo);
            }
         }

         for (DataInfoPlatform dataInfo : dataInfos) {
            dataInfo.setFragmentId(fragmentId);
            if (map.containsKey(dataInfo.getId())) {
               dataInfo.setDataRemarkId(map.get(dataInfo.getId()).getId());
               dataInfo.setRemarked(1);
               dataInfo.setRemark(map.get(dataInfo.getId()).getRemark());
            }
         }
      }
   }

   private ModelParserPlatform getModelParser(List<ModelFragment> modelFragmentList, boolean isFragment, String fragmentContent) {
      ModelParserPlatform modelParser;
      if (!isFragment) {
         modelParser = new ModelParserPlatform(null, fragmentContent, this.getModelFragmentHm(modelFragmentList));
      } else {
         modelParser = new ModelParserPlatform(null, fragmentContent, null);
         modelParser.setReplace(false);
      }

      return modelParser;
   }

   private String dealJyTagContent(String tagContentRule, boolean isFragment) {
      if (!isFragment && !StringUtils.isNullOrEmpry(tagContentRule)) {
         ObjectMapper mapper = new ObjectMapper();
         StringBuffer filter = new StringBuffer();
         List<HashMap<String, Object>> list = null;

         try {
            list = (List<HashMap<String, Object>>)mapper.readValue(tagContentRule, List.class);
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
         } catch (IOException var19) {
            this.logger.error("处理静音标签错误", var19);
            return "处理静音标签错误";
         }

         return filter.toString();
      } else {
         return "";
      }
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

   private FilterParserPlatform getFilterParser(String modelDimensions, List<DimensionConfig> dimensionConfigs) throws ParseException {
      modelDimensions = this.filterTime(modelDimensions, dimensionConfigs);
      FilterParserPlatform filterParser = new FilterParserPlatform(modelDimensions);
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

   private ColumnInfoPlatform getColumnMaps(String columns) {
      ColumnInfoPlatform columnInfo = new ColumnInfoPlatform();
      if (columns != null && !"".equals(columns)) {
         List list = (List)JSON.parseObject(columns, List.class);
         if (list != null && list.size() != 0) {
            for (int i = 0; i < list.size(); i++) {
               JSONObject jsonObject = (JSONObject)list.get(i);
               ColumnMapPlatform columnMap = new ColumnMapPlatform(jsonObject.getString("columnName"), jsonObject.getString("column"));
               columnInfo.getColumnMaps().add(columnMap);
               columnInfo.getColumns().add(columnMap.getColumn());
            }
         }
      }

      columnInfo.getColumnMaps().add(new ColumnMapPlatform("匹配规则", "keyword"));
      columnInfo.getColumnMaps().add(new ColumnMapPlatform("人工测听标记", "mark"));
      columnInfo.getColumnMaps().add(new ColumnMapPlatform("备注", "remark"));
      columnInfo.setColumnsStr(columnInfo.getColumns().toString());
      return columnInfo;
   }

   private TaskInfo createTaskInfo(Long id, String dataSource, String userName, long runSchema, String filterStr) {
      TaskInfo taskInfo = new TaskInfo();
      String taskName2 = UUID.randomUUID().toString().replace("-", "");
      taskInfo.setTaskName(taskName2);
      taskInfo.setUserName(userName);
      taskInfo.setTaskType(1L);
      taskInfo.setTaskStatus(0L);
      taskInfo.setDataSource(dataSource);
      taskInfo.setCreateTime(DateUtils.parseDateToString(new Date(), "yyyy-MM-dd"));
      taskInfo.setRunSchema(runSchema);
      taskInfo.setFilterType(1L);
      String operateContent2 = "<operations><operation type='03' value='" + id + "'></operation></operations>";
      taskInfo.setOperateContent(operateContent2);
      taskInfo.setFilterContent(filterStr);
      return taskInfo;
   }

   private String dealShowFilterRow(String cond, String topicName) {
      String result = "";

      try {
         ObjectMapper mapper = new ObjectMapper();
         List<HashMap<String, Object>> reportParam = (List<HashMap<String, Object>>)mapper.readValue(cond, List.class);

         for (HashMap<String, Object> current : (List<HashMap<String, Object>>)reportParam) {
            String key = current.get("key").toString();
            if ("timestamp".equals(key)) {
               if (StringUtils.isNullOrEmpry(topicName)) {
                  current.put("name", "时间");
               } else {
                  current.put("name", "时间");
               }
               break;
            }
         }

         result = mapper.writeValueAsString(reportParam);
      } catch (Exception var9) {
         var9.printStackTrace();
      }

      return result;
   }

   private List<ModelFragment> rebuildModelFragmentList(long modelId, List<ModelFragment> modelFragmentList, long previewId) {
      List<ModelFragment> mfList = new ArrayList<>();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      Calendar calendar = Calendar.getInstance();
      Date date = new Date();
      calendar.setTime(date);
      long timeStamp = calendar.getTimeInMillis();

      for (ModelFragment mf : modelFragmentList) {
         mf.setModelId(modelId);
         mf.setPreviewId(previewId);
         mf.setCreateTime(sdf.format(date));
         mf.setCreateTimestamp(timeStamp);
         mfList.add(mf);
      }

      return mfList;
   }

   private boolean isModelFragment(String modelFragments) {
      return modelFragments == null || "".equals(modelFragments) || "[]".equals(modelFragments);
   }

   private List<Long> getFragmentId(List<ModelFragment> fragments) {
      List<Long> ids = new ArrayList<>();

      for (ModelFragment mf : fragments) {
         ids.add(mf.getFragmentId());
      }

      return ids;
   }

   private List<Long> getDeleteFramentIds(List<Long> oldFragmentIds, List<Long> newFragmentIds) {
      List<Long> deleteIds = new ArrayList<>();

      for (int i = 0; i < oldFragmentIds.size(); i++) {
         long id = oldFragmentIds.get(i);
         if (!newFragmentIds.contains(id)) {
            deleteIds.add(id);
         }
      }

      return deleteIds;
   }

   private List<ModelFragment> getUpdateFragments(List<ModelFragment> oldFramgents, List<ModelFragment> newFragments) {
      List<ModelFragment> result = new ArrayList<>();

      for (int i = 0; i < oldFramgents.size(); i++) {
         ModelFragment frag = oldFramgents.get(i);
         long fragmentId = frag.getFragmentId();

         for (int j = 0; j < newFragments.size(); j++) {
            ModelFragment mf = newFragments.get(j);
            if (mf.getFragmentId() == fragmentId) {
               frag.setChannel(mf.getChannel());
               frag.setFragmentNum(mf.getFragmentNum());
               frag.setTagContent(mf.getTagContent());
               frag.setIsTag(mf.getIsTag());
               frag.setFragmentContent(mf.getFragmentContent());
               frag.setRemark(mf.getRemark());
               frag.setTagText(mf.getTagText());
               frag.setTagVersion(mf.getTagVersion());
               frag.setModelId(mf.getModelId());
               frag.setRuleType(mf.getRuleType());
               result.add(frag);
            }
         }
      }

      return result;
   }

   public void setFilterInfoDB(FilterInfoDB filterInfoDB) {
      this.filterInfoDB = filterInfoDB;
   }

   public ModelDBPlatform getModelDB() {
      return this.modelDB;
   }

   public void setModelDB(ModelDBPlatform modelDB) {
      this.modelDB = modelDB;
   }

   public ModelFragmentDB getFragmentDB() {
      return this.fragmentDB;
   }

   public void setFragmentDB(ModelFragmentDB fragmentDB) {
      this.fragmentDB = fragmentDB;
   }

   public ModelGroupDBPlatform getModelGroupDB() {
      return this.modelGroupDB;
   }

   public void setModelGroupDB(ModelGroupDBPlatform modelGroupDB) {
      this.modelGroupDB = modelGroupDB;
   }

   public DataMarkInfoDB getDataMarkInfoDB() {
      return this.dataMarkInfoDB;
   }

   public void setDataMarkInfoDB(DataMarkInfoDB dataMarkInfoDB) {
      this.dataMarkInfoDB = dataMarkInfoDB;
   }

   public DataRemarkInfoDB getDataRemarkInfoDB() {
      return this.dataRemarkInfoDB;
   }

   public void setDataRemarkInfoDB(DataRemarkInfoDB dataRemarkInfoDB) {
      this.dataRemarkInfoDB = dataRemarkInfoDB;
   }

   public TaskInfoDB getTaskInfoDB() {
      return this.taskInfoDB;
   }

   public void setTaskInfoDB(TaskInfoDB taskInfoDB) {
      this.taskInfoDB = taskInfoDB;
   }

   public ModelTaskDB getModelTaskDB() {
      return this.modelTaskDB;
   }

   public void setModelTaskDB(ModelTaskDB modelTaskDB) {
      this.modelTaskDB = modelTaskDB;
   }

   public ExcuteContext getExcuteContext() {
      return this.excuteContext;
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
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

   public CommonDataPlatform getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonDataPlatform commonData) {
      this.commonData = commonData;
   }

   public ModelTagDB getModelTagDB() {
      return this.modelTagDB;
   }

   public void setModelTagDB(ModelTagDB modelTagDB) {
      this.modelTagDB = modelTagDB;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public String getDataTypeDimension() {
      return this.dataTypeDimension;
   }

   public void setDataTypeDimension(String dataTypeDimension) {
      this.dataTypeDimension = dataTypeDimension;
   }

   public void setCollectTaskDB(DataCollectTaskDB collectTaskDB) {
      this.collectTaskDB = collectTaskDB;
   }

   public DimensionDBPlatform getDimensionDB() {
      return this.dimensionDB;
   }

   public void setDimensionDB(DimensionDBPlatform dimensionDB) {
      this.dimensionDB = dimensionDB;
   }

   public BatchDB getBatchInfoDB() {
      return this.batchInfoDB;
   }

   public void setBatchInfoDB(BatchDB batchInfoDB) {
      this.batchInfoDB = batchInfoDB;
   }

   public void setFilterAnaylseService(FilterAnaylseService filterAnaylseService) {
      this.filterAnaylseService = filterAnaylseService;
   }
}
