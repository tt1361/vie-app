package com.iflytek.vie.app.provider.ruleparse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.utils.StringUtils;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;

public class PreviewProcessPlatform implements Serializable {
   private static final long serialVersionUID = 6588917774781322349L;
   private String columns;
   private String[] anydrillHost;
   private PreviewParser previewParser;
   private PreviewDetailParser previewDetailParser;
   private HitCountParserPlatform hitCountParser;
   private int count = 0;
   private int dimCount = 0;
   private List<DataInfoPlatform> dataInfos = new ArrayList<>();
   private final DecimalFormat NDF = new DecimalFormat("0.000");
   private final DecimalFormat df = new DecimalFormat("0.00");
   private int pageNum;
   private int pageSize;
   private boolean isVoiceType;
   private static final int BATCH_PAGE_NUM = 100000;
   private List<DimensionConfig> dimensionConfigs;
   private String orderTextRule;

   public PreviewProcessPlatform(
      String textRule,
      int channel,
      List<FilterPlatform> filters,
      String tableName,
      int pageNum,
      int pageSize,
      String order,
      String orderType,
      String columns,
      String[] anydrillHost,
      String textRules,
      String dataLimitSql,
      int searchType,
      String objecntRule,
      String textColumn,
      String quaCondition,
      String textFilter,
      List<DimensionConfig> dimensionConfigs,
      String channelRule
   ) {
      this.previewParser = new PreviewParser(
         textRule,
         channel,
         filters,
         tableName,
         pageNum,
         pageSize,
         order,
         orderType,
         textRules,
         dataLimitSql,
         objecntRule,
         textColumn,
         quaCondition,
         textFilter,
         channelRule
      );
      this.previewDetailParser = new PreviewDetailParser(textRule, channel, filters, tableName, order, orderType, columns, null, dataLimitSql);
      this.hitCountParser = new HitCountParserPlatform(textRule, channel, filters, tableName, true, dataLimitSql, "", searchType);
      this.anydrillHost = anydrillHost;
      this.isVoiceType = !StringUtils.isNullOrEmpry(objecntRule);
      this.columns = columns;
      this.pageNum = pageNum;
      this.pageSize = pageSize;
      this.dimensionConfigs = dimensionConfigs;
      this.orderTextRule = textRule;
   }

   public void process(String dataSource) {
      PreparedStatement pst = new PreparedStatement(this.anydrillHost);
      String sql = String.valueOf(this.previewParser.parseSql(dataSource));
      System.out.println("word sql is : " + sql);
      ResultSet rs = pst.executeQuery(sql);
      Map<String, List<Map<String, Object>>> idsMap = this.previewResultProcess(rs, dataSource);
      if (idsMap != null && idsMap.size() != 0) {
         this.previewDetailParser.setColumns(this.columns);
         List<String> ids = new ArrayList<>();

         for (String id : idsMap.keySet()) {
            ids.add(id);
         }

         this.previewDetailParser.setIds(ids);
         String detailSql = String.valueOf(this.previewDetailParser.parseSql(dataSource));
         System.out.println("the detail sql is :" + detailSql);
         rs = pst.executeQuery(detailSql);
         this.detailResultProcess(rs, idsMap, dataSource);
         String hitCountSql = String.valueOf(this.hitCountParser.parseSql(dataSource));
         System.out.println("the counts sql is " + hitCountSql);
         rs = pst.executeQuery(hitCountSql);
         this.filterHitCountProcess(rs);
      } else {
         String hitCountSql = String.valueOf(this.hitCountParser.parseSql(dataSource));
         rs = pst.executeQuery(hitCountSql);
         this.filterHitCountProcess(rs);
      }
   }

