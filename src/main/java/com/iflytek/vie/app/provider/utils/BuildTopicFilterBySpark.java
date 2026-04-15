package com.iflytek.vie.app.provider.utils;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.provider.ruleparse.Filter;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.pojo.RequestData;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.ESQueryUtils;
import com.iflytek.vie.utils.StringUtils;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuildTopicFilterBySpark {
   public static void builTopicFilter(String filterJson, RequestData requestData) throws ParseException {
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
            Map<String, String> map = new HashMap<>();
            map.put("connectType", "must");
            map.put("queryType", "range");
            map.put("expression", ESQueryUtils.getRangeExpression("timestamp", past.getTimeInMillis(), now.getTimeInMillis()));
            map.put("ifSqlOrJson", "json");
            requestData.addfilterMap(map);
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
               buildTopicFilter(requestData, filter);
            }
         }
      }
   }

   private static void buildTopicFilter(RequestData requestData, Filter filter) throws ParseException {
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

            Map<String, String> map = new HashMap<>();
            map.put("connectType", "must");
            map.put("queryType", "range");
            map.put("expression", ESQueryUtils.getRangeExpression("timestamp", start, end));
            map.put("ifSqlOrJson", "json");
            requestData.addfilterMap(map);
         }
      }
   }

   public static void builPathFilter(List<HashMap<String, String>> pathMap, LinkedHashMap<String, DimensionConfig> dimenMap, RequestData requestData) {
      if (pathMap != null) {
         for (Map<String, String> cd : pathMap) {
            String field = cd.get("field").toString();
            String value = String.valueOf(cd.get("value"));
            String isNegate = String.valueOf(cd.get("isNegate"));
            DimensionConfig dimensionConfig = dimenMap.get(field);
            String showType = "";
            String dataType = "";
            boolean isDuration = false;
            String indexField = "";
            if ("model".equals(field)) {
               showType = DimType.offLineTagId.toString();
               isDuration = false;
            }

            if (dimensionConfig != null) {
               showType = dimensionConfig.getShowType();
               dataType = dimensionConfig.getDataType();
               isDuration = dimensionConfig.getFlag() == 1 || dimensionConfig.getFlag() == 2;
               indexField = dimensionConfig.getIndexField();
            }

            if (!StringUtils.isNullOrEmpry(showType)) {
               Map<String, String> map = new HashMap<>();
               switch (DimType.valueOf(showType)) {
                  case offLineTagId:
                     if (!StringUtils.isNullOrEmpry(value)) {
                        if ("0".equals(isNegate)) {
                           requestData.addModelIdList(Long.parseLong(value));
                        } else {
                           requestData.addExcludeModelIdList(Long.parseLong(value));
                        }
                     }
                     break;
                  case radio:
                     if (StringUtils.isNullOrEmpry(value)) {
                        break;
                     }

                     map.put("connectType", "must");
                     map.put("queryType", "terms");
                     map.put("ifSqlOrJson", "json");
                     if ("string".equals(dataType)) {
                        String result = "";
                        String[] arr = value.split(",");

                        for (String current : arr) {
                           result = result + "\"" + current + "\",";
                        }

                        if (!StringUtils.isNullOrEmpry(result)) {
                           result = result.substring(0, result.length() - 1);
                           map.put("expression", ESQueryUtils.getTermsExpression(field, result));
                        }
                     } else {
                        map.put("expression", ESQueryUtils.getTermsExpression(field, value));
                     }

                     requestData.addfilterMap(map);
                     break;
                  case mulEqu:
                     if (StringUtils.isNullOrEmpry(value)) {
                        break;
                     }

                     String[] valueList = value.split(",");
                     if ("0".equals(isNegate)) {
                        StringBuffer sbx = new StringBuffer();
                        sbx.append("[");

                        for (String current : valueList) {
                           if (current.indexOf("*") == -1 && current.indexOf("?") == -1) {
                              current = current + ".*";
                           } else if (current.indexOf("*") != -1) {
                              current = current.replace("*", ".*");
                           } else {
                              current = current.replace("?", ".?");
                           }

                           if ("string".equals(dataType)) {
                              sbx.append(ESQueryUtils.getTypeExpression("regexp", field, current, "string") + ",");
                           } else {
                              sbx.append(ESQueryUtils.getTypeExpression("regexp", field, current) + ",");
                           }
                        }

                        sbx.deleteCharAt(sbx.length() - 1);
                        sbx.append("]");
                        map.put("connectType", "and");
                        map.put("queryType", "or");
                        map.put("expression", sbx.toString());
                        map.put("ifSqlOrJson", "json");
                        requestData.addfilterMap(map);
                     } else {
                        for (String current : valueList) {
                           Map<String, String> var28 = new HashMap();
                           if (current.indexOf("*") == -1 && current.indexOf("?") == -1) {
                              current = current + ".*";
                           } else if (current.indexOf("*") != -1) {
                              current = current.replace("*", ".*");
                           } else {
                              current = current.replace("?", ".?");
                           }

                           var28.put("connectType", "not");
                           var28.put("queryType", "regexp");
                           if ("string".equals(dataType)) {
                              var28.put("expression", ESQueryUtils.getExpression(field, current, "string"));
                           } else {
                              var28.put("expression", ESQueryUtils.getExpression(field, current));
                           }

                           var28.put("ifSqlOrJson", "json");
                           requestData.addfilterMap(var28);
                        }
                     }
                     break;
                  case range:
                     if (StringUtils.isNullOrEmpry(value)) {
                        break;
                     }

                     String[] cvArraykx = value.split(",");
                     StringBuffer esExpression = new StringBuffer();
                     esExpression.append("[");
                     if ("0".equals(isNegate)) {
                        if (cvArraykx != null && cvArraykx.length > 0) {
                           for (String valueStr : cvArraykx) {
                              String[] params = valueStr.split("~");
                              double start = Double.parseDouble(params[0]);
                              double end = Double.parseDouble(params[1]);
                              if (isDuration) {
                                 if (!"n0avgSpeed".equals(indexField) && !"n1avgSpeed".equals(indexField)) {
                                    start = start * 1000.0 - 500.0 > 0.0 ? start * 1000.0 - 500.0 : 0.0;
                                    end = end * 1000.0 + 499.0;
                                 } else {
                                    start *= 1000.0;
                                    end *= 1000.0;
                                 }
                              }

                              esExpression.append(ESQueryUtils.getRangeExpression(field, start, end, true)).append(",");
                           }
                        }

                        if (esExpression.length() > 0) {
                           esExpression.deleteCharAt(esExpression.length() - 1);
                           esExpression.append("]");
                        }

                        map.put("connectType", "and");
                        map.put("queryType", "or");
                        map.put("expression", esExpression.toString());
                        map.put("ifSqlOrJson", "json");
                        requestData.addfilterMap(map);
                     } else if (cvArraykx != null && cvArraykx.length > 0) {
                        for (String valueStr : cvArraykx) {
                           map = new HashMap<>();
                           esExpression.setLength(0);
                           esExpression.append("[");
                           map.clear();
                           String[] params = valueStr.split("~");
                           double start = Double.parseDouble(params[0]);
                           double end = Double.parseDouble(params[1]);
                           if (isDuration) {
                              if (!"n0avgSpeed".equals(indexField) && !"n1avgSpeed".equals(indexField)) {
                                 start = start * 1000.0 - 500.0 > 0.0 ? start * 1000.0 - 500.0 : 0.0;
                                 end = end * 1000.0 + 499.0;
                              } else {
                                 start *= 1000.0;
                                 end *= 1000.0;
                              }
                           }

                           if (start != 0.0) {
                              esExpression.append(ESQueryUtils.getRangeExpression(field, 0, start, true))
                                 .append(",")
                                 .append(ESQueryUtils.getRangeExpression(field, end, Integer.MAX_VALUE, true))
                                 .append(",");
                           } else {
                              esExpression.append(ESQueryUtils.getRangeExpression(field, end, Integer.MAX_VALUE, true)).append(",");
                           }

                           if (esExpression.length() > 0) {
                              esExpression.deleteCharAt(esExpression.length() - 1);
                              esExpression.append("]");
                           }

                           map.put("connectType", "and");
                           map.put("queryType", "or");
                           map.put("expression", esExpression.toString());
                           map.put("ifSqlOrJson", "json");
                           requestData.addfilterMap(map);
                        }
                     }
                     break;
                  case timeRange:
                     if (StringUtils.isNullOrEmpry(value)) {
                        break;
                     }

                     String[] cvArrayk = value.split(",");
                     StringBuffer sb = new StringBuffer();
                     sb.append("[");
                     if (null == cvArrayk || cvArrayk.length == 0) {
                        break;
                     }

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
                        sb.append(ESQueryUtils.getRangeExpression(field, startTime, endTime, true)).append(",");
                     }

                     if (sb.length() > 1) {
                        map.put("connectType", "and");
                        map.put("queryType", "or");
                        sb.deleteCharAt(sb.length() - 1);
                        sb.append("]");
                        map.put("expression", sb.toString());
                        map.put("ifSqlOrJson", "json");
                        requestData.addfilterMap(map);
                     }
                     break;
                  case mulSel:
                     if (!StringUtils.isNullOrEmpry(value)) {
                        if ("0".equals(isNegate)) {
                           map.put("connectType", "must");
                        } else {
                           map.put("connectType", "must_not");
                        }

                        map.put("queryType", "terms");
                        if (!"string".equals(dataType)) {
                           map.put("expression", ESQueryUtils.getTermsExpression(field, value));
                        } else {
                           String result = "";
                           String[] arr = value.split(",");

                           for (String current : arr) {
                              result = result + "\"" + current + "\",";
                           }

                           if (!StringUtils.isNullOrEmpry(result)) {
                              result = result.substring(0, result.length() - 1);
                              map.put("expression", ESQueryUtils.getTermsExpression(field, result));
                           }
                        }

                        map.put("ifSqlOrJson", "json");
                        requestData.addfilterMap(map);
                     }
               }
            }
         }
      }
   }

   public static void groupProcessParam(RequestData requestData) {
      if (requestData != null) {
         Map<String, String> map = new HashMap<>();
         map.put("connectType", "must");
         map.put("queryType", "term");
         map.put("expression", ESQueryUtils.getExpression("processed", "0"));
         map.put("ifSqlOrJson", "json");
         requestData.addfilterMap(map);
      }
   }

   public static void groupTopicCondRequest(String topicCondition, RequestData requestData) throws ParseException, JsonParseException, JsonMappingException, IOException {
      builTopicFilter(topicCondition, requestData);
   }
}
