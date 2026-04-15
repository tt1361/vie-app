package com.iflytek.vie.app.provider.utils;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.fastjson.JSON;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArgumentLogFilter implements Filter {
   private static Logger logger = LoggerFactory.getLogger(ArgumentLogFilter.class);

   public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
      RpcContext context = RpcContext.getContext();
      String interName = invoker.getInterface().getName();
      String version = invoker.getUrl().getParameter("version");
      StringBuffer serviceName = new StringBuffer();
      serviceName.append(interName);
      if (null != version && version.length() > 0) {
         serviceName.append(":").append(version);
      }

      StringBuffer parameterTypes = new StringBuffer();
      Class<?>[] types = invocation.getParameterTypes();
      if (types != null && types.length > 0) {
         boolean first = true;

         for (Class<?> type : types) {
            if (first) {
               first = false;
            } else {
               parameterTypes.append(",");
            }

            parameterTypes.append(type.getName());
         }
      }

      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      String currentTime = simpleDateFormat.format(new Date());
      logger.info(
         "{} {}:{} -> {}:{} - {} {}({}) {}",
         new Object[]{
            currentTime,
            context.getRemoteHost(),
            context.getRemotePort(),
            context.getLocalHost(),
            context.getLocalPort(),
            serviceName,
            invocation.getMethodName(),
            parameterTypes,
            JSON.toJSONString(invocation.getArguments())
         }
      );
      long startTime = System.currentTimeMillis();
      Result result = invoker.invoke(invocation);
      long endTime = System.currentTimeMillis();
      String output = "";

      try {
         output = JSON.toJSONString(result).getBytes("UTF-8").length > 3000 ? "出参太长，此处不打印" : JSON.toJSONString(result);
      } catch (UnsupportedEncodingException var18) {
         output = var18.getMessage();
         logger.info("字符串转换字节数组异常", var18);
      }

      logger.info(
         "{} {}:{} -> {}:{} - {} {} runtime: {}ms result is: {}",
         new Object[]{
            currentTime,
            context.getLocalHost(),
            context.getLocalPort(),
            context.getRemoteHost(),
            context.getRemotePort(),
            serviceName,
            invocation.getMethodName(),
            endTime - startTime,
            output
         }
      );
      return result;
   }
}
