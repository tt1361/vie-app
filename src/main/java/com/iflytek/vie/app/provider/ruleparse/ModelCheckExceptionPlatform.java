package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;

public class ModelCheckExceptionPlatform extends RuntimeException implements Serializable {
   private static final long serialVersionUID = 3734693049795697302L;

   public ModelCheckExceptionPlatform() {
   }

   public ModelCheckExceptionPlatform(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }

   public ModelCheckExceptionPlatform(Throwable cause) {
      super(cause);
   }

   public ModelCheckExceptionPlatform(String message, Throwable cause) {
      super(message, cause);
   }

   public ModelCheckExceptionPlatform(String message) {
      super(message);
   }
}
