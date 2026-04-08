package com.iflytek.vie.app.provider.impl.player;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.pojo.dataquery.PlayAudio;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.RestUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import org.anydrill.calculate.set.ResultSet;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Component
@Path("download")
@RestController
public class download {
   private static final Logger logger = LoggerFactory.getLogger(download.class);
   private ExcuteContext excuteContext = new ExcuteContext();

   @POST
   @Path("test")
   @Produces("application/json")
   @Consumes("application/json")
   @RequestMapping("/download/getAudio")
   public void getAudio(@Context HttpServletRequest request, HttpServletResponse response) throws Exception {
      String functionName = "getAudio()";
      long startTime1 = System.currentTimeMillis();
      Map<String, String[]> parameters_map = request.getParameterMap();
      String voiceId = "";
      String dataSource = "";
      String insightType = "";

      try {
         String paramters_check_result = this.checkParameters(parameters_map);
         if (!paramters_check_result.equals("true")) {
            JSONObject return_json = new JSONObject();
            return_json.put("ErrorParametersMessage", paramters_check_result);
            response.getOutputStream().write(return_json.toString().getBytes());
            response.getOutputStream().flush();
            response.getOutputStream().close();
            logger.info(functionName + "已通知传参错误信息");
            return;
         }

         logger.info(functionName + "校验入参通过");
         voiceId = String.valueOf(parameters_map.get("voiceId")[0]);
         dataSource = String.valueOf(parameters_map.get("dataSource")[0]);
         insightType = String.valueOf(parameters_map.get("insightType")[0]);
         logger.info(functionName + "获得 voiceId=" + voiceId + ",dataSource=" + dataSource + ",insightType=" + insightType);
      } catch (Exception var18) {
         logger.error("接口获取 voiceId, dataSource ,insightType入参异常", var18);
      }

      new LinkedHashMap();
      String audio_data = "";

      try {
         if ("0".equals(insightType)) {
            logger.info("insightType=0表示按[录音]，将查找对应录音信息");
            LinkedHashMap<String, Object> var20 = this.getDatasFrom_ES_byVoice(voiceId, dataSource, insightType);
            audio_data = this.sendHttpRequest(var20);
         } else {
            if (!"1".equals(insightType)) {
               String errorMessage = "insightTpye can only equal to 0 or 1, please check it.";
               this.getErrorMessage(response, errorMessage);
               logger.error("insightType值既不是0也不是1，系统没有对应分类。请确认传递参数是否正确。");
               return;
            }

            logger.info("insightType=1表示按[任务]，将查找对应录音信息");
            LinkedHashMap<String, Object> var21 = this.getDatasFrom_ES_byTask(voiceId, dataSource, insightType);
            audio_data = this.sendHttpRequest(var21);
         }
      } catch (Exception var17) {
         logger.error(functionName + "从ES查找数据、请求音频异常", var17);
         throw new Exception(functionName + "从ES查找数据、请求音频异常", var17);
      }

      new PlayAudio();
      PlayAudio dataMap = this.package_audio(audio_data);
      response = this.setHttpResponse(response, dataMap, voiceId);
      response.getOutputStream().write(dataMap.getPlayBytes());
      response.getOutputStream().flush();
      response.getOutputStream().close();
      long endTime1 = System.currentTimeMillis();
      long timeSpan1 = endTime1 - startTime1;
      logger.info(functionName + "下载传输 voiceId=" + voiceId + "的录音【成功】,总共耗时" + timeSpan1 + "ms");
   }

   private void writeBinaryDatasIntoFile(byte[] bytes, String path, String name) {
      try {
         File file = new File(path, name);
         FileOutputStream fileOutputStream = new FileOutputStream(file);
         byte[] b = new byte[1024];
         int nRead = 0;
         fileOutputStream.write(bytes, 0, bytes.length);
         fileOutputStream.flush();
         fileOutputStream.close();
      } catch (Exception var8) {
         logger.error("录音保存到provider所在服务器失败", var8);
      }
   }

   private HttpServletResponse setHttpResponse(HttpServletResponse response, PlayAudio dataMap, String voiceId) {
      response.setContentType("audio/wav");
      response.addHeader("Content-Length", String.valueOf(dataMap.getPlayBytes().length));
      response.addHeader("Accept-Ranges", "bytes");
      response.addHeader("Content-Range", dataMap.getContentRange());
      String headerKey = "Content-Disposition";
      String headerValue = String.format("attachment; filename=\"%s\"", voiceId + ".wav");
      response.setHeader(headerKey, headerValue);
      return response;
   }

