package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MyThreadFactory implements ThreadFactory, Serializable {
   private static final long serialVersionUID = -7763648040236643736L;
   final ThreadGroup group;
   final AtomicInteger threadNumber = new AtomicInteger(1);
   final String namePrefix;
   private boolean isDaemon;

   public MyThreadFactory(String namePrefix) {
      this.namePrefix = namePrefix;
      SecurityManager s = System.getSecurityManager();
      this.group = s != null ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
   }

   @Override
   public Thread newThread(Runnable r) {
      Thread t = new Thread(this.group, r, this.namePrefix + "[T#" + this.threadNumber.getAndIncrement() + "]", 0L);
      t.setDaemon(this.isDaemon);
      return t;
   }

   public boolean isDaemon() {
      return this.isDaemon;
   }

   public void setDaemon(boolean isDaemon) {
      this.isDaemon = isDaemon;
   }
}
