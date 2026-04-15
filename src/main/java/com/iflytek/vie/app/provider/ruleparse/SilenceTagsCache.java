package com.iflytek.vie.app.provider.ruleparse;

import com.iflytek.vie.app.pojo.model.TagDimension;
import com.iflytek.vie.app.pojo.model.TagDimensionProperty;
import com.iflytek.vie.app.pojo.model.TagPropertyOperation;
import com.iflytek.vie.app.provider.database.ModelTagDB;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SilenceTagsCache {
   private static final Logger logger = LoggerFactory.getLogger(SilenceTagsCache.class);
   public static ModelTagDB modelTagDB;
   public static Map<Integer, List<TagDimension>> tagDimensions = new HashMap<>();
   public static Map<Long, List<TagDimensionProperty>> tagProperties = new HashMap<>();
   public static Map<Long, List<TagPropertyOperation>> tagOperations = new HashMap<>();

   public static void init() {
      try {
         logger.debug("###初始化获取静音标签开始!###");
         List<TagDimension> dimensionList = modelTagDB.getTagDimension(1);
         tagDimensions.put(1, dimensionList);

         for (int i = 0; i < dimensionList.size(); i++) {
            TagDimension td = dimensionList.get(i);
            List<TagDimensionProperty> tagProsList = modelTagDB.getPropertys(td.getDimensionId(), td.getDimensionCode(), false);
            tagProperties.put(td.getDimensionId(), tagProsList);

            for (int j = 0; j < tagProsList.size(); j++) {
               TagDimensionProperty tdp = tagProsList.get(j);
               if (!tagOperations.containsKey(tdp.getPropertyId())) {
                  List<TagPropertyOperation> tpo = modelTagDB.getOperations(tdp.getPropertyId(), tdp.getPropertyCode(), false);
                  tagOperations.put(tdp.getPropertyId(), tpo);
               }
            }
         }

         logger.debug("###初始化获取静音标签结束!###");
      } catch (Exception var7) {
         logger.error("####获取静音方式失败", var7);
      }
   }

   public void setModelTagDB(ModelTagDB modelTagDB) {
      SilenceTagsCache.modelTagDB = modelTagDB;
   }
}