   private PlayAudio package_audio(String audio_data) throws Exception {
      String functionName = "package_audio()";
      PlayAudio dataMap = new PlayAudio();
      byte[] bys2 = null;
      Map<String, Object> vgsData = (Map<String, Object>)JSON.parseObject(audio_data, Map.class);

      try {
         bys2 = Base64.decodeBase64(vgsData.get("vgsAudioData").toString());
      } catch (Exception var8) {
         logger.error(functionName + "录音解码错误");
         throw new IOException(functionName + "录音解码错误", var8);
      }

      dataMap.setContentLength(String.valueOf(bys2.length));
      dataMap.setPlayBytes(bys2);
      return dataMap;
   }

   private String sendHttpRequest(LinkedHashMap<String, Object> datasFrom_ES) throws Exception {
      String functionName = "sendHttpRequest()";
      String voiceUri = String.valueOf(datasFrom_ES.get("voiceUri"));
      String machineId = String.valueOf(datasFrom_ES.get("machineId"));
      RestUtil restUtil = new RestUtil();
      Hashtable<String, Object> paramsInfo = new Hashtable<>();
      paramsInfo.put("voicePath", voiceUri);
      paramsInfo.put("macTag", machineId);
      ObjectMapper mapper = new ObjectMapper();
      String par = "";

      try {
         par = mapper.writeValueAsString(paramsInfo);
      } catch (JsonProcessingException var11) {
         logger.error(functionName + "Hashtable转String出错", var11);
         throw new Exception(functionName + "Hashtable转String出错", var11);
      }

      String http_url = "http://" + machineId + "/voiceService/getAudioBinaryDatas";
      logger.info(functionName + "发送http[请求录音]参数为 par=" + par + ",url=" + http_url);
      String data = restUtil.postRequst(http_url, par);
      if (data != null && !data.equals("")) {
         return data;
      } else {
         logger.error(functionName + "请求返回后的音频数据为空，录音不存在。");
         throw new Exception(functionName + "请求返回后的音频数据为空，录音不存在。");
      }
   }

   private LinkedHashMap<String, Object> getDatasFrom_ES_byVoice(String voiceId, String dataSource, String insightType) throws Exception {
      String functionName = "getDatasFrom_ES_byVoice()";
      String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
      String execute_sql = "select voiceId,machineId,voiceUri from " + dataSource + " where voiceId='" + voiceId + "'";
      logger.info(functionName + " execute_sql :" + execute_sql + ",anydrill url :" + url);
      LinkedHashMap<String, Object> dataFrom_ES = new LinkedHashMap<>();

      try {
         ResultSet rs = this.excuteContext.executeSearchQuery(url, execute_sql);
         if (rs != null && rs.getTotalCount() > 0L) {
            dataFrom_ES = (LinkedHashMap<String, Object>)rs.toList().get(0);
            String machineId = String.valueOf(dataFrom_ES.get("machineId"));
            String voiceUri = String.valueOf(dataFrom_ES.get("voiceUri"));
            logger.info(functionName + " 从ES得到 machineId=" + machineId + ",voiceUri=" + voiceUri);
            return dataFrom_ES;
         } else {
            logger.info(functionName + "从ES中没有找到数据");
            return dataFrom_ES;
         }
      } catch (Exception var11) {
         logger.error(functionName + "从ES查找数据异常", var11);
         throw new Exception(functionName + "从ES查找数据异常", var11);
      }
   }

   private LinkedHashMap<String, Object> getDatasFrom_ES_byTask(String voiceId, String dataSource, String insightType) throws Exception {
      String functionName = "getDatasFrom_ES_byTask()";
      String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
      String execute_sql = "select taskId,voiceId,childVoiceId,childMachineId,childVoiceUri,childTimeFormat from "
         + dataSource
         + " where childVoiceId='"
         + voiceId
         + "'";
      logger.info(functionName + " execute_sql :" + execute_sql + ",anydrill url :" + url);
      new LinkedHashMap();
      new ArrayList();
      String all_voiceId = "";
      String taskId = "";
      LinkedHashMap<String, Object> return_result = new LinkedHashMap<>();

      try {
         ResultSet rs = this.excuteContext.executeSearchQuery(url, execute_sql);
         if (rs != null && rs.getTotalCount() > 0L) {
            LinkedHashMap<String, Object> dataFrom_ES = (LinkedHashMap)rs.toList().get(0);
            ArrayList<HashMap> child_fields = (ArrayList)dataFrom_ES.get("child_fields");
            all_voiceId = String.valueOf(dataFrom_ES.get("voiceId"));
            taskId = String.valueOf(dataFrom_ES.get("taskId"));
            int voicePositionOfTheTask = this.getAudioPositionOfTask(voiceId, taskId, dataSource, all_voiceId);
            HashMap<String, String> target_audio = (HashMap<String, String>)child_fields.get(voicePositionOfTheTask);
            String target_audio_childMachineId = String.valueOf(target_audio.get("childMachineId")) == null
               ? ""
               : String.valueOf(target_audio.get("childMachineId"));
            String target_audio_childVoiceUri = String.valueOf(target_audio.get("childVoiceUri")) == null
               ? ""
               : String.valueOf(target_audio.get("childVoiceUri"));
            if (this.checkIsNotEmpty(target_audio_childMachineId, target_audio_childVoiceUri)) {
               logger.info(functionName + "目标音频 childMachineId=" + target_audio_childMachineId + ",childVoiceUri=" + target_audio_childVoiceUri);
               return_result.put("machineId", target_audio_childMachineId);
               return_result.put("voiceUri", target_audio_childVoiceUri);
               return return_result;
            } else {
               logger.error(
                  functionName + "目标音频 childMachineId=" + target_audio_childMachineId + ",childVoiceUri=" + target_audio_childVoiceUri + "至少有一个空值，无法找到录音"
               );
               throw new Exception(functionName + "目标音频 childMachineId,childVoiceUri 有空值");
            }
         } else {
            logger.info(functionName + "从ES中没有找到数据");
            return return_result;
         }
      } catch (Exception var17) {
         logger.error(functionName + "服务内部异常", var17);
         throw new Exception(functionName + "服务内部异常", var17);
      }
   }

