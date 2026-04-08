package com.iflytek.vie.app.provider.impl.datadrill;

import com.iflytek.vie.app.api.model.ModelService;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.model.ModelRequest;
import com.iflytek.vie.app.provider.engine.VoiceDetail;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataFormat {
   private static String modelid = "modelId";
   private static String rows = "rows";
   private static String child_fields = "child_fields";

   public static List<String> getMoldelList(String userId, String systemID, String dataSource, ModelService modelService) throws Exception {
      List<String> result = new ArrayList<>();
      ModelRequest modelRequest = new ModelRequest();
      modelRequest.setUserId(userId);
      modelRequest.setDataSource(systemID);
      modelRequest.setSystemId(systemID);
      modelRequest.setDataSource(dataSource);
      modelRequest.setModelGroupId(-1L);
      modelRequest.setType("online");
      Map<String, Object> modelList = modelService.searModelByGroupService(modelRequest);
      if (modelList == null) {
         throw new ViePlatformServiceException("明细数据查询接口错误:模型接口获取失败！");
      } else {
         if (modelList.size() > 0) {
            List<HashMap<String, BigDecimal>> dataList = (List<HashMap<String, BigDecimal>>)modelList.get(rows);
            if (rows != null) {
               for (HashMap<String, BigDecimal> cd : dataList) {
                  result.add(cd.get(modelid).toString());
               }
            }
         }

         return result;
      }
   }

   public static List<LinkedHashMap<String, Object>> formatData(List<LinkedHashMap<String, Object>> orgList, List<String> modelList) {
      for (LinkedHashMap<String, Object> cd : orgList) {
         List<HashMap<String, Object>> moldelList = (List<HashMap<String, Object>>)cd.get(child_fields);
         List<HashMap<String, Object>> newList = new ArrayList<>();

         for (HashMap<String, Object> ccd : (List<HashMap<String, Object>>)moldelList) {
            String modelID = ccd.get(VoiceDetail.Model) == null ? "-1" : ccd.get(VoiceDetail.Model).toString();
            if (modelList.contains(modelID)) {
               newList.add(ccd);
            }
         }

         cd.put(child_fields, newList);
      }

      return orgList;
   }
}
