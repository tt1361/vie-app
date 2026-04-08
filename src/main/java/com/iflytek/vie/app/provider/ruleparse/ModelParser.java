package com.iflytek.vie.app.provider.ruleparse;

import com.iflytek.vie.app.pojo.model.ModelInfo;
import java.io.Serializable;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelParser extends QueryParser implements Serializable {
   private static final long serialVersionUID = 9125797205031503183L;
   private static final Logger logger = LoggerFactory.getLogger(ModelParser.class);
   private ModelInfo existModel;
   private String inputRules;
   private boolean hasChange;
   private Map<Integer, String> rulesMap;
   private String combineRule;
   private Query query;
   private ParserResult parserResult;
   private boolean isReplace = true;

   public ModelParser(ModelInfo existModel, String inputRules, Map<Integer, String> rulesMap) {
      super(Version.LUCENE_4_9, "", new ExactAnalyzer());
      this.existModel = existModel;
      this.inputRules = inputRules;
      this.rulesMap = rulesMap;
   }

   public ParserResult parseReplace() {
      StringBuffer ruleBuffer = new StringBuffer();
      RuleConvert ruleConvert = new RuleConvert(this.rulesMap, this.isReplace);

      try {
         ruleConvert.query(this.query, ruleBuffer);
         this.combineRule = ruleBuffer.toString();
      } catch (ModelCheckException var5) {
         logger.error("[parseReplace]出现异常", var5);
         ParserResult parserResult = new ParserResult();
         parserResult.setRet(0);
         parserResult.setMsg(var5.getMessage());
         return parserResult;
      }

      return new ParserResult();
   }

   public ParserResult parse() {
      ParserResult parserResult = new ParserResult();
      if (this.inputRules != null) {
         try {
            String replaceRule = this.inputRules.replace("|", " OR ").replace("&", " AND ").replace("!", " NOT ").replace("@", " AND ");
            this.query = this.parse(replaceRule);
            this.myRuleCheck(this.query, this.inputRules);
         } catch (ParseException var7) {
            logger.error("[parse]出现异常", var7);
            parserResult.setRet(0);
            parserResult.setMsg("输入规则:[" + this.inputRules + "] 错误");
            this.parserResult = parserResult;
            return parserResult;
         } catch (ModelCheckException var8) {
            logger.error("[parse]出现异常", var8);
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
               logger.error("[parse]出现异常", var5);
               parserResult.setRet(entry.getKey());
               parserResult.setMsg("序号:[" + entry.getKey() + "],规则:[" + entry.getValue() + "] 错误");
               this.parserResult = parserResult;
               return parserResult;
            } catch (ModelCheckException var6) {
               logger.error("[parse]出现异常", var6);
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
      RuleCheck ruleCheck = new RuleCheck();
      ruleCheck.check(query);
      RuleCheck.checkNearNoBrackets(rule);
      RuleCheck.checkNoOperator(rule);
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

   public ParserResult getParserResult() {
      return this.parserResult;
   }

   public void setParserResult(ParserResult parserResult) {
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
}
