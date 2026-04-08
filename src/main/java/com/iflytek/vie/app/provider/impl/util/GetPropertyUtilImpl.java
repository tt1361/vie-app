package com.iflytek.vie.app.provider.impl.util;

import com.iflytek.vie.app.api.utils.GetPropertyUtil;
import com.iflytek.vie.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class GetPropertyUtilImpl implements GetPropertyUtil {
   @Autowired
   private Environment env;

   public String getProperty(String property) {
      String value = this.env.getProperty(property);
      return StringUtils.isNullOrEmpry(value) ? null : value;
   }
}
