package com.iflytek.vie.app.provider.impl.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.api.permission.GroupAuthService;
import com.iflytek.vie.app.exception.PermissionServiceException;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.common.ColumnMap;
import com.iflytek.vie.app.pojo.common.PagerResponse;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.permission.GroupAuthRequest;
import com.iflytek.vie.app.pojo.report.ReportTableDataRequest;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SearchQuery {
   private final Logger logger = LoggerFactory.getLogger(SearchQuery.class);

   public abstract PagerResponse<LinkedHashMap<String, Object>> queryVoiceList(
      ReportTableDataRequest var1,
      LinkedHashMap<String, DimensionConfig> var2,
      List<HashMap<String, Object>> var3,
      String var4,
      LinkedHashMap<String, String> var5,
      List<ColumnMap> var6,
      HashMap<String, String> var7,
      LinkedList<String> var8,
      boolean var9
   ) throws VieAppServiceException;

   protected List<LinkedHashMap<String, Object>> dealHitVoiceValue(
      String[] connectUrl,
      List<LinkedHashMap<String, Object>> data,
      String tableName,
      String modelInfo,
      HashMap<String, String> allModels,
      int methodType,
      List<String> showColumn
   ) {
      if (data != null && data.size() != 0) {
         String voiceStr = IndexConstants.getId(data, "childVoiceId");
         new HashMap();

         try {
            String sql = "select hitVoiceId, offLineTagInfo,offLineTagId from "
               + IndexConstants.getModelTableName(tableName)
               + " where childVoiceId in ("
               + voiceStr
               + ")";
            PreparedStatement pst = new PreparedStatement(connectUrl);
            ResultSet rs = pst.executeQuery(sql);
            if (rs != null) {
               Map<String, Map<String, Object>> modelMap = IndexConstants.dealModelInfor(rs, allModels);

               for (LinkedHashMap<String, Object> eachCd : data) {
                  String voiceId = String.valueOf(eachCd.get("childVoiceId"));
                  if (voiceId != null && modelMap != null) {
                     Map<String, Object> map = (Map<String, Object>)modelMap.get(voiceId);
                     if (map != null) {
                        String modelName = map.get("modelName") == null ? "" : String.valueOf(map.get("modelName"));
                        String keyWord = map.get("keyword") == null ? "" : String.valueOf(map.get("keyword"));
                        eachCd.put("offLineTagId", modelName);
                        eachCd.put("keyword", keyWord);
                     } else {
                        eachCd.put("offLineTagId", "");
                        eachCd.put("keyword", "");
                     }
                  } else {
                     eachCd.put("offLineTagId", "");
                     eachCd.put("keyword", "");
                  }
               }
            } else {
               for (LinkedHashMap<String, Object> eachCdx : data) {
                  eachCdx.put("offLineTagId", "");
                  eachCdx.put("keyword", "");
               }
            }
         } catch (Exception var19) {
            var19.printStackTrace();
         }

         return data;
      } else {
         return null;
      }
   }

   protected List<LinkedHashMap<String, Object>> dealModelReturenValue(
      String[] connectUrl,
      List<LinkedHashMap<String, Object>> data,
      String tableName,
      String modelInfo,
      HashMap<String, String> allModels,
      int methodType,
      List<String> showColumn
   ) throws Exception {
      List<LinkedHashMap<String, Object>> realData = new ArrayList<>();
      if (data != null && data.size() != 0) {
         String taskStr = IndexConstants.getId(data, "id");
         Map<String, List<Map<String, Object>>> modelMap = null;
         if (!StringUtils.isNullOrEmpry(taskStr)) {
            modelMap = IndexConstants.getModelInforById(connectUrl, tableName, modelInfo, taskStr, null);
         }

         StringBuffer modelNames = new StringBuffer();
         StringBuffer keywords = new StringBuffer();
         String id = "";

         for (LinkedHashMap<String, Object> eachCd : data) {
            id = String.valueOf(eachCd.get("id"));
            if (modelMap != null) {
               modelNames = new StringBuffer();
               keywords = new StringBuffer();
               List<Map<String, Object>> modelList = modelMap.get(id);
               if (modelList != null && modelList.size() > 0) {
                  for (Map<String, Object> hm : (List<Map<String, Object>>)modelList) {
                     String offLineId = String.valueOf(hm.get("offLineTagId"));
                     if (!StringUtils.isNullOrEmpry(offLineId)) {
                        String modelName = allModels.get(offLineId);
                        if (!StringUtils.isNullOrEmpry(modelName)) {
                           if (modelNames.length() > 0) {
                              modelNames.append(",");
                           }

                           modelNames.append(modelName);
                           if (hm.get("offLineTagInfo") != null && !hm.get("offLineTagInfo").equals("[]")) {
                              ObjectMapper mapper = new ObjectMapper();
                              List<HashMap<String, Object>> wordList = (List<HashMap<String, Object>>)mapper.readValue(
                                 hm.get("offLineTagInfo").toString(), List.class
                              );
                              if (showColumn != null && showColumn.contains("voiceId") && IndexConstants.getInsightType(tableName) == 1) {
                                 for (HashMap<String, Object> hmss : (List<HashMap<String, Object>>)wordList) {
                                    if (hmss.get("voiceId").equals(eachCd.get("childVoiceId"))) {
                                       if (keywords.length() > 0) {
                                          keywords.append(",");
                                       }

                                       keywords.append(hmss.get("content"));
                                    }
                                 }
                              } else {
                                 for (HashMap<String, Object> hmssx : (List<HashMap<String, Object>>)wordList) {
                                    if (keywords.length() > 0) {
                                       String content = String.valueOf(hmssx.get("content"));
                                       if (!StringUtils.isNullOrEmpry(content)) {
                                          keywords.append(",");
                                       }
                                    }

                                    keywords.append(hmssx.get("content"));
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }

            eachCd.put("keyword", keywords);
            eachCd.put("offLineTagId", modelNames);
            realData.add(eachCd);
         }

         return realData;
      } else {
         return null;
      }
   }

   protected List<LinkedHashMap<String, Object>> handleListData(List<LinkedHashMap<String, Object>> data, int channel, HashMap<String, String> switchColMap) {
      if (data != null && data.size() != 0) {
         int switchColMapSize = 0;
         if (switchColMap != null) {
            switchColMapSize = switchColMap.size();
         }

         for (LinkedHashMap<String, Object> cd : data) {
            if (switchColMapSize > 0) {
               for (String key : switchColMap.keySet()) {
                  cd.put(switchColMap.get(key), cd.get(key));
               }
            }
         }

         return data;
      } else {
         return null;
      }
   }

   protected List<Long> getModelGroupIds(GroupAuthService groupAuthService, int type, String userId) {
      GroupAuthRequest groupAuthRequest = new GroupAuthRequest();
      groupAuthRequest.setUserId(Long.parseLong(userId));
      groupAuthRequest.setGroupType(type);
      List<Long> groupId = null;

      try {
         return groupAuthService.queryGroupAuth(groupAuthRequest);
      } catch (PermissionServiceException var7) {
         this.logger.error("getModelGroupIds method has a error!", var7);
         return groupId;
      }
   }
}
