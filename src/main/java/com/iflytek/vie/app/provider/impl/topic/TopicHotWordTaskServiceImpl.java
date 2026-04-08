package com.iflytek.vie.app.provider.impl.topic;

import com.iflytek.vie.app.api.dimension.FetchHotWordService;
import com.iflytek.vie.app.api.topic.TopicHotWordTaskService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.topic.HotWordRankResponse;
import com.iflytek.vie.app.pojo.topic.HotWordStateResponse;
import com.iflytek.vie.app.pojo.topic.HotWordTaskRequest;
import com.iflytek.vie.app.provider.common.CommonData;
import com.iflytek.vie.app.provider.database.TopicDB;
import com.iflytek.vie.app.provider.database.TopicHotWordStatDB;
import com.iflytek.vie.utils.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicHotWordTaskServiceImpl implements TopicHotWordTaskService {
   private final Logger logger = LoggerFactory.getLogger(TopicHotWordTaskServiceImpl.class);
   private CommonData commonData;
   private TopicHotWordStatDB topicHotWordStatDB;
   private TopicDB topicDB;
   private FetchHotWordService fetchHotWordService;

   public List<HotWordRankResponse> queryHotWordStat(HotWordTaskRequest hotWordTaskRequest) throws VieAppServiceException {
      List<HotWordStateResponse> list = null;
      List<HotWordRankResponse> result = new ArrayList<>();
      if (hotWordTaskRequest != null) {
         try {
            String systemId = hotWordTaskRequest.getSystemId();
            String userId = hotWordTaskRequest.getUserId();
            Long pathId = hotWordTaskRequest.getPathId();
            Long topicId = hotWordTaskRequest.getTopicId();
            String type = hotWordTaskRequest.getType();
            String order = hotWordTaskRequest.getOrder();
            String orderType = hotWordTaskRequest.getOrderType();
            String kwType = hotWordTaskRequest.getKwType();
            String keyWord = hotWordTaskRequest.getKeyword();
            Integer pageNum = hotWordTaskRequest.getPageNum();
            Integer pageSize = hotWordTaskRequest.getPageSize();
            Integer dataType = hotWordTaskRequest.getDataType();
            String batchId = hotWordTaskRequest.getBatchId();
            boolean isExport = hotWordTaskRequest.isIfExport();
            int ifLone = hotWordTaskRequest.getIfLone();
            String dataSource = hotWordTaskRequest.getDataSource();
            if (isExport) {
               pageNum = 0;
               pageSize = 0;
            }

            if (userId == null
               || "".equals(userId)
               || systemId == null
               || "".equals(systemId)
               || pathId == null
               || topicId == null
               || type == null
               || "".equals(type)
               || order == null
               || "".equals(order)
               || orderType == null
               || "".equals(orderType)
               || kwType == null
               || "".equals(kwType)
               || pageNum == null
               || pageSize == null) {
               this.logger.error("查询热词统计结果入参传递有误");
               throw new VieAppServiceException("查询热词统计结果入参传递有误");
            }

            if (dataType == null) {
               dataType = 2;
            }

            if (StringUtils.isNullOrEmpry(batchId)) {
               batchId = "0";
            }

            String sql = this.commonData.getDataSql(userId, systemId, dataSource);
            boolean showIncrement = false;
            Integer lastTotalCount = null;
            if (sql == null || "".equals(sql)) {
               this.logger.info("sql：{}", sql);
               return null;
            }

            List<String> dateList = this.topicHotWordStatDB.getDateList(pathId, sql, dataType, batchId, ifLone);
            if (dateList == null || dateList.size() == 0) {
               this.logger.error("没有查到数据");
               throw new VieAppServiceException("没有查到数据");
            }

            Map<String, Object> timeMap = this.topicDB.getTopicTimeType(topicId);
            if (timeMap != null) {
               List<String> valueList = (List<String>)timeMap.get("value");
               if (valueList != null && valueList.size() > 1 && valueList.get(0).equals(valueList.get(1))) {
                  showIncrement = true;
               }
            }

            if (showIncrement) {
               lastTotalCount = this.topicHotWordStatDB.getLastTotalCount(dateList.get(0));
            }

            Map<String, Object> map = new HashMap<>();
            map.put("pathId", pathId);
            map.put("type", type);
            map.put("order", order);
            map.put("orderType", orderType);
            map.put("kwType", kwType);
            map.put("keyWord", keyWord);
            if (lastTotalCount != null) {
               map.put("lastTotalCount", lastTotalCount);
            }

            map.put("pageNum", pageNum);
            map.put("pageSize", pageSize);
            map.put("dataAuth", sql);
            map.put("dateList", dateList);
            map.put("showIncrement", showIncrement);
            map.put("isExport", isExport);
            map.put("dataSource", dataSource);

            for (HotWordStateResponse h : this.fetchHotWordService.fetchHotWord(map)) {
               HotWordRankResponse hot = new HotWordRankResponse();
               BeanUtils.copyProperties(hot, h);
               result.add(hot);
            }
         } catch (Exception var29) {
            this.logger.error("[查询热词统计结果]失败", var29);
         }

         return result;
      } else {
         this.logger.error("参数传递有误");
         throw new VieAppServiceException("参数传递有误");
      }
   }

   public CommonData getCommonData() {
      return this.commonData;
   }

   public void setCommonData(CommonData commonData) {
      this.commonData = commonData;
   }

   public TopicHotWordStatDB getTopicHotWordStatDB() {
      return this.topicHotWordStatDB;
   }

   public void setTopicHotWordStatDB(TopicHotWordStatDB topicHotWordStatDB) {
      this.topicHotWordStatDB = topicHotWordStatDB;
   }

   public TopicDB getTopicDB() {
      return this.topicDB;
   }

   public void setTopicDB(TopicDB topicDB) {
      this.topicDB = topicDB;
   }

   public FetchHotWordService getFetchHotWordService() {
      return this.fetchHotWordService;
   }

   public void setFetchHotWordService(FetchHotWordService fetchHotWordService) {
      this.fetchHotWordService = fetchHotWordService;
   }
}
