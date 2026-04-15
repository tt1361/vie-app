package com.iflytek.vie.app.provider.utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CountDbTimeAspect {
   private static final Logger logger = LoggerFactory.getLogger(CountDbTimeAspect.class);
   private String timeout;

   public Object doAround(ProceedingJoinPoint process) throws Throwable {
      long time = System.currentTimeMillis();
      Object obj = null;

      try {
         obj = process.proceed();
      } catch (Exception var6) {
         logger.error("统计数据库操作切面错误", var6);
      }

      time = System.currentTimeMillis() - time;
      if (time >= Integer.valueOf(this.timeout).intValue()) {
         logger.info("--------->>" + process.getSignature() + " 查询数据库超时:time:" + time + " ms");
      }

      return obj;
   }

   public String getTimeout() {
      return this.timeout;
   }

   public void setTimeout(String timeout) {
      this.timeout = timeout;
   }
}