   private boolean checkIsNotEmpty(String target_audio_childMachineId, String target_audio_childVoiceUri) {
      return !target_audio_childMachineId.equals("") && !target_audio_childVoiceUri.equals("");
   }

   private int getAudioPositionOfTask(String voiceId, String taskId, String dataSource, String all_voiceId) throws Exception {
      int voicePositionOfTheTask = -1;
      String[] voiceIds = all_voiceId.split("\\$");

      for (int i = 0; i < voiceIds.length; i++) {
         if (voiceId.equals(String.valueOf(voiceIds[i]))) {
            voicePositionOfTheTask = i;
            break;
         }
      }

      if (voicePositionOfTheTask == -1) {
         logger.error("在数据源为" + dataSource + "的ES中没有找到对应的 childVoiceId，taskId=" + taskId + ",voiceId=" + all_voiceId + ",childVoiceId=" + voiceId);
         throw new Exception("任务号下没有找到对应录音，taskId=" + taskId + ",childVoiceId=" + voiceId);
      } else {
         return voicePositionOfTheTask;
      }
   }

   private String checkParameters(Map<String, String[]> parameters_map) {
      String functionName = "checkParameters()";
      int voiceId_flag = 0;
      int dataSource_flag = 0;
      int insightType_flag = 0;
      logger.info(functionName + "收到参数为" + this.mapToString(parameters_map));

      for (String key : parameters_map.keySet()) {
         if (key.equals("voiceId")) {
            voiceId_flag = 1;
         }

         if (key.equals("dataSource")) {
            dataSource_flag = 1;
         }

         if (key.equals("insightType")) {
            insightType_flag = 1;
         }
      }

      String return_message = "";
      if (voiceId_flag == 0) {
         return_message = return_message + "'voiceId' parameter error,";
      }

      if (dataSource_flag == 0) {
         return_message = return_message + "'dataSource' parameter error,";
      }

      if (insightType_flag == 0) {
         return_message = return_message + "'insightType' parameter error,";
      }

      if (insightType_flag == 1) {
         String insightType = parameters_map.get("insightType")[0];
         if (!insightType.equals("0") && !insightType.equals("1")) {
            return_message = return_message + "'insightType' can only equal to 0 or 1,please check it,";
         }
      }

      return return_message.equals("") ? "true" : return_message.substring(0, return_message.length() - 1);
   }

   private String mapToString(Map<String, String[]> parameters_map) {
      String str_result = "";

      for (String key : parameters_map.keySet()) {
         str_result = str_result + key + ":" + parameters_map.get(key)[0] + ",";
      }

      return str_result.substring(0, str_result.length() - 1);
   }

   private void getErrorMessage(HttpServletResponse response, String errorMessage) throws IOException {
      String functionName = "getErrorMessage()";
      JSONObject return_json = new JSONObject();
      return_json.put("ErrorParametersMessage", errorMessage);

      try {
         response.getOutputStream().write(return_json.toString().getBytes());
         response.getOutputStream().flush();
         response.getOutputStream().close();
         logger.info(functionName + "已通知传参错误信息");
      } catch (IOException var6) {
         logger.error(functionName + "错误信息流写出出错", var6);
         throw new IOException(functionName + "错误信息流写出出错");
      }
   }

   public ExcuteContext getExcuteContext() {
      return this.excuteContext;
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
   }
}
