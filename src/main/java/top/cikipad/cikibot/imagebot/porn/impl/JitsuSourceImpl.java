package top.cikipad.cikibot.imagebot.porn.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import top.cikipad.cikibot.imagebot.porn.ImageSourceInterface;
import top.cikipad.cikibot.imagebot.porn.LoliHttpClient;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;


import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class JitsuSourceImpl implements ImageSourceInterface {

    //https://moe.jitsu.top/api/?sort=jitsu&size=original&type=json&num=5&proxy=i.pixiv.re
    public String url = "https://moe.jitsu.top/img/";

    @Override
    public String getType() {
        return SourceTypeConstant.JITSU;
    }

    @Override
    public List<InputStream> getImageStream(Map<String, Object> params) {
        throw new UnsupportedOperationException("不支持此操作");
    }


    @Override
    public List<String> getImageUrl(Map<String, Object> params) {
        String url = handleReqUrl(params);
        String s = LoliHttpClient.get(url, "User-Agent", "PostmanRuntime/7.35.0");
        if (s == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Map map = gson.fromJson(s, Map.class);
        List<String> imageUrls = (List<String>) map.get("pics");

        List<String> ret = new ArrayList<>();
        for (String imageUrl:imageUrls) {
            ret.add(imageUrl);
        }
        return handleRespUrl(ret, params);
    }

    @Override
    public Map<String, Object> standardParams(Map<String, Object> params) {
        if (params.containsKey(ParamsConstant.TAG)) {
            params.remove(ParamsConstant.TAG);
        }

        params.put("type", "json");
        params.put("proxy", ParamsConstant.PROXY_HOST);

        if (params.containsKey(ParamsConstant.R18)) {
            int r18 = (int) params.get(ParamsConstant.R18);
            params.remove(ParamsConstant.R18);
            if (r18 == 1) {
                params.put(ParamsConstant.JITSU_SORT, "r18");
            }
            else {
                params.put(ParamsConstant.JITSU_SORT, "pixiv");
            }
        }


        return params;
    }


    public List<String> handleRespUrl(List<String> urls,Map<String, Object> params) {
        return urls;
    }


    public String handleReqUrl(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        sb.append(url).append("?").append(generateUrlParams(standardParams(params)));
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
        try {
            String encodedKey = URLEncoder.encode(key, "UTF-8");
            String encodedValue = URLEncoder.encode(value.toString(), "UTF-8");
            return encodedKey + "=" + encodedValue;
        } catch (UnsupportedEncodingException e) {
            // Handle encoding exception as per your requirement
            log.error("",e);
            return "";
        }
    }


}
