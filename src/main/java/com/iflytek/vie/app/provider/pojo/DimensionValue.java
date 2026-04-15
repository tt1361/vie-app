package com.iflytek.vie.app.provider.pojo;

import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import java.io.Serializable;
import java.util.Date;

public class DimensionValue implements Serializable {
   private static final long serialVersionUID = -4382697805028338773L;
   private Long dimensionId;
   private String dimValue;
   private Date updateTime;
   private DimensionConfig dimensionConfig;

   public DimensionConfig getDimensionConfig() {
      return this.dimensionConfig;
   }

   public void setDimensionConfig(DimensionConfig dimensionConfig) {
      this.dimensionConfig = dimensionConfig;
   }

   public Long getDimensionId() {
      return this.dimensionId;
   }

   public void setDimensionId(Long dimensionId) {
      this.dimensionId = dimensionId;
   }

   public String getDimValue() {
      return this.dimValue;
   }

   public void setDimValue(String dimValue) {
      this.dimValue = dimValue;
   }

   public Date getUpdateTime() {
      return this.updateTime;
   }

   public void setUpdateTime(Date updateTime) {
      this.updateTime = updateTime;
   }
}
