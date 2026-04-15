package com.iflytek.vie.app.provider.common;

import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.provider.pojo.CommonFun;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.CommonXmlConfig;
import com.iflytek.vie.constants.DimType;
import com.iflytek.vie.constants.DimensionMappingConfig;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class AnyDrillParse {
   public DecimalFormat decimalFormat = new DecimalFormat("###0.0");
   public DecimalFormat decimalFormat2 = new DecimalFormat("###0.000");
   public DecimalFormat longFormat = new DecimalFormat("###0");
   public LinkedHashMap<String, String> rangeShowMap = new LinkedHashMap<>();
   public List<String> showColOrder = new ArrayList<>();
   public List<String> showColText = new ArrayList<>();
   public List<String> specialCol = new ArrayList<>();
   public List<String> rangeList = new ArrayList<>();
   public String erroeMsg = "";
   private Logger log;
   private String tableName;
   private int pageNum;
   private int pageSize;
   private String order;
   private String orderType;
   private List<HashMap<String, String>> measureList;
   private String startTime;
   private String endTime;
   private HashMap<String, Object> timeCondition;
   private List<String> modelList = new ArrayList<>();
   private LinkedHashMap<String, DimensionConfig> dimensionMap;
   private LinkedHashMap<String, String> rangeMap = new LinkedHashMap<>();
   private HashMap<String, String> measureMap = new HashMap<>();
   private String dataSql;

   public AnyDrillParse() {
   }

   public AnyDrillParse(
      List<HashMap<String, String>> measureList,
      LinkedHashMap<String, DimensionConfig> dimensionMap,
      String tableName,
      String startTime,
      String endTime,
      List<String> modelList,
      int pageNum,
      int pageSize,
      String order,
      String orderType,
      Logger log
   ) {
      this.measureList = measureList;
      this.tableName = tableName;
      this.pageNum = pageNum;
      this.pageSize = pageSize;
      this.order = order;
      this.orderType = orderType;
      this.log = log;
      this.startTime = startTime;
      this.endTime = endTime;
      this.dimensionMap = dimensionMap;
      this.modelList = modelList;
      if (!StringUtils.isNullOrEmpry(startTime) && !StringUtils.isNullOrEmpry(endTime)) {
         String tc = startTime + "|" + endTime;
         this.timeCondition = new HashMap<>();
         this.timeCondition.put(CommonParams.conditionFiled, "time");
         this.timeCondition.put(CommonParams.conditionType, DimType.timeRange.getName());
         this.timeCondition.put(CommonParams.conditionValue, tc);
      }
   }

   public AnyDrillParse(
      List<HashMap<String, String>> measureList,
      LinkedHashMap<String, DimensionConfig> dimensionMap,
      String tableName,
      String startTime,
      String endTime,
      List<String> modelList,
      int pageNum,
      int pageSize,
      String order,
      String orderType,
      String dataSql,
      Logger log
   ) {
      this.measureList = measureList;
      this.tableName = tableName;
      this.pageNum = pageNum;
      this.pageSize = pageSize;
      this.order = order;
      this.orderType = orderType;
      this.log = log;
      this.startTime = startTime;
      this.endTime = endTime;
      this.dimensionMap = dimensionMap;
      this.modelList = modelList;
      if (!StringUtils.isNullOrEmpry(startTime) && !StringUtils.isNullOrEmpry(endTime)) {
         String tc = startTime + "|" + endTime;
         this.timeCondition = new HashMap<>();
         this.timeCondition.put(CommonParams.conditionFiled, "time");
         this.timeCondition.put(CommonParams.conditionType, DimType.timeRange.getName());
         this.timeCondition.put(CommonParams.conditionValue, tc);
      }

      this.dataSql = dataSql;
   }

   public String dealStatsTable(HashMap<String, Object> params, boolean isDetail, String dataSource) {
      String sql = "";

      try {
         StringBuffer selColBuffer = new StringBuffer();
         StringBuffer groupColBuffer = new StringBuffer();
         String condition = "";
         List<HashMap<String, Object>> reportConD = (List<HashMap<String, Object>>)params.get(CommonParams.condition);
         List<HashMap<String, String>> talColList = (List<HashMap<String, String>>)params.get(CommonParams.tabCol);
         if (isDetail) {
            HashMap<String, String> idMap = new HashMap<>();
            idMap.put("name", "id");
            idMap.put("text", "id");
            idMap.put("type", "dimension");
            idMap.put("filed", "id");
            idMap.put("expression", "id");
            idMap.put("showType", "value");
            idMap.put("dtype", "mulEqu");
            talColList.add(idMap);
         }

         boolean containVoiceId = false;
         if (IndexConstants.getInsightType(dataSource) == 1) {
            for (HashMap<String, String> talCol : (List<HashMap<String, String>>)talColList) {
               if (talCol.get("filed").equals("voiceId")) {
                  containVoiceId = true;
                  break;
               }
            }
         }

         boolean isRange = false;
         boolean isModel = false;
         boolean isKwd = false;
         condition = this.dealReportCondition(reportConD, containVoiceId, isDetail);

         for (HashMap<String, String> talColx : (List<HashMap<String, String>>)talColList) {
            String colType = talColx.get(CommonParams.tabColType);
            String colFiled = talColx.get(CommonParams.tabColFiled);
            String expression = talColx.get(CommonParams.tabColExp);
            String showType = talColx.get(CommonParams.tabColShowType);
            String showText = talColx.get(CommonParams.tabColText);
            colType = isDetail ? TableColType.detail.toString() : colType;
            boolean ctRange = false;
            ctRange = this.dealMeasure(colFiled, colType, expression, showType, selColBuffer, groupColBuffer, isDetail, containVoiceId);
            this.showColOrder.add(colFiled);
            this.showColText.add(showText);
            isRange = isRange || ctRange;
            if (CommonParams.modelFiemld.equals(colFiled)) {
               isModel = true;
            }

            if (CommonParams.keyword.equals(colFiled)) {
               isKwd = true;
            }
         }

         if (this.rangeList.size() > 0) {
            String temp = "";

            for (String str : this.rangeList) {
               temp = temp + str + ",";
            }

            if (!StringUtils.isNullOrEmpry(temp)) {
               selColBuffer = new StringBuffer(temp + selColBuffer.toString());
            }
         }

         if (this.specialCol.size() > 0) {
            for (String str : this.specialCol) {
               selColBuffer.append(str + ",");
               groupColBuffer.append(str + ",");
            }
         }

         if ((isKwd || isModel) && isDetail && selColBuffer.indexOf("id") == -1 && selColBuffer.indexOf("childVoiceId") == -1) {
            selColBuffer.append("id,");
         }

         if (isModel && !isDetail) {
            condition = this.addModelCondition(condition);
         }

         sql = this.dealTableSql(condition, isRange, selColBuffer, groupColBuffer, isDetail, dataSource);
      } catch (Exception var22) {
         this.log.error("[处理统计报表(dealStatsTable)]出现异常", var22);
      }

      return sql;
   }

   private String addModelCondition(String condition) {
      try {
         String temp = "";

         for (String current : this.modelList) {
            temp = temp + current + ",";
         }

         if (temp.length() > 1) {
            temp = temp.substring(0, temp.length() - 1);
            condition = condition + CommonParams.modelFiemld + " in (" + temp + ") and ";
         } else {
            condition = condition + CommonParams.modelFiemld + " in (-1) and ";
            this.log.error("can not find effective model");
         }
      } catch (Exception var5) {
         this.log.error("[添加模型筛选条件(addModelCondition)]出现异常", var5);
      }

      return condition;
   }

   private String dealTableSql(String condition, boolean isRange, StringBuffer selColBuffer, StringBuffer groupColBuffer, boolean isDetail, String dataSource) {
      String sql = "";

      try {
         String selCol = "";
         String groupCol = "";
         String having = "";
         String orderStr = "";
         if (!StringUtils.isNullOrEmpry(this.order) && !StringUtils.isNullOrEmpry(this.orderType)) {
            if (isDetail && IndexConstants.getInsightType(dataSource) == 1 && selColBuffer.indexOf("childVoiceId") != -1) {
               this.order = DimensionMappingConfig.getMappingName(this.order) == null ? this.order : DimensionMappingConfig.getMappingName(this.order);
            }

            String trueOrder = this.rangeShowMap.get(this.order) == null ? this.order : this.rangeShowMap.get(this.order).toString();
            orderStr = orderStr + trueOrder + " " + this.orderType;
         }

         selCol = StringUtils.isNullOrEmpry(selColBuffer.toString()) ? "" : selColBuffer.toString().substring(0, selColBuffer.toString().length() - 1);
         if (isDetail && selColBuffer.indexOf("childVoiceId") != -1 && IndexConstants.getInsightType(dataSource) == 1) {
            List<String> strVoice = new ArrayList<>();
            List<String> strTask = new ArrayList<>();

            for (String group : Arrays.asList(groupColBuffer.toString().split("\\,"))) {
               if (DimensionMappingConfig.getOriginalName(group) != null) {
                  strVoice.add(group);
               } else {
                  strTask.add(group);
               }
            }

            groupColBuffer = new StringBuffer();

            for (String groupx : strVoice) {
               groupColBuffer.append(groupx + ",");
            }

            for (String groupx : strTask) {
               groupColBuffer.append(groupx + ",");
            }
         }

         groupCol = StringUtils.isNullOrEmpry(groupColBuffer.toString()) ? "" : groupColBuffer.toString().substring(0, groupColBuffer.toString().length() - 1);
         if (this.pageNum != -1 && this.pageSize != -1) {
            int rownumStart = this.pageSize * this.pageNum + 1;
            int rownumEnd = (this.pageNum + 1) * this.pageSize;
            if ("".equals(groupCol) && !isRange) {
               condition = condition + " rownum between " + rownumStart + " and " + rownumEnd + " and ";
            } else {
               having = having + " rownum between " + rownumStart + " and " + rownumEnd + " and ";
            }
         }

         condition = StringUtils.isNullOrEmpry(condition) ? "" : condition.toString().substring(0, condition.length() - 5);
         having = StringUtils.isNullOrEmpry(having) ? "" : having.toString().substring(0, having.length() - 5);
         sql = "select " + selCol + " from " + this.tableName;
         sql = StringUtils.isNullOrEmpry(condition) ? sql : sql + " where " + condition;
         sql = StringUtils.isNullOrEmpry(groupCol) ? sql : sql + " group by " + groupCol;
         sql = StringUtils.isNullOrEmpry(having) ? sql : sql + " having " + having;
         sql = !StringUtils.isNullOrEmpry(orderStr)
               && (orderStr.indexOf(CommonParams.modelFiemld.toString()) == -1 || !isDetail)
               && (orderStr.indexOf(CommonParams.keyword.toString()) == -1 || !isDetail)
            ? sql + " order by " + orderStr
            : sql;
      } catch (Exception var17) {
         this.log.error("[拼接表格sql(dealTableSql)]出现异常", var17);
      }

      return sql;
   }

   public String dealPieChart(HashMap<String, Object> params) {
      String sql = "";

      try {
         String selCol = "";
         String groupCol = "";
         String condition = "";
         String having = "";
         String orderStr = "";
         StringBuffer selColBuffer = new StringBuffer();
         StringBuffer groupColBuffer = new StringBuffer();
         List<HashMap<String, Object>> reportConD = (List<HashMap<String, Object>>)params.get(CommonParams.condition);
         condition = this.dealReportCondition(reportConD, false, false);
         String colFiled = CommonFun.getParamsValue(params, CommonParams.pieMeasure);
         String colType = CommonFun.getParamsValue(params, CommonParams.pieMType);
         String legend = CommonFun.getParamsValue(params, CommonParams.pieLegend);
         String expression = CommonFun.getParamsValue(params, CommonParams.pieMExp);
         String showType = CommonFun.getParamsValue(params, CommonParams.pieMShowType);
         if (!StringUtils.isNullOrEmpry(legend)) {
            boolean isRange = this.isDurationDimsion(legend);
            if (isRange) {
               String selX = this.getRangeStr(legend, true);
               if (selX.equals(legend)) {
                  selX = this.dealDimension(legend);
                  groupCol = groupCol + legend;
               }

               selCol = selCol + selX + ",";
            } else {
               selCol = selCol + legend + ",";
               groupCol = groupCol + legend;
            }

            if (CommonParams.modelFiemld.equals(legend)) {
               condition = this.addModelCondition(condition);
            }

            if (!StringUtils.isNullOrEmpry(colFiled) && !StringUtils.isNullOrEmpry(colType)) {
               this.dealMeasure(colFiled, colType, expression, showType, selColBuffer, groupColBuffer, true, false);
               if (!StringUtils.isNullOrEmpry(this.order) && !StringUtils.isNullOrEmpry(this.orderType)) {
                  orderStr = orderStr + this.order + " " + this.orderType;
               }

               selCol = selCol
                  + (StringUtils.isNullOrEmpry(selColBuffer.toString()) ? "" : selColBuffer.toString().substring(0, selColBuffer.toString().length() - 1));
               condition = StringUtils.isNullOrEmpry(condition) ? "" : condition.toString().substring(0, condition.length() - 5);
               sql = "select " + selCol + " from " + this.tableName;
               sql = StringUtils.isNullOrEmpry(condition) ? sql : sql + " where " + condition;
               sql = StringUtils.isNullOrEmpry(groupCol) ? sql : sql + " group by " + groupCol;
               sql = StringUtils.isNullOrEmpry(having) ? sql : sql + " having " + having;
               sql = StringUtils.isNullOrEmpry(orderStr) ? sql : sql + " order by " + orderStr;
            } else {
               this.log.error("参数错误 colFiled:" + colFiled + " colType:" + colType);
            }
         } else {
            this.log.error("参数错误 legend 不能为空！");
         }
      } catch (Exception var18) {
         this.log.error("[处理饼状图(dealPieChart)]出现异常", var18);
      }

      return sql;
   }

   public String dealLineColumnChart(HashMap<String, Object> params, LinkedHashMap<String, String> measureMap) {
      String sql = "";

      try {
         String selCol = "";
         String groupCol = "";
         String condition = "";
         String having = "";
         String orderStr = "";
         StringBuffer selColBuffer = new StringBuffer();
         StringBuffer groupColBuffer = new StringBuffer();
         List<HashMap<String, Object>> reportConD = (List<HashMap<String, Object>>)params.get(CommonParams.condition);
         condition = this.dealReportCondition(reportConD, false, false);
         Object pathCod = params.get(CommonParams.pathCod);
         if (pathCod != null) {
            condition = condition + " " + pathCod + " and ";
         }

         String xAxis = CommonFun.getParamsValue(params, CommonParams.lineColXAxis);
         List<HashMap<String, Object>> mainAxisMap = params.get(CommonParams.lineColMainAxis) == null ? null : (List)params.get(CommonParams.lineColMainAxis);
         List<HashMap<String, Object>> secondaryAxisMap = params.get(CommonParams.lineColSecondaryAxis) == null
            ? null
            : (List)params.get(CommonParams.lineColSecondaryAxis);
         if (!StringUtils.isNullOrEmpry(xAxis)) {
            boolean isRange = this.isDurationDimsion(xAxis);
            if (isRange) {
               String selX = this.getRangeStr(xAxis, true);
               if (selX.equals(xAxis)) {
                  selX = this.dealDimension(xAxis);
                  groupCol = groupCol + xAxis;
               }

               selCol = selCol + selX + ",";
            } else {
               selCol = selCol + xAxis + ",";
               groupCol = groupCol + xAxis;
            }
         } else {
            this.log.error("xAxis is null!");
         }

         if (CommonParams.modelFiemld.equals(xAxis)) {
            condition = this.addModelCondition(condition);
         }

         this.dealAxis(mainAxisMap, measureMap, selColBuffer, groupColBuffer);
         this.dealAxis(secondaryAxisMap, measureMap, selColBuffer, groupColBuffer);
         if (!StringUtils.isNullOrEmpry(this.order) && !StringUtils.isNullOrEmpry(this.orderType)) {
            String trueOrder = this.rangeShowMap.get(this.order) == null ? this.order : this.rangeShowMap.get(this.order).toString();
            orderStr = orderStr + trueOrder + " " + this.orderType;
         }

         condition = StringUtils.isNullOrEmpry(condition) ? "" : condition.toString().substring(0, condition.length() - 5);
         selCol = selCol
            + (StringUtils.isNullOrEmpry(selColBuffer.toString()) ? "" : selColBuffer.toString().substring(0, selColBuffer.toString().length() - 1));
         sql = "select " + selCol + " from " + this.tableName;
         sql = StringUtils.isNullOrEmpry(condition) ? sql : sql + " where " + condition;
         sql = StringUtils.isNullOrEmpry(groupCol) ? sql : sql + " group by " + groupCol;
         sql = StringUtils.isNullOrEmpry(having) ? sql : sql + " having " + having;
         sql = StringUtils.isNullOrEmpry(orderStr) ? sql : sql + " order by " + orderStr;
      } catch (Exception var18) {
         this.log.error("[处理柱折图(dealLineColumnChart)]出现异常", var18);
      }

      return sql;
   }

   private void dealAxis(List<HashMap<String, Object>> data, LinkedHashMap<String, String> measureMap, StringBuffer selColBuffer, StringBuffer groupColBuffer) {
      if (data != null) {
         for (HashMap<String, Object> currentMap : (List<HashMap<String, Object>>)data) {
            String colFiled = CommonFun.getParamsValue(currentMap, CommonParams.lineColMeasure);
            String colText = CommonFun.getParamsValue(currentMap, CommonParams.lineColText);
            String colType = CommonFun.getParamsValue(currentMap, CommonParams.lineColType);
            String expression = CommonFun.getParamsValue(currentMap, CommonParams.lineColExp);
            this.dealMeasure(colFiled, colType, expression, MeasureShowType.value.toString(), selColBuffer, groupColBuffer, true, false);
            measureMap.put(colFiled, colText);
         }
      }
   }

   private boolean dealMeasure(
      String colFiled, String colType, String expression, String showType, StringBuffer selCol, StringBuffer groupCol, boolean isDetail, boolean containVoiceId
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
               }
               break;
            case computer:
               selCol.append(this.getComputer(expression, colFiled, showType));
               break;
            case detail:
               String detailFiled = "";
               detailFiled = this.dealDetailDimension(colFiled, containVoiceId, groupCol, isDetail);
               if (!StringUtils.isNullOrEmpry(detailFiled)) {
                  selCol.append(detailFiled + ",");
               }
               break;
            default:
               this.log.error(colType + " formar error!");
         }
      } catch (Exception var11) {
         this.log.error("[获取列查询名(dealMeasure)]出现异常", var11);
      }

      return isRange;
   }

   private String getMeasure(String colFiled) {
      String selCol = "";

      try {
         selCol = this.dealMeasure(colFiled);
         if (!StringUtils.isNullOrEmpry(selCol)) {
            selCol = selCol + " as " + colFiled + ",";
         } else {
            this.log.error(colFiled + " formar error!");
         }
      } catch (Exception var4) {
         this.log.error(colFiled + " formar error!");
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
            this.log.error(colFiled + " RangeData error!");
         }
      } catch (Exception var6) {
         this.log.error(colFiled + " formar error!");
      }

      return selCol;
   }

   private String getDefaultTimeRange(String colFiled) {
      String rangeValue = "";
      Map<String, String> defaultDurationMap = CommonXmlConfig.getDurationMap();
      if (defaultDurationMap.keySet().contains(colFiled)) {
         String[] default_rang = defaultDurationMap.get(colFiled).split(",");

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
                  rangeValue = rangeValue + this.decimalFormat.format(start) + "_" + this.decimalFormat.format(end) + "_" + oldS + "~" + oldE + ",";
               } else {
                  start *= 1000.0;
                  end = end * 1000.0 - 0.001;
                  rangeValue = rangeValue + this.decimalFormat2.format(start) + "_" + this.decimalFormat2.format(end) + "_" + oldS + "~" + oldE + ",";
               }
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
            if (dimensionConfig != null && dimensionConfig.getFlag() == 1) {
               selCol = selCol + "Math.round(" + col[1] + "(" + col[0] + ")/1000.0)";
            } else {
               selCol = selCol + "Math.round(" + col[1] + "(" + col[0] + ")" + "*100.0)/100.0";
            }
         } else {
            this.log.error(colFiled + " formar error!");
         }
      } catch (Exception var6) {
         this.log.error("[处理指标(dealMeasure)]出现异常", var6);
      }

      return selCol;
   }

   private String dealDimension(String colFiled) {
      String selCol = "";

      try {
         DimensionConfig dimensionConfig = this.dimensionMap.get(colFiled);
         if (dimensionConfig != null && dimensionConfig.getFlag() == 1) {
            String showCol = colFiled + "time";
            selCol = selCol + "max(" + colFiled + "/1000.0) as " + showCol;
            this.rangeShowMap.put(colFiled, showCol);
         } else {
            selCol = selCol + colFiled;
         }
      } catch (Exception var5) {
         this.log.error("[处理维度(dealDimension)]出现异常", var5);
      }

      return selCol;
   }

   private String dealDetailDimension(String colFiled, boolean containVoiceId, StringBuffer groupCol, boolean isDetail) {
      String selCol = "";

      try {
         DimensionConfig dimensionConfig = this.dimensionMap.get(colFiled);
         if (dimensionConfig != null && (dimensionConfig.getFlag() == 1 || dimensionConfig.getFlag() == 2)) {
            String showCol = "";
            if (containVoiceId && isDetail) {
               showCol = DimensionMappingConfig.getMappingName(colFiled) == null ? colFiled : DimensionMappingConfig.getMappingName(colFiled);
               selCol = selCol + showCol;
               groupCol.append(showCol + ",");
            } else {
               showCol = colFiled;
               selCol = selCol + "(" + colFiled + "/1000.0) as " + colFiled;
            }

            this.rangeShowMap.put(colFiled, showCol);
         } else if (!CommonParams.keyword.equals(colFiled) && !CommonParams.modelFiemld.equals(colFiled)) {
            String filed = "";
            if (containVoiceId && isDetail) {
               filed = DimensionMappingConfig.getMappingName(colFiled) == null ? colFiled : DimensionMappingConfig.getMappingName(colFiled);
               selCol = selCol + filed;
               groupCol.append(filed + ",");
            } else {
               selCol = selCol + colFiled;
            }
         } else {
            selCol = selCol + "";
         }
      } catch (Exception var8) {
         this.log.error("[处理维度(dealDetailDimension)]出现异常", var8);
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
                  result = expression + " as " + colFiled + ",";
                  break;
               case percent:
                  result = "Math.round((" + expression + ")*10000.0)/100.0+'%' as " + colFiled + ",";
                  break;
               default:
                  result = "Math.round((" + expression + ")*100.0)/100.0 as " + colFiled + ",";
            }
         }
      } catch (Exception var9) {
         this.log.error("[获取计算列查询项(getComputer)]出现异常", var9);
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
      String reg = "^\\d+$";
      if (String.valueOf(charArray[0]).matches(reg)) {
         this.erroeMsg = "表达式" + exp + "格式错误";
      }

      String comItem = "";
      HashMap<String, String> mMap = this.getMeasureMap();
      String lastChar = "";

      for (int i = 0; i < charArray.length; i++) {
         char tmp = charArray[i];
         if ("(".equals(tmp + "") && !"".equals(lastChar) && cOptionChart.indexOf(lastChar + "") == -1
            || ")".equals(lastChar) && cOptionChart.indexOf(tmp + "") == -1) {
            this.erroeMsg = "表达式" + exp + "格式错误";
            break;
         }

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
            lastChar = tmp + "";
         } else {
            comItem = comItem + tmp;
            lastChar = tmp + "";
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

   private boolean isNumeric(String str) {
      int i = str.length();

      while (--i >= 0) {
         if (!Character.isDigit(str.charAt(i))) {
            return false;
         }
      }

      return true;
   }

   public String dealReportCondition(List<HashMap<String, Object>> reportConD, boolean containVoiceId, boolean isDetail) {
      String result = CommonParams.processed + "=0 and ";
      result = !StringUtils.isNullOrEmpry(this.dataSql) ? result + this.dataSql + " and " : result;

      try {
         reportConD = (List<HashMap<String, Object>>)(reportConD != null ? reportConD : new ArrayList<>());
         if (this.timeCondition != null) {
            reportConD.add(this.timeCondition);
         }

         for (HashMap<String, Object> currentCond : (List<HashMap<String, Object>>)reportConD) {
            String type = currentCond.get(CommonParams.conditionType).toString().trim();
            String key = currentCond.get(CommonParams.conditionFiled).toString();
            Object condValue = currentCond.get(CommonParams.conditionValue);
            Object isNegate = currentCond.get(CommonParams.conditionNegate);
            String conValue = "";
            switch (DimType.valueOf(type)) {
               case timeDim:
               default:
                  break;
               case offLineTagId:
                  conValue = this.dealMulModelCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     if (containVoiceId && isDetail) {
                        key = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
                     }

                     result = result + " " + key + " in (" + conValue + ") and ";
                  }
                  break;
               case radio:
                  conValue = dealRadioCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     if (containVoiceId && isDetail) {
                        key = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
                     }

                     result = result + " " + key + "=" + conValue + " and ";
                  }
                  break;
               case mulEqu:
                  conValue = this.dealMulEqlCond(condValue);
                  String opt = "true";
                  if (isNegate != null && "1".equals(isNegate.toString())) {
                     opt = "false";
                  }

                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     if (containVoiceId && isDetail) {
                        key = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
                     }

                     result = result + " fulltext('" + key + ":(" + conValue + ")')=" + opt + " and";
                  }
                  break;
               case range:
                  conValue = this.dealRangeCond(key, condValue, containVoiceId, isDetail);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     result = result + " " + conValue + " and ";
                  }
                  break;
               case timeRange:
                  conValue = this.dealTRCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     result = result + " " + CommonParams.timestamp + " " + conValue + " and ";
                  }
                  break;
               case mulSel:
                  conValue = this.dealMulCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     if (containVoiceId && isDetail) {
                        key = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
                     }

                     result = result + " " + key + " in (" + conValue + ") and ";
                  }
            }
         }
      } catch (Exception var13) {
         this.log.error("[处理报表条件(dealReportCondition)]出现异常", var13);
      }

      return result;
   }

   public String dealReportCondition(List<HashMap<String, Object>> reportConD, String optionType) {
      String result = CommonParams.processed + "=0 and ";
      result = !StringUtils.isNullOrEmpry(this.dataSql) ? result + this.dataSql + " and " : result;
      String dimension = "";

      try {
         reportConD = (List<HashMap<String, Object>>)(reportConD != null ? reportConD : new ArrayList<>());
         if (this.timeCondition != null) {
            reportConD.add(this.timeCondition);
         }

         for (HashMap<String, Object> currentCond : (List<HashMap<String, Object>>)reportConD) {
            String type = currentCond.get(CommonParams.conditionType).toString().trim();
            String key = currentCond.get(CommonParams.conditionFiled).toString();
            Object condValue = currentCond.get(CommonParams.conditionValue);
            Object isNegate = currentCond.get(CommonParams.conditionNegate);
            String conValue = "";
            switch (DimType.valueOf(type)) {
               case timeDim:
               default:
                  break;
               case offLineTagId:
                  conValue = this.dealMulModelCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     dimension = dimension + " " + key + " in (" + conValue + ") " + optionType + " ";
                  }
                  break;
               case radio:
                  conValue = this.dealMulCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     dimension = dimension + " " + key + "=" + conValue + " " + optionType + " ";
                  }
                  break;
               case mulEqu:
                  conValue = this.dealMulEqlCond(condValue);
                  String opt = "true";
                  if (isNegate != null && "1".equals(isNegate.toString())) {
                     opt = "false";
                  }

                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     result = result + "fulltext('" + key + ":(" + conValue + ")')=" + opt + " and";
                  }
                  break;
               case range:
                  conValue = this.dealRangeCond(key, condValue, false, false);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     dimension = dimension + " " + conValue + " " + optionType + " ";
                  }
                  break;
               case timeRange:
                  conValue = this.dealTRCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     result = result + " " + CommonParams.timestamp + " " + conValue + " and ";
                  }
                  break;
               case mulSel:
                  conValue = this.dealMulCond(condValue);
                  if (!StringUtils.isNullOrEmpry(conValue)) {
                     dimension = dimension + " " + key + " in (" + conValue + ") " + optionType + " ";
                  }
            }
         }

         if (!StringUtils.isNullOrEmpry(dimension)) {
            dimension = dimension.substring(0, dimension.length() - (2 + optionType.length()));
            result = result + "(" + dimension + ") and ";
         }
      } catch (Exception var13) {
         this.log.error("[处理报表条件(dealReportCondition)]出现异常", var13);
      }

      return result;
   }

   private String dealMulEqlCond(Object condValue) {
      String result = "";

      try {
         for (String current : (List<String>)condValue) {
            if (current.indexOf("*") == -1 && current.indexOf("?") == -1) {
               current = current + "*";
            }

            result = result + "" + current + " OR ";
         }

         result = result.length() > 0 ? result.substring(0, result.length() - 4) : result;
      } catch (Exception var6) {
         this.log.error("[处理单多选条件(dealMulEqlCond)]出现异常", var6);
      }

      return result;
   }

   private static String dealRadioCond(Object condValue) {
      String result = "";
      if (condValue instanceof String) {
         result = " '" + condValue + "'";
      } else {
         result = " " + condValue;
      }

      return result;
   }

   private String dealMulCond(Object condValue) {
      String result = "";

      try {
         for (String current : (List<String>)condValue) {
            result = result + "'" + current + "',";
         }

         result = result.length() > 0 ? result.substring(0, result.length() - 1) : result;
      } catch (Exception var6) {
         this.log.error("[处理单多选条件(dealMulCond)]出现异常", var6);
      }

      return result;
   }

   private String dealMulModelCond(Object condValue) {
      String result = "";

      try {
         for (HashMap<String, String> current : (List<HashMap<String, String>>)condValue) {
            result = result + "" + current.get("key") + ",";
         }

         result = result.length() > 0 ? result.substring(0, result.length() - 1) : result;
      } catch (Exception var6) {
         this.log.error("[处理模型(dealMulModelCond)]出现异常", var6);
      }

      return result;
   }

   private String dealTRCond(Object condValue) {
      String result = "";

      try {
         String exp = (String)condValue;
         if (!StringUtils.isNullOrEmpry(exp)) {
            String[] expArray = exp.split("\\|");
            if (expArray.length == 2) {
               String begin = expArray[0];
               String end = expArray[1];
               end = "uptonow".equals(end) ? DateUtils.parseDateToString(new Date(), "yyyy-MM-dd HH:mm:ss") : end;
               result = " between " + DateUtils.getTimestampByFULL(begin) + " and " + DateUtils.getTimestampByFULL(end) + " ";
            }
         }

         result = result.length() > 0 ? result.substring(0, result.length() - 1) : result;
      } catch (Exception var7) {
         this.log.error("[处理单多选条件(dealTRCond)]出现异常", var7);
      }

      return result;
   }

   private String dealRangeCond(String key, Object condValue, boolean containVoiceId, boolean isDetail) {
      String result = "";
      String rangeValue = "";

      try {
         List<String> valueList = (List<String>)condValue;
         boolean isEqualSE = false;

         for (String current : valueList) {
            String[] params = current.split("\\|");
            double start = 0.0;
            double end = 0.0;
            String oldS = null;
            String oldE = null;
            if (params.length == 2) {
               start = CommonFun.isNumber(params[0]) ? Double.parseDouble(params[0]) : Integer.parseInt(params[0]);
               end = CommonFun.isNumber(params[1]) ? Double.parseDouble(params[1]) : Integer.parseInt(params[1]);
               oldS = CommonFun.isNumber(params[0]) ? this.longFormat.format(start) + "" : this.longFormat.format(start) + "";
               oldE = CommonFun.isNumber(params[1]) ? this.longFormat.format(end) + "" : this.longFormat.format(end) + "";
            } else if (params.length == 1) {
               start = CommonFun.isNumber(params[0]) ? Double.parseDouble(params[0]) : Integer.parseInt(params[0]);
               end = 2.147483647E9;
               oldS = CommonFun.isNumber(params[0]) ? this.longFormat.format(start) + "" : this.longFormat.format(start) + "";
               oldE = "";
            }

            if (Double.compare(start, end) == 0) {
               isEqualSE = true;
            }

            if (this.isDurationDimsion(key)) {
               if (!key.equals("n0avgSpeed") && !key.equals("n1avgSpeed")) {
                  start = start * 1000.0 - 500.0 > 0.0 ? start * 1000.0 - 500.0 : start * 1000.0;
                  end = end * 1000.0 + 499.0;
               } else {
                  start *= 1000.0;
                  end *= 1000.0;
               }
            }

            String voiceKey = "";
            if (containVoiceId && isDetail) {
               voiceKey = DimensionMappingConfig.getMappingName(key) == null ? key : DimensionMappingConfig.getMappingName(key);
               result = result
                  + "("
                  + voiceKey
                  + ">="
                  + this.decimalFormat.format(start)
                  + " and "
                  + voiceKey
                  + "<="
                  + this.decimalFormat.format(end)
                  + ") or ";
            } else {
               result = result + "(" + key + ">=" + this.decimalFormat.format(start) + " and " + key + "<=" + this.decimalFormat.format(end) + ") or ";
            }

            rangeValue = rangeValue + this.decimalFormat.format(start) + "_" + this.decimalFormat.format(end) + "_" + oldS + "~" + oldE + ",";
         }

         result = !"".equals(result) ? "(" + result.substring(0, result.length() - 4) + ")" : result;
         if (!isEqualSE || valueList.size() != 1) {
            rangeValue = !"".equals(rangeValue) ? rangeValue.substring(0, rangeValue.length() - 1) : rangeValue;
            this.rangeMap.put(key, rangeValue);
         }
      } catch (Exception var19) {
         this.log.error("[处理区间条件(dealRangeCond)]出现异常", var19);
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
            this.log.info("isDurationDimsion 找不到维度" + key);
         }
      } catch (Exception var5) {
         this.log.error("[判读维度是否为时长维度(isDurationDimsion)]出现异常", var5);
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

   public Logger getLog() {
      return this.log;
   }

   public void setLog(Logger log) {
      this.log = log;
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
