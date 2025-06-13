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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class YdSourceImpl implements ImageSourceInterface {

    //https://api.r10086.com/樱道随机图片api接口.php?图片系列=原神横屏系列1&参数=json
    public String url = "https://api.r10086.com/樱道随机图片api接口.php";

    @Override
    public String getType() {
        return SourceTypeConstant.YD;
    }

    @Override
    public List<InputStream> getImageStream(Map<String, Object> params) {
        throw new UnsupportedOperationException("不支持此操作");
    }


    @Override
    public List<String> getImageUrl(Map<String, Object> params) {
        String url = handleReqUrl(params);
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "PostmanRuntime/7.35.0");
        headers.put("Connection", "keep-alive");

        String s = LoliHttpClient.get(url, headers);
        if (s == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Map map = gson.fromJson(s, Map.class);
        String img = (String) map.get("img");

        List<String> ret = new ArrayList<>();

        ret.add(img);

        return handleRespUrl(ret, params);
    }

    @Override
    public Map<String, Object> standardParams(Map<String, Object> params) {
        if (params.containsKey(ParamsConstant.TAG)) {
            if (params.get(ParamsConstant.TAG) != null && ((String)params.get(ParamsConstant.TAG)).trim().length()>0 ) {
                params.put(ParamsConstant.YD_TYPE, params.get(ParamsConstant.TAG));
            }
            else {
                params.put(ParamsConstant.YD_TYPE, "P站系列4");
            }
            params.remove(ParamsConstant.TAG);
        }
        else {
            params.put(ParamsConstant.YD_TYPE, "P站系列4");
        }

        params.put("参数", "json");

        return params;
    }


    public List<String> handleRespUrl(List<String> urls,Map<String, Object> params) {
        List<String> ret = new ArrayList<>();
        for (String url:urls) {
            ret.add("https:" + url);
        }
        return ret;
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
        String encodedKey = key;
        Object encodedValue = value;
        return encodedKey + "=" + encodedValue;
    }


//    private static String encodeUrlParam(String key, Object value) {
//        try {
//            String encodedKey = URLEncoder.encode(key, "UTF-8");
//            String encodedValue = URLEncoder.encode(value.toString(), "UTF-8");
//            return encodedKey + "=" + encodedValue;
//        } catch (UnsupportedEncodingException e) {
//            // Handle encoding exception as per your requirement
//            log.error("",e);
//            return "";
//        }
//    }


}
