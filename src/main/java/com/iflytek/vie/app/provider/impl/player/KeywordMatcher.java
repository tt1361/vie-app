package com.iflytek.vie.app.provider.impl.player;

import com.google.common.base.Strings;
import com.iflytek.vie.app.pojo.player.TagInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KeywordMatcher {
   public static int ruleNearSpan = 10;

   public static List<TagInfo> match(String[] words, String content) {
      if (Strings.isNullOrEmpty(content)) {
         return new ArrayList<>();
      } else {
         List<TagInfo> kwdList = new ArrayList<>();
         content = content.toLowerCase();

         for (int iw = 0; iw < words.length; iw++) {
            String word = words[iw];
            if (word != null && !word.isEmpty()) {
               if (word.indexOf("#") <= 0) {
                  Pattern pattern = Pattern.compile(toRegexStr(word, 10));
                  Matcher matcher = pattern.matcher(content);

                  while (matcher.find()) {
                     int bos = matcher.start();
                     int eos = matcher.end();
                     if (!judgeHasSameKwd(kwdList, bos, eos)) {
                        kwdList.add(new TagInfo(word, bos, eos));
                     }
                  }
               }

               if (word.indexOf("#") > 0) {
                  String[] temChars = word.split("#");
                  int wordNear = 0;
                  String tempWord = word;

                  try {
                     wordNear = Integer.parseInt(temChars[temChars.length - 1]);
                     tempWord = word.substring(0, word.lastIndexOf("#"));
                  } catch (Exception var16) {
                     wordNear = ruleNearSpan;
                  }

                  Pattern pattern2 = Pattern.compile(toRegexStr(tempWord, wordNear));
                  Matcher matcher2 = pattern2.matcher(content);
                  String[] keyWord = tempWord.split("#");
                  int wordLength = 0;

                  for (String w : keyWord) {
                     wordLength += w.length();
                  }

                  while (matcher2.find()) {
                     int bos = matcher2.start();
                     int eos = matcher2.end();
                     if (eos - bos - wordLength <= wordNear) {
                        kwdList.add(new TagInfo(word, bos, eos));
                     }
                  }
               }
            }
         }

         return kwdList;
      }
   }

   private static boolean judgeHasSameKwd(List<TagInfo> kwdList, int bos, int eos) {
      if (kwdList != null && kwdList.size() > 0) {
         int mun = eos - bos;

         for (TagInfo kwd : kwdList) {
            if (isBetweenNum(bos, kwd.getBeginTime(), mun) || isBetweenNum(bos, kwd.getEndTime(), mun)) {
               return true;
            }
         }
      }

      return false;
   }

   public static String toRegexStr(String str, int span) {
      if (str != null && !str.isEmpty()) {
         str = str.replace("+", "#");
         int pos = str.indexOf("#");
         if (pos > 0) {
            String[] words = str.split("#");
            String nearRule = "";

            for (int i = 0; i < words.length; i++) {
               if (i == 0) {
                  nearRule = words[0] + "((?!" + words[0] + ").){0," + span + "}?";
               } else if (i == words.length - 1) {
                  nearRule = nearRule + words[i];
               } else {
                  nearRule = nearRule + words[i] + ".{0," + span + "}?";
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

   private static boolean isBetweenNum(int firstNum, int secNum, int baseNum) {
      return secNum - firstNum >= 0 && secNum - firstNum <= baseNum;
   }
}
