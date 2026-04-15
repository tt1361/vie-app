package com.iflytek.vie.app.provider.common;

import java.security.MessageDigest;

public class MD5Util {
   public static String md5(String s) throws Exception {
      char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
      byte[] btInput = s.getBytes();
      MessageDigest mdInst = MessageDigest.getInstance("MD5");
      mdInst.update(btInput);
      byte[] md = mdInst.digest();
      int j = md.length;
      char[] str = new char[j * 2];
      int k = 0;

      for (int i = 0; i < j; i++) {
         byte byte0 = md[i];
         str[k++] = hexDigits[byte0 >>> 4 & 15];
         str[k++] = hexDigits[byte0 & 15];
      }

      return new String(str);
   }
}
