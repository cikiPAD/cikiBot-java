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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仅用于处理排行榜,不注册
 */
@Slf4j
public class AcgMxSourceImpl implements ImageSourceInterface {

    private String url = "https://api.acgmx.com/illusts/ranking";

    @Value("${acgmx.token:null}")
    private String token;

    @Override
    public String getType() {
        return SourceTypeConstant.ACGMX;
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
        List<Map<String, Object>> data = (List<Map<String, Object>>) map.get("illusts");


        if (data.isEmpty()) {
            log.info("未生成这个时间的日榜，调整日期");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String date = dateFormat.format(new Date(System.currentTimeMillis() - 2*24*60*60*1000L));
            oriParams.put(ParamsConstant.ACGMX_DATE, date);
            String urlChange = handleReqUrl(oriParams);
            String twiceStr = LoliHttpClient.get(urlChange, headers);
            if (twiceStr == null) {
                return new ArrayList<>();
            }
            Map twiceMap = gson.fromJson(twiceStr, Map.class);
            data = (List<Map<String, Object>>) twiceMap.get("illusts");
        }

        List<String> ret = new ArrayList<>();

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

            List<Map<String, Object>> metaPages = (List<Map<String, Object>>) one.get("meta_pages");
            if (metaPages!=null && !metaPages.isEmpty()) {

                int meta_pages_count = 0;

                for (Map<String, Object> onePage: metaPages) {
                    String oneUrl = getUrlFromImageUrls((Map<String, Object>)(onePage.get("image_urls")), params);
                    if (oneUrl!=null & oneUrl.length()!=0) {
                        ret.add(oneUrl);
                        meta_pages_count++;
                    }
                    if (meta_pages_count >= 3) {
                        break;
                    }
                }
            }
            else {
                Map<String, Object> imageUrls = (Map<String, Object>) one.get("image_urls");
                String oneUrl = getUrlFromImageUrls(imageUrls, params);
                if (oneUrl!=null & oneUrl.length()!=0) {
                    ret.add(oneUrl);
                }
            }

            count++;
            if (count>=num) {
                break;
            }
        }
        return handleRespUrl(ret, params);
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
        List<Map<String, Object>> data = (List<Map<String, Object>>) map.get("illusts");


        if (data.isEmpty()) {
            log.info("未生成这个时间的日榜，调整日期");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String date = dateFormat.format(new Date(System.currentTimeMillis() - 2*24*60*60*1000L));
            oriParams.put(ParamsConstant.ACGMX_DATE, date);
            String urlChange = handleReqUrl(oriParams);
            String twiceStr = LoliHttpClient.get(urlChange, headers);
            if (twiceStr == null) {
                return new ArrayList<>();
            }
            Map twiceMap = gson.fromJson(twiceStr, Map.class);
            data = (List<Map<String, Object>>) twiceMap.get("illusts");
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

                String issultId =  new BigDecimal(one.get("id") + "").longValue() + "";

                Map<String, Object> author = (Map<String, Object>) one.get("user");

                String userId =  new BigDecimal(author.get("id") + "").longValue() + "";

                String userName =  author.get("name") + "";

                List<Map<String, Object>> tagsMap = (List<Map<String, Object>>) one.get("tags");

                tagsMap.stream().map(p->p.get("name") + "").collect(Collectors.toSet());

                String tags = String.join(",",tagsMap.stream().map(p->p.get("name") + "").collect(Collectors.toSet()));


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
                String oneUrl = getUrlFromImageUrls(imageUrls, params);
                if (oneUrl!=null & oneUrl.length()!=0) {
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


        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date = dateFormat.format(new Date(System.currentTimeMillis() - 24*60*60*1000L));

        if (!params.containsKey(ParamsConstant.ACGMX_DATE)) {
            params.put(ParamsConstant.ACGMX_DATE, date);
        }


        if (params.containsKey(ParamsConstant.TAG)) {
            if (params.get(ParamsConstant.TAG) != null && ((String)params.get(ParamsConstant.TAG)).trim().length()>0 ) {
                params.put(ParamsConstant.ACGMX_MODE, params.get(ParamsConstant.TAG));
            }
            else {
                params.put(ParamsConstant.ACGMX_MODE, ParamsConstant.ACGMX_DEFAULT_MODE);
            }
            params.remove(ParamsConstant.TAG);
        }
        else {
            params.put(ParamsConstant.ACGMX_MODE, ParamsConstant.ACGMX_DEFAULT_MODE);
        }

        String mode = (String) params.get(ParamsConstant.ACGMX_MODE);

        if (params.containsKey(ParamsConstant.R18)) {
            int r18 = (int) params.get(ParamsConstant.R18);
            if (r18 == 0 && mode.toLowerCase().contains("r18")) {
                params.put(ParamsConstant.ACGMX_MODE, ParamsConstant.ACGMX_DEFAULT_MODE);
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
