package top.cikipad.cikibot.imagebot.porn.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import top.cikipad.cikibot.imagebot.porn.ImageSourceInterface;
import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;
import top.cikipad.cikibot.imagebot.porn.LoliHttpClient;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;


import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class LoliconSourceImpl implements ImageSourceInterface {


    public String url = "https://api.lolicon.app/setu/v2";

    @Override
    public String getType() {
        return SourceTypeConstant.LOLICON;
    }

    @Override
    public List<InputStream> getImageStream(Map<String, Object> params) {
        throw new UnsupportedOperationException("不支持此操作");
    }


    @Override
    public List<String> getImageUrl(Map<String, Object> params) {
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        Gson gson = new Gson();
        String reqJson = gson.toJson(standardParams(params));
        String s = LoliHttpClient.postForBody(url, reqJson, header);
        if (s == null) {
            return new ArrayList<>();
        }

        Map map = gson.fromJson(s, Map.class);
        List<Map<String, Map<String, Object>>> data = (List<Map<String, Map<String, Object>>>) map.get("data");

        List<String> ret = new ArrayList<>();
        for (Map<String, Map<String, Object>> one:data) {
            Map<String, Object> urlsData = one.get("urls");
            for (String key :urlsData.keySet()) {
                ret.add((String) urlsData.get(key));
            }
        }
        return ret;
    }


    @Override
    public List<ImageUrlEntity> getImageUrlEntity(Map<String, Object> params) {
        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json");
        Gson gson = new Gson();
        String reqJson = gson.toJson(standardParams(params));
        String s = LoliHttpClient.postForBody(url, reqJson, header);
        if (s == null) {
            return new ArrayList<>();
        }

        Map map = gson.fromJson(s, Map.class);
        List<Map<String, Map<String, Object>>> data = (List<Map<String, Map<String, Object>>>) map.get("data");

        List<ImageUrlEntity> ret = new ArrayList<>();
        for (Map<String, Map<String, Object>> one:data) {

            ImageUrlEntity entity = new ImageUrlEntity();
            List<String> oneRet = new ArrayList<>();

            entity.setUrls(oneRet);
            entity.setSource(getType());


            Map<String, Object> urlsData = one.get("urls");
            for (String key :urlsData.keySet()) {
                oneRet.add((String) urlsData.get(key));
            }

            try {
                StringBuilder displayString = new StringBuilder();

                String title = one.get("title") + "";

                String issultId =  new BigDecimal(one.get("pid") + "").longValue() + "";


                String userId =  new BigDecimal(one.get("uid") + "").longValue() + "";

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
            List<String> tagList = handleTags((String) params.get(ParamsConstant.TAG));
            if (tagList == null || tagList.isEmpty()) {
                params.remove(ParamsConstant.TAG);
            }
            else {
                params.put(ParamsConstant.TAG, tagList);
            }
        }


        return params;
    }


    public List<String> handleTags(String tags) {
        if (tags == null || tags.trim().length() == 0) {
            return new ArrayList<>();
        }
        String[] arr = tags.split("\\&");

        List<String> ret = new ArrayList<>();
        for (String tmp:arr) {
            ret.add(tmp);
        }
        return ret;
    }


}
