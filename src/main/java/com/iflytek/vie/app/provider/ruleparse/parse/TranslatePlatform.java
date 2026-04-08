package com.iflytek.vie.app.provider.ruleparse.parse;

import com.google.common.base.Strings;
import com.iflytek.vie.app.provider.ruleparse.model.operator.OperatorType;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagPropType;
import com.iflytek.vie.app.provider.ruleparse.model.property.TagProperty;
import com.iflytek.vie.app.provider.ruleparse.model.tag.Tag;
import com.iflytek.vie.app.provider.ruleparse.model.tag.TagContainer;
import com.iflytek.vie.app.provider.ruleparse.model.tag.TagType;
import com.iflytek.vie.utils.StringUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TranslatePlatform {
   private String objecntRule;
   private String textColumn;
   private String quaCondition;
   private String textFilterCondition;

   public String translate(String objectRule) {
      StringBuffer sql = new StringBuffer();
      StringBuffer columns = new StringBuffer();
      StringBuffer objFilter = new StringBuffer();
      StringBuffer textFilter = new StringBuffer();
      String id = "id";
      String content = "content";
      String contentN0 = "contentN0";
      String contentN1 = "contentN1";
      String timePosition = "timePosition";
      String silences = "silences";
      String duration = "duration";
      boolean hasDuration = false;
      boolean needTime = false;
      String n0Speed = "n0Speeds";
      String n1Speed = "n1Speeds";
      String n0Volume = "n0Energys";
      String n1Volume = "n1Energys";
      boolean hasN0Speeds = false;
      boolean hasN0Volumes = false;
      boolean hasN1Speeds = false;
      boolean hasN1Volumes = false;
      columns.append(id);
      columns.append(",");
      String textRule = " ";
      TagContainer tagContainer = (new TagRuleParse(objectRule)).tagContainer;

      for (TagType type : tagContainer.tagTypeMap.keySet()) {
         switch (type) {
            case Keyword:
               Set<Tag> tags = tagContainer.tagTypeMap.get(TagType.Keyword);
               Set<String> channels = new HashSet<>();
               if (!Strings.isNullOrEmpty(textRule)) {
                  channels.add(content);
               }

               for (Tag tag : tags) {
                  if (tag.isRelative || tag.isOpposite || tag.isAbsolue) {
                     needTime = true;
                     hasDuration = true;
                  }

                  String contentField;
                  if (tag.contain(TagPropType.Channel)) {
                     contentField = tag.getNO1Property(TagPropType.Channel).strValue;
                     if (contentField.equalsIgnoreCase("0")) {
                        contentField = contentN0;
                     } else {
                        contentField = contentN1;
                     }
                  } else {
                     contentField = content;
                  }

                  if (!channels.contains(contentField)) {
                     columns.append(contentField);
                     columns.append(",");
                     channels.add(contentField);
                  }

                  String filter = new KeywordParse(tag).parse(contentField);
                  if (!Strings.isNullOrEmpty(filter)) {
                     boolean isNoTimes = false;
                     Map map = new HashMap();
                     if (null != tag && "OBJ001".equals(tag.type.code) && null != tag.tagPropList) {
                        for (int tt = 0; tt < tag.tagPropList.size(); tt++) {
                           TagProperty tp = tag.tagPropList.get(tt);
                           map.put(tp.type.name(), tp.intValue);
                        }
                     }

                     if (map.size() > 0
                        && null != map.get("List")
                        && null != map.get("NumOfOccurrences")
                        && 0 == (Integer)map.get("NumOfOccurrences")
                        && (null != map.get("AbsolutePosition") || null != map.get("RelativePosition"))) {
                        isNoTimes = true;
                     }

                     if (!isNoTimes) {
                        if (textFilter.length() > 0) {
                           textFilter.append(" and ");
                        }

                        textFilter.append(" fulltext('" + filter + "','content-query-parser')=true ");
                     }
                  }
               }
               break;
            case Silence:
               for (Tag tagx : tagContainer.tagTypeMap.get(TagType.Silence)) {
                  if (tagx.isAbsolue) {
                     needTime = true;
                     hasDuration = true;
                  }

                  String filter = (new SilenceParse(tagx)).querySyntax;
                  if (tagx.contain(TagPropType.AbsolutePosition) && tagx.getNO1Property(TagPropType.AbsolutePosition).operator.type == OperatorType.EndSpeech) {
                     hasDuration = true;
                  }

                  if (!Strings.isNullOrEmpty(filter)) {
                     objFilter.append(filter);
                     objFilter.append(" AND ");
                  }
               }

               columns.append(silences);
               columns.append(",");
               break;
            case Speed:
               for (Tag tag : tagContainer.tagTypeMap.get(TagType.Speed)) {
                  if (tag.contain(TagPropType.SingleSpeed)) {
                     if (tag.contain(TagPropType.Channel)) {
                        if (tag.getNO1Property(TagPropType.Channel).strValue.equals("1")) {
                           hasN1Speeds = true;
                        } else {
                           hasN0Speeds = true;
                        }
                     } else {
                        hasN0Speeds = true;
                     }

                     String filter = (new SpeedAndVolumeParse(tag)).querySyntax;
                     if (!Strings.isNullOrEmpty(filter)) {
                        objFilter.append(filter);
                        objFilter.append(" AND ");
                     }
                  } else if (tag.contain(TagPropType.AvgSpeed)) {
                     tag.ToSqlFormat();
                     if (!StringUtils.isNullOrEmpry(tag.getConditionSql())) {
                        objFilter.append(tag.getConditionSql()).append(" AND ");
                     }
                  }
               }

               if (hasN0Speeds) {
                  columns.append(n0Speed);
               } else if (hasN1Speeds) {
                  columns.append(n1Speed);
               } else {
                  columns.append(n0Speed);
               }

               columns.append(",");
               break;
            case Volume:
               for (Tag tagx : tagContainer.tagTypeMap.get(TagType.Volume)) {
                  if (tagx.isAbsolue) {
                     needTime = true;
                  }

                  if (tagx.contain(TagPropType.AbsolutePosition) && tagx.getNO1Property(TagPropType.AbsolutePosition).operator.type == OperatorType.EndSpeech) {
                     hasDuration = true;
                  }

                  if (tagx.contain(TagPropType.Channel)) {
                     if (tagx.getNO1Property(TagPropType.Channel).strValue.equals("1")) {
                        hasN1Volumes = true;
                     } else {
                        hasN0Volumes = true;
                     }
                  } else {
                     hasN0Volumes = true;
                  }

                  String filterx = (new SpeedAndVolumeParse(tagx)).querySyntax;
                  if (!Strings.isNullOrEmpty(filterx)) {
                     objFilter.append(filterx);
                     objFilter.append(" AND ");
                  }
               }

               if (hasN0Volumes) {
                  columns.append(n0Volume);
               } else if (hasN1Volumes) {
                  columns.append(n1Volume);
               } else {
                  columns.append(n0Volume);
               }

               columns.append(",");
         }
      }

      if (hasDuration) {
         columns.append(duration);
         columns.append(",");
      }

      if (needTime) {
         columns.append(timePosition);
         columns.append(",");
      }

      if (columns.length() > 0) {
         columns.deleteCharAt(columns.length() - 1);
      }

      this.textColumn = columns.toString();
      if (objFilter.length() > 0) {
         objFilter.delete(objFilter.length() - 5, objFilter.length());
         this.quaCondition = "(" + objFilter + ")";
      }

      this.objecntRule = objectRule.replace("'", "\"");
      StringBuffer where = new StringBuffer();
      if (textFilter.length() > 0) {
         where.append(textFilter);
      }

      this.textFilterCondition = where.toString();
      return sql.toString();
   }

   public String getObjecntRule() {
      return this.objecntRule;
   }

   public void setObjecntRule(String objecntRule) {
      this.objecntRule = objecntRule;
   }

   public String getTextColumn() {
      return this.textColumn;
   }

   public void setTextColumn(String textColumn) {
      this.textColumn = textColumn;
   }

   public String getQuaCondition() {
      return this.quaCondition;
   }

   public void setQuaCondition(String quaCondition) {
      this.quaCondition = quaCondition;
   }

   public String getTextFilterCondition() {
      return this.textFilterCondition;
   }

   public void setTextFilterCondition(String textFilterCondition) {
      this.textFilterCondition = textFilterCondition;
   }

   @Override
   public String toString() {
      return "objecntRule["
         + this.objecntRule
         + "],textColumn["
         + this.textColumn
         + "],quaCondition["
         + this.quaCondition
         + "],textFilterCondition["
         + this.textFilterCondition
         + "]";
   }
}
