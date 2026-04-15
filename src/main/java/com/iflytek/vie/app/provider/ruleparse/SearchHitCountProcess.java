package com.iflytek.vie.app.provider.ruleparse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SearchHitCountProcess implements Serializable {
   private static final long serialVersionUID = -3264100642774905486L;
   private String[] anydrillHosts;
   private List<Map<String, Object>> modelFragments;
   private String tableName;
   private int channel;
   private List<FilterPlatform> filters;
   private long timeOut;
   private BlockingQueue<Map<String, Long>> retQuene = new LinkedBlockingQueue<>();
   private List<Map<String, Long>> showMap = new ArrayList<>();
   private String dataLimitSql;
   private int searchType;
   private ConcurrentHashMap<Long, Map<String, Long>> fragmentIdsMap = new ConcurrentHashMap<>();
   private Set<Long> fragmentIdSet = new TreeSet<>();

   public SearchHitCountProcess(
      String[] anydrillHosts,
      List<Map<String, Object>> modelFragments,
      String tableName,
      int channel,
      List<FilterPlatform> filters,
      long timeOut,
      String dataLimit,
      int searchType
   ) {
      this.anydrillHosts = anydrillHosts;
      this.modelFragments = modelFragments;
      this.tableName = tableName;
      this.channel = channel;
      this.filters = filters;
      this.timeOut = timeOut;
      this.dataLimitSql = dataLimit;
      this.searchType = searchType;
   }

   public void process(String dataSource) throws InterruptedException {
      List<Map<String, Object>> fragmentsMap = this.modelFragments;
      if (fragmentsMap != null && fragmentsMap.size() != 0) {
         int fragmentCount = fragmentsMap.size();

         for (Map<String, Object> fragmentMap : (List<Map<String, Object>>)fragmentsMap) {
            Map<String, Long> retMap = new HashMap<>();
            String textRule = String.valueOf(fragmentMap.get("fragmentContent"));
            String otherRuleText = String.valueOf(fragmentMap.get("otherRuleText"));
            HitCountParserPlatform hitCountSqlParser = new HitCountParserPlatform(
               textRule,
               Integer.parseInt(fragmentMap.get("channel").toString()),
               this.filters,
               this.tableName,
               false,
               this.dataLimitSql,
               otherRuleText,
               this.searchType
            );
            long fragmentNum = Long.parseLong(String.valueOf(fragmentMap.get("fragmentNum")));
            long fragmentId = Long.parseLong(String.valueOf(fragmentMap.get("fragmentId")));
            retMap.put("fragmentId", fragmentId);
            retMap.put("fragmentNum", fragmentNum);
            this.fragmentIdSet.add(fragmentId);
            this.fragmentIdsMap.put(fragmentId, retMap);
            SearchHitCountTask searchHitCountTask = new SearchHitCountTask(hitCountSqlParser, this.anydrillHosts, retMap, this.retQuene, dataSource);
            ThreadPoolContain.searchHitThreadPool.submitTask(searchHitCountTask);
         }

         long start = System.currentTimeMillis();
         int count = 0;

         while (true) {
            Map<String, Long> countMap = this.retQuene.poll(5000L, TimeUnit.MILLISECONDS);
            if (countMap != null) {
               if (++count == fragmentCount) {
                  return;
               }
            }

            long end = System.currentTimeMillis();
            if (end - start >= this.timeOut) {
               throw new ModelRunException("timeOut");
            }

            Thread.sleep(500L);
         }
      }
   }

   public List<Map<String, Long>> getRet() {
      for (Long fragmentId : this.fragmentIdSet) {
         this.showMap.add(this.fragmentIdsMap.get(fragmentId));
      }

      return this.showMap;
   }

   public String[] getAnydrillHosts() {
      return this.anydrillHosts;
   }

   public void setAnydrillHosts(String[] anydrillHosts) {
      this.anydrillHosts = anydrillHosts;
   }

   public String getTableName() {
      return this.tableName;
   }

   public void setTableName(String tableName) {
      this.tableName = tableName;
   }

   public int getChannel() {
      return this.channel;
   }

   public void setChannel(int channel) {
      this.channel = channel;
   }

   public List<FilterPlatform> getFilters() {
      return this.filters;
   }

   public void setFilters(List<FilterPlatform> filters) {
      this.filters = filters;
   }

   public long getTimeOut() {
      return this.timeOut;
   }

   public void setTimeOut(long timeOut) {
      this.timeOut = timeOut;
   }

   public BlockingQueue<Map<String, Long>> getRetQuene() {
      return this.retQuene;
   }

   public void setRetQuene(BlockingQueue<Map<String, Long>> retQuene) {
      this.retQuene = retQuene;
   }

   public List<Map<String, Long>> getShowMap() {
      return this.showMap;
   }

   public void setShowMap(List<Map<String, Long>> showMap) {
      this.showMap = showMap;
   }

   public ConcurrentHashMap<Long, Map<String, Long>> getFragmentIdsMap() {
      return this.fragmentIdsMap;
   }

   public void setFragmentIdsMap(ConcurrentHashMap<Long, Map<String, Long>> fragmentIdsMap) {
      this.fragmentIdsMap = fragmentIdsMap;
   }

   public Set<Long> getFragmentIdSet() {
      return this.fragmentIdSet;
   }

   public void setFragmentIdSet(Set<Long> fragmentIdSet) {
      this.fragmentIdSet = fragmentIdSet;
   }
}
