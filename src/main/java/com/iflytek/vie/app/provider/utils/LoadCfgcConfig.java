package com.iflytek.vie.app.provider.utils;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class LoadCfgcConfig implements EnvironmentAware {
   public static Environment env;

   public static String get(String key) {
      return env.getProperty(key);
   }

   public void setEnvironment(Environment env) {
      LoadCfgcConfig.env = env;
   }
}
