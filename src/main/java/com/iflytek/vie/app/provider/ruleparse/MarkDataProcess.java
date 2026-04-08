package com.iflytek.vie.app.provider.ruleparse;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkDataProcess implements Serializable {
   private final Logger logger = LoggerFactory.getLogger(MarkDataProcess.class);
   private String order;
   private String orderType = "asc";
   private int desc;
   private String[] anydrillHost;
   private MarkCountParser yMarkCountParser;
   private MarkCountParser nMarkCountParser;
   private MarkDetailParser markDetailParser;
   private MarkDetailParser existMarkDetailParser;
   private List<DataInfo> dataInfos = new ArrayList<>();
   private Map<String, DataInfo> dataInfoMap = new HashMap<>();
   private boolean yRun = true;
   private boolean nRun = true;
   private boolean run = true;
   private float percent = 0.0F;
   private int yExist = 0;
   private int nExist = 0;
   private int yNoExist = 0;
   private int nNoExist = 0;
   private int searchType;
   private List<String> ids;
   private List<DimensionConfig> dimensionConfigs;
   private Map<String, List<Map<String, Object>>> silenceKeywordMap = new HashMap<>();
   private String dataLimit;
   private String channelRule;
   private final DecimalFormat NDF = new DecimalFormat("0.000");
   private final DecimalFormat df = new DecimalFormat("0.00");

   public MarkDataProcess(
      String textRule,
      int channel,
      List<Filter> filters,
      String tableName,
      int batchSize,
      String order,
      String orderType,
      String columns,
      List<String> yesIds,
      List<String> noIds,
      List<String> ids,
      String[] anydrillHost,
      int searchType,
      String paseRule,
      String objectRule,
      String textColumn,
      String quaCondition,
      List<DimensionConfig> dimensionConfigs,
      String dataLimit,
      String textFilter,
      String channelRule
   ) {
      int idSize = 0;
      if (yesIds == null || yesIds.size() == 0) {
         this.yRun = false;
      }

      if (noIds == null || noIds.size() == 0) {
         this.nRun = false;
      }

      if (ids != null && ids.size() != 0) {
         idSize = ids.size();
      } else {
         this.run = false;
      }

      if (anydrillHost != null) {
         this.anydrillHost = Arrays.copyOf(anydrillHost, anydrillHost.length);
      } else {
         this.anydrillHost = null;
      }

      this.yMarkCountParser = new MarkCountParser(textRule, channel, filters, tableName, yesIds, searchType, paseRule, objectRule, textColumn, quaCondition);
      this.nMarkCountParser = new MarkCountParser(textRule, channel, filters, tableName, noIds, searchType, paseRule, objectRule, textColumn, quaCondition);
      this.markDetailParser = new MarkDetailParser(
         textRule,
         channel,
         filters,
         tableName,
         batchSize,
         idSize,
         columns.replace("[", "").replace("]", ""),
         ids,
         true,
         searchType,
         paseRule,
         objectRule,
         textColumn,
         quaCondition,
         dataLimit,
         textFilter,
         channelRule
      );
      this.existMarkDetailParser = new MarkDetailParser(
         textRule,
         channel,
         filters,
         tableName,
         500000,
         idSize,
         "id",
         ids,
         false,
         searchType,
         paseRule,
         objectRule,
         textColumn,
         quaCondition,
         dataLimit,
         textFilter,
         channelRule
      );
      this.order = order;
      if ("desc".equals(orderType)) {
         this.desc = 1;
      } else {
         this.desc = 0;
      }

      this.ids = ids;
      this.searchType = searchType;
      this.dimensionConfigs = dimensionConfigs;
      this.dataLimit = dataLimit;
   }

   public void process(String dataSource) {
      PreparedStatement pst = new PreparedStatement(this.anydrillHost);
      Set<String> yIdSet = new HashSet<>();
      FilterParser.listToSet(this.yMarkCountParser.getIds(), yIdSet);

      for (String sql : (List<String>)this.existMarkDetailParser.parseSql(dataSource)) {
         this.logger.info("the sql is " + sql);
         ResultSet rs = pst.executeQuery(sql);
         this.existIdProcess(rs, this.ids, yIdSet);
      }

      for (String sql : (List<String>)this.markDetailParser.parseSql(dataSource)) {
         this.logger.info("the exiest sql is " + sql);
         this.dataInfoProcess(pst.executeQuery(sql));
      }

      Collections.sort(this.dataInfos, new DataInfoComprators(this.order, this.desc));
      this.logger.info("the dataInfos size is " + this.dataInfos.size());
   }

   public void processSilence(String dataSource) {
      PreparedStatement pst = new PreparedStatement(this.anydrillHost);
      Set<String> yIdSet = new HashSet<>();
      FilterParser.listToSet(this.yMarkCountParser.getIds(), yIdSet);

      for (String sql : (List<String>)this.existMarkDetailParser.parseSilenceSql()) {
         System.out.println("exist sql:" + sql);
         ResultSet rs = pst.executeQuery(sql);
         this.existSilenceIdProcess(rs, this.ids, yIdSet);
      }

      for (String sql : (List<String>)this.markDetailParser.parseSql(dataSource)) {
         System.out.println("exist data sql:" + sql);
         this.dataInfoProcess(pst.executeQuery(sql));
      }

      Collections.sort(this.dataInfos, new DataInfoComprators(this.order, this.desc));
   }

   private void existSilenceIdProcess(ResultSet rs, List<String> ids, Set<String> yIdSet) {
      if (rs != null && rs.getDataMiningResult() != null) {
         JSONObject jsonObject = JSONObject.parseObject((String)rs.getDataMiningResult());
         JSONArray detail = jsonObject.getJSONArray("detail");
         Iterator<Object> speechList = detail.iterator();

         while (speechList.hasNext()) {
            DataInfo dataInfo = new DataInfo();
            JSONObject speech = (JSONObject)speechList.next();
            String id = speech.get("id").toString();
            dataInfo.setId(id);
            dataInfo.setIsExist(1);
            boolean isY = yIdSet.contains(id);
            dataInfo.setMark(isY ? 1 : -1);
            if (isY) {
               this.yExist++;
            } else {
               this.nExist++;
            }

            this.parseTextKeyWord(id, (JSONArray)speech.get("keyword"));
            this.dataInfoMap.put(id, dataInfo);
         }
      }

      if (ids != null && ids.size() > 0) {
         for (String markId : ids) {
            if (!this.dataInfoMap.containsKey(markId)) {
               DataInfo dataInfo = new DataInfo();
               dataInfo.setId(markId);
               dataInfo.setIsExist(0);
               boolean isY = yIdSet.contains(markId);
               if (isY) {
                  this.yNoExist++;
               } else {
                  this.nNoExist++;
               }

               dataInfo.setMark(isY ? 1 : -1);
               this.dataInfoMap.put(markId, dataInfo);
            }
         }
      }

      this.caculatePercent(this.yExist, this.yExist + this.nExist);
   }

   private List<String> forGetDurationConfig() {
      List<String> durationConfig = new ArrayList<>();
      if (this.dimensionConfigs != null && this.dimensionConfigs.size() > 0) {
         for (int i = 0; i < this.dimensionConfigs.size(); i++) {
            DimensionConfig dc = this.dimensionConfigs.get(i);
            if (1 == dc.getFlag() || 2 == dc.getFlag()) {
               durationConfig.add(dc.getIndexField());
            }
         }
      }

      return durationConfig;
   }

   private List<Map<String, Object>> parseTextKeyWord(String id, JSONArray keyWordArray) {
      List<Map<String, Object>> textKeyWordMap = new ArrayList<>();
      if (keyWordArray != null && keyWordArray.size() > 0) {
         for (int i = 0; i < keyWordArray.size(); i++) {
            Map<String, Object> keyWordMap = new HashMap<>();
            String keyWordStr = String.valueOf(keyWordArray.get(i));
            String[] keyWords = keyWordStr.split("-");
            keyWordMap.put("word", keyWords[0]);
            keyWordMap.put("begin", Integer.parseInt(keyWords[1]));
            keyWordMap.put("end", Integer.parseInt(keyWords[2]));
            textKeyWordMap.add(keyWordMap);
         }

         this.silenceKeywordMap.put(id, textKeyWordMap);
      }

      return textKeyWordMap;
   }

   public int hitCountProcess(PreparedStatement pst, MarkCountParser markCountParser, String dataSource) {
      String sql = String.valueOf(markCountParser.parseSql(dataSource));
      ResultSet rs = pst.executeQuery(sql);
      if (rs == null || rs.getTotalCount() == 0L) {
         return 0;
      } else if (rs.hasRow()) {
         rs.next();
         return rs.getValue("hitCount") == null ? 0 : (int)((Double)rs.getValue("hitCount")).doubleValue();
      } else {
         return 0;
      }
   }

   public void caculatePercent(int yHitCount, int totalHitCount) {
      if (totalHitCount == 0) {
         this.percent = 0.0F;
      } else {
         this.percent = (float)yHitCount / totalHitCount * 100.0F;
         this.percent = Math.round(this.percent * 100.0F) / 100.0F;
      }
   }

   public void existIdProcess(ResultSet rs, List<String> ids, Set<String> yIdSet) {
      if (rs != null) {
         while (rs.hasRow()) {
            DataInfo dataInfo = new DataInfo();
            rs.next();
            String id = String.valueOf(rs.getValue("id"));
            dataInfo.setId(id);
            dataInfo.setIsExist(1);
            boolean isY = yIdSet.contains(id);
            dataInfo.setMark(isY ? 1 : -1);
            if (isY) {
               this.yExist++;
            } else {
               this.nExist++;
            }

            this.dataInfoMap.put(id, dataInfo);
         }
      }

      if (ids != null && ids.size() > 0) {
         for (String markId : ids) {
            if (!this.dataInfoMap.containsKey(markId)) {
               DataInfo dataInfo = new DataInfo();
               dataInfo.setId(markId);
               dataInfo.setIsExist(0);
               boolean isY = yIdSet.contains(markId);
               if (isY) {
                  this.yNoExist++;
               } else {
                  this.nNoExist++;
               }

               dataInfo.setMark(isY ? 1 : -1);
               this.dataInfoMap.put(markId, dataInfo);
            }
         }
      }

      this.caculatePercent(this.yExist, this.yExist + this.nExist);
   }

   public void dataInfoProcess(ResultSet rs) {
      if (rs != null && rs.getTotalCount() != 0L) {
         List<String> durationConfig = this.forGetDurationConfig();

         for (LinkedHashMap<String, Object> dataMap : rs.toList()) {
            String id = String.valueOf(dataMap.get("id"));
            DataInfo dataInfo = this.dataInfoMap.get(id);

            for (int mn = 0; mn < durationConfig.size(); mn++) {
               String indexField = durationConfig.get(mn);
               String currnt = dataMap.get(indexField) == null ? null : String.valueOf(dataMap.get(indexField));
               if (currnt != null) {
                  if (!"n0avgSpeed".equals(indexField) && !"n1avgSpeed".equals(indexField)) {
                     dataMap.put(indexField, this.toSecond(currnt));
                  } else {
                     dataMap.put(indexField, this.toSecondTwo(currnt));
                  }
               }
            }

            if (dataInfo != null) {
               dataInfo.setDataMaps(dataMap);
               this.logger.info("add dataInfos first");
               this.dataInfos.add(dataInfo);
            }
         }
      } else {
         this.logger.info("the search result is null");
      }
   }

   public String toSecond(String millSecond) {
      if (millSecond == null) {
         return "0";
      } else {
         try {
            return this.NDF.format(this.NDF.parse(millSecond).floatValue() / 1000.0F);
         } catch (ParseException var3) {
            var3.printStackTrace();
            return "0";
         }
      }
   }

   public String toSecondTwo(String millSecond) {
      if (millSecond != null && !"".equals(millSecond) && !"null".equals(millSecond)) {
         try {
            return this.df.format(this.df.parse(millSecond).floatValue() / 1000.0F);
         } catch (ParseException var3) {
            return "0";
         }
      } else {
         return "0";
      }
   }

   public List<DataInfo> getDataInfos() {
      return this.dataInfos;
   }

   public void setDataInfos(List<DataInfo> dataInfos) {
      this.dataInfos = dataInfos;
   }

   public float getPercent() {
      return this.percent;
   }

   public void setPercent(float percent) {
      this.percent = percent;
   }

   public int getyExist() {
      return this.yExist;
   }

   public void setyExist(int yExist) {
      this.yExist = yExist;
   }

   public int getnExist() {
      return this.nExist;
   }

   public void setnExist(int nExist) {
      this.nExist = nExist;
   }

   public int getyNoExist() {
      return this.yNoExist;
   }

   public void setyNoExist(int yNoExist) {
      this.yNoExist = yNoExist;
   }

   public int getnNoExist() {
      return this.nNoExist;
   }

   public void setnNoExist(int nNoExist) {
      this.nNoExist = nNoExist;
   }

   public Map<String, List<Map<String, Object>>> getSilenceKeywordMap() {
      return this.silenceKeywordMap;
   }

   public void setSilenceKeywordMap(Map<String, List<Map<String, Object>>> silenceKeywordMap) {
      this.silenceKeywordMap = silenceKeywordMap;
   }
}