   public void processFragment(String dataSource) {
      PreparedStatement pst = new PreparedStatement(this.anydrillHost);
      String sql = String.valueOf(this.previewParser.parseFragmentSql(dataSource));
      System.out.println("word sql is : " + sql);
      ResultSet rs = pst.executeQuery(sql);
      Map<String, List<Map<String, Object>>> idsMap = this.previewResultProcess(rs, dataSource);
      if (idsMap != null && idsMap.size() != 0) {
         this.previewDetailParser.setColumns(this.columns);
         List<String> ids = new ArrayList<>();

         for (String id : idsMap.keySet()) {
            ids.add(id);
         }

         this.previewDetailParser.setIds(ids);
         String detailSql = String.valueOf(this.previewDetailParser.parseSql(dataSource));
         System.out.println("the detail sql is :" + detailSql);
         rs = pst.executeQuery(detailSql);
         this.detailResultProcess(rs, idsMap, dataSource);
         String hitCountSql = String.valueOf(this.hitCountParser.parseSql());
         System.out.println("the counts sql is " + hitCountSql);
         rs = pst.executeQuery(hitCountSql);
         this.filterHitCountProcess(rs);
      } else {
         String hitCountSql = String.valueOf(this.hitCountParser.parseSql());
         rs = pst.executeQuery(hitCountSql);
         this.filterHitCountProcess(rs);
      }
   }

   public void processSilenceExport(String dataSource) {
      PreparedStatement pst = new PreparedStatement(this.anydrillHost);
      String hitCountSql = String.valueOf(this.hitCountParser.parseSql(dataSource));
      System.out.println("the counts sql is : " + hitCountSql);
      ResultSet rs = pst.executeQuery(hitCountSql);
      this.filterHitCountProcess(rs);
      System.out.println("the counts are " + this.dimCount);
      if (this.dimCount != 0) {
         String sql = String.valueOf(this.previewParser.parseSql(dataSource));
         System.out.println("word sql is : " + sql);
         int page = 1;
         JSONArray jsonArray = new JSONArray();
         String searchSql = sql + " and rownum between 1 and " + page * this.pageSize;
         System.out.println("search sql: " + searchSql);
         rs = pst.executeQuery(searchSql);
         this.previewSilenceParser(jsonArray, rs);
         Map<String, List<Map<String, Object>>> idsMap = new HashMap<>();
         this.previewSilenceResultProcess(jsonArray, idsMap, dataSource);
         if (idsMap == null || idsMap.size() == 0) {
            return;
         }

         this.previewDetailParser.setColumns(this.columns);
         List<String> ids = new ArrayList<>();

         for (String id : idsMap.keySet()) {
            ids.add(id);
         }

         this.previewDetailParser.setIds(ids);
         String detailSql = String.valueOf(this.previewDetailParser.parseSql(dataSource));
         System.out.println("the detail sql is :" + detailSql);
         rs = pst.executeQuery(detailSql);
         this.detailResultProcess(rs, idsMap, dataSource);
      }
   }

   public void processSilence(String dataSource) {
      PreparedStatement pst = new PreparedStatement(this.anydrillHost);
      String hitCountSql = String.valueOf(this.hitCountParser.parseSql(dataSource));
      System.out.println("the counts sql is : " + hitCountSql);
      ResultSet rs = pst.executeQuery(hitCountSql);
      this.filterHitCountProcess(rs);
      System.out.println("the counts are " + this.dimCount);
      if (this.dimCount != 0) {
         String sql = String.valueOf(this.previewParser.parseSql(dataSource));
         System.out.println("word sql is : " + sql);
         int page = 1;
         int batchPageNum = 100000;
         int batchPageSize = (this.dimCount + batchPageNum - 1) / batchPageNum;

         JSONArray jsonArray;
         for (jsonArray = new JSONArray(); page <= batchPageSize; page++) {
            String searchSql = sql + " and rownum between " + ((page - 1) * batchPageNum + 1) + " and " + page * batchPageNum;
            System.out.println("search sql: " + searchSql);
            rs = pst.executeQuery(searchSql);
            this.previewSilenceParser(jsonArray, rs);
         }

         Map<String, List<Map<String, Object>>> idsMap = new HashMap<>();
         this.previewSilenceResultProcess(jsonArray, idsMap, dataSource);
         if (idsMap == null || idsMap.size() == 0) {
            return;
         }

         this.previewDetailParser.setColumns(this.columns);
         List<String> ids = new ArrayList<>();

         for (String id : idsMap.keySet()) {
            ids.add(id);
         }

         this.previewDetailParser.setIds(ids);
         String detailSql = String.valueOf(this.previewDetailParser.parseSql(dataSource));
         System.out.println("the detail sql is :" + detailSql);
         rs = pst.executeQuery(detailSql);
         this.detailResultProcess(rs, idsMap, dataSource);
      }
   }

