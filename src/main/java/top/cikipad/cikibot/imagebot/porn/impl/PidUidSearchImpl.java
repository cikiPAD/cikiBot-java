package top.cikipad.cikibot.imagebot.porn.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import top.cikipad.cikibot.imagebot.porn.ImageSourceInterface;
import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;
import top.cikipad.cikibot.imagebot.porn.LoliHttpClient;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;


import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仅用于处理搜图,不注册
 */
@Slf4j
public class PidUidSearchImpl implements ImageSourceInterface {

    //https://api.acgmx.com/public/ranking?ranking_type=illust&mode=daily&date=2023-12-19&per_page=20&page=1
    private String pid_url = "https://api.acgmx.com/illusts/detail";

    private String uid_url = "https://api.acgmx.com/public/search/users/illusts";

    @Value("${acgmx.token:null}")
    private String token;

    @Override
    public String getType() {
        return SourceTypeConstant.PIC_SEARCH;
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
        String url = handleReqUrl(params);

        Map<String, String> headers = new HashMap<>();
        headers.put("token", token);
        String s = LoliHttpClient.get(url, headers);
        if (s == null) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();


        Map map = gson.fromJson(s, Map.class);


        List<Map<String, Object>> data = new ArrayList<>();

        boolean isPidSearch = false;
        if (params.containsKey(ParamsConstant.SEARCH_TYPE_KEY)) {
            if (ParamsConstant.SEARCH_TYPE_PID.equalsIgnoreCase((String) params.get(ParamsConstant.SEARCH_TYPE_KEY))) {
                isPidSearch = true;
                data.add((Map<String, Object>) map.get("data"));
            }
            else {
                data = (List<Map<String, Object>>) map.get("illusts");
            }
        }





        List<ImageUrlEntity> ret = new ArrayList<>();

        int num = 5;

        if (params.containsKey(ParamsConstant.NUM)) {
            num = (int) params.get(ParamsConstant.NUM);
        }

        int count = 0;
        for (Map<String, Object> one:data) {

            String type = (String) one.get("type");

            if ("manga".equalsIgnoreCase(type)) {
                continue;
            }

            ImageUrlEntity entity = new ImageUrlEntity();
            List<String> oneRet = new ArrayList<>();

            entity.setUrls(oneRet);
            entity.setSource(getType());

            StringBuilder displayString = new StringBuilder();

            try {

                String title = one.get("title") + "";


                String issultId = "";
                if (isPidSearch) {
                    issultId =  new BigDecimal(one.get("illust") + "").longValue() + "";

                }else {
                    issultId =  new BigDecimal(one.get("id") + "").longValue() + "";
                }



                Map<String, Object> author = (Map<String, Object>) one.get("user");

                String userId =  new BigDecimal(author.get("id") + "").longValue() + "";

                String userName =  author.get("name") + "";

                List<Map<String, Object>> tagsMap = (List<Map<String, Object>>) one.get("tags");

                tagsMap.stream().map(p->p.get("name") + "").collect(Collectors.toSet());

                Set<String> tagSet = tagsMap.stream().map(p->p.get("name") + "").collect(Collectors.toSet());

                //过滤r18
                if ( (tagSet.contains("R-18") || tagSet.contains("r-18")) && (int) params.get(ParamsConstant.R18) != 1) {
                    continue;
                }

                String tags = String.join(",",tagSet);


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



            List<Map<String, Object>> metaPages = (List<Map<String, Object>>) one.get("meta_pages");
            if (metaPages!=null && !metaPages.isEmpty()) {

                int meta_pages_count = 0;

                for (Map<String, Object> onePage: metaPages) {
                    String oneUrl = getUrlFromImageUrls((Map<String, Object>)(onePage.get("image_urls")), params);
                    if (oneUrl!=null & oneUrl.length()!=0) {
                        oneRet.add(oneUrl);
                        meta_pages_count++;
                    }
                    if (meta_pages_count >= 3) {
                        break;
                    }
                }
            }
            else {
                Map<String, Object> imageUrls = (Map<String, Object>) one.get("image_urls");
                String oneUrl = null;
                if (imageUrls!= null) {
                    oneUrl = getUrlFromImageUrls(imageUrls, params);
                }
                else {
                    oneUrl = (String) one.get("large");
                }
                if (oneUrl!=null && oneUrl.length()!=0) {
                    oneRet.add(oneUrl);
                }
            }

            if (!entity.getUrls().isEmpty()) {
                ret.add(entity);
            }

            if (!entity.getUrls().isEmpty()) {
                entity.setUrls(handleRespUrl(entity.getUrls(), params));
            }


            count++;
            if (count>=num) {
                break;
            }
        }
        return ret;
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

        params.put("offset", 30);



        String q = "";
        if (params.containsKey(ParamsConstant.TAG)) {
            if (params.get(ParamsConstant.TAG) != null && ((String) params.get(ParamsConstant.TAG)).trim().length() > 0) {
                q = (String) params.get(ParamsConstant.TAG);
            }
            params.remove(ParamsConstant.TAG);
        }


        if (params.containsKey(ParamsConstant.SEARCH_TYPE_KEY)) {
            if (ParamsConstant.SEARCH_TYPE_PID.equalsIgnoreCase((String) params.get(ParamsConstant.SEARCH_TYPE_KEY))) {

                params.put("illustId", q);
            }
            else if (ParamsConstant.SEARCH_TYPE_UID.equalsIgnoreCase((String) params.get(ParamsConstant.SEARCH_TYPE_KEY))) {

                params.put("id", q);

            }
        }


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

        String url = uid_url;

        if (params.containsKey(ParamsConstant.SEARCH_TYPE_KEY)) {
            if (ParamsConstant.SEARCH_TYPE_PID.equalsIgnoreCase((String) params.get(ParamsConstant.SEARCH_TYPE_KEY))) {

                url = pid_url;
            }
            else if (ParamsConstant.SEARCH_TYPE_UID.equalsIgnoreCase((String) params.get(ParamsConstant.SEARCH_TYPE_KEY))) {

                url = uid_url;

            }
        }
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
