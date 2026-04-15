package com.iflytek.vie.app.provider.common;

import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.provider.pojo.CommonFun;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.utils.ExceptionUtil;
import com.iflytek.vie.utils.StringUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkSqlParse {
   private final Logger logger = LoggerFactory.getLogger(SparkSqlParse.class);
   public DecimalFormat decimalFormat = new DecimalFormat("###0.0");
   public LinkedHashMap<String, String> rangeShowMap = new LinkedHashMap<>();
   public List<String> showColOrder = new ArrayList<>();
   public List<String> showColText = new ArrayList<>();
   public Set<String> selColOrder = new HashSet<>();
   public List<String> specialCol = new ArrayList<>();
   public List<String> rangeList = new ArrayList<>();
   public String erroeMsg = "";
   private String tableName;
   private String order;
   private String orderType;
   private List<HashMap<String, String>> measureList;
   private String startTime;
   private String endTime;
   private HashMap<String, DimensionConfig> dimensionMap;
   private LinkedHashMap<String, String> rangeMap = new LinkedHashMap<>();
   private HashMap<String, String> measureMap = new HashMap<>();
   private List<HashMap<String, Object>> reportConD = new ArrayList<>();
   private Map<String, String> defaultDurationMap = new HashMap<>();

   public SparkSqlParse() {
   }

   public SparkSqlParse(
      List<HashMap<String, String>> measureList,
      HashMap<String, DimensionConfig> dimensionMap,
      List<HashMap<String, Object>> reportConD,
      String tableName,
      String startTime,
      String endTime,
      List<String> modelList,
      int pageNum,
      int pageSize,
      String order,
      String orderType,
      LinkedHashMap<String, String> rangeMap,
      Map<String, String> durationMap
   ) {
      this.measureList = measureList;
      this.tableName = tableName;
      this.order = order;
      this.orderType = orderType;
      this.startTime = startTime;
      this.endTime = endTime;
      this.dimensionMap = dimensionMap;
      this.reportConD = reportConD;
      this.rangeMap = rangeMap;
      this.defaultDurationMap = durationMap;
   }

   public String dealStatsTable(HashMap<String, Object> params, boolean isDetail, String modelConIn, String modelConNotIn) {
      String sql = "";

      try {
         StringBuffer selColBuffer = new StringBuffer();
         new StringBuffer();
         StringBuffer groupColBuffer = new StringBuffer();
         String condition = "";
         List<HashMap<String, String>> talColList = (List<HashMap<String, String>>)params.get(CommonParams.tabCol);
         boolean isRange = false;
         boolean isKwd = false;

         for (HashMap<String, String> talCol : (List<HashMap<String, String>>)talColList) {
            String colType = talCol.get(CommonParams.tabColType);
            String colFiled = talCol.get(CommonParams.tabColFiled);
            String expression = talCol.get(CommonParams.tabColExp);
            String showText = talCol.get(CommonParams.tabColText);
            String showType = talCol.get(CommonParams.tabColShowType);
            colType = isDetail ? TableColType.detail.toString() : colType;
            boolean ctRange = this.dealMeasure(colFiled, colType, expression, showType, selColBuffer, groupColBuffer, isDetail);
            this.getMeasureField(colFiled, colType, expression, showType, selColBuffer, groupColBuffer, isDetail);
            this.showColOrder.add(colFiled);
            this.showColText.add(showText);
            isRange = isRange || ctRange;
            if (CommonParams.keyword.equals(colFiled)) {
               isKwd = true;
            }
         }

         StringBuffer subSelBuffer = new StringBuffer("");

         for (String showColString : this.selColOrder) {
            subSelBuffer.append(showColString + ",");
         }

         if (this.rangeList.size() > 0 && !isDetail) {
            String temp = "";

            for (String str : this.rangeList) {
               str = str.substring(str.indexOf("(") + 1, str.lastIndexOf(")"));
               String[] strings = str.split("'");
               String field = strings[0].replace(",", "");
               String fieldRange = strings[1];
               String[] ranges = fieldRange.split(",");
               temp = temp + " case ";

               for (String range : ranges) {
                  String[] values = range.split("_");
                  temp = temp + " when " + field + " >= " + values[0] + " and " + field + " < " + values[1] + " then '" + values[2] + "' ";
               }

               temp = temp + " end " + this.rangeShowMap.get(field) + ",";
            }

            if (!StringUtils.isNullOrEmpry(temp)) {
               for (String rangeShowColStr : this.rangeShowMap.keySet()) {
                  groupColBuffer.append(this.rangeShowMap.get(rangeShowColStr) + ",");
                  selColBuffer.append(this.rangeShowMap.get(rangeShowColStr) + ",");
               }

               subSelBuffer.append(temp);
            }
         }

         if (this.specialCol.size() > 0) {
            for (String str : this.specialCol) {
               selColBuffer.append(str + ",");
               groupColBuffer.append(str + ",");
            }
         }

         if (isKwd) {
            selColBuffer.append(CommonParams.modelFieldInfo + "," + CommonParams.modelFiemld + ",");
         }

         sql = this.dealTableSql(condition, isRange, selColBuffer, subSelBuffer, groupColBuffer, isDetail, modelConIn, modelConNotIn);
      } catch (Exception var25) {
         this.logger.error(ExceptionUtil.getStackTrace(var25));
      }

      return sql;
   }

   private String dealTableSql(
      String condition,
      boolean isRange,
      StringBuffer selColBuffer,
      StringBuffer subSelBuffer,
      StringBuffer groupColBuffer,
      boolean isDetail,
      String modelConIn,
      String modelConNotIn
   ) {
      StringBuffer sql = new StringBuffer("");

      try {
         String selCol = "";
         String subSel = "";
         String groupCol = "";
         String having = "";
         String rownumStr = "";
         if (!StringUtils.isNullOrEmpry(this.order) && !StringUtils.isNullOrEmpry(this.orderType)) {
            String trueOrder = this.rangeShowMap.get(this.order) == null ? this.order : this.rangeShowMap.get(this.order).toString();
            rownumStr = rownumStr + "row_number() over ( order by " + trueOrder + " " + this.orderType + ") as rownum";
            if (trueOrder.equals(CommonParams.keyword)) {
               rownumStr = "";
            }
         }

         selCol = StringUtils.isNullOrEmpry(selColBuffer.toString()) ? "" : selColBuffer.substring(0, selColBuffer.length() - 1);
         subSel = StringUtils.isNullOrEmpry(subSelBuffer.toString()) ? "" : subSelBuffer.substring(0, subSelBuffer.length() - 1);
         groupCol = StringUtils.isNullOrEmpry(groupColBuffer.toString()) ? "" : groupColBuffer.substring(0, groupColBuffer.length() - 1);
         condition = StringUtils.isNullOrEmpry(condition) ? "" : condition.substring(0, condition.length() - 5);
         if (StringUtils.isNullOrEmpry(having)) {
            String var10000 = "";
         } else {
            having.substring(0, having.length() - 5);
         }

         if (!selCol.contains("id") && isDetail) {
            selCol = selCol + "," + "id";
         }

         sql.append("select *," + rownumStr + " from ( ");
         sql.append("select " + selCol + " from ");
         if (!subSel.matches(".*[, ]id[, ].*")) {
            subSel = "id," + subSel;
         }

         sql.append(" (select " + subSel + " from ");
         if (!StringUtils.isNotNullAndEmpry(modelConNotIn) && !StringUtils.isNotNullAndEmpry(modelConIn)) {
            sql.append(this.tableName);
         } else {
            String tempTable = "";
            if (StringUtils.isNotNullAndEmpry(modelConNotIn) && StringUtils.isNotNullAndEmpry(modelConIn)) {
               tempTable = " (select temp.* from "
                  + this.tableName
                  + " temp "
                  + "left join ("
                  + modelConNotIn
                  + ")notTable on temp."
                  + "id"
                  + " = notTable."
                  + "id"
                  + " where notTable."
                  + "id"
                  + " is null ) ";
               tempTable = " (select temp.* from "
                  + tempTable
                  + " temp "
                  + "left join ("
                  + modelConIn
                  + ")inTable on temp."
                  + "id"
                  + " = inTable."
                  + "id"
                  + " where inTable."
                  + "id"
                  + " is not null ) temp ";
            } else if (StringUtils.isNotNullAndEmpry(modelConNotIn)) {
               tempTable = " (select temp.* from "
                  + this.tableName
                  + "main"
                  + " temp "
                  + "left join ("
                  + modelConNotIn
                  + ")notTable on temp."
                  + "id"
                  + " = notTable."
                  + "id"
                  + " where notTable."
                  + "id"
                  + " is null ) temp ";
            } else {
               tempTable = " (select temp.* from "
                  + this.tableName
                  + " temp "
                  + "left join ("
                  + modelConIn
                  + ")inTable on temp."
                  + "id"
                  + " = inTable."
                  + "id"
                  + " where inTable."
                  + "id"
                  + " is not null ) temp ";
            }

            sql.append(tempTable);
         }

         sql.append(StringUtils.isNullOrEmpry(condition) ? "" : " where " + condition);
         String subGrouoCol = "";
         if (!subSel.contains("case  when")) {
            subGrouoCol = subSel;
         } else {
            String[] subSelList = subSel.split(",");

            for (String str : subSelList) {
               if (!str.trim().startsWith("case  when")) {
                  subGrouoCol = subGrouoCol + str + ",";
               }
            }

            subGrouoCol = StringUtils.isNullOrEmpry(subGrouoCol) ? "" : subGrouoCol.substring(0, subGrouoCol.length() - 1);
         }

         if (!subGrouoCol.contains(CommonParams.modelFiemld)) {
            sql.append("  group by " + subGrouoCol);
         }

         sql.append(" ) a ");
         sql.append(StringUtils.isNullOrEmpry(groupCol) ? "" : " group by " + groupCol);
         sql.append(" ) b ");
      } catch (Exception var21) {
         this.logger.error(ExceptionUtil.getStackTrace(var21));
      }

      return sql.toString();
   }

   private boolean dealMeasure(
      String colFiled, String colType, String expression, String showType, StringBuffer selCol, StringBuffer groupCol, boolean isDetail
   ) {
      boolean isRange = false;

      try {
         switch (TableColType.valueOf(colType)) {
            case measure:
               selCol.append(this.getMeasure(colFiled));
               break;
            case dimension:
               if (this.isDurationDimsion(colFiled)) {
                  String cd = this.getRangeStr(colFiled, isDetail);
                  if (colFiled.equals(cd)) {
                     selCol.append(this.dealDimension(cd) + ",");
                     groupCol.append(cd + ",");
                  } else {
                     this.rangeList.add(cd);
                     isRange = true;
                  }
               } else if (!this.isModel(colFiled)) {
                  selCol.append(colFiled + ",");
                  groupCol.append(colFiled + ",");
               } else {
                  this.specialCol.add(colFiled);
               }
               break;
            case computer:
               selCol.append(this.getComputer(expression, colFiled, showType));
               break;
            case detail:
               String detailFiled = this.dealDetailDimension(colFiled);
               if (!StringUtils.isNullOrEmpry(detailFiled)) {
                  selCol.append(this.dealDetailDimension(colFiled) + ",");
               }
               break;
            default:
               this.logger.error(colType + " formar error!");
         }
      } catch (Exception var10) {
         this.logger.error(ExceptionUtil.getStackTrace(var10));
      }

      return isRange;
   }

   private void getMeasureField(
      String colFiled, String colType, String expression, String showType, StringBuffer selCol, StringBuffer groupCol, boolean isDetail
   ) {
      try {
         switch (TableColType.valueOf(colType)) {
            case measure:
               String[] col = colFiled.split("_");
               if (col.length == 2) {
                  this.selColOrder.add(col[0]);
               }
               break;
            case dimension:
               this.selColOrder.add(colFiled);
               break;
            case computer:
               this.checkExpression(expression);

               for (HashMap<String, String> current : this.measureList) {
                  String key = current.get(CommonParams.expressionParam);
                  String measure = current.get(CommonParams.measure);
                  if (expression.contains(key)) {
                     String[] ex = measure.split("_");
                     if (ex.length == 2) {
                        this.selColOrder.add(ex[0]);
                     }
                  }
               }
               break;
            case detail:
               this.selColOrder.add(colFiled);
               break;
            default:
               this.logger.error(colType + " formar error!");
         }
      } catch (Exception var13) {
         this.logger.error(ExceptionUtil.getStackTrace(var13));
      }
   }

   private String getMeasure(String colFiled) {
      String selCol = "";

      try {
         selCol = this.dealMeasure(colFiled);
         if (!StringUtils.isNullOrEmpry(selCol)) {
            selCol = selCol + " as " + colFiled + ",";
         } else {
            this.logger.error(colFiled + " formar error!");
         }
      } catch (Exception var4) {
         this.logger.error(colFiled + " formar error!");
      }

      return selCol;
   }

   private String getRangeStr(String colFiled, boolean isDetail) {
      String selCol = "";

      try {
         String rangeValue = this.rangeMap.get(colFiled);
         if (!StringUtils.isNullOrEmpry(rangeValue)) {
            String showName = colFiled + "range";
            selCol = selCol + "range(" + colFiled + ",'" + rangeValue + "',sum(" + colFiled + ")) as " + showName;
            this.rangeShowMap.put(colFiled, showName);
         } else if (StringUtils.isNullOrEmpry(rangeValue) && !isDetail) {
            String showName = colFiled + "range";
            rangeValue = this.getDefaultTimeRange(colFiled);
            selCol = selCol + "range(" + colFiled + ",'" + rangeValue + "',sum(" + colFiled + ")) as " + showName;
            this.rangeShowMap.put(colFiled, showName);
         } else {
            selCol = colFiled;
            this.logger.error(colFiled + " RangeData error!");
         }
      } catch (Exception var6) {
         this.logger.error(colFiled + " formar error!");
      }

      return selCol;
   }

   private String getDefaultTimeRange(String colFiled) {
      String rangeValue = "";
      if (this.defaultDurationMap.keySet().contains(colFiled)) {
         String[] default_rang = this.defaultDurationMap.get(colFiled).split(",");

         for (String currentRange : default_rang) {
            String[] params = currentRange.split("\\|");
            if (params.length == 2) {
               double start = CommonFun.isNumber(params[0]) ? Double.parseDouble(params[0]) : Integer.parseInt(params[0]);
               double end = CommonFun.isNumber(params[1]) ? Double.parseDouble(params[1]) : Integer.parseInt(params[1]);
               String oldS = CommonFun.isNumber(params[0]) ? start + "" : (int)start + "";
               String oldE = CommonFun.isNumber(params[1]) ? end + "" : (int)end + "";
               if (!colFiled.equals("n0avgSpeed") && !colFiled.equals("n1avgSpeed")) {
                  start = start * 1000.0 - 500.0 > 0.0 ? start * 1000.0 - 500.0 : start * 1000.0;
                  end = end * 1000.0 + 499.0;
               } else {
                  start *= 1000.0;
                  end *= 1000.0;
               }

               rangeValue = rangeValue + this.decimalFormat.format(start) + "_" + this.decimalFormat.format(end) + "_" + oldS + "~" + oldE + ",";
            } else if (params.length == 1) {
               double start = CommonFun.isNumber(params[0]) ? Double.parseDouble(params[0]) : Integer.parseInt(params[0]);
               String oldS = CommonFun.isNumber(params[0]) ? start + "" : (int)start + "";
               double end = 2.147483647E9;
               if (!colFiled.equals("n0avgSpeed") && !colFiled.equals("n1avgSpeed")) {
                  start = start * 1000.0 - 500.0 > 0.0 ? start * 1000.0 - 500.0 : start * 1000.0;
                  end = end * 1000.0 + 499.0;
               } else {
                  start *= 1000.0;
                  end *= 1000.0;
               }

               String oldE = "";
               rangeValue = rangeValue + this.decimalFormat.format(start) + "_" + this.decimalFormat.format(end) + "_" + oldS + "~" + oldE + ",";
            }
         }
      }

      return !"".equals(rangeValue) ? rangeValue.substring(0, rangeValue.length() - 1) : rangeValue;
   }

   private String dealMeasure(String colFiled) {
      String selCol = "";

      try {
         String[] col = colFiled.split("_");
         if (col.length == 2) {
            String field = col[0];
            DimensionConfig dimensionConfig = this.dimensionMap.get(field);
            if (dimensionConfig != null && "1".equals(String.valueOf(dimensionConfig.getFlag()))) {
               selCol = selCol + "cast(" + col[1] + "(" + col[0] + ")/1000.0 as BIGINT)";
               this.selColOrder.add(col[0]);
            } else {
               selCol = selCol + col[1] + "(" + col[0] + ")";
            }
         } else {
            this.logger.error(colFiled + " formar error!");
         }
      } catch (Exception var6) {
         this.logger.error(ExceptionUtil.getStackTrace(var6));
      }

      return selCol;
   }

   private String dealDimension(String colFiled) {
      String selCol = "";

      try {
         DimensionConfig dimensionConfig = this.dimensionMap.get(colFiled);
         if (dimensionConfig != null && "1".equals(String.valueOf(dimensionConfig.getFlag()))) {
            String showCol = colFiled + "time";
            selCol = selCol + "max(" + colFiled + "/1000.0) as " + showCol;
            this.rangeShowMap.put(colFiled, showCol);
         } else {
            selCol = selCol + colFiled;
         }
      } catch (Exception var5) {
         this.logger.error(ExceptionUtil.getStackTrace(var5));
      }

      return selCol;
   }

   private String dealDetailDimension(String colFiled) {
      String selCol = "";

      try {
         DimensionConfig dimensionConfig = this.dimensionMap.get(colFiled);
         if (dimensionConfig != null && "1".equals(String.valueOf(dimensionConfig.getFlag()))) {
            String showCol = colFiled + "time";
            selCol = selCol + "(" + colFiled + "/1000.0) as " + showCol;
            this.rangeShowMap.put(colFiled, showCol);
         } else if (CommonParams.keyword.equals(colFiled)) {
            selCol = selCol + "";
         } else {
            selCol = selCol + colFiled;
         }
      } catch (Exception var5) {
         this.logger.error(ExceptionUtil.getStackTrace(var5));
      }

      return selCol;
   }

   private String getComputer(String expression, String colFiled, String showType) {
      String result = "";

      try {
         this.checkExpression(expression);

         for (HashMap<String, String> current : this.measureList) {
            String key = current.get(CommonParams.expressionParam);
            String measure = current.get(CommonParams.measure);
            expression = expression.replaceAll(key, this.dealMeasure(measure));
         }

         if (!StringUtils.isNullOrEmpry(expression)) {
            showType = StringUtils.isNullOrEmpry(showType) ? MeasureShowType.value.toString() : showType;
            switch (MeasureShowType.valueOf(showType)) {
               case value:
                  result = "cast((" + expression + ")*100.0 as BIGINT)/100.0 as " + colFiled + ",";
                  break;
               case percent:
                  result = "cast((" + expression + ")*10000.0 as BIGINT)/100.0+'%' as " + colFiled + ",";
                  break;
               default:
                  result = "cast((" + expression + ")*100.0 as BIGINT)/100.0 as " + colFiled + ",";
            }
         }
      } catch (Exception var9) {
         this.logger.error(ExceptionUtil.getStackTrace(var9));
      }

      return result;
   }

   private boolean checkExpression(String exp) {
      boolean isSuccess = false;
      if (!StringUtils.isNullOrEmpry(this.switchComputerExpress(exp))) {
         isSuccess = true;
      }

      return isSuccess;
   }

   public String switchComputerExpress(String exp) {
      String result = "";
      String optionChart = "()+-*/";
      String cOptionChart = "+-*/";
      char[] charArray = exp.toCharArray();
      boolean isComItem = false;
      String comItem = "";
      HashMap<String, String> mMap = this.getMeasureMap();

      for (int i = 0; i < charArray.length; i++) {
         char tmp = charArray[i];
         if (optionChart.indexOf(tmp) != -1) {
            if (i == 0 && cOptionChart.indexOf(tmp) != -1) {
               this.erroeMsg = "表达式" + exp + "格式错误";
               break;
            }

            if (!"".equals(result)) {
               String tResult = result + comItem;
               String lastStr = tResult.length() <= 1 ? tResult : tResult.substring(tResult.length() - 1, tResult.length());
               if (cOptionChart.indexOf(lastStr) != -1 && cOptionChart.indexOf(tmp) != -1) {
                  this.erroeMsg = "表达式" + exp + "格式错误";
               }
            }

            if (!"".equals(comItem)) {
               String tmKey = comItem;
               comItem = CommonFun.isNumber(comItem) ? comItem : mMap.get(comItem);
               if (comItem == null) {
                  this.erroeMsg = "表达式" + exp + "中" + tmKey + " 不是合法项";
               }
            }

            result = result + comItem + tmp;
            comItem = "";
         } else {
            comItem = comItem + tmp;
         }
      }

      comItem = CommonFun.isNumber(comItem) ? comItem : mMap.get(comItem);
      if (comItem == null) {
         this.erroeMsg = "表达式" + exp + "中" + comItem + " 不是合法项";
      } else {
         result = result + comItem;
      }

      String lastStr = result.length() <= 1 ? result : result.substring(result.length() - 1, result.length());
      if (cOptionChart.indexOf(lastStr) != -1) {
         this.erroeMsg = "表达式" + exp + "格式错误";
      }

      return result;
   }

   public boolean isDurationDimsion(String key) {
      boolean result = false;

      try {
         DimensionConfig dimensionConfig = this.dimensionMap.get(key);
         if (dimensionConfig != null) {
            int flag = dimensionConfig.getFlag();
            if (flag == 1 || flag == 2) {
               result = true;
            }
         } else {
            this.logger.info("isDurationDimsion 维度库找不到维度" + key);
         }
      } catch (Exception var5) {
         this.logger.error(ExceptionUtil.getStackTrace(var5));
      }

      return result;
   }

   public boolean isModel(String key) {
      return CommonParams.modelFiemld.equals(key);
   }

   public HashMap<String, String> getMeasureMap() {
      if (this.measureMap.size() == 0) {
         for (HashMap<String, String> currend : this.measureList) {
            String key = currend.get(CommonParams.expressionParam);
            String measure = currend.get(CommonParams.measure);
            this.measureMap.put(key, measure);
         }
      }

      return this.measureMap;
   }

   public String getTableName() {
      return this.tableName;
   }

   public void setTableName(String tableName) {
      this.tableName = tableName;
   }

   public String getOrder() {
      return this.order;
   }

   public void setOrder(String order) {
      this.order = order;
   }

   public String getOrderType() {
      return this.orderType;
   }

   public void setOrderType(String orderType) {
      this.orderType = orderType;
   }

   public List<HashMap<String, String>> getMeasureList() {
      return this.measureList;
   }

   public void setMeasureList(List<HashMap<String, String>> measureList) {
      this.measureList = measureList;
   }
}
