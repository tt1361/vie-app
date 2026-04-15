package com.iflytek.vie.app.provider.impl.dimension;

import com.iflytek.vie.app.api.dimension.FetchHotWordService;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.dimension.HotWordRankResponse;
import com.iflytek.vie.app.pojo.topic.HotWordStateResponse;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.internal.SessionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchHotWordServiceImpl implements FetchHotWordService {
   private final Logger logger = LoggerFactory.getLogger(FetchHotWordServiceImpl.class);
   private HibernateTemplateUtils hibernateTemplate;
   DecimalFormat df = new DecimalFormat("########0.00");

   public List<HotWordStateResponse> fetchHotWord(Map<String, Object> map) throws ViePlatformServiceException {
      List<HotWordStateResponse> listHashMap = null;
      Connection conn = null;
      CallableStatement queryList = null;
      ResultSet rs = null;
      int lastTotalCount = 0;
      if (map != null) {
         try {
            String keyword = String.valueOf(map.get("keyWord"));
            String type = String.valueOf(map.get("type"));
            String kwType = String.valueOf(map.get("kwType"));
            String order = String.valueOf(map.get("order"));
            String orderType = String.valueOf(map.get("orderType"));
            Long pathId = (Long)map.get("pathId");
            String dataAuth = String.valueOf(map.get("dataAuth"));
            int pageNum = Integer.parseInt(String.valueOf(map.get("pageNum")));
            int pageSize = Integer.parseInt(String.valueOf(map.get("pageSize")));
            boolean showIncrement = (Boolean)map.get("showIncrement");
            boolean isExport = (Boolean)map.get("isExport");
            if (map.get("lastTotalCount") != null) {
               lastTotalCount = Integer.parseInt(String.valueOf(map.get("lastTotalCount")));
            }

            List<String> dateList = (List<String>)map.get("dateList");
            String dataSource = String.valueOf(map.get("dataSource"));
            String sql = this.getfetchHotWordSql(
               isExport, keyword, type, kwType, order, orderType, pathId, dataAuth, pageNum, pageSize, showIncrement, lastTotalCount, dateList, dataSource
            );
            conn = ((SessionFactoryImpl)this.hibernateTemplate.getSessionFactory()).getConnectionProvider().getConnection();
            queryList = conn.prepareCall(sql);
            queryList.execute();
            this.logger.info("queryHotWordStat-> fetchHotWord sql: " + sql);
            rs = queryList.getResultSet();
            listHashMap = new ArrayList<>();
            String computerCol = "kwpercent";
            computerCol = "voice".equals(kwType) ? "kwfilepercent" : computerCol;

            while (rs.next()) {
               HotWordStateResponse hotWordStateResponse = new HotWordStateResponse();
               hotWordStateResponse.setHotVocabulary(rs.getString("keyword"));
               hotWordStateResponse.setIncrement(rs.getInt("kwrange"));
               hotWordStateResponse.setRate(this.df.format(rs.getDouble(computerCol) * 100.0) + "%");
               hotWordStateResponse.setRn(rs.getInt("RN"));
               hotWordStateResponse.setTotalCount(rs.getInt("totalCount"));
               listHashMap.add(hotWordStateResponse);
            }
         } catch (Exception var30) {
            this.logger.error("查询热词db类异常1");
            throw new ViePlatformServiceException("查询热词db类异常1", var30);
         } finally {
            try {
               if (rs != null) {
                  rs.close();
               }

               if (conn != null) {
                  conn.close();
               }

               if (queryList != null) {
                  queryList.close();
               }
            } catch (SQLException var29) {
               this.logger.error("查询热词db类异常2");
               throw new ViePlatformServiceException("查询热词db类异常2", var29);
            }
         }

         return listHashMap;
      } else {
         throw new ViePlatformServiceException("参数传递有误");
      }
   }

   public List<HotWordRankResponse> fetchCallHotWord(Map<String, Object> map) throws ViePlatformServiceException {
      List<HotWordRankResponse> listHashMap = null;
      Connection conn = null;
      CallableStatement queryList = null;
      ResultSet rs = null;
      if (map != null) {
         try {
            Long taskId = (Long)map.get("taskId");
            String wordType = String.valueOf(map.get("wordType"));
            String countType = String.valueOf(map.get("countType"));
            String keyword = String.valueOf(map.get("keyWord"));
            int pageNum = Integer.parseInt(String.valueOf(map.get("pageNum")));
            int pageSize = Integer.parseInt(String.valueOf(map.get("pageSize")));
            String sql = this.getfetchCallHotWordSql(taskId, wordType, countType, keyword, pageNum, pageSize);
            conn = ((SessionFactoryImpl)this.hibernateTemplate.getSessionFactory()).getConnectionProvider().getConnection();
            queryList = conn.prepareCall(sql);
            queryList.execute();
            rs = queryList.getResultSet();
            listHashMap = new ArrayList<>();
            String computerCol = "kwpercent";
            computerCol = "voice".equals(countType) ? "kwfilepercent" : computerCol;

            while (rs.next()) {
               HotWordRankResponse hotWordRankResponse = new HotWordRankResponse();
               hotWordRankResponse.setHotVocabulary(rs.getString("keyword"));
               hotWordRankResponse.setRate(this.df.format(rs.getDouble(computerCol) * 100.0) + "%");
               hotWordRankResponse.setRn(rs.getInt("RN"));
               hotWordRankResponse.setTotalCount(rs.getInt("totalCount"));
               listHashMap.add(hotWordRankResponse);
            }
         } catch (Exception var22) {
            this.logger.error("【通过任务id查询任务的热词分析结果】失败", var22);
            throw new ViePlatformServiceException("服务器内部错误", var22);
         } finally {
            try {
               if (rs != null) {
                  rs.close();
               }

               if (queryList != null) {
                  queryList.close();
               }

               if (conn != null) {
                  conn.close();
               }
            } catch (SQLException var21) {
               this.logger.error("查询热词db类异常");
               throw new ViePlatformServiceException("查询热词db类异常", var21);
            }
         }

         return listHashMap;
      } else {
         throw new ViePlatformServiceException("参数传递有误");
      }
   }

   private String getfetchHotWordSql(
      boolean isExport,
      String keyword,
      String type,
      String kwType,
      String order,
      String orderType,
      long pathId,
      String dataAuth,
      int pageNum,
      int pageSize,
      boolean showIncrement,
      Integer lastTotalCount,
      List<String> dateList,
      String dataSource
   ) {
      String sql = "";
      dataAuth = dataAuth.replace("'", "''");

      try {
         String computerCol = "kwpercent";
         computerCol = "voice".equals(kwType) ? "kwfilepercent" : computerCol;
         String sumCol = "sum(kwcount)/sum(totalkwcount)";
         sumCol = "voice".equals(kwType) ? "sum(kwfilecount)/sum(totalfilekwcount)" : sumCol;
         int startNum = (pageNum - 1) * pageSize;
         int endNum = pageNum * pageSize;
         String kwdCond = "";
         kwdCond = !StringUtils.isNullOrEmpry(keyword) ? "keyword like'%" + keyword + "%'" : "1=1";
         String kwTypeCond = "focusKwd".equals(type) ? " and keyword in(select keyword from FOCUSHOTWORD where datasource='" + dataSource + "') " : " ";
         if (isExport) {
            sql = this.getfetchHotWordSqlForExport(
               kwType, order, orderType, pathId, dataAuth, lastTotalCount, dateList, computerCol, sumCol, kwdCond, kwTypeCond, dataSource
            );
         } else if (lastTotalCount != null && dateList.size() > 1) {
            String lastday = dateList.get(0);
            String beforelastDay = dateList.get(1);
            String orderCol = null;
            if (DBSelectUtil.isMysql()) {
               if ("range".equals(orderType)) {
                  orderCol = "abs(kwrange) " + order + ", A." + computerCol + " desc";
               } else {
                  orderCol = "A." + computerCol + " " + order + ", abs(kwrange) desc";
               }

               sql = "(select * from(select (@rownum_a := @rownum_a+1) as RN,A.*,0 as kwrange  from (select keyword, "
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + "          from TOPIC_HOTWORD_STAT "
                  + "   where statdate = '"
                  + lastday
                  + "' "
                  + " and data_auth = '"
                  + dataAuth
                  + "' "
                  + " and path_id = '"
                  + pathId
                  + "'"
                  + kwTypeCond
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + "  group by keyword, statdate order by "
                  + computerCol
                  + " desc, keyword) A ,(select (@rownum_a :=0) a )a1)B)C";
               sql = "select D.*  from (select (@rownum_c := @rownum_c+1) as TRN, C.*,(select count(0) from "
                  + sql
                  + " where "
                  + kwdCond
                  + " and  keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "')) as totalCount"
                  + "  from "
                  + sql
                  + ",(select (@rownum_c :=0) c )c1 where "
                  + kwdCond
                  + ")D where TRN>"
                  + startNum
                  + "  and TRN<="
                  + endNum
                  + " order by RN";
            } else if (DBSelectUtil.isSqlServer()) {
               if ("range".equals(orderType)) {
                  orderCol = "kwrange " + order + ", " + computerCol + " desc, keyword";
               } else {
                  orderCol = computerCol + " " + order + ", kwrange desc, keyword";
               }

               sql = "with A as (select row_number() over(order by "
                  + computerCol
                  + " DESC) as RN,T.* "
                  + "from"
                  + "( select top 100 PERCENT keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from TOPIC_HOTWORD_STAT "
                  + "where statdate='"
                  + lastday
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + kwTypeCond
                  + " group by keyword, statdate "
                  + " order by "
                  + computerCol
                  + " desc) T ), "
                  + "B as "
                  + "(select row_number() over(order by "
                  + computerCol
                  + " DESC) as RN,T.*  "
                  + "from "
                  + "( select top 100 PERCENT keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from TOPIC_HOTWORD_STAT "
                  + "where statdate='"
                  + beforelastDay
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + kwTypeCond
                  + "  group by keyword, statdate "
                  + " order by "
                  + computerCol
                  + " desc) T),"
                  + "C as (select * from( "
                  + "select top 100 PERCENT A.RN, abs(isnull(B.RN-A.RN,"
                  + lastTotalCount
                  + "-A.RN+1)) as kwrange,A.keyword,A."
                  + computerCol
                  + " "
                  + "from A left join B on A.keyword=B.keyword "
                  + "where A.keyword not in(select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') and "
                  + ("1=1".equals(kwdCond) ? kwdCond : "A." + kwdCond)
                  + " order by "
                  + orderCol
                  + ") T) "
                  + "select * from(select row_number() over(order by "
                  + orderCol
                  + ") as TRN,C.*,(select count(0) from C) as totalCount from C ) T2 where T2.TRN>"
                  + startNum
                  + " and T2.TRN<="
                  + endNum;
            } else {
               if ("range".equals(orderType)) {
                  orderCol = "abs(kwrange) " + order + ", A." + computerCol + " desc";
               } else {
                  orderCol = "A." + computerCol + " " + order + ", abs(kwrange) desc";
               }

               sql = "with A as (select rownum as RN,T.* from( select keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from TOPIC_HOTWORD_STAT "
                  + "where statdate='"
                  + lastday
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + kwTypeCond
                  + " group by keyword, statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T ), "
                  + "B as "
                  + "(select rownum as RN,T.*  "
                  + "from "
                  + "( select keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from TOPIC_HOTWORD_STAT "
                  + "where statdate='"
                  + beforelastDay
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + kwTypeCond
                  + "  group by keyword, statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T),"
                  + "C as (select * from( "
                  + "select  A.RN, nvl(B.RN-A.RN,"
                  + lastTotalCount
                  + "-A.RN+1) as kwrange,A.keyword,A."
                  + computerCol
                  + " "
                  + "from A left join B on A.keyword=B.keyword "
                  + "where A.keyword not in(select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') and "
                  + ("1=1".equals(kwdCond) ? kwdCond : "A." + kwdCond)
                  + " order by "
                  + orderCol
                  + ")) "
                  + "select * from(select rownum as TRN,C.*,(select count(0) from C) as totalCount from C )where TRN>"
                  + startNum
                  + " and TRN<="
                  + endNum;
            }
         } else {
            String computerPer = "sum(hw.kwcount) / sum(hw.totalkwcount)";
            computerPer = "voice".equals(kwType) ? "sum(hw.kwfilecount) / sum(hw.totalfilekwcount)" : computerPer;
            if (DBSelectUtil.isMysql()) {
               sql = "(select * from(select (@rownum_a := @rownum_a+1) as RN,A.*,0 as kwrange  from (select keyword, "
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + "          from TOPIC_HOTWORD_STAT "
                  + "   where statdate = "
                  + dateList.get(0)
                  + " and data_auth = '"
                  + dataAuth
                  + "' "
                  + " and path_id = '"
                  + pathId
                  + "'"
                  + kwTypeCond
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + "  group by keyword, statdate order by "
                  + computerCol
                  + " desc, keyword) A ,(select (@rownum_a :=0) a )a1)B)C";
               sql = "select D.*  from (select (@rownum_c := @rownum_c+1) as TRN, C.*,(select count(0) from "
                  + sql
                  + " where "
                  + kwdCond
                  + " and  keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "')) as totalCount"
                  + "  from "
                  + sql
                  + ",(select (@rownum_c :=0) c )c1 where "
                  + kwdCond
                  + ")D where TRN>"
                  + startNum
                  + "  and TRN<="
                  + endNum
                  + " order by RN";
            } else if (DBSelectUtil.isSqlServer()) {
               sql = "select * from(select row_number() over(order by "
                  + computerCol
                  + " "
                  + order
                  + ") as RN,A.*,0 as kwrange"
                  + "  from (select top 100 PERCENT keyword,"
                  + "              "
                  + computerPer
                  + " as "
                  + computerCol
                  + ""
                  + "          from TOPIC_HOTWORD_STAT hw"
                  + "         where statdate = '"
                  + dateList.get(0)
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + kwTypeCond
                  + "         group by keyword "
                  + "         order by "
                  + computerCol
                  + " "
                  + order
                  + ") A) t ";
               sql = "with A as ("
                  + sql
                  + ") select *"
                  + "  from (select row_number() over(order by "
                  + computerCol
                  + " "
                  + order
                  + ", keyword) as TRN, A.*,(select count(0) from A where "
                  + kwdCond
                  + ") as totalCount"
                  + "  from A where "
                  + kwdCond
                  + ") t2 where TRN>"
                  + startNum
                  + " and TRN<="
                  + endNum;
            } else {
               sql = "select * from(select rownum as RN,A.*,0 as kwrange  from (select keyword,              "
                  + computerPer
                  + " as "
                  + computerCol
                  + ""
                  + "          from TOPIC_HOTWORD_STAT hw"
                  + "         where statdate = '"
                  + dateList.get(0)
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + kwTypeCond
                  + "         group by keyword "
                  + "         order by "
                  + computerCol
                  + " "
                  + order
                  + ", keyword) A)";
               sql = "with A as ("
                  + sql
                  + ") select *"
                  + "  from (select rownum as TRN, A.*,(select count(0) from A where "
                  + kwdCond
                  + ") as totalCount"
                  + "  from (A) where "
                  + kwdCond
                  + ")where TRN>"
                  + startNum
                  + " and TRN<="
                  + endNum;
            }
         }
      } catch (Exception var26) {
         var26.printStackTrace();
      }

      return sql;
   }

   private String getfetchHotWordSqlForExport(
      String kwType,
      String order,
      String orderType,
      long pathId,
      String dataAuth,
      Integer lastTotalCount,
      List<String> dateList,
      String computerCol,
      String sumCol,
      String kwdCond,
      String kwTypeCond,
      String dataSource
   ) {
      String sql = "";

      try {
         if (lastTotalCount != null && dateList.size() > 1) {
            String lastday = dateList.get(0);
            String beforelastDay = dateList.get(1);
            String orderCol = null;
            if (DBSelectUtil.isMysql()) {
               if ("range".equals(orderType)) {
                  orderCol = "abs(kwrange) " + order + ", A." + computerCol + " desc";
               } else {
                  orderCol = "A." + computerCol + " " + order + ", abs(kwrange) desc";
               }

               sql = "(select * from(select (@rownum_a := @rownum_a+1) as RN,A.*,0 as kwrange  from (select keyword, "
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + "          from TOPIC_HOTWORD_STAT "
                  + "   where statdate = '"
                  + lastday
                  + "' "
                  + " and data_auth = '"
                  + dataAuth
                  + "' "
                  + " and path_id = '"
                  + pathId
                  + "'"
                  + kwTypeCond
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + "  group by keyword, statdate order by "
                  + computerCol
                  + " desc, keyword) A ,(select (@rownum_a :=0) a )a1)B)C";
               sql = "select D.*  from (select (@rownum_c := @rownum_c+1) as TRN, C.*,(select count(0) from "
                  + sql
                  + " where "
                  + kwdCond
                  + " and  keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "')) as totalCount"
                  + "  from "
                  + sql
                  + ",(select (@rownum_c :=0) c )c1 where "
                  + kwdCond
                  + ")D order by RN";
            } else if (DBSelectUtil.isSqlServer()) {
               if ("range".equals(orderType)) {
                  orderCol = "kwrange " + order + ", " + computerCol + " desc, keyword";
               } else {
                  orderCol = computerCol + " " + order + ", kwrange desc, keyword";
               }

               sql = "with A as (select row_number() over(order by "
                  + computerCol
                  + " DESC) as RN,T.* "
                  + "from"
                  + "( select top 100 PERCENT keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from TOPIC_HOTWORD_STAT "
                  + "where statdate='"
                  + lastday
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + kwTypeCond
                  + " group by keyword, statdate "
                  + " order by "
                  + computerCol
                  + " desc) T ), "
                  + "B as "
                  + "(select row_number() over(order by "
                  + computerCol
                  + " DESC) as RN,T.*  "
                  + "from "
                  + "( select top 100 PERCENT keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from TOPIC_HOTWORD_STAT "
                  + "where statdate='"
                  + beforelastDay
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + kwTypeCond
                  + "  group by keyword, statdate "
                  + " order by "
                  + computerCol
                  + " desc) T),"
                  + "C as (select * from( "
                  + "select top 100 PERCENT A.RN, abs(isnull(B.RN-A.RN,"
                  + lastTotalCount
                  + "-A.RN+1)) as kwrange,A.keyword,A."
                  + computerCol
                  + " "
                  + "from A left join B on A.keyword=B.keyword "
                  + "where A.keyword not in(select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') and "
                  + ("1=1".equals(kwdCond) ? kwdCond : "A." + kwdCond)
                  + " order by "
                  + orderCol
                  + ") T) "
                  + "select * from(select row_number() over(order by "
                  + orderCol
                  + ") as TRN,C.*,(select count(0) from C) as totalCount from C ) T2 ";
            } else {
               if ("range".equals(orderType)) {
                  orderCol = "abs(kwrange) " + order + ", A." + computerCol + " desc";
               } else {
                  orderCol = "A." + computerCol + " " + order + ", abs(kwrange) desc";
               }

               sql = "with A as (select rownum as RN,T.* from( select keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from TOPIC_HOTWORD_STAT "
                  + "where statdate='"
                  + lastday
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + kwTypeCond
                  + " group by keyword, statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T ), "
                  + "B as "
                  + "(select rownum as RN,T.*  "
                  + "from "
                  + "( select keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from TOPIC_HOTWORD_STAT "
                  + "where statdate='"
                  + beforelastDay
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + kwTypeCond
                  + "  group by keyword, statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T),"
                  + "C as (select * from( "
                  + "select  A.RN, nvl(B.RN-A.RN,"
                  + lastTotalCount
                  + "-A.RN+1) as kwrange,A.keyword,A."
                  + computerCol
                  + " "
                  + "from A left join B on A.keyword=B.keyword "
                  + "where A.keyword not in(select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') and "
                  + ("1=1".equals(kwdCond) ? kwdCond : "A." + kwdCond)
                  + " order by "
                  + orderCol
                  + ")) "
                  + "select * from(select rownum as TRN,C.*,(select count(0) from C) as totalCount from C )";
            }
         } else {
            String computerPer = "sum(hw.kwcount) / sum(hw.totalkwcount)";
            computerPer = "voice".equals(kwType) ? "sum(hw.kwfilecount) / sum(hw.totalfilekwcount)" : computerPer;
            if (DBSelectUtil.isMysql()) {
               sql = "(select * from(select (@rownum_a := @rownum_a+1) as RN,A.*,0 as kwrange  from (select keyword, "
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + "          from TOPIC_HOTWORD_STAT "
                  + "   where statdate = "
                  + dateList.get(0)
                  + " and data_auth = '"
                  + dataAuth
                  + "' "
                  + " and path_id = '"
                  + pathId
                  + "'"
                  + kwTypeCond
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + "  group by keyword, statdate order by "
                  + computerCol
                  + " desc, keyword) A ,(select (@rownum_a :=0) a )a1)B)C";
               sql = "select D.*  from (select (@rownum_c := @rownum_c+1) as TRN, C.*,(select count(0) from "
                  + sql
                  + " where "
                  + kwdCond
                  + " and  keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "')) as totalCount"
                  + "  from "
                  + sql
                  + ",(select (@rownum_c :=0) c )c1 where "
                  + kwdCond
                  + ")D order by RN";
            } else if (DBSelectUtil.isSqlServer()) {
               sql = "select * from(select row_number() over(order by "
                  + computerCol
                  + " "
                  + order
                  + ") as RN,A.*,0 as kwrange"
                  + "  from (select top 100 PERCENT keyword,"
                  + "              "
                  + computerPer
                  + " as "
                  + computerCol
                  + ""
                  + "          from TOPIC_HOTWORD_STAT hw"
                  + "         where statdate = '"
                  + dateList.get(0)
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + kwTypeCond
                  + "         group by keyword "
                  + "         order by "
                  + computerCol
                  + " "
                  + order
                  + ") A) t ";
               sql = "with A as ("
                  + sql
                  + ") select *"
                  + "  from (select row_number() over(order by "
                  + computerCol
                  + " "
                  + order
                  + ", keyword) as TRN, A.*,(select count(0) from A where "
                  + kwdCond
                  + ") as totalCount"
                  + "  from A where "
                  + kwdCond
                  + ") t2 ";
            } else {
               sql = "select * from(select rownum as RN,A.*,0 as kwrange  from (select keyword,              "
                  + computerPer
                  + " as "
                  + computerCol
                  + ""
                  + "          from TOPIC_HOTWORD_STAT hw"
                  + "         where statdate = '"
                  + dateList.get(0)
                  + "' and data_auth = '"
                  + dataAuth
                  + "' and path_id = '"
                  + pathId
                  + "' "
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + kwTypeCond
                  + "         group by keyword "
                  + "         order by "
                  + computerCol
                  + " "
                  + order
                  + ", keyword) A)";
               sql = "with A as ("
                  + sql
                  + ") select *"
                  + "  from (select rownum as TRN, A.*,(select count(0) from A where "
                  + kwdCond
                  + ") as totalCount"
                  + "  from (A) where "
                  + kwdCond
                  + ")";
            }
         }
      } catch (Exception var18) {
         this.logger.error("组装导出专题sql异常", var18);
      }

      return sql;
   }

   private String getfetchCallHotWordSql(Long taskId, String wordType, String countType, String keyword, int pageNum, int pageSize) throws ViePlatformServiceException {
      String sql = "";

      try {
         String computerCol = "kwpercent";
         computerCol = "voice".equals(countType) ? "kwfilepercent" : computerCol;
         String sumCol = "sum(kwcount)/sum(totalkwcount)";
         sumCol = "voice".equals(countType) ? "sum(kwfilecount)/sum(totalfilekwcount)" : sumCol;
         int startNum = (pageNum - 1) * pageSize;
         int endNum = pageNum * pageSize;
         String kwdCond = "";
         kwdCond = !StringUtils.isNullOrEmpry(keyword) ? "keyword like'%" + keyword + "%'" : "1=1";
         String kwTypeCond = "focusKwd".equals(wordType) ? " and keyword in(select keyword from FOCUSHOTWORD) " : " ";
         String computerPer = "sum(hw.kwcount) / sum(hw.totalkwcount)";
         computerPer = "voice".equals(countType) ? "sum(hw.kwfilecount) / sum(hw.totalfilekwcount)" : computerPer;
         if (DBSelectUtil.isSqlServer()) {
            sql = "select * from(select row_number() over(order by "
               + computerCol
               + " percent) as RN,A.*,0 as kwrange"
               + "  from (select top 100 PERCENT keyword,"
               + "              "
               + computerPer
               + " as "
               + computerCol
               + ""
               + "          from CALL_HOTWORD_STAT hw"
               + "         where  task_id = "
               + taskId
               + " "
               + " and keyword not in (select keyword from excludehotword) "
               + kwTypeCond
               + "         group by keyword "
               + "         order by "
               + computerCol
               + " DESC) A) t ";
            sql = "with A as ("
               + sql
               + ") select *"
               + "  from (select row_number() over(order by "
               + computerCol
               + ", keyword) as TRN, A.*,(select count(0) from A where "
               + kwdCond
               + ") as totalCount"
               + "  from A where "
               + kwdCond
               + ") t2 where TRN>"
               + startNum
               + " and TRN<="
               + endNum;
         } else {
            sql = "select * from(select rownum as RN,A.*,0 as kwrange  from (select keyword,              "
               + computerPer
               + " as "
               + computerCol
               + ""
               + "          from CALL_HOTWORD_STAT hw"
               + "         where  task_id = "
               + taskId
               + " "
               + " and keyword not in (select keyword from excludehotword) "
               + kwTypeCond
               + "         group by keyword "
               + "         order by "
               + computerCol
               + " DESC, keyword) A)";
            sql = "with A as ("
               + sql
               + ") select *"
               + "  from (select rownum as TRN, A.*,(select count(0) from A where "
               + kwdCond
               + ") as totalCount"
               + "  from A where "
               + kwdCond
               + ")where TRN>"
               + startNum
               + " and TRN<="
               + endNum;
         }

         return sql;
      } catch (Exception var15) {
         this.logger.error("【获取热词查询sql】失败", var15);
         throw new ViePlatformServiceException("【获取热词查询sql】失败", var15);
      }
   }

   public HibernateTemplateUtils getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplateUtils hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
