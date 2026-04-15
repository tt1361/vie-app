package com.iflytek.vie.app.provider.impl.datadrill;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.iflytek.vie.app.api.datadrill.FilterAnaylseService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.datadrill.FilterDimension;
import com.iflytek.vie.app.pojo.datadrill.FilterRequest;
import com.iflytek.vie.app.pojo.datadrill.FilterRuleEnum;
import com.iflytek.vie.app.pojo.filter.FilterInfo;
import com.iflytek.vie.app.provider.database.FilterInfoDB;
import com.iflytek.vie.app.provider.database.ModelDBPlatform;
import com.iflytek.vie.app.provider.engine.FieldType;
import com.iflytek.vie.app.provider.engine.StringUtils;
import com.iflytek.vie.app.provider.impl.datadrill.parse.FilterParse;
import com.iflytek.vie.app.provider.impl.datadrill.parse.ModelFilterParse;
import com.iflytek.vie.app.provider.impl.datadrill.parse.RuleFilterParse;
import com.iflytek.vie.app.provider.ruleparse.ModelParserPlatform;
import com.iflytek.vie.app.provider.ruleparse.ParserResultPlatform;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.anydrill.calculate.set.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterAnaylseServiceImpl implements FilterAnaylseService {
   private static final String MESSAGE = "message";
   private static final String SQL = "sql";
   private static final String SUCCESS = "success";
   private static final String IS_MODEL = "isModel";
   private static final String MODEL = "offLineTagId";
   private static final String ERRREGEX = "!error\\[(.{0,})\\]";
   private ModelDBPlatform modelDB;
   private FilterInfoDB filterDB;
   private static final Logger logger = LoggerFactory.getLogger(FilterAnaylseServiceImpl.class);

   public Map<String, Object> fetchSqlByFilterDimension(FilterRequest request) throws ViePlatformServiceException {
      Date d1 = new Date();
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "成功获取筛选器sql");
      response.put("isModel", 0);
      logger.debug("fetchSqlByFilterDimension method has start!");
      List<FilterDimension> filters = request.getFilter();
      Map<String, String> aliasToSqlMap = new HashMap<>();
      if (this.dealConditionSql(aliasToSqlMap, filters, response, request.getDataSource())) {
         if (null != request.getExpression() && !"".equals(request.getExpression())) {
            ModelParserPlatform mpp = new ModelParserPlatform(null, request.getExpression(), null);
            mpp.setReplace(false);
            ParserResultPlatform parserResult = mpp.parse();
            if (parserResult.getRet() >= 0) {
               response.put("success", false);
               response.put("message", "筛选器规则校验不通过" + parserResult.getMsg());
               return response;
            }
         }

         String sql = this.assemblingSql(aliasToSqlMap, request.getExpression(), response);
         logger.debug("fetchSqlByFilterDimension method has end !fetch sql:" + sql + ", cost time :" + (new Date().getTime() - d1.getTime()) / 1000L + "s");
         response.put("sql", sql);
      } else {
         response.put("success", false);
      }

      return response;
   }

   public Map<String, Object> fetchSqlByFilterId(Integer filterId) throws ViePlatformServiceException {
      Map<String, Object> response = new HashMap<>();
      response.put("success", true);
      response.put("message", "成功获取筛选器sql");

      try {
         FilterInfo filterInfo = this.filterDB.queryFilterInfoById(filterId);
         if (filterInfo == null) {
            response.put("success", false);
            response.put("message", "要使用的筛选器不存在");
         } else {
            response.put("isModel", filterInfo.getContainModel());
            response.put("sql", filterInfo.getFilterSql());
         }
      } catch (VieAppServiceException var4) {
         response.put("success", false);
         response.put("message", "查询筛选器失败");
         logger.error("查询筛选器条件出错!", var4);
      }

      return response;
   }

   private String assemblingSql(Map<String, String> aliasToSqlMap, String expression, Map<String, Object> response) {
      if (StringUtils.isNullOrEmpry(expression.trim())) {
         return "";
      } else {
         String sql = "(" + expression.trim() + ")";
         char[] conger = new char[]{'&', '|', '(', ')'};
         int current = 0;
         int preview = 0;

         StringBuffer stringBuffer;
         for (stringBuffer = new StringBuffer(); current < sql.length(); current++) {
            char a1 = sql.charAt(current);
            if (containChar(conger, a1)) {
               String str = sql.substring(preview, current);
               if (str != null && !"".equals(str)) {
                  String ruleStr = aliasToSqlMap.get(str.trim());
                  if (ruleStr == null || "".equals(ruleStr)) {
                     response.put("success", false);
                     response.put("message", "别名为" + str.trim() + " 的表达式未匹配上相应条件");
                     return "error";
                  }

                  stringBuffer.append(aliasToSqlMap.get(str.trim()));
               }

               switch (a1) {
                  case '&':
                     stringBuffer.append(" and ");
                     break;
                  case '|':
                     stringBuffer.append(" or ");
                     break;
                  default:
                     stringBuffer.append(a1);
               }

               preview = current + 1;
            }
         }

         String newSql = stringBuffer.toString();
         logger.debug("通过筛选器生成的sql语句:" + newSql);
         return newSql;
      }
   }

   private boolean dealConditionSql(Map<String, String> aliasToSqlMap, List<FilterDimension> filters, Map<String, Object> response, String dataSource) {
      FilterParse filterParse = null;
      boolean containModel = false;

      for (int i = 0; i < filters.size(); i++) {
         FilterDimension filterDimension = filters.get(i);
         Map<Long, String> allModelMaps = null;
         if ("offLineTagId".equals(filterDimension.getKey())) {
            if (allModelMaps == null || !containModel) {
               allModelMaps = this.modelDB.queryAllModels();
               containModel = true;
               response.put("isModel", 1);
            }

            filterParse = new ModelFilterParse(
               filterDimension.getKey(),
               filterDimension.getValue(),
               FilterRuleEnum.ValueOf(filterDimension.getOpt()),
               FieldType.valueOf(filterDimension.getType()),
               allModelMaps
            );
         } else {
            if ("long".equals(filterDimension.getType()) || "Long".equals(filterDimension)) {
               filterDimension.setType("integer");
            }

            filterParse = new RuleFilterParse(
               filterDimension.getKey(),
               filterDimension.getValue(),
               FilterRuleEnum.ValueOf(filterDimension.getOpt()),
               FieldType.valueOf(filterDimension.getType())
            );
         }

         String toSql = this.dealToSql(filterParse.getSql(), response, dataSource);
         if (toSql == null || "error".equals(toSql)) {
            response.put("success", false);
            return false;
         }

         aliasToSqlMap.put(filterDimension.getAlias(), toSql);
      }

      return true;
   }

   private String dealToSql(String sql, Map<String, Object> response, String dataSource) {
      Pattern pattern = Pattern.compile("!error\\[(.{0,})\\]");
      Matcher matcher = pattern.matcher(sql);
      if (matcher.find()) {
         response.put("message", matcher.group(1));
         return "error";
      } else {
         String regx = "\\((select.{1,}from.{1,})\\)";
         matcher.usePattern(Pattern.compile(regx));
         matcher.reset(sql);
         if (!matcher.find()) {
            return sql;
         } else {
            String indexAnydrillModelAddress = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
            ExcuteContext indexContext = new ExcuteContext();
            logger.debug(" model anydrill url :" + indexAnydrillModelAddress + ", select sql:" + sql);
            String anydrillSql = matcher.group(1);
            StringBuffer sb = new StringBuffer("(");
            ResultSet resultSet = indexContext.executeSearchQuery(indexAnydrillModelAddress, anydrillSql);
            if (resultSet != null && resultSet.getTotalCount() > 0L) {
               List<LinkedHashMap<String, Object>> idLists = resultSet.toList();
               int pre = 0;

               for (int i = 0; i < idLists.size(); i++) {
                  String id = String.valueOf(idLists.get(i).get("id"));
                  if (!StringUtils.isNullOrEmpry(id)) {
                     if (pre++ > 0) {
                        sb.append(",");
                     }

                     sb.append("'").append(id).append("'");
                  }
               }
            }

            sb.append(")");
            if ("()".equals(sb.toString())) {
               sb = new StringBuffer("('')");
            }

            return sql.substring(0, matcher.start()) + sb.toString();
         }
      }
   }

   private static boolean containChar(char[] conger, char a1) {
      for (int i = 0; i < conger.length; i++) {
         if (a1 == conger[i]) {
            return true;
         }
      }

      return false;
   }

   private String dealInputRules(String input) {
      String[] a = input.split("");
      String[] arr = this.deleteOne(a);
      List<Integer> list = new ArrayList<>();

      for (int i = 0; i < arr.length; i++) {
         if ("!".equals(arr[i])) {
            list.add(i);
         }
      }

      if (list != null && list.size() > 0) {
         for (int j = 0; j < list.size(); j++) {
            String ss = input.substring(list.get(j) + 1, input.length());
            if (ss.startsWith("(")) {
               String str2 = this.getPP(ss);
               String[] str2s = str2.split("");
               String[] str22 = this.deleteOne(str2s);
               String result = "";

               for (int m = 0; m < str22.length; m++) {
                  if ("|".equals(str22[m])) {
                     result = result + "&";
                  } else if ("&".equals(str22[m])) {
                     result = result + "|";
                  } else {
                     result = result + str22[m];
                  }
               }

               input = input.substring(0, list.get(j) + 1) + result + input.substring(list.get(j) + 1 + str2.length(), input.length());
            }
         }
      }

      return input;
   }

   private String[] deleteOne(String[] strArr) {
      return Arrays.copyOfRange(strArr, 1, strArr.length);
   }

   public String getPP(String str) {
      Stack<Character> stack = new Stack<>();
      int n = 0;

      for (int j = 0; j < str.length(); j++) {
         if ('(' == str.charAt(j)) {
            stack.add(str.charAt(j));
         }

         if (')' == str.charAt(j)) {
            stack.pop();
            if (stack.isEmpty()) {
               n = j;
               break;
            }
         }
      }

      return str.substring(0, n + 1);
   }

   public void setModelDB(ModelDBPlatform modelDB) {
      this.modelDB = modelDB;
   }

   public void setFilterDB(FilterInfoDB filterDB) {
      this.filterDB = filterDB;
   }

   public static void main(String[] args) {
      FilterRequest request = (FilterRequest)JSON.parseObject(
         "{\"filter\":[{\"alias\":1,\"type\":\"string\",\"showType\":\"mulsel\",\"key\":\"brand\",\"showName\":\"品牌\",\"opt\":\"in\",\"optName\":\"等于\",\"value\":[\"嵊州\",\"本经\"],\"filterRule\":[],\"dimSelectShow\":false,\"ruleSelectShow\":false,\"textRadioShow\":false}],\"expression\":\"1\"}",
         new TypeReference<FilterRequest>() {},
         new Feature[0]
      );
      FilterAnaylseServiceImpl impl = new FilterAnaylseServiceImpl();

      try {
         System.out.println(impl.fetchSqlByFilterDimension(request));
      } catch (ViePlatformServiceException var4) {
         logger.error("组装成sql出错!");
      }
   }
}
