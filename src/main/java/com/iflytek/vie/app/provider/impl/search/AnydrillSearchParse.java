package com.iflytek.vie.app.provider.impl.search;

import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.provider.pojo.CommonFun;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.constants.DimensionMappingConfig;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnydrillSearchParse {
   private final Logger log = LoggerFactory.getLogger(AnydrillSearchParse.class);
   public DecimalFormat decimalFormat = new DecimalFormat("###0.0");
   public DecimalFormat longFormat = new DecimalFormat("###0");
   public String erroeMsg = "";
   private String tableName;
   private HashMap<String, Object> timeCondition;
   private String startTime;
   private String endTime;
   private LinkedHashMap<String, DimensionConfig> dimensionMap;
   private String dataSql;

   public AnydrillSearchParse() {
   }

   public AnydrillSearchParse(LinkedHashMap<String, DimensionConfig> dimensionMap, String tableName, String startTime, String endTime) {
      this(dimensionMap, tableName, startTime, endTime, null);
   }

   public AnydrillSearchParse(LinkedHashMap<String, DimensionConfig> dimensionMap, String tableName, String startTime, String endTime, String dataSql) {
      this.tableName = tableName;
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

      this.dataSql = dataSql;
   }

   public String dealCondition(List<HashMap<String, Object>> conD, String optionType, String dataSource) {
      String result = CommonParams.processed + "=0 and ";
      result = !StringUtils.isNullOrEmpry(this.dataSql) ? result + this.dataSql + " and " : result;
      String dimension = "";

      try {
         conD = (List<HashMap<String, Object>>)(conD != null ? conD : new ArrayList<>());
         if (this.timeCondition != null) {
            conD.add(this.timeCondition);
         }

         for (HashMap<String, Object> currentCond : (List<HashMap<String, Object>>)conD) {
            String type = currentCond.get(CommonParams.conditionType).toString().trim();
            String key = currentCond.get(CommonParams.conditionFiled).toString();
            Object condValue = currentCond.get(CommonParams.conditionValue);
            Object isNegate = currentCond.get(CommonParams.conditionNegate);
            String conValue = "";
            switch (DimType.valueOf(type)) {
               case timeDim:
               default:
                  break;
               case offLineTagId:
                  conValue = this.dealMulModelCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     dimension = dimension + " " + key + " in (" + conValue + ") " + optionType + " ";
                  }
                  break;
               case radio:
                  conValue = this.dealMulCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     dimension = dimension + " " + key + "=" + conValue + " " + optionType + " ";
                  }
                  break;
               case mulEqu:
                  conValue = this.dealMulEqlCond(condValue);
                  String opt = "true";
                  if (isNegate != null && "1".equals(isNegate.toString())) {
                     opt = "false";
                  }

                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     if (IndexConstants.getInsightType(dataSource) == 1 && "voiceId".equals(key)) {
                        key = DimensionMappingConfig.getMappingName(key);
                     }

                     dimension = dimension + "fulltext('" + key + ":(" + conValue + ")')=" + opt + " " + optionType + " ";
                  }
                  break;
               case range:
                  conValue = this.dealRangeCond(key, condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     dimension = dimension + " " + conValue + " " + optionType + " ";
                  }
                  break;
               case timeRange:
                  conValue = this.dealTRCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     result = result + " " + CommonParams.timestamp + " " + conValue + " and ";
                  }
                  break;
               case mulSel:
                  conValue = this.dealMulCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     dimension = dimension + " " + key + " in (" + conValue + ") " + optionType + " ";
                  }
            }
         }

         if (!StringUtils.isNullOrEmpry(dimension)) {
            dimension = dimension.substring(0, dimension.length() - (2 + optionType.length()));
            result = result + "(" + dimension + ") and ";
         }
      } catch (Exception var14) {
         this.log.error("异常错误", var14);
      }

      return result;
   }

   private String dealMulEqlCond(Object condValue) {
      String result = "";

      try {
         for (String current : (List<String>)condValue) {
            if (current.indexOf("*") == -1 && current.indexOf("?") == -1) {
               current = current + "*";
            }

            result = result + "" + current + " OR ";
         }

         result = result.length() > 0 ? result.substring(0, result.length() - 4) : result;
      } catch (Exception var6) {
         this.log.error("异常错误", var6);
      }

      return result;
   }

   private String dealMulCond(Object condValue) {
      String result = "";

      try {
         for (String current : (List<String>)condValue) {
            result = result + "'" + current + "',";
         }

         result = result.length() > 0 ? result.substring(0, result.length() - 1) : result;
      } catch (Exception var6) {
         this.log.error("异常错误", var6);
      }

      return result;
   }

   private String dealMulModelCond(Object condValue) {
      String result = "";

      try {
         for (HashMap<String, Object> current : (List<HashMap<String, Object>>)condValue) {
            result = result + "" + current.get("key") + ",";
         }

         result = result.length() > 0 ? result.substring(0, result.length() - 1) : result;
      } catch (Exception var6) {
         this.log.error("异常错误", var6);
      }

      return result;
   }

   private String dealTRCond(Object condValue) {
      String result = "";

      try {
         String exp = String.valueOf(condValue);
         if (!StringUtils.isNullOrEmpry(exp)) {
            String[] expArray = exp.split("\\|");
            if (expArray.length == 2) {
               String begin = expArray[0];
               String end = expArray[1];
               result = " between " + DateUtils.getTimestampByFULL(begin) + " and " + DateUtils.getTimestampByFULL(end);
            }
         }
      } catch (Exception var7) {
         this.log.error("异常错误", var7);
      }

      return result;
   }

   private String dealRangeCond(String key, Object condValue) {
      String result = "";

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

               result = result + key + "=" + this.longFormat.format(start) + " or ";
            } else {
               if (this.isDurationDimsion(key)) {
                  start = start * 1000.0 - 500.0 > 0.0 ? start * 1000.0 - 500.0 : start * 1000.0;
                  end = end * 1000.0 + 499.0;
               }

               result = result + "(" + key + ">=" + this.decimalFormat.format(start) + " and " + key + "<" + this.decimalFormat.format(end) + ") or ";
            }
         }

         result = !"".equals(result) ? "(" + result.substring(0, result.length() - 4) + ")" : result;
      } catch (Exception var13) {
         this.log.error("异常错误", var13);
      }

      return result;
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

   public String getErroeMsg() {
      return this.erroeMsg;
   }

   public void setErroeMsg(String erroeMsg) {
      this.erroeMsg = erroeMsg;
   }

   public String getTableName() {
      return this.tableName;
   }

   public void setTableName(String tableName) {
      this.tableName = tableName;
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

   public String getDataSql() {
      return this.dataSql;
   }

   public void setDataSql(String dataSql) {
      this.dataSql = dataSql;
   }
}
