package com.iflytek.vie.app.provider.impl.player;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.api.player.PlayerService;
import com.iflytek.vie.app.api.player.server.wave.objects.InitialiseWaveFormat;
import com.iflytek.vie.app.api.player.server.wave.objects.WaveFormat;
import com.iflytek.vie.app.api.player.server.wave.objects.WaveFormat.Builder;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.datadrill.DataDetailRequest;
import com.iflytek.vie.app.pojo.datadrill.TaskDetailsResponse;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.pojo.player.ContactKwContext;
import com.iflytek.vie.app.pojo.player.ContantKwRequest;
import com.iflytek.vie.app.pojo.player.DialogTextResponse;
import com.iflytek.vie.app.pojo.player.ModelKeyWordRequest;
import com.iflytek.vie.app.pojo.player.PlayerDataRequest;
import com.iflytek.vie.app.pojo.player.RuleInfo;
import com.iflytek.vie.app.pojo.player.TagInfo;
import com.iflytek.vie.app.pojo.player.VoiceBaseInfo;
import com.iflytek.vie.app.provider.common.CommonFunPlatform;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.ModelInfoDBPlatform;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.RestUtil;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.anydrill.calculate.set.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerServiceImpl implements PlayerService {
   private final Logger logger = LoggerFactory.getLogger(PlayerServiceImpl.class);
   private static final int WAV_HEADER_LENGTH = 44;
   public static final String OFFLINETAGID = "offLineTagId";
   public static final String OFFLINETAGINFO = "offLineTagInfo";
   public static final String OFFLINETAGNAME = "offLineTagName";
   public static final String CONTENT_ORIGIN = "contentOrigin";
   public static final String TIME_POSITION = "timePosition";
   public static final String CONTENT = "content";
   public static final String DURATION = "duration";
   public static final String DATA_TYPE = "dataType";
   public static final String ANYDRILL_CHILDFILEDS = "child_fields";
   public static final String CHILDDURATION = "childDuration";
   public static final String VOICE_ID = "voiceId";
   public static final String CHILD_CONTENT_LENGTH = "childContentLength";
   private int blank_font_size;
   private ModelInfoDBPlatform modelInfoDB;
   private ExcuteContext excuteContext;
   private GroupAuthService groupService;
   private DataSourceInfo dataSourceInfo;

   public VoiceBaseInfo getFullAudioInfoService(PlayerDataRequest request) throws ViePlatformServiceException {
      this.logger.info("### 语音信息服务接口getFullAudioInfoService被调用 ###");
      VoiceBaseInfo resultInfo = null;
      ObjectMapper mapper = new ObjectMapper();

      try {
         if (request == null) {
            return null;
         }

         AudioInfo audioInfo = new AudioInfo(request.getContentOrigin(), request.getChannelSeq(), request.getTimePosition());
         audioInfo.process();
         this.logger.info("voice service path :" + this.getVoiceServerPath(request.getMacTag()));
         RestUtil restUtil = new RestUtil();
         Hashtable<String, Object> paramsInfo = new Hashtable<>();
         String voicePath = request.getVoiceUrl().replace("|$|", "#");
         paramsInfo.put("voicePath", voicePath);
         String regx = "[0-9]+.[0-9]+.[0-9]+.[0-9]+:[0-9]+";
         Pattern pattern = Pattern.compile(regx);
         Matcher matcher = pattern.matcher(voicePath);
         boolean isV3Player = false;
         paramsInfo.put("macTag", request.getMacTag());
         resultInfo = new VoiceBaseInfo();
         if (!isV3Player) {
            String par = mapper.writeValueAsString(paramsInfo);
            String strVoiceLength = restUtil.postRequst(this.getVoiceServerPath(request.getMacTag()) + "GetVoiceLength", par);
            if (strVoiceLength != null && !strVoiceLength.equals("")) {
               int voiceLength = Integer.parseInt(strVoiceLength);
               resultInfo.setVoiceLength(voiceLength);
            }
         }

         resultInfo.setOneBest(audioInfo.getOneBest());
         resultInfo.setCurrentIndex(audioInfo.getCurrentIndex());
         resultInfo.setVoiceIndex(audioInfo.getVoiceIndex());
         resultInfo.setTimePositionStr(audioInfo.getTimePositionStr());
         resultInfo.setTrueOnebestTime(audioInfo.getTrueOnebestTime());
      } catch (Exception var15) {
         this.logger.error("getFullAudioInfoService service has a error!");
         throw new ViePlatformServiceException("服务内部错误", var15);
      }

      this.logger.info("### service is ending! ###");
      return resultInfo;
   }

   public String getSlicedStreamService(PlayerDataRequest request) throws ViePlatformServiceException {
      this.logger.info("### 语音信息服务接口getSlicedStreamService被调用 ###");
      String resultStr = "";
      ObjectMapper mapper = new ObjectMapper();
      if (request != null && !StringUtils.isNullOrEmpry(request.getVoiceUrl()) && !StringUtils.isNullOrEmpry(request.getMacTag()) && request.getCount() != 0) {
         try {
            this.logger.info("voice service path :" + this.getVoiceServerPath(request.getMacTag()));
            Hashtable<String, Object> paramsInfo = new Hashtable<>();
            paramsInfo.put("voicePath", request.getVoiceUrl().replace("|$|", "#"));
            paramsInfo.put("macTag", request.getMacTag());
            paramsInfo.put("offset", request.getOffset());
            int countSum = Math.min(request.getCount(), 102400);
            paramsInfo.put("count", countSum);
            this.logger.info("count:" + countSum + ", offset:" + request.getOffset());
            String par = mapper.writeValueAsString(paramsInfo);
            RestUtil restUtil = new RestUtil();
            String data = restUtil.postRequst(this.getVoiceServerPath(request.getMacTag()) + "GetVoiceVgsInfo ", par);
            if (StringUtils.isNullOrEmpry(data)) {
               throw new ViePlatformServiceException("录音不存在!");
            }

            resultStr = data;
         } catch (ViePlatformServiceException var9) {
            this.logger.error("录音不存在!");
            throw new ViePlatformServiceException("录音不存在!");
         } catch (Exception var10) {
            this.logger.error("getSlicedStreamService service has a error!");
            throw new ViePlatformServiceException("服务内部错误", var10);
         }

         this.logger.info("### service is ending! ###");
         return resultStr;
      } else {
         this.logger.error("### getSlicedStreamService参数不能为空 ###");
         throw new ViePlatformServiceException("request请求参数不能为空");
      }
   }

   public InitialiseWaveFormat getVoiceFormatService(PlayerDataRequest request) throws ViePlatformServiceException {
      this.logger.info("### 语音信息服务接口getVoiceFormatService被调用 ###");
      ObjectMapper mapper = new ObjectMapper();
      InitialiseWaveFormat iwf = null;
      if (request != null && !StringUtils.isNullOrEmpry(request.getVoiceUrl()) && !StringUtils.isNullOrEmpry(request.getMacTag())) {
         try {
            Hashtable<String, Object> paramInfo = new Hashtable<>();
            String voiceUrl = request.getVoiceUrl();
            voiceUrl = voiceUrl.replace("|$|", "#");
            paramInfo.put("count", 44);
            paramInfo.put("voicePath", voiceUrl);
            paramInfo.put("macTag", request.getMacTag());
            String par = mapper.writeValueAsString(paramInfo);
            RestUtil restUtil = new RestUtil();
            this.logger.info("voice service path :" + this.getVoiceServerPath(request.getMacTag()));
            String voiceHeaderData = restUtil.postRequst(this.getVoiceServerPath(request.getMacTag()) + "GetVoiceHeader", par);
            if (!StringUtils.isNullOrEmpry(voiceHeaderData)) {
               if ("-1".equals(voiceHeaderData)) {
                  throw new ViePlatformServiceException("录音不存在");
               }

               Map<String, Object> vgsData = (Map<String, Object>)mapper.readValue(voiceHeaderData, Map.class);
               Builder builder = this.getWareFormatBuilder(vgsData);
               iwf = this.waveFormat(builder);
            }
         } catch (ViePlatformServiceException var11) {
            this.logger.error("录音不存在!");
            throw new ViePlatformServiceException("录音不存在!", var11);
         } catch (Exception var12) {
            this.logger.error("getVoiceFormatService service has a error!");
            throw new ViePlatformServiceException("服务内部错误", var12);
         }

         this.logger.info("### service is ending! ###");
         return iwf;
      } else {
         throw new ViePlatformServiceException("请求参数不能为空");
      }
   }

   public String getAudioGramService(PlayerDataRequest request) throws ViePlatformServiceException {
      this.logger.info("### 语音信息服务接口getAudioGramService被调用 ###");
      String resultStr = "";
      ObjectMapper mapper = new ObjectMapper();
      if (request == null) {
         throw new ViePlatformServiceException("PlayerDataRequest 参数不能为空。");
      } else if (request.getStartSample() > request.getEndSample()) {
         throw new ViePlatformServiceException("startSample必须小于endSample 。");
      } else {
         try {
            Hashtable<String, Object> paramInfo = new Hashtable<>();
            paramInfo.put("voicePath", request.getVoiceUrl().replace("|$|", "#"));
            paramInfo.put("macTag", request.getMacTag());
            paramInfo.put("channel", request.getChannel());
            paramInfo.put("offset", request.getStartSample());
            paramInfo.put("endsample", request.getEndSample());
            paramInfo.put("blocksize", request.getBlockSize());
            String par = mapper.writeValueAsString(paramInfo);
            RestUtil restUtil = new RestUtil();
            this.logger.info("#######channel-data####:" + par);
            String voiceHeaderData = restUtil.postRequst(this.getVoiceServerPath(request.getMacTag()) + "GetGram", par);
            if (StringUtils.isNullOrEmpry(voiceHeaderData)) {
               throw new ViePlatformServiceException("录音不存在!");
            }

            resultStr = voiceHeaderData;
         } catch (ViePlatformServiceException var8) {
            this.logger.error("录音不存在!");
            throw new ViePlatformServiceException("录音不存在!");
         } catch (Exception var9) {
            this.logger.error("getAudioGramService service has a error!");
            throw new ViePlatformServiceException("服务内部错误", var9);
         }

         this.logger.info("### service is ending! ###");
         return resultStr;
      }
   }

   public List<List<DialogTextResponse>> getFullTextInfoService_ByVoice(PlayerDataRequest request) throws ViePlatformServiceException {
      this.logger.info("### 语音信息服务接口getFullTextInfoService被调用 ###");
      List<DialogTextResponse> dialogList = null;
      List<List<DialogTextResponse>> dialogListWholeTask = new ArrayList<>();
      if (request == null) {
         throw new ViePlatformServiceException("request参数不能为空");
      } else if (request.getContentOrigin() == null || request.getContentOrigin().length() == 0) {
         throw new ViePlatformServiceException("contentOrigin参数不能为空");
      } else if (request.getChannelSeq() == null || request.getChannelSeq().length() == 0) {
         throw new ViePlatformServiceException("channelSeq参数不能为空");
      } else if (request.getTimePosition() != null && request.getTimePosition().length() != 0) {
         try {
            AudioInfo audioInfo = new AudioInfo();
            dialogList = audioInfo.fetchTextInfo_ByVoice(request.getContentOrigin(), request.getChannelSeq(), request.getTimePosition());
            dialogListWholeTask.add(dialogList);
         } catch (Exception var5) {
            this.logger.error("getFullTextInfoService service has a error!");
            throw new ViePlatformServiceException("服务内部错误", var5);
         }

         this.logger.info("### service is ending! ###");
         return dialogListWholeTask;
      } else {
         throw new ViePlatformServiceException("timePosition参数不能为空");
      }
   }

   public List<List<DialogTextResponse>> getFullTextInfoService_ByTask(PlayerDataRequest request) throws ViePlatformServiceException {
      this.logger.info("### 语音信息服务接口getFullTextInfoService被调用 ###");
      List<DialogTextResponse> dialogList = null;
      new ArrayList();
      List<List<DialogTextResponse>> dialogListWholeTask2 = new ArrayList<>();
      List<HashMap> ContainDialogListAndTimeFormat = new ArrayList<>();
      if (request == null) {
         throw new ViePlatformServiceException("request参数不能为空");
      } else if (request.getContentOrigin() == null || request.getContentOrigin().length() == 0) {
         throw new ViePlatformServiceException("contentOrigin参数不能为空");
      } else if (request.getChannelSeq() == null || request.getChannelSeq().length() == 0) {
         throw new ViePlatformServiceException("channelSeq参数不能为空");
      } else if (request.getTimePosition() == null || request.getTimePosition().length() == 0) {
         throw new ViePlatformServiceException("timePosition参数不能为空");
      } else if (request.getChildVoiceIdList() != null && !request.getChildVoiceIdList().isEmpty()) {
         try {
            AudioInfo audioInfo = new AudioInfo();
            String[] singleContent = request.getContentOrigin().split("\\$");
            String[] singleChannelSeq = request.getChannelSeq().split("\\$");
            String[] singleTimePosition = request.getTimePosition().split("\\$");
            if (request.getChildVoiceIdList().size() != singleChannelSeq.length
               || singleChannelSeq.length != singleContent.length
               || singleContent.length != singleTimePosition.length
               || singleChannelSeq.length != singleTimePosition.length) {
               this.logger.error("任务的Content、ChannelSeq、TimePosition有问题");
               throw new ViePlatformServiceException("任务的Content、ChannelSeq、TimePosition有问题");
            }

            String childVoiceId = "";
            String childTimeFormat = "";

            for (int i = 0; i < singleContent.length; i++) {
               HashMap<String, Object> hm = new HashMap<>();
               childVoiceId = String.valueOf(((HashMap)request.getChildVoiceIdList().get(i)).get("childVoiceId"));
               childTimeFormat = String.valueOf(((HashMap)request.getChildVoiceIdList().get(i)).get("childTimeFormat"));
               dialogList = audioInfo.fetchTextInfo_ByTask(singleContent[i], singleChannelSeq[i], singleTimePosition[i], childVoiceId);
               hm.put("dialogList", dialogList);
               hm.put("childTimeFormat", childTimeFormat);
               ContainDialogListAndTimeFormat.add(hm);
            }

            Collections.sort(ContainDialogListAndTimeFormat, new SortByChildTimeFormat());

            for (int i = 0; i < singleContent.length; i++) {
               List<DialogTextResponse> dialogList2 = (List<DialogTextResponse>)ContainDialogListAndTimeFormat.get(i).get("dialogList");
               dialogListWholeTask2.add(dialogList2);
            }
         } catch (Exception var14) {
            this.logger.error("getFullTextInfoService service has a error!");
            throw new ViePlatformServiceException("服务内部错误", var14);
         }

         this.logger.info("### service is ending! ###");
         return dialogListWholeTask2;
      } else {
         throw new ViePlatformServiceException("ChildVoiceIdList参数不能为空");
      }
   }

   public List<RuleInfo> getOnlineModelKeyWordService_ByVoice(ModelKeyWordRequest keyWordRequest) throws ViePlatformServiceException {
      this.logger.info("### 语音信息服务接口getOnlineModelKeyWordService被调用 ###");
      List<RuleInfo> ruleMap = new ArrayList<>();
      if (keyWordRequest != null && keyWordRequest.getCallId().length() != 0 && keyWordRequest.getDataSource().length() != 0) {
         try {
            if (keyWordRequest.getIsLoad() == 0) {
               this.logger.info("### not offline play");
            } else {
               this.logger.info("### is offline play");
            }

            String mainSelectColu = "duration,dataType,timePosition,content";
            String indexSelectColu = "offLineTagId,offLineTagInfo,offLineTagName";
            String dataSource = keyWordRequest.getDataSource();
            String sql = "select " + mainSelectColu + " from " + dataSource + " where id='" + keyWordRequest.getCallId() + "'";
            this.logger.info("##search sql: " + sql);
            String content = "";
            String timePosition = "";
            String duration = "";
            List<Long> groupId = null;
            int dataType = 1;
            List<String> timeList = new ArrayList<>();
            List<Integer> positionList = new ArrayList<>();
            List<String> modelNameList = new ArrayList<>();
            String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
            ResultSet rs = this.excuteContext.executeSearchQuery(url, sql);
            if (rs != null && rs.getTotalCount() > 0L) {
               LinkedHashMap<String, Object> currentData = (LinkedHashMap<String, Object>)rs.toList().get(0);
               if (currentData.get("duration") != null) {
                  duration = currentData.get("duration").toString();
               }

               if (currentData.get("timePosition") != null) {
                  timePosition = currentData.get("timePosition").toString();
               }

               if (currentData.get("dataType") != null) {
                  dataType = Integer.parseInt(currentData.get("dataType").toString());
               }

               if (currentData.get("content") != null) {
                  content = currentData.get("content").toString();
               }

               String[] timeArray = timePosition.split(" ");

               for (int i = 0; i < timeArray.length; i++) {
                  String[] current = timeArray[i].split("\\|");
                  if (current.length == 2) {
                     timeList.add(current[0]);
                     positionList.add(Integer.parseInt(current[1].split(",")[1]));
                  }
               }

               String indexSql = "select " + indexSelectColu + " from " + dataSource + "model where id='" + keyWordRequest.getCallId() + "'";
               this.logger.info("##search indexSql: " + indexSql);
               String url1 = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
               ResultSet indexRs = this.excuteContext.executeSearchQuery(url1, indexSql);
               List<LinkedHashMap<String, Object>> ruleInfoData = new ArrayList<>();
               if (indexRs != null && indexRs.getTotalCount() > 0L) {
                  ruleInfoData = indexRs.toList();
               }

               if (!StringUtils.isNullOrEmpry(keyWordRequest.getUserId())) {
                  GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
                  groupAuthRequest.setUserId(Long.parseLong(keyWordRequest.getUserId()));
                  groupAuthRequest.setGroupType(1);
                  groupAuthRequest.setDataSource(keyWordRequest.getDataSource());
                  groupId = this.groupService.queryGroupAuth(groupAuthRequest);
               }

               if (groupId == null || groupId.isEmpty()) {
                  return ruleMap;
               }

               ruleMap = this.dealRuleInfo(
                  ruleInfoData, modelNameList, timeList, positionList, 0, 0, dataType, keyWordRequest.getIsLoad(), dataSource, groupId, duration
               );
               List<RuleInfo> ruleInfos = this.dealRuleInfoForEditModel(
                  modelNameList, timeList, positionList, dataType, keyWordRequest.getIsLoad(), dataSource, groupId, duration, content
               );
               ruleMap.addAll(ruleInfos);
            }
         } catch (Exception var24) {
            this.logger.error("getOnlineModelKeyWordService service has a error!");
            throw new ViePlatformServiceException("服务内部错误", var24);
         }

         this.logger.info("### service is ending! ###");
         return ruleMap;
      } else {
         throw new ViePlatformServiceException("callId和dataSource参数不能为空");
      }
   }

   public List<RuleInfo> getOnlineModelKeyWordService_ByTask(ModelKeyWordRequest keyWordRequest) throws ViePlatformServiceException {
      this.logger.info("### 语音信息服务接口getOnlineModelKeyWordService被调用 ###");
      List<RuleInfo> ruleMap = new ArrayList<>();
      if (keyWordRequest != null
         && keyWordRequest.getCallId().length() != 0
         && keyWordRequest.getDataSource().length() != 0
         && keyWordRequest.getTaskId().length() != 0) {
         try {
            if (keyWordRequest.getIsLoad() == 0) {
               this.logger.info("### not offline play");
            } else {
               this.logger.info("### is offline play");
            }

            String mainSelectColu = "childContentLength,childDuration,dataType,timePosition,contentOrigin,voiceId";
            String indexSelectColu = "offLineTagId,offLineTagInfo,offLineTagName";
            String dataSource = keyWordRequest.getDataSource();
            String sql = "select " + mainSelectColu + " from " + dataSource + " where taskId='" + keyWordRequest.getTaskId() + "'";
            this.logger.info("##search sql: " + sql);
            String content = "";
            String timePosition = "";
            String duration = "";
            List<Long> groupId = null;
            int dataType = 1;
            List<String> timeList = new ArrayList<>();
            List<Integer> positionList = new ArrayList<>();
            List<String> modelNameList = new ArrayList<>();
            String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
            ResultSet rs = this.excuteContext.executeSearchQuery(url, sql);
            if (rs != null && rs.getTotalCount() > 0L) {
               LinkedHashMap<String, Object> currentData = (LinkedHashMap<String, Object>)rs.toList().get(0);
               int CallIdCorrespondToContentPosition = -1;
               String[] voiceId = currentData.get("voiceId").toString().split("\\$");

               for (int i = 0; i < voiceId.length; i++) {
                  if (keyWordRequest.getCallId().equals(String.valueOf(voiceId[i]))) {
                     CallIdCorrespondToContentPosition = i;
                     break;
                  }
               }

               if (CallIdCorrespondToContentPosition == -1) {
                  throw new ViePlatformServiceException("在" + keyWordRequest.getTaskId() + "任务中没有找到录音号为:" + keyWordRequest.getCallId() + "的录音");
               }

               String[] SplitedContent = currentData.get("contentOrigin").toString().split("\\$");
               String[] SplitedTimePositon = currentData.get("timePosition").toString().split("\\$");
               new ArrayList();
               ArrayList<HashMap> child_fields = (ArrayList)currentData.get("child_fields");
               if (((HashMap)child_fields.get(CallIdCorrespondToContentPosition)).get("childDuration") != null) {
                  duration = String.valueOf(((HashMap)child_fields.get(CallIdCorrespondToContentPosition)).get("childDuration"));
               }

               if (SplitedTimePositon[CallIdCorrespondToContentPosition] != null) {
                  timePosition = SplitedTimePositon[CallIdCorrespondToContentPosition].toString();
               }

               if (currentData.get("dataType") != null) {
                  dataType = Integer.parseInt(currentData.get("dataType").toString());
               }

               if (SplitedContent[CallIdCorrespondToContentPosition] != null) {
                  content = SplitedContent[CallIdCorrespondToContentPosition].toString().replace(" ", "");
               }

               String[] timeArray = timePosition.split(" ");

               for (int ix = 0; ix < timeArray.length; ix++) {
                  String[] current = timeArray[ix].split("\\|");
                  if (current.length == 2) {
                     timeList.add(current[0]);
                     positionList.add(Integer.parseInt(current[1].split(",")[1]));
                  }
               }

               String indexSql = "select " + indexSelectColu + " from " + dataSource + "model where taskId='" + keyWordRequest.getTaskId() + "'";
               this.logger.info("##search indexSql: " + indexSql);
               String url1 = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
               ResultSet indexRs = this.excuteContext.executeSearchQuery(url1, indexSql);
               List<LinkedHashMap<String, Object>> ruleInfoData = new ArrayList<>();
               if (indexRs != null && indexRs.getTotalCount() > 0L) {
                  ruleInfoData = indexRs.toList();
               }

               if (!StringUtils.isNullOrEmpry(keyWordRequest.getUserId())) {
                  GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
                  groupAuthRequest.setUserId(Long.parseLong(keyWordRequest.getUserId()));
                  groupAuthRequest.setGroupType(1);
                  groupAuthRequest.setDataSource(keyWordRequest.getDataSource());
                  groupId = this.groupService.queryGroupAuth(groupAuthRequest);
               }

               if (groupId == null || groupId.isEmpty()) {
                  return ruleMap;
               }

               int redundantTime = 0;

               for (int ixx = 0; ixx < CallIdCorrespondToContentPosition; ixx++) {
                  redundantTime += Integer.parseInt(String.valueOf(((HashMap)child_fields.get(ixx)).get("childContentLength")));
               }

               int thisVoiceStartTimeInTask = 0;
               int thisVoiceEndTimeInTask = Integer.parseInt(
                     String.valueOf(((HashMap)child_fields.get(CallIdCorrespondToContentPosition)).get("childContentLength"))
                  )
                  - 1;

               for (int ixx = 0; ixx <= CallIdCorrespondToContentPosition; ixx++) {
                  if (ixx == 0) {
                     thisVoiceStartTimeInTask = 0;
                  } else {
                     thisVoiceStartTimeInTask += Integer.parseInt(String.valueOf(((HashMap)child_fields.get(ixx - 1)).get("childContentLength")));
                  }
               }

               thisVoiceEndTimeInTask = thisVoiceStartTimeInTask
                  + Integer.parseInt(String.valueOf(((HashMap)child_fields.get(CallIdCorrespondToContentPosition)).get("childContentLength")))
                  - 1;
               ruleMap = this.dealRuleInfo_ByTask(
                  ruleInfoData,
                  modelNameList,
                  timeList,
                  positionList,
                  0,
                  0,
                  dataType,
                  keyWordRequest.getIsLoad(),
                  dataSource,
                  groupId,
                  duration,
                  thisVoiceStartTimeInTask,
                  thisVoiceEndTimeInTask,
                  redundantTime
               );
               List<RuleInfo> ruleInfos = this.dealRuleInfoForEditModel(
                  modelNameList, timeList, positionList, dataType, keyWordRequest.getIsLoad(), dataSource, groupId, duration, content
               );
               ruleMap.addAll(ruleInfos);
            }
         } catch (Exception var32) {
            this.logger.error("getOnlineModelKeyWordService service has a error!");
            throw new ViePlatformServiceException("服务内部错误", var32);
         }

         this.logger.info("### service is ending! ###");
         return ruleMap;
      } else {
         throw new ViePlatformServiceException("callId、dataSource、taskId参数不能为空");
      }
   }

   public TaskDetailsResponse getDetailsOfTaskByTaskID(DataDetailRequest dataDetailRequest) throws ViePlatformServiceException {
      LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
      TaskDetailsResponse taskDetailsResponse = new TaskDetailsResponse();
      if (dataDetailRequest.getColumns() == null
         && dataDetailRequest.getColumns().isEmpty()
         && dataDetailRequest.getTaskID() == null
         && "".equals(dataDetailRequest.getTaskID())) {
         this.logger.error("getDetailsOfTaskByTaskID()入参信息不足，taskId、Columns不能为空");
         throw new ViePlatformServiceException("getDetailsOfTaskByTaskID()入参信息不足,taskId、Columns不能为空");
      } else {
         List<String> ColumnsToBeDisplayed = dataDetailRequest.getColumns();
         String ColumnsToBeDisplayedStr = "";

         for (String columns : ColumnsToBeDisplayed) {
            ColumnsToBeDisplayedStr = ColumnsToBeDisplayedStr + columns + ",";
         }

         ColumnsToBeDisplayedStr = ColumnsToBeDisplayedStr.substring(0, ColumnsToBeDisplayedStr.length() - 1);
         String taskId = dataDetailRequest.getTaskID();
         String tableName = "";

         try {
            tableName = dataDetailRequest.getDataSource();
         } catch (Exception var13) {
            this.logger.error("拿不到dataSource，找不到表名。");
            throw new ViePlatformServiceException("拿不到dataSource，找不到表名。");
         }

         if (!StringUtils.isNullOrEmpry(taskId) && !StringUtils.isNullOrEmpry(tableName) && !StringUtils.isNullOrEmpry(ColumnsToBeDisplayedStr)) {
            String sql = "select " + ColumnsToBeDisplayedStr + " from " + tableName + " where taskId = '" + taskId + "'";
            this.logger.info("getDetailsOfTaskByTaskId()接口sql为:" + sql);
            String url = DynamicEsSource.getEsSourceByType(dataDetailRequest.getDataSource(), "mainAnydrillAddress");
            ResultSet rs = this.excuteContext.executeSearchQuery(url, sql);
            if (rs != null && rs.getTotalCount() > 0L) {
               dataMap = (LinkedHashMap<String, Object>)rs.toList().get(0);
               ArrayList<HashMap> child_fields = (ArrayList<HashMap>)dataMap.get("child_fields");
               Collections.sort(child_fields, new SortByChildTimeFormat());
               dataMap.put("child_fields", child_fields);
            }

            LinkedHashMap<String, Object> ValueAndDimension = new LinkedHashMap<>();
            List<LinkedHashMap<String, Object>> rowName = new ArrayList<>();

            for (int i = 0; i < ColumnsToBeDisplayed.size(); i++) {
               LinkedHashMap<String, Object> hm = new LinkedHashMap<>();
               if ("childVoiceId".equals(ColumnsToBeDisplayed.get(i))) {
                  hm.put("column", "childVoiceId");
                  hm.put("columnName", "流水号");
                  rowName.add(hm);
               } else if ("childDuration".equals(ColumnsToBeDisplayed.get(i))) {
                  hm.put("column", "childDuration");
                  hm.put("columnName", "通话时长");
                  rowName.add(hm);
               } else if ("childSilenceLong".equals(ColumnsToBeDisplayed.get(i))) {
                  hm.put("column", "childSilenceLong");
                  hm.put("columnName", "静音时长");
                  rowName.add(hm);
               } else if ("childTimeFormat".equals(ColumnsToBeDisplayed.get(i))) {
                  hm.put("column", "childTimeFormat");
                  hm.put("columnName", "来电时间");
                  rowName.add(hm);
               } else if ("childVoiceUri".equals(ColumnsToBeDisplayed.get(i))) {
               }
            }

            if (dataMap.get("child_fields") != null) {
               ArrayList<HashMap> child_fields = (ArrayList<HashMap>)dataMap.get("child_fields");

               for (int ix = 0; ix < child_fields.size(); ix++) {
                  if (child_fields.get(ix).get("childDuration") != null) {
                     int childDuration = Integer.parseInt(String.valueOf(child_fields.get(ix).get("childDuration")));
                     child_fields.get(ix).put("childDuration", childDuration);
                  }

                  if (child_fields.get(ix).get("childSilenceLong") != null) {
                     int childSilenceLong = Integer.parseInt(String.valueOf(child_fields.get(ix).get("childSilenceLong")));
                     child_fields.get(ix).put("childSilenceLong", childSilenceLong);
                  }
               }
            }

            ValueAndDimension.putAll(dataMap);
            ValueAndDimension.put("columns", rowName);
            taskDetailsResponse.setValues(ValueAndDimension);
            return taskDetailsResponse;
         } else {
            this.logger.error("taskId、tableName、ColumnsToBeDisplayedStr 不能为空");
            throw new ViePlatformServiceException("taskId、tableName、ColumnsToBeDisplayedStr 不能为空");
         }
      }
   }

   private List<RuleInfo> dealRuleInfoForEditModel(
      List<String> modelNameList,
      List<String> timeList,
      List<Integer> positionList,
      int dataType,
      int isLoad,
      String dataSource,
      List<Long> groupId,
      String duration,
      String content
   ) {
      List<RuleInfo> ruleInfoList = new ArrayList<>();
      List<Map<String, Object>> modelList = this.modelInfoDB.queryEditModelByGroupId(isLoad, dataSource, groupId);

      for (int index = 0; index < modelList.size(); index++) {
         Map<String, Object> singleModel = modelList.get(index);
         String textRule = singleModel.get("textRule").toString();
         String modelName = singleModel.get("modelName").toString();
         if (!modelNameList.contains(modelName)) {
            modelNameList.add(modelName);
            String[] wordList = toWordsList(textRule);
            List<TagInfo> keywords = KeywordMatcher.match(wordList, content);
            if (keywords != null && keywords.size() != 0) {
               RuleInfo ruleInfo = new RuleInfo();
               List<TagInfo> trueTageList = this.dealRuleInfoResponse(timeList, positionList, 0, keywords, 0, dataType, duration);
               if (trueTageList.size() > 0) {
                  ruleInfo.setRuleName(modelName);
                  ruleInfo.setHitKw(trueTageList);
                  ruleInfo.setOnline(1);
                  ruleInfoList.add(ruleInfo);
               }
            }
         }
      }

      return ruleInfoList;
   }

   public static String[] toWordsList(String str) {
      ArrayList<String> wordsList = new ArrayList<>();
      String[] tmp = str.split("\\(|\\)|\\||!|&");

      for (String item : tmp) {
         if (item.contains("#")) {
            wordsList.add(item);
         } else if (!item.replace(" ", "").equals("")) {
            wordsList.add(item.replace(" ", ""));
         }
      }

      String[] words = new String[wordsList.size()];
      return wordsList.toArray(words);
   }

   public ContactKwContext getContantTaskKwContext(ContantKwRequest kwrequest) throws ViePlatformServiceException {
      this.logger.info("### 语音信息服务接口getContantTaskKwContext被调用 ###");
      ContactKwContext dataMap = new ContactKwContext();
      if (kwrequest != null && kwrequest.getContentOrigin().length() != 0 && kwrequest.getTimePosition().length() != 0 && kwrequest.getDuration().length() != 0
         )
       {
         try {
            int begin = kwrequest.getBegin();
            String[] oneBestKeyword = kwrequest.getContentOrigin() == null ? null : kwrequest.getContentOrigin().split(" ");
            String[] timePositions = kwrequest.getTimePosition() == null ? null : kwrequest.getTimePosition().split(" ");
            String duration = kwrequest.getDuration() == null ? "" : kwrequest.getDuration();
            if (timePositions != null) {
               int position = 0;

               for (int i = 0; i < timePositions.length; i++) {
                  String timeRange = timePositions[i].split("\\|")[1];
                  String time = timeRange.split(",")[0];
                  if (begin < Integer.parseInt(time) || begin == Integer.parseInt(time)) {
                     position = i;
                     break;
                  }
               }

               position = position > 0 ? position - 1 : 0;
               int endPosition = position + this.blank_font_size >= timePositions.length ? timePositions.length - 1 : position + this.blank_font_size;
               String keywordContent = "";
               int silenceCount = 0;

               for (int ix = position; ix < endPosition + 1; ix++) {
                  keywordContent = keywordContent + oneBestKeyword[ix];
                  if (oneBestKeyword[ix].matches("[sn]\\d{12}")) {
                     silenceCount++;
                  }
               }

               int dialogEndPosi = endPosition + silenceCount >= timePositions.length ? timePositions.length - 1 : endPosition + silenceCount;
               int beginDialog = Integer.parseInt(timePositions[position].split("\\|")[0].split(",")[0]);
               int endDialog = Integer.parseInt(timePositions[dialogEndPosi].split("\\|")[0].split(",")[0]);
               keywordContent = StringUtils.isNullOrEmpry(keywordContent) ? "" : keywordContent.replaceAll(CommonParams.contentRGX, "").trim();
               dataMap.setContext(keywordContent);
               dataMap.setStart(beginDialog);
               dataMap.setEnd(endDialog);
               dataMap.setDuration(Integer.parseInt(duration));
            }
         } catch (Exception var14) {
            this.logger.error("getContantTaskKwContext service has a error!");
            throw new ViePlatformServiceException("服务内部错误", var14);
         }

         this.logger.info("### service is ending! dataMap: " + dataMap.getContext() + "###");
         return dataMap;
      } else {
         throw new ViePlatformServiceException("请求参数不能contentOrigin、timePosition不能为空");
      }
   }

   private Builder getWareFormatBuilder(Map<String, Object> vgsData) {
      int headerLength = 44;
      int datalength = Integer.parseInt(vgsData.get("dataLength").toString());
      int waveformat = Integer.parseInt(vgsData.get("audioformat").toString());
      int samplaRate = Integer.parseInt(vgsData.get("samplerate").toString());
      int channel = Integer.parseInt(vgsData.get("channels").toString());
      int blockAlign = Integer.parseInt(vgsData.get("blockalign").toString());
      int bitsPerSample = Integer.parseInt(vgsData.get("bitspersample").toString());
      return new Builder()
         .blockAlign((short)blockAlign)
         .headerLength(headerLength)
         .dataLength(datalength)
         .waveformatEncoding(waveformat)
         .sampleRate(samplaRate)
         .bitsPerSample((short)bitsPerSample)
         .channels((short)channel)
         .existHeader(true);
   }

   public InitialiseWaveFormat waveFormat(Builder builder) {
      try {
         InitialiseWaveFormat format = this.InitialisedWaveFormat(builder.build());
         this.logger.info("Return gram initialise data: wave-format, files: {}, initialised wave format: {}" + builder.toString() + format.toString());
         return format;
      } catch (Throwable var3) {
         throw Throwables.propagate(var3);
      }
   }

   private InitialiseWaveFormat InitialisedWaveFormat(WaveFormat format) {
      return new InitialiseWaveFormat(
         format.getWaveFormatEncoding(),
         format.getBitsPerSample(),
         format.getBlockAlign(),
         format.getChannels(),
         format.getSampleRate(),
         (int)(format.getDataLength() / format.getBlockAlign())
      );
   }

   private List<RuleInfo> dealRuleInfo(
      List<LinkedHashMap<String, Object>> ruleInfoData,
      List<String> modelList,
      List<String> timeList,
      List<Integer> positionList,
      int contentLength,
      int nextLength,
      int dataType,
      int isLoad,
      String tableName,
      List<Long> groupIds,
      String duration
   ) {
      List<RuleInfo> ruleInfoList = new ArrayList<>();

      try {
         Map<Long, Map<String, Object>> modelMap = this.modelInfoDB.getModelByGroupId(tableName, groupIds, isLoad);

         for (LinkedHashMap<String, Object> currentData : ruleInfoData) {
            RuleInfo ruleInfo = new RuleInfo();
            boolean isRealTag = true;
            Object objID = currentData.get("realTimeTagId");
            if (objID == null) {
               objID = currentData.get("offLineTagId");
               isRealTag = false;
            }

            int modelID = objID != null ? Integer.parseInt(objID.toString()) : -1;
            Map<String, Object> dataModelInfo = null;
            if (modelMap != null) {
               dataModelInfo = modelMap.get((long)modelID);
            }

            String modelName = dataModelInfo == null ? "" : dataModelInfo.get("modelName").toString();
            if (!modelList.contains(modelName) && modelID != -1 && !StringUtils.isNullOrEmpry(modelName)) {
               modelList.add(modelName);
               modelName = currentData.get("offLineTagName") == null ? "" : currentData.get("offLineTagName").toString();
               Object objTag = isRealTag ? currentData.get("realTimeTagInfo") : currentData.get("offLineTagInfo");
               if (objTag != null) {
                  ObjectMapper mapper = new ObjectMapper();
                  List<TagInfo> tagInfoList = (List<TagInfo>)mapper.readValue(objTag.toString(), new TypeReference<List<TagInfo>>() {});
                  if (tagInfoList != null && tagInfoList.size() != 0) {
                     List<TagInfo> trueTageList = this.dealRuleInfoResponse(timeList, positionList, contentLength, tagInfoList, nextLength, dataType, duration);
                     if (trueTageList.size() > 0) {
                        ruleInfo.setRuleName(modelName);
                        ruleInfo.setHitKw(trueTageList);
                        ruleInfo.setOnline(0);
                        ruleInfoList.add(ruleInfo);
                     }
                  }
               } else {
                  ruleInfo.setOnline(0);
                  ruleInfo.setRuleName(modelName);
                  ruleInfoList.add(ruleInfo);
               }
            }
         }
      } catch (Exception var26) {
         var26.printStackTrace();
      }

      return ruleInfoList;
   }

   private List<RuleInfo> dealRuleInfo_ByTask(
      List<LinkedHashMap<String, Object>> ruleInfoData,
      List<String> modelList,
      List<String> timeList,
      List<Integer> positionList,
      int contentLength,
      int nextLength,
      int dataType,
      int isLoad,
      String tableName,
      List<Long> groupIds,
      String duration,
      int thisVoiceStartTimeInTask,
      int thisVoiceEndTimeInTask,
      int redundantTime
   ) {
      List<RuleInfo> ruleInfoList = new ArrayList<>();

      try {
         Map<Long, Map<String, Object>> modelMap = this.modelInfoDB.getModelByGroupId(tableName, groupIds, isLoad);

         for (LinkedHashMap<String, Object> currentData : ruleInfoData) {
            RuleInfo ruleInfo = new RuleInfo();
            boolean isRealTag = true;
            Object objID = currentData.get("realTimeTagId");
            if (objID == null) {
               objID = currentData.get("offLineTagId");
               isRealTag = false;
            }

            int modelID = objID != null ? Integer.parseInt(objID.toString()) : -1;
            Map<String, Object> dataModelInfo = null;
            if (modelMap != null) {
               dataModelInfo = modelMap.get((long)modelID);
            }

            String modelName = dataModelInfo == null ? "" : dataModelInfo.get("modelName").toString();
            if (!modelList.contains(modelName) && modelID != -1 && !StringUtils.isNullOrEmpry(modelName)) {
               modelList.add(modelName);
               modelName = currentData.get("offLineTagName") == null ? "" : currentData.get("offLineTagName").toString();
               Object objTag = isRealTag ? currentData.get("realTimeTagInfo") : currentData.get("offLineTagInfo");
               if (objTag != null) {
                  ObjectMapper mapper = new ObjectMapper();
                  List<TagInfo> tagInfoList = (List<TagInfo>)mapper.readValue(objTag.toString(), new TypeReference<List<TagInfo>>() {});
                  if (tagInfoList != null && tagInfoList.size() != 0) {
                     int i = 0;

                     for (int len = tagInfoList.size(); i < len; i++) {
                        if (tagInfoList.get(i).getBeginTime() >= thisVoiceStartTimeInTask && tagInfoList.get(i).getBeginTime() <= thisVoiceEndTimeInTask) {
                           tagInfoList.get(i).setBeginTime(tagInfoList.get(i).getBeginTime() - redundantTime);
                        } else {
                           tagInfoList.remove(i);
                           len--;
                           i--;
                        }
                     }

                     List<TagInfo> trueTageList = this.dealRuleInfoResponse(timeList, positionList, contentLength, tagInfoList, nextLength, dataType, duration);
                     if (trueTageList.size() > 0) {
                        ruleInfo.setRuleName(modelName);
                        ruleInfo.setHitKw(trueTageList);
                        ruleInfo.setOnline(0);
                        ruleInfoList.add(ruleInfo);
                     }
                  }
               } else {
                  ruleInfo.setOnline(0);
                  ruleInfo.setRuleName(modelName);
                  ruleInfoList.add(ruleInfo);
               }
            }
         }
      } catch (Exception var30) {
         var30.printStackTrace();
      }

      return ruleInfoList;
   }

   private List<TagInfo> dealRuleInfoResponse(
      List<String> timeList, List<Integer> positionList, int contentLength, List<TagInfo> tagInfoList, int nextLength, int dataType, String duration
   ) {
      List<TagInfo> trueTageList = new ArrayList<>();

      try {
         for (TagInfo tagInfo : tagInfoList) {
            switch (tagInfo.getType()) {
               case 0:
                  TagInfo currentTage = null;
                  if (dataType == 0) {
                     currentTage = CommonFunPlatform.voicePositionToTime(tagInfo, timeList, positionList, contentLength, duration);
                     if (currentTage == null) {
                        break;
                     }
                  } else {
                     currentTage = CommonFunPlatform.voicePositionToTimeByText(tagInfo, contentLength, nextLength, duration);
                  }

                  if (currentTage != null) {
                     trueTageList.add(currentTage);
                  }
                  break;
               case 1:
                  tagInfo.setContent("静音时长");
                  TagInfo currentTage1 = null;
                  if (dataType == 0) {
                     currentTage1 = CommonFunPlatform.voicePositionToTime(tagInfo, timeList, positionList, contentLength, duration);
                     if (currentTage1 == null) {
                        break;
                     }
                  } else {
                     currentTage1 = CommonFunPlatform.voicePositionToTimeByText(tagInfo, contentLength, nextLength, duration);
                  }

                  if (currentTage1 != null) {
                     trueTageList.add(currentTage1);
                  }
                  break;
               case 2:
                  if (tagInfo.getContent() != null && tagInfo.getContent() != "") {
                     tagInfo.setContent("抢插话");
                     trueTageList.add(tagInfo);
                  }
                  break;
               case 3:
                  if (tagInfo.getContent() != null && tagInfo.getContent() != "") {
                     tagInfo.setContent("语速");
                     trueTageList.add(tagInfo);
                  }
                  break;
               case 4:
                  if (tagInfo.getContent() != null && tagInfo.getContent() != "") {
                     tagInfo.setContent("音量");
                     trueTageList.add(tagInfo);
                  }
            }
         }
      } catch (Exception var13) {
         var13.printStackTrace();
      }

      return trueTageList;
   }

   private String getVoiceServerPath(String machineId) {
      return "http://" + machineId + "/voiceService/";
   }

   public void setModelInfoDB(ModelInfoDBPlatform modelInfoDB) {
      this.modelInfoDB = modelInfoDB;
   }

   public void setBlank_font_size(int blank_font_size) {
      this.blank_font_size = blank_font_size;
   }

   public ExcuteContext getExcuteContext() {
      return this.excuteContext;
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
   }

   public GroupAuthService getGroupService() {
      return this.groupService;
   }

   public void setGroupService(GroupAuthService groupService) {
      this.groupService = groupService;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }
}
