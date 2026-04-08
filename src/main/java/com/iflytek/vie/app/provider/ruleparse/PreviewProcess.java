package com.iflytek.vie.app.provider.ruleparse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.iflytek.vie.constants.IndexConstants;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.anydrill.calculate.set.ResultSet;

public class PreviewProcess implements Serializable {
   private static final long serialVersionUID = -2570381464845356481L;
   private String columns;
   private String[] anydrillHost;
   private int count = 0;
   private int dimCount = 0;
   private List<DataInfo> dataInfos = new ArrayList<>();

   public Map<String, List<Map<String, Object>>> previewResultProcess(ResultSet rs, String dataSource) {
      Map<String, List<Map<String, Object>>> idsMap = new HashMap<>();
      if (rs != null && rs.getDataMiningResult() != null) {
         Map<String, Object> map = (Map<String, Object>)JSON.parseObject(String.valueOf(rs.getDataMiningResult()), Map.class);
         this.count = Integer.parseInt(String.valueOf(map.get("count")));
         if (this.count == 0) {
            return idsMap;
         } else {
            JSONArray jsonArray = (JSONArray)map.get("detail");
            if (jsonArray != null && jsonArray.size() != 0) {
               for (int i = 0; i < jsonArray.size(); i++) {
                  JSONObject jsonObject = (JSONObject)jsonArray.get(i);
                  String id = String.valueOf(jsonObject.get("id"));
                  idsMap.put(id, this.parseTextKeyWord((JSONArray)jsonObject.get("text_keyword"), dataSource));
               }

               return idsMap;
            } else {
               return idsMap;
            }
         }
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
            if (1 == insightType && keyWords.length > 3) {
               keyWordMap.put("voiceId", keyWords[3]);
            }

            textKeyWordMap.add(keyWordMap);
         }

         return textKeyWordMap;
      } else {
         return textKeyWordMap;
      }
   }

   public void detailResultProcess(ResultSet rs, Map<String, List<Map<String, Object>>> idsMap) {
      if (rs != null && rs.getTotalCount() != 0L) {
         for (LinkedHashMap<String, Object> result : rs.toList()) {
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

            DataInfo dataInfo = new DataInfo(id, 0, 0);
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

   public long toSecond(long millSecond) {
      return millSecond == 0L ? 0L : Math.round((float)millSecond / 1000.0F);
   }

   public String[] getAnydrillHost() {
      return this.anydrillHost != null ? (String[])this.anydrillHost.clone() : null;
   }

   public void setAnydrillHost(String[] anydrillHost) {
      if (anydrillHost != null) {
         this.anydrillHost = (String[])anydrillHost.clone();
      } else {
         this.anydrillHost = null;
      }
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

   public List<DataInfo> getDataInfos() {
      return this.dataInfos;
   }

   public void setDataInfos(List<DataInfo> dataInfos) {
      this.dataInfos = dataInfos;
   }
}
