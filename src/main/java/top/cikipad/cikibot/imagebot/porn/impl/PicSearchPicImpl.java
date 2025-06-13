package top.cikipad.cikibot.imagebot.porn.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import top.cikipad.cikibot.imagebot.porn.ImageSourceInterface;
import top.cikipad.cikibot.imagebot.porn.ImageSourceManager;
import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;
import top.cikipad.cikibot.imagebot.porn.LoliHttpClient;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;


import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;


/**
 * 仅用于处理搜图,不注册
 */
@Slf4j
public class PicSearchPicImpl implements ImageSourceInterface {

    //https://saucenao.com/search.php?db=999&output_type=2&testmode=1&numres=16&url=http%3A%2F%2Fsaucenao.com%2Fimages%2Fstatic%2Fbanner.gif&api_key=ca6cf55dab93b95ba77344379bd63fe6d834e232
    private String url = "https://saucenao.com/search.php";

    @Value("${saucenao.apikey:null}")
    private String api_key;

    private String txImageUri = "https://gchat.qpic.cn/gchatpic_new/0/0-0-%s/0";

    @Override
    public String getType() {
        return SourceTypeConstant.PSP_SEARCH;
    }

    @Override
    public boolean visible() {
        return false;
    }

    @Override
    public List<InputStream> getImageStream(Map<String, Object> params) {
        throw new UnsupportedOperationException("不支持此操作");
    }


    @Override
    public List<String> getImageUrl(Map<String, Object> params) {
        throw new UnsupportedOperationException("不支持此操作");
    }

    @Override
    public List<ImageUrlEntity> getImageUrlEntity(Map<String, Object> params) {

        Map<String, Object> oriParams = new HashMap<>();
        oriParams.putAll(params);

        Map<String, Object> oriParams2 = new HashMap<>();
        oriParams2.putAll(params);

        String url = handleReqUrl(params);
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0");



        String s = LoliHttpClient.get(url, headers);
        if (s == null) {
            return new ArrayList<>();
        }

        Gson gson = new Gson();


        Map map = gson.fromJson(s, Map.class);

        List<Map<String, Object>> resultList = (List<Map<String, Object>>) map.get("results");


        Map<String, Object> matched = null;

        for (int i=0;i<5&&i<resultList.size();i++) {

            matched = resultList.get(i);
            Map<String, Object> data = (Map<String, Object>) matched.get("data");

            if (data.containsKey("pixiv_id")) {
                break;
            }

        }


        if (matched == null) {
            return new ArrayList<>();
        }

        Map<String, Object> header = (Map<String, Object>) matched.get("header");
        Map<String, Object> data = (Map<String, Object>) matched.get("data");

        String pixivId =  new BigDecimal(data.get("pixiv_id") + "").longValue() + "";
        String memberId =  new BigDecimal(data.get("member_id") + "").longValue() + "";



        String thumbnail = header.get("thumbnail") + "";

        String similarity = header.get("similarity") + "";

        String detail = printMap(data);

        ImageUrlEntity firstEntity = new ImageUrlEntity();

        StringBuilder displayString = new StringBuilder();

        if (pixivId!=null && pixivId.trim().length() > 0) {
            displayString.append("匹配到了pixiv结果").append("\r\n");
        }
        else {
            displayString.append("未匹配到pixiv结果，其他匹配结果如下").append("\r\n");
        }

        displayString.append("相似度:").append(similarity).append("\r\n");

        displayString.append("明细如下:").append("\r\n").append(detail);




        List<String> urlList = new ArrayList<>();
        urlList.add(thumbnail);

        firstEntity.setDisplayString(displayString.toString());
        firstEntity.setUrls(urlList);

        List<ImageUrlEntity> ret = new ArrayList<>();

        ret.add(firstEntity);

        if (params.containsKey(ParamsConstant.PSP_NEED_RELATION)) {
            oriParams.put(ParamsConstant.SEARCH_TYPE_KEY,ParamsConstant.SEARCH_TYPE_PID);
            oriParams.put(ParamsConstant.TAG, pixivId);
            ret.addAll(ImageSourceManager.getInstance().getImageUrlsEntity(SourceTypeConstant.PIC_SEARCH ,oriParams));
        }

        if (params.containsKey(ParamsConstant.PSP_NEED_MORE)) {
            oriParams2.put(ParamsConstant.SEARCH_TYPE_KEY,ParamsConstant.SEARCH_TYPE_UID);
            oriParams2.put(ParamsConstant.TAG, memberId);
            ret.addAll(ImageSourceManager.getInstance().getImageUrlsEntity(SourceTypeConstant.PIC_SEARCH ,oriParams2));
        }



        return ret;
    }


