package com.iflytek.vie.app.provider.common;

import com.iflytek.vie.app.pojo.player.TagInfo;
import com.iflytek.vie.constants.IntegerType;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonFunPlatform {
   private static final Logger logger = LoggerFactory.getLogger(CommonFunPlatform.class);
   public static final String ANYDRILL_CHILDFILEDS = "child_fields";
   public static final String VOICEDIMINFO = "voiceDimInfo";
   public static final String TIME_POSITION = "timePosition";
   public static final String VOICEANALYSISINFO = "voiceAnalysisInfo";
   public static final String CONTENT_ORIGIN = "contentOrigin";
   public static final String CONTENT = "content";
   public static final String DATA_TYPE = "dataType";
   public static final String TASKITEMTYPE = "taskItemType";
   public static final String DURATION = "duration";
   public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
   public static final String LISTIDFIELD = "id";
   public static final String LISTIDNAME = "编号";
   public static final String TIMEFORMAT = "timeFormat";
   public static final String TIMEFORMATNAME = "来电时间";
   public static final String FILTERKEY = "key";
   public static final String FILTEROPT = "opt";
   public static final String FILTERVALUE = "value";
   public static final String FILTERTYPE = "type";
   public static final String OFFLINETAGID = "offLineTagId";
   public static final String REALTIMETAGID = "realTimeTagId";
   public static final String OFFLINETAGNAME = "offLineTagName";
   public static final String TIMESTAMP = "timestamp";

   public static long getTimestamp(String strDate) {
      long result = 0L;

      try {
         result = SDF.parse(strDate).getTime();
      } catch (Exception var4) {
         logger.error("method getTimestamp exception");
      }

      return result;
   }

   public static String getFilterSql(List<HashMap<String, String>> filterMapList, String fType, String filterConfig) {
      String result = "";

      try {
         String relation = "1".equals(fType) ? " OR " : " AND ";
         List<HashMap<String, String>> modelFilter = new ArrayList<>();

         for (HashMap<String, String> currentFilter : (List<HashMap<String, String>>)filterMapList) {
            int filterType = Integer.parseInt(currentFilter.get("type"));
            String opt = currentFilter.get("opt");
            String value = currentFilter.get("value");
            String key = currentFilter.get("key");
            switch (filterType) {
               case 0:
                  if (value.contains("~")) {
                     String[] v = value.split("~");
                     if (v.length > 1) {
                        result = result
                           + " (timestamp>="
                           + getTimestamp(v[0])
                           + " AND "
                           + "timestamp"
                           + "<="
                           + DateUtils.getDaybeforeOrAfter(SDF.parse(v[1]), 1).getTime()
                           + ")"
                           + relation;
                     } else {
                        result = result + " timestamp" + filteOptSwitch(opt) + getTimestamp(v[0]) + relation;
                     }
                  } else {
                     result = result + " timestamp" + filteOptSwitch(opt) + getTimestamp(value) + relation;
                  }
                  break;
               case 1:
                  result = result + " " + dealAttrFilterOpt(currentFilter) + relation;
               case 2:
               default:
                  break;
               case 3:
                  modelFilter.add(currentFilter);
                  break;
               case 4:
                  modelFilter.add(currentFilter);
                  break;
               case 5:
                  modelFilter.add(currentFilter);
            }
         }

         if (modelFilter.size() > 0) {
            result = result + dealModelFilter(modelFilter, fType) + relation;
         }

         result = result.length() > 1 ? result.substring(0, result.length() - relation.length() + 1) : result;
         if (!StringUtils.isAbsEmpry(result)) {
            result = "(" + result + ")";
         }

         if (!StringUtils.isAbsEmpry(filterConfig)) {
            result = result + " AND fulltext('" + filterConfig + "','content-query-parser')=true";
         }
      } catch (Exception var13) {
         logger.error("method getFilterSql exception");
      }

      return result;
   }

   public static String dealModelFilter(List<HashMap<String, String>> filterMapList, String fType) {
      String result = "";

      try {
         String relation = "1".equals(fType) ? "" : "+";

         for (HashMap<String, String> currentFilter : (List<HashMap<String, String>>)filterMapList) {
            int filterType = Integer.parseInt(currentFilter.get("type"));
            String value = currentFilter.get("value");
            String key = currentFilter.get("key");
            if (filterType == 3 || filterType == 5) {
               result = result + " " + relation + "" + "offLineTagId" + ":(" + value + ") ";
            }
         }

         result = result.length() > 0 ? "fulltext('(" + result + ")')=true" : "";
      } catch (Exception var9) {
         logger.error("method dealModelFilter exception");
      }

      return result;
   }

   private static String filteOptSwitch(String opt) {
      String result = "";
      switch (FilterOptPlatform.valueOf(opt)) {
         case eq:
            result = "=";
            break;
         case gt:
            result = ">";
            break;
         case lt:
            result = "<";
            break;
         case gte:
            result = ">=";
            break;
         case lte:
            result = "<=";
      }

      return result;
   }

   private static String dealAttrFilterOpt(HashMap<String, String> currentFilter) {
      String result = "";
      List list = new ArrayList();
      Object[] object = IntegerType.values();

      for (Object o : object) {
         list.add(o);
      }

      try {
         String opt = currentFilter.get("opt");
         String value = currentFilter.get("value");
         String key = currentFilter.get("key");
         switch (FilterOptPlatform.valueOf(opt)) {
            case eq:
               if (list.contains(key)) {
                  result = key + "=" + value;
               } else {
                  result = key + "='" + value + "'";
               }
               break;
            case ct:
               result = "fulltext('(" + key + ":(" + value + "))')=true";
         }
      } catch (Exception var8) {
         logger.error("method dealAttrFilterOpt exception");
      }

      return result;
   }

   public static TagInfo voicePositionToTime(TagInfo tagInfo, List<String> timeList, List<Integer> positionList, int contentLength, String duration) {
      TagInfo result = null;

      try {
         int durTime = Integer.parseInt(duration);

         for (int i = 0; i < positionList.size(); i++) {
            if (tagInfo.getBeginTime() - contentLength <= positionList.get(i) && tagInfo.getBeginTime() >= contentLength) {
               String timeStr = timeList.get(i);
               String[] timeArray = timeStr.split(",");
               int start = Integer.parseInt(timeArray[0]);
               int end = Integer.parseInt(timeArray[1]);
               start = start - 2000 > 0 ? start - 2000 : start;
               end = Math.min(end + 2000, durTime) - 1000;
               tagInfo.setBeginTime(start / 1000);
               tagInfo.setEndTime(end / 1000);
               result = tagInfo;
               break;
            }
         }
      } catch (Exception var12) {
         logger.error("method voicePositionToTime exception");
      }

      return result;
   }

   public static TagInfo voicePositionToTimeNew(TagInfo tagInfo, HashMap<Integer, String> positionMap) {
      TagInfo result = null;

      try {
         String timeStr = positionMap.get(tagInfo.getBeginTime());
         String[] timeArray = timeStr.split(",");
         int start = Integer.parseInt(timeArray[0]);
         int end = Integer.parseInt(timeArray[1]);
         start = start - 2000 > 0 ? start - 2000 : start;
         end += 2000;
         tagInfo.setBeginTime(start);
         tagInfo.setEndTime(end);
         result = tagInfo;
      } catch (Exception var7) {
         logger.error("method voicePositionToTimeNew exception");
      }

      return result;
   }

   public static TagInfo voicePositionToTimeByText(TagInfo tagInfo, int contentLength, int nextLength, String duration) {
      TagInfo result = null;

      try {
         if (contentLength == nextLength && contentLength == 0) {
            return tagInfo;
         }

         if (tagInfo.getBeginTime() + 1 > contentLength && tagInfo.getBeginTime() + 1 <= nextLength) {
            tagInfo.setBeginTime(tagInfo.getBeginTime() - contentLength);
            tagInfo.setEndTime(tagInfo.getEndTime() - contentLength);
            result = tagInfo;
         }
      } catch (Exception var6) {
         logger.error("method voicePositionToTimeByText exception");
      }

      return result;
   }

   public static String getParamsValue(HashMap<String, Object> currentMap, String key) {
      return currentMap.get(key) == null ? "" : currentMap.get(key).toString();
   }

   public static boolean isNumber(String str) {
      boolean isNumber = true;
      int i = str.length();

      while (--i >= 0) {
         int chr = str.charAt(i);
         if ((chr < 48 || chr > 57) && chr != 45 && chr != 46) {
            isNumber = false;
            break;
         }
      }

      return isNumber;
   }
}
