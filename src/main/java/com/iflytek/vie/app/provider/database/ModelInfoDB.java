package com.iflytek.vie.app.provider.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.hibernate4.HibernateTemplate;

public class ModelInfoDB {
   private final Logger logger = LoggerFactory.getLogger(ModelInfoDB.class);
   private HibernateTemplate hiberTemp;
   private JdbcTemplate jdbcTemplate;

   public HibernateTemplate getHiberTemp() {
      return this.hiberTemp;
   }

   public void setHiberTemp(HibernateTemplate hiberTemp) {
      this.hiberTemp = hiberTemp;
   }

   public JdbcTemplate getJdbcTemplate() {
      return this.jdbcTemplate;
   }

   public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
   }
}
