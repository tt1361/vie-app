package com.iflytek.vie.app.provider.ruleparse.parse;

import java.util.ArrayList;

public class TextRuleParse {
   public String rule;
   public String[] words;
   public String syntax;

   public TextRuleParse(String ruleStr) {
      this.rule = this.normalize(ruleStr);
      this.syntax = this.toQuerySyntax(this.rule);
      this.words = toWordsList(this.rule);
   }

   private String normalize(String ruleStr) {
      String tmp = ruleStr.toLowerCase();
      tmp = tmp.trim();
      return tmp.replaceAll(" ", "");
   }

   public static String[] toWordsList(String str) {
      ArrayList<String> wordsList = new ArrayList<>();
      String[] tmp = str.split("\\(|\\)|\\||!|&");

      for (String item : tmp) {
         if (item.contains("#")) {
            wordsList.add(item);
         } else if (!item.replace(" ", "").equals("")) {
            wordsList.add(item.replace(" ", ""));
         }
      }

      String[] words = new String[wordsList.size()];
      return wordsList.toArray(words);
   }

   public String toRegexStr(String str, int span, boolean isReverse) {
      if (str != null && !str.isEmpty()) {
         str = str.replace("+", "#");
         int pos = str.indexOf("#");
         if (pos > 0) {
            String[] words = str.split("#");
            String nearRule = "";

            for (int i = 0; i < words.length; i++) {
               if (i == 0) {
                  nearRule = words[0] + ".{0," + span + "}";
               } else if (i == words.length - 1) {
                  nearRule = nearRule + words[i];
               } else {
                  nearRule = nearRule + words[i] + ".{0," + span + "}";
               }
            }

            return nearRule;
         } else {
            return str;
         }
      } else {
         return "";
      }
   }

   public String toQuerySyntax(String str) {
      StringBuffer syntax = new StringBuffer(1024);
      syntax.append("(");

      for (int i = 0; i < str.length(); i++) {
         char a = str.charAt(i);
         if (a == '|') {
            syntax.append(" OR ");
         } else if (a == '&') {
            syntax.append(" AND ");
         } else if (a == '!') {
            syntax.append(" NOT ");
         } else {
            syntax.append(a);
         }
      }

      syntax.append(")");
      return syntax.toString();
   }
}
