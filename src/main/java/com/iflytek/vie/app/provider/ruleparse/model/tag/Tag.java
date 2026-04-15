package com.iflytek.vie.app.provider.ruleparse.model.tag;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.iflytek.vie.app.provider.ruleparse.model.operator.OperatorType;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagProperty;
import java.util.ArrayList;
import java.util.List;

public class Tag {
   public String id = null;
   public TagType type;
   public boolean isNot = false;
   public boolean isRelative = false;
   public boolean isOpposite = false;
   public boolean isAbsolue = false;
   public boolean numIsZero = false;
   public boolean sizeIsZeroWithOthers = false;
   public List<TagProperty> tagPropList = new ArrayList<>();
   public ListMultimap<TagPropType, TagProperty> tagProTypeMap = ArrayListMultimap.create();
   private String n0avgSpeed = "n0avgSpeed";
   private String n1avgSpeed = "n1avgSpeed";
   private String conditionSql = "";

   public int getPropertyCount(TagPropType propType) {
      return this.tagProTypeMap.get(propType).size();
   }

   public TagProperty getNO1Property(TagPropType propType) {
      return (TagProperty)this.tagProTypeMap.get(propType).get(0);
   }

   public List<TagProperty> getTagPropList(TagPropType propType) {
      return this.tagProTypeMap.get(propType);
   }

   public TagProperty getNO2Property(TagPropType propType) {
      return (TagProperty)this.tagProTypeMap.get(propType).get(1);
   }

   public boolean contain(TagPropType propType) {
      return this.tagProTypeMap.containsKey(propType);
   }

   public boolean containNumIsZero() {
      if (this.isNot && this.numIsZero) {
         return false;
      } else {
         return this.isNot && !this.numIsZero ? true : !this.isNot && this.numIsZero;
      }
   }

   public void containAbsolueAndSizeIsZero() {
      if (this.tagPropList.size() > 1 && this.sizeEqualZero()) {
         this.sizeIsZeroWithOthers = true;
      }
   }

   public boolean isSizeZero() {
      return this.contain(TagPropType.Size) && this.getNO1Property(TagPropType.Size).intValue == 0;
   }

   public boolean sizeEqualZero() {
      return this.contain(TagPropType.Size)
         && this.getNO1Property(TagPropType.Size).operator.type == OperatorType.Equal
         && this.getNO1Property(TagPropType.Size).intValue == 0;
   }

   public boolean compareNumOfOccurs(int num) {
      if (!this.contain(TagPropType.NumOfOccurrences)) {
         return true;
      } else {
         for (TagProperty prop : this.tagProTypeMap.get(TagPropType.NumOfOccurrences)) {
            boolean isAccord = prop.compareInt(num);
            if (!isAccord) {
               return false;
            }
         }

         return true;
      }
   }

   public boolean compareNumOfSizeIsZero(int num) {
      if (this.contain(TagPropType.NumOfOccurrences)) {
         TagProperty prop = this.getNO1Property(TagPropType.NumOfOccurrences);
         boolean isAccord = prop.compareInt(num);
         if (!isAccord) {
            return false;
         }
      } else if (!this.contain(TagPropType.NumOfOccurrences) && num > 0) {
         return false;
      }

      return true;
   }

   public boolean isOnlyNumIsZero() {
      return this.tagPropList.size() == 1 && this.sizeEqualZero();
   }

   public boolean compareOccurNotRelative(int num) {
      return !this.isRelative ? this.compareNumOfOccurs(num) : true;
   }

   public void ToSqlFormat() {
      if (this.tagProTypeMap.containsKey(TagPropType.AvgSpeed) && this.tagProTypeMap.containsKey(TagPropType.Channel)) {
         String channel = this.n0avgSpeed;
         if (((TagProperty)this.tagProTypeMap.get(TagPropType.Channel).get(0)).strValue.equals("1")) {
            channel = this.n1avgSpeed;
         }

         List<TagProperty> tagProperties = this.tagProTypeMap.get(TagPropType.AvgSpeed);

         for (int i = 0; i < tagProperties.size(); i++) {
            TagProperty tagProperty = tagProperties.get(i);
            if (i > 0) {
               this.conditionSql = this.conditionSql + " and ";
            }

            this.conditionSql = this.conditionSql + channel;
            String relationship = "";
            switch (tagProperty.operator.type) {
               case GreaterThan:
                  relationship = " > ";
                  break;
               case LessThan:
                  relationship = " < ";
                  break;
               case Equal:
                  relationship = " = ";
                  break;
               case GTOrEqual:
                  relationship = " >= ";
                  break;
               case LTOrEqual:
                  relationship = " <= ";
                  break;
               case NotEqual:
                  relationship = " != ";
            }

            this.conditionSql = this.conditionSql + relationship + (int)(tagProperty.doubleValue * 1000.0);
         }
      }
   }

   public String getConditionSql() {
      return this.conditionSql;
   }
}
