package top.cikipad.cikibot.imagebot.porn.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import top.cikipad.cikibot.imagebot.porn.ImageSourceInterface;
import top.cikipad.cikibot.imagebot.porn.LoliHttpClient;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class SimpleSourceImpl implements ImageSourceInterface {



    @Override
    public List<InputStream> getImageStream(Map<String, Object> params) {
        throw new UnsupportedOperationException("不支持此操作");
    }

    abstract String getUrl();


    @Override
    public List<String> getImageUrl(Map<String, Object> params) {
        String url = handleReqUrl(params);
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "PostmanRuntime/7.35.0");
        //headers.put("Connection", "keep-alive");

        List<String> urls = new ArrayList<>();
        if(params.containsKey(ParamsConstant.NUM)) {
            int num = (int) params.get(ParamsConstant.NUM);

            for (int i=0;i<num;i++) {
                String resp = LoliHttpClient.get(url, headers);
                urls.addAll(getImageUrlsFromResp(resp));
            }
        }
        else {
            String resp = LoliHttpClient.get(url, headers);
            urls.addAll(getImageUrlsFromResp(resp));
        }

        return urls;
    }

    abstract List<String> getImageUrlsFromResp(String resp);




    public String handleReqUrl(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        sb.append(getUrl()).append("?").append(generateUrlParams(standardParams(params)));
        return sb.toString();
    }


    public static String generateUrlParams(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof List) {
                List<?> listValue = (List<?>) value;
                for (Object item : listValue) {
                    sb.append(encodeUrlParam(key, item));
                    sb.append("&");
                }
            } else {
                sb.append(encodeUrlParam(key, value));
                sb.append("&");
            }
        }

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1); // Remove the trailing '&'
        }

        return sb.toString();
    }

    private static String encodeUrlParam(String key, Object value) {
        String encodedKey = key;
        Object encodedValue = value;
        return encodedKey + "=" + encodedValue;
    }

}
