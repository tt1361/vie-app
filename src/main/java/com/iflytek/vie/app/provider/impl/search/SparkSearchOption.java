package com.iflytek.vie.app.provider.impl.search;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.pojo.RequestData;
import com.iflytek.vie.utils.ESQueryUtils;
import com.iflytek.vie.utils.StringUtils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkSearchOption extends SearchOption {
   private final Logger logger = LoggerFactory.getLogger(SparkSearchOption.class);
   private boolean ifContainFunc = false;
   private RequestData requestData;

   public SparkSearchOption() {
   }

   public SparkSearchOption(
      String content,
      String tableName,
      LinkedHashMap<String, DimensionConfig> dimensionMap,
      int chanel,
      String optionType,
      int searchType,
      Map<String, String> selCol,
      RequestData requestData
   ) throws VieAppServiceException {
      this.contentParse(content);
      this.tableName = tableName;
      this.dimensionMap = dimensionMap;
      this.selCol = selCol;
      this.chanel = chanel;
      this.searchType = searchType;
      this.requestData = requestData;
   }

   public void setSearchSql() {
      try {
         this.requestData.addColumn("id");
         this.switchColMap.put("id", "contact_id");
         this.requestData.addColumn("dataType");
         this.requestData.addColumn("timePosition");
         this.requestData.addColumn("channelSeq");
         this.requestData.addColumn("contentOrigin");
         this.requestData.setIfRelateContent(true);
         String funcExpression = this.dealDimension();
         if (this.ifContainFunc) {
            funcExpression = "id, dataType, timePosition, channelSeq, contentOrigin," + funcExpression;
            this.requestData.setFuncExpression(funcExpression);
         }

         this.setCommonSql();
      } catch (Exception var2) {
         this.errorMsg = "数据异常";
         this.logger.error(this.errorMsg, var2);
      }
   }

   public void setTableSearchSql() {
      try {
         String funcExpression = this.dealDimension();
         if (this.ifContainFunc) {
            this.requestData.setFuncExpression(funcExpression);
         }

         this.setCommonSql();
      } catch (Exception var2) {
         this.errorMsg = "数据异常";
         this.logger.error(this.errorMsg, var2);
      }
   }

   private void setCommonSql() {
      String contantKey = "content";
      if (this.chanel == 0) {
         contantKey = contantKey + "N0";
      } else if (this.chanel == 1) {
         contantKey = contantKey + "N1";
      }

      this.dealCondition(contantKey);
   }

   private String dealDimension() {
      StringBuffer sb = new StringBuffer();
      String timeDimension = "";

      for (String key : this.selCol.keySet()) {
         this.requestData.addColumn(key);
         timeDimension = this.getTimeDimension(key);
         if (StringUtils.isNotNullAndEmpry(timeDimension)) {
            sb.append(timeDimension + ",");
            this.ifContainFunc = true;
         } else {
            sb.append(key + ",");
         }
      }

      if (sb.length() > 0) {
         sb.deleteCharAt(sb.length() - 1);
      }

      return sb.toString();
   }

   private String getTimeDimension(String colFiled) {
      String selCol = "";

      try {
         DimensionConfig dimensionConfig = this.dimensionMap.get(colFiled);
         if (dimensionConfig != null && dimensionConfig.getFlag() == 1) {
            selCol = "round(" + colFiled + "/1000.0) as " + colFiled;
         }
      } catch (Exception var4) {
         this.logger.error("[dealDimension]出现异常", var4);
      }

      return selCol;
   }

   private void dealCondition(String contenKey) {
      try {
         for (String andWord : this.andWordList) {
            Map<String, String> map = new HashMap<>();
            map.put("connectType", "must");
            if (this.searchType == 1) {
               map.put("queryType", "match_phrase");
            } else {
               map.put("queryType", "match");
            }

            map.put("expression", ESQueryUtils.getExpression(contenKey, andWord, "string"));
            map.put("ifSqlOrJson", "json");
            this.requestData.addfilterMap(map);
            this.requestData.setIfRelateContent(true);
         }

         for (String fullWord : this.fullContent) {
            Map<String, String> map = new HashMap<>();
            map.put("connectType", "must");
            if (this.searchType == 1) {
               map.put("queryType", "match_phrase");
            } else {
               map.put("queryType", "match");
            }

            map.put("expression", ESQueryUtils.getExpression(contenKey, fullWord, "string"));
            map.put("ifSqlOrJson", "json");
            this.requestData.addfilterMap(map);
            this.requestData.setIfRelateContent(true);
         }

         for (String orWord : this.orWordList) {
            Map<String, String> map = new HashMap<>();
            map.put("connectType", "should");
            if (this.searchType == 1) {
               map.put("queryType", "match_phrase");
            } else {
               map.put("queryType", "match");
            }

            map.put("expression", ESQueryUtils.getExpression(contenKey, orWord, "string"));
            map.put("ifSqlOrJson", "json");
            this.requestData.addfilterMap(map);
            this.requestData.setIfRelateContent(true);
         }

         for (String notWord : this.notWordList) {
            Map<String, String> map = new HashMap<>();
            map.put("connectType", "must_not");
            if (this.searchType == 1) {
               map.put("queryType", "match_phrase");
            } else {
               map.put("queryType", "match");
            }

            map.put("expression", ESQueryUtils.getExpression(contenKey, notWord, "string"));
            map.put("ifSqlOrJson", "json");
            this.requestData.addfilterMap(map);
            this.requestData.setIfRelateContent(true);
         }
      } catch (Exception var5) {
         this.errorMsg = "数据异常";
         this.logger.error(this.errorMsg, var5);
      }
   }
}
