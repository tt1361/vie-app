package com.iflytek.vie.app.provider.ruleparse;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;

public class MyThreadPool implements Serializable {
   private static final long serialVersionUID = 3823020744766175107L;
   private String poolName = "platform-dic";
   private int queueSize = 5;
   private int poolSize = 3;
   private ListeningExecutorService executorService = null;
   private boolean isDaemon = true;

   public MyThreadPool() {
      this.executorService = this.newExecutor();
   }

   public MyThreadPool(String poolName, int poolSize, int queueSize) {
      this.queueSize = queueSize;
      this.poolSize = poolSize;
      this.poolName = poolName;
      this.executorService = this.newExecutor();
   }

   private ListeningExecutorService newExecutor() {
      BlockingQueue<Runnable> queue;
      if (this.queueSize < 0) {
         queue = new LinkedTransferQueue<>();
      } else {
         queue = new SizeBlockingQueue<Runnable>(new LinkedTransferQueue<Runnable>(), this.queueSize);
      }

      ExecutorService executorService = new MyThreadPoolExecutor(
         this.poolSize, this.poolSize, 0L, TimeUnit.MILLISECONDS, queue, new MyThreadFactory(this.poolName)
      );
      return MoreExecutors.listeningDecorator(executorService);
   }

   public void submitTask(RunProcess runProcess) {
      this.executorService.submit(runProcess);
   }

   public void submitTask(RunProcess runProcess, ProcessCallBack callback) {
      ListenableFuture listenableFuture = this.executorService.submit(runProcess);
      Futures.addCallback(listenableFuture, callback);
   }

   public boolean isDaemon() {
      return this.isDaemon;
   }

   public void setDaemon(boolean isDaemon) {
      this.isDaemon = isDaemon;
   }
}
