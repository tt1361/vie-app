package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;

public class ParserResultPlatform implements Serializable {
   private static final long serialVersionUID = -7062406997286870687L;
   private int ret = -1;
   private String msg;
   private boolean isShow = false;
   private String showMsg;

   public int getRet() {
      return this.ret;
   }

   public void setRet(int ret) {
      this.ret = ret;
   }

   public String getMsg() {
      return this.msg;
   }

   public void setMsg(String msg) {
      this.msg = msg;
   }

   public boolean isShow() {
      return this.isShow;
   }

   public void setShow(boolean isShow) {
      this.isShow = isShow;
   }

   public String getShowMsg() {
      return this.showMsg;
   }

   public void setShowMsg(String showMsg) {
      this.showMsg = showMsg;
   }
}
