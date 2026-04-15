package com.iflytek.vie.app.provider.common;

import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioInfoPlatform {
   private final Logger logger = LoggerFactory.getLogger(AudioInfoPlatform.class);
   private String contentOrigin;
   private String channelSeq;
   private String timePosition;
   private String oneBest = "";
   private String timePositionStr = "";
   private String trueOnebestTime = "";
   private String voiceIndex = "";
   private int currentIndex = 1;

   public AudioInfoPlatform() {
   }

   public AudioInfoPlatform(String contentOrigin, String channelSeq, String timePosition) {
      this.contentOrigin = contentOrigin;
      this.channelSeq = channelSeq;
      this.timePosition = timePosition;
   }

   public void process() {
      String[] timePositions = this.timePosition.split(" ");
      String[] channels = this.channelSeq.split(" ");
      String[] oneBestKeyword = this.contentOrigin.split(" ");
      this.fetchInfo(oneBestKeyword, timePositions, channels);
   }

   private void fetchInfo(String[] oneBestKeyword, String[] timePositions, String[] channels) {
      try {
         int count1 = 0;
         int count2 = 0;

         for (int i = 0; i < oneBestKeyword.length; i++) {
            if (!oneBestKeyword[i].matches(CommonParams.contentRGX)) {
               if (channels[i].compareTo("0") == 0) {
                  count1++;
                  count2 = 0;
                  if (count1 == 1) {
                     if (!StringUtils.isAbsEmpry(this.oneBest)) {
                        this.oneBest = this.oneBest + "\n";
                     }

                     this.oneBest = this.oneBest + "坐席： " + oneBestKeyword[i] + " ";
                     this.timePositionStr = this.timePositionStr + timePositions[i].split("\\|")[0] + " ";
                     this.trueOnebestTime = this.trueOnebestTime + timePositions[i].split("\\|")[0] + " ";
                     this.voiceIndex = this.voiceIndex + this.currentIndex + " ";
                     this.voiceIndex = this.voiceIndex + this.currentIndex + " ";
                  } else {
                     this.oneBest = this.oneBest + oneBestKeyword[i] + " ";
                     this.voiceIndex = this.voiceIndex + this.currentIndex + " ";
                  }
               }

               if (channels[i].compareTo("1") == 0) {
                  count2++;
                  count1 = 0;
                  if (count2 == 1) {
                     if (!StringUtils.isAbsEmpry(this.oneBest)) {
                        this.oneBest = this.oneBest + "\n";
                     }

                     this.oneBest = this.oneBest + "客户： " + oneBestKeyword[i] + " ";
                     this.timePositionStr = this.timePositionStr + timePositions[i].split("\\|")[0] + " ";
                     this.trueOnebestTime = this.trueOnebestTime + timePositions[i].split("\\|")[0] + " ";
                     this.voiceIndex = this.voiceIndex + this.currentIndex + " ";
                     this.voiceIndex = this.voiceIndex + this.currentIndex + " ";
                  } else {
                     this.oneBest = this.oneBest + oneBestKeyword[i] + " ";
                     this.voiceIndex = this.voiceIndex + this.currentIndex + " ";
                  }
               }

               this.timePositionStr = this.timePositionStr + timePositions[i].split("\\|")[0] + " ";
               this.trueOnebestTime = this.trueOnebestTime + timePositions[i].split("\\|")[0] + " ";
            }
         }
      } catch (Exception var7) {
         this.logger.error("method fetchInfo exception");
      }
   }

   public List<LinkedHashMap<String, Object>> fetchTextInfo(String contentOrigin, String channelSeq, String timePosition) {
      List<LinkedHashMap<String, Object>> dialogList = new ArrayList<>();
      if (contentOrigin == null) {
         System.out.println("处理结果为空!");
         return null;
      } else {
         String[] channels = channelSeq.split(" ");
         String[] contents = contentOrigin.split(" ");
         String[] times = timePosition.split(" ");
         if (channels.length != contents.length) {
            System.out.println("对话文本有问题");
         }

         new LinkedHashMap();
         String cur = channels[0];
         String dialog = "";
         String ti = "";
         if (contents[0].matches(CommonParams.contentRGX)) {
            dialog = "";
            ti = "";
         } else {
            dialog = contents[0] + " ";
            ti = times[0].split("\\|")[0] + " ";
         }

         for (int i = 1; i < channels.length; i++) {
            if (contents[i].matches(CommonParams.contentRGX)) {
               if (contents[i].matches("VE")) {
                  LinkedHashMap<String, Object> each = new LinkedHashMap();
                  each.put("channel", cur);
                  each.put("content", dialog.trim());
                  each.put("time", ti.trim());
                  dialogList.add(each);
               }
            } else if (channels[i].equals(cur) && i != channels.length - 1) {
               dialog = dialog + contents[i] + " ";
               ti = ti + times[i].split("\\|")[0] + " ";
            } else {
               LinkedHashMap<String, Object> var13 = new LinkedHashMap();
               var13.put("channel", cur);
               var13.put("content", dialog.trim());
               var13.put("time", ti.trim());
               dialogList.add(var13);
               dialog = contents[i] + " ";
               cur = channels[i];
               ti = times[i].split("\\|")[0] + " ";
            }
         }

         return dialogList;
      }
   }

   public String getContentOrigin() {
      return this.contentOrigin;
   }

   public void setContentOrigin(String contentOrigin) {
      this.contentOrigin = contentOrigin;
   }

   public String getChannelSeq() {
      return this.channelSeq;
   }

   public void setChannelSeq(String channelSeq) {
      this.channelSeq = channelSeq;
   }

   public String getTimePosition() {
      return this.timePosition;
   }

   public void setTimePosition(String timePosition) {
      this.timePosition = timePosition;
   }

   public String getOneBest() {
      return this.oneBest;
   }

   public void setOneBest(String oneBest) {
      this.oneBest = oneBest;
   }

   public String getTimePositionStr() {
      return this.timePositionStr;
   }

   public void setTimePositionStr(String timePositionStr) {
      this.timePositionStr = timePositionStr;
   }

   public String getTrueOnebestTime() {
      return this.trueOnebestTime;
   }

   public void setTrueOnebestTime(String trueOnebestTime) {
      this.trueOnebestTime = trueOnebestTime;
   }

   public String getVoiceIndex() {
      return this.voiceIndex;
   }

   public void setVoiceIndex(String voiceIndex) {
      this.voiceIndex = voiceIndex;
   }

   public void setCurrentIndex(int currentIndex) {
      this.currentIndex = currentIndex;
   }

   public int getCurrentIndex() {
      return this.currentIndex;
   }
}
