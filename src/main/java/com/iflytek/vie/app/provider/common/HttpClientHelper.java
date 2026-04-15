package com.iflytek.vie.app.provider.common;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientHelper {
   private static final Logger logger = LoggerFactory.getLogger(HttpClientHelper.class);
   private HttpClient httClient = HttpClientUtils.getConnection();

   public String get(String url, String encoding, Map<String, String> headers) {
      HttpGet httpGet = new HttpGet(url);
      if (headers != null && headers.size() > 0) {
         for (Entry<String, String> entry : headers.entrySet()) {
            httpGet.addHeader(entry.getKey(), entry.getValue());
         }
      }

      try {
         HttpResponse response = (CloseableHttpResponse)this.httClient.execute(httpGet);
         if (response.getStatusLine().getStatusCode() != 200) {
            httpGet.abort();
            return null;
         }

         HttpEntity entity = response.getEntity();
         String respString = EntityUtils.toString(entity);
         EntityUtils.consume(entity);
         return respString;
      } catch (ConnectTimeoutException var8) {
         httpGet.abort();
         logger.info("Connect Timeout:" + url);
      } catch (SocketTimeoutException var9) {
         httpGet.abort();
         logger.info("Socket Timeout:" + url);
      } catch (IOException var10) {
         httpGet.abort();
         logger.info("error info is :", var10);
      }

      return null;
   }

   public String get(String url, String encoding) {
      StringBuffer content = new StringBuffer();
      HttpGet httpGet = new HttpGet(url);
      CloseableHttpResponse response = null;
      this.httClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
      httpGet.addHeader("Connection", "close");
      httpGet.getParams().setParameter("http.protocol.content-charset", encoding);
      httpGet.getParams().setParameter("http.protocol.element-charset", encoding);

      try {
         response = (CloseableHttpResponse)this.httClient.execute(httpGet);
         if (response == null) {
            return null;
         }

         HttpEntity entity = response.getEntity();
         String respString = EntityUtils.toString(entity);
         content.append(respString);
      } catch (ConnectTimeoutException var8) {
         logger.info("Connect Timeout:" + url);
      } catch (SocketTimeoutException var9) {
         logger.info("Socket Timeout:" + url);
      } catch (IOException var10) {
         logger.info("error info is :", var10);
      }

      return content.toString();
   }

   public static void main(String[] args) throws ClientProtocolException, IOException, InterruptedException {
      HttpHelper http = null;

      for (int j = 0; j < 2; j++) {
         ThreadB[] threadArray = new ThreadB[10];

         for (int i = 0; i < 10; i++) {
            http = new HttpHelper();
            threadArray[i] = new ThreadB(http);
         }

         for (int i = 0; i < 10; i++) {
            threadArray[i].start();
         }

         Thread.sleep(1000L);
      }
   }
}
