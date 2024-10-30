package top.cikipad.cikibot.imagebot.porn;


import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;


/**
 * HttpClient
 *
 * @author hekm
 * @date 2017-6-2
 *
 */
@Slf4j
@Component
public class LoliHttpClient {

    private static CloseableHttpClient normalClient = null;

    private static CloseableHttpClient sslClient = null;

    @PostConstruct
    public static void init() {
        normalClient = HttpClients.createDefault();
        try {
            sslClient = new SSLClient();
        } catch (Exception e) {
            log.error("",e);
        }
    }

    public static void shutdown() {
        if (normalClient != null) {
            try {
                normalClient.close();
            } catch (IOException e) {
                log.error("",e);
            }
        }

        if (sslClient != null) {
            try {
                sslClient.close();
            } catch (IOException e) {
                log.error("",e);
            }
        }
    }


    public static String postForBody(String url, String json,Map<String,String> headerMap) {

        CloseableHttpResponse response = null;
        String content = null;
        CloseableHttpClient httpclient = normalClient;
        if(url.startsWith("https")) {
            httpclient = sslClient;
        }

        try {

            HttpPost httppost = new HttpPost(url);
            //httppost.addHeader("Content-Type","application/json;charset=UTF-8");
            if(null != headerMap) {
                for(String key : headerMap.keySet()) {
                    httppost.addHeader(key, headerMap.get(key));
                }
            }

            if(null != json && !"".equals(json.trim())) {
                StringEntity se = new StringEntity(json,"utf-8");
                httppost.setEntity(se);
            }
            response = httpclient.execute(httppost);

            content = getBody(response);

        } catch (ClientProtocolException e) {
            log.error("",e);
        } catch (IOException e) {
            log.error("",e);
        }
        finally {
            if (response!=null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("",e);
                }
            }
        }

        return content;
    }


    public static String getBody(CloseableHttpResponse response) {
        String content = null;
        if(null != response.getEntity()) {
            try {
                content = EntityUtils.toString(response.getEntity());
                return content;
            }catch (Exception e) {
                log.error("",e);
            }
        }

        return content;
    }

    public static String get(String url,String authorizationKeyName,String authorization) {
        String content = null;
        CloseableHttpClient httpclient = normalClient;
        if(url.startsWith("https")) {
            httpclient = sslClient;
        }

        CloseableHttpResponse response = null;
        try {

            HttpGet httpget = new HttpGet(url);
            if(null != authorization) {
                httpget.addHeader(authorizationKeyName, authorization);
            }
            response = httpclient.execute(httpget);
            content = getBody(response);
        } catch (ClientProtocolException e) {
            log.error("",e);
        } catch (IOException e) {
            log.error("",e);
        }
        finally {
            if (response!=null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("",e);
                }
            }
        }

        return content;
    }


    public static String get(String url,Map<String, String> headers) {
        String content = null;
        CloseableHttpClient httpclient = normalClient;
        if(url.startsWith("https")) {
            httpclient = sslClient;
        }
        CloseableHttpResponse response = null;
        try {

            HttpGet httpget = new HttpGet(url);
            for (String headerkey:headers.keySet()) {
                httpget.addHeader(headerkey, headers.get(headerkey));
            }

            response = httpclient.execute(httpget);
            content = getBody(response);
        } catch (ClientProtocolException e) {
            log.error("",e);
        } catch (IOException e) {
            log.error("",e);
        }
        finally {
            if (response!=null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("",e);
                }
            }
        }
        return content;
    }


}
