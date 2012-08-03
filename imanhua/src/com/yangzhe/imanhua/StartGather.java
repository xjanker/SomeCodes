package com.yangzhe.imanhua;


import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import sun.org.mozilla.javascript.internal.NativeArray;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * Date: 12-8-3
 * Time: 下午2:02
 *
 * @author zhe.yangz
 */
public class StartGather {

    public static void main(String[] args) {
        String url = "http://www.imanhua.com/comic/1915/list_52912.html";
        String content = httpget(url);

        if (content != null && !content.isEmpty()) {
            ArrayList<String> imgs;
            imgs = parseContent(content);
            for (String img : imgs) {
                System.out.println(img);
            }

        }

    }

    private static ArrayList<String> parseContent(String content) {
        int start = content.indexOf("eval");
        content = content.substring(start);
        int end = content.indexOf("imanhua.");
        content = content.substring(0, end);
        System.out.println(content);

        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("js");
        try {
            String pre = "var setting = {chapterInfo:{}};";
            Object o = se.eval(pre+content+"setting.chapterInfo.images");
            sun.org.mozilla.javascript.internal.NativeArray nativeArray =
                    (NativeArray) o;
            ArrayList<String> imgs = new ArrayList<String>();
            for (int idx = 0; idx < nativeArray.getLength(); idx++) {
                imgs.add(nativeArray.get(idx, null).toString());
            }
            return imgs;
        } catch (ScriptException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String httpget(String url) {
        //构造HttpClient的实例
        HttpClient httpClient = new HttpClient();
        //创建GET方法的实例
        GetMethod getMethod = new GetMethod(url);
        //使用系统提供的默认的恢复策略
        getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                                          new DefaultHttpMethodRetryHandler());
        try {
          //执行getMethod
          int statusCode = httpClient.executeMethod(getMethod);
          if (statusCode != HttpStatus.SC_OK) {
                  System.err.println("Method failed: "
                                                 + getMethod.getStatusLine());
              }
           //读取内容
           byte[] responseBody = getMethod.getResponseBody();
           //处理内容
           return (new String(responseBody, "GBK"));
        } catch (HttpException e) {
           //发生致命的异常，可能是协议不对或者返回的内容有问题
           System.out.println("Please check your provided http address!");
           e.printStackTrace();
        } catch (IOException e) {
           //发生网络异常
           e.printStackTrace();
        } finally {
           //释放连接
           getMethod.releaseConnection();
        }
        return null;
    }
}
