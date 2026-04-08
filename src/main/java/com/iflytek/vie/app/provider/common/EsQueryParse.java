package com.iflytek.vie.app.provider.common;

import com.iflytek.vie.utils.DateUtils;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang.StringUtils;

public class EsQueryParse {
   public static String timestamp = "timestamp";

   public static String getResultQuery(String... subQueryList) {
      StringBuffer result = new StringBuffer(
         "{\n    \"size\":0,\n    \"query\":{\n       \"filtered\": {\n              \"filter\":{\n                    \"and\":[\n                        {\"match_all\":{}}"
      );

      for (String subQuery : subQueryList) {
         result.append(",").append(subQuery);
      }

      result.append("                ]\n              }\n          }\n      } \n}");
      return result.toString();
   }

   public static String getTimeRangeQuery(String startTime, String endTime) {
      String queryString = "";
      if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
         queryString = " {\n                   \"range\": {\n                        \""
            + timestamp
            + "\": {\n"
            + "                             \"gte\":"
            + DateUtils.getTimestamp(startTime, "yyyy-MM-dd HH:mm:ss")
            + ",\n"
            + "                              \"lte\": "
            + DateUtils.getTimestamp(endTime, "yyyy-MM-dd HH:mm:ss")
            + "\n"
            + "                        }\n"
            + "                    }\n"
            + "                }";
      }

      return queryString;
   }

   public static String getTimeRangeQuery(String field, Long startTime, Long endTime) {
      String queryString = "";
      return " {\n                   \"range\": {\n                        \""
         + field
         + "\": {\n"
         + "                             \"gte\":"
         + startTime
         + ",\n"
         + "                              \"lte\": "
         + endTime
         + "\n"
         + "                        }\n"
         + "                    }\n"
         + "                }";
   }

   public static String getRangeQuery(String key, double start, double end) {
      String queryString = "";
      return " {\n                   \"range\": {\n                        \""
         + key
         + "\": {\n"
         + "                             \"gte\":"
         + start
         + ",\n"
         + "                              \"lte\": "
         + end
         + "\n"
         + "                        }\n"
         + "                    }\n"
         + "                }";
   }

   public static String getMulSelQuery(String key, List<Object> valueList) {
      StringBuffer result = new StringBuffer("");

      try {
         String cond = "";
         if (valueList != null && valueList.size() > 0) {
            if (valueList.get(0) instanceof String) {
               for (Object current : valueList) {
                  cond = cond + "\"" + current + "\",";
               }
            } else {
               for (Object current : valueList) {
                  cond = cond + "" + current + ",";
               }
            }
         }

         cond = cond.substring(0, cond.length() - 1);
         result = result.append("{\"terms\":{\"" + key + "\":[" + cond + "]}}");
      } catch (Exception var6) {
         var6.printStackTrace();
      }

      return result.toString();
   }

   public static String getEquQuery(String key, String value) {
      StringBuffer result = new StringBuffer("");
      result.append("{\"regexp\":{\"" + key + "\": \"" + value + "\"}}");
      return result.toString();
   }

   public static String getMulEquQuery(String key, List<String> valueList) {
      String result = "";
      List<String> queryList = new LinkedList<>();

      for (String value : valueList) {
         queryList.add("{\"regexp\":{\"" + key + "\": \"" + value + "\"}}");
      }

      return getOrQuery(queryList.toArray(new String[queryList.size()]));
   }

   public static String getTermQuery(String key, Object value) {
      StringBuffer result = new StringBuffer("");

      try {
         String cond = "";
         if (value instanceof String) {
            cond = cond + "\"" + value + "\"";
         } else {
            cond = cond + "" + value;
         }

         result = result.append("{\"term\":{\"" + key + "\":" + cond + "}}");
      } catch (Exception var4) {
         var4.printStackTrace();
      }

      return result.toString();
   }

   public static String getNotQuery(String query) {
      return "{\"not\": " + query + "}";
   }

   public static String getOrQuery(String... subQueryLits) {
      StringBuffer resultBuffer = new StringBuffer("");
      if (subQueryLits.length > 0) {
         resultBuffer.append("{\"or\":[");

         for (String subQuery : subQueryLits) {
            resultBuffer.append(subQuery).append(",");
         }

         resultBuffer = resultBuffer.deleteCharAt(resultBuffer.length() - 1);
         resultBuffer.append("]}");
      }

      return resultBuffer.toString();
   }

   public static String getAndQuery(String... subQueryLits) {
      StringBuffer resultBuffer = new StringBuffer("");
      if (subQueryLits.length > 0) {
         resultBuffer.append("{\"and\":[");

         for (String subQuery : subQueryLits) {
            resultBuffer.append(subQuery).append(",");
         }

         resultBuffer = resultBuffer.deleteCharAt(resultBuffer.length() - 1);
         resultBuffer.append("]}");
      }

      return resultBuffer.toString();
   }
}
