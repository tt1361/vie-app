package com.iflytek.vie.app.provider.demo;

import com.iflytek.vie.app.api.demo.DemoService;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoServiceImpl implements DemoService {
   private static final Logger logger = LoggerFactory.getLogger(DemoServiceImpl.class);

   public String sayHello(String name) throws ViePlatformServiceException {
      System.out.println("====DemoService服务被调用=====hello:" + name);
      return name;
   }
}
