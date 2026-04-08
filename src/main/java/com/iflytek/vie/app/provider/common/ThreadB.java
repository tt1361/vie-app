package com.iflytek.vie.app.provider.common;

import java.util.HashMap;
import java.util.Map;

class ThreadB extends Thread {
   private HttpHelper http;

   public ThreadB(HttpHelper http) {
      this.http = http;
   }

   @Override
   public void run() {
      Map<String, String> params = new HashMap<>();
      long l1 = System.currentTimeMillis();
      params.put("userId", "133");
      params.put("contextPath", "/VIEWEB");
      String response = this.http.get("http://192.168.78.75:8888/permission/rest/userAuth/getFuncAuths", "UTF-8", params);
      System.out.println("Thread id = " + this.getName() + "    回应结果长度:" + response.length() + "    查询耗时" + (System.currentTimeMillis() - l1));
   }
}
