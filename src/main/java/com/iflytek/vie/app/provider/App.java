package com.iflytek.vie.app.provider;

import com.iflytek.vie.dynamic.DynamicDataSourceRegist;
import com.iflytek.vie.dynamic.DynamicEsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication(scanBasePackages = "com.iflytek.vie")
@EnableAutoConfiguration(
   exclude = {
         DataSourceAutoConfiguration.class,
         HibernateJpaAutoConfiguration.class,
         DataSourceTransactionManagerAutoConfiguration.class,
         JpaRepositoriesAutoConfiguration.class
   }
)
@Import({DynamicDataSourceRegist.class, DynamicEsSource.class})
@ImportResource({"classpath:/spring/*.xml", "classpath:/spring/*/*.xml"})
public class App {
   private static final Logger logger = LoggerFactory.getLogger(App.class);

   public static void main(String[] args) {
      SpringApplication.run(App.class, args);
      logger.info("======服务已经启动========");
   }
}
