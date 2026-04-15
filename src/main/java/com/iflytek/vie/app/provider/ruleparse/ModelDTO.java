package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ModelDTO implements Serializable {
   private static final long serialVersionUID = 2558313584553857735L;
   private String modelGroup;
   private Long modelGroupId;
   private List<SpecDimValue> value = new ArrayList<>();

   public String getModelGroup() {
      return this.modelGroup;
   }

   public void setModelGroup(String modelGroup) {
      this.modelGroup = modelGroup;
   }

   public Long getModelGroupId() {
      return this.modelGroupId;
   }

   public void setModelGroupId(Long modelGroupId) {
      this.modelGroupId = modelGroupId;
   }

   public List<SpecDimValue> getValue() {
      return this.value;
   }

   public void setValue(List<SpecDimValue> value) {
      this.value = value;
   }
}
