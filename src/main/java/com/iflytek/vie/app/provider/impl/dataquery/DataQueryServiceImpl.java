package com.iflytek.vie.app.provider.impl.dataquery;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.dataquery.DataQueryService;
import com.iflytek.vie.app.api.dimension.DimensionService;
import com.iflytek.vie.app.api.player.PlayerService;
import com.iflytek.vie.app.api.player.server.wave.objects.InitialiseWaveFormat;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.dataquery.DataFilter;
import com.iflytek.vie.app.pojo.dataquery.DataQueryRequest;
import com.iflytek.vie.app.pojo.dataquery.PlayAudio;
import com.iflytek.vie.app.pojo.dataquery.VoiceDataRequest;
import com.iflytek.vie.app.pojo.dataquery.VoiceDataResponse;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.DimensionRequest;
import com.iflytek.vie.app.pojo.player.PlayerDataRequest;
import com.iflytek.vie.app.pojo.player.VoiceBaseInfo;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.anydrill.calculate.set.ResultSet;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataQueryServiceImpl implements DataQueryService {
   private final Logger logger = LoggerFactory.getLogger(DataQueryServiceImpl.class);
   private ExcuteContext excuteContext;
   private PlayerService playerService;
   private DimensionService dimensionService;
   public static final int playRangeSize = 2000;
   private ObjectMapper mapper = new ObjectMapper();
   private DataSourceInfo dataSourceInfo;

   public VoiceDataResponse getVoiceList_ByVoice(VoiceDataRequest voiceDataRequest) throws VieAppServiceException {
      String callId = voiceDataRequest.getCallId();
      String selVoiceColumn = voiceDataRequest.getSelVoiceColumn();
      String dataSource = voiceDataRequest.getDataSource();
      if (StringUtils.isNullOrEmpry(callId)) {
         throw new VieAppServiceException("参数(callId)为空");
      } else if (StringUtils.isNullOrEmpry(selVoiceColumn)) {
         throw new VieAppServiceException("参数(selVoiceColumn)为空");
      } else if (StringUtils.isNullOrEmpry(dataSource)) {
         throw new VieAppServiceException("参数(dataSource)为空");
      } else {
         VoiceDataResponse voiceDataResponse = new VoiceDataResponse();

         try {
            String sql = "select " + selVoiceColumn + " from " + dataSource + " where id = '" + callId + "' and processed=0 ";
            String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
            ResultSet resultSet = this.excuteContext.executeSearchQuery(url, sql);
            if (resultSet != null && resultSet.getTotalCount() != 0L) {
               DimensionRequest request = new DimensionRequest();
               request.setDataSource(dataSource);
               LinkedHashMap<String, DimensionConfig> configMap = this.dimensionService.getDimensionMap(request);
               List<String> showColumn = this.getShowColumn(voiceDataResponse, selVoiceColumn.split(","), configMap);
               List<LinkedHashMap<String, Object>> rows = new ArrayList<>();

               for (LinkedHashMap<String, Object> currentData : resultSet.toList()) {
                  LinkedHashMap<String, Object> trueData = new LinkedHashMap<>();

                  for (int i = 0; i < showColumn.size(); i++) {
                     String cSelColumn = showColumn.get(i);
                     trueData.put(cSelColumn, currentData.get(cSelColumn));
                  }

                  rows.add(trueData);
               }

               voiceDataResponse.setRows(rows);
            }
         } catch (Exception var20) {
            this.logger.error("服务内部错误", var20);
         }

         return voiceDataResponse;
      }
   }

   public VoiceDataResponse getVoiceList_ByTask(VoiceDataRequest voiceDataRequest) throws VieAppServiceException {
      String taskId = voiceDataRequest.getTaskId();
      String selVoiceColumn = voiceDataRequest.getSelVoiceColumn();
      String dataSource = voiceDataRequest.getDataSource();
      if (StringUtils.isNullOrEmpry(taskId)) {
         throw new VieAppServiceException("参数taskId为空。");
      } else if (StringUtils.isNullOrEmpry(selVoiceColumn)) {
         throw new VieAppServiceException("参数selVoiceColumn为空。");
      } else if (StringUtils.isNullOrEmpry(dataSource)) {
         throw new VieAppServiceException("参数dataSource为空。");
      } else {
         VoiceDataResponse voiceDataResponse = new VoiceDataResponse();

         try {
            String sql = "select " + selVoiceColumn + " from " + dataSource + " where taskId = '" + taskId + "' and processed = 0";
            String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
            ResultSet resultSet = this.excuteContext.executeSearchQuery(url, sql);
            if (resultSet != null && resultSet.getTotalCount() != 0L) {
               DimensionRequest request = new DimensionRequest();
               request.setDataSource(dataSource);
               LinkedHashMap<String, DimensionConfig> configMap = this.dimensionService.getDimensionMap(request);
               List<String> showColumn = this.getShowColumn(voiceDataResponse, selVoiceColumn.split(","), configMap);
               List<LinkedHashMap<String, Object>> rows = new ArrayList<>();

               for (LinkedHashMap<String, Object> currentData : resultSet.toList()) {
                  LinkedHashMap<String, Object> trueData = new LinkedHashMap<>();

                  for (int i = 0; i < showColumn.size(); i++) {
                     String cSelColumn = showColumn.get(i);
                     if (cSelColumn.equals("duration")) {
                        int duration = Integer.parseInt(String.valueOf(currentData.get(cSelColumn)));
                        trueData.put(cSelColumn, duration);
                     } else if (cSelColumn.equals("silenceLong")) {
                        int silenceLong = Integer.parseInt(String.valueOf(currentData.get(cSelColumn)));
                        trueData.put(cSelColumn, silenceLong);
                     } else if (cSelColumn.equals("vadDuration")) {
                        int vadDuration = Integer.parseInt(String.valueOf(currentData.get(cSelColumn)));
                        trueData.put(cSelColumn, vadDuration);
                     } else {
                        trueData.put(cSelColumn, currentData.get(cSelColumn));
                     }
                  }

                  rows.add(trueData);
               }

               voiceDataResponse.setRows(rows);
            }
         } catch (Exception var21) {
            this.logger.error("服务内部错误", var21);
         }

         return voiceDataResponse;
      }
   }

   private List<String> getShowColumn(VoiceDataResponse voiceDataResponse, String[] selArray, LinkedHashMap<String, DimensionConfig> configMap) {
      String LISTIDFIELD = "id";
      String LISTIDNAME = "编号";
      String TIMEFORMAT = "timeFormat";
      String TIMEFORMATNAME = "来电时间";
      LinkedHashMap<String, String> rowsHead = new LinkedHashMap<>();
      List<HashMap<String, Object>> rowsName = new ArrayList<>();
      List<String> showColumn = new ArrayList<>();

      for (int i = 0; i < selArray.length; i++) {
         HashMap<String, Object> hm = new HashMap<>();
         String currentColumn = selArray[i];
         DimensionConfig dimensionInfo = configMap.get(currentColumn);
         if (dimensionInfo != null && dimensionInfo.getShow() == 1) {
            String columnName = dimensionInfo.getShowName();
            rowsHead.put(currentColumn, columnName);
            hm.put("column", currentColumn);
            hm.put("columnName", columnName);
            rowsName.add(hm);
            showColumn.add(currentColumn);
         } else if ("id".equals(currentColumn)) {
            showColumn.add(currentColumn);
            rowsHead.put(currentColumn, "编号");
            hm.put("column", currentColumn);
            hm.put("columnName", "编号");
            rowsName.add(hm);
         } else if ("timeFormat".equals(currentColumn)) {
            showColumn.add(currentColumn);
            rowsHead.put(currentColumn, "来电时间");
            hm.put("column", currentColumn);
            hm.put("columnName", "来电时间");
            rowsName.add(hm);
         }
      }

      voiceDataResponse.setRowsHead(rowsHead);
      voiceDataResponse.setRowsName(rowsName);
      return showColumn;
   }

   public LinkedHashMap<String, Object> getAudioBaseInfo_ByVoice(DataFilter dataFilter) throws VieAppServiceException {
      LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
      if (dataFilter == null) {
         return null;
      } else if (dataFilter.getVoiceId() == null || dataFilter.getVoiceId().length() == 0) {
         this.logger.error("voiceId 参数不能为空");
         throw new VieAppServiceException("voiceId 参数不能为空");
      } else if (dataFilter.getDataSource() != null && dataFilter.getDataSource().length() != 0) {
         try {
            List<String> selColum = dataFilter.getQueryList();
            String selStr = "";

            for (String str : selColum) {
               if (!selStr.contains(str + ",")) {
                  selStr = selStr + str + ",";
               }
            }

            selStr = selStr.substring(0, selStr.length() - 1);
            String voiceId = dataFilter.getVoiceId();
            String indexTableName = dataFilter.getDataSource();
            if (!StringUtils.isNullOrEmpry(voiceId) && !StringUtils.isNullOrEmpry(indexTableName) && !StringUtils.isNullOrEmpry(selStr)) {
               String sql = "select " + selStr + " from " + indexTableName + " where voiceId='" + voiceId + "'";
               this.logger.info(sql);
               String url = DynamicEsSource.getEsSourceByType(dataFilter.getDataSource(), "mainAnydrillAddress");
               ResultSet rs = this.excuteContext.executeSearchQuery(url, sql);
               if (rs != null && rs.getTotalCount() > 0L) {
                  dataMap = (LinkedHashMap<String, Object>)rs.toList().get(0);
               }

               return dataMap;
            } else {
               this.logger.error("参数voiceId，tableName，queryList不能为空！");
               throw new VieAppServiceException("参数voiceId，tableName，queryList不能为空！");
            }
         } catch (Exception var10) {
            this.logger.error("服务内部错误", var10);
            throw new VieAppServiceException("服务内部错误", var10);
         }
      } else {
         this.logger.error("dateSource 参数不能为空");
         throw new VieAppServiceException("dateSource 参数不能为空");
      }
   }

   public LinkedHashMap<String, Object> getAudioBaseInfo_ByTask(DataFilter dataFilter) throws VieAppServiceException {
      LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
      if (dataFilter == null) {
         return null;
      } else if (dataFilter.getTaskId() == null || dataFilter.getTaskId().length() == 0) {
         this.logger.error("TaskId不能为空");
         throw new VieAppServiceException("TaskId不能为空");
      } else if (dataFilter.getDataSource() != null && dataFilter.getDataSource().length() != 0) {
         try {
            List<String> selColum = dataFilter.getQueryList();
            String selStr = "";

            for (String str : selColum) {
               if (!selStr.contains(str + ",")) {
                  selStr = selStr + str + ",";
               }
            }

            selStr = selStr.substring(0, selStr.length() - 1);
            String taskId = dataFilter.getTaskId();
            String indexTableName = dataFilter.getDataSource();
            if (!StringUtils.isNullOrEmpry(taskId) && !StringUtils.isNullOrEmpry(indexTableName) && !StringUtils.isNullOrEmpry(selStr)) {
               String sql = "select " + selStr + " from " + indexTableName + " where taskId='" + taskId + "'";
               this.logger.info(sql);
               String url = DynamicEsSource.getEsSourceByType(dataFilter.getDataSource(), "mainAnydrillAddress");
               ResultSet rs = this.excuteContext.executeSearchQuery(url, sql);
               if (rs != null && rs.getTotalCount() > 0L) {
                  dataMap = (LinkedHashMap<String, Object>)rs.toList().get(0);
               }

               return dataMap;
            } else {
               this.logger.error("taskId、tableName、queryList不能为空");
               throw new VieAppServiceException("taskId、tableName、queryList不能为空");
            }
         } catch (Exception var10) {
            this.logger.error("服务内部错误", var10);
            throw new VieAppServiceException("服务内部错误", var10);
         }
      } else {
         this.logger.error("DataSource不能为空");
         throw new VieAppServiceException("DataSource不能为空");
      }
   }

   public String getSilverlightPlayText_ByVoice(DataQueryRequest queryRequest) throws VieAppServiceException {
      String resultStr = "";
      if (StringUtils.isNullOrEmpry(queryRequest.getCallId())
         || StringUtils.isNullOrEmpry(queryRequest.getSpeechUrl())
         || StringUtils.isNullOrEmpry(queryRequest.getDataSource())) {
         this.logger.error("参数不能为空");
         throw new VieAppServiceException("参数不能为空");
      } else if (queryRequest.getDataSource() != null && queryRequest.getDataSource().length() != 0) {
         try {
            String indexTableName = queryRequest.getDataSource();
            String sql = "select contentOrigin,channelSeq,timePosition,voiceUri,machineId from "
               + indexTableName
               + " where id='"
               + queryRequest.getCallId()
               + "'";
            this.logger.info(sql);
            String url1 = DynamicEsSource.getEsSourceByType(queryRequest.getDataSource(), "mainAnydrillAddress");
            ResultSet resultSet = this.excuteContext.executeSearchQuery(url1, sql);
            if (resultSet != null && resultSet.getTotalCount() != 0L) {
               List<LinkedHashMap<String, Object>> dataList = resultSet.toList();
               LinkedHashMap<String, Object> data = dataList.get(0);
               int voiceID = 1;
               String[] oneBestKeywordTask = data.get("contentOrigin").toString().split("\\$");
               String[] timePositionsTask = data.get("timePosition").toString().split("\\$");
               String[] channelsTask = data.get("channelSeq").toString().split("\\$");
               String oneBestKeyword = oneBestKeywordTask[voiceID - 1];
               String timePositions = timePositionsTask[voiceID - 1];
               String channels = channelsTask[voiceID - 1];
               String voiceUrlInfo = data.get("voiceUri") == null ? "" : data.get("voiceUri").toString();
               String[] voiceUrlArray = voiceUrlInfo.split(" ");
               String voiceUrl = "";

               for (int i = 0; i <= voiceUrlArray.length; i++) {
                  if (voiceID == i + 1) {
                     voiceUrl = voiceUrlArray[i];
                  }
               }

               String strMacTagArray = data.get("machineId") == null ? "" : data.get("machineId").toString();
               String[] macTagArray = strMacTagArray.split("\\$");
               String macTag = "";
               if (macTagArray.length >= voiceID) {
                  macTag = macTagArray[voiceID - 1];
               }

               PlayerDataRequest request = new PlayerDataRequest(oneBestKeyword, channels, timePositions, voiceUrl, macTag);
               VoiceBaseInfo baseInfo = this.playerService.getFullAudioInfoService(request);
               voiceUrl = voiceUrl.replace("#", "|$|");
               String url = queryRequest.getSpeechUrl() + (!queryRequest.getIsV3Player() ? "?voiceUrl=" + voiceUrl + "&amp;macTag=" + macTag : "");
               int audionLength = queryRequest.getIsV3Player() ? queryRequest.getAudioLength() : baseInfo.getVoiceLength();
               resultStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<AudioCtrRes>\n    <SpeechUri>"
                  + url
                  + "</SpeechUri>\n"
                  + "    <onebest>"
                  + baseInfo.getOneBest()
                  + "</onebest>\n"
                  + "    <onebestTime>"
                  + baseInfo.getTimePositionStr()
                  + "</onebestTime>\n"
                  + "    <voiceIndex>"
                  + baseInfo.getVoiceIndex()
                  + "</voiceIndex>\n"
                  + "    <audioLength>"
                  + audionLength
                  + "</audioLength>\n"
                  + "    <currentIndex>"
                  + baseInfo.getCurrentIndex()
                  + "</currentIndex>\n"
                  + "    <trueOnebestTime>"
                  + baseInfo.getTrueOnebestTime()
                  + "</trueOnebestTime>\n"
                  + " <tags></tags>\n"
                  + "</AudioCtrRes>";
            }

            return resultStr;
         } catch (Exception var26) {
            this.logger.error("服务内部错误", var26);
            throw new VieAppServiceException("服务内部错误", var26);
         }
      } else {
         this.logger.error("数据源参数不能为空");
         throw new VieAppServiceException("数据源参数不能为空");
      }
   }

   public String getSilverlightPlayText_ByTask(DataQueryRequest queryRequest) throws VieAppServiceException {
      String resultStr = "";
      if (StringUtils.isNullOrEmpry(queryRequest.getCallId())
         || StringUtils.isNullOrEmpry(queryRequest.getSpeechUrl())
         || StringUtils.isNullOrEmpry(queryRequest.getDataSource())) {
         this.logger.error("参数不能为空");
         throw new VieAppServiceException("参数不能为空");
      } else if (queryRequest.getDataSource() == null || queryRequest.getDataSource().length() == 0) {
         this.logger.error("数据源参数不能为空");
         throw new VieAppServiceException("数据源参数不能为空");
      } else if (queryRequest.getTaskId() != null && !StringUtils.isNullOrEmpry(queryRequest.getTaskId())) {
         try {
            String indexTableName = queryRequest.getDataSource();
            String sql = "select contentOrigin,channelSeq,timePosition,voiceId,childVoiceUri,machineId from "
               + indexTableName
               + " where taskId='"
               + queryRequest.getTaskId()
               + "'";
            this.logger.info(sql);
            String url1 = DynamicEsSource.getEsSourceByType(queryRequest.getDataSource(), "mainAnydrillAddress");
            ResultSet resultSet = this.excuteContext.executeSearchQuery(url1, sql);
            if (resultSet != null && resultSet.getTotalCount() != 0L) {
               List<LinkedHashMap<String, Object>> dataList = resultSet.toList();
               LinkedHashMap<String, Object> data = dataList.get(0);
               int voiceID = 1;
               String[] oneBestKeywordTask = data.get("contentOrigin").toString().split("\\$");
               String[] timePositionsTask = data.get("timePosition").toString().split("\\$");
               String[] channelsTask = data.get("channelSeq").toString().split("\\$");
               String[] voiceIdTask = data.get("voiceId").toString().split("\\$");
               int CallIdCorrespondToTaskPosition = -1;

               for (int i = 0; i < voiceIdTask.length; i++) {
                  if (queryRequest.getCallId().equals(String.valueOf(voiceIdTask[i]))) {
                     CallIdCorrespondToTaskPosition = i;
                     break;
                  }
               }

               if (CallIdCorrespondToTaskPosition == -1) {
                  this.logger.error("在任务号为" + queryRequest.getTaskId() + "的任务下没有找到录音流水号为" + queryRequest.getCallId() + "的录音");
                  throw new VieAppServiceException("在任务号为" + queryRequest.getTaskId() + "的任务下没有找到录音流水号为" + queryRequest.getCallId() + "的录音");
               }

               String oneBestKeyword = oneBestKeywordTask[CallIdCorrespondToTaskPosition];
               String timePositions = timePositionsTask[CallIdCorrespondToTaskPosition];
               String channels = channelsTask[CallIdCorrespondToTaskPosition];
               ArrayList<HashMap> child_fields = (ArrayList<HashMap>)data.get("child_fields");
               String voiceUri = child_fields.get(CallIdCorrespondToTaskPosition).get("childVoiceUri") == null
                  ? ""
                  : child_fields.get(CallIdCorrespondToTaskPosition).get("childVoiceUri").toString();
               String voiceUrl = "";
               String strMacTagArray = data.get("machineId") == null ? "" : data.get("machineId").toString();
               String[] macTagArray = strMacTagArray.split("\\$");
               String macTag = "";
               if (macTagArray.length >= voiceID) {
                  macTag = macTagArray[voiceID - 1];
               }

               PlayerDataRequest request = new PlayerDataRequest(oneBestKeyword, channels, timePositions, voiceUri, macTag);
               VoiceBaseInfo baseInfo = this.playerService.getFullAudioInfoService(request);
               voiceUrl = voiceUri.replace("#", "|$|");
               String url = queryRequest.getSpeechUrl() + (!queryRequest.getIsV3Player() ? "?voiceUrl=" + voiceUrl + "&amp;macTag=" + macTag : "");
               int audionLength = queryRequest.getIsV3Player() ? queryRequest.getAudioLength() : baseInfo.getVoiceLength();
               resultStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<AudioCtrRes>\n    <SpeechUri>"
                  + url
                  + "</SpeechUri>\n"
                  + "    <onebest>"
                  + baseInfo.getOneBest()
                  + "</onebest>\n"
                  + "    <onebestTime>"
                  + baseInfo.getTimePositionStr()
                  + "</onebestTime>\n"
                  + "    <voiceIndex>"
                  + baseInfo.getVoiceIndex()
                  + "</voiceIndex>\n"
                  + "    <audioLength>"
                  + audionLength
                  + "</audioLength>\n"
                  + "    <currentIndex>"
                  + baseInfo.getCurrentIndex()
                  + "</currentIndex>\n"
                  + "    <trueOnebestTime>"
                  + baseInfo.getTrueOnebestTime()
                  + "</trueOnebestTime>\n"
                  + " <tags></tags>\n"
                  + "</AudioCtrRes>";
            }

            return resultStr;
         } catch (Exception var28) {
            this.logger.error("服务内部错误", var28);
            throw new VieAppServiceException("服务内部错误", var28);
         }
      } else {
         this.logger.error("请求的taskId不能为空。");
         throw new VieAppServiceException("请求的taskId不能为空。");
      }
   }

   public String getUcSilverlightPlayText(DataQueryRequest queryRequest) throws VieAppServiceException {
      String resultStr = "";
      if (StringUtils.isNullOrEmpry(queryRequest.getVoicePath())) {
         this.logger.error("voicePath 参数不能为空");
         throw new VieAppServiceException("voicePath 参数不能为空");
      } else if (StringUtils.isNullOrEmpry(queryRequest.getMacTag())) {
         this.logger.error("macTag 参数不能为空");
         throw new VieAppServiceException("macTag 参数不能为空");
      } else if (queryRequest.getChannel() != 1 && queryRequest.getChannel() != 2) {
         this.logger.error("channel 参数符合规范");
         throw new VieAppServiceException("channel 参数符合规范");
      } else if (queryRequest.getBegin() == queryRequest.getEnd() || queryRequest.getEnd() == 0) {
         this.logger.error("begin和end 参数不符合规范");
         throw new VieAppServiceException("begin和end 参数不符合规范");
      } else if (queryRequest.getDuration() == 0) {
         this.logger.error("duration 参数不能为空或0");
         throw new VieAppServiceException("duration 参数不能为空或0");
      } else {
         try {
            int[] audioPosition = new int[2];
            int tStart = queryRequest.getBegin();
            int tEnd = queryRequest.getEnd();
            int channel = queryRequest.getChannel();
            audioPosition[0] = Math.max(0, tStart - 2000) * 16 * channel;
            audioPosition[1] = Math.min(queryRequest.getDuration(), tEnd + 2000) * 16 * channel;
            String voiceUrl = queryRequest.getVoicePath();
            voiceUrl = voiceUrl.replace("#", "|$|");
            String url = queryRequest.getSpeechUrl()
               + String.format("?voiceUrl=%s&amp;macTag=%s&amp;begin=%s", voiceUrl, queryRequest.getMacTag(), audioPosition[0]);
            int voiceLength = Math.max(0, audioPosition[1] - audioPosition[0]);
            String oneBest = "";
            String timePositionStr = "";
            String trueOnebestTime = "";
            String voiceIndex = "";
            resultStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<AudioCtrRes>\n    <SpeechUri>"
               + url
               + "</SpeechUri>\n"
               + "    <onebest>"
               + oneBest
               + "</onebest>\n"
               + "    <onebestTime>"
               + timePositionStr
               + "</onebestTime>\n"
               + "    <voiceIndex>"
               + voiceIndex
               + "</voiceIndex>\n"
               + "    <audioLength>"
               + voiceLength
               + "</audioLength>\n"
               + "    <currentIndex>"
               + 1
               + "</currentIndex>\n"
               + "    <trueOnebestTime>"
               + trueOnebestTime
               + "</trueOnebestTime>\n"
               + "    <tags></tags>\n"
               + "</AudioCtrRes>";
         } catch (Exception var14) {
            this.logger.error(var14.getMessage());
         }

         return resultStr;
      }
   }

   public byte[] getSlicedStreamData(DataQueryRequest queryRequest) throws VieAppServiceException {
      byte[] bys2 = null;
      if (StringUtils.isNullOrEmpry(queryRequest.getVoicePath())) {
         this.logger.error("voicePath 参数不能为空");
         throw new VieAppServiceException("voicePath 参数不能为空");
      } else if (StringUtils.isNullOrEmpry(queryRequest.getMacTag())) {
         this.logger.error("macTag 参数不能为空");
         throw new VieAppServiceException("macTag 参数不能为空");
      } else if (queryRequest.getCount() == 0) {
         this.logger.error("count 参数不能为0");
         throw new VieAppServiceException("count 参数不能为0");
      } else {
         try {
            String voiceUrl = queryRequest.getVoicePath();
            voiceUrl = voiceUrl.replace("|$|", "#");
            PlayerDataRequest request = new PlayerDataRequest(voiceUrl, queryRequest.getMacTag(), queryRequest.getOffset(), queryRequest.getCount());
            String data = this.playerService.getSlicedStreamService(request);
            Map<String, Object> vgsData = (Map<String, Object>)JSON.parseObject(data, Map.class);
            return Base64.decodeBase64(vgsData.get("vgsAudioData").toString());
         } catch (Exception var8) {
            this.logger.error("服务内部错误", var8);
            throw new VieAppServiceException("服务内部错误", var8);
         }
      }
   }

   public Object getGramData(DataQueryRequest queryRequest) throws VieAppServiceException {
      Object resultObj = null;
      if (StringUtils.isNullOrEmpry(queryRequest.getVoicePath())) {
         this.logger.error("voicePath 参数不能为空");
         throw new VieAppServiceException("voicePath 参数不能为空");
      } else if (StringUtils.isNullOrEmpry(queryRequest.getMacTag())) {
         this.logger.error("macTag 参数不能为空");
         throw new VieAppServiceException("macTag 参数不能为空");
      } else {
         String voice_format = "";
         if (StringUtils.isNullOrEmpry(queryRequest.getVoiceGramAction())) {
            this.logger.error("VoiceGram-Action 参数不能为空");
            throw new VieAppServiceException("VoiceGram-Action 参数不能为空");
         } else {
            voice_format = queryRequest.getVoiceGramAction();
            if ("wave-format".equals(voice_format)) {
               this.logger.info("wave-format：{}", voice_format);
            } else {
               if (!"channel-data".equals(voice_format)) {
                  this.logger.error("VoiceGram-Action 参数不合法");
                  throw new VieAppServiceException("VoiceGram-Action 参数不合法");
               }

               if (StringUtils.isNullOrEmpry(queryRequest.getVoiceGramSampleRange())) {
                  this.logger.error("VoiceGram-Sample-Range 参数不能为空");
                  throw new VieAppServiceException("VoiceGram-Sample-Range 参数不能为空");
               }

               if (StringUtils.isNullOrEmpry(queryRequest.getVoiceGramChannel())) {
                  this.logger.error("VoiceGram-Channel 参数不能为空");
                  throw new VieAppServiceException("VoiceGram-Channel 参数不能为空");
               }

               if (StringUtils.isNullOrEmpry(queryRequest.getVoiceGramBlockSize())) {
                  this.logger.error("VoiceGram-Block-Size 参数不能为空");
                  throw new VieAppServiceException("VoiceGram-Block-Size 参数不能为空");
               }
            }

            try {
               String voiceUrl = queryRequest.getVoicePath();
               voiceUrl = voiceUrl.replace("|$|", "#");
               PlayerDataRequest playerDataRequest = new PlayerDataRequest(voiceUrl, queryRequest.getMacTag());
               if ("wave-format".equals(voice_format)) {
                  InitialiseWaveFormat iwf = this.playerService.getVoiceFormatService(playerDataRequest);
                  resultObj = iwf;
               } else {
                  if (!"channel-data".equals(voice_format)) {
                     this.logger.error("你的VoiceGram-Action参数呢?");
                     throw new VieAppServiceException("VoiceGram-Action播放参数丢失.");
                  }

                  String rangeSample = queryRequest.getVoiceGramSampleRange();
                  String channel = queryRequest.getVoiceGramChannel();
                  String blockSize = queryRequest.getVoiceGramBlockSize();
                  String[] sampleRange = StringUtils.removeStart(rangeSample, "samples=").split("-");
                  int chan = Integer.parseInt(channel);
                  int startSample = Integer.parseInt(sampleRange[0]);
                  int endSample = Integer.parseInt(sampleRange[1]);
                  int bsize = Integer.parseInt(blockSize);
                  playerDataRequest.setChannel(chan);
                  playerDataRequest.setOffset(startSample);
                  playerDataRequest.setEndSample(endSample);
                  playerDataRequest.setBlockSize(bsize);
                  String voiceHeaderData1 = this.playerService.getAudioGramService(playerDataRequest);
                  if (voiceHeaderData1 != null && !voiceHeaderData1.endsWith("]")) {
                     voiceHeaderData1 = voiceHeaderData1 + "]";
                  }

                  Object[] channelData = (Object[])JSON.parseObject(voiceHeaderData1, Object[].class);
                  resultObj = channelData;
               }

               return resultObj;
            } catch (Exception var16) {
               this.logger.error("服务内部错误", var16);
               throw new VieAppServiceException("服务内部错误", var16);
            }
         }
      }
   }

   public PlayAudio getPlayData(DataQueryRequest queryRequest) throws VieAppServiceException {
      PlayAudio dataMap = new PlayAudio();
      if (StringUtils.isNullOrEmpry(queryRequest.getVoicePath())) {
         this.logger.error("voicePath 参数不能为空");
         throw new VieAppServiceException("voicePath 参数不能为空");
      } else if (StringUtils.isNullOrEmpry(queryRequest.getMacTag())) {
         this.logger.error("macTag 参数不能为空");
         throw new VieAppServiceException("macTag 参数不能为空");
      } else if (StringUtils.isNullOrEmpry(queryRequest.getRange())) {
         this.logger.error("range 参数不能为空");
         throw new VieAppServiceException("range 参数不能为空");
      } else if (queryRequest.getCount() == 0) {
         this.logger.error("count 参数不能为0");
         throw new VieAppServiceException("count 参数不能为0");
      } else {
         try {
            String request_range = queryRequest.getRange();
            int index = 0;
            String fileLengthReturn = "";
            byte[] bys2 = null;
            String voiceUrl = queryRequest.getVoicePath();
            voiceUrl = voiceUrl.replace("|$|", "#");
            if (request_range == null) {
               this.logger.error("getPlayData传入参数有误");
               throw new VieAppServiceException("getPlayData传入参数有误");
            } else {
               PlayerDataRequest playRequest = null;
               String[] sampleRange = StringUtils.removeStart(request_range, "bytes=").split("-");
               int[] indexes = new int[sampleRange.length];

               for (int i = 0; i < sampleRange.length; i++) {
                  indexes[i] = Integer.parseInt(sampleRange[i]);
               }

               index = indexes[0];
               switch (indexes.length) {
                  case 1:
                     playRequest = new PlayerDataRequest(voiceUrl, queryRequest.getMacTag(), index, queryRequest.getCount());
                     String data = this.playerService.getSlicedStreamService(playRequest);
                     Map<String, Object> vgsData = (Map<String, Object>)JSON.parseObject(data, Map.class);
                     bys2 = Base64.decodeBase64(vgsData.get("vgsAudioData").toString());
                     fileLengthReturn = vgsData.get("vgsLength").toString();
                     break;
                  case 2:
                     int count = indexes[1] - index + 1;
                     playRequest = new PlayerDataRequest(voiceUrl, queryRequest.getMacTag(), index, count);
                     String data1 = this.playerService.getSlicedStreamService(playRequest);
                     Map<String, Object> vgsData1 = (Map<String, Object>)JSON.parseObject(data1, Map.class);
                     bys2 = Base64.decodeBase64(vgsData1.get("vgsAudioData").toString());
                     fileLengthReturn = vgsData1.get("vgsLength").toString();
               }

               String contentRange = String.format("bytes %s-%s/%s", index, bys2.length - 1 + index, fileLengthReturn);
               dataMap.setContentLength(String.valueOf(bys2.length));
               dataMap.setPlayBytes(bys2);
               dataMap.setContentRange(contentRange);
               return dataMap;
            }
         } catch (Exception var17) {
            this.logger.error("服务内部错误", var17);
            throw new VieAppServiceException("服务内部错误", var17);
         }
      }
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
   }

   public void setPlayerService(PlayerService playerService) {
      this.playerService = playerService;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   public void setDimensionService(DimensionService dimensionService) {
      this.dimensionService = dimensionService;
   }
}
