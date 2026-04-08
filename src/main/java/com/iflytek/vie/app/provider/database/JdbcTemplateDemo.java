package com.iflytek.vie.app.provider.database;

import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcTemplateDemo {
   private JdbcTemplate jdbcTemplate;

   public void execute() {
      String sql = "select * from model_info where id=? ";
      this.jdbcTemplate.query(sql, new Object[]{1L}, new ModelMapper());
      this.jdbcTemplate.update("update model_info set name=?", new Object[]{"test"});
   }

   public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
   }
}
