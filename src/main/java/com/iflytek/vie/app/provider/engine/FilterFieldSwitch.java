package com.iflytek.vie.app.provider.engine;

import com.iflytek.vie.app.pojo.dimension.DimensionConfig;

public class FilterFieldSwitch {
   public static String filterSwitch(String fieldName, DimensionConfig dimensionConfig, boolean isTime, boolean isSys) {
      String result = FieldType.string.toString();
      if (!isTime && !isSys) {
         int durationFlag = dimensionConfig.getFlag();
         if (durationFlag == 1) {
            result = FieldType.duration.toString();
         } else {
            String dt = dimensionConfig.getDataType();
            result = FieldType.string.toString().equalsIgnoreCase(dt) ? result : FieldType.numerical.toString();
         }
      }

      return result;
   }
}
