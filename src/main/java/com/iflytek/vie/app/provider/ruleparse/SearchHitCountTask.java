package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.anydrill.calculate.set.ResultSet;
import org.anydrill.netty.client.PreparedStatement;

public class SearchHitCountTask extends RunProcess implements Serializable {
   private static final long serialVersionUID = -4586483223347192038L;
   private HitCountParserPlatform hitCountSqlParser;
   private String[] anydrillHosts;
   private Map<String, Long> countMap;
   private BlockingQueue<Map<String, Long>> retQuene;
   private String dataSource;

   public SearchHitCountTask(
      HitCountParserPlatform hitCountSqlParser,
      String[] anydrillHosts,
      Map<String, Long> countMap,
      BlockingQueue<Map<String, Long>> retQuene,
      String dataSource
   ) {
      this.hitCountSqlParser = hitCountSqlParser;
      this.anydrillHosts = anydrillHosts;
      this.countMap = countMap;
      this.retQuene = retQuene;
      this.dataSource = dataSource;
   }

   @Override
   public Object call() throws Exception {
      try {
         PreparedStatement pst = new PreparedStatement(this.anydrillHosts);
         String sql = String.valueOf(this.hitCountSqlParser.parseSql(this.dataSource));
         System.out.println("the pianduan sql is : " + sql);
         ResultSet rs = pst.executeQuery(sql);
         if (rs == null || rs.getTotalCount() == 0L) {
            this.countMap.put("count", 0L);
            this.retQuene.put(this.countMap);
            return null;
         }

         if (rs.hasRow()) {
            rs.next();
            long hitCount = rs.getValue("hitCount") == null ? 0L : (long)((Double)rs.getValue("hitCount")).doubleValue();
            this.countMap.put("count", hitCount);
            this.retQuene.put(this.countMap);
            return null;
         }
      } catch (Exception var6) {
         this.countMap.put("count", 0L);
         this.retQuene.put(this.countMap);
      }

      return null;
   }

   public HitCountParserPlatform getHitCountSqlParser() {
      return this.hitCountSqlParser;
   }

   public void setHitCountSqlParser(HitCountParserPlatform hitCountSqlParser) {
      this.hitCountSqlParser = hitCountSqlParser;
   }

   public String[] getAnydrillHosts() {
      return this.anydrillHosts;
   }

   public void setAnydrillHosts(String[] anydrillHosts) {
      this.anydrillHosts = anydrillHosts;
   }

   public BlockingQueue<Map<String, Long>> getRetQuene() {
      return this.retQuene;
   }

   public void setRetQuene(BlockingQueue<Map<String, Long>> retQuene) {
      this.retQuene = retQuene;
   }

   public Map<String, Long> getCountMap() {
      return this.countMap;
   }

   public void setCountMap(Map<String, Long> countMap) {
      this.countMap = countMap;
   }

   public void setDataSource(String dataSource) {
      this.dataSource = dataSource;
   }

   public String getDataSource() {
      return this.dataSource;
   }
}
