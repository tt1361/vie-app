package com.iflytek.vie.app.provider.common;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HttpClientUtils {
   private static PoolingHttpClientConnectionManager connectionManager = null;
   private static HttpClientBuilder httpBulder = null;
   private static int MAXCONNECTION = 10;
   private static int DEFAULTMAXCONNECTION = 5;

   public static CloseableHttpClient getConnection() {
      CloseableHttpClient httpClient = httpBulder.build();
      return httpBulder.build();
   }

   static {
      connectionManager = new PoolingHttpClientConnectionManager();
      connectionManager.setMaxTotal(MAXCONNECTION);
      connectionManager.setDefaultMaxPerRoute(DEFAULTMAXCONNECTION);
      httpBulder = HttpClients.custom();
      httpBulder.setConnectionManager(connectionManager);
   }
}
