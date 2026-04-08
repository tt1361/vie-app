package com.iflytek.vie.app.provider.ruleparse;

import com.google.common.util.concurrent.FutureCallback;
import java.io.Serializable;

public class ProcessCallBack implements FutureCallback, Serializable {
   private static final long serialVersionUID = -2741447327616568618L;

   public void onSuccess(Object o) {
      System.out.println("success");
   }

   public void onFailure(Throwable throwable) {
      System.out.println("fail");
   }
}
