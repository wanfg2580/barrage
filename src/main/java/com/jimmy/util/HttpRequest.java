package com.jimmy.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HttpRequest {

    private static final Logger LOGGER = Logger.getLogger(HttpRequest.class);
    private static final Charset CHARSET_UTF8 = Charset.forName("utf-8");
    private static final String WENHAO = "?";

    private static PoolingHttpClientConnectionManager cm = null;

    static {
        LayeredConnectionSocketFactory sslsf = null;
        try {
            sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault());
        } catch (NoSuchAlgorithmException e) {
            LOGGER.info("创建SSL连接失败");
        }
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", sslsf)
                .register("http", new PlainConnectionSocketFactory())
                .build();
        cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(20);
    }

    private static CloseableHttpClient getHttpClient() {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
        return httpClient;
    }

    /**
     *
     * @param url
     * @return
     */
    public static String get(String url) {
        return get(url, null);
    }

    /**
     * 高效Get请求
     * @param url
     * @param params
     * @return
     */
    public static String get(String url, Map<String, String> params) {
        // 创建默认的httpClient实例
        CloseableHttpClient httpClient = HttpRequest.getHttpClient();
        CloseableHttpResponse httpResponse = null;
        // 发送get请求
        try {
            // 用get方法发送http请求
            HttpGet get = null;
            if (null != params) {
                get = new HttpGet(url + WENHAO + EntityUtils.toString(new UrlEncodedFormEntity(packageParams(params), CHARSET_UTF8)));
            } else  {
                get = new HttpGet(url);
            }
            LOGGER.info("执行get请求, uri: " + get.getURI());
            httpResponse = httpClient.execute(get);
            // response实体
            HttpEntity entity = httpResponse.getEntity();
            if (null != entity) {
                String response = EntityUtils.toString(entity);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                LOGGER.info("当前请求返回码: {}" + statusCode);
                if (statusCode == HttpStatus.SC_OK) {
                    // 成功
                    return response;
                } else {
                    return null;
                }
            }
            return null;
        } catch (IOException e) {
            LOGGER.info("httpclient请求失败", e);
            return null;
        } finally {
            if (httpResponse != null) {
                try {
                    EntityUtils.consume(httpResponse.getEntity());
                    httpResponse.close();
                } catch (IOException e) {
                    LOGGER.info("关闭response失败", e);
                }
            }
        }
    }

    /**
     * 封装请求参数
     * @param params
     * @return
     */
    private static List<NameValuePair> packageParams(Map<String, String> params) {
        List<NameValuePair> pairs = new ArrayList<>();
        params.forEach((key, value) -> pairs.add(new BasicNameValuePair(key, value)));
        return pairs;
    }

    /**
     * 高效Post请求
     * @param url
     * @param data
     * @return
     */
    public static String post(String url, String data) {
        // 创建默认的httpClient实例
        CloseableHttpClient httpClient = HttpRequest.getHttpClient();
        CloseableHttpResponse httpResponse = null;
        try {
            HttpPost post = new HttpPost(url);
            Charset charset = CHARSET_UTF8;
            StringEntity entity = new StringEntity(data, charset);
            entity.setContentEncoding(charset.name());
            entity.setContentType("application/json");
            post.setEntity(entity);

            httpResponse = httpClient.execute(post);
            // response实体
            HttpEntity httpEntity = httpResponse.getEntity();
            if (null != httpEntity) {
                String response = EntityUtils.toString(httpEntity);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                LOGGER.info("当前请求返回码: {}" + statusCode);
                if (statusCode == HttpStatus.SC_OK) {
                    // 成功
                    return response;
                } else {
                    return null;
                }
            }
            return null;
        } catch (IOException e) {
            LOGGER.info("httpclient请求失败", e);
            return null;
        } finally {
            if (httpResponse != null) {
                try {
                    EntityUtils.consume(httpResponse.getEntity());
                    httpResponse.close();
                } catch (IOException e) {
                    LOGGER.info("关闭response失败", e);
                }
            }
        }
    }


    /**
     * 高效表单Post请求
     * @param url
     * @param map
     * @return
     */
    public static String post(String url, Map<String, String> map) {
        // 创建默认的httpClient实例
        CloseableHttpClient httpClient = HttpRequest.getHttpClient();
        CloseableHttpResponse httpResponse = null;
        try {
            HttpPost post = new HttpPost(url);
            Charset charset = CHARSET_UTF8;
            List<NameValuePair> params = new ArrayList<>();
            map.forEach((key, value) -> params.add(new BasicNameValuePair(key, value)));

            post.setEntity(new UrlEncodedFormEntity(params, charset));

            httpResponse = httpClient.execute(post);
            // response实体
            HttpEntity httpEntity = httpResponse.getEntity();
            if (null != httpEntity) {
                String response = EntityUtils.toString(httpEntity);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                LOGGER.info("当前请求返回码: {}" + statusCode);
                if (statusCode == HttpStatus.SC_OK) {
                    // 成功
                    return response;
                } else {
                    return null;
                }
            }
            return null;
        } catch (IOException e) {
            LOGGER.info("httpclient请求失败", e);
            return null;
        } finally {
            if (httpResponse != null) {
                try {
                    EntityUtils.consume(httpResponse.getEntity());
                    httpResponse.close();
                } catch (IOException e) {
                    LOGGER.info("关闭response失败", e);
                }
            }
        }
    }
}
