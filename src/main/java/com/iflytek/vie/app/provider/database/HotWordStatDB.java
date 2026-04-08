package com.iflytek.vie.app.provider.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.vie.app.exception.ViePlatformServiceException;
import com.iflytek.vie.app.pojo.dimension.HotWordHistoryResponse;
import com.iflytek.vie.app.pojo.dimension.HotWordRankResponse;
import com.iflytek.vie.app.pojo.topic.CallHotWordTask;
import com.iflytek.vie.app.pojo.topic.CallHotWordTaskResponse;
import com.iflytek.vie.app.pojo.topic.HotWordTask;
import com.iflytek.vie.constants.Constants.TaskStatus;
import com.iflytek.vie.utils.DBSelectUtil;
import com.iflytek.vie.utils.DateUtils;
import com.iflytek.vie.utils.HibernateTemplateUtils;
import com.iflytek.vie.utils.StringUtils;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.internal.SessionFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HotWordStatDB {
   private static final Logger logger = LoggerFactory.getLogger(HotWordStatDB.class);
   private HibernateTemplateUtils hibernateTemplate;
   public String withTableName = "HKwtn";
   NumberFormat nFromat = NumberFormat.getPercentInstance();

   public HotWordStatDB() {
      this.nFromat.setMinimumFractionDigits(2);
   }

   public void addHotWord(String isBlack, String word, String dataSource) throws ViePlatformServiceException {
      Session session = null;
      PreparedStatement ps = null;
      Connection con = null;

      try {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         Date date = new Date();
         session = this.hibernateTemplate.getSessionFactory().openSession();
         session.beginTransaction();
         con = ((SessionFactoryImpl)this.getHibernateTemplate().getSessionFactory()).getConnectionProvider().getConnection();
         con.setAutoCommit(false);
         String sql = "";
         if ("true".equals(isBlack)) {
            sql = "insert into excludehotword(keyword,create_time,datasource) values(?,?,?)";
            ps = con.prepareStatement(sql);
            ps.setString(1, word);
            ps.setString(2, sdf.format(date));
            ps.setString(3, dataSource);
            ps.addBatch();
         } else {
            sql = "insert into focushotword(keyword,create_time,datasource) values(?,?,?)";
            ps = con.prepareStatement(sql);
            ps.setString(1, word);
            ps.setString(2, sdf.format(date));
            ps.setString(3, dataSource);
            ps.addBatch();
         }

         ps.executeBatch();
         con.commit();
         session.getTransaction().commit();
         ps.close();
         con.close();
      } catch (Exception var17) {
         logger.error("添加关注热词（排除热词）db类异常1");
         throw new ViePlatformServiceException("添加关注热词（排除热词）db类异常1", var17);
      } finally {
         try {
            ps.close();
            con.close();
            session.close();
         } catch (Exception var16) {
            logger.error("添加关注热词（排除热词）db类异常2");
            throw new ViePlatformServiceException("添加关注热词（排除热词）db类异常2", var16);
         }
      }
   }

   public int getTotalCount(String startTime, String dataSource) throws ViePlatformServiceException {
      int count = 0;
      Connection conn = null;
      CallableStatement queryList = null;
      ResultSet rs = null;

      try {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         Date now = sdf.parse(startTime);
         String sql = "select count(0) as count from hotwordstat t where t.statdate="
            + now.getTime()
            + " and datasource='"
            + dataSource
            + "' and keyword not in (select keyword from excludehotword where datasource='"
            + dataSource
            + "') ";
         conn = ((SessionFactoryImpl)this.getHibernateTemplate().getSessionFactory()).getConnectionProvider().getConnection();
         queryList = conn.prepareCall(String.format(sql));
         queryList.execute();
         rs = queryList.getResultSet();

         while (rs.next()) {
            count = rs.getInt("count");
         }
      } catch (Exception var17) {
         logger.error("获取前一天的总记录数db类异常1");
         throw new ViePlatformServiceException("获取前一天的总记录数db类异常1", var17);
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
         } catch (SQLException var16) {
            logger.error("获取前一天的总记录数db类异常2");
            throw new ViePlatformServiceException("获取前一天的总记录数db类异常2", var16);
         }
      }

      return count;
   }

   public List<HotWordRankResponse> fetchHotWord(
      String keyword,
      String type,
      String kwType,
      String order,
      int totalCount,
      String startTime,
      String endTime,
      int pageNum,
      int pageSize,
      String centerSql,
      int ifLone,
      String batchId,
      int dataType,
      String dataSource
   ) throws ViePlatformServiceException {
      List<HotWordRankResponse> listHashMap = null;
      Connection conn = null;
      CallableStatement queryList = null;
      ResultSet rs = null;

      try {
         String sql = this.getfetchHotWordSql(
            keyword, type, kwType, order, totalCount, startTime, endTime, pageNum, pageSize, centerSql, ifLone, batchId, dataType, dataSource
         );
         conn = ((SessionFactoryImpl)this.getHibernateTemplate().getSessionFactory()).getConnectionProvider().getConnection();
         logger.debug(sql);
         queryList = conn.prepareCall(sql);
         queryList.execute();
         rs = queryList.getResultSet();
         listHashMap = new ArrayList<>();
         String computerCol = "kwpercent";
         computerCol = "voice".equals(kwType) ? "kwfilepercent" : computerCol;

         while (rs.next()) {
            HotWordRankResponse hotWordRankResponse = new HotWordRankResponse();
            hotWordRankResponse.setHotVocabulary(rs.getString("keyword"));
            hotWordRankResponse.setIncrement(rs.getInt("kwrange"));
            hotWordRankResponse.setRate(this.nFromat.format(rs.getDouble(computerCol)));
            hotWordRankResponse.setRn(rs.getInt("TRN"));
            hotWordRankResponse.setTotalCount(rs.getInt("totalCount"));
            listHashMap.add(hotWordRankResponse);
         }
      } catch (Exception var29) {
         logger.error("查询热词db类异常1");
         throw new ViePlatformServiceException("查询热词db类异常1", var29);
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
         } catch (SQLException var28) {
            logger.error("查询热词db类异常2");
            throw new ViePlatformServiceException("查询热词db类异常2", var28);
         }
      }

      return listHashMap;
   }

   private String getfetchHotWordSql(
      String keyword,
      String wordType,
      String countType,
      String orderType,
      int totalCount,
      String startTime,
      String endTime,
      int pageNum,
      int pageSize,
      String centerSql,
      int ifLone,
      String batchId,
      int dataType,
      String dataSource
   ) throws ViePlatformServiceException {
      String sql = "";

      try {
         String dataSourceSql = " datasource='" + dataSource + "'";
         String computerCol = "kwpercent";
         computerCol = "voice".equals(countType) ? "kwfilepercent" : computerCol;
         String sumCol = "sum(kwcount)/sum(totalkwcount)";
         sumCol = "voice".equals(countType) ? "sum(kwfilecount)/sum(totalfilekwcount)" : sumCol;
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         int startNum = (pageNum - 1) * pageSize;
         int endNum = pageNum * pageSize;
         String kwdCond = "";
         kwdCond = !StringUtils.isNullOrEmpry(keyword) ? "keyword like'%" + keyword + "%'" : "1=1";
         String kwType = "focusKwd".equals(wordType) ? "and keyword in(select keyword from FOCUSHOTWORD where datasource='" + dataSource + "') " : "";
         String importSql = "";
         if (ifLone == 1) {
            importSql = " and if_lone = 1 and batch_id = '" + batchId + "'and data_type = " + dataType;
         } else {
            importSql = " and if_lone != 1 and data_type = " + dataType;
         }

         if (startTime.equals(endTime)) {
            Date now = null;
            now = sdf.parse(startTime);
            Date lastDay = DateUtils.getDaybeforeOrAfter(now, -1);
            String orderCol = "range".equals(orderType) ? "abs(kwrange) desc," + computerCol + " desc,keyword" : computerCol + " desc,keyword";
            if (DBSelectUtil.isMysql()) {
               sql = "select E.* from (select @rownum_e := @rownum_e+1 as TRN,D.*,(select count(0) from(select * from (select A.RN,IFNULL(B.RN-A.RN,"
                  + totalCount
                  + "-A.RN+1) as kwrange,A.keyword,A."
                  + computerCol
                  + " "
                  + "from"
                  + "(select (@rownum_a := @rownum_a+1) as RN,T.* "
                  + "from"
                  + "( select keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + " "
                  + " from hotwordstat "
                  + "where "
                  + dataSourceSql
                  + " and statdate='"
                  + now.getTime()
                  + "'"
                  + importSql
                  + " and "
                  + centerSql
                  + kwType
                  + " group by keyword,statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T,(select @rownum_a :=0) a ) A"
                  + " left join"
                  + " "
                  + "(select (@rownum_b := @rownum_b+1)as RN,T.*  "
                  + "from "
                  + "( select keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + " "
                  + " from hotwordstat "
                  + "where "
                  + dataSourceSql
                  + " and statdate='"
                  + lastDay.getTime()
                  + "' and "
                  + centerSql
                  + kwType
                  + "  group by keyword,statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T,(select @rownum_b :=0) b ) B"
                  + " on A.keyword=B.keyword "
                  + "where A.keyword not in(select keyword from excludehotword where "
                  + dataSourceSql
                  + ") and "
                  + ("(1=1)".equals(kwdCond) ? kwdCond : " " + kwdCond)
                  + " order by "
                  + orderCol
                  + ") "
                  + " C) D )as totalCount from (select @rownum_e :=0) rnc,"
                  + "(select * from (select A.RN,IFNULL(B.RN-A.RN,"
                  + totalCount
                  + "-A.RN+1) as kwrange,A.keyword,A."
                  + computerCol
                  + " "
                  + "from"
                  + "(select (@rownum_a := @rownum_a+1) as RN,T.* "
                  + "from"
                  + "( select keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + " "
                  + " from hotwordstat "
                  + "where "
                  + dataSourceSql
                  + " and statdate='"
                  + now.getTime()
                  + "'"
                  + importSql
                  + " and "
                  + centerSql
                  + kwType
                  + " group by keyword,statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T,(select @rownum_a :=0) a ) A"
                  + " left join"
                  + " "
                  + "(select (@rownum_b := @rownum_b+1)as RN,T.*  "
                  + "from "
                  + "( select keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + " "
                  + " from hotwordstat "
                  + "where "
                  + dataSourceSql
                  + " and statdate='"
                  + lastDay.getTime()
                  + "' and "
                  + centerSql
                  + kwType
                  + "  group by keyword,statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T,(select @rownum_b :=0) b ) B"
                  + " on A.keyword=B.keyword "
                  + "where A.keyword not in(select keyword from excludehotword where "
                  + dataSourceSql
                  + ") and "
                  + ("(1=1)".equals(kwdCond) ? kwdCond : " " + kwdCond)
                  + " order by "
                  + orderCol
                  + ") "
                  + " C) D )E where TRN>"
                  + startNum
                  + " and TRN<="
                  + endNum;
            } else if (DBSelectUtil.isSqlServer()) {
               sql = "with A as (select row_number() over(order by "
                  + computerCol
                  + " desc,keyword) as RN,T.* "
                  + "from"
                  + "( select top 100 PERCENT keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from hotwordstat "
                  + "where datasource='"
                  + dataSource
                  + "' and statdate='"
                  + now.getTime()
                  + "' and "
                  + centerSql
                  + kwType
                  + "  and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') group by keyword,statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T ), "
                  + "B as "
                  + "(select row_number() over(order by "
                  + computerCol
                  + " desc,keyword) as RN,T.*  "
                  + "from "
                  + "( select top 100 PERCENT keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from hotwordstat "
                  + "where datasource='"
                  + dataSource
                  + "' and statdate='"
                  + lastDay.getTime()
                  + "' and "
                  + centerSql
                  + kwType
                  + "  and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') group by keyword,statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T),"
                  + "C as (select * from( "
                  + "select top 100 PERCENT A.RN, isnull(B.RN-A.RN,"
                  + totalCount
                  + "-A.RN+1) as kwrange,A.keyword,A."
                  + computerCol
                  + " "
                  + "from A left join B on A.keyword=B.keyword "
                  + "where A.keyword not in(select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') and "
                  + ("1=1".equals(kwdCond) ? kwdCond : "A." + kwdCond)
                  + " ) T) "
                  + "select * from(select row_number() over(order by "
                  + orderCol
                  + " ) as TRN,C.*,(select count(0) from C) as totalCount from C ) T2 where T2.TRN>"
                  + startNum
                  + " and T2.TRN<="
                  + endNum;
            } else {
               sql = "with A as (select rownum as RN,T.* from( select keyword,statdate,"
                  + sumCol
                  + " as "
                  + computerCol
                  + ""
                  + " from hotwordstat "
                  + "where datasource='"
                  + dataSource
                  + "' and statdate='"
                  + now.getTime()
                  + "'"
                  + importSql
                  + " and "
                  + centerSql
                  + kwType
                  + "  and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') group by keyword,statdate "
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
                  + " from hotwordstat "
                  + "where datasource='"
                  + dataSource
                  + "' and statdate='"
                  + lastDay.getTime()
                  + "' and "
                  + centerSql
                  + kwType
                  + "  and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') group by keyword,statdate "
                  + " order by "
                  + computerCol
                  + " desc,keyword) T),"
                  + "C as (select * from( "
                  + "select  A.RN, nvl(B.RN-A.RN,"
                  + totalCount
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
            long start = sdf.parse(startTime).getTime();
            long end = sdf.parse(endTime).getTime();
            String computerPer = "sum(hw.kwcount) / sum(hw.totalkwcount)";
            computerPer = "voice".equals(countType) ? "sum(hw.kwfilecount) / sum(hw.totalfilekwcount)" : computerPer;
            if (DBSelectUtil.isMysql()) {
               sql = "(select * from(select (@rownum_a := @rownum_a+1) as RN,A.*,0 as kwrange  from (select keyword, "
                  + computerPer
                  + " as "
                  + computerCol
                  + ""
                  + "          from hotwordstat hw"
                  + "   where datasource='"
                  + dataSource
                  + "' and statdate >= '"
                  + start
                  + "' and statdate<='"
                  + end
                  + "' "
                  + importSql
                  + "  and "
                  + centerSql
                  + kwType
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + "  group by keyword "
                  + "         order by "
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
                  + " DESC) as RN,A.*,0 as kwrange"
                  + "  from (select top 100 PERCENT keyword,"
                  + "              "
                  + computerPer
                  + " as "
                  + computerCol
                  + ""
                  + "          from hotwordstat hw"
                  + "         where datasource='"
                  + dataSource
                  + "' and statdate >= '"
                  + start
                  + "' and statdate<='"
                  + end
                  + "' and "
                  + centerSql
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + kwType
                  + "         group by keyword "
                  + "         order by "
                  + computerCol
                  + " desc, keyword) A) t ";
               sql = "with A as ("
                  + sql
                  + ") select *"
                  + "  from (select row_number() over(order by "
                  + computerCol
                  + " DESC) as TRN, A.*,(select count(0) from A where "
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
                  + "          from hotwordstat hw"
                  + "         where datasource='"
                  + dataSource
                  + "' and statdate >= '"
                  + start
                  + "' and statdate<='"
                  + end
                  + "'"
                  + importSql
                  + " and "
                  + centerSql
                  + kwType
                  + " and keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "') "
                  + "         group by keyword "
                  + "         order by "
                  + computerCol
                  + " desc, keyword) A)";
               sql = "with A as ("
                  + sql
                  + ") select *"
                  + "  from (select rownum as TRN, A.*,(select count(0) from A where "
                  + kwdCond
                  + "and  keyword not in (select keyword from excludehotword where datasource='"
                  + dataSource
                  + "')) as totalCount"
                  + "  from (A) where "
                  + kwdCond
                  + ")where TRN>"
                  + startNum
                  + " and TRN<="
                  + endNum
                  + " order by RN";
            }
         }
      } catch (Exception var30) {
         logger.error("获取热词查询sql db类异常");
         throw new ViePlatformServiceException("获取热词查询sql db类异常", var30);
      }

      logger.info(sql);
      return sql;
   }

   public List<HotWordHistoryResponse> fetchHotWordByKwd(
      String keword, String type, String dataType, String dateString, String centerSql, int iflone, String batchId, int importDataType, String dataSource
   ) throws ViePlatformServiceException {
      List<HotWordHistoryResponse> listHashMap = new ArrayList<>();
      Connection conn = null;
      CallableStatement queryList = null;
      ResultSet rs = null;

      try {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         String computerCol = "kwpercent";
         computerCol = "voice".equals(dataType) ? "kwfilepercent" : computerCol;
         String sql = this.getfetchHotWordByKwdSql(keword, type, dataType, dateString, centerSql, iflone, batchId, importDataType, dataSource);
         logger.info(sql);
         conn = ((SessionFactoryImpl)this.getHibernateTemplate().getSessionFactory()).getConnectionProvider().getConnection();
         queryList = conn.prepareCall(String.format(sql));
         queryList.execute();
         rs = queryList.getResultSet();

         while (rs.next()) {
            HotWordHistoryResponse hotWordHistoryResponse = new HotWordHistoryResponse();
            Date cd = new Date(Long.parseLong(rs.getString("statdate")));
            hotWordHistoryResponse.setLastWeek(sdf.format(cd));
            hotWordHistoryResponse.setRank(rs.getInt("RN"));
            hotWordHistoryResponse.setRate(this.nFromat.format(rs.getDouble(computerCol)));
            listHashMap.add(hotWordHistoryResponse);
         }
      } catch (Exception var26) {
         logger.error("查询最近七天热词db类异常1");
         throw new ViePlatformServiceException("查询最近七天热词db类异常1", var26);
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
         } catch (SQLException var25) {
            logger.error("查询最近七天热词db类异常2");
            throw new ViePlatformServiceException("查询最近七天热词db类异常2", var25);
         }
      }

      return listHashMap;
   }

   private String getfetchHotWordByKwdSql(
      String keyword, String type, String dataType, String dateString, String centerSql, int ifLone, String batchId, int importDataType, String dataSource
   ) throws ViePlatformServiceException {
      String sql = "";
      String dataSourceSql = " dataSource='" + dataSource + "'";

      try {
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
         Date now = sdf.parse(dateString);
         Date start = DateUtils.getDaybeforeOrAfter(now, -7);
         List<String> tableName = new ArrayList<>();
         int index = 1;
         String tmpsql = "";
         String kwType = "focusKwd".equals(type) ? " and keyword in(select keyword from FOCUSHOTWORD where" + dataSourceSql + ") " : "";
         String computerCol = "kwpercent";
         computerCol = "voice".equals(dataType) ? "kwfilepercent" : computerCol;
         String computerPer = "sum(kwcount) / sum(totalkwcount)";
         computerPer = "voice".equals(dataType) ? "sum(kwfilecount) / sum(totalfilekwcount)" : computerPer;
         String importSql = "";
         if (ifLone == 1) {
            importSql = " and if_lone = 1 and batch_id = '" + batchId + "' and data_type = " + importDataType;
         } else {
            importSql = " and if_lone != 1 and data_type = " + importDataType;
         }

         if (DBSelectUtil.isMysql()) {
            while (start.getTime() < now.getTime()) {
               String cTable = this.withTableName + index++;
               int a = 1;
               String newTable = "T" + a++;
               sql = "select  (@rownum_a := @rownum_a+1) as RN, T.* from(Select keyword,statdate,"
                  + computerPer
                  + " as "
                  + computerCol
                  + " from hotwordstat "
                  + "where "
                  + dataSourceSql
                  + " and statdate='"
                  + start.getTime()
                  + "'"
                  + importSql
                  + kwType
                  + " and "
                  + centerSql
                  + " and keyword not in (select keyword from excludehotword where"
                  + dataSourceSql
                  + ") "
                  + " group by keyword, statdate"
                  + " order by "
                  + computerPer
                  + " desc,keyword)T,(select (@rownum_a :=0) b )"
                  + cTable
                  + " ";
               tableName.add(cTable);
               tmpsql = tmpsql + "select * from (" + sql + ")" + newTable + " where keyword='" + keyword + "' Union ";
               start = DateUtils.getDaybeforeOrAfter(start, 1);
            }

            if (tableName.size() > 0) {
               tmpsql = tmpsql.substring(0, tmpsql.length() - 7);
               sql = "  select * from( " + tmpsql + " )  T order by T.statdate desc";
            }
         } else {
            while (start.getTime() < now.getTime()) {
               String cTable = this.withTableName + index++;
               if (DBSelectUtil.isSqlServer()) {
                  sql = sql
                     + cTable
                     + ""
                     + " as (select row_number() over(order by "
                     + computerCol
                     + " DESC) as RN,T.*"
                     + "from"
                     + "( select top 100 PERCENT keyword,statdate,"
                     + computerPer
                     + " as "
                     + computerCol
                     + " from hotwordstat "
                     + "where "
                     + dataSourceSql
                     + " and  statdate='"
                     + start.getTime()
                     + "'"
                     + kwType
                     + " and "
                     + centerSql
                     + " and keyword not in (select keyword from excludehotword where"
                     + dataSourceSql
                     + ") "
                     + " group by keyword, statdate"
                     + " order by "
                     + computerPer
                     + " desc,keyword) T ),";
               } else {
                  sql = sql
                     + cTable
                     + ""
                     + " as (select rownum as RN,T.*"
                     + "from"
                     + "( select keyword,statdate,"
                     + computerPer
                     + " as "
                     + computerCol
                     + " from hotwordstat "
                     + "where "
                     + dataSourceSql
                     + " and statdate='"
                     + start.getTime()
                     + "'"
                     + importSql
                     + kwType
                     + " and "
                     + centerSql
                     + " and keyword not in (select keyword from excludehotword where"
                     + dataSourceSql
                     + ") "
                     + " group by keyword, statdate"
                     + " order by "
                     + computerPer
                     + " desc,keyword) T ),";
               }

               tableName.add(cTable);
               tmpsql = tmpsql + "select * from " + cTable + " where keyword='" + keyword + "' Union ";
               start = DateUtils.getDaybeforeOrAfter(start, 1);
            }

            if (tableName.size() > 0) {
               sql = " with " + sql.substring(0, sql.length() - 1);
               tmpsql = tmpsql.substring(0, tmpsql.length() - 7);
               sql = sql + "  select * from( " + tmpsql + " )  T order by T.statdate desc";
            }
         }

         return sql;
      } catch (Exception var25) {
         logger.error("获取sql（获取上期数据）db类异常1");
         throw new ViePlatformServiceException("获取sql（获取上期数据）db类异常1", var25);
      }
   }

   public boolean isFocusKwd(String keword, String dataSource) throws ViePlatformServiceException {
      boolean isContain = false;
      Connection conn = null;
      CallableStatement queryList = null;
      ResultSet rs = null;

      try {
         String sql = "select keyword from FOCUSHOTWORD where keyword='" + keword + "' and datasource='" + dataSource + "'";
         conn = ((SessionFactoryImpl)this.getHibernateTemplate().getSessionFactory()).getConnectionProvider().getConnection();
         queryList = conn.prepareCall(String.format(sql));
         queryList.execute();
         rs = queryList.getResultSet();
         if (rs.next()) {
            isContain = true;
         }
      } catch (Exception var15) {
         logger.error("是否是关注词db类异常1");
         throw new ViePlatformServiceException("是否是关注词db类异常1", var15);
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
         } catch (SQLException var14) {
            logger.error("是否是关注词db类异常2");
            throw new ViePlatformServiceException("是否是关注词db类异常2", var14);
         }
      }

      return isContain;
   }

   public boolean isExcludeKwd(String keword, String dataSource) throws ViePlatformServiceException {
      boolean isContain = false;
      Connection conn = null;
      CallableStatement queryList = null;
      ResultSet rs = null;

      try {
         String sql = "select keyword from EXCLUDEHOTWORD where keyword='" + keword + "' and datasource='" + dataSource + "'";
         conn = ((SessionFactoryImpl)this.getHibernateTemplate().getSessionFactory()).getConnectionProvider().getConnection();
         queryList = conn.prepareCall(String.format(sql));
         queryList.execute();
         rs = queryList.getResultSet();
         if (rs.next()) {
            isContain = true;
         }
      } catch (Exception var15) {
         logger.error("是否是排除词db类异常1");
         throw new ViePlatformServiceException("是否是排除词db类异常1", var15);
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
         } catch (SQLException var14) {
            logger.error("是否是排除词db类异常2");
            throw new ViePlatformServiceException("是否是排除词db类异常2", var14);
         }
      }

      return isContain;
   }

   public List<String> getHotWord(boolean isBlack, String wordName, String dataSource) throws ViePlatformServiceException {
      Session session = null;

      List var8;
      try {
         session = this.getHibernateTemplate().getSessionFactory().openSession();
         String sql = "";
         if (isBlack) {
            if (StringUtils.isNullOrEmpry(wordName)) {
               sql = "select t.keyword from excludehotword t where t.keyword is not null and datasource='" + dataSource + "' order by t.create_time asc";
            } else {
               wordName = wordName.replace("_", "\\_");
               sql = "select t.keyword from excludehotword t where t.keyword is not null and t.keyword like '%"
                  + wordName
                  + "%' and datasource='"
                  + dataSource
                  + "'";
               if (wordName.contains("\\")) {
                  sql = sql + " ESCAPE '\\'";
               }

               sql = sql + " order by t.create_time asc";
            }
         } else if (StringUtils.isNullOrEmpry(wordName)) {
            sql = "select t.keyword from focushotword t where t.keyword is not null and datasource='" + dataSource + "' order by t.create_time asc";
         } else {
            wordName = wordName.replace("_", "\\_");
            sql = "select t.keyword from focushotword t where t.keyword is not null and t.keyword like '%"
               + wordName
               + "%' and datasource='"
               + dataSource
               + "'";
            if (wordName.contains("\\")) {
               sql = sql + " ESCAPE '\\'";
            }

            sql = sql + " order by t.create_time asc";
         }

         Query query = session.createSQLQuery(sql);
         List<String> wordList = query.list();
         logger.info(sql);
         logger.info("查询热词db正常");
         var8 = wordList;
      } catch (Exception var12) {
         logger.error("查询热词db异常");
         throw new ViePlatformServiceException("查询热词db异常", var12);
      } finally {
         session.close();
      }

      return var8;
   }

   public boolean deleteHotWord(boolean isBlack, String word, String dataSource) throws ViePlatformServiceException {
      Session session = null;
      boolean flag = false;

      try {
         session = this.getHibernateTemplate().getSessionFactory().openSession();
         session.beginTransaction();
         String sql = "";
         if (isBlack) {
            sql = "delete from excludehotword where keyword = '" + word + "' and datasource='" + dataSource + "'";
         } else {
            sql = "delete from focushotword where keyword = '" + word + "' and datasource='" + dataSource + "'";
         }

         session.createSQLQuery(sql).executeUpdate();
         session.getTransaction().commit();
         logger.info(sql);
         logger.info("删除黑名单（白名单）热词db类服务正常");
         flag = true;
      } catch (Exception var10) {
         logger.error("删除黑名单（白名单）热词db类服务异常");
         throw new ViePlatformServiceException("删除黑名单（白名单）热词db类服务异常", var10);
      } finally {
         if (session != null) {
            session.close();
         }
      }

      return flag;
   }

   public long createHotWordTask(String dataAuth, Long pathId, Integer dataType, String batchId, int ifLone) {
      long taskId = 0L;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      List<HotWordTask> pageList = null;
      boolean existTask = false;
      String hql = "FROM HotWordTask WHERE pathId = :pathId AND dataAuth = :dataAuth";

      try {
         if (dataType == 2) {
            if (ifLone == 0) {
               hql = hql + " AND ifLone = :ifLone";
            } else {
               hql = hql + " AND batchId = :batchId AND ifLone = :ifLone";
               paramNames.add("batchId");
               values.add(batchId);
            }
         } else if (ifLone == 0) {
            hql = hql + " AND dataType = :dataType AND ifLone = :ifLone";
            paramNames.add("dataType");
            values.add(dataType);
         } else {
            hql = hql + " AND dataType = :dataType AND batchId = :batchId AND ifLone = :ifLone";
            paramNames.add("dataType");
            values.add(dataType);
            paramNames.add("batchId");
            values.add(batchId);
         }

         paramNames.add("pathId");
         values.add(pathId);
         paramNames.add("dataAuth");
         values.add(dataAuth);
         paramNames.add("ifLone");
         values.add(ifLone);
         logger.info("createHotWordTask params dataType:" + dataType + " batchId:" + batchId + " ifLone:" + ifLone);
         pageList = (List)this.hibernateTemplate.createQuery(hql, paramNames, values);
         if (pageList != null && pageList.size() > 0) {
            for (int i = 0; i < pageList.size(); i++) {
               HotWordTask task = pageList.get(i);
               if (task.getStatus() != TaskStatus.NOT_STARTED && task.getStatus() != TaskStatus.STARTING) {
                  this.hibernateTemplate.delete(task);
               } else {
                  this.hibernateTemplate.update(task);
                  existTask = true;
                  taskId = task.getTaskId();
               }
            }
         }

         if (!existTask) {
            HotWordTask task = new HotWordTask();
            task.setPathId(pathId);
            task.setStatus(TaskStatus.NOT_STARTED);
            task.setDataAuth(dataAuth);
            task.setDataType(dataType);
            task.setBatchId(batchId);
            task.setIfLone(ifLone);
            this.hibernateTemplate.save(task);
            taskId = task.getTaskId();
         }
      } catch (Exception var15) {
         logger.error("createHotWordTask DB has a error!", var15);
      }

      return taskId;
   }

   public int getHotWordTaskStatus(String dataAuth, Long pathId, Integer dataType, String batchId, int ifLone) {
      int status = 0;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String hql = "SELECT status FROM HotWordTask WHERE pathId = :pathId AND dataAuth = :dataAuth";

      try {
         if (dataType == 2) {
            if (ifLone == 0) {
               hql = hql + " AND ifLone = :ifLone";
            } else {
               hql = hql + " AND batchId = :batchId AND ifLone = :ifLone";
               paramNames.add("batchId");
               values.add(batchId);
            }
         } else if (ifLone == 0) {
            hql = hql + " AND ifLone = :ifLone AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
         } else {
            hql = hql + " AND batchId = :batchId AND ifLone = :ifLone AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
            paramNames.add("batchId");
            values.add(batchId);
         }

         paramNames.add("pathId");
         values.add(pathId);
         paramNames.add("dataAuth");
         values.add(dataAuth);
         paramNames.add("ifLone");
         values.add(ifLone);
         logger.info("getHotWordTaskStatus params dataType:" + dataType + " batchId:" + batchId + " ifLone:" + ifLone);
         Object obj = this.hibernateTemplate.uniqueResult(hql, paramNames, values);
         if (obj != null) {
            status = Integer.parseInt(String.valueOf(obj));
         }
      } catch (Exception var11) {
         logger.error("getHotWordTaskStatus DB has a error!", var11);
      }

      return status;
   }

   public String getHotWordLastFlushTime(String dataAuth, Long pathId, Integer dataType, String batchId, int ifLone) {
      String time = null;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String hql = "SELECT updateTime FROM HotWordTask WHERE pathId = :pathId AND dataAuth = :dataAuth";

      try {
         if (dataType == 2) {
            if (ifLone == 0) {
               hql = hql + " AND ifLone = :ifLone";
            } else {
               hql = hql + " AND batchId = :batchId AND ifLone = :ifLone";
               paramNames.add("batchId");
               values.add(batchId);
            }
         } else if (ifLone == 0) {
            hql = hql + " AND ifLone = :ifLone AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
         } else {
            hql = hql + " AND batchId = :batchId AND ifLone = :ifLone AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
            paramNames.add("batchId");
            values.add(batchId);
         }

         paramNames.add("dataAuth");
         values.add(dataAuth);
         paramNames.add("pathId");
         values.add(pathId);
         paramNames.add("ifLone");
         values.add(ifLone);
         logger.info("getHotWordLastFlushTime params dataType:" + dataType + " batchId:" + batchId + " ifLone:" + ifLone);
         Object obj = this.hibernateTemplate.uniqueResult(hql, paramNames, values);
         if (obj != null) {
            time = String.valueOf(obj);
         }
      } catch (Exception var11) {
         logger.error("getHotWordLastFlushTime DB has a error!", var11);
      }

      return time;
   }

   public Map<String, Object> parseJsonDataToMap(String data) {
      Map<String, Object> resultMap = null;

      try {
         if (data != null) {
            ObjectMapper mapper = new ObjectMapper();
            List<HashMap<String, Object>> topicConditionList = (List<HashMap<String, Object>>)mapper.readValue(data, List.class);
            if (topicConditionList != null && topicConditionList.size() > 0) {
               Map<String, Object> map = null;

               for (int i = 0; i < topicConditionList.size(); i++) {
                  map = topicConditionList.get(i);
                  if ("timestamp".equals(map.get("key"))) {
                     resultMap = map;
                     break;
                  }
               }
            }
         }
      } catch (Exception var7) {
         logger.error("parseJsonDataToMap DB has a error!", var7);
      }

      return resultMap;
   }

   public CallHotWordTaskResponse createCallHotWordTask(String dataAuth, Map<String, Object> condition) throws ViePlatformServiceException {
      CallHotWordTaskResponse response = new CallHotWordTaskResponse();
      int analyzeType = Integer.parseInt(String.valueOf(condition.get("analyzeType")));
      String analyzeIds = String.valueOf(condition.get("analyzeIds"));
      String startTime = String.valueOf(condition.get("startTime"));
      String endTime = String.valueOf(condition.get("endTime"));
      Integer dataType = Integer.parseInt(String.valueOf(condition.get("dataType")));
      String batchId = String.valueOf(condition.get("batchId"));
      int ifLone = Integer.parseInt(String.valueOf(condition.get("ifLone")));
      Integer flag = Integer.parseInt(String.valueOf(condition.get("flag")));
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      List<CallHotWordTask> pageList = null;
      String hql = "FROM CallHotWordTask WHERE analyzeType = :analyzeType AND analyzeIds = :analyzeIds AND startTime = :startTime AND endTime = :endTime AND dataAuth = :dataAuth AND ifLone = :ifLone";

      try {
         if (dataType == 2) {
            if (ifLone == 1) {
               hql = hql + " AND batchId = :batchId";
               paramNames.add("batchId");
               values.add(batchId);
            }
         } else if (ifLone == 0) {
            hql = hql + " AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
         } else {
            hql = hql + " AND batchId = :batchId AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
            paramNames.add("batchId");
            values.add(batchId);
         }

         paramNames.add("analyzeType");
         values.add(analyzeType);
         paramNames.add("analyzeIds");
         values.add(analyzeIds);
         paramNames.add("startTime");
         values.add(startTime);
         paramNames.add("endTime");
         values.add(endTime);
         paramNames.add("dataAuth");
         values.add(dataAuth);
         paramNames.add("ifLone");
         values.add(ifLone);
         logger.info("addCallHotWordTask-> dataType:" + dataType + " batchId:" + batchId + " ifLone:" + ifLone + " sql:" + hql);
         pageList = (List)this.hibernateTemplate.createQuery(hql, paramNames, values);
         if (pageList != null && pageList.size() > 0) {
            for (int i = 0; i < pageList.size(); i++) {
               CallHotWordTask task = pageList.get(i);
               if (flag == 2) {
                  task.setStatus(TaskStatus.NOT_STARTED);
                  task.setUpdateTime(DateUtils.parseDateToString(new Date(), "yyyy.MM.dd HH:mm:ss"));
                  this.hibernateTemplate.update(task);
                  response.setTaskId(task.getTaskId());
                  response.setStatus(task.getStatus());
                  response.setRefreshTime(task.getUpdateTime());
               } else {
                  response.setTaskId(task.getTaskId());
                  response.setStatus(task.getStatus());
                  response.setRefreshTime(task.getUpdateTime());
               }
            }
         } else {
            CallHotWordTask task = new CallHotWordTask();
            task.setAnalyzeIds(analyzeIds);
            task.setAnalyzeType(analyzeType);
            task.setStartTime(startTime);
            task.setEndTime(endTime);
            task.setStatus(TaskStatus.NOT_STARTED);
            task.setUpdateTime(DateUtils.parseDateToString(new Date(), "yyyy.MM.dd HH:mm:ss"));
            task.setDataAuth(dataAuth);
            task.setDataType(dataType);
            task.setBatchId(batchId);
            task.setIfLone(ifLone);
            this.hibernateTemplate.save(task);
            response.setTaskId(task.getTaskId());
            response.setStatus(task.getStatus());
            response.setRefreshTime(task.getUpdateTime());
         }

         return response;
      } catch (Exception var18) {
         logger.error("createHotWordTask DB has a error!", var18);
         throw new ViePlatformServiceException("createHotWordTask DB has a error", var18);
      }
   }

   public CallHotWordTaskResponse getCallHotWordTaskAndStatus(String dataAuth, Map<String, Object> condition) throws ViePlatformServiceException {
      CallHotWordTaskResponse response = new CallHotWordTaskResponse();
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      int analyzeType = Integer.parseInt(String.valueOf(condition.get("analyzeType")));
      String analyzeIds = String.valueOf(condition.get("analyzeIds"));
      String startTime = String.valueOf(condition.get("startTime"));
      String endTime = String.valueOf(condition.get("endTime"));
      int dataType = Integer.parseInt(String.valueOf(condition.get("dataType")));
      String batchId = String.valueOf(condition.get("batchId"));
      int ifLone = Integer.parseInt(String.valueOf(condition.get("ifLone")));
      CallHotWordTask task = null;
      String hql = "FROM CallHotWordTask WHERE analyzeType = :analyzeType AND analyzeIds = :analyzeIds AND startTime = :startTime AND endTime = :endTime AND dataAuth = :dataAuth AND ifLone = :ifLone";

      try {
         if (dataType == 2) {
            if (ifLone == 1) {
               hql = hql + " AND batchId = :batchId";
               paramNames.add("batchId");
               values.add(batchId);
            }
         } else if (ifLone == 0) {
            hql = hql + " AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
         } else {
            hql = hql + " AND batchId = :batchId AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
            paramNames.add("batchId");
            values.add(batchId);
         }

         paramNames.add("analyzeType");
         values.add(analyzeType);
         paramNames.add("analyzeIds");
         values.add(analyzeIds);
         paramNames.add("startTime");
         values.add(startTime);
         paramNames.add("endTime");
         values.add(endTime);
         paramNames.add("dataAuth");
         values.add(dataAuth);
         paramNames.add("ifLone");
         values.add(ifLone);
         logger.info("getCallHotWordTaskAndStatus-> dataType:" + dataType + " batchId:" + batchId + " ifLone:" + ifLone + " sql:" + hql);
         task = (CallHotWordTask)this.hibernateTemplate.uniqueResult(hql, paramNames, values);
         if (task == null) {
            return null;
         } else {
            response.setTaskId(task.getTaskId());
            response.setStatus(task.getStatus());
            response.setRefreshTime(task.getUpdateTime());
            return response;
         }
      } catch (Exception var16) {
         logger.error("【查询任务ID和执行状态】失败", var16);
         throw new ViePlatformServiceException("服务器内部错误", var16);
      }
   }

   public int getCallHotWordTaskStatus(String dataAuth, Long taskId, Integer dataType, String batchId, int ifLone) throws ViePlatformServiceException {
      int status = 0;
      List<String> paramNames = new ArrayList<>();
      List<Object> values = new ArrayList<>();
      String hql = "SELECT status FROM CallHotWordTask WHERE taskId = :taskId AND dataAuth = :dataAuth AND ifLone = :ifLone";

      try {
         if (dataType == 2) {
            if (ifLone == 1) {
               hql = hql + " AND batchId = :batchId";
               paramNames.add("batchId");
               values.add(batchId);
            }
         } else if (ifLone == 0) {
            hql = hql + " AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
         } else {
            hql = hql + " AND batchId = :batchId AND dataType = :dataType";
            paramNames.add("dataType");
            values.add(dataType);
            paramNames.add("batchId");
            values.add(batchId);
         }

         paramNames.add("taskId");
         values.add(taskId);
         paramNames.add("dataAuth");
         values.add(dataAuth);
         paramNames.add("ifLone");
         values.add(ifLone);
         logger.info("getCallHotWordTaskStatus-> dataType:" + dataType + " batchId:" + batchId + " ifLone:" + ifLone + " sql:" + hql);
         Object obj = this.hibernateTemplate.uniqueResult(hql, paramNames, values);
         if (obj != null) {
            status = Integer.parseInt(String.valueOf(obj));
         }

         return status;
      } catch (Exception var11) {
         logger.error("getHotWordTaskStatus DB has a error!", var11);
         throw new ViePlatformServiceException("getHotWordTaskStatus DB has a error", var11);
      }
   }

   public String getStatDate(Long taskId) throws ViePlatformServiceException {
      String statDate = "";
      List<Object> values = new ArrayList<>();
      List<String> paramNames = new ArrayList<>();

      try {
         String hql = "SELECT statDate FROM CallHotWordStat WHERE taskId = :taskId";
         paramNames.add("taskId");
         values.add(taskId);
         Object obj = this.hibernateTemplate.uniqueResult(hql, paramNames, values);
         if (obj != null) {
            statDate = String.valueOf(obj);
         }

         return statDate;
      } catch (Exception var7) {
         logger.error("getStatDate DB has a error!", var7);
         throw new ViePlatformServiceException("getStatDate DB has a error", var7);
      }
   }

   public HibernateTemplateUtils getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplateUtils hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }
}
