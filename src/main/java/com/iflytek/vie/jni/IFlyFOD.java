package com.iflytek.vie.jni;

public class IFlyFOD {
   private String fileAdress;

   public void init() {
      try {
         System.load(this.fileAdress);
      } catch (Exception var2) {
         var2.printStackTrace();
      }
   }

   public native int jFODConfigWrite(String var1);

   public native int jFODTrainModel(String var1, int var2, String var3);

   public native int jFODTrainModelEx(String var1, String var2, int var3, String var4);

   public native int jFODInit(String var1);

   public native int jFODModelFini();

   public native String jFODSearch(String var1, int var2);

   public String getFileAdress() {
      return this.fileAdress;
   }

   public void setFileAdress(String fileAdress) {
      this.fileAdress = fileAdress;
   }
}
