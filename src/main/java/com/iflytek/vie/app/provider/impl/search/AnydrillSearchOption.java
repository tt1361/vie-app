package com.iflytek.vie.app.provider.impl.search;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.utils.StringUtils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnydrillSearchOption extends SearchOption {
   private final Logger logger = LoggerFactory.getLogger(AnydrillSearchOption.class);

   public AnydrillSearchOption(
      String content,
      String tableName,
      List<HashMap<String, Object>> conditionMap,
      LinkedHashMap<String, DimensionConfig> dimensionMap,
      AnydrillSearchParse anyDrillParse,
      int chanel,
      String optionType,
      int searchType,
      int pageNum,
      int pageSize,
      Map<String, String> selCol
   ) throws VieAppServiceException {
      this.contentParse(content);
      this.tableName = tableName;
      this.pageNum = pageNum;
      this.pageSize = pageSize;
      this.dimensionCond = anyDrillParse.dealCondition(conditionMap, optionType, tableName);
      this.dimensionMap = dimensionMap;
      if (!StringUtils.isNullOrEmpry(anyDrillParse.erroeMsg)) {
         this.errorMsg = anyDrillParse.erroeMsg;
      }

      this.selCol = selCol;
      this.chanel = chanel;
      this.searchType = searchType;
   }

   public String getSearchSql(String multipleSql) {
      String sql = "";

      try {
         String selectCol = "id, dataType, timePosition, channelSeq, contentOrigin";
         this.switchColMap.put("id", "contact_id");
         sql = this.getCommonSql(selectCol, multipleSql);
      } catch (Exception var4) {
         this.errorMsg = "数据异常";
         this.logger.error(this.errorMsg, var4);
      }

      return sql;
   }

   public String getTableSearchSql(String multipleSql) {
      String sql = "";

      try {
         sql = this.getCommonSql(null, multipleSql);
      } catch (Exception var4) {
         this.errorMsg = "数据异常";
         this.logger.error(this.errorMsg, var4);
      }

      return sql;
   }

   private String getCommonSql(String selectCol, String multipleSql) {
      if (StringUtils.isNullOrEmpry(selectCol)) {
         if (this.selCol.size() == 0) {
            selectCol = " id ";
         } else {
            selectCol = this.getSelStr();
         }
      } else {
         selectCol = this.selCol.size() == 0 ? selectCol : selectCol + "," + this.getSelStr();
      }

      String contantKey = "content";
      if (this.chanel == 0) {
         contantKey = contantKey + "N0";
      } else if (this.chanel == 1) {
         contantKey = contantKey + "N1";
      }

      int rownumStart = this.pageSize * this.pageNum + 1;
      int rownumEnd = (this.pageNum + 1) * this.pageSize;
      String conditon = " rownum between " + rownumStart + " and " + rownumEnd + " and ";
      if (!StringUtils.isNullOrEmpry(this.dimensionCond)) {
         conditon = conditon + "(" + this.dimensionCond.substring(0, this.dimensionCond.length() - 5) + ") and ";
      }

      conditon = conditon + multipleSql + " and ";
      String textCond = this.dealCondition(contantKey);
      conditon = StringUtils.isNullOrEmpry(textCond) ? conditon : conditon + textCond;
      return "select " + selectCol + " from " + this.tableName + " where " + conditon.substring(0, conditon.length() - 5);
   }

   public String getSearchTotalCountSql(String multipleSql) {
      String sql = "";

      try {
         String selectCol = "count(id) as count";
         String conditon = "";
         if (!StringUtils.isNullOrEmpry(this.dimensionCond)) {
            conditon = this.dimensionCond.substring(0, this.dimensionCond.length() - 5);
         }

         sql = "select " + selectCol + " from " + this.tableName;
         if (!StringUtils.isNullOrEmpry(conditon)) {
            sql = sql + " where " + conditon + " and " + multipleSql;
         } else {
            sql = sql + " where " + multipleSql;
         }
      } catch (Exception var5) {
         this.errorMsg = "数据异常";
         this.logger.error(this.errorMsg, var5);
      }

      return sql;
   }

   private String getSelStr() {
      String result = "";

      for (String key : this.selCol.keySet()) {
         result = result + "," + this.dealDimension(key, this.selCol);
      }

      return result.length() > 1 ? result.substring(1) : result;
   }

   private String dealDimension(String colFiled, Map<String, String> selMap) {
      String selCol = "";

      try {
         DimensionConfig dimensionConfig = this.dimensionMap.get(colFiled);
         if (dimensionConfig != null && dimensionConfig.getFlag() == 1) {
            selCol = selCol + "(" + colFiled + "/1000.0) as " + colFiled;
         } else {
            selCol = selCol + colFiled + " as " + selMap.get(colFiled);
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      return selCol;
   }

   private String dealCondition(String contenKey) {
      String condition = "";

      try {
         for (String andWord : this.andWordList) {
            condition = condition
               + "(fulltext('+("
               + contenKey
               + ":((("
               + andWord
               + "))))','"
               + (this.searchType == 1 ? "content-query-parser" : "content-word-query-parser")
               + "')=true) and ";
         }

         for (String fullWord : this.fullContent) {
            condition = condition
               + "(fulltext('+("
               + contenKey
               + ":((("
               + fullWord
               + "))))','"
               + (this.searchType == 1 ? "content-query-parser" : "content-word-query-parser")
               + "')=true) and ";
         }

         String orAllWord = "";

         for (String orWord : this.orWordList) {
            orAllWord = orAllWord + orWord + " ";
         }

         if (orAllWord.length() > 1) {
            condition = condition
               + "(fulltext('+("
               + contenKey
               + ":((("
               + orAllWord.substring(0, orAllWord.length() - 1)
               + "))))','"
               + (this.searchType == 1 ? "content-query-parser" : "content-word-query-parser")
               + "')=true) and ";
         }

         String notAllWord = "";

         for (String notWord : this.notWordList) {
            notAllWord = notAllWord + notWord + " ";
         }

         if (notAllWord.length() > 1) {
            condition = condition
               + "(fulltext('+("
               + contenKey
               + ":((("
               + notAllWord.substring(0, notAllWord.length() - 1)
               + "))))','"
               + (this.searchType == 1 ? "content-query-parser" : "content-word-query-parser")
               + "')=false) and ";
         }
      } catch (Exception var7) {
         this.errorMsg = "数据异常";
         this.logger.error(this.errorMsg, var7);
      }

      return condition;
   }
}
