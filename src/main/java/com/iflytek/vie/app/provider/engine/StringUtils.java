package com.iflytek.vie.app.provider.engine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
   public static boolean isNullOrEmpry(String str) {
      return str == null || "".equals(str);
   }

   public static boolean isAbsEmpry(String str) {
      return str == null ? true : str.trim().isEmpty();
   }

   public static boolean checkStr(String str) {
      String regEx = "[`~!@#$%^&*<>]";
      Pattern p = Pattern.compile(regEx);
      Matcher m = p.matcher(str);
      return m.find();
   }
}
