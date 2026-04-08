package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkKeyWordProcess extends PreviewProcess implements Serializable {
   private static final long serialVersionUID = 6828192657253644079L;
   private final Logger logger = LoggerFactory.getLogger(MarkKeyWordProcess.class);
   private String[] anydrillHost;
   private PreviewIdParser previewIdParser;
   private List<DataInfo> dataInfos;
   private long fragmentId;
   private Map<String, List<Map<String, Object>>> silenceKeywordMap = new HashMap<>();

   public MarkKeyWordProcess(
      String textRule,
      int channel,
      List<Filter> filters,
      String tableName,
      List<DataInfo> dataInfos,
      long fragmentId,
      String[] anydrillHost,
      Map<String, List<Map<String, Object>>> silenceKeywordMap,
      String channelRule
   ) {
      if (anydrillHost != null) {
         this.anydrillHost = (String[])anydrillHost.clone();
      } else {
         this.anydrillHost = null;
      }

      this.dataInfos = dataInfos;
      this.previewIdParser = new PreviewIdParser(textRule, channel, filters, tableName, this.getIds(), channelRule);
      this.fragmentId = fragmentId;
      this.silenceKeywordMap = silenceKeywordMap;
   }

   public void process(String dataSource) {
      List<String> ids = this.getIds();
      if (ids != null && ids.size() != 0) {
         String sql = String.valueOf(this.previewIdParser.parseSql(dataSource));
         this.logger.info("mark keyword sql:" + sql);
         PreparedStatement pst = new PreparedStatement(this.anydrillHost);
         ResultSet rs = pst.executeQuery(sql);
         this.keyWordProcess(rs, dataSource);
      }
   }

   public List<String> getIds() {
      List<String> ids = new ArrayList<>();
      if (this.dataInfos != null && this.dataInfos.size() != 0) {
         for (DataInfo dataInfo : this.dataInfos) {
            ids.add(dataInfo.getId());
         }

         return ids;
      } else {
         return ids;
      }
   }

   public void keyWordProcess(ResultSet rs, String dataSource) {
      if (rs != null && rs.getDataMiningResult() != null) {
         Map<String, List<Map<String, Object>>> idsMap = this.previewResultProcess(rs, dataSource);
         if (idsMap != null && idsMap.size() != 0) {
            for (DataInfo dataInfo : this.dataInfos) {
               dataInfo.setFragmentId(this.fragmentId);
               List<Map<String, Object>> dataMap = idsMap.get(dataInfo.getId());
               if (this.silenceKeywordMap != null && this.silenceKeywordMap.keySet().contains(dataInfo.getId())) {
                  dataMap.addAll(this.silenceKeywordMap.get(dataInfo.getId()));
               }

               this.logger.info("dataInfo add keywordinfos");
               dataInfo.setKeywordInfos(dataMap);
            }

            this.logger.info("the dataInfos size is " + this.dataInfos.size());
         } else {
            this.logger.info("idsMap is null");
         }
      } else {
         this.logger.info("the search result is null");
      }
   }
}
