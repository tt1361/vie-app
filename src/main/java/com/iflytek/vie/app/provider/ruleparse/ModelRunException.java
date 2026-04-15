package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;

public class ModelRunException extends RuntimeException implements Serializable {
   private static final long serialVersionUID = -4434109738777097936L;

   public ModelRunException() {
   }

   public ModelRunException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
   }

   public ModelRunException(Throwable cause) {
      super(cause);
   }

   public ModelRunException(String message, Throwable cause) {
      super(message, cause);
   }

   public ModelRunException(String message) {
      super(message);
   }
}
