package com.iflytek.vie.app.provider.impl.report;

import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.provider.common.EsQueryParse;
import com.iflytek.vie.app.provider.common.SparkSqlParse;
import com.iflytek.vie.app.provider.pojo.CommonFun;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.ExceptionUtil;
import com.iflytek.vie.utils.StringUtils;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsQueryParseReport {
   private final Logger reportLogger = LoggerFactory.getLogger(SparkSqlParse.class);
   public DecimalFormat decimalFormat = new DecimalFormat("###0.0");
   public DecimalFormat longFormat = new DecimalFormat("###0");
   private HashMap<String, DimensionConfig> dimensionMap;
   public LinkedHashMap<String, String> rangeMap = new LinkedHashMap<>();

   public EsQueryParseReport() {
   }

   public EsQueryParseReport(HashMap<String, DimensionConfig> dimensionMap) {
      this.dimensionMap = dimensionMap;
   }

   public List<String> dealReportCondition(List<HashMap<String, Object>> reportConD) {
      List<String> result = new ArrayList<>();
      result.add(EsQueryParse.getTermQuery(CommonParams.processed, 0));

      try {
         reportConD = (List<HashMap<String, Object>>)(reportConD != null ? reportConD : new ArrayList<>());
         Iterator i$ = reportConD.iterator();

         while (true) {
            String type;
            String key;
            Object condValue;
            Object isNegate;
            while (true) {
               if (!i$.hasNext()) {
                  return result;
               }

               HashMap<String, Object> currentCond = (HashMap<String, Object>)i$.next();
               type = currentCond.get(CommonParams.conditionType).toString().trim();
               key = currentCond.get(CommonParams.conditionFiled).toString();
               condValue = currentCond.get(CommonParams.conditionValue);
               isNegate = currentCond.get(CommonParams.conditionNegate);
               List<Object> valueObjectList = null;
               List<String> valueStringList = null;

               try {
                  valueObjectList = (List<Object>)condValue;
                  valueObjectList.remove("");
                  valueObjectList.remove(null);
                  if (valueObjectList.size() == 0) {
                     continue;
                  }
                  break;
               } catch (ClassCastException var12) {
                  if (condValue != null && !condValue.equals("")) {
                     break;
                  }
               }
            }

            switch (DimType.valueOf(type)) {
               case timeDim:
               default:
                  break;
               case offLineTagId:
                  List<Object> var17 = this.dealMulModelCond(condValue);
                  if (var17 != null && var17.size() > 0) {
                     result.add(EsQueryParse.getMulSelQuery(key, var17));
                  }
                  break;
               case radio:
                  if (condValue == null) {
                     break;
                  }

                  if (isNegate != null && "1".equals(isNegate.toString())) {
                     result.add(EsQueryParse.getNotQuery(EsQueryParse.getTermQuery(key, condValue)));
                     break;
                  }

                  result.add(EsQueryParse.getTermQuery(key, condValue));
                  break;
               case mulEqu:
                  List<String> var18 = this.dealMulEqlCond(condValue);
                  if (var18.size() <= 0) {
                     break;
                  }

                  if (isNegate != null && "1".equals(isNegate.toString())) {
                     result.add(EsQueryParse.getNotQuery(EsQueryParse.getMulEquQuery(key, var18)));
                     break;
                  }

                  result.add(EsQueryParse.getMulEquQuery(key, var18));
                  break;
               case range:
                  result.add(this.getRangeQuery(key, condValue));
                  break;
               case timeRange:
                  result.add(this.getTRQuery(key, condValue));
                  break;
               case mulSel:
                  List<Object> var16 = (List<Object>)(List<?>)condValue;
                  if (var16.size() > 0) {
                     result.add(EsQueryParse.getMulSelQuery(key, var16));
                  }
            }
         }
      } catch (Exception var13) {
         this.reportLogger.error(ExceptionUtil.getStackTrace(var13));
         return result;
      }
   }

   public static void dealBatchQuery(List<String> query, String batchId, Integer dataType, Integer ifLone, String dataSql) {
      Pattern p = Pattern.compile(".*\\s+(.*)\\s+in\\s+\\((.*)\\).*");
      Matcher m = p.matcher(dataSql);
      if (m.matches()) {
         String key = m.group(1);
         String str = m.group(2);
         if (str.indexOf(")") != -1) {
            str = str.substring(0, str.indexOf(")"));
         }

         String[] dataYearList = str.replaceAll("'", "").split(",");
         String dataYearQuery = EsQueryParse.getMulSelQuery(key, (List<Object>)(List<?>)Arrays.asList(dataYearList));
         query.add(dataYearQuery);
      }

      if (1 == ifLone) {
         query.add(EsQueryParse.getTermQuery("ifLone", 1));
         if (StringUtils.isNotNullAndEmpry(batchId)) {
            query.add(EsQueryParse.getTermQuery("batchId", batchId));
         }
      } else {
         query.add(EsQueryParse.getNotQuery(EsQueryParse.getTermQuery("ifLone", 1)));
      }

      if (dataType != null && dataType != 2) {
         query.add(EsQueryParse.getTermQuery("dataType", dataType));
      }
   }

   private String getTRQuery(String key, Object condValue) {
      String result = "";

      try {
         String exp = (String)condValue;
         if (!StringUtils.isNullOrEmpry(exp)) {
            String[] expArray = exp.split("\\|");
            if (expArray.length == 2) {
               String begin = expArray[0];
               String end = expArray[1];
               end = "uptonow".equals(end) ? DateUtils.parseDateToString(new Date(), "yyyy-MM-dd") : end;
               end = DateUtils.getDaybeforeOrAfter(end, 1);
               result = EsQueryParse.getTimeRangeQuery(key, DateUtils.getTimestamp(begin), DateUtils.getTimestamp(end));
            }
         }
      } catch (Exception var8) {
         this.reportLogger.error(ExceptionUtil.getStackTrace(var8));
      }

      return result;
   }

   private List<Object> dealMulModelCond(Object condValue) {
      List<Object> result = new ArrayList<>();

      try {
         for (HashMap<String, Object> current : (List<HashMap<String, Object>>)condValue) {
            result.add(current.get("key"));
         }
      } catch (Exception var6) {
         this.reportLogger.error(ExceptionUtil.getStackTrace(var6));
      }

      return result;
   }

   private List<String> dealMulEqlCond(Object condValue) {
      List<String> result = new ArrayList<>();

      try {
         for (String current : (List<String>)condValue) {
            if (StringUtils.isNotNullAndEmpry(current)) {
               String lastStr = current.substring(current.length() - 1, current.length());
               if (!lastStr.equals("*")) {
                  current = current + "*";
               }
            }

            current = current.replaceAll("\\*", ".*");
            result.add(current);
         }
      } catch (Exception var7) {
         this.reportLogger.error(ExceptionUtil.getStackTrace(var7));
      }

      return result;
   }

   private String getRangeQuery(String key, Object condValue) {
      String resultQuery = "";
      String rangeValue = "";

      try {
         List<String> valueList = (List<String>)condValue;
         List<String> queryList = new LinkedList<>();

         for (String current : valueList) {
            String[] params = current.split("\\|");
            double start = 0.0;
            double end = 0.0;
            String oldS = null;
            String oldE = null;
            if (params.length == 2) {
               start = CommonFun.isNumber(params[0]) ? Double.parseDouble(params[0]) : Integer.parseInt(params[0]);
               end = CommonFun.isNumber(params[1]) ? Double.parseDouble(params[1]) : Integer.parseInt(params[1]);
               oldS = CommonFun.isNumber(params[0]) ? this.longFormat.format(start) + "" : this.longFormat.format(start) + "";
               oldE = CommonFun.isNumber(params[1]) ? this.longFormat.format(end) + "" : this.longFormat.format(end) + "";
            } else if (params.length == 1) {
               start = CommonFun.isNumber(params[0]) ? Double.parseDouble(params[0]) : Integer.parseInt(params[0]);
               end = 2.147483647E9;
               oldS = CommonFun.isNumber(params[0]) ? this.longFormat.format(start) + "" : this.longFormat.format(start) + "";
               oldE = "";
            }

            if (this.isDurationDimsion(key)) {
               if (!key.equals("n0avgSpeed") && !key.equals("n1avgSpeed")) {
                  start = start * 1000.0 - 500.0 > 0.0 ? start * 1000.0 - 500.0 : start * 1000.0;
                  end = end * 1000.0 + 499.0;
               } else {
                  start *= 1000.0;
                  end *= 1000.0;
               }
            }

            queryList.add(EsQueryParse.getRangeQuery(key, new BigDecimal(start).doubleValue(), new BigDecimal(end).doubleValue()));
            rangeValue = rangeValue + this.decimalFormat.format(start) + "_" + this.decimalFormat.format(end) + "_" + oldS + "~" + oldE + ",";
         }

         resultQuery = EsQueryParse.getOrQuery(queryList.toArray(new String[queryList.size()]));
         this.rangeMap.put(key, rangeValue);
      } catch (Exception var16) {
         this.reportLogger.error(ExceptionUtil.getStackTrace(var16));
      }

      return resultQuery;
   }

   public boolean isDurationDimsion(String key) {
      boolean result = false;

      try {
         DimensionConfig dimensionConfig = this.dimensionMap.get(key);
         if (dimensionConfig != null) {
            int flag = Integer.parseInt(dimensionConfig.getFlag().toString());
            if (flag == 1 || flag == 2) {
               result = true;
            }
         } else {
            this.reportLogger.info("isDurationDimsion 找不到维度" + key);
         }
      } catch (Exception var5) {
         this.reportLogger.error(ExceptionUtil.getStackTrace(var5));
      }

      return result;
   }
}
