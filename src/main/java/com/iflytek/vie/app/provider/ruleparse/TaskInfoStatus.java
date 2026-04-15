package com.iflytek.vie.app.provider.ruleparse;

public enum TaskInfoStatus {
   OnLineFail(-4, "上线失败"),
   OffLineTask(-3, "已下线"),
   OptimizeTask(-2, "优化中"),
   GoLineTask(-1, "已上线"),
   OnLineTask(0, "上线中"),
   AnydrillConnectFail(2, "anydrill地址连接不上"),
   AnydrillDataQueryFail(3, "anydrill查询数据失败"),
   Other(4, "anydrill查询数据失败");

   private int taskStatus;
   private String errorReason;

   private TaskInfoStatus(int taskStatus, String errorReason) {
      this.taskStatus = taskStatus;
      this.errorReason = errorReason;
   }

   public static TaskInfoStatus getTaskStatus(int code) {
      switch (code) {
         case -4:
            return OnLineFail;
         case -3:
            return OffLineTask;
         case -2:
            return OptimizeTask;
         case -1:
            return GoLineTask;
         case 0:
            return OnLineTask;
         case 1:
         default:
            return null;
         case 2:
            return AnydrillConnectFail;
         case 3:
            return AnydrillDataQueryFail;
         case 4:
            return Other;
      }
   }

   public static int getStatusByObj(Object obj) {
      int objInt = Integer.parseInt(String.valueOf(obj));
      switch (objInt) {
         case 1:
            return OffLineTask.getTaskStatus();
         default:
            return OptimizeTask.getTaskStatus();
      }
   }

   public int getTaskStatus() {
      return this.taskStatus;
   }

   public String getErrorReason() {
      return this.errorReason;
   }
}
