package com.iflytek.vie.app.provider.impl.serial;

import com.iflytek.vie.app.api.dimension.DimensionService;
import com.iflytek.vie.app.api.serial.SerialService;
import com.iflytek.vie.app.exception.VieAppServiceException;
import com.iflytek.vie.app.pojo.common.Column;
import com.iflytek.vie.app.pojo.common.PagerResponse;
import com.iflytek.vie.app.pojo.common.ProcessResultEnum;
import com.iflytek.vie.app.pojo.dimension.DimensionConfig;
import com.iflytek.vie.app.pojo.dimension.KeyDimensionRequest;
import com.iflytek.vie.app.pojo.serial.SerialDetail;
import com.iflytek.vie.app.pojo.serial.SerialRequest;
import com.iflytek.vie.app.pojo.serial.SerialResponse;
import com.iflytek.vie.app.pojo.serial.SerialTask;
import com.iflytek.vie.app.provider.database.SerialDetailDB;
import com.iflytek.vie.app.provider.database.SerialTaskDB;
import com.iflytek.vie.utils.StringUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialServiceImpl implements SerialService {
   private final Logger logger = LoggerFactory.getLogger(SerialServiceImpl.class);
   private SerialTaskDB serialTaskDB;
   private SerialDetailDB serialDetailDB;
   private int totalCount;
   private DimensionService dimensionService;

   public boolean addSerial(SerialRequest serialRequest) throws VieAppServiceException {
      List<SerialDetail> list = serialRequest.getSerialDetailList();
      if (list != null && list.size() != 0) {
         boolean status = false;

         try {
            if (list.size() > this.getRestImportCount()) {
               this.logger.error("超过了当天的可导入数量，无法导入");
               throw new VieAppServiceException("超过了当天的可导入数量，无法导入");
            } else {
               Date date = new Date();
               String oldBatchId = this.serialTaskDB.getBatchId();
               String batchId = this.getBatchId(oldBatchId);
               SerialTask serialTask = new SerialTask();
               serialTask.setTotal(list.size());
               serialTask.setCreateTime(date);
               serialTask.setBatchId(batchId);
               status = this.serialTaskDB.addSerialTask(serialTask);
               List<SerialDetail> serialDetailList = new ArrayList<>();
               if (status) {
                  for (int i = 0; i < list.size(); i++) {
                     SerialDetail serialDetail = list.get(i);
                     serialDetail.setCreateTime(date);
                     serialDetail.setBatchId(batchId);
                     serialDetailList.add(serialDetail);
                  }
               }

               this.serialDetailDB.addSerialDetail(serialDetailList);
               return status;
            }
         } catch (Exception var11) {
            this.logger.error("导入维度信息调用addSerialDetail接口出错", var11);
            throw new VieAppServiceException("导入维度信息异常", var11);
         }
      } else {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      }
   }

   public PagerResponse<SerialTask> querySerialTask(SerialRequest serialRequest) throws VieAppServiceException {
      PagerResponse<SerialTask> pagerResponse = new PagerResponse();
      int pageSize = serialRequest.getPageSize() == 0 ? 15 : serialRequest.getPageSize();
      int pageNum = serialRequest.getPageNum() == 0 ? 1 : serialRequest.getPageNum();

      try {
         int totalRows = this.serialTaskDB.getSerialTaskCount();
         int totalPages = totalRows % pageSize == 0 ? totalRows / pageSize : totalRows / pageSize + 1;
         List<SerialTask> list = this.serialTaskDB.querySerialTask(pageSize, pageNum, serialRequest.getSortColumn(), serialRequest.getSortType());
         pagerResponse.setPageNum(pageNum);
         pagerResponse.setPageSize(pageSize);
         pagerResponse.setTotalPages(totalPages);
         pagerResponse.setTotalRows(totalRows);
         pagerResponse.setRows(list);
         return pagerResponse;
      } catch (Exception var8) {
         this.logger.error("查询已有维度更新记录调用querySerialTask接口出错", var8);
         throw new VieAppServiceException("查询已有维度更新记录异常", var8);
      }
   }

   public SerialResponse querySerialDetail(SerialRequest serialRequest) throws VieAppServiceException {
      if (!StringUtils.isNullOrEmpry(serialRequest.getBatchId()) && !StringUtils.isNullOrEmpry(serialRequest.getDataSource())) {
         int pageSize = serialRequest.getPageSize() == 0 ? 15 : serialRequest.getPageSize();
         int pageNum = serialRequest.getPageNum() == 0 ? 1 : serialRequest.getPageNum();
         SerialResponse serialResponse = new SerialResponse();

         try {
            int totalRows = this.serialDetailDB.getSerialDetailCount(serialRequest.getBatchId());
            int totalPages = totalRows % pageSize == 0 ? totalRows / pageSize : totalRows / pageSize + 1;
            List<SerialDetail> list = this.serialDetailDB.querySerialDetail(serialRequest.getBatchId(), pageSize, pageNum);
            List<Column> columnList = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();
            if (list != null && list.size() != 0) {
               for (int i = 0; i < list.size(); i++) {
                  Map<String, Object> columnMap = new HashMap<>();
                  SerialDetail serialDetail = list.get(i);
                  columnMap.put("serialId", serialDetail.getSerialId());
                  int processResult = serialDetail.getProcessResult();
                  String processResultInfo = ProcessResultEnum.getProcessResultInfo(processResult);
                  columnMap.put("failReason", processResultInfo);
                  JSONArray jsonArr = null;

                  try {
                     jsonArr = JSONArray.fromObject(serialDetail.getDimensions());
                  } catch (Exception var21) {
                     this.logger.error("维度dimensions数据异常", var21);
                     throw new VieAppServiceException("维度dimensions数据异常", var21);
                  }

                  String[] columnName = new String[jsonArr.size()];
                  String[] column = new String[jsonArr.size()];
                  String[] value = new String[jsonArr.size()];

                  for (int j = 0; j < jsonArr.size(); j++) {
                     columnName[j] = jsonArr.getJSONObject(j).getString("columnName");
                     column[j] = jsonArr.getJSONObject(j).getString("column");
                     value[j] = jsonArr.getJSONObject(j).getString("value");
                     if (StringUtils.isNullOrEmpry(columnName[j]) && StringUtils.isNullOrEmpry(column[j])) {
                        this.logger.error("查询的数据维度异常");
                        throw new VieAppServiceException("查询的数据维度异常");
                     }

                     Column columnPojo = new Column();
                     columnPojo.setColumn(column[j]);
                     columnPojo.setColumnName(columnName[j]);
                     if (i == 0) {
                        columnList.add(columnPojo);
                     }

                     columnMap.put(column[j], value[j]);
                  }

                  rows.add(columnMap);
               }

               Column serialIdColumn = new Column();
               KeyDimensionRequest keyDimensionRequest = new KeyDimensionRequest();
               keyDimensionRequest.setKey("voiceId");
               keyDimensionRequest.setDataSource(serialRequest.getDataSource());
               DimensionConfig dimensionConfig = this.dimensionService.getOneDimensionService(keyDimensionRequest);
               serialIdColumn.setColumnName(dimensionConfig.getShowName());
               serialIdColumn.setColumn("serialId");
               columnList.add(serialIdColumn);
               Column processResultColumn = new Column();
               processResultColumn.setColumnName("错误原因");
               processResultColumn.setColumn("failReason");
               columnList.add(processResultColumn);
               PagerResponse<Map<String, Object>> pagerResponse = new PagerResponse();
               pagerResponse.setPageNum(pageNum);
               pagerResponse.setPageSize(pageSize);
               pagerResponse.setTotalPages(totalPages);
               pagerResponse.setTotalRows(totalRows);
               pagerResponse.setRows(rows);
               serialResponse.setColums(columnList);
               serialResponse.setPagerResponse(pagerResponse);
               return serialResponse;
            } else {
               return null;
            }
         } catch (Exception var22) {
            this.logger.error("查询已有维度更新明细调用querySerialDetail接口出错", var22);
            throw new VieAppServiceException("查询已有维度更新明细异常", var22);
         }
      } else {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      }
   }

   public List<SerialDetail> downloadSerialDetail(SerialRequest serialRequest) throws VieAppServiceException {
      if (StringUtils.isNullOrEmpry(serialRequest.getBatchId())) {
         this.logger.error("请求参数异常");
         throw new VieAppServiceException("请求参数异常");
      } else {
         List<SerialDetail> list = null;

         try {
            return this.serialDetailDB.downloadSerialDetail(serialRequest.getBatchId());
         } catch (Exception var4) {
            this.logger.error("请求下载更新明细数据调用downloadSerialDetail接口出错", var4);
            throw new VieAppServiceException("请求下载更新明细数据异常", var4);
         }
      }
   }

   public int getRestImportCount() throws VieAppServiceException {
      int restImportCount = 0;

      try {
         int importCount = this.serialTaskDB.getImportCount();
         return this.totalCount - importCount;
      } catch (Exception var3) {
         this.logger.error("得到当天剩余可导入条数调用getRestImportCount接口出错", var3);
         throw new VieAppServiceException("得到当天剩余可导入条数异常", var3);
      }
   }

   private String getBatchId(String batchId) {
      Date date = new Date();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
      String time = sdf.format(date);
      if (StringUtils.isNullOrEmpry(batchId)) {
         batchId = time + "001";
      } else {
         String strTime = batchId.substring(0, 14);
         int num = Integer.parseInt(batchId.substring(14, 17));
         if (!time.equals(strTime)) {
            batchId = time + "001";
         } else if (num < 10) {
            batchId = time + "00" + ++num;
         } else if (num < 100) {
            batchId = time + "0" + ++num;
         } else {
            batchId = time + ++num;
         }
      }

      return batchId;
   }

   public void setSerialTaskDB(SerialTaskDB serialTaskDB) {
      this.serialTaskDB = serialTaskDB;
   }

   public void setSerialDetailDB(SerialDetailDB serialDetailDB) {
      this.serialDetailDB = serialDetailDB;
   }

   public void setTotalCount(int totalCount) {
      this.totalCount = totalCount;
   }

   public void setDimensionService(DimensionService dimensionService) {
      this.dimensionService = dimensionService;
   }
}
