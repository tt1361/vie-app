package com.iflytek.vie.app.provider.impl.esdata;

import com.iflytek.vie.app.api.esdata.EsDataService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.search.ESSearchRequest;
import com.iflytek.vie.app.pojo.search.ESSearchResponse;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.StringUtils;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import net.sf.json.JSONArray;
import org.anydrill.calculate.set.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EsDataServiceImpl implements EsDataService {
   private final Logger logger = LoggerFactory.getLogger(EsDataServiceImpl.class);
   private DataSourceInfo dataSourceInfo;
   private ExcuteContext excuteContext;

   public ESSearchResponse getEsDetailData(ESSearchRequest esSearchRequest) throws VieAppServiceException {
      this.logger.info("getEsDetailData（标准服务） 方法开始响应");
      String where = "";

      try {
         Properties prop = new Properties();
         Properties systemProperties = new Properties(System.getProperties());
         File configPath = new File(systemProperties.getProperty("APP_CONFIG"));
         this.logger.info("config路径：" + configPath);
         String commonFilePath = configPath + File.separator + "config.properties";
         File commonIntegrationFile = new File(commonFilePath);
         this.logger.info("加载config配置文件:" + commonFilePath);
         prop.load(new FileInputStream(commonIntegrationFile));
         String selects = prop.getProperty("main.field.name");
         String table = esSearchRequest.getDataSource();
         where = where + " timestamp >= " + esSearchRequest.getStartTime() + " and timestamp <= " + esSearchRequest.getEndTime();
         where = where + " and processed = 0";
         if (esSearchRequest.getPageSize() == null
            || esSearchRequest.getPageNum() == null
            || esSearchRequest.getPageSize() == 0L
            || esSearchRequest.getPageNum() == 0L) {
            esSearchRequest.setPageSize(10L);
            esSearchRequest.setPageNum(1L);
         }

         Long start = esSearchRequest.getPageSize() * (esSearchRequest.getPageNum() - 1L) + 1L;
         Long end = esSearchRequest.getPageSize() * esSearchRequest.getPageNum();
         where = where + " and rownum between " + start + " and " + end;
         String sql = "select " + selects + " from " + table + " where " + where;
         this.logger.info("getEsDetailData sql :" + sql);
         String url = DynamicEsSource.getEsSourceByType(esSearchRequest.getDataSource(), "mainAnydrillAddress");
         ResultSet rs = this.excuteContext.executeSearchQuery(url, sql);
         List<LinkedHashMap<String, Object>> resultList = rs.toList();

         for (int i = 0; i < resultList.size(); i++) {
            resultList.get(i).put("modelInfo", this.getEsModelData(resultList.get(i).get("voiceId").toString(), esSearchRequest.getDataSource()));
         }

         ESSearchResponse esSearchResponse = new ESSearchResponse(esSearchRequest.getPageNum(), esSearchRequest.getPageSize(), rs.getTotalCount());
         esSearchResponse.setValues(resultList);
         this.logger.info("getEsDetailData（标准服务） 方法响应成功");
         return esSearchResponse;
      } catch (Exception var17) {
         this.logger.error(var17.getMessage(), var17);
         throw new VieAppServiceException(var17.getMessage());
      }
   }

   public List<LinkedHashMap<String, Object>> getEsModelData(String id, String datasource) throws VieAppServiceException {
      this.logger.info("getEsModelData（标准服务） 方法开始响应");
      String where = "";

      try {
         String selects = "offLineTagId,offLineTagName,offLineTagInfo";
         where = where + "id = '" + id + "'";
         where = where + " and processed = 0";
         String sql = "select " + selects + " from " + datasource + " where " + where;
         this.logger.info("getEsModelData sql :" + sql);
         String url = DynamicEsSource.getEsSourceByType(datasource, "modelAnydrillAddress");
         ResultSet rs = this.excuteContext.executeSearchQuery(url, sql);
         List<LinkedHashMap<String, Object>> resultList = rs.toList();
         List<LinkedHashMap<String, Object>> listOne = new ArrayList<>();
         List<LinkedHashMap<String, Object>> listFinal = new ArrayList<>();

         for (LinkedHashMap<String, Object> lhmOne : resultList) {
            Iterator<Entry<String, Object>> iterator = lhmOne.entrySet().iterator();
            LinkedHashMap<String, Object> lhmTwo = new LinkedHashMap<>();

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

                  lhmTwo.put("offlineTagInfo", listOne);
               } else {
                  lhmTwo.put(entry.getKey().toString(), entry.getValue());
               }
            }

            listFinal.add(lhmTwo);
         }

         this.logger.info("getEsModelData（标准服务） 方法响应成功");
         return listFinal;
      } catch (Exception var23) {
         this.logger.error(var23.getMessage(), var23);
         throw new VieAppServiceException(var23.getMessage());
      }
   }

   public ESSearchResponse getEsModelData(ESSearchRequest esSearchRequest) throws VieAppServiceException {
      this.logger.info("getEsModelData（标准服务） 方法开始响应");
      String where = "";

      try {
         String selects = "offLineTagId,offLineTagName,offLineTagInfo";
         String table = esSearchRequest.getDataSource();
         where = where + "id = '" + esSearchRequest.getId() + "'";
         where = where + " and processed = 0";
         if (esSearchRequest.getPageSize() == null
            || esSearchRequest.getPageNum() == null
            || esSearchRequest.getPageSize() == 0L
            || esSearchRequest.getPageNum() == 0L) {
            esSearchRequest.setPageSize(10L);
            esSearchRequest.setPageNum(1L);
         }

         Long start = esSearchRequest.getPageSize() * (esSearchRequest.getPageNum() - 1L) + 1L;
         Long end = esSearchRequest.getPageSize() * esSearchRequest.getPageNum();
         where = where + " and rownum between " + start + " and " + end;
         String sql = "select " + selects + " from " + table + " where " + where;
         this.logger.info("getEsModelData sql :" + sql);
         String url = DynamicEsSource.getEsSourceByType(esSearchRequest.getDataSource(), "modelAnydrillAddress");
         ResultSet rs = this.excuteContext.executeSearchQuery(url, sql);
         List<LinkedHashMap<String, Object>> resultList = rs.toList();
         List<LinkedHashMap<String, Object>> listOne = new ArrayList<>();
         List<LinkedHashMap<String, Object>> listFinal = new ArrayList<>();

         for (LinkedHashMap<String, Object> lhmOne : resultList) {
            Iterator<Entry<String, Object>> iterator = lhmOne.entrySet().iterator();
            LinkedHashMap<String, Object> lhmTwo = new LinkedHashMap<>();

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

                  lhmTwo.put("offlineTagInfo", listOne);
               } else {
                  lhmTwo.put(entry.getKey().toString(), entry.getValue());
               }
            }

            listFinal.add(lhmTwo);
         }

         ESSearchResponse esSearchResponse = new ESSearchResponse(esSearchRequest.getPageNum(), esSearchRequest.getPageSize(), rs.getTotalCount());
         esSearchResponse.setValues(listFinal);
         this.logger.info("getEsModelData（标准服务） 方法响应成功");
         return esSearchResponse;
      } catch (Exception var24) {
         this.logger.error(var24.getMessage(), var24);
         throw new VieAppServiceException(var24.getMessage());
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
}
