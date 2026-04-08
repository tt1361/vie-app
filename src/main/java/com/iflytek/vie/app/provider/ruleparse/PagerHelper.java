package com.iflytek.vie.app.provider.ruleparse;

import com.iflytek.vie.app.pojo.common.PagerResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PagerHelper implements Serializable {
   private static final long serialVersionUID = -5641202766859584033L;

   public static void setPagerInfo(ResultTable pager, int totalSize, int totalNum, List list, int pageNum, int pageSize) {
      pager.setTotalRows(totalSize);
      pager.setTotalPages(totalNum);
      pager.setPageNum(pageNum);
      pager.setPageSize(pageSize);
      pager.setRows(list);
   }

   public static void setPagerInfo2(PagerResponse pager, int totalRows, int totalPages, List rows, int pageNum, int pageSize) {
      pager.setPageNum(pageNum);
      pager.setPageSize(pageSize);
      pager.setRows(rows);
      pager.setTotalPages(totalPages);
      pager.setTotalRows(totalRows);
   }

   public static int caculateTotalNum(int pageSize, int totalCount) {
      return totalCount % pageSize == 0 ? totalCount / pageSize : totalCount / pageSize + 1;
   }

   public static List subList(List list, int pageNum, int pageSize) {
      if (list != null && list.size() != 0) {
         return list.size() < pageNum * pageSize
            ? list.subList((pageNum - 1) * pageSize, list.size())
            : list.subList((pageNum - 1) * pageSize, pageNum * pageSize);
      } else {
         return new ArrayList();
      }
   }
}