   private void previewSilenceParser(JSONArray reslLists, ResultSet rs) {
      if (rs != null && rs.getDataMiningResult() != null) {
         Map<String, Object> map = (Map<String, Object>)JSON.parseObject(String.valueOf(rs.getDataMiningResult()), Map.class);
         this.count = this.count + Integer.parseInt(String.valueOf(map.get("count")));
         JSONArray jsonArray = (JSONArray)map.get("detail");
         reslLists.addAll(jsonArray);
      }
   }

   public Map<String, List<Map<String, Object>>> previewResultProcess(ResultSet rs, String dataSource) {
      Map<String, List<Map<String, Object>>> idsMap = new HashMap<>();
      if (rs != null && rs.getDataMiningResult() != null) {
         Map<String, Object> map = (Map<String, Object>)JSON.parseObject(String.valueOf(rs.getDataMiningResult()), Map.class);
         this.count = Integer.parseInt(String.valueOf(map.get("count")));
         if (this.count == 0) {
            return idsMap;
         } else {
            JSONArray jsonArray = (JSONArray)map.get("detail");
            JSONArray reslList = new JSONArray();
            if (this.isVoiceType) {
               if (jsonArray.size() == 1) {
                  reslList.addAll(jsonArray);
               } else if (this.pageNum * this.pageSize + 1 <= jsonArray.size()) {
                  reslList.addAll(jsonArray.subList((this.pageNum - 1) * this.pageSize, this.pageNum * this.pageSize));
               } else {
                  reslList.addAll(jsonArray.subList((this.pageNum - 1) * this.pageSize, jsonArray.size()));
               }
            } else {
               reslList.addAll(jsonArray);
            }

            for (int i = 0; i < reslList.size(); i++) {
               JSONObject jsonObject = (JSONObject)reslList.get(i);
               String id = String.valueOf(jsonObject.get("id"));
               JSONArray keyWordArr = jsonObject.get("text_keyword") != null ? (JSONArray)jsonObject.get("text_keyword") : (JSONArray)jsonObject.get("keyword");
               idsMap.put(id, this.parseTextKeyWord(keyWordArr, dataSource));
            }

            return idsMap;
         }
      } else {
         return idsMap;
      }
   }

   public Map<String, List<Map<String, Object>>> previewSilenceResultProcess(
      JSONArray jsonArray, Map<String, List<Map<String, Object>>> idsMap, String dataSource
   ) {
      if (this.count == 0) {
         return idsMap;
      } else if (jsonArray != null && jsonArray.size() != 0) {
         JSONArray reslList = new JSONArray();
         if (this.isVoiceType || null != this.orderTextRule && this.orderTextRule.contains("@")) {
            if (jsonArray.size() == 1) {
               reslList.addAll(jsonArray);
            } else if (this.pageNum * this.pageSize + 1 <= jsonArray.size()) {
               reslList.addAll(jsonArray.subList((this.pageNum - 1) * this.pageSize, this.pageNum * this.pageSize));
            } else {
               reslList.addAll(jsonArray.subList((this.pageNum - 1) * this.pageSize, jsonArray.size()));
            }
         } else {
            reslList.addAll(jsonArray);
         }

         for (int i = 0; i < reslList.size(); i++) {
            JSONObject jsonObject = (JSONObject)reslList.get(i);
            String id = String.valueOf(jsonObject.get("id"));
            JSONArray textWordArr = (JSONArray)jsonObject.get("text_keyword");
            JSONArray keyWordArr = (JSONArray)jsonObject.get("keyword");
            idsMap.put(id, this.parseTextKeyWord(textWordArr, keyWordArr, dataSource));
         }

         return idsMap;
      } else {
         return idsMap;
      }
   }

   public List<Map<String, Object>> parseTextKeyWord(JSONArray keyWordArray, String dataSource) {
      int insightType = IndexConstants.getInsightType(dataSource);
      List<Map<String, Object>> textKeyWordMap = new ArrayList<>();
      if (keyWordArray != null && keyWordArray.size() != 0) {
         for (int i = 0; i < keyWordArray.size(); i++) {
            Map<String, Object> keyWordMap = new HashMap<>();
            String keyWordStr = String.valueOf(keyWordArray.get(i));
            String[] keyWords = keyWordStr.split("-");
            keyWordMap.put("word", keyWords[0]);
            keyWordMap.put("begin", Integer.parseInt(keyWords[1]));
            keyWordMap.put("end", Integer.parseInt(keyWords[2]));
            if (1 == insightType && keyWords.length >= 4) {
               keyWordMap.put("voiceId", keyWords[3]);
            }

            textKeyWordMap.add(keyWordMap);
         }

         return textKeyWordMap;
      } else {
         return textKeyWordMap;
      }
   }

