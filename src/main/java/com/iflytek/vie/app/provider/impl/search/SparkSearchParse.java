package com.iflytek.vie.app.provider.impl.search;

import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.provider.pojo.CommonFun;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.constants.OperateType;
import com.iflytek.vie.pojo.RequestData;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.ESQueryUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkSearchParse {
   private final Logger log = LoggerFactory.getLogger(SparkSearchParse.class);
   public DecimalFormat decimalFormat = new DecimalFormat("###0.0");
   public DecimalFormat longFormat = new DecimalFormat("###0");
   private HashMap<String, Object> timeCondition;
   private String startTime;
   private String endTime;
   private LinkedHashMap<String, DimensionConfig> dimensionMap;
   private RequestData requestData;
   private String optionType;

   public SparkSearchParse() {
   }

   public SparkSearchParse(LinkedHashMap<String, DimensionConfig> dimensionMap, String startTime, String endTime, RequestData requestData) {
      this(dimensionMap, startTime, endTime, requestData, null);
   }

   public SparkSearchParse(LinkedHashMap<String, DimensionConfig> dimensionMap, String startTime, String endTime, RequestData requestData, String optionType) {
      this.startTime = startTime;
      this.endTime = endTime;
      this.dimensionMap = dimensionMap;
      if (!StringUtils.isNullOrEmpry(startTime) && !StringUtils.isNullOrEmpry(endTime)) {
         String tc = startTime + "|" + endTime;
         this.timeCondition = new HashMap<>();
         this.timeCondition.put(CommonParams.conditionFiled, "time");
         this.timeCondition.put(CommonParams.conditionType, DimType.timeRange.getName());
         this.timeCondition.put(CommonParams.conditionValue, tc);
      }

      if (StringUtils.isNullOrEmpry(optionType)) {
         optionType = "and";
      }

      this.optionType = optionType;
      this.requestData = requestData;
   }

   public void dealCondition(List<HashMap<String, Object>> conditionList, String dataSource) {
      Map<String, String> map1 = new HashMap<>();
      map1.put("connectType", "must");
      map1.put("queryType", "term");
      map1.put("expression", ESQueryUtils.getExpression("processed", "0"));
      map1.put("ifSqlOrJson", "json");
      this.requestData.addfilterMap(map1);

      try {
         conditionList = (List<HashMap<String, Object>>)(conditionList != null ? conditionList : new ArrayList<>());
         if (this.timeCondition != null) {
            conditionList.add(this.timeCondition);
         }

         for (HashMap<String, Object> currentCond : (List<HashMap<String, Object>>)conditionList) {
            String type = currentCond.get(CommonParams.conditionType).toString().trim();
            String key = currentCond.get(CommonParams.conditionFiled).toString();
            Object condValue = currentCond.get(CommonParams.conditionValue);
            Object isNegate = currentCond.get(CommonParams.conditionNegate);
            switch (DimType.valueOf(type)) {
               case timeDim:
               default:
                  break;
               case offLineTagId:
                  this.dealMulModelCond(key, condValue);
                  break;
               case radio:
                  this.dealMulCond(key, condValue);
                  break;
               case mulEqu:
                  boolean opt = true;
                  if (isNegate != null && "1".equals(isNegate.toString())) {
                     opt = false;
                  }

                  this.dealMulEqlCond(key, condValue, opt, dataSource);
                  break;
               case range:
                  this.dealRangeCond(key, condValue);
                  break;
               case timeRange:
                  this.dealTRCond(condValue);
                  break;
               case mulSel:
                  this.dealMulCond(key, condValue);
            }
         }
      } catch (Exception var11) {
         this.log.error("异常错误", var11);
      }
   }

   private void dealMulEqlCond(String key, Object condValue, boolean opt, String dataSource) {
      try {
         List<String> valueList = (List<String>)condValue;
         if (opt) {
            if (valueList != null && valueList.size() > 0) {
               Map<String, String> map = new HashMap<>();
               if ("and".equals(this.optionType.toLowerCase())) {
                  map.put("connectType", "and");
               } else {
                  map.put("connectType", "or");
               }

               map.put("queryType", "or");
               StringBuffer sb = new StringBuffer();
               sb.append("[");

               for (String current : valueList) {
                  if (current.indexOf("*") == -1 && current.indexOf("?") == -1) {
                     current = current + ".*";
                  } else if (current.indexOf("*") != -1) {
                     current = current.replace("*", ".*");
                  } else {
                     current = current.replace("?", ".?");
                  }

                  current = this.handleValue(current, key, dataSource);
                  sb.append(ESQueryUtils.getTypeExpression("regexp", key, current, "string") + ",");
               }

               sb.deleteCharAt(sb.length() - 1);
               sb.append("]");
               map.put("expression", sb.toString());
               map.put("ifSqlOrJson", "json");
               this.requestData.addfilterMap(map);
            }
         } else if ("and".equals(this.optionType.toLowerCase())) {
            if (valueList != null && valueList.size() > 0) {
               for (String current : valueList) {
                  if (current.indexOf("*") == -1 && current.indexOf("?") == -1) {
                     current = current + ".*";
                  } else if (current.indexOf("*") != -1) {
                     current = current.replace("*", ".*");
                  } else {
                     current = current.replace("?", ".?");
                  }

                  current = this.handleValue(current, key, dataSource);
                  Map<String, String> map = new HashMap<>();
                  map.put("connectType", "not");
                  map.put("queryType", "regexp");
                  map.put("expression", ESQueryUtils.getExpression(key, current, "string"));
                  map.put("ifSqlOrJson", "json");
                  this.requestData.addfilterMap(map);
               }
            }
         } else if (valueList != null && valueList.size() > 0) {
            Map<String, String> map = new HashMap<>();
            map.put("connectType", "or");
            map.put("queryType", "and");
            StringBuffer sb = new StringBuffer();
            sb.append("[");

            for (String current : valueList) {
               if (current.indexOf("*") == -1 && current.indexOf("?") == -1) {
                  current = current + ".*";
               } else if (current.indexOf("*") != -1) {
                  current = current.replace("*", ".*");
               } else {
                  current = current.replace("?", ".?");
               }

               current = this.handleValue(current, key, dataSource);
               sb.append("{").append("\"not\"：").append(ESQueryUtils.getTypeExpression("regexp", key, current, "string")).append("},");
            }

            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
            map.put("expression", sb.toString());
            map.put("ifSqlOrJson", "json");
            this.requestData.addfilterMap(map);
         }
      } catch (Exception var10) {
         this.log.error("异常错误", var10);
      }
   }

   private String handleValue(String current, String key, String dataSource) {
      if (IndexConstants.getInsightType(dataSource) == 1 && "voiceId".equals(key) && !current.startsWith(".*") && !current.startsWith(".?")) {
         current = ".*" + current;
      }

      return current;
   }

   private void dealMulCond(String key, Object condValue) {
      try {
         String result = "";

         for (String current : (List<String>)condValue) {
            result = result + "\"" + current + "\",";
         }

         if (!StringUtils.isNullOrEmpry(result)) {
            result = result.substring(0, result.length() - 1);
            Map<String, String> map = new HashMap<>();
            if ("and".equals(this.optionType.toLowerCase())) {
               map.put("connectType", "must");
            } else {
               map.put("connectType", "or");
            }

            map.put("queryType", "terms");
            map.put("expression", ESQueryUtils.getTermsExpression(key, result));
            map.put("ifSqlOrJson", "json");
            this.requestData.addfilterMap(map);
         }
      } catch (Exception var7) {
         this.log.error("异常错误", var7);
      }
   }

   private void dealMulModelCond(String key, Object condValue) {
      try {
         if ("and".equals(this.optionType.toLowerCase())) {
            for (HashMap<String, Object> current : (List<HashMap<String, Object>>)condValue) {
               this.requestData.addModelIdList(Long.parseLong(String.valueOf(current.get("key"))));
            }

            this.requestData.setOperateType(OperateType.OR_TYPE.getOperateType());
         } else {
            String result = "";

            for (HashMap<String, Object> current : (List<HashMap<String, Object>>)condValue) {
               result = result + "" + current.get("key") + ",";
            }

            if (!StringUtils.isNullOrEmpry(result)) {
               result = result.substring(0, result.length() - 1);
               Map<String, String> map = new HashMap<>();
               map.put("connectType", "or");
               map.put("queryType", "terms");
               map.put("expression", ESQueryUtils.getTermsExpression(key, result));
               map.put("ifSqlOrJson", "json");
               this.requestData.addfilterMap(map);
               this.requestData.setIfQueryInAllModel(true);
            }
         }
      } catch (Exception var7) {
         this.log.error("异常错误", var7);
      }
   }

   private void dealTRCond(Object condValue) {
      try {
         String exp = String.valueOf(condValue);
         if (!StringUtils.isNullOrEmpry(exp)) {
            String[] expArray = exp.split("\\|");
            if (expArray.length == 2) {
               String begin = expArray[0];
               String end = expArray[1];
               Map<String, String> map = new HashMap<>();
               map.put("connectType", "must");
               map.put("queryType", "range");
               map.put(
                  "expression", ESQueryUtils.getRangeExpression(CommonParams.timestamp, DateUtils.getTimestampByFULL(begin), DateUtils.getTimestampByFULL(end))
               );
               map.put("ifSqlOrJson", "json");
               this.requestData.addfilterMap(map);
            }
         }
      } catch (Exception var7) {
         this.log.error("异常错误", var7);
      }
   }

   private void dealRangeCond(String key, Object condValue) {
      StringBuffer sb = new StringBuffer();
      sb.append("[");

      try {
         List<String> valueList = (List<String>)condValue;
         boolean isEqualSE = false;

         for (String current : valueList) {
            String[] params = current.split("\\|");
            double start = CommonFun.isNumber(params[0]) ? Double.parseDouble(params[0]) : Integer.parseInt(params[0]);
            double end = CommonFun.isNumber(params[1]) ? Double.parseDouble(params[1]) : Integer.parseInt(params[1]);
            if (start == end) {
               isEqualSE = true;
            }

            if (isEqualSE && valueList.size() == 1) {
               if (this.isDurationDimsion(key)) {
                  start *= 1000.0;
               }

               Map<String, String> map = new HashMap<>();
               if ("and".equals(this.optionType.toLowerCase())) {
                  map.put("connectType", "must");
               } else {
                  map.put("connectType", "or");
               }

               map.put("queryType", "term");
               map.put("expression", ESQueryUtils.getExpression(key, this.longFormat.format(start)));
               map.put("ifSqlOrJson", "json");
               this.requestData.addfilterMap(map);
            } else {
               if (this.isDurationDimsion(key)) {
                  start = start * 1000.0 - 500.0 > 0.0 ? start * 1000.0 - 500.0 : start * 1000.0;
                  end = end * 1000.0 + 499.0;
               }

               sb.append(ESQueryUtils.getRangeExpression(key, this.decimalFormat.format(start), this.decimalFormat.format(end), true)).append(",");
            }
         }

         if (sb.length() > 1) {
            Map<String, String> map = new HashMap<>();
            if ("and".equals(this.optionType.toLowerCase())) {
               map.put("connectType", "and");
            } else {
               map.put("connectType", "or");
            }

            map.put("queryType", "or");
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
            map.put("expression", sb.toString());
            map.put("ifSqlOrJson", "json");
            this.requestData.addfilterMap(map);
         }
      } catch (Exception var14) {
         this.log.error("异常错误", var14);
      }
   }

   public boolean isDurationDimsion(String key) {
      boolean result = false;

      try {
         DimensionConfig dimensionConfig = this.dimensionMap.get(key);
         if (dimensionConfig != null) {
            int flag = dimensionConfig.getFlag();
            if (flag == 1) {
               result = true;
            }
         } else {
            this.log.error("isDurationDimsion 找不到维度" + key);
         }
      } catch (Exception var5) {
         this.log.error("异常错误", var5);
      }

      return result;
   }

   public DecimalFormat getDecimalFormat() {
      return this.decimalFormat;
   }

   public void setDecimalFormat(DecimalFormat decimalFormat) {
      this.decimalFormat = decimalFormat;
   }

   public DecimalFormat getLongFormat() {
      return this.longFormat;
   }

   public void setLongFormat(DecimalFormat longFormat) {
      this.longFormat = longFormat;
   }

   public HashMap<String, Object> getTimeCondition() {
      return this.timeCondition;
   }

   public void setTimeCondition(HashMap<String, Object> timeCondition) {
      this.timeCondition = timeCondition;
   }

   public String getStartTime() {
      return this.startTime;
   }

   public void setStartTime(String startTime) {
      this.startTime = startTime;
   }

   public String getEndTime() {
      return this.endTime;
   }

   public void setEndTime(String endTime) {
      this.endTime = endTime;
   }

   public LinkedHashMap<String, DimensionConfig> getDimensionMap() {
      return this.dimensionMap;
   }

   public void setDimensionMap(LinkedHashMap<String, DimensionConfig> dimensionMap) {
      this.dimensionMap = dimensionMap;
   }

   public String getOptionType() {
      return this.optionType;
   }

   public void setOptionType(String optionType) {
      this.optionType = optionType;
   }
}
