package com.iflytek.vie.app.provider.impl.datadrill.parse;

import com.iflytek.vie.app.pojo.datadrill.FilterRuleEnum;
import com.iflytek.vie.app.provider.engine.FieldType;
import com.iflytek.vie.app.provider.engine.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ModelFilterParse extends FilterParse {
   private Map<Long, String> allModelMap;
   private List<Long> modelIds;
   private String dataSource;

   public ModelFilterParse(String key, Object value, FilterRuleEnum ruleEnum, FieldType fieldType, Map<Long, String> allModelMap) {
      super(key, value, ruleEnum, fieldType);
      this.allModelMap = allModelMap;
      this.modelIds = new ArrayList<>(this.allModelMap.keySet());
   }

   @Override
   public String getSql() {
      String sql = "";
      String condition = this.toCondition();
      if (!StringUtils.isNullOrEmpry(condition) && !"error".equals(condition)) {
         switch (this.ruleEnum) {
            case IN:
               sql = "id in " + condition;
               break;
            case NOT_IN:
               sql = "id not in " + condition;
               break;
            default:
               sql = "!error[model opt must be in or not_in ,besides no other opts]";
         }

         return sql;
      } else {
         return "!error[model condition is null]";
      }
   }

   @Override
   public String toCondition() {
      List<Object> valueList = (List<Object>)this.value;
      if (valueList != null && valueList.size() != 0) {
         List<Long> newModelIds = new ArrayList<>();
         boolean isfindModel = false;

         for (int i = 0; i < valueList.size(); i++) {
            Long modelId = Long.valueOf(this.toObject(valueList.get(i)).toString());
            if (this.modelIds.contains(modelId)) {
               if (StringUtils.isNullOrEmpry(this.dataSource) || !isfindModel) {
                  this.dataSource = this.allModelMap.get(modelId);
                  isfindModel = true;
               }

               newModelIds.add(modelId);
            } else {
               System.out.println("######" + modelId + " is not online model");
            }
         }

         if (newModelIds.size() > 0) {
            String offLineTag = "(select id from " + this.dataSource + "model where ";

            for (int ix = 0; ix < newModelIds.size(); ix++) {
               if (ix == 0) {
                  offLineTag = offLineTag + "(";
               }

               offLineTag = offLineTag + "offLineTagId=" + newModelIds.get(ix);
               if (ix < newModelIds.size() - 1) {
                  offLineTag = offLineTag + " or ";
               } else {
                  offLineTag = offLineTag + ")";
               }
            }

            return offLineTag + " group by id order by id)";
         } else {
            return valueList.size() > 0 && newModelIds.size() == 0 ? "error" : null;
         }
      } else {
         return "!error[model value must be not null!]";
      }
   }

   public void setAllModelMap(Map<Long, String> allModelMap) {
      this.allModelMap = allModelMap;
   }

   public void setModelIds(List<Long> modelIds) {
      this.modelIds = modelIds;
   }

   public void setDataSource(String dataSource) {
      this.dataSource = dataSource;
   }
}
