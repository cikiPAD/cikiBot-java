package top.cikipad.cikibot.imagebot.porn.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import top.cikipad.cikibot.imagebot.porn.ImageSourceInterface;
import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;
import top.cikipad.cikibot.imagebot.porn.LoliHttpClient;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;


import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class NyanSourceImpl implements ImageSourceInterface {


    public String url = "https://sex.nyan.run/api/v2/";

    @Override
    public String getType() {
        return SourceTypeConstant.NYAN;
    }

    @Override
    public List<InputStream> getImageStream(Map<String, Object> params) {
        throw new UnsupportedOperationException("不支持此操作");
    }


    @Override
    public List<String> getImageUrl(Map<String, Object> params) {
        String url = handleReqUrl(params);
        String s = LoliHttpClient.get(url, null, null);
        if (s == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Map map = gson.fromJson(s, Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) map.get("data");

        List<String> ret = new ArrayList<>();
        for (Map<String, Object> one:data) {
            String imageUrl = (String) one.get("url");
            ret.add(imageUrl);
        }
        return handleRespUrl(ret, params);
    }


    @Override
    public List<ImageUrlEntity> getImageUrlEntity(Map<String, Object> params) {
        String url = handleReqUrl(params);
        String s = LoliHttpClient.get(url, null, null);
        if (s == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Map map = gson.fromJson(s, Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) map.get("data");

        List<ImageUrlEntity> ret = new ArrayList<>();
        for (Map<String, Object> one:data) {

            ImageUrlEntity entity = new ImageUrlEntity();
            List<String> oneRet = new ArrayList<>();

            entity.setUrls(oneRet);
            entity.setSource(getType());


            String imageUrl = (String) one.get("url");
            oneRet.add(imageUrl);
            entity.setUrls(handleRespUrl(oneRet, params));

            try {
                StringBuilder displayString = new StringBuilder();

                String title = one.get("title") + "";

                String issultId =  new BigDecimal(one.get("pid") + "").longValue() + "";


                String userId =  new BigDecimal(one.get("author_uid") + "").longValue() + "";

                String userName =  one.get("author") + "";

                String tags = String.join(",",(List<String>) one.get("tags"));

                displayString.append("作品标题:").append(title).append("\r\n");

                displayString.append("作品id:").append(issultId).append("\r\n");

                displayString.append("作者id:").append(userId).append("\r\n");

                displayString.append("作者名称:").append(userName).append("\r\n");

                displayString.append("图片来源:").append(getType()).append("\r\n");

                displayString.append("tags:").append(tags).append("\r\n");

                entity.setDisplayString(displayString.toString());


            }
            catch (Exception e) {
                log.error("",e);
            }

            ret.add(entity);

        }
        return ret;
    }

    @Override
    public Map<String, Object> standardParams(Map<String, Object> params) {
        if (params.containsKey(ParamsConstant.TAG)) {
            if (params.get(ParamsConstant.TAG) != null && ((String)params.get(ParamsConstant.TAG)).trim().length()>0 ) {
                params.put(ParamsConstant.NYAN_KEYWORD, params.get(ParamsConstant.TAG));
            }
            params.remove(ParamsConstant.TAG);
        }

        if (params.containsKey(ParamsConstant.R18)) {
            int r18 = (int) params.get(ParamsConstant.R18);
            if (r18 == 1) {
                params.put(ParamsConstant.R18, true);
            }
            else {
                params.put(ParamsConstant.R18, false);
            }
        }


        return params;
    }


    public List<String> handleRespUrl(List<String> urls,Map<String, Object> params) {
        //System.out.println(urls.toString());
        if (params.containsKey(ParamsConstant.SIZE)) {
            String size = (String) params.get(ParamsConstant.SIZE);
            if (ParamsConstant.ORIGINAL_SIZE.equalsIgnoreCase(size)) {
                return urls;
            }
            else {
                List<String> ret = new ArrayList<>();
                for (String url:urls) {
                    String tmp = url.replace("img-original", "img-master").replace("sex.nyan.xyz", ParamsConstant.PROXY_HOST);
                    tmp = tmp.substring(0, tmp.lastIndexOf("."));
                    tmp = tmp + "_master1200.jpg";
                    ret.add(tmp);
                }
                return ret;
            }
        }
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
