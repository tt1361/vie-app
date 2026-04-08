package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.Comparator;

public class DataInfoComprators implements Comparator<DataInfo>, Serializable {
   private String order;
   private int desc;

   public DataInfoComprators(String order, int desc) {
      this.order = order;
      this.desc = desc;
   }

   public int compare(DataInfo o1, DataInfo o2) {
      int v1High = this.highScore(o1.getMark(), o1.getIsExist());
      int v2High = this.highScore(o2.getMark(), o2.getIsExist());
      if (v1High > v2High) {
         return -1;
      } else if (v1High < v2High) {
         return 1;
      } else {
         Object oV1 = o1.getDataMaps() == null ? null : o1.getDataMaps().get(this.order);
         Object oV2 = o2.getDataMaps() == null ? null : o2.getDataMaps().get(this.order);
         return this.compareOrder(oV1, oV2);
      }
   }

   public int highScore(int first, int end) {
      if (first == 1 && end == 0) {
         return 4;
      } else if (first == -1 && end == 1) {
         return 3;
      } else if (first == 1 && end == 1) {
         return 2;
      } else {
         return first == -1 && end == 0 ? 1 : 0;
      }
   }

   public int compareOrder(Object o1, Object o2) {
      if (o1 == null && o2 == null) {
         return 0;
      } else if (o1 != null && o2 == null) {
         return 1;
      } else if (o1 == null && o2 != null) {
         return -1;
      } else if (o1 instanceof String) {
         float ret = String.valueOf(o1).compareTo(String.valueOf(o2));
         return this.compareRet(ret);
      } else if (o1 instanceof Long) {
         float ret = (float)((Long)o1 - (Long)o2);
         return this.compareRet(ret);
      } else if (o1 instanceof Integer) {
         float ret = (Integer)o1 - (Integer)o2;
         return this.compareRet(ret);
      } else if (o1 instanceof Double) {
         float ret = (float)((Double)o1 - (Double)o2);
         return this.compareRet(ret);
      } else if (o1 instanceof Float) {
         float ret = (Float)o1 - (Float)o2;
         return this.compareRet(ret);
      } else {
         return 0;
      }
   }

   public int compareRet(float v) {
      if (v > 0.0F) {
         return this.desc == 1 ? 1 : -1;
      } else if (v < 0.0F) {
         return this.desc == 1 ? -1 : 1;
      } else {
         return 0;
      }
   }
}
