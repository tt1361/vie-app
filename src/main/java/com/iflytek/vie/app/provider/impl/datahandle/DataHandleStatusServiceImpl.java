package com.iflytek.vie.app.provider.impl.datahandle;

import com.iflytek.vie.app.api.datahandle.DataHandleRequest;
import com.iflytek.vie.app.api.datahandle.DataHandleStatusService;
import com.iflytek.vie.app.api.datahandle.TaskfileDetailInfo;
import com.iflytek.vie.app.api.datahandle.TaskfileHandleInfo;
import com.iflytek.vie.app.api.utils.DateUtils;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.provider.common.DataSourceInfo;
import com.iflytek.vie.app.provider.database.TaskFileHandleDB;
import com.iflytek.vie.dynamic.DynamicEsSource;
import com.iflytek.vie.utils.ExcuteContext;
import com.iflytek.vie.utils.StringUtils;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.anydrill.calculate.set.ResultSet;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataHandleStatusServiceImpl implements DataHandleStatusService {
   private final Logger logger = LoggerFactory.getLogger(DataHandleStatusServiceImpl.class);
   private TaskFileHandleDB taskFileHandleDB;
   private ExcuteContext excuteContext;
   private DataSourceInfo dataSourceInfo;

   public TaskfileHandleInfo getHandleInfoBytime(DataHandleRequest request) throws VieAppServiceException {
      String timeStr = request.getHandleTime();
      String dataSource = request.getDataSource();
      Date time = DateUtils.parseStringToDate(timeStr);
      if (StringUtils.isNullOrEmpry(dataSource)) {
         throw new VieAppServiceException("参数(dataSource)为空");
      } else {
         TaskfileHandleInfo TaskfileHandleInfo = this.taskFileHandleDB.getHandleInfoByTime(time, dataSource);
         if (TaskfileHandleInfo == null) {
            TaskfileHandleInfo = new TaskfileHandleInfo();
         }

         try {
            String sql = "select sum(idCounter) as idCounter_sum from " + dataSource + " where processed = 0 ";
            String url = DynamicEsSource.getEsSourceByType(dataSource, "mainAnydrillAddress");
            ExcuteContext es = new ExcuteContext();
            ResultSet resultSet = es.executeSearchQuery(url, sql);
            Long allTotalNum = resultSet != null ? new BigDecimal(((LinkedHashMap)resultSet.toList().get(0)).get("idCounter_sum").toString()).longValue() : 0L;
            TaskfileHandleInfo.setAllTotalNum(allTotalNum);
         } catch (Exception var11) {
            this.logger.error("服务内部错误", var11.getMessage());
         }

         return TaskfileHandleInfo;
      }
   }

   public Map<String, Object> getDetailInfoMap(DataHandleRequest request) {
      Map<String, Object> infoMap = new HashMap<>();
      List<TaskfileDetailInfo> infoList = this.getTaskfileDetailInfoList(request);
      if (CollectionUtils.isNotEmpty(infoList)) {
         for (TaskfileDetailInfo detailInfo : infoList) {
            if (detailInfo != null && detailInfo.getFailStep() != null) {
               switch (detailInfo.getFailStep()) {
                  case 0:
                     detailInfo.setFailReason("下载失败");
                     break;
                  case 1:
                     detailInfo.setFailReason("转码失败");
                     break;
                  case 2:
                     detailInfo.setFailReason("转写失败");
                     break;
                  case 3:
                     detailInfo.setFailReason("建索失败");
                     break;
                  default:
                     detailInfo.setFailReason("其他");
               }
            }
         }
      }

      infoMap.put("infoList", infoList);
      int totalCount = this.getTaskfileDetailInfoCount(request);
      infoMap.put("totalCount", totalCount);
      infoMap.put("pageNum", request.getPageNum());
      infoMap.put("pageSize", request.getPageSize());
      return infoMap;
   }

   public List<TaskfileDetailInfo> getTaskfileDetailInfoList(DataHandleRequest request) {
      int pageNum = request.getPageNum();
      int pageSize = request.getPageSize();
      Date time = DateUtils.parseStringToDate(request.getHandleTime());
      return this.taskFileHandleDB.getTaskfileDetailInfoList(pageNum, pageSize, time, request.getDataSource());
   }

   public int getTaskfileDetailInfoCount(DataHandleRequest request) {
      Date time = DateUtils.parseStringToDate(request.getHandleTime());
      return this.taskFileHandleDB.getTaskfileDetailInfoCount(time, request.getDataSource());
   }

   public TaskFileHandleDB getTaskFileHandleDB() {
      return this.taskFileHandleDB;
   }

   public void setTaskFileHandleDB(TaskFileHandleDB taskFileHandleDB) {
      this.taskFileHandleDB = taskFileHandleDB;
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
