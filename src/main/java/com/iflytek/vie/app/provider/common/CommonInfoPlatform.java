package com.iflytek.vie.app.provider.common;

import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class CommonInfoPlatform implements Serializable {
   private static final long serialVersionUID = -5215707272281088884L;

   public static HashMap<String, DimensionConfig> getDimensinMap(List<DimensionConfig> dimensionConfigList) {
      HashMap<String, DimensionConfig> result = new HashMap<>();

      for (DimensionConfig dimensionConfig : dimensionConfigList) {
         result.put(dimensionConfig.getIndexField(), dimSwitch(dimensionConfig));
      }

      return result;
   }

   private static DimensionConfig dimSwitch(DimensionConfig dimensionConfig) {
      DimensionConfig newDim = new DimensionConfig();
      newDim.setAnalysis(dimensionConfig.getAnalysis());
      newDim.setAvailable(dimensionConfig.getAvailable());
      newDim.setCreator(dimensionConfig.getCreator());
      newDim.setDataSource(dimensionConfig.getDataSource());
      newDim.setDataType(dimensionConfig.getDataType());
      newDim.setFlag(dimensionConfig.getFlag());
      newDim.setGenValue(dimensionConfig.getGenValue());
      newDim.setId(dimensionConfig.getId());
      newDim.setIndexField(dimensionConfig.getIndexField());
      newDim.setModifyStatus(dimensionConfig.getModifyStatus());
      newDim.setOnlineType(dimensionConfig.getOnlineType());
      newDim.setShow(dimensionConfig.getShow());
      newDim.setShowName(dimensionConfig.getShowName());
      newDim.setShowType(dimensionConfig.getShowType());
      newDim.setTaskType(dimensionConfig.getTaskType());
      newDim.setValueList(dimensionConfig.getValueList());
      return newDim;
   }
}
