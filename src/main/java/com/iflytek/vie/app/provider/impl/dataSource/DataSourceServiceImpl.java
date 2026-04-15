package com.iflytek.vie.app.provider.impl.dataSource;

import com.iflytek.vie.app.api.dataSource.DataSourceService;
import com.iflytek.vie.dynamic.DynamicDataSourceContextHolder;
import java.util.List;

public class DataSourceServiceImpl implements DataSourceService {
   public List<String> getDataSource() {
      return DynamicDataSourceContextHolder.dataSourceIds;
   }
}
