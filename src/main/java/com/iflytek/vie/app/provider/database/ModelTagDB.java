package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.TagDimension;
import com.iflytek.vie.app.pojo.model.TagDimensionProperty;
import com.iflytek.vie.app.pojo.model.TagPropertyOperation;
import com.iflytek.vie.utils.DBSelectUtil;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class ModelTagDB {
   private HibernateTemplate hibernateTemplate;
   private JdbcTemplate jdbcTemplate;

   public List<TagDimension> getTagDimension(int searchType) throws Exception {
      String hql = "FROM TagDimension TD WHERE TD.dimensionType=?";
      return (List)this.hibernateTemplate.find(hql, new Object[]{searchType});
   }

   public List<TagDimensionProperty> getPropertys(long dimensionId, String diemsionCode, boolean isAll) throws Exception {
      String hql = "SELECT TDP FROM TagDimensionProperty TDP INNER JOIN TDP.dimensions TD WHERE TD.dimension.dimensionId=? AND TD.dimension.dimensionCode=?";
      String sql = "";
      List<TagDimensionProperty> resultList = null;
      if (!isAll) {
         sql = "select t.property_id, t.property_code, t.property_name, t.is_depend, t.flag   from TAG_DIMENSION_PROPERTY t   join TAG_DPCONNECTION tdp     on t.property_id = tdp.property_id   join TAG_DIMENSION td     on td.dimension_id = tdp.dimension_id  where td.dimension_id =?    and td.dimension_code =?";
         resultList = this.jdbcTemplate.query(sql, new Object[]{dimensionId, diemsionCode}, new RowMapper<TagDimensionProperty>() {
            TagDimensionProperty tdp = null;

            public TagDimensionProperty mapRow(ResultSet resultSet, int i) throws SQLException {
               this.tdp = new TagDimensionProperty();
               this.tdp.setPropertyId(resultSet.getLong("property_id"));
               this.tdp.setPropertyCode(resultSet.getString("property_code"));
               this.tdp.setPropertyName(resultSet.getString("property_name"));
               this.tdp.setIsDepend(resultSet.getInt("is_depend"));
               this.tdp.setFlag(resultSet.getInt("flag"));
               return this.tdp;
            }
         });
      } else {
         sql = "select distinct tdp.property_id,                 tdp.property_code,                 tdp.property_name,                 tdp.is_depend,                 tdp.flag   from TAG_DIMENSION_PROPERTY tdp   join TAG_DPCONNECTION td     on tdp.property_id = td.property_id   join TAG_DIMENSION t     on td.dimension_id = t.dimension_id    and t.dimension_type = 1";
         resultList = this.jdbcTemplate.query(sql, new RowMapper<TagDimensionProperty>() {
            TagDimensionProperty tdp = null;

            public TagDimensionProperty mapRow(ResultSet resultSet, int i) throws SQLException {
               this.tdp = new TagDimensionProperty();
               this.tdp.setPropertyId(resultSet.getLong("property_id"));
               this.tdp.setPropertyCode(resultSet.getString("property_code"));
               this.tdp.setPropertyName(resultSet.getString("property_name"));
               this.tdp.setIsDepend(resultSet.getInt("is_depend"));
               this.tdp.setFlag(resultSet.getInt("flag"));
               return this.tdp;
            }
         });
      }

      return resultList;
   }

   public List<TagPropertyOperation> getOperations(long propertyId, String propertyCode, boolean isAll) {
      String hql = "SELECT TPO FROM TagPropertyOperation TPO INNER JOIN TPO.properties TP WHERE TP.property.propertyId=? AND TP.property.propertyCode=?";
      String sql = "";
      List<TagPropertyOperation> resultList = null;
      if (!isAll) {
         sql = "select t.operation_id,       t.operation_code,       t.operation_name,       t.operation_status,       t.type   from TAG_PROPERTY_OPERATION t   join tag_po_connection tpc     on t.operation_id = tpc.operation_id   join tag_dimension_property tdp     on tpc.property_id = tdp.property_id  where tdp.property_id =?    and tdp.property_code =?";
         resultList = this.jdbcTemplate.query(sql, new Object[]{propertyId, propertyCode}, new RowMapper<TagPropertyOperation>() {
            TagPropertyOperation tpo = null;

            public TagPropertyOperation mapRow(ResultSet resultSet, int i) throws SQLException {
               this.tpo = new TagPropertyOperation();
               this.tpo.setOperationId(resultSet.getLong("operation_id"));
               this.tpo.setOperationCode(resultSet.getString("operation_code"));
               this.tpo.setOperationName(resultSet.getString("operation_name"));
               this.tpo.setOperationStatus(resultSet.getInt("operation_status"));
               this.tpo.setType(resultSet.getInt("type"));
               return this.tpo;
            }
         });
      } else {
         if (DBSelectUtil.isMysql()) {
            sql = "select distinct tpo.operation_id,                 tpo.operation_code,                 tpo.operation_name,                 tpo.operation_status,                 tpo.type   from (select distinct tdp.property_id pId     from TAG_DIMENSION_PROPERTY tdp     join TAG_DPCONNECTION td       on tdp.property_id = td.property_id     join TAG_DIMENSION t       on td.dimension_id = t.dimension_id      and t.dimension_type = 1) ot   join TAG_PO_CONNECTION tpc     on tpc.property_id = ot.pId   join TAG_PROPERTY_OPERATION tpo     on tpc.operation_id = tpo.operation_id";
         } else {
            sql = "with operationTemp as  (select distinct tdp.property_id pId     from TAG_DIMENSION_PROPERTY tdp     join TAG_DPCONNECTION td       on tdp.property_id = td.property_id     join TAG_DIMENSION t       on td.dimension_id = t.dimension_id      and t.dimension_type = 1) select distinct tpo.operation_id,                 tpo.operation_code,                 tpo.operation_name,                 tpo.operation_status,                 tpo.type   from operationTemp ot   join TAG_PO_CONNECTION tpc     on tpc.property_id = ot.pId   join TAG_PROPERTY_OPERATION tpo     on tpc.operation_id = tpo.operation_id";
         }

         resultList = this.jdbcTemplate.query(sql, new RowMapper<TagPropertyOperation>() {
            TagPropertyOperation tpo = null;

            public TagPropertyOperation mapRow(ResultSet resultSet, int i) throws SQLException {
               this.tpo = new TagPropertyOperation();
               this.tpo.setOperationId(resultSet.getLong("operation_id"));
               this.tpo.setOperationCode(resultSet.getString("operation_code"));
               this.tpo.setOperationName(resultSet.getString("operation_name"));
               this.tpo.setOperationStatus(resultSet.getInt("operation_status"));
               this.tpo.setType(resultSet.getInt("type"));
               return this.tpo;
            }
         });
      }

      return resultList;
   }

   public HibernateTemplate getHibernateTemplate() {
      return this.hibernateTemplate;
   }

   public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
      this.hibernateTemplate = hibernateTemplate;
   }

   public JdbcTemplate getJdbcTemplate() {
      return this.jdbcTemplate;
   }

   public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
   }
}