   public List<Map<String, Object>> parseTextKeyWord(JSONArray textWordArray, JSONArray keyWordArray, String dataSource) {
      List<Map<String, Object>> textKeyWordMap = new ArrayList<>();
      if (textWordArray != null || keyWordArray != null) {
         if (textWordArray != null && textWordArray.size() > 0) {
            for (int i = 0; i < textWordArray.size(); i++) {
               Map<String, Object> keyWordMap = new HashMap<>();
               String textWordStr = String.valueOf(textWordArray.get(i));
               String[] keyWords = textWordStr.split("-");
               keyWordMap.put("word", keyWords[0]);
               keyWordMap.put("begin", Integer.parseInt(keyWords[1]));
               keyWordMap.put("end", Integer.parseInt(keyWords[2]));
               if (1 == IndexConstants.getInsightType(dataSource)) {
                  keyWordMap.put("voiceId", keyWords[3]);
               }

               textKeyWordMap.add(keyWordMap);
            }
         }

         if (keyWordArray != null && keyWordArray.size() > 0) {
            for (int i = 0; i < keyWordArray.size(); i++) {
               Map<String, Object> keyWordMap = new HashMap<>();
               String keyWordStr = String.valueOf(keyWordArray.get(i));
               String[] keyWords = keyWordStr.split("-");
               keyWordMap.put("word", keyWords[0]);
               keyWordMap.put("begin", Integer.parseInt(keyWords[1]));
               keyWordMap.put("end", Integer.parseInt(keyWords[2]));
               if (1 == IndexConstants.getInsightType(dataSource)) {
                  keyWordMap.put("voiceId", keyWords[3]);
               }

               textKeyWordMap.add(keyWordMap);
            }
         }
      }

      return textKeyWordMap;
   }

   public void detailResultProcess(ResultSet rs, Map<String, List<Map<String, Object>>> idsMap, String dataSource) {
      if (rs != null && rs.getTotalCount() != 0L) {
         List<String> durationConfig = this.getDurationConfig();

         for (LinkedHashMap<String, Object> result : rs.toList()) {
            String id;
            if (1 == IndexConstants.getInsightType(dataSource)) {
               id = String.valueOf(result.get("taskId"));
            } else {
               id = String.valueOf(result.get("voiceId"));
            }

            for (int mn = 0; mn < durationConfig.size(); mn++) {
               String indexField = durationConfig.get(mn);
               String currnt = result.get(indexField) == null ? null : String.valueOf(result.get(indexField));
               if (currnt != null) {
                  if (!"n0avgSpeed".equals(indexField) && !"n1avgSpeed".equals(indexField)) {
                     result.put(indexField, this.toSecond(currnt));
                  } else {
                     result.put(indexField, this.toSecondTwo(currnt));
                  }
               }
            }

            DataInfoPlatform dataInfo = new DataInfoPlatform(id, 0, 0);
            dataInfo.setKeywordInfos(idsMap.get(id));
            dataInfo.setDataMaps(result);
            this.dataInfos.add(dataInfo);
         }
      }
   }

   public void filterHitCountProcess(ResultSet rs) {
      if (rs != null && rs.getTotalCount() != 0L) {
         if (rs.hasRow()) {
            rs.next();
            if (rs.getValue("hitCount") == null) {
               this.dimCount = 0;
            } else {
               this.dimCount = (int)((Double)rs.getValue("hitCount")).doubleValue();
            }
         }
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

   public List<String> getDurationConfig() {
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

   public PreviewParser getPreviewParser() {
      return this.previewParser;
   }

   public void setPreviewParser(PreviewParser previewParser) {
      this.previewParser = previewParser;
   }

   public String[] getAnydrillHost() {
      return this.anydrillHost;
   }

   public void setAnydrillHost(String[] anydrillHost) {
      this.anydrillHost = anydrillHost;
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

   public List<DataInfoPlatform> getDataInfos() {
      return this.dataInfos;
   }

   public void setDataInfos(List<DataInfoPlatform> dataInfos) {
      this.dataInfos = dataInfos;
   }
}
