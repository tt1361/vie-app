package com.iflytek.vie.app.provider.ruleparse;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.utils.DateUtils;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterParser implements Serializable {
   private static final long serialVersionUID = -5241968939223481942L;
   private static final Logger logger = LoggerFactory.getLogger(FilterParser.class);
   private List<Filter> filters = new ArrayList<>();
   private String filterStr;
   private String autoFolterStr;
   private boolean upToNow;
   private String filterJson;
   private boolean ret = true;

   public FilterParser(String filterJson) {
      this.filterJson = filterJson;
   }

   public FilterParser() {
   }

   public void parserFilters() throws ParseException {
      if (this.filterJson != null && !"".equals(this.filterJson.trim())) {
         List<Map<String, Object>> filterMaps = (List<Map<String, Object>>)JSON.parseObject(this.filterJson, List.class);
         StringBuffer filterBuffer = new StringBuffer("<filters>");
         StringBuffer autoFilterBuffer = new StringBuffer("<filters>");
         int count = 0;

         for (Map<String, Object> singleFilterMap : (List<Map<String, Object>>)filterMaps) {
            String key = String.valueOf(singleFilterMap.get("key"));
            String type = String.valueOf(singleFilterMap.get("type"));
            if ("null".equals(type)) {
               type = "mulSel";
            }

            String dataType = String.valueOf(singleFilterMap.get("dataType"));
            if ("null".equals(dataType)) {
               dataType = "string";
            }

            Object value = singleFilterMap.get("value");
            String exclude = String.valueOf(singleFilterMap.get("exclude"));
            List<String> valueList = (List<String>)JSON.parseObject(JSON.toJSONString(value), List.class);
            String name = String.valueOf(singleFilterMap.get("name"));
            Filter filter = new Filter(name, key, dataType, type, value, exclude);
            this.filters.add(filter);
            if ("timestamp".equals(key)) {
               String upToNow = singleFilterMap.get("uptonow") == null ? "0" : String.valueOf(singleFilterMap.get("uptonow"));
               if (Integer.parseInt(upToNow) == 1) {
                  this.upToNow = true;
               } else {
                  this.upToNow = false;
               }

               filterBuffer.append(filter.toFilterStr());
            } else {
               String str = filter.toFilterStr();
               filterBuffer.append(str);
               autoFilterBuffer.append(str);
               count++;
            }
         }

         filterBuffer.append("</filters>");
         autoFilterBuffer.append("</filters>");
         this.filterStr = filterBuffer.toString();
         if (count != 0) {
            this.autoFolterStr = autoFilterBuffer.toString();
         }
      } else {
         this.ret = false;
      }
   }

   public void parserFiltersOnline() throws ParseException {
      if (this.filterJson != null && !"".equals(this.filterJson.trim())) {
         List<Map<String, Object>> filterMaps = (List<Map<String, Object>>)JSON.parseObject(this.filterJson, List.class);
         StringBuffer filterBuffer = new StringBuffer("<filters>");
         StringBuffer autoFilterBuffer = new StringBuffer("<filters>");
         int count = 0;

         for (Map<String, Object> singleFilterMap : (List<Map<String, Object>>)filterMaps) {
            String key = String.valueOf(singleFilterMap.get("key"));
            String type = String.valueOf(singleFilterMap.get("type"));
            String dataType = String.valueOf(singleFilterMap.get("dataType"));
            String exclude = String.valueOf(singleFilterMap.get("exclude"));
            Object value = singleFilterMap.get("value");
            if ("silenceLong".equals(key) || "duration".equals(key) || "inVadDuration".equals(key)) {
               value = this.filterTime(value);
            }

            List<String> valueList = (List<String>)JSON.parseObject(JSON.toJSONString(value), List.class);
            String name = String.valueOf(singleFilterMap.get("name"));
            Filter filter = new Filter(name, key, dataType, type, value, exclude);
            this.filters.add(filter);
            if ("timestamp".equals(key)) {
               String upToNow = singleFilterMap.get("uptonow") == null ? "0" : String.valueOf(singleFilterMap.get("uptonow"));
               if (Integer.parseInt(upToNow) == 1) {
                  this.upToNow = true;
               } else {
                  this.upToNow = false;
               }

               filterBuffer.append(filter.toFilterStr());
            } else {
               String str = filter.toFilterStr();
               filterBuffer.append(str);
               autoFilterBuffer.append(str);
               count++;
            }
         }

         filterBuffer.append("</filters>");
         autoFilterBuffer.append("</filters>");
         this.filterStr = filterBuffer.toString();
         if (count != 0) {
            this.autoFolterStr = autoFilterBuffer.toString();
         }
      } else {
         this.ret = false;
      }
   }

   public void parserTopicFiltersOnline() throws ParseException {
      if (this.filterJson != null && !"".equals(this.filterJson.trim())) {
         List<Map<String, Object>> filterMaps = (List<Map<String, Object>>)JSON.parseObject(this.filterJson, List.class);
         StringBuffer filterBuffer = new StringBuffer("<filters>");
         StringBuffer autoFilterBuffer = new StringBuffer("<filters>");
         int count = 0;

         for (Map<String, Object> singleFilterMap : (List<Map<String, Object>>)filterMaps) {
            String key = String.valueOf(singleFilterMap.get("key"));
            String type = String.valueOf(singleFilterMap.get("type"));
            String dataType = String.valueOf(singleFilterMap.get("dataType"));
            String exclude = String.valueOf(singleFilterMap.get("exclude"));
            Object value = singleFilterMap.get("value");
            String timeType = String.valueOf(singleFilterMap.get("timeType"));
            String timeValue = String.valueOf(singleFilterMap.get("timeValue"));
            if ("silenceLong".equals(key) || "duration".equals(key) || "inVadDuration".equals(key)) {
               value = this.filterTime(value);
            }

            if ("2".equals(timeType) && "timestamp".equals(key)) {
               SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
               Date now = new Date();
               String end = sdf.format(now);
               String start = DateUtils.getDaybeforeOrAfter(end, Integer.parseInt(timeValue) + 1);
               JSONArray valueList = (JSONArray)value;
               valueList.set(0, start);
               valueList.set(1, end);
            }

            List<String> valueList = (List<String>)JSON.parseObject(JSON.toJSONString(value), List.class);
            String name = String.valueOf(singleFilterMap.get("name"));
            Filter filter = new Filter(name, key, dataType, type, value, exclude);
            this.filters.add(filter);
            if ("timestamp".equals(key)) {
               String upToNow = singleFilterMap.get("uptonow") == null ? "0" : String.valueOf(singleFilterMap.get("uptonow"));
               if (Integer.parseInt(upToNow) == 1) {
                  this.upToNow = true;
               } else {
                  this.upToNow = false;
               }

               filterBuffer.append(filter.toFilterStr());
            } else {
               String str = filter.toFilterStr();
               filterBuffer.append(str);
               autoFilterBuffer.append(str);
               count++;
            }
         }

         filterBuffer.append("</filters>");
         autoFilterBuffer.append("</filters>");
         this.filterStr = filterBuffer.toString();
         if (count != 0) {
            this.autoFolterStr = autoFilterBuffer.toString();
         }
      } else {
         this.ret = false;
      }
   }

   public static boolean filterChange(List<Filter> oldList, List<Filter> newList) {
      ObjectMapper mapper = new ObjectMapper();
      if (newList == null && oldList == null) {
         return false;
      } else if (newList != null && oldList != null) {
         if (newList.size() != oldList.size()) {
            return true;
         } else {
            try {
               String newJson = mapper.writeValueAsString(newList);
               String oldJson = mapper.writeValueAsString(oldList);
               if (!newJson.equals(oldJson)) {
                  return true;
               }
            } catch (Exception var7) {
               logger.error("[filterChange]出现异常", var7);
            }

            Map<String, Filter> newMap = listToMap(newList);
            Map<String, Filter> oldMap = listToMap(oldList);

            for (Entry<String, Filter> entry : newMap.entrySet()) {
               if (!oldMap.containsKey(entry.getKey())) {
                  return true;
               }

               if (!entry.getValue().equals(oldMap.get(entry.getKey()))) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   public static void listToSet(List list, Set set) {
      if (list != null) {
         for (Object object : list) {
            set.add(object);
         }
      }
   }

   public static Map<String, Filter> listToMap(List<Filter> list) {
      Map<String, Filter> map = new HashMap<>();
      if (list == null) {
         return map;
      } else {
         for (Filter filter : list) {
            map.put(filter.getKey(), filter);
         }

         return map;
      }
   }

   public List<Filter> getFilters() {
      return this.filters;
   }

   public void setFilters(List<Filter> filters) {
      this.filters = filters;
   }

   public String getFilterStr() {
      return this.filterStr;
   }

   public void setFilterStr(String filterStr) {
      this.filterStr = filterStr;
   }

   public String getAutoFolterStr() {
      return this.autoFolterStr;
   }

   public void setAutoFolterStr(String autoFolterStr) {
      this.autoFolterStr = autoFolterStr;
   }

   public boolean isUpToNow() {
      return this.upToNow;
   }

   public void setUpToNow(boolean upToNow) {
      this.upToNow = upToNow;
   }

   public String getFilterJson() {
      return this.filterJson;
   }

   public void setFilterJson(String filterJson) {
      this.filterJson = filterJson;
   }

   public boolean isRet() {
      return this.ret;
   }

   public void setRet(boolean ret) {
      this.ret = ret;
   }

   private Object filterTime(Object value) {
      System.out.println(value);
      List valuse = (List)value;
      JSONArray jsonArray = new JSONArray();

      for (int i = 0; i < valuse.size(); i++) {
         String v = (String)valuse.get(i);
         String[] arr = v.split("~");
         if (arr.length > 1) {
            String st = Integer.parseInt(arr[0]) * 1000 - 500 + "";
            String le = Integer.parseInt(arr[1]) * 1000 + 499 + "";
            v = st + "~" + le;
         }

         jsonArray.add(v);
      }

      return jsonArray;
   }
}
