package com.yangzhe.imanhua;


import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * Date: 12-8-3
 * Time: 下午2:02
 *
 * @author zhe.yangz
 */
public class StartGather {

    private static final String SEVER_DOMAIN = "http://c4.mangafiles.com";

    public static void main(String[] args) {

        String[] urls = {
                "http://www.imanhua.com/comic/2066/list_49071.html",
                "http://www.imanhua.com/comic/2066/list_50677.html",
               /* "http://www.imanhua.com/comic/2066/list_51234.html",
                "http://www.imanhua.com/comic/2066/list_52747.html",
                "http://www.imanhua.com/comic/2066/list_54312.html",
                "http://www.imanhua.com/comic/2066/list_61640.html",
                "http://www.imanhua.com/comic/2066/list_63733.html",
                "http://www.imanhua.com/comic/2066/list_63734.html",
                "http://www.imanhua.com/comic/2066/list_70677.html",
                "http://www.imanhua.com/comic/2066/list_70678.html",
                "http://www.imanhua.com/comic/2066/list_70679.html",*/
        };


        StartGather startGather = new StartGather();
        startGather.runIt("F:\\2066\\", urls);


    }

    public void runIt(String dir, String[] urls) {
        File file = new File(dir + "code.txt");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();  //default
        }

        try {
            int idx = 0;
            PrintStream ps = new PrintStream(new FileOutputStream(file));
            for (String url : urls) {
                parsebook(idx++, url, ps, dir);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //default
                }
            }
            ps.close();
        } catch (IOException e) {
            e.printStackTrace();  //default
        }
    }

    private void parsebook(int idx, String url, PrintStream ps, String dir) throws IOException {
        String content = httpget(url);

        if (content != null && !content.isEmpty()) {
            ArrayList<String> imgs;
            imgs = parseContent(content);
            ps.println("imgArr[" + idx + "] = new String[]{");
            int cnt = 0;
            for (String img : imgs) {
                ps.println("\""+img+"\",");
            }
            ps.println("};");

            // bitmap save
            String firstImageUrl = SEVER_DOMAIN + imgs.get(0);
            File saveDir = new File(dir + "firstPage\\");
            if (!saveDir.exists()) saveDir.mkdirs();
            File saveImageFile = new File(saveDir.getAbsolutePath() + "\\p" + (idx+1) + "_1.jpg");
            URL uri = new URL(firstImageUrl);
            URLConnection urlConnection = uri.openConnection();
            urlConnection.setAllowUserInteraction(false);
            urlConnection.setDoOutput(true);
            urlConnection.addRequestProperty("Referer",
                    "http://www.imanhua.com/");
            OutputStream os = new FileOutputStream(saveImageFile);
            InputStream is = urlConnection.getInputStream();
            byte[] buff = new byte[1024];
            while(true) {
                int readed = is.read(buff);
                if(readed == -1) {
                    break;
                }
                byte[] temp = new byte[readed];
                System.arraycopy(buff, 0, temp, 0, readed);
                os.write(temp);
            }
            is.close();
            os.close();

            File smallDir = new File(dir + "smallPage\\");
            if (!smallDir.exists()) smallDir.mkdirs();
            jpegSmallPage(saveImageFile.getAbsolutePath(),
                    smallDir.getAbsolutePath() + "\\p" + (idx+1) + "_1.jpg");

            jpegFirstPage(saveImageFile.getAbsolutePath(), saveImageFile.getAbsolutePath());
        }
    }

    private void jpegFirstPage(String ori, String dest) throws IOException {
        File _file = new File(ori);                       //读入文件
        Image src = javax.imageio.ImageIO.read(_file);                     //构造Image对象
        int wideth=src.getWidth(null);                                     //得到源图宽
        int height=src.getHeight(null);                                    //得到源图长
        BufferedImage tag = new BufferedImage(wideth,height,BufferedImage.TYPE_INT_RGB);
        tag.getGraphics().drawImage(src,0,0,wideth,height,null);       //绘制缩小后的图
        FileOutputStream out=new FileOutputStream(dest);          //输出到文件流
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
        JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(tag);
        param.setQuality(0.5f, false);
        encoder.setJPEGEncodeParam(param);
        encoder.encode(tag);                                               //近JPEG编码
        //System.out.print(width+"*"+height);
        out.close();
    }

    private void jpegSmallPage(String ori, String dest) throws IOException {
        File _file = new File(ori);                       //读入文件
        Image src = javax.imageio.ImageIO.read(_file);                     //构造Image对象
        int wideth=src.getWidth(null);                                     //得到源图宽
        int height=src.getHeight(null);                                    //得到源图长
        BufferedImage tag = new BufferedImage(200, 285, BufferedImage.TYPE_INT_RGB);
        tag.getGraphics().drawImage(src,0,0,200,285,null);       //绘制缩小后的图
        FileOutputStream out=new FileOutputStream(dest);          //输出到文件流
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
        encoder.encode(tag);                                               //近JPEG编码
        //System.out.print(width+"*"+height);
        out.close();
    }

    private ArrayList<String> parseContent(String content) {
        //System.out.println(content);
        int start = content.indexOf("eval");
        if (start < 0) start = content.indexOf("var len=");
        content = content.substring(start);
        int end = content.indexOf("</script>");
        content = content.substring(0, end);
        //System.out.println(content);

        ScriptEngineManager sem = new ScriptEngineManager();
        ScriptEngine se = sem.getEngineByName("js");
        try {
            String pre = "var setting = {chapterInfo:{}};";
            Object o = se.eval(pre+content+";pic;");
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
