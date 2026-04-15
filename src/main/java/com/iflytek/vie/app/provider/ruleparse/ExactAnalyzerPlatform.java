package com.iflytek.vie.app.provider.ruleparse;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

public class ExactAnalyzerPlatform extends Analyzer implements Serializable {
   private static final long serialVersionUID = -4738574905828113676L;

   protected TokenStreamComponents createComponents(String s, Reader reader) {
      return new TokenStreamComponents(new ExactAnalyzerPlatform.ExactTokenizer(reader));
   }

   public static List<String> analyze(String str) {
      List<String> wordList = new ArrayList<>();

      try {
         ExactAnalyzerPlatform.ExactTokenizer tokenizer = new ExactAnalyzerPlatform.ExactTokenizer(new StringReader(str));
         CharTermAttribute termTextAttr = (CharTermAttribute)tokenizer.getAttribute(CharTermAttribute.class);

         while (tokenizer.incrementToken()) {
            String word = termTextAttr.toString();
            wordList.add(word);
         }
      } catch (Exception var5) {
      }

      return wordList;
   }

   static class ExactTokenizer extends Tokenizer {
      StringBuilder buffer = new StringBuilder(1024);
      private CharTermAttribute termAttr;
      private OffsetAttribute offsetAttr;
      private TypeAttribute typeAttr;

      public ExactTokenizer(Reader input) {
         super(input);

         try {
            this.reset();
         } catch (IOException var3) {
            var3.printStackTrace();
         }

         this.offsetAttr = (OffsetAttribute)this.addAttribute(OffsetAttribute.class);
         this.termAttr = (CharTermAttribute)this.addAttribute(CharTermAttribute.class);
         this.typeAttr = (TypeAttribute)this.addAttribute(TypeAttribute.class);
      }

      public boolean incrementToken() throws IOException {
         this.clearAttributes();
         if (this.buffer.length() > 0) {
            this.buffer.delete(0, this.buffer.length());
         }

         int a = -1;
         if (-1 != (a = this.input.read())) {
            this.buffer.append((char)a);
            this.termAttr.setEmpty().append(this.buffer.toString().toLowerCase());
            this.offsetAttr.setOffset(0, this.input.toString().length());
            this.typeAttr.setType("char");
            return true;
         } else {
            return false;
         }
      }

      protected boolean isTokenChar(int c) {
         String regexStr = "[\\S]";
         Pattern pattern = Pattern.compile(regexStr);
         char ch = (char)c;
         Matcher matcher = pattern.matcher(String.valueOf(ch));
         return matcher.matches();
      }
   }
}
