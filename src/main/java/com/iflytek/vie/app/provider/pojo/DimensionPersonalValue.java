package com.iflytek.vie.app.provider.pojo;

import com.iflytek.vie.app.pojo.dimension.DimensionPersonal;

public class DimensionPersonalValue {
   private long id;
   private String dimensionName;
   private String dimensionColor;
   private int showIndex;
   private DimensionPersonal dimensionPersonal;

   public long getId() {
      return this.id;
   }

   public void setId(long id) {
      this.id = id;
   }

   public String getDimensionName() {
      return this.dimensionName;
   }

   public void setDimensionName(String dimensionName) {
      this.dimensionName = dimensionName;
   }

   public String getDimensionColor() {
      return this.dimensionColor;
   }

   public void setDimensionColor(String dimensionColor) {
      this.dimensionColor = dimensionColor;
   }

   public int getShowIndex() {
      return this.showIndex;
   }

   public void setShowIndex(int showIndex) {
      this.showIndex = showIndex;
   }

   public DimensionPersonal getDimensionPersonal() {
      return this.dimensionPersonal;
   }

   public void setDimensionPersonal(DimensionPersonal dimensionPersonal) {
      this.dimensionPersonal = dimensionPersonal;
   }
}
