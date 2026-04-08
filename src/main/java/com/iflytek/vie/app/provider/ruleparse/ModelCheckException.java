package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;

public class ModelCheckException extends RuntimeException implements Serializable {
   public ModelCheckException() {
   }

   public ModelCheckException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }

   public ModelCheckException(Throwable cause) {
      super(cause);
   }

   public ModelCheckException(String message, Throwable cause) {
      super(message, cause);
   }

   public ModelCheckException(String message) {
      super(message);
   }
}
