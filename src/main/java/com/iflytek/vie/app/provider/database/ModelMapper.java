package com.iflytek.vie.app.provider.database;

import com.iflytek.vie.app.pojo.model.ModelInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

public class ModelMapper implements RowMapper<ModelInfo> {
   public ModelInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
      ModelInfo modelInfo = new ModelInfo();
      modelInfo.setModelId(rs.getLong("id"));
      return modelInfo;
   }
}
