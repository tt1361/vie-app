package com.iflytek.vie.app.provider.impl.report;

import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.provider.pojo.CommonFun;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.constants.DimensionMappingConfig;
import com.iflytek.vie.constants.OperateType;
import com.iflytek.vie.pojo.RequestData;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.ESQueryUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkParse {
   private final Logger log = LoggerFactory.getLogger(SparkParse.class);
   public DecimalFormat decimalFormat = new DecimalFormat("###0.0");
   public DecimalFormat longFormat = new DecimalFormat("###0");
   private HashMap<String, Object> timeCondition;
   private String startTime;
   private String endTime;
   private LinkedHashMap<String, DimensionConfig> dimensionMap;
   private RequestData requestData;
   private String optionType;

   public SparkParse() {
   }

   public SparkParse(LinkedHashMap<String, DimensionConfig> dimensionMap, String startTime, String endTime, RequestData requestData) {
      this(dimensionMap, startTime, endTime, requestData, null);
   }

   public SparkParse(LinkedHashMap<String, DimensionConfig> dimensionMap, String startTime, String endTime, RequestData requestData, String optionType) {
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

   public void dealCondition(List<HashMap<String, Object>> conditionList, boolean containVoiceId, LinkedList<String> correctModel) {
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
                  this.dealMulModelCond(key, condValue, containVoiceId, correctModel);
                  break;
               case radio:
                  this.dealMulCond(key, condValue, containVoiceId);
                  break;
               case mulEqu:
                  boolean opt = true;
                  if (isNegate != null && "1".equals(isNegate.toString())) {
                     opt = false;
                  }

                  this.dealMulEqlCond(key, condValue, opt, containVoiceId);
                  break;
               case range:
                  this.dealRangeCond(key, condValue, containVoiceId);
                  break;
               case timeRange:
                  this.dealTRCond(condValue);
                  break;
               case mulSel:
                  this.dealMulCond(key, condValue, containVoiceId);
            }
         }
      } catch (Exception var12) {
         this.log.error("异常错误", var12);
      }
   }

   private void dealMulEqlCond(String key, Object condValue, boolean opt, boolean containVoiceId) {
      try {
         List<String> valueList = (List<String>)condValue;
         if (opt) {
            if (valueList != null && valueList.size() > 0) {
               if (containVoiceId && DimensionMappingConfig.getMappingName(key) != null) {
                  Map<String, String> map = new HashMap<>();
                  map.put("name", "(");
                  map.put("nextConnect", "");
                  this.requestData.addfilterMap(map);
                  int size = 0;

                  for (String current : valueList) {
                     size++;
                     if (current.indexOf("*") == -1 && current.indexOf("?") == -1) {
                        current = current + "%";
                     }

                     if (current.indexOf("?") != -1) {
                        current = current.replace("?", "%");
                     }

                     if (current.indexOf("*") != -1) {
                        current = current.replace("*", "%");
                     }

                     String column = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
                     Map<String, String> map1 = new HashMap<>();
                     map1.put("name", "(");
                     map1.put("nextConnect", "");
                     this.requestData.addfilterMap(map1);
                     Map<String, String> map2 = new HashMap<>();
                     map2.put("name", column);
                     map2.put("expression", "\"" + current + "\"");
                     map2.put("connect", "like");
                     map2.put("nextConnect", "");
                     this.requestData.addfilterMap(map2);
                     Map<String, String> map3 = new HashMap<>();
                     map3.put("name", ")");
                     map3.put("nextConnect", "");
                     if (size < valueList.size()) {
                        map3.put("nextConnect", "or");
                     }

                     this.requestData.addfilterMap(map3);
                  }

                  Map<String, String> map4 = new HashMap<>();
                  map4.put("name", ")");
                  map4.put("nextConnect", "and");
                  this.requestData.addfilterMap(map4);
               } else {
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

                     String column = key;
                     if (containVoiceId) {
                        column = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
                     }

                     sb.append(ESQueryUtils.getTypeExpression("regexp", column, current, "string") + ",");
                  }

                  sb.deleteCharAt(sb.length() - 1);
                  sb.append("]");
                  map.put("expression", sb.toString());
                  map.put("ifSqlOrJson", "json");
                  this.requestData.addfilterMap(map);
               }
            }
         } else if (valueList != null && valueList.size() > 0) {
            if (containVoiceId && DimensionMappingConfig.getMappingName(key) != null) {
               Map<String, String> map = new HashMap<>();
               map.put("name", "(");
               map.put("nextConnect", "");
               this.requestData.addfilterMap(map);
               int size = 0;

               for (String current : valueList) {
                  size++;
                  if (current.indexOf("*") == -1 && current.indexOf("?") == -1) {
                     current = current + "%";
                  } else {
                     current = current.replace("?", "%");
                  }

                  String column = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
                  Map<String, String> map1 = new HashMap<>();
                  map1.put("name", "(");
                  map1.put("nextConnect", "");
                  this.requestData.addfilterMap(map1);
                  Map<String, String> map2 = new HashMap<>();
                  map2.put("name", column);
                  map2.put("expression", "\"" + current + "\"");
                  map2.put("connect", "not like");
                  map2.put("nextConnect", "");
                  this.requestData.addfilterMap(map2);
                  Map<String, String> map3 = new HashMap<>();
                  map3.put("name", ")");
                  map3.put("nextConnect", "");
                  if (size < valueList.size()) {
                     map3.put("nextConnect", "and");
                  }

                  this.requestData.addfilterMap(map3);
               }

               Map<String, String> map4 = new HashMap<>();
               map4.put("name", ")");
               map4.put("nextConnect", "and");
               this.requestData.addfilterMap(map4);
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

                     Map<String, String> map = new HashMap<>();
                     map.put("connectType", "not");
                     map.put("queryType", "regexp");
                     if (containVoiceId) {
                        key = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
                     }

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

                  sb.append("{").append("\"not\"：").append(ESQueryUtils.getTypeExpression("regexp", key, current, "string")).append("},");
               }

               sb.deleteCharAt(sb.length() - 1);
               sb.append("]");
               map.put("expression", sb.toString());
               map.put("ifSqlOrJson", "json");
               this.requestData.addfilterMap(map);
            }
         }
      } catch (Exception var14) {
         this.log.error("异常错误", var14);
      }
   }

   private void dealMulCond(String key, Object condValue, boolean containVoiceId) {
      try {
         String result = "";
         List<String> valueList = (List<String>)condValue;

         for (String current : valueList) {
            result = result + "\"" + current + "\",";
         }

         if (containVoiceId && DimensionMappingConfig.getMappingName(key) != null) {
            Map<String, String> map = new HashMap<>();
            map.put("name", "(");
            map.put("nextConnect", "");
            this.requestData.addfilterMap(map);
            String column = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);

            for (int i = 0; i < valueList.size(); i++) {
               Map<String, String> map1 = new HashMap<>();
               map1.put("name", "(");
               map1.put("nextConnect", "");
               this.requestData.addfilterMap(map1);
               Map<String, String> map2 = new HashMap<>();
               map2.put("name", column);
               map2.put("expression", String.valueOf(valueList.get(i)));
               map2.put("connect", "=");
               map2.put("nextConnect", "");
               this.requestData.addfilterMap(map2);
               Map<String, String> map3 = new HashMap<>();
               map3.put("name", ")");
               map3.put("nextConnect", "");
               if (i < valueList.size() - 1) {
                  map3.put("nextConnect", "or");
               }

               this.requestData.addfilterMap(map3);
            }

            Map<String, String> map4 = new HashMap<>();
            map4.put("name", ")");
            map4.put("nextConnect", "and");
            this.requestData.addfilterMap(map4);
         } else if (!StringUtils.isNullOrEmpry(result)) {
            result = result.substring(0, result.length() - 1);
            Map<String, String> map = new HashMap<>();
            if ("and".equals(this.optionType.toLowerCase())) {
               map.put("connectType", "must");
            } else {
               map.put("connectType", "or");
            }

            map.put("queryType", "terms");
            String column = key;
            if (containVoiceId) {
               column = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
            }

            map.put("expression", ESQueryUtils.getTermsExpression(column, result));
            map.put("ifSqlOrJson", "json");
            this.requestData.addfilterMap(map);
         }
      } catch (Exception var12) {
         this.log.error("异常错误", var12);
      }
   }

   private void dealMulModelCond(String key, Object condValue, boolean containVoiceId, LinkedList<String> correctModel) {
      try {
         if ("and".equals(this.optionType.toLowerCase())) {
            for (HashMap<String, Object> current : (List<HashMap<String, Object>>)condValue) {
               if (correctModel != null && correctModel.contains(String.valueOf(current.get("key")))) {
                  this.requestData.addModelIdList(Long.parseLong(String.valueOf(current.get("key"))));
               }
            }

            this.requestData.setOperateType(OperateType.OR_TYPE.getOperateType());
         } else {
            String result = "";

            for (HashMap<String, Object> currentx : (List<HashMap<String, Object>>)condValue) {
               if (correctModel != null && correctModel.contains(String.valueOf(currentx.get("key")))) {
                  result = result + "" + currentx.get("key") + ",";
               }
            }

            if (!StringUtils.isNullOrEmpry(result)) {
               result = result.substring(0, result.length() - 1);
               Map<String, String> map = new HashMap<>();
               map.put("connectType", "or");
               map.put("queryType", "terms");
               String column = key;
               if (containVoiceId) {
                  column = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
               }

               map.put("expression", ESQueryUtils.getTermsExpression(column, result));
               map.put("ifSqlOrJson", "json");
               this.requestData.addfilterMap(map);
               this.requestData.setIfQueryInAllModel(true);
            }
         }
      } catch (Exception var9) {
         this.log.error("异常错误", var9);
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
               end = "uptonow".equals(end) ? DateUtils.parseDateToString(new Date(), "yyyy-MM-dd HH:mm:ss") : end;
               Map<String, String> map = new HashMap<>();
               if ("and".equals(this.optionType.toLowerCase())) {
                  map.put("connectType", "must");
               } else {
                  map.put("connectType", "or");
               }

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

   private void dealRangeCond(String key, Object condValue, boolean containVoiceId) {
      StringBuffer sb = new StringBuffer();
      sb.append("[");

      try {
         List<String> valueList = (List<String>)condValue;
         String column = "";
         int size = 0;
         if (containVoiceId && DimensionMappingConfig.getMappingName(key) != null) {
            Map<String, String> map = new HashMap<>();
            map.put("name", "(");
            map.put("nextConnect", "");
            this.requestData.addfilterMap(map);
         }

         for (String current : valueList) {
            size++;
            String[] params = current.split("\\|");
            double start = CommonFun.isNumber(params[0]) ? Double.parseDouble(params[0]) : Integer.parseInt(params[0]);
            double end = CommonFun.isNumber(params[1]) ? Double.parseDouble(params[1]) : Integer.parseInt(params[1]);
            if (this.isDurationDimsion(key)) {
               if (!key.equals("n0avgSpeed") && !key.equals("n1avgSpeed")) {
                  start = start * 1000.0 - 500.0 > 0.0 ? start * 1000.0 - 500.0 : start * 1000.0;
                  end = end * 1000.0 + 499.0;
               } else {
                  start *= 1000.0;
                  end *= 1000.0;
               }
            }

            column = key;
            if (containVoiceId) {
               column = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
            }

            if (containVoiceId && !column.equals(key)) {
               Map<String, String> map1 = new HashMap<>();
               map1.put("name", "(");
               map1.put("nextConnect", "");
               this.requestData.addfilterMap(map1);
               Map<String, String> map2 = new HashMap<>();
               map2.put("name", column);
               map2.put("expression", String.valueOf(start));
               map2.put("connect", ">=");
               map2.put("nextConnect", "and");
               this.requestData.addfilterMap(map2);
               Map<String, String> map3 = new HashMap<>();
               map3.put("name", column);
               map3.put("expression", String.valueOf(end));
               map3.put("connect", "<=");
               map3.put("nextConnect", "");
               this.requestData.addfilterMap(map3);
               Map<String, String> map4 = new HashMap<>();
               map4.put("name", ")");
               if (size < valueList.size()) {
                  map4.put("nextConnect", "or");
               } else {
                  map4.put("nextConnect", "");
               }

               this.requestData.addfilterMap(map4);
            } else {
               sb.append(ESQueryUtils.getRangeExpression(column, this.decimalFormat.format(start), this.decimalFormat.format(end), true)).append(",");
            }
         }

         if (containVoiceId && !column.equals(key)) {
            Map<String, String> map = new HashMap<>();
            map.put("name", ")");
            map.put("nextConnect", "and");
            this.requestData.addfilterMap(map);
         } else if (sb.length() > 1) {
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
      } catch (Exception var19) {
         this.log.error("异常错误", var19);
      }
   }

   public boolean isDurationDimsion(String key) {
      boolean result = false;

      try {
         DimensionConfig dimensionConfig = this.dimensionMap.get(key);
         if (dimensionConfig != null) {
            int flag = dimensionConfig.getFlag();
            if (flag == 1 || flag == 2) {
               result = true;
            }
         } else {
            this.log.info("isDurationDimsion 找不到维度" + key);
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
