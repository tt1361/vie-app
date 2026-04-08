package com.iflytek.vie.app.provider.engine;

import com.iflytek.vie.app.pojo.datadrill.DataDetailRequest;
import com.iflytek.vie.app.pojo.datadrill.DetailFilter;
import com.iflytek.vie.app.pojo.datadrill.FilterField;
import com.iflytek.vie.app.pojo.datadrill.FilterRuleEnum;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.permission.DataResourceAuth;
import com.iflytek.vie.app.pojo.permission.DimensionAuth;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VoiceDetail {
   private HashMap<String, DimensionConfig> dimensionConfigMap = new HashMap<>();
   private DataDetailRequest dataDetailRequest;
   private String tableName;
   private String timeFiled = "time";
   private String timeIndexFiled = "timestamp";
   private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   private static List<String> timeDimension = new ArrayList<>();
   private static List<String> systemStrDimension = new ArrayList<>();
   private static List<String> systemNumDimension = new ArrayList<>();
   private static String PID = "id";
   public static String Model = "offLineTagId";
   private static String ModelInfo = "offLineTagInfo";
   private static String IllegalCall = "illegalCall";
   private static String ModelCategory = "modelCategoryId";
   private static String VoiceUri = "voiceUri";
   private static String DataType = "dataType";
   private static String MachineId = "machineId";
   private static String Content = "content";
   private static String ContentOrigin = "contentOrigin";
   private boolean isFormat = false;
   private DataResourceAuth dataResourceAuth;

   public VoiceDetail() {
   }

   public VoiceDetail(
      String tableName, DataDetailRequest dataDetailRequest, HashMap<String, DimensionConfig> dimensionConfigMap, DataResourceAuth dataResourceAuth
   ) {
      this.tableName = tableName;
      this.dataDetailRequest = dataDetailRequest;
      this.dimensionConfigMap = dimensionConfigMap;
      this.dataResourceAuth = dataResourceAuth;
   }

   public String getSql(String userId) throws Exception {
      String result = "";
      StringBuffer errorMsg = new StringBuffer();
      if (this.checkParams(errorMsg)) {
         return this.dealAnyDrillSql(userId);
      } else {
         throw new Exception(errorMsg.toString());
      }
   }

   private String dealAnyDrillSql(String userId) throws Exception {
      String sql = "";
      String selCol = "";
      List<String> colList = this.dataDetailRequest.getColumns();
      boolean isModel = false;
      boolean isModelInfo = false;

      for (String col : colList) {
         isModel = isModel || this.isModel(col);
         isModelInfo = isModelInfo || this.isModelInfo(col);
         selCol = selCol + "," + col;
      }

      if (isModelInfo && !isModel) {
         selCol = selCol + "," + Model;
      }

      this.isFormat = isModelInfo;
      selCol = selCol.substring(1);
      String cond = this.dealCondition(this.dataDetailRequest.getFilter(), userId);
      String page = this.getPageSql();
      String order = this.getOrderSql();
      cond = cond.length() > 1 ? cond + " and " + page : page;
      sql = "select " + selCol + " from " + this.tableName;
      sql = cond.length() > 1 ? sql + " where " + cond : sql;
      return sql + order;
   }

   public boolean isLoneBatch(int ifLone) {
      return 1 == ifLone;
   }

   public boolean isAllBatch(int ifLone) {
      return 1 != ifLone;
   }

   private boolean checkDataType(int dataType) {
      return 0 == dataType || 1 == dataType || 2 == dataType || 3 == dataType || 4 == dataType || 5 == dataType || 6 == dataType;
   }

   private String dealCondition(DetailFilter detailFilter, String userId) throws Exception {
      String cond = "";
      DetailFilter filter = this.dataDetailRequest.getFilter();
      if (filter != null) {
         String exp = filter.getExpression();
         if (exp != null) {
            Map<String, FilterField> condMap = filter.getFilterFields();
            LinkedHashMap<String, FilterField> reverseMap = this.getReverseMap(condMap);
            if (!StringUtils.isNullOrEmpry(exp) && condMap.size() > 0) {
               for (String cc : reverseMap.keySet()) {
                  if (!this.checkAlias(cc)) {
                     throw new Exception("别名 " + cc + " 不合法");
                  }

                  String strValue = this.dealFilterField(condMap.get(cc)).replace("$", "\\$");
                  exp = exp.replaceAll(cc, " " + strValue + " ");
               }
            }

            cond = exp.replaceAll("&&&", "fulltextand");
            cond = cond.replaceAll("&", " and ").replaceAll("\\|", " or ");
            cond = cond.replaceAll("fulltextand", "&&");
         }
      }

      String dateResource = this.dealDataResource(userId);
      cond = "".equals(cond) ? dateResource : cond + " and " + dateResource;
      if ("".equals(cond)) {
         cond = " processed=0 ";
      } else if (cond.trim().endsWith("and")) {
         cond = cond + " processed=0";
      } else {
         cond = cond + " and processed=0";
      }

      return cond;
   }

   private LinkedHashMap<String, FilterField> getReverseMap(Map<String, FilterField> oldMap) {
      LinkedHashMap<String, FilterField> reverseMap = new LinkedHashMap<>();
      List<String> keyList = new ArrayList<>();
      Iterator<String> it = oldMap.keySet().iterator();

      while (it.hasNext()) {
         keyList.add(it.next());
      }

      Collections.sort(keyList);
      Collections.reverse(keyList);

      for (String key : keyList) {
         reverseMap.put(key, oldMap.get(key));
      }

      return reverseMap;
   }

   private String dealDataResource(String userId) throws Exception {
      String result = "";

      for (DimensionAuth cd : this.dataResourceAuth.getDataDimension()) {
         String col = cd.getEnglishName();
         boolean isTime = this.isTimeDimension(col);
         if (this.dimensionConfigMap.get(col) == null && !isTime) {
            throw new Exception("查询字段 " + col + " 不存在");
         }

         List<String> dtValue = cd.getValue();
         String dmValue = "";

         for (String cv : dtValue) {
            DimensionConfig dimensionConfig = this.dimensionConfigMap.get(col);
            String fieldType = FilterFieldSwitch.filterSwitch(col, dimensionConfig, isTime, false);
            dmValue = dmValue + "," + this.getValue(fieldType, cv);
         }

         if (dmValue.length() > 1) {
            result = result + col + " in(" + dmValue.substring(1) + ") and ";
         }
      }

      if (result.length() > 1) {
         result = result.substring(0, result.length() - 4);
      } else if (!userId.equals("-1")) {
         result = "processed=1";
      }

      return result;
   }

   private String dealFilterField(FilterField filterField) throws Exception {
      String result = "";
      String filterRule = filterField.getFilterRule();
      String fieldName = filterField.getFieldName();
      Object[] fieldValue = filterField.getParams();
      boolean isTimeD = this.isTimeDimension(fieldName);
      boolean isStrSys = this.isStrSystemDim(fieldName);
      boolean isNumSys = this.isNumSystemDim(fieldName);
      boolean isModel = this.isModel(fieldName);
      DimensionConfig dimensionConfig = this.dimensionConfigMap.get(fieldName);
      String fieldType = "";
      if (this.isTime(fieldName)) {
         fieldName = this.timeIndexFiled;
         fieldType = FieldType.time.toString();
      } else if (isStrSys | isTimeD) {
         fieldType = FieldType.string.toString();
      } else if (isNumSys) {
         fieldType = FieldType.numerical.toString();
      } else if (isModel) {
         if (Model.equals(fieldName)) {
            this.tableName = this.tableName + "model";
         }

         fieldType = FieldType.numerical.toString();
      } else {
         if (dimensionConfig == null) {
            throw new Exception("can not find  dimension " + fieldName);
         }

         fieldType = FilterFieldSwitch.filterSwitch(fieldName, dimensionConfig, isTimeD, isStrSys || isNumSys);
      }

      switch (FilterRuleEnum.ValueOf(filterRule)) {
         case GREAT_THAN:
            if (fieldValue.length != 1) {
               throw new Exception(fieldName + "条件值格式不正确");
            }

            result = fieldName + " > " + this.getValue(fieldType, fieldValue[0], true);
            break;
         case LITTLE_THAN:
            if (fieldValue.length != 1) {
               throw new Exception(fieldName + "条件值格式不正确");
            }

            result = fieldName + " < " + this.getValue(fieldType, fieldValue[0], false);
            break;
         case IN:
            if (fieldValue.length == 1) {
               result = fieldName + " = " + this.getValue(fieldType, fieldValue[0]);
            } else {
               if (fieldValue.length <= 1) {
                  throw new Exception(fieldName + "条件值格式不正确");
               }

               String cv = "";

               for (Object obj : fieldValue) {
                  cv = cv + "," + this.getValue(fieldType, obj);
               }

               if (cv.length() <= 1) {
                  throw new Exception(fieldName + "条件值格式不正确");
               }

               result = fieldName + " in (" + cv.substring(1) + ")";
            }
            break;
         case EQUAL:
            if (fieldValue.length == 1) {
               result = fieldName + " = " + this.getValue(fieldType, fieldValue[0]);
            } else {
               if (fieldValue.length <= 1) {
                  throw new Exception(fieldName + "条件值格式不正确");
               }

               String cv = "";

               for (Object obj : fieldValue) {
                  cv = cv + "," + this.getValue(fieldType, obj);
               }

               if (cv.length() <= 1) {
                  throw new Exception(fieldName + "条件值格式不正确");
               }

               result = fieldName + " in (" + cv.substring(1) + ")";
            }
            break;
         case NOT_IN:
            if (fieldValue.length == 1) {
               result = fieldName + " !=  " + this.getValue(fieldType, fieldValue[0]);
            } else {
               if (fieldValue.length <= 1) {
                  throw new Exception(fieldName + "条件值格式不正确");
               }

               String cv = "";

               for (Object obj : fieldValue) {
                  cv = cv + "," + this.getValue(fieldType, obj);
               }

               if (cv.length() <= 1) {
                  throw new Exception(fieldName + "条件值格式不正确");
               }

               result = fieldName + " not in (" + cv.substring(1) + ")";
            }
            break;
         case NOT_EQUAL:
            if (fieldValue.length == 1) {
               result = fieldName + " !=  " + this.getValue(fieldType, fieldValue[0]);
            } else {
               if (fieldValue.length <= 1) {
                  throw new Exception(fieldName + "条件值格式不正确");
               }

               String cv = "";

               for (Object obj : fieldValue) {
                  cv = cv + "," + this.getValue(fieldType, obj);
               }

               if (cv.length() <= 1) {
                  throw new Exception(fieldName + "条件值格式不正确");
               }

               result = fieldName + " not in (" + cv.substring(1) + ")";
            }
            break;
         case GREAT_EQUAL:
            if (fieldValue.length != 1) {
               throw new Exception(fieldName + "条件值格式不正确");
            }

            result = fieldName + " >= " + this.getValue(fieldType, fieldValue[0], true);
            break;
         case LITTLE_EQUAL:
            if (fieldValue.length != 1) {
               throw new Exception(fieldName + "条件值格式不正确");
            }

            result = fieldName + " <= " + this.getValue(fieldType, fieldValue[0], false);
            break;
         case LEFT_RANGE:
            if (fieldValue.length != 2) {
               throw new Exception(fieldName + "条件值格式不正确");
            }

            result = fieldName
               + " > "
               + this.getValue(fieldType, fieldValue[0], true)
               + " and "
               + fieldName
               + " <= "
               + this.getValue(fieldType, fieldValue[1], false);
            break;
         case RIGHT_RANGE:
            if (fieldValue.length != 2) {
               throw new Exception(fieldName + "条件值格式不正确");
            }

            result = fieldName
               + " >= "
               + this.getValue(fieldType, fieldValue[0], true)
               + " and "
               + fieldName
               + " < "
               + this.getValue(fieldType, fieldValue[1], false);
            break;
         case RANGE:
            if (fieldValue.length != 2) {
               throw new Exception(fieldName + "条件值格式不正确");
            }

            result = fieldName
               + " > "
               + this.getValue(fieldType, fieldValue[0], true)
               + " and "
               + fieldName
               + " < "
               + this.getValue(fieldType, fieldValue[1], false);
            break;
         case BOTH_RANGE:
            if (fieldValue.length != 2) {
               throw new Exception(fieldName + "条件值格式不正确");
            }

            result = fieldName
               + " >= "
               + this.getValue(fieldType, fieldValue[0], true)
               + " and "
               + fieldName
               + " <= "
               + this.getValue(fieldType, fieldValue[1], false);
            break;
         case FULL_TEXT:
            String ftValue = this.dealFullTextCond(fieldValue, isModel);
            if (StringUtils.isNullOrEmpry(ftValue)) {
               throw new Exception(fieldName + "条件值格式不正确");
            }

            result = result + " fulltext('" + fieldName + ":(" + ftValue + ")')=true ";
            break;
         case NO_FULL_TEXT:
            String nftValue = this.dealFullTextCond(fieldValue, isModel);
            if (StringUtils.isNullOrEmpry(nftValue)) {
               throw new Exception(fieldName + "条件值格式不正确");
            }

            result = result + " fulltext('" + fieldName + ":(" + nftValue + ")')=false ";
      }

      return result;
   }

   private String dealFullTextCond(Object[] fieldValue, boolean isModel) {
      String result = "";
      if (isModel) {
         result = this.dealModelFulltextCond(fieldValue);
      } else {
         result = this.dealInputFullTextCond(fieldValue);
      }

      return result;
   }

   private String dealInputFullTextCond(Object[] fieldValue) {
      String result = "";

      for (Object current : fieldValue) {
         String cd = String.valueOf(current);
         result = result + "" + cd + " OR ";
      }

      return result.length() > 0 ? result.substring(0, result.length() - 4) : result;
   }

   private String dealModelFulltextCond(Object[] fieldValue) {
      String result = "";

      for (Object current : fieldValue) {
         String cd = String.valueOf(current);
         result = result + "" + cd + " AND ";
      }

      return result.length() > 0 ? result.substring(0, result.length() - 5) : result;
   }

   private boolean checkAlias(String fieldName) {
      for (String dimensionKey : this.dimensionConfigMap.keySet()) {
         if (dimensionKey.indexOf(fieldName) != -1 && fieldName.length() == 1) {
            return false;
         }
      }

      return true;
   }

   private String getValue(String fieldType, Object obj) throws Exception {
      String svalue = String.valueOf(obj);
      String result = "";
      if ("null".equals(svalue)) {
         throw new Exception("条件值不能为空");
      } else {
         switch (FieldType.valueOf(fieldType)) {
            case string:
               result = "'" + svalue + "'";
               break;
            case duration:
               result = (int)(Double.valueOf(svalue) * 1000.0) + "";
               break;
            case numerical:
               result = svalue;
               break;
            case time:
               result = this.sdf.parse(svalue).getTime() + "";
         }

         return result;
      }
   }

   private String getValue(String fieldType, Object obj, boolean durationStart) throws Exception {
      String svalue = String.valueOf(obj);
      String result = "";
      if ("null".equals(svalue)) {
         throw new Exception("条件值不能为空");
      } else {
         switch (FieldType.valueOf(fieldType)) {
            case string:
               result = "'" + svalue + "'";
               break;
            case duration:
               Double sv = Double.valueOf(svalue) * 1000.0;
               if (durationStart) {
                  sv = sv - 500.0 > 0.0 ? sv - 500.0 : 0.0;
               } else {
                  sv = sv + 499.0;
               }

               result = sv + "";
               break;
            case numerical:
               result = svalue;
               break;
            case time:
               result = this.sdf.parse(svalue).getTime() + "";
         }

         return result;
      }
   }

   private boolean checkParams(StringBuffer errorMsg) {
      return true;
   }

   private boolean checkOrderType(StringBuffer errorMsg) {
      String type = this.dataDetailRequest.getOrderType();
      if (!"asc".equals(type) && !"desc".equals(type)) {
         errorMsg.append("排序类型" + type + "不正确");
         return false;
      } else {
         return true;
      }
   }

   private String getPageSql() throws Exception {
      String result = "";
      int pageIndex = this.dataDetailRequest.getFetchFrom();
      pageIndex = pageIndex < 1 ? 1 : pageIndex;
      int pageSize = this.dataDetailRequest.getFetchSize();
      int startNum = (pageIndex - 1) * pageSize + 1;
      int endNum = pageIndex * pageSize;
      return " rownum between " + startNum + " and " + endNum;
   }

   private String getOrderSql() throws Exception {
      String result = "";
      String order = this.dataDetailRequest.getOrderField();
      String type = this.dataDetailRequest.getOrderType();
      StringBuffer errorMsg = new StringBuffer();
      if (!StringUtils.isNullOrEmpry(order) && !StringUtils.isNullOrEmpry(type)) {
         if (!this.checkParams(errorMsg) || !this.checkOrderType(errorMsg)) {
            throw new Exception(errorMsg.toString());
         }

         result = " order by " + order + " " + type;
      }

      return result;
   }

   private boolean isTimeDimension(String feild) {
      return timeDimension.contains(feild);
   }

   private boolean isTime(String filed) {
      return this.timeFiled.equals(filed);
   }

   private boolean isSystemDim(String feild) {
      return systemStrDimension.contains(feild) || systemNumDimension.contains(feild);
   }

   private boolean isNumSystemDim(String feild) {
      return systemNumDimension.contains(feild);
   }

   private boolean isStrSystemDim(String feild) {
      return systemStrDimension.contains(feild);
   }

   private boolean isModel(String feild) {
      return Model.equals(feild) || IllegalCall.equals(feild);
   }

   private boolean isModelInfo(String feild) {
      return ModelInfo.equals(feild);
   }

   private boolean isModelCateGory(String feild) {
      return ModelCategory.equals(feild);
   }

   public boolean isIsFormat() {
      return this.isFormat;
   }

   public void setIsFormat(boolean isFormat) {
   }

   public String getTableName() {
      return this.tableName;
   }

   public void setTableName(String tableName) {
      this.tableName = tableName;
   }

   static {
      timeDimension.add("year");
      timeDimension.add("quarter");
      timeDimension.add("month");
      timeDimension.add("day");
      timeDimension.add("hour");
      timeDimension.add("dimYear");
      timeDimension.add("dimMonth");
      timeDimension.add("dimDay");
      timeDimension.add("dimHour");
      timeDimension.add("weekOfYear");
      timeDimension.add("channelSeq");
      timeDimension.add("contentOrigin");
      systemStrDimension.add(ModelCategory);
      systemStrDimension.add(PID);
      systemStrDimension.add(VoiceUri);
      systemNumDimension.add(DataType);
   }
}
