package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;

public class RuleCheck implements Serializable {
   public void check(Query query) {
      if (query instanceof BooleanQuery) {
         this.checkBooleanQuery((BooleanQuery)query);
      } else if (query instanceof SpanNearQuery) {
         this.checkSpanNearQuery((SpanNearQuery)query);
      } else if (query instanceof TermQuery) {
         return;
      }
   }

   public void checkBooleanQuery(BooleanQuery booleanQuery) {
      BooleanClause[] booleanClauses = booleanQuery.getClauses();
      int clauseCount = booleanClauses.length;
      boolean isOneClause = clauseCount == 1;
      boolean must = false;
      boolean should = false;

      for (int i = 0; i < clauseCount; i++) {
         BooleanClause clause = booleanClauses[i];
         if (!isOneClause && clause.getOccur() == Occur.MUST_NOT) {
            throw new ModelCheckException("请在!前面加括号");
         }

         if (clause.getOccur() == Occur.MUST) {
            must = true;
         }

         if (clause.getOccur() == Occur.SHOULD) {
            should = true;
         }

         if (must && should) {
            throw new ModelCheckException("不能存在连续的|&");
         }

         if (clause.getQuery() instanceof BooleanQuery) {
            this.checkBooleanQuery((BooleanQuery)clause.getQuery());
         } else if (clause.getQuery() instanceof SpanNearQuery) {
            this.checkSpanNearQuery((SpanNearQuery)clause.getQuery());
         } else if (clause.getQuery() instanceof TermQuery) {
         }
      }
   }

   public void checkSpanNearQuery(SpanNearQuery spanNearQuery) {
      SpanQuery[] spanQueries = spanNearQuery.getClauses();
      if (spanQueries.length == 1 && spanQueries[0] instanceof SpanTermQuery) {
         throw new ModelCheckException("不能有嵌套near,或者near的词不能为空！");
      } else {
         for (int i = 0; i < spanQueries.length; i++) {
            Query query = spanQueries[i];
            if ("".equals(query.toString())) {
               throw new ModelCheckException("不能有嵌套near,或者near的词不能为空！");
            }
         }
      }
   }

   public static void checkNearNoBrackets(String rule) {
      Pattern pattern = Pattern.compile("#[a-zA-Z0-9\\u4e00-\\u9fa5]+(&|\\|)+");
      Pattern pattern1 = Pattern.compile("(&|\\|)+[a-zA-Z0-9\\u4e00-\\u9fa5]+#");
      Pattern pattern2 = Pattern.compile("#(\\()+");
      Pattern pattern3 = Pattern.compile("(\\))+#");
      Matcher matcher = pattern.matcher(rule);
      Matcher matcher1 = pattern1.matcher(rule);
      Matcher matcher2 = pattern2.matcher(rule);
      Matcher matcher3 = pattern3.matcher(rule);
      if (matcher.find() || matcher1.find()) {
         throw new ModelCheckException("near外层必须加括号，输入的规则为:" + rule);
      } else if (matcher2.find() || matcher3.find()) {
         throw new ModelCheckExceptionPlatform("near之间不可加括号，输入的规则为:" + rule);
      } else if (checkEnddingNear(rule)) {
         throw new ModelCheckExceptionPlatform("near不可以#结尾，输入的规则为:" + rule);
      }
   }

   public static boolean checkEnddingNear(String rule) {
      boolean flag = false;
      rule = rule.replace("(", "");
      rule = rule.replace(")", "");
      String[] rules = rule.split("\\||\\&");
      if (rules.length > 0) {
         for (String rl : rules) {
            if (rl.endsWith("#")) {
               flag = true;
               break;
            }
         }
      }

      return flag;
   }

   public static void checkNotRule(String rule) {
      Pattern pattern = Pattern.compile("!\\([(a-zA-Z0-9\\u4e00-\\u9fa5)|\\|]+\\)");
      Pattern pattern1 = Pattern.compile("!\\([(a-zA-Z0-9\\u4e00-\\u9fa5)&]+\\)");
      Pattern pattern2 = Pattern.compile("!\\([(a-zA-Z0-9\\u4e00-\\u9fa5#a-zA-Z0-9\\u4e00-\\u9fa5)|\\|]+\\)");
      Pattern pattern3 = Pattern.compile("!\\([(a-zA-Z0-9\\u4e00-\\u9fa5#a-zA-Z0-9\\u4e00-\\u9fa5)&]+\\)");
      Pattern pattern4 = Pattern.compile("!\\([(a-zA-Z0-9\\u4e00-\\u9fa5#a-zA-Z0-9\\u4e00-\\u9fa5#0-9)|\\|]+\\)");
      Pattern pattern5 = Pattern.compile("!\\([(a-zA-Z0-9\\u4e00-\\u9fa5#a-zA-Z0-9\\u4e00-\\u9fa5#0-9)&]+\\)");
      Matcher matcher = pattern.matcher(rule);
      Matcher matcher1 = pattern1.matcher(rule);
      Matcher matcher2 = pattern2.matcher(rule);
      Matcher matcher3 = pattern3.matcher(rule);
      Matcher matcher4 = pattern4.matcher(rule);
      Matcher matcher5 = pattern5.matcher(rule);
      if (!matcher.find() && !matcher1.find() && !matcher2.find() && !matcher3.find() && !matcher4.find() && !matcher5.find()) {
         throw new ModelCheckException("顺序模型片段取非规则错误，输入的规则为:" + rule);
      }
   }

   public static void checkNoOperator(String rule) {
      Pattern pattern = Pattern.compile("[a-zA-Z0-9\\u4e00-\\u9fa5]+\\(+");
      Pattern pattern1 = Pattern.compile("\\)+[a-zA-Z0-9\\u4e00-\\u9fa5]+");
      Matcher matcher = pattern.matcher(rule);
      Matcher matcher1 = pattern1.matcher(rule);
      if (matcher.find() || matcher1.find()) {
         throw new ModelCheckException("多个词组间必须有逻辑运算符，输入的规则为:" + rule);
      }
   }

   public static void main(String[] args) {
      Map<Integer, String> map = new HashMap<>();
      String rule = "(查询#话费)&谢谢&虽然&嗯&我的&套餐&流量&话费";
      ModelParser parser = new ModelParser(null, rule, map);
      parser.parse();
   }
}
