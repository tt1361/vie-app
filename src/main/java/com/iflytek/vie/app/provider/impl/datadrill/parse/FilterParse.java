package com.iflytek.vie.app.provider.impl.datadrill.parse;

import com.iflytek.vie.app.pojo.datadrill.FilterRuleEnum;
import com.iflytek.vie.app.provider.engine.FieldType;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public abstract class FilterParse {
   public String key;
   public Object value;
   public FilterRuleEnum ruleEnum;
   public FieldType fieldType;
   public static final DecimalFormat DF = new DecimalFormat("0.000");
   public static final DecimalFormat DF_NOEND = new DecimalFormat("0");
   public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
   public static final String[] SPECIALCHARACTERS = new String[]{"(", ")", "^", "{", "}"};

   public abstract String getSql();

   public abstract String toCondition();

   public Object toObject(Object object) {
      if (object.getClass() == Integer.class) {
         return (Integer)object;
      } else if (object.getClass() == double.class) {
         return (Double)object;
      } else if (object.getClass() == BigDecimal.class) {
         return (BigDecimal)object;
      } else if (object.getClass() == Double.class) {
         return (Double)object;
      } else if (object.getClass() == String.class) {
         return (String)object;
      } else if (object.getClass() == long.class) {
         return (Long)object;
      } else if (object.getClass() == Long.class) {
         return (Long)object;
      } else {
         throw new RuntimeException("value can't match java base type :[key:" + this.key + ",value:" + object + "]");
      }
   }

   public double toDouble(Object object) {
      if (object.getClass() == String.class) {
         String duration = (String)object;

         try {
            return DF.parse(duration).doubleValue();
         } catch (ParseException var4) {
            throw new RuntimeException("value can't conver number type :[key:" + this.key + ",value:" + object + "]");
         }
      } else {
         throw new RuntimeException("value can't match number type :[key:" + this.key + ",value:" + object + "]");
      }
   }

   public FilterParse(String key, Object value, FilterRuleEnum ruleEnum, FieldType fieldType) {
      this.key = key;
      this.value = value;
      this.ruleEnum = ruleEnum;
      this.fieldType = fieldType;
   }
}
