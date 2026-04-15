package com.iflytek.vie.app.provider.ruleparse;

import com.alibaba.fastjson.JSON;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Filter implements Serializable {
   private static final long serialVersionUID = 8753020347762770050L;
   private static final Logger logger = LoggerFactory.getLogger(Filter.class);
   private String key;
   private String name;
   private String dataType;
   private String type;
   private Object value;
   private List convertValue = new ArrayList();
   private String exclude;

   public Filter() {
   }

   public Filter(String name, String key, String dataType, String type, Object value) {
      this.key = key;
      this.name = name;
      this.value = value;
      this.dataType = dataType;
      this.type = type;
      this.value = value;
   }

   public Filter(String name, String key, String dataType, String type, Object value, String exclude) {
      this.key = key;
      this.name = name;
      this.value = value;
      this.dataType = dataType;
      this.type = type;
      this.value = value;
      this.exclude = exclude;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof Filter)) {
         return false;
      } else {
         Filter filter = (Filter)o;
         if (this.value instanceof List) {
            List<String> thisValue = (List<String>)this.value;
            List<String> otherValue = (List<String>)filter.getValue();
            if (filter.getValue() == null) {
               return false;
            } else if (thisValue.size() != otherValue.size()) {
               return false;
            } else if (DimType.timeRange.getName().equals(filter.getType())) {
               return this.timeCompare(this, filter);
            } else {
               Set<String> thisSet = new HashSet<>();
               Set<String> otherSet = new HashSet<>();
               FilterParser.listToSet(thisValue, thisSet);
               FilterParser.listToSet(otherValue, otherSet);
               return this.valueCompare(thisSet, otherSet) && this.valueCompare(otherSet, thisSet);
            }
         } else if (this.value == null || filter.getValue() == null) {
            return true;
         } else {
            return this.value == null && filter.getValue() == null ? false : this.value.equals(filter.getValue());
         }
      }
   }

   @Override
   public int hashCode() {
      int result = 17;
      if (this.value instanceof List) {
         result *= 31;
      } else {
         result = result * 31 + (this.value == null ? 0 : this.value.hashCode());
      }

      return result;
   }

   public String toFilterStr() throws ParseException {
      StringBuffer singleFilter = new StringBuffer();
      String ex = "false";
      if (!StringUtils.isNullOrEmpry(this.exclude) && !"null".equals(this.exclude)) {
         ex = this.exclude;
      }

      singleFilter.append(
         "<filter name='"
            + this.key
            + "'"
            + " exclude='"
            + ex
            + "' showType='"
            + this.type
            + "'"
            + " type='tag' "
            + " dataType='"
            + this.dataType
            + "'"
            + " showName='"
            + this.name
            + "'"
            + " value='"
      );
      List<String> values = (List<String>)this.value;
      if (values != null && values.size() != 0) {
         if (DimType.timeRange.getName().equals(this.type)) {
            long start = DateUtils.dateToTimestamp(values.get(0), "yyyy-MM-dd HH:mm:ss", 0L);
            singleFilter.append(start + "~");
            this.convertValue.add(start);
            long end;
            if ("".equals(values.get(1))) {
               end = Long.MAX_VALUE;
               singleFilter.append(Long.MAX_VALUE);
            } else {
               end = DateUtils.dateToTimestamp(values.get(1), "yyyy-MM-dd HH:mm:ss", 0L);
               singleFilter.append(end);
            }

            this.convertValue.add(end);
         } else if (DimType.range.getName().equals(this.type)) {
            this.convertValue = values;
            singleFilter.append(JSON.toJSONString(values));
         } else if (DimType.mulSel.getName().equals(this.type) || DimType.mulEqu.getName().equals(this.type)) {
            if (values.size() == 1) {
               String realValue = this.parserDataType(values.get(0), this.dataType);
               this.convertValue.add(realValue);
               singleFilter.append("[\"" + realValue + "\"]");
            } else {
               List<String> realValues = new ArrayList<>();

               for (String singleValue : values) {
                  String realValue = this.parserDataType(singleValue, this.dataType);
                  this.convertValue.add(realValue);
                  realValues.add(realValue);
               }

               singleFilter.append(JSON.toJSONString(realValues));
            }
         }

         singleFilter.append("'>");
         singleFilter.append("</filter>");
         return singleFilter.toString();
      } else {
         return null;
      }
   }

   public String toSqlCondition() {
      StringBuffer sqlConBuffer = new StringBuffer();
      if (StringUtils.isNullOrEmpry(this.dataType) || "null".equals(this.dataType)) {
         this.dataType = "string";
      }

      boolean isString;
      if ("string".equals(this.dataType)) {
         isString = true;
      } else {
         isString = false;
      }

      if (StringUtils.isNullOrEmpry(this.type) || "null".equals(this.type)) {
         this.type = "mulSel";
      }

      switch (DimType.valueOf(this.type)) {
         case timeRange:
            sqlConBuffer.append(this.key + " between " + this.convertValue.get(0) + " and " + this.convertValue.get(1));
            break;
         case range:
            this.listToSql(sqlConBuffer, true, isString);
            break;
         case mulSel:
            this.listToSql(sqlConBuffer, false, isString);
            break;
         case mulEqu:
            String conValue = "";
            conValue = this.dealMulEqlCond(this.convertValue);
            String opt = "true";
            if ("true".equals(this.exclude)) {
               opt = "false";
            }

            if (!StringUtils.isNullOrEmpry(conValue)) {
               sqlConBuffer.append("fulltext('" + this.key + ":(" + conValue + ")')=" + opt);
            }
      }

      return sqlConBuffer.toString();
   }

   public void listToSql(StringBuffer stringBuffer, boolean isRange, boolean isString) {
      int count = 0;
      if (this.convertValue != null && this.convertValue.size() != 0) {
         for (Object singleValue : this.convertValue) {
            String valueStr = String.valueOf(singleValue);
            if ("true".equals(this.exclude)) {
               if (count++ != 0) {
                  stringBuffer.append(" and ");
               }

               if (isString) {
                  if (valueStr.contains("*")) {
                     stringBuffer.append(" fulltext('" + this.key + ":(" + valueStr + ")')=false ");
                  } else {
                     stringBuffer.append(this.key + " !='" + valueStr + "'");
                  }
               } else {
                  stringBuffer.append(this.key + " !=" + valueStr);
               }
            } else {
               if (count++ != 0) {
                  stringBuffer.append(" or ");
               }

               if (isRange) {
                  String[] details = valueStr.split("~");
                  stringBuffer.append(this.key + " between " + details[0] + " and " + details[1]);
               } else if (isString) {
                  if (valueStr.contains("*")) {
                     stringBuffer.append(" fulltext('" + this.key + ":(" + valueStr + ")')=true ");
                  } else {
                     stringBuffer.append(this.key + "='" + valueStr + "'");
                  }
               } else {
                  stringBuffer.append(this.key + "=" + valueStr);
               }
            }
         }
      }
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
         logger.error("处理单多选条件异常", var6);
      }

      return result;
   }

   private String parserDataType(String value, String dataType) {
      return value;
   }

   private boolean valueCompare(Set<String> s1, Set<String> s2) {
      for (String value : s1) {
         if (!s2.contains(value)) {
            return false;
         }
      }

      return true;
   }

   private boolean timeCompare(Filter filter1, Filter filter2) {
      long oldStart = (Long)filter1.getConvertValue().get(0);
      long oldEnd = (Long)filter1.getConvertValue().get(1);
      long newStart = (Long)filter2.getConvertValue().get(0);
      long newEnd = (Long)filter2.getConvertValue().get(1);
      return oldStart >= newStart && oldEnd <= newEnd;
   }

   public String getKey() {
      return this.key;
   }

   public void setKey(String key) {
      this.key = key;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getDataType() {
      return this.dataType;
   }

   public void setDataType(String dataType) {
      this.dataType = dataType;
   }

   public String getType() {
      return this.type;
   }

   public void setType(String type) {
      this.type = type;
   }

   public Object getValue() {
      return this.value;
   }

   public void setValue(Object value) {
      this.value = value;
   }

   public List getConvertValue() {
      return this.convertValue;
   }

   public void setConvertValue(List convertValue) {
      this.convertValue = convertValue;
   }

   public String getExclude() {
      return this.exclude;
   }

   public void setExclude(String exclude) {
      this.exclude = exclude;
   }
}
