package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;

public class ConfigAction implements Serializable {
   private static final long serialVersionUID = -7324515119905114865L;
   private int fragmentPoolSize = 5;
   private int fragmentQueneSize = 10000;

   public void init() {
      ThreadPoolContain.searchHitThreadPool = new MyThreadPool("searchFragmentHitCount", this.fragmentPoolSize, this.fragmentQueneSize);
   }

   public int getFragmentPoolSize() {
      return this.fragmentPoolSize;
   }

   public void setFragmentPoolSize(int fragmentPoolSize) {
      this.fragmentPoolSize = fragmentPoolSize;
   }

   public int getFragmentQueneSize() {
      return this.fragmentQueneSize;
   }

   public void setFragmentQueneSize(int fragmentQueneSize) {
      this.fragmentQueneSize = fragmentQueneSize;
   }
}
