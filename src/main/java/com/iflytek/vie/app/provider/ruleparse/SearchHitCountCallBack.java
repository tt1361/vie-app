package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;

public class SearchHitCountCallBack extends ProcessCallBack implements Serializable {
   private static final long serialVersionUID = -2662258188646496421L;

   @Override
   public void onSuccess(Object o) {
      System.out.println("success");
   }

   @Override
   public void onFailure(Throwable throwable) {
      System.out.println("fail");
   }
}
