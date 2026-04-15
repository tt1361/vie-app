package com.iflytek.vie.app.provider.impl.player;

import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.player.DialogTextResponse;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;

public class AudioInfo {
   private String contentOrigin;
   private String channelSeq;
   private String timePosition;
   private String oneBest = "";
   private String timePositionStr = "";
   private String trueOnebestTime = "";
   private String voiceIndex = "";
   private int currentIndex = 1;

   public AudioInfo() {
   }

   public AudioInfo(String contentOrigin, String channelSeq, String timePosition) {
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
         var7.printStackTrace();
      }
   }

   public List<DialogTextResponse> fetchTextInfo_ByVoice(String contentOrigin, String channelSeq, String timePosition) throws ViePlatformServiceException {
      List<DialogTextResponse> dialogList = new ArrayList<>();
      if (contentOrigin == null) {
         System.out.println("处理结果为空!");
         return null;
      } else {
         String[] channels = channelSeq.split(" ");
         String[] contents = contentOrigin.split(" ");
         String[] times = timePosition.split(" ");
         if (channels.length != contents.length) {
            System.out.println("对话文本有问题");
            throw new ViePlatformServiceException("对话文本格式有问题");
         } else {
            String[] voiceTimes = new String[times.length];

            for (int j = 0; j < times.length; j++) {
               voiceTimes[j] = times[j].split("\\|")[0];
            }

            DialogTextResponse each = null;
            String cur = channels[0];
            String dialog = "";
            String ti = "";
            if (contents[0].matches(CommonParams.contentRGX)) {
               dialog = "";
               ti = "";
            } else {
               dialog = contents[0] + " ";
               ti = voiceTimes[0] + " ";
            }

            for (int i = 1; i < channels.length; i++) {
               if ("VE".equals(contents[i])) {
                  each = new DialogTextResponse();
                  each.setChannel(this.talkName(cur));
                  each.setContent(dialog.trim());
                  each.setTime(ti.trim());
                  dialogList.add(each);
               } else if (!contents[i].matches(CommonParams.contentRGX)) {
                  if (channels[i].equals(cur) && i != channels.length - 1) {
                     dialog = dialog + contents[i] + " ";
                     ti = ti + voiceTimes[i] + " ";
                  } else if (i == channels.length - 1) {
                     if (!channels[i].equals(cur)) {
                        each = new DialogTextResponse();
                        each.setChannel(this.talkName(cur));
                        each.setContent(dialog.trim());
                        each.setTime(ti.trim());
                        dialogList.add(each);
                        dialog = contents[i] + " ";
                        cur = channels[i];
                        ti = voiceTimes[i] + " ";
                     } else {
                        dialog = dialog + contents[i] + " ";
                        ti = ti + voiceTimes[i] + " ";
                     }

                     each = new DialogTextResponse();
                     each.setChannel(this.talkName(cur));
                     each.setContent(dialog.trim());
                     each.setTime(ti.trim());
                     dialogList.add(each);
                  } else {
                     each = new DialogTextResponse();
                     each.setChannel(this.talkName(cur));
                     each.setContent(dialog.trim());
                     each.setTime(ti.trim());
                     dialogList.add(each);
                     dialog = contents[i] + " ";
                     cur = channels[i];
                     ti = voiceTimes[i] + " ";
                  }
               }
            }

            return dialogList;
         }
      }
   }

   public List<DialogTextResponse> fetchTextInfo_ByTask(String contentOrigin, String channelSeq, String timePosition, String childVoiceId) throws ViePlatformServiceException {
      List<DialogTextResponse> dialogList = new ArrayList<>();
      if (contentOrigin == null) {
         System.out.println("处理结果为空!");
         return null;
      } else {
         String[] channels = channelSeq.split(" ");
         String[] contents = contentOrigin.split(" ");
         String[] times = timePosition.split(" ");
         if (channels.length != contents.length) {
            System.out.println("对话文本有问题");
            throw new ViePlatformServiceException("对话文本格式有问题");
         } else {
            String[] voiceTimes = new String[times.length];

            for (int j = 0; j < times.length; j++) {
               voiceTimes[j] = times[j].split("\\|")[0];
            }

            DialogTextResponse each = null;
            String cur = channels[0];
            String dialog = "";
            String ti = "";
            if (contents[0].matches(CommonParams.contentRGX)) {
               dialog = "";
               ti = "";
            } else {
               dialog = contents[0] + " ";
               ti = voiceTimes[0] + " ";
            }

            for (int i = 1; i < channels.length; i++) {
               if ("VE".equals(contents[i])) {
                  each = new DialogTextResponse();
                  each.setChannel(this.talkName(cur));
                  each.setContent(dialog.trim());
                  each.setTime(ti.trim());
                  each.setChildVoiceId(childVoiceId);
                  dialogList.add(each);
               } else if (!contents[i].matches(CommonParams.contentRGX)) {
                  if (channels[i].equals(cur) && i != channels.length - 1) {
                     dialog = dialog + contents[i] + " ";
                     ti = ti + voiceTimes[i] + " ";
                  } else if (i == channels.length - 1) {
                     if (!channels[i].equals(cur)) {
                        each = new DialogTextResponse();
                        each.setChannel(this.talkName(cur));
                        each.setContent(dialog.trim());
                        each.setTime(ti.trim());
                        dialogList.add(each);
                        dialog = contents[i] + " ";
                        cur = channels[i];
                        ti = voiceTimes[i] + " ";
                     } else {
                        dialog = dialog + contents[i] + " ";
                        ti = ti + voiceTimes[i] + " ";
                     }

                     each = new DialogTextResponse();
                     each.setChannel(this.talkName(cur));
                     each.setContent(dialog.trim());
                     each.setTime(ti.trim());
                     dialogList.add(each);
                  } else {
                     each = new DialogTextResponse();
                     each.setChannel(this.talkName(cur));
                     each.setContent(dialog.trim());
                     each.setTime(ti.trim());
                     each.setChildVoiceId(childVoiceId);
                     dialogList.add(each);
                     dialog = contents[i] + " ";
                     cur = channels[i];
                     ti = voiceTimes[i] + " ";
                  }
               }
            }

            return dialogList;
         }
      }
   }

   private String talkName(String channel) {
      if ("0".equals(channel)) {
         return "坐席:";
      } else {
         return "1".equals(channel) ? "客户:" : "";
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
