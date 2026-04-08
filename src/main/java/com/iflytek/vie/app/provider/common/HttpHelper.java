package com.iflytek.vie.app.provider.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.http.client.ClientProtocolException;

public class HttpHelper {
   private int timeOut = 3000;
   private HttpClient httClient = null;
   private String errorString = "";

   public HttpHelper() {
      this.httClient = new HttpClient();
   }

   public HttpHelper(int timeOut) {
      this();
      this.timeOut = timeOut;
   }

   public int getTimeOut() {
      return this.timeOut;
   }

   public void setTimeOut(int timeOut) {
      this.timeOut = timeOut;
   }

   public String getErrorString() {
      return this.errorString;
   }

   public String get(String url, String encoding, Map<String, String> headers) {
      StringBuffer content = new StringBuffer();
      HttpMethod method = new GetMethod(url);
      this.httClient.getHttpConnectionManager().getParams().setConnectionTimeout(this.timeOut);
      this.httClient.getHttpConnectionManager().getParams().setSoTimeout(this.timeOut);
      this.httClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
      method.addRequestHeader("Connection", "close");
      if (headers != null && headers.size() > 0) {
         for (Entry<String, String> entry : headers.entrySet()) {
            method.addRequestHeader(entry.getKey(), entry.getValue());
         }
      }

      method.getParams().setParameter("http.protocol.content-charset", encoding);
      method.getParams().setParameter("http.protocol.element-charset", encoding);

      try {
         this.httClient.executeMethod(method);
         String respString = method.getResponseBodyAsString();
         content.append(respString);
      } catch (ConnectTimeoutException var13) {
         this.errorString = "Connect Timeout:" + url;
      } catch (SocketTimeoutException var14) {
         this.errorString = "Socket Timeout:" + url;
      } catch (IOException var15) {
         this.errorString = var15.toString();
      } finally {
         method.releaseConnection();
      }

      return content.toString();
   }

   public String get(String url, String encoding) {
      StringBuffer content = new StringBuffer();
      HttpMethod method = new GetMethod(url);
      this.httClient.getHttpConnectionManager().getParams().setConnectionTimeout(this.timeOut);
      this.httClient.getHttpConnectionManager().getParams().setSoTimeout(this.timeOut);
      this.httClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
      method.addRequestHeader("Connection", "close");
      method.getParams().setParameter("http.protocol.content-charset", encoding);

      try {
         this.httClient.executeMethod(method);
         String respString = method.getResponseBodyAsString();
         content.append(respString);
      } catch (ConnectTimeoutException var11) {
         this.errorString = "Connect Timeout:" + url;
      } catch (SocketTimeoutException var12) {
         this.errorString = "Socket Timeout:" + url;
      } catch (IOException var13) {
         this.errorString = var13.toString();
      } finally {
         method.releaseConnection();
      }

      System.out.println(this.errorString);
      return content.toString();
   }

   public String post(String url, String sendData, String encoding) {
      StringBuffer content = new StringBuffer();
      PostMethod method = new PostMethod(url);
      this.httClient.getHttpConnectionManager().getParams().setConnectionTimeout(this.timeOut);
      this.httClient.getHttpConnectionManager().getParams().setSoTimeout(this.timeOut);
      this.httClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
      method.addRequestHeader("Connection", "close");
      method.getParams().setParameter("http.protocol.content-charset", encoding);

      try {
         method.setRequestBody(sendData);
         this.httClient.executeMethod(method);
         String respString = method.getResponseBodyAsString();
         content.append(respString);
      } catch (ConnectTimeoutException var12) {
         this.errorString = "Connect Timeout:" + url;
      } catch (SocketTimeoutException var13) {
         this.errorString = "Socket Timeout:" + url;
      } catch (IOException var14) {
         this.errorString = var14.toString();
      } finally {
         method.releaseConnection();
      }

      return content.toString();
   }

   public String postNew(String url, String contentType, String sendData, String encoding) {
      StringBuffer content = new StringBuffer();
      PostMethod method = new PostMethod(url);
      this.httClient.getHttpConnectionManager().getParams().setConnectionTimeout(this.timeOut);
      this.httClient.getHttpConnectionManager().getParams().setSoTimeout(this.timeOut);
      this.httClient.getParams().setBooleanParameter("http.protocol.expect-continue", false);
      method.addRequestHeader("Connection", "close");
      method.getParams().setParameter("http.protocol.content-charset", encoding);

      try {
         RequestEntity entity = new StringRequestEntity(sendData, contentType, encoding);
         method.setRequestEntity(entity);
         this.httClient.executeMethod(method);
         int code = method.getStatusCode();
         if (code == 200) {
            InputStream in = method.getResponseBodyAsStream();
            BufferedReader buffer = new BufferedReader(new InputStreamReader(in, encoding));
            String tempstr = "";

            while ((tempstr = buffer.readLine()) != null) {
               content = content.append(tempstr);
            }

            buffer.close();
            in.close();
         }
      } catch (ConnectTimeoutException var17) {
         this.errorString = "Connect Timeout:" + url;
      } catch (SocketTimeoutException var18) {
         this.errorString = "Socket Timeout:" + url;
      } catch (IOException var19) {
         this.errorString = var19.toString();
      } finally {
         method.releaseConnection();
      }

      return content.toString();
   }

   public static void main(String[] args) throws ClientProtocolException, IOException, InterruptedException {
      HttpClientHelper http = null;

      for (int j = 0; j < 2; j++) {
         ThreadA[] threadArray = new ThreadA[10];

         for (int i = 0; i < 10; i++) {
            http = new HttpClientHelper();
            threadArray[i] = new ThreadA(http);
         }

         for (int i = 0; i < 10; i++) {
            threadArray[i].start();
         }
      }
   }
}
