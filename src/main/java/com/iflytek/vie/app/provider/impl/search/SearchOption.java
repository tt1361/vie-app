package com.iflytek.vie.app.provider.impl.search;

import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchOption {
   protected HashMap<String, String> switchColMap = new HashMap<>();
   protected List<String> andWordList = new ArrayList<>();
   protected List<String> orWordList = new ArrayList<>();
   protected List<String> fullContent = new ArrayList<>();
   protected List<String> notWordList = new ArrayList<>();
   protected String errorMsg = "";
   protected String optStr = "\"-(";
   protected String tableName = "";
   protected int pageNum;
   protected int pageSize;
   protected int chanel = 2;
   protected String dimensionCond = "";
   protected Map<String, String> selCol;
   protected LinkedHashMap<String, DimensionConfig> dimensionMap;
   protected String switchChar = "@@";
   protected int searchType = 1;
   protected static final String errorInfo = "文本规则格式错误";

   protected void contentParse(String content) throws VieAppServiceException {
      try {
         if (!StringUtils.isNullOrEmpry(content)) {
            content = this.switchContent(content);
            String[] cotentArray = content.split(" ");

            for (String ckey : cotentArray) {
               if (!StringUtils.isNullOrEmpry(ckey)) {
                  char[] chars = ckey.toCharArray();
                  char opt = chars[0];
                  if (this.optStr.indexOf(opt) != -1) {
                     if ("\"".equals(opt + "")) {
                        this.fullContent.add(this.filterSpecialWord(ckey.replaceAll(this.switchChar, "")));
                     } else if (!"(".equals(opt + "") && !"-".equals(opt + "")) {
                        this.errorMsg = "文本规则格式错误";
                     } else {
                        String[] array = ckey.split("\\|");

                        for (int i = 0; i < array.length; i++) {
                           if ("(".equals(opt + "")) {
                              this.orWordList.add(this.filterSpecialWord(array[i].replaceAll(this.switchChar, "")));
                           } else {
                              this.notWordList.add(this.filterSpecialWord(array[i].replaceAll(this.switchChar, "")));
                           }
                        }
                     }
                  } else if (!ckey.endsWith("|") && !ckey.startsWith("|")) {
                     this.andWordList.add(this.filterSpecialWord(ckey));
                  } else {
                     this.errorMsg = "文本规则格式错误";
                  }
               }
            }
         }
      } catch (Exception var11) {
         throw new VieAppServiceException(1002, "文本规则格式错误");
      }
   }

   private String filterSpecialWord(String key) {
      if (StringUtils.isNotNullAndEmpry(key)) {
         key = key.replace("\"", "").replace("(", "").replace(")", "").replace("-", "").replace("+", "");
         if (StringUtils.isNullOrEmpry(key)) {
            this.errorMsg = "文本规则格式错误";
         }
      } else {
         this.errorMsg = "文本规则格式错误";
      }

      return key;
   }

   private String switchContent(String content) {
      String[] patterns = new String[]{"\\([^)]+\\(", "\\)[^(]+\\)", "\\([^()]+\\)", "\"(.*?)\""};

      for (int i = 0; i < patterns.length; i++) {
         Matcher matcher = Pattern.compile(patterns[i]).matcher(content);

         while (matcher.find()) {
            content = content.replace(matcher.group(), matcher.group().replaceAll(" ", this.switchChar));
         }
      }

      return content.toString();
   }

   public String getTableName() {
      return this.tableName;
   }

   public void setTableName(String tableName) {
      this.tableName = tableName;
   }

   public String getErrorMsg() {
      return this.errorMsg;
   }

   public void setErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
   }

   public HashMap<String, String> getSwitchColMap() {
      return this.switchColMap;
   }

   public int getChanel() {
      return this.chanel;
   }
}
