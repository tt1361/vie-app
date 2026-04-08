package com.iflytek.vie.app.provider.ruleparse;

import com.iflytek.vie.app.pojo.model.ModelInfo;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.Version;

public class ModelParserPlatform extends QueryParser implements Serializable {
   private static final long serialVersionUID = 8452556674398947065L;
   private ModelInfo existModel;
   private String inputRules;
   private boolean hasChange;
   private Map<Integer, String> rulesMap;
   private String combineRule;
   private Query query;
   private ParserResultPlatform parserResult;
   private boolean isReplace = true;

   public ModelParserPlatform(ModelInfo existModel, String inputRules, Map<Integer, String> rulesMap) {
      super(Version.LUCENE_4_9, "", new ExactAnalyzerPlatform());
      this.existModel = existModel;
      this.inputRules = inputRules;
      this.rulesMap = rulesMap;
   }

   public ParserResultPlatform parseReplace() {
      StringBuffer ruleBuffer = new StringBuffer();
      RuleConvertPlatform ruleConvert = new RuleConvertPlatform(this.rulesMap, this.isReplace);

      try {
         ruleConvert.query(this.query, ruleBuffer);
         this.combineRule = ruleBuffer.toString();
      } catch (ModelCheckExceptionPlatform var5) {
         var5.printStackTrace();
         ParserResultPlatform parserResult = new ParserResultPlatform();
         parserResult.setRet(0);
         parserResult.setMsg(var5.getMessage());
         return parserResult;
      }

      return new ParserResultPlatform();
   }

   public ParserResultPlatform parse() {
      ParserResultPlatform parserResult = new ParserResultPlatform();
      if (this.inputRules != null) {
         try {
            String replaceRule = this.inputRules.replace("|", " OR ").replace("&", " AND ").replace("!", " NOT ").replace("@", " AND ");
            this.query = this.parse(replaceRule);
            this.myRuleCheck(this.query, this.inputRules);
         } catch (ParseException var7) {
            var7.printStackTrace();
            parserResult.setRet(0);
            parserResult.setMsg("输入规则:[" + this.inputRules + "] 错误");
            this.parserResult = parserResult;
            return parserResult;
         } catch (ModelCheckExceptionPlatform var8) {
            var8.printStackTrace();
            parserResult.setRet(0);
            parserResult.setMsg(var8.getMessage());
            parserResult.setShow(true);
            this.parserResult = parserResult;
            return parserResult;
         }
      }

      if (this.rulesMap == null) {
         this.parserResult = parserResult;
         return parserResult;
      } else {
         for (Entry<Integer, String> entry : this.rulesMap.entrySet()) {
            try {
               String replaceRule = entry.getValue().replace("|", " OR ").replace("&", " AND ").replace("!", " NOT ");
               this.myRuleCheck(this.parse(replaceRule), entry.getValue());
            } catch (ParseException var5) {
               var5.printStackTrace();
               parserResult.setRet(entry.getKey());
               parserResult.setMsg("序号:[" + entry.getKey() + "],规则:[" + entry.getValue() + "] 错误");
               this.parserResult = parserResult;
               return parserResult;
            } catch (ModelCheckExceptionPlatform var6) {
               var6.printStackTrace();
               parserResult.setRet(0);
               parserResult.setMsg(var6.getMessage());
               parserResult.setShow(true);
               this.parserResult = parserResult;
               return parserResult;
            }
         }

         this.parserResult = parserResult;
         return parserResult;
      }
   }

   public void myRuleCheck(Query query, String rule) {
      RuleCheckPlatform ruleCheck = new RuleCheckPlatform();
      ruleCheck.check(query);
      RuleCheckPlatform.checkNearNoBrackets(rule);
      RuleCheckPlatform.checkNoOperator(rule);
   }

   protected Query newFieldQuery(Analyzer analyzer, String field, String queryText, boolean quoted) throws ParseException {
      if (queryText.indexOf("+") == -1 && queryText.indexOf("#") == -1) {
         return this.newTermQuery(new Term(field, queryText));
      } else {
         queryText = queryText.replace("+", "#");
         String[] words = queryText.split("#");
         int nearSpan = -1;
         int wordsCount = 0;

         try {
            nearSpan = Integer.parseInt(words[words.length - 1]);
            wordsCount = words.length - 1;
         } catch (Exception var10) {
            nearSpan = 10;
            wordsCount = words.length;
         }

         SpanQuery[] queries = new SpanQuery[wordsCount];

         for (int i = 0; i < wordsCount; i++) {
            queries[i] = new SpanTermQuery(new Term(field, words[i]));
         }

         return new SpanNearQuery(queries, nearSpan, true);
      }
   }

   public ModelInfo getExistModel() {
      return this.existModel;
   }

   public void setExistModel(ModelInfo existModel) {
      this.existModel = existModel;
   }

   public String getInputRules() {
      return this.inputRules;
   }

   public void setInputRules(String inputRules) {
      this.inputRules = inputRules;
   }

   public boolean isHasChange() {
      return this.hasChange;
   }

   public void setHasChange(boolean hasChange) {
      this.hasChange = hasChange;
   }

   public Map<Integer, String> getRulesMap() {
      return this.rulesMap;
   }

   public void setRulesMap(Map<Integer, String> rulesMap) {
      this.rulesMap = rulesMap;
   }

   public String getCombineRule() {
      return this.combineRule;
   }

   public void setCombineRule(String combineRule) {
      this.combineRule = combineRule;
   }

   public ParserResultPlatform getParserResult() {
      return this.parserResult;
   }

   public void setParserResult(ParserResultPlatform parserResult) {
      this.parserResult = parserResult;
   }

   public Query getQuery() {
      return this.query;
   }

   public void setQuery(Query query) {
      this.query = query;
   }

   public boolean isReplace() {
      return this.isReplace;
   }

   public void setReplace(boolean isReplace) {
      this.isReplace = isReplace;
   }

   public static void main(String[] args) {
      Map<Integer, String> map = new HashMap<>();
      map.put(2, "你好");
      map.put(3, "3&3");
      map.put(4, "4&4");
      String rule = "1&(1|2)&((2#3#3#10)|(!2))";
      ModelParserPlatform parser = new ModelParserPlatform(null, rule, map);
      parser.parse();
      ParserResultPlatform p = parser.parseReplace();
      String reRule = parser.getCombineRule();
      System.out.println(reRule);
      System.out.println(new ModelParserPlatform(null, reRule, null).parse().getMsg());
   }
}
