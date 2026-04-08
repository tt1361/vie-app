package com.iflytek.vie.app.provider.ruleparse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.pojo.model.ModelInfo;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.TopicMarkDB;
import com.iflytek.vie.app.provider.pojo.TopicMarkInfo;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.utils.LoadCommonConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushReadCallProcess implements Serializable {
   private final Logger logger = LoggerFactory.getLogger(PushReadCallProcess.class);
   private static final long serialVersionUID = 4237266893198633624L;
   private String columns;
   private PushReadCallDetailParser topicDetailParser;
   private int count = 0;
   private int dimCount = 0;
   private List<com.iflytek.vie.app.pojo.topicgroup.DataInfo> dataInfos = new ArrayList<>();
   private ModelDB modelDB;
   private TopicMarkDB topicMarkDB;
   private int topicId;
   private Map<Long, List<Long>> modelGroupMap;
   private List<Long> modelIdList;
   private String tableName;

   public PushReadCallProcess() {
   }

   public PushReadCallProcess(
      List<Filter> filters,
      String tableName,
      int pageNum,
      int pageSize,
      String order,
      String orderType,
      String columns,
      ModelDB modelDb,
      TopicMarkDB topicMarkDb,
      List<String> callIdList,
      List<Long> modelIdList,
      int topicId,
      Map<Long, List<Long>> modelGroupMap,
      int topicGroupType,
      Map<String, String> groupModelMap,
      String dataTypeDimension,
      String multipleDataSql
   ) {
      this.topicDetailParser = new PushReadCallDetailParser(
         filters,
         tableName,
         order,
         orderType,
         columns,
         pageNum,
         pageSize,
         callIdList,
         modelIdList,
         topicId,
         topicGroupType,
         groupModelMap,
         dataTypeDimension,
         multipleDataSql
      );
      this.columns = columns;
      this.modelDB = modelDb;
      this.topicMarkDB = topicMarkDb;
      this.topicId = topicId;
      this.modelGroupMap = modelGroupMap;
      this.modelIdList = modelIdList;
      this.tableName = tableName;
   }

   public void process() throws Exception {
      Map<String, String> map = (Map<String, String>)this.topicDetailParser.parseSql(this.tableName);
      String type = map.get("index");
      String sql = map.get("sql");
      String[] anydrillHost = null;
      if (type.equals("main")) {
         anydrillHost = LoadCommonConfig.get("mainAnydrillAddress").split(";");
      } else {
         anydrillHost = LoadCommonConfig.get("modelAnydrillAddress").split(";");
      }

      this.logger.info(anydrillHost.toString());
      PreparedStatement pst = new PreparedStatement(anydrillHost);
      this.logger.info("执行detailSql：" + sql);
      ResultSet rs = pst.executeQuery(sql);
      this.detailResultProcess(rs);
      this.dimCount = (int)rs.getTotalCount();
   }

   public void detailResultProcess(ResultSet rs) throws Exception {
      if (rs != null && rs.getTotalCount() != 0L) {
         List<LinkedHashMap<String, Object>> detailMaps = rs.toList();
         String modelInfo = "offLineTagId,offLineTagInfo";
         String voiceStr = IndexConstants.getIdStr(detailMaps);
         String[] anydrillAddressModel = LoadCommonConfig.get("modelAnydrillAddress").split(";");
         Map<String, List<Map<String, Object>>> map = IndexConstants.getModelInforById(anydrillAddressModel, this.tableName, modelInfo, voiceStr, null);
         if (detailMaps != null && detailMaps.size() > 0) {
            List<String> ids = new ArrayList<>();

            for (LinkedHashMap<String, Object> result : detailMaps) {
               ids.add(String.valueOf(result.get("id")));
            }

            Map<String, TopicMarkInfo> topicMarkInfoMap = this.topicMarkDB.getMarkInfo(ids, this.topicId);
            String modelIdListStr = null;
            String modelName = null;
            TopicMarkInfo topicMarkInfo = null;
            List<Map<String, Object>> keyWordInfo = null;
            List<HashMap<String, Object>> modelList = new ArrayList<>();

            for (LinkedHashMap<String, Object> result : detailMaps) {
               modelList.clear();
               List<Map<String, Object>> modeInfoList = null;
               if (map.containsKey(String.valueOf(result.get("id")))) {
                  for (Map<String, Object> map2 : map.get(String.valueOf(result.get("id")))) {
                     HashMap<String, Object> modelMap = new HashMap<>();
                     int offLineTagId = Integer.parseInt(String.valueOf(map2.get("offLineTagId")));
                     String offLineTagInfo = (String)map2.get("offLineTagInfo");
                     modelMap.put("offLineTagId", offLineTagId);
                     modelMap.put("offLineTagInfo", offLineTagInfo);
                     this.logger.info("offLineTagId={}", offLineTagId);
                     modelList.add(modelMap);
                  }

                  modelName = this.getModelName(modelList);
                  modelIdListStr = this.getModelIdListstr(modelList);
                  keyWordInfo = this.getKeyWord(modelList);
               }

               result.put("modelName", modelName);
               result.put("modelIdList", modelIdListStr);
               if (topicMarkInfoMap != null && topicMarkInfoMap.size() > 0) {
                  topicMarkInfo = topicMarkInfoMap.remove(String.valueOf(result.get("id")));
               }

               boolean isHaveMark = false;
               String mark = "";
               Integer markId = null;
               if (topicMarkInfo != null) {
                  isHaveMark = true;
                  mark = topicMarkInfo.getMark();
                  markId = topicMarkInfo.getId();
               }

               result.put("remark", mark);
               result.put("isHaveRemark", isHaveMark);
               result.put("markId", markId);
               String id = String.valueOf(result.get("id"));
               String duration = result.get("duration") == null ? null : String.valueOf(result.get("duration"));
               if (duration != null) {
                  result.put("duration", this.toSecond(Long.parseLong(duration)));
               }

               String silenceLong = result.get("silenceLong") == null ? null : String.valueOf(result.get("silenceLong"));
               if (silenceLong != null) {
                  result.put("silenceLong", this.toSecond(Long.parseLong(silenceLong)));
               }

               String inVadDuration = result.get("inVadDuration") == null ? null : String.valueOf(result.get("inVadDuration"));
               if (inVadDuration != null) {
                  result.put("inVadDuration", this.toSecond(Long.parseLong(inVadDuration)));
               }

               result.put("isRead", this.topicId != 0);
               com.iflytek.vie.app.pojo.topicgroup.DataInfo dataInfo = new com.iflytek.vie.app.pojo.topicgroup.DataInfo(id, 0, 0);
               dataInfo.setKeywordInfos(keyWordInfo);
               dataInfo.setDataMaps(result);
               this.dataInfos.add(dataInfo);
            }
         }
      }
   }

   private String getModelIdListstr(List<HashMap<String, Object>> modelIdList) {
      if (this.topicId == -1) {
         return "";
      } else {
         StringBuffer modelName = new StringBuffer();
         List<Long> beforeModelIdList = new ArrayList<>();

         for (HashMap<String, Object> modelIdHm : (List<HashMap<String, Object>>)modelIdList) {
            if (!modelIdHm.get("offLineTagId").toString().isEmpty()) {
               long modelId = Long.parseLong(modelIdHm.get("offLineTagId").toString());
               if (!beforeModelIdList.contains(modelId)) {
                  beforeModelIdList.add(modelId);
                  List<Long> groups = this.modelGroupMap.get(modelId);
                  if (groups != null && !groups.isEmpty()) {
                     for (Long group : groups) {
                        modelName.append(modelId).append(",").append(group).append("|");
                     }
                  }
               }
            }
         }

         return modelName.substring(0, modelName.length() - 1);
      }
   }

   private List<Map<String, Object>> getKeyWord(List<HashMap<String, Object>> keyWordInfo) throws Exception {
      ObjectMapper mapper = new ObjectMapper();
      List<Map<String, Object>> list = new ArrayList<>();

      for (HashMap<String, Object> hm : (List<HashMap<String, Object>>)keyWordInfo) {
         if (hm.get("offLineTagInfo") != null && !hm.get("offLineTagInfo").equals("[]")) {
            for (HashMap<String, Object> hmss : (List<HashMap<String, Object>>)mapper.readValue(hm.get("offLineTagInfo").toString(), List.class)) {
               Map<String, Object> hms = new HashMap<>();
               hms.put("word", hmss.get("content"));
               hms.put("begin", hmss.get("beginTime"));
               hms.put("end", hmss.get("endTime"));
               if (!list.contains(hms)) {
                  list.add(hms);
               }
            }
         }
      }

      return list;
   }

   private String getModelName(List<HashMap<String, Object>> modelIdLists) {
      String modelName = "";
      List<Long> modelList = new ArrayList<>();

      for (HashMap<String, Object> modelIdHm : (List<HashMap<String, Object>>)modelIdLists) {
         if (!modelIdHm.get("offLineTagId").toString().isEmpty()) {
            long modelId = Long.parseLong(modelIdHm.get("offLineTagId").toString());
            if (this.modelIdList != null && this.modelIdList.contains(modelId)) {
               modelList.add(modelId);
            }
         }
      }

      if (modelList.isEmpty()) {
         modelList.add(0L);
      }

      List<ModelInfo> modelInfoList = this.modelDB.queryModelByIds(modelList);
      if (modelInfoList != null && !modelInfoList.isEmpty()) {
         for (ModelInfo mf : modelInfoList) {
            modelName = modelName + mf.getModelName() + ",";
         }
      }

      if (modelName.length() > 1) {
         modelName = modelName.substring(0, modelName.length() - 1);
      }

      return modelName;
   }

   public long toSecond(long millSecond) {
      return millSecond == 0L ? 0L : Math.round((float)millSecond / 1000.0F);
   }

   public int getDimCount() {
      return this.dimCount;
   }

   public void setDimCount(int dimCount) {
      this.dimCount = dimCount;
   }

   public int getCount() {
      return this.count;
   }

   public void setCount(int count) {
      this.count = count;
   }

   public String getColumns() {
      return this.columns;
   }

   public void setColumns(String columns) {
      this.columns = columns;
   }

   public List<com.iflytek.vie.app.pojo.topicgroup.DataInfo> getDataInfos() {
      return this.dataInfos;
   }

   public void setDataInfos(List<com.iflytek.vie.app.pojo.topicgroup.DataInfo> dataInfos) {
      this.dataInfos = dataInfos;
   }

   public Map<Long, List<Long>> getModelGroupMap() {
      return this.modelGroupMap;
   }

   public void setModelGroupMap(Map<Long, List<Long>> modelGroupMap) {
      this.modelGroupMap = modelGroupMap;
   }

   public List<Long> getModelIdList() {
      return this.modelIdList;
   }

   public void setModelIdList(List<Long> modelIdList) {
      this.modelIdList = modelIdList;
   }

   public String getTableName() {
      return this.tableName;
   }

   public void setTableName(String tableName) {
      this.tableName = tableName;
   }
}
