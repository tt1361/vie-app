package com.iflytek.vie.app.provider.impl.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.common.ColumnMap;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.pojo.player.PlayerDataRequest;
import com.iflytek.vie.app.pojo.search.SearchRequest;
import com.iflytek.vie.app.pojo.search.SearchResultResponse;
import com.iflytek.vie.app.provider.database.ModelDB;
import com.iflytek.vie.app.provider.database.ModelGroupDB;
import com.iflytek.vie.constants.CommonParams;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SearchQuery {
   private final Logger logger = LoggerFactory.getLogger(SearchQuery.class);

   public abstract SearchResultResponse queryTextSearchList(
      SearchRequest var1, LinkedHashMap<String, DimensionConfig> var2, List<HashMap<String, Object>> var3, String var4
   ) throws VieAppServiceException;

   public abstract SearchResultResponse queryTableSearchList(
      SearchRequest var1,
      LinkedHashMap<String, DimensionConfig> var2,
      List<HashMap<String, Object>> var3,
      String var4,
      LinkedHashMap<String, String> var5,
      List<ColumnMap> var6,
      ModelDB var7,
      ModelGroupDB var8,
      GroupAuthService var9
   ) throws VieAppServiceException;

   protected List<LinkedHashMap<String, Object>> dealAnydrillReturenValue(
      String[] connectUrl, List<LinkedHashMap<String, Object>> data, String tableName, String modelInfo, HashMap<Integer, String> allModels, int methodType
   ) throws Exception {
      if (data != null && data.size() != 0) {
         String idStr = IndexConstants.getIdStr(data);
         Map<String, List<Map<String, Object>>> modelMap = null;
         if (!StringUtils.isNullOrEmpry(idStr)) {
            modelMap = IndexConstants.getModelInforById(connectUrl, tableName, modelInfo, idStr, null);
         }

         List<LinkedHashMap<String, Object>> realData = new ArrayList<>();
         List keywordlist = null;
         List<String> modelNames = null;
         String voiceId = "";

         for (LinkedHashMap<String, Object> eachCd : data) {
            voiceId = String.valueOf(eachCd.get("id"));
            if (modelMap != null) {
               modelNames = new ArrayList<>();
               keywordlist = new ArrayList();
               List<Map<String, Object>> modelList = modelMap.get(voiceId);
               if (modelList != null && modelList.size() > 0) {
                  for (Map<String, Object> hm : (List<Map<String, Object>>)modelList) {
                     String offLineId = String.valueOf(hm.get("offLineTagId"));
                     if (!StringUtils.isNullOrEmpry(offLineId)) {
                        String modelName = allModels.get(Integer.parseInt(offLineId));
                        if (!StringUtils.isNullOrEmpry(modelName)) {
                           modelNames.add(modelName);
                           if (hm.get("offLineTagInfo") != null && !hm.get("offLineTagInfo").equals("[]")) {
                              ObjectMapper mapper = new ObjectMapper();

                              for (HashMap<String, Object> hmss : (List<HashMap<String, Object>>)mapper.readValue(hm.get("offLineTagInfo").toString(), List.class)) {
                                 if (methodType != 1) {
                                    int type = Integer.parseInt(hmss.get("type").toString());
                                    if (type == 0) {
                                       Map<String, String> hms = new HashMap<>();
                                       if (IndexConstants.getInsightType(tableName) == 1) {
                                          hms.put("voiceId", String.valueOf(hmss.get("voiceId")));
                                          hms.put("machineId", String.valueOf(hmss.get("machineId")));
                                          hms.put("voiceUri", String.valueOf(hmss.get("voiceUri")));
                                       }

                                       hms.put("word", String.valueOf(hmss.get("content")));
                                       hms.put("begin", String.valueOf(hmss.get("beginTime")));
                                       hms.put("end", String.valueOf(hmss.get("endTime")));
                                       if (!keywordlist.contains(hms)) {
                                          keywordlist.add(hms);
                                       }
                                    }
                                 } else {
                                    StringBuffer hmsx = new StringBuffer();
                                    int type = Integer.parseInt(hmss.get("type").toString());
                                    if (type == 0) {
                                       if (!StringUtils.isNullOrEmpry(String.valueOf(hmss.get("content")))) {
                                          hmsx.append(hmss.get("content") + " ");
                                       }

                                       if (!keywordlist.contains(hmsx.toString())) {
                                          keywordlist.add(hmsx.toString());
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            eachCd.put("keyword", keywordlist);
            eachCd.put("modelName", modelNames);
            realData.add(eachCd);
         }

         return realData;
      } else {
         return null;
      }
   }

   protected List<LinkedHashMap<String, Object>> handleListData(
      List<LinkedHashMap<String, Object>> data, int channel, HashMap<String, String> switchColMap, int methodType
   ) {
      if (data != null && data.size() != 0) {
         int switchColMapSize = 0;
         if (switchColMap != null) {
            switchColMapSize = switchColMap.size();
         }

         String contentOrigin = "";
         PlayerDataRequest playerDataRequest = null;

         for (LinkedHashMap<String, Object> cd : data) {
            if (switchColMapSize > 0) {
               for (String key : switchColMap.keySet()) {
                  cd.put(switchColMap.get(key), cd.get(key));
               }
            }

            contentOrigin = String.valueOf(cd.remove("contentOrigin")).replaceAll(CommonParams.contentRGX, "").trim();
            playerDataRequest = new PlayerDataRequest();
            playerDataRequest.setTimePosition(String.valueOf(cd.remove("timePosition")));
            playerDataRequest.setChannelSeq(String.valueOf(cd.remove("channelSeq")));
            playerDataRequest.setContentOrigin(contentOrigin);

            try {
               List<String> content = this.getFullTextInfo(playerDataRequest, methodType);
               if (methodType == 1) {
                  cd.put("content", content);
               } else if (content != null && content.size() != 0) {
                  StringBuffer sb = new StringBuffer();

                  for (int i = 0; i < content.size(); i++) {
                     sb.append(content.get(i) + " ");
                  }

                  cd.put("content", sb.toString());
               } else {
                  cd.put("content", "");
               }
            } catch (Exception var13) {
               this.logger.error("出现异常，id为：{}，异常信息：{}", cd.get("contact_id"), var13.getMessage());
               cd.put("content", "");
            }
         }

         return data;
      } else {
         return null;
      }
   }

   public List<String> getFullTextInfo(PlayerDataRequest request, int methodType) throws ViePlatformServiceException, VieAppServiceException {
      List<String> rsList = new ArrayList<>();
      if (request.getContentOrigin() == null || request.getContentOrigin().length() == 0) {
         throw new VieAppServiceException("contentOrigin参数不能为空");
      } else if (request.getChannelSeq() == null || request.getChannelSeq().length() == 0) {
         throw new VieAppServiceException("channelSeq参数不能为空");
      } else if (request.getTimePosition() != null && request.getTimePosition().length() != 0) {
         try {
            String[] singleContent = request.getContentOrigin().split("\\$");
            String[] singleChannelSeq = request.getChannelSeq().split("\\$");
            String[] singleTimePosition = request.getTimePosition().split("\\$");
            if (singleChannelSeq.length == singleContent.length
               && singleContent.length == singleTimePosition.length
               && singleChannelSeq.length == singleTimePosition.length) {
               String temp = "";

               for (int i = 0; i < singleContent.length; i++) {
                  temp = this.groupTextInfo(singleContent[i], singleChannelSeq[i], singleTimePosition[i], methodType);
                  if (StringUtils.isNotNullAndEmpry(temp)) {
                     rsList.add(temp.replace(" ", ""));
                  }
               }

               return rsList;
            } else {
               this.logger.error("录音的Content、ChannelSeq、TimePosition有问题");
               throw new VieAppServiceException("录音的Content、ChannelSeq、TimePosition有问题");
            }
         } catch (Exception var9) {
            this.logger.error("getFullTextInfo method has a error!");
            throw new VieAppServiceException("服务内部错误", var9);
         }
      } else {
         throw new VieAppServiceException("timePosition参数不能为空");
      }
   }

   public String groupTextInfo(String contentOrigin, String channelSeq, String timePosition, int methodType) throws VieAppServiceException {
      StringBuffer result = new StringBuffer();
      if (contentOrigin == null) {
         System.out.println("处理结果为空!");
         return null;
      } else {
         String[] channels = channelSeq.split(" ");
         String[] contents = contentOrigin.split(" ");
         String[] times = timePosition.split(" ");
         if (channels.length != contents.length) {
            this.logger.error("对话文本有问题");
            throw new VieAppServiceException("对话文本格式有问题");
         } else {
            String[] voiceTimes = new String[times.length];

            for (int j = 0; j < times.length; j++) {
               voiceTimes[j] = times[j].split("\\|")[0];
            }

            String cur = channels[0];
            String dialog = "";
            if (contents[0].matches(CommonParams.contentRGX)) {
               dialog = "";
            } else {
               dialog = contents[0] + " ";
            }

            for (int i = 1; i < channels.length; i++) {
               if ("VE".equals(contents[i])) {
                  result.append(this.parseContent(this.talkName(cur), dialog.trim(), methodType));
               } else if (!contents[i].matches(CommonParams.contentRGX)) {
                  if (channels[i].equals(cur) && i != channels.length - 1) {
                     dialog = dialog + contents[i] + " ";
                  } else if (i == channels.length - 1) {
                     dialog = dialog + contents[i] + " ";
                     result.append(this.parseContent(this.talkName(cur), dialog.trim(), methodType));
                  } else {
                     result.append(this.parseContent(this.talkName(cur), dialog.trim(), methodType));
                     dialog = contents[i] + " ";
                     cur = channels[i];
                  }
               }
            }

            return result.toString();
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

   private String parseContent(String channel, String content, int methodType) {
      String rscontent = "";
      if (StringUtils.isNotNullAndEmpry(channel)) {
         rscontent = channel + content + " ";
      }

      return rscontent;
   }

   protected List<Long> getModelGroupIds(GroupAuthService groupAuthService, int type, String userId, String dataSource) {
      GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
      groupAuthRequest.setUserId(Long.parseLong(userId));
      groupAuthRequest.setGroupType(type);
      groupAuthRequest.setDataSource(dataSource);
      List<Long> groupId = null;

      try {
         return groupAuthService.queryGroupAuth(groupAuthRequest);
      } catch (PermissionServiceException var8) {
         this.logger.error("getModelGroupIds method has a error!", var8);
         return groupId;
      }
   }

   protected HashMap<Integer, String> getAllModels(
      ModelGroupDB modelGroupDB, ModelDB modelDB, List<Long> groupIds, String tableName, String keyword, String dataSource
   ) {
      HashMap<Integer, String> allModels = null;
      List<Long> allGroupIds = null;

      try {
         if (groupIds != null) {
            allGroupIds = modelGroupDB.getModelGroupIdById(groupIds, dataSource);
            allModels = modelDB.queryModelOnline(keyword, allGroupIds, tableName);
         } else {
            allModels = new HashMap<>();
         }

         return allModels;
      } catch (Exception var10) {
         this.logger.error("getAllModels method has a error!", var10);
         return new HashMap<>();
      }
   }
}
