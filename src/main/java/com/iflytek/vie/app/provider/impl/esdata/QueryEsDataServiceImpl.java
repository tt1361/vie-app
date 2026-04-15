package com.iflytek.vie.app.provider.impl.esdata;

import com.iflytek.vie.app.api.esdata.QueryEsDataService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.search.ESDataRequest;
import com.iflytek.vie.app.pojo.search.ESSearchResponse;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.DimensionDBPlatform;
import com.iflytek.vie.app.provider.database.ModelInfoDBPlatform;
import com.iflytek.vie.constants.IndexConstants;
import com.iflytek.vie.dynamic.DynamicDataSourceContextHolder;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.sf.json.JSONArray;
import org.anydrill.calculate.set.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueryEsDataServiceImpl implements QueryEsDataService {
   private final Logger logger = LoggerFactory.getLogger(QueryEsDataServiceImpl.class);
   private static Map<String, String> modelMap = new HashMap<>();
   private DataSourceInfo dataSourceInfo;
   private ExcuteContext excuteContext;
   @Autowired
   private DimensionDBPlatform dimensionDBPlatform;
   @Autowired
   private ModelInfoDBPlatform modelInfoDBPlatform;

   @RequestMapping("/querySingleModelData")
   public ESSearchResponse querySingleModelData(ESDataRequest esDataRequest) throws VieAppServiceException {
      this.logger.info("querySingleModelData（标准服务） 方法开始响应");
      String sql = "";
      String selects = "offLineTagId,offLineTagName,offLineTagInfo";
      String dataSource = esDataRequest.getDataSource();
      String num = esDataRequest.getPageNum();
      String size = esDataRequest.getPageSize();
      String modelName = esDataRequest.getModelName();
      String tableNaeme = dataSource + "model";
      String where = "";
      if (!DynamicDataSourceContextHolder.containsDataSource(dataSource)) {
         this.logger.warn("数据源：" + dataSource + "为不可用数据源！");
         return new ESSearchResponse("数据源：" + dataSource + "为不可用数据源！");
      } else if (num != null && size != null) {
         Long pageNum = null;
         Long pageSize = null;

         try {
            pageNum = Long.parseLong(num);
            pageSize = Long.parseLong(size);
            if (pageNum == null || pageSize == null) {
               this.logger.warn("请传入合法的分页相关参数！");
               return new ESSearchResponse("请传入合法的分页相关参数！");
            }

            if (pageSize < 1L || pageNum < 1L || pageSize > 10000L) {
               this.logger.warn("请传入合法的分页相关参数！");
               return new ESSearchResponse("请传入合法的分页相关参数！");
            }
         } catch (Exception var34) {
            this.logger.warn("请传入合法的分页相关参数！");
            return new ESSearchResponse("请传入合法的分页相关参数！");
         }

         new ArrayList();

         List dimensionConfigList;
         try {
            dimensionConfigList = this.dimensionDBPlatform.searchAllDim(dataSource);
         } catch (ViePlatformServiceException var33) {
            this.logger.error("querySingleModelData 查询维度接口出错！");
            throw new VieAppServiceException(var33);
         }

         if (dimensionConfigList != null && dimensionConfigList.size() != 0) {
            for (int i = 0; i < dimensionConfigList.size(); i++) {
               selects = selects + "," + ((DimensionConfig)dimensionConfigList.get(i)).getIndexField();
            }

            Long modelId = this.modelInfoDBPlatform.queryModelId(dataSource, modelName);
            if (modelId == -99L) {
               this.logger.info("没有\"" + modelName + "\"这个模型");
               return new ESSearchResponse("没有\"" + modelName + "\"这个模型");
            } else {
               where = where + "offLineTagId = " + modelId;
               where = where + " and processed = 0";
               Long start = pageSize * (pageNum - 1L) + 1L;
               Long end = pageSize * pageNum;
               where = where + " and rownum between " + start + " and " + end;
               sql = "select " + selects + " from " + tableNaeme + " where " + where;
               this.logger.info("querySingleModelData sql :" + sql);
               String url = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
               ResultSet rs = new ExcuteContext().executeSearchQuery(url, sql);
               List<LinkedHashMap<String, Object>> resultList = rs.toList();
               List<LinkedHashMap<String, Object>> listFinal = new ArrayList<>();

               for (LinkedHashMap<String, Object> lhmOne : resultList) {
                  Iterator<Entry<String, Object>> iterator = lhmOne.entrySet().iterator();
                  List<LinkedHashMap<String, Object>> listOne = new ArrayList<>();
                  LinkedHashMap<String, Object> lhmTwo = new LinkedHashMap<>();
                  LinkedHashMap<String, Object> modelInfoTag = new LinkedHashMap<>();

                  while (iterator.hasNext()) {
                     Entry entry = iterator.next();
                     if ("offLineTagInfo".equals(entry.getKey().toString())) {
                        if (lhmOne.get("offLineTagInfo") != null && !StringUtils.isNullOrEmpry(String.valueOf(lhmOne.get("offLineTagInfo")))) {
                           JSONArray jsonArray = JSONArray.fromObject(String.valueOf(lhmOne.get("offLineTagInfo")));
                           if (jsonArray != null && jsonArray.size() > 0) {
                              for (Map<String, Object> tempWord : (List<Map<String, Object>>)jsonArray) {
                                 int type = Integer.parseInt(tempWord.get("type").toString());
                                 LinkedHashMap<String, Object> hms = new LinkedHashMap<>();
                                 hms.put("content", tempWord.get("content"));
                                 hms.put("beginTime", tempWord.get("beginTime"));
                                 hms.put("endTime", tempWord.get("endTime"));
                                 hms.put("type", type);
                                 listOne.add(hms);
                              }
                           }
                        }

                        lhmTwo.put("offLineTagInfo", listOne);
                        modelInfoTag.put("modelInfo", lhmTwo);
                     } else if (!"offLineTagId".equals(entry.getKey().toString()) && !"offLineTagName".equals(entry.getKey().toString())) {
                        modelInfoTag.put(entry.getKey().toString(), entry.getValue());
                     } else {
                        lhmTwo.put(entry.getKey().toString(), entry.getValue());
                        modelInfoTag.put("modelInfo", lhmTwo);
                     }
                  }

                  listFinal.add(modelInfoTag);
               }

               ESSearchResponse esSearchResponse = new ESSearchResponse(pageNum, pageSize, rs.getTotalCount());
               esSearchResponse.setMessage("响应成功！");
               esSearchResponse.setValues(listFinal);
               return esSearchResponse;
            }
         } else {
            this.logger.info("querySingleModelData 查询维度结果为空！");
            return new ESSearchResponse("querySingleModelData 查询维度结果为空！");
         }
      } else {
         this.logger.warn("请传入合法的分页相关参数！");
         return new ESSearchResponse("请传入合法的分页相关参数！");
      }
   }

   @RequestMapping("/querySingleIdData")
   public ESSearchResponse querySingleIdData(ESDataRequest esDataRequest) throws VieAppServiceException {
      this.logger.info("querySingleIdData（标准服务） 方法开始响应");
      String dataSource = esDataRequest.getDataSource();
      String id = esDataRequest.getId();
      if (!DynamicDataSourceContextHolder.containsDataSource(dataSource)) {
         this.logger.warn("数据源：" + dataSource + "为不可用数据源！");
         return new ESSearchResponse("数据源：" + dataSource + "为不可用数据源！");
      } else if (StringUtils.isNullOrEmpry(id)) {
         int insightType = IndexConstants.getInsightType(dataSource);
         if (insightType == 0) {
            this.logger.warn("请传入合法的录音流水号参数");
            return new ESSearchResponse("请传入合法的录音流水号参数");
         } else {
            this.logger.warn("请传入合法的任务号参数");
            return new ESSearchResponse("请传入合法的任务号参数");
         }
      } else {
         String where = "";
         String selects = "offLineTagId,offLineTagName,offLineTagInfo";
         String table = dataSource + "model";
         new ArrayList();

         List dimensionConfigList;
         try {
            dimensionConfigList = this.dimensionDBPlatform.searchAllDim(dataSource);
         } catch (ViePlatformServiceException var26) {
            this.logger.error("querySingleIdData 查询维度接口出错！");
            throw new VieAppServiceException(var26);
         }

         if (dimensionConfigList != null && dimensionConfigList.size() != 0) {
            for (int i = 0; i < dimensionConfigList.size(); i++) {
               selects = selects + "," + ((DimensionConfig)dimensionConfigList.get(i)).getIndexField();
            }

            where = where + "id = '" + id + "'";
            where = where + " and processed = 0";
            String sql = "select " + selects + " from " + table + " where " + where;
            this.logger.info("querySingleIdData sql :" + sql);
            String url = DynamicEsSource.getEsSourceByType(dataSource, "modelAnydrillAddress");
            ResultSet rs = new ExcuteContext().executeSearchQuery(url, sql);
            List<LinkedHashMap<String, Object>> resultList = rs.toList();
            List<LinkedHashMap<String, Object>> listFinal = new ArrayList<>();

            for (LinkedHashMap<String, Object> lhmOne : resultList) {
               Iterator<Entry<String, Object>> iterator = lhmOne.entrySet().iterator();
               List<LinkedHashMap<String, Object>> listOne = new ArrayList<>();
               LinkedHashMap<String, Object> lhmTwo = new LinkedHashMap<>();
               LinkedHashMap<String, Object> modelInfoTag = new LinkedHashMap<>();

               while (iterator.hasNext()) {
                  Entry entry = iterator.next();
                  if ("offLineTagInfo".equals(entry.getKey().toString())) {
                     if (lhmOne.get("offLineTagInfo") != null && !StringUtils.isNullOrEmpry(String.valueOf(lhmOne.get("offLineTagInfo")))) {
                        JSONArray jsonArray = JSONArray.fromObject(String.valueOf(lhmOne.get("offLineTagInfo")));
                        if (jsonArray != null && jsonArray.size() > 0) {
                           for (Map<String, Object> tempWord : (List<Map<String, Object>>)jsonArray) {
                              int type = Integer.parseInt(tempWord.get("type").toString());
                              LinkedHashMap<String, Object> hms = new LinkedHashMap<>();
                              hms.put("content", tempWord.get("content"));
                              hms.put("beginTime", tempWord.get("beginTime"));
                              hms.put("endTime", tempWord.get("endTime"));
                              hms.put("type", type);
                              listOne.add(hms);
                           }
                        }
                     }

                     lhmTwo.put("offLineTagInfo", listOne);
                     modelInfoTag.put("modelInfo", lhmTwo);
                  } else if (!"offLineTagId".equals(entry.getKey().toString()) && !"offLineTagName".equals(entry.getKey().toString())) {
                     modelInfoTag.put(entry.getKey().toString(), entry.getValue());
                  } else {
                     lhmTwo.put(entry.getKey().toString(), entry.getValue());
                     modelInfoTag.put("modelInfo", lhmTwo);
                  }
               }

               listFinal.add(modelInfoTag);
            }

            ESSearchResponse esSearchResponse = new ESSearchResponse();
            esSearchResponse.setValues(listFinal);
            esSearchResponse.setMessage("响应成功！");
            this.logger.info("querySingleIdData 方法响应成功");
            return esSearchResponse;
         } else {
            this.logger.info("querySingleIdData 查询维度结果为空！");
            return new ESSearchResponse("querySingleIdData 查询维度结果为空！");
         }
      }
   }

   @RequestMapping("/queryTimeRangeData")
   public ESSearchResponse queryTimeRangeData(ESDataRequest esDataRequest) throws VieAppServiceException {
      this.logger.info("queryTimeRangeData（标准服务） 方法开始响应");
      String dataSource = esDataRequest.getDataSource();
      String num = esDataRequest.getPageNum();
      String size = esDataRequest.getPageSize();
      String beginTime = esDataRequest.getBeginTime();
      String endTime = esDataRequest.getEndTime();
      if (!DynamicDataSourceContextHolder.containsDataSource(dataSource)) {
         this.logger.warn("数据源：" + dataSource + "为不可用数据源！");
         return new ESSearchResponse("数据源：" + dataSource + "为不可用数据源！");
      } else {
         Long pageNum = null;
         Long pageSize = null;

         try {
            pageNum = Long.parseLong(num);
            pageSize = Long.parseLong(size);
            if (pageNum == null || pageSize == null) {
               this.logger.warn("请传入合法的分页相关参数！");
               return new ESSearchResponse("请传入合法的分页相关参数！");
            }

            if (pageSize < 1L || pageNum < 1L || pageSize > 10000L) {
               this.logger.warn("请传入合法的分页相关参数！");
               return new ESSearchResponse("请传入合法的分页相关参数！");
            }
         } catch (Exception var26) {
            this.logger.warn("请传入合法的分页相关参数！");
            return new ESSearchResponse("请传入合法的分页相关参数！");
         }

         try {
            Long begin = Long.parseLong(beginTime);
            Long end = Long.parseLong(endTime);
            if (begin.toString().length() != 13 || end.toString().length() != 13) {
               this.logger.warn("请传入合法的时间相关参数！");
               return new ESSearchResponse("请传入合法的时间相关参数！");
            }

            if (begin > end) {
               this.logger.warn("开始时间不能大于结束时间！");
               return new ESSearchResponse("开始时间不能大于结束时间！");
            }

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String b = format.format(begin);
            Date date = format.parse(b);
            String e = format.format(end);
            Date date2 = format.parse(e);
         } catch (Exception var25) {
            this.logger.warn("请传入合法的时间相关参数！");
            return new ESSearchResponse("请传入合法的时间相关参数！");
         }

         new ArrayList();

         List var30;
         try {
            var30 = this.dimensionDBPlatform.searchAllDim(dataSource);
         } catch (ViePlatformServiceException var24) {
            this.logger.error("queryTimeRangeData 查询维度接口出错！");
            throw new VieAppServiceException(var24);
         }

         if (var30 != null && var30.size() != 0) {
            String where = " where ";
            StringBuffer sql = new StringBuffer();
            sql.append("select content");

            for (int i = 0; i < var30.size(); i++) {
               sql.append("," + ((DimensionConfig)var30.get(i)).getIndexField());
            }

            sql.append(" from " + dataSource + " ");
            where = where + " processed = 0";
            where = where + " and timestamp between " + beginTime + " and " + endTime;
            Long start = pageSize * (pageNum - 1L) + 1L;
            Long endx = pageSize * pageNum;
            where = where + " and rownum between " + start + " and " + endx;
            sql.append(where);
            this.logger.info("queryTimeRangeData sql :" + sql);
            String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
            ResultSet rs = new ExcuteContext().executeSearchQuery(url, sql.toString());
            List<LinkedHashMap<String, Object>> resultList = rs.toList();
            List<LinkedHashMap<String, Object>> listFinal = new ArrayList<>();

            for (LinkedHashMap<String, Object> lhmOne : resultList) {
               Iterator<Entry<String, Object>> iterator = lhmOne.entrySet().iterator();
               LinkedHashMap<String, Object> lhmTwo = new LinkedHashMap<>();

               while (iterator.hasNext()) {
                  Entry entry = iterator.next();
                  lhmTwo.put(entry.getKey().toString(), entry.getValue());
               }

               listFinal.add(lhmTwo);
            }

            ESSearchResponse esSearchResponse = new ESSearchResponse(pageNum, pageSize, rs.getTotalCount());
            esSearchResponse.setValues(listFinal);
            esSearchResponse.setMessage("响应成功！");
            this.logger.info("queryTimeRangeData 方法响应成功");
            return esSearchResponse;
         } else {
            this.logger.info("queryTimeRangeData 查询维度结果为空！");
            return new ESSearchResponse("queryTimeRangeData 查询维度结果为空！");
         }
      }
   }

   public ExcuteContext getExcuteContext() {
      return this.excuteContext;
   }

   public void setExcuteContext(ExcuteContext excuteContext) {
      this.excuteContext = excuteContext;
   }

   public DataSourceInfo getDataSourceInfo() {
      return this.dataSourceInfo;
   }

   public void setDataSourceInfo(DataSourceInfo dataSourceInfo) {
      this.dataSourceInfo = dataSourceInfo;
   }

   static {
      modelMap.put("modelName", "offLineTagId");
      modelMap.put("keyword", "offLineTagInfo");
      modelMap.put("hitVoiceId", "hitVoiceId");
   }
}
