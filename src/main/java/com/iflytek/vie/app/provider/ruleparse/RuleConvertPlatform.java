package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

public class RuleConvertPlatform implements Serializable {
   private static final long serialVersionUID = 5447701199335344888L;
   private Map<Integer, String> replaceMap;
   private boolean isReplace;

   public RuleConvertPlatform(Map<Integer, String> replaceMap, boolean isReplace) {
      this.replaceMap = replaceMap;
      if (replaceMap != null && replaceMap.size() != 0) {
         this.isReplace = true;
      } else {
         this.isReplace = false;
      }

      this.isReplace = isReplace;
   }

   public void query(Query q, StringBuffer ruleBuffer) {
      if (q instanceof BooleanQuery) {
         this.booleanQuery((BooleanQuery)q, ruleBuffer);
      } else if (q instanceof TermQuery) {
         Term term = ((TermQuery)q).getTerm();
         String termText = term.text();
         if (ruleBuffer.toString().endsWith("( !")) {
            if (this.isReplace) {
               int key = Integer.parseInt(termText);
               if (this.replaceMap.get(key) == null) {
                  throw new ModelCheckExceptionPlatform("该项不存在，[" + key + "]");
               }

               ruleBuffer.append("(" + this.replaceMap.get(key) + "))");
            } else {
               ruleBuffer.append(termText + ")");
            }
         } else if (this.isReplace) {
            int key = Integer.parseInt(termText);
            if (this.replaceMap.get(key) == null) {
               throw new ModelCheckExceptionPlatform("该项不存在，[" + key + "]");
            }

            ruleBuffer.append("(" + this.replaceMap.get(key) + ")");
         } else {
            ruleBuffer.append(termText);
         }
      }
   }

   private void booleanQuery(BooleanQuery q, StringBuffer ruleBuffer) {
      BooleanClause[] clauses = q.getClauses();
      int count = 0;

      for (BooleanClause clause : clauses) {
         if (clause.getOccur() == Occur.MUST_NOT) {
            if (!ruleBuffer.toString().endsWith("|") && !ruleBuffer.toString().endsWith("&") && count != 0) {
               if (clauses[count - 1].getOccur() == Occur.SHOULD) {
                  ruleBuffer.append("|!");
               } else {
                  ruleBuffer.append("&!");
               }
            } else if (count == 0) {
               ruleBuffer.append("!");
            } else {
               ruleBuffer.append("( !");
            }
         }

         if (clause.getQuery() instanceof BooleanQuery) {
            if (count == 0) {
               ruleBuffer.append("(");
            }

            if (count++ > 0) {
               if (clause.getOccur() == Occur.SHOULD) {
                  ruleBuffer.append("|(");
               } else if (clause.getOccur() == Occur.MUST) {
                  ruleBuffer.append("&(");
               } else if (clause.getOccur() == Occur.MUST_NOT) {
                  ruleBuffer.append("(");
               }
            }

            this.booleanQuery((BooleanQuery)clause.getQuery(), ruleBuffer);
            ruleBuffer.append(")");
         } else {
            if (count++ > 0) {
               if (clause.getOccur() == Occur.SHOULD) {
                  ruleBuffer.append("|");
               } else if (clause.getOccur() == Occur.MUST) {
                  List<String> valuesList = new ArrayList<>(this.replaceMap.values());
                  boolean isOrder = false;

                  for (String str : valuesList) {
                     if (str.contains("@")) {
                        isOrder = true;
                        break;
                     }
                  }

                  if (isOrder) {
                     ruleBuffer.append("@");
                  } else {
                     ruleBuffer.append("&");
                  }
               }
            }

            this.query(clause.getQuery(), ruleBuffer);
         }
      }
   }

   public Map<Integer, String> getReplaceMap() {
      return this.replaceMap;
   }

   public void setReplaceMap(Map<Integer, String> replaceMap) {
      this.replaceMap = replaceMap;
   }

   public static void main(String[] args) {
      Map<Integer, String> map = new HashMap<>();
      map.put(1, "111");
      map.put(2, "222");
      map.put(3, "333");
      map.put(4, "444");
      String rule = "(1&(2|3)&!(1|2))|!2";
      new StringBuffer();
      new RuleConvertPlatform(map, true);
   }
}