    public static String printMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "Map is empty.";
        }

        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key != null && value != null) {
                result.append(key).append(": ").append(value).append("\r\n");
            } else {
                result.append("Invalid key-value pair: ").append(key).append(" - ").append(value).append("\r\n");
            }
        }

        return result.toString();
    }

    private String getUrlFromImageUrls(Map<String, Object> imageUrls ,Map<String, Object> params) {
        if (imageUrls == null) {
            return null;
        }
        String original = "";
        String large = "";
        if (imageUrls.containsKey("original")) {
            original = (String) imageUrls.get("original");
        }

        if (imageUrls.containsKey("large")) {
            large = (String) imageUrls.get("large");
        }

        if (params.containsKey(ParamsConstant.SIZE) && ParamsConstant.ORIGINAL_SIZE.equalsIgnoreCase((String) params.get(ParamsConstant.SIZE))) {
            return original;
        }

        if (original.toLowerCase().endsWith(".gif")) {
            return original;
        }
        else if (large == null || large.length() == 0) {
            return original;
        }
        else {
            return large;
        }
    }

    @Override
    public Map<String, Object> standardParams(Map<String, Object> params) {

        params.put("api_key", api_key);

        params.put("output_type", 2);

        params.put("testmode", 1);

        params.put("numres", 16);



        String resource_url = "";
        if (params.containsKey(ParamsConstant.ORI_MSG)) {
            if (params.get(ParamsConstant.ORI_MSG) != null && ((String) params.get(ParamsConstant.ORI_MSG)).trim().length() > 0) {
                resource_url = parseAndGenerateURL((String) params.get(ParamsConstant.ORI_MSG), txImageUri);
            }
            params.remove(ParamsConstant.ORI_MSG);
        }

        if (resource_url == null || resource_url.trim().length() == 0) {
            throw new IllegalArgumentException();
        }

        params.put("url", resource_url);



        return params;
    }


    public List<String> handleRespUrl(List<String> urls,Map<String, Object> params) {

        List<String> ret = new ArrayList<>();
        for (String url:urls) {
            String tmp = url.replace(ParamsConstant.ORI_HOST, ParamsConstant.PROXY_HOST);
            ret.add(processURL(tmp));
        }
        return ret;

    }


    public static String parseAndGenerateURL(String input, String formatString) {
        if (input == null || input.isEmpty() || formatString == null || formatString.isEmpty()) {
            return null;
        }

        String startTag = "[mirai:image:{";
        String endTag = "}";

        int startIndex = input.indexOf(startTag);
        int endIndex = input.indexOf(endTag);

        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            String content = input.substring(startIndex + startTag.length(), endIndex);
            String picUri = content.replace("-", "");
            return String.format(formatString, picUri);
        }

        return null;
    }

    public static String processURL(String url) {
        String prefixToRemove = "/c/";
        int prefixIndex = url.indexOf(prefixToRemove);

        if (prefixIndex != -1) {
            int endIndex = url.indexOf('/', prefixIndex + prefixToRemove.length());
            if (endIndex != -1) {
                return url.substring(0, prefixIndex) + url.substring(endIndex);
            }
        }

        return url;
    }


    public String handleReqUrl(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        sb.append(url).append("?").append(generateUrlParams(standardParams(params)));
        return sb.toString();
    }


    public static String generateUrlParams(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();

        //这个来源要求db在第一位
        sb.append(encodeUrlParam("db", 999));
        sb.append("&");

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
