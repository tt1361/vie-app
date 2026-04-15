package com.iflytek.vie.app.provider.utils;

import com.alibaba.fastjson.JSON;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.provider.ruleparse.Filter;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildTopicFilterByAnydri {
   private static final Logger logger = LoggerFactory.getLogger(BuildTopicFilterByAnydri.class);
   public static DecimalFormat df = new DecimalFormat("0.0");

   public static String buildTopicFilterSql(String filterJson) throws ParseException {
      String sql = "";
      List<Map<String, Object>> filterMaps = (List<Map<String, Object>>)JSON.parseObject(filterJson, List.class);
      if (filterMaps != null && filterMaps.size() > 0) {
         Map<String, Object> hm = filterMaps.get(0);
         Integer timeType = (Integer)hm.get("timeType");
         Integer timeValue = (Integer)hm.get("timeValue");
         String inputValue = String.valueOf(hm.get("inputValue"));
         String[] timeStr = inputValue.split("~");
         String startHMS = "";
         String endHMS = "";
         if (timeStr.length > 0) {
            String start = timeStr[0];
            String end = timeStr[1];
            if (start.length() > 8) {
               startHMS = start.substring(start.length() - 8, start.length());
            }

            if (end.length() > 8) {
               endHMS = end.substring(end.length() - 8, end.length());
            }
         }

         if (timeType != null && timeValue != null && timeType == 2) {
            String[] starthms = startHMS.split(":");
            int hour = Integer.parseInt(starthms[0]);
            int min = Integer.parseInt(starthms[1]);
            int sec = Integer.parseInt(starthms[2]);
            String[] endhms = endHMS.split(":");
            int hour2 = Integer.parseInt(endhms[0]);
            int min2 = Integer.parseInt(endhms[1]);
            int sec2 = Integer.parseInt(endhms[2]);
            Date date = new Date();
            Calendar past = Calendar.getInstance();
            past.setTime(date);
            past.add(6, timeValue + 1);
            past.set(11, hour);
            past.set(12, min);
            past.set(13, sec);
            past.set(14, 0);
            Calendar now = Calendar.getInstance();
            now.setTime(date);
            now.add(6, 0);
            now.set(11, hour2);
            now.set(12, min2);
            now.set(13, sec2);
            now.set(14, 0);
            sql = sql + "timestamp between " + past.getTimeInMillis() + " and " + now.getTimeInMillis();
         } else {
            String name = null;
            String key = null;
            Object value = null;
            String type = null;
            String dataType = null;
            String exclude = null;

            for (Map<String, Object> singleFilterMap : (List<Map<String, Object>>)filterMaps) {
               key = String.valueOf(singleFilterMap.get("key"));
               type = String.valueOf(singleFilterMap.get("type"));
               if ("null".equals(type)) {
                  type = "mulSel";
               }

               dataType = String.valueOf(singleFilterMap.get("dataType"));
               if ("null".equals(dataType)) {
                  dataType = "string";
               }

               value = singleFilterMap.get("value");
               exclude = String.valueOf(singleFilterMap.get("exclude"));
               name = String.valueOf(singleFilterMap.get("name"));
               Filter filter = new Filter(name, key, dataType, type, value, exclude);
               sql = buildTopicFilter(filterJson, filter);
            }
         }
      }

      return sql;
   }

   private static String buildTopicFilter(String filterJson, Filter filter) throws ParseException {
      String sql = "";
      String type = filter.getType();
      Object value = filter.getValue();
      List<String> values = (List<String>)value;
      if (values != null && values.size() != 0) {
         if (DimType.timeRange.getName().equals(type)) {
            long start = DateUtils.dateToTimestamp(values.get(0), "yyyy-MM-dd HH:mm:ss", 0L);
            long end;
            if ("".equals(values.get(1))) {
               end = Long.MAX_VALUE;
            } else {
               end = DateUtils.dateToTimestamp(values.get(1), "yyyy-MM-dd HH:mm:ss", 0L);
            }

            sql = sql + "timestamp between " + start + " and " + end;
         }

         return sql;
      } else {
         return sql;
      }
   }

   public static String getPathCond(List<HashMap<String, String>> pathMap, LinkedHashMap<String, DimensionConfig> dimenMap) {
      String result = "";
      List<HashMap<String, String>> modelList = new ArrayList<>();
      List<HashMap<String, String>> notModelList = new ArrayList<>();
      if (pathMap != null) {
         for (HashMap<String, String> cd : (List<HashMap<String, String>>)pathMap) {
            String field = String.valueOf(cd.get("field"));
            String value = String.valueOf(cd.get("value"));
            String isNegate = String.valueOf(cd.get("isNegate"));
            DimensionConfig dimensionConfig = dimenMap.get(field);
            String showType = "";
            boolean isDuration = false;
            String indexField = "";
            if ("model".equals(field)) {
               showType = DimType.offLineTagId.toString();
               isDuration = false;
            }

            if (dimensionConfig != null) {
               showType = dimensionConfig.getShowType();
               isDuration = dimensionConfig.getFlag() == 1 || dimensionConfig.getFlag() == 2;
               indexField = dimensionConfig.getIndexField();
            }

            String nageteOpt = "0".equals(isNegate) ? "in" : "not in";
            String eqOpt = "0".equals(isNegate) ? "=" : "!=";
            switch (DimType.valueOf(showType)) {
               case offLineTagId:
                  if (!StringUtils.isNullOrEmpry(value)) {
                     if ("0".equals(isNegate)) {
                        modelList.add(cd);
                     } else {
                        notModelList.add(cd);
                     }
                  }
                  break;
               case radio:
                  if (!StringUtils.isNullOrEmpry(value)) {
                     result = result + " " + field + eqOpt + value + " and ";
                  }
                  break;
               case mulEqu:
                  String[] valstr = value.split(",");
                  String conValue = dealMulEqlCond(valstr);
                  String opt = "true";
                  if ("1".equals(isNegate)) {
                     opt = "false";
                  }

                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     result = result + "fulltext('" + field + ":(" + conValue + ")')=" + opt + " and ";
                  }
                  break;
               case range:
                  value = dealRangeCond(field, value, isDuration, isNegate, indexField);
                  if (!StringUtils.isNullOrEmpry(value)) {
                     result = result + " " + value + " and ";
                  }
                  break;
               case mulSel:
                  if (!StringUtils.isNullOrEmpry(value)) {
                     value = value.replaceAll(",", "','");
                     result = result + " " + field + " " + nageteOpt + " ('" + value + "') and ";
                  }
                  break;
               case timeRange:
                  if (!StringUtils.isNullOrEmpry(value)) {
                     String[] cvArrayk = value.split(",");
                     result = result + " and (";
                     if (null != cvArrayk && cvArrayk.length != 0) {
                        String start = null;
                        String end = null;
                        long startTime = 0L;
                        long endTime = 0L;

                        for (String str : cvArrayk) {
                           String[] params = str.split("~");
                           start = params[0];
                           end = DateUtils.getDaybeforeOrAfter(params[1], 1);
                           startTime = DateUtils.getTimestamp(start);
                           endTime = DateUtils.getTimestamp(end);
                           result = result + " (" + field + ">=" + startTime + " and " + field + "<" + endTime + ") or ";
                        }

                        result = result.substring(0, result.length() - 2);
                        result = result + ")";
                     }
                  }
            }
         }
      }

      String modelValue = "";

      for (HashMap<String, String> cd : (List<HashMap<String, String>>)modelList) {
         modelValue = modelValue + " and +offLineTagId:(" + cd.get("value") + ")";
      }

      result = result + ("".equals(modelValue) ? "" : " fulltext('(" + modelValue.substring(5) + ")')=true and ");
      String noModelValue = "";

      for (HashMap<String, String> cd : (List<HashMap<String, String>>)notModelList) {
         noModelValue = noModelValue + "," + cd.get("value");
      }

      result = result + ("".equals(noModelValue) ? "" : " offLineTagId not in(" + noModelValue.substring(1) + ") and ");
      return "".equals(result) ? result : result.substring(0, result.length() - 4);
   }

   private static String dealMulEqlCond(String[] valueList) {
      String result = "";

      try {
         for (String current : valueList) {
            if (current.indexOf("*") == -1 && current.indexOf("?") == -1) {
               current = current + "*";
            }

            result = result + "" + current + " OR ";
         }

         result = result.length() > 0 ? result.substring(0, result.length() - 4) : result;
      } catch (Exception var6) {
         logger.error("处理单多选条件出错", var6);
      }

      return result;
   }

   private static String dealRangeCond(String field, String conValue, boolean isDuration, String isNegate, String indexField) {
      String result = "";

      try {
         String[] cvArrayk = conValue.split(",");

         for (String value : cvArrayk) {
            String[] params = value.split("~");
            double start = Double.parseDouble(params[0]);
            double end = Double.parseDouble(params[1]);
            if (isDuration) {
               if (!"n0avgSpeed".equals(indexField) && !"n1avgSpeed".equals(indexField)) {
                  start = start * 1000.0 - 500.0 > 0.0 ? start * 1000.0 - 500.0 : start * 1000.0;
                  end = end * 1000.0 + 499.0;
               } else {
                  start *= 1000.0;
                  end *= 1000.0;
               }
            }

            if ("0".equals(isNegate)) {
               result = result + "(" + field + ">=" + df.format(start) + " and " + field + "<=" + df.format(end) + ") or ";
            } else if (start != 0.0) {
               result = result + "((" + field + ">= 0.0 and " + field + "<" + df.format(start) + ") or (" + field + ">" + df.format(end) + ")) and ";
            }
         }

         result = "(" + result.substring(0, result.length() - 4) + ")";
      } catch (Exception var16) {
         logger.error("处理区间函数出错", var16);
      }

      return result;
   }

   public static String groupTopicCondByAnyDri(String topicCondition) {
      String sql = "";

      try {
         sql = buildTopicFilterSql(topicCondition);
      } catch (Exception var3) {
         logger.error("anydrill查询组装专题条件出错", var3);
      }

      return sql;
   }

   public static List<String> getDurationDimension(LinkedHashMap<String, DimensionConfig> dimenMap) {
      List<String> durationList = new ArrayList<>();

      for (Entry<String, DimensionConfig> entry : dimenMap.entrySet()) {
         if (entry.getValue().getFlag() == 1 || entry.getValue().getFlag() == 2) {
            durationList.add(entry.getValue().getIndexField());
         }
      }

      return durationList;
   }
}
