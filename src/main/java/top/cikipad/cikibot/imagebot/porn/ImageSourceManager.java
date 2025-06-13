package top.cikipad.cikibot.imagebot.porn;



import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.cikipad.cikibot.constant.CommonConstant;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;
import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;
import top.cikipad.cikibot.imagebot.porn.impl.*;
import top.cikipad.cikibot.util.ConfigManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 多个文件来源的统一入口
 * 不引入spring，在kotlin插件环境下使用
 */
@Slf4j
@Component
public class ImageSourceManager {

    public static ImageSourceManager instance = new ImageSourceManager();

    private String currentTypeNormal = SourceTypeConstant.LOLICON;

    public static final String CURRENT_TYPE_NORMAL = "current_type_normal";

    private String currentTypeSp = SourceTypeConstant.LOLICON;

    public static final String CURRENT_TYPE_SP = "current_type_sp";

    private Map<String, Object> additionParamNormal = new ConcurrentHashMap<>();

    private Map<String, Object> additionParamSp = new ConcurrentHashMap<>();

    @Autowired
    private ConfigManager configManager;


    private ImageSourceManager() {
    }

    public static ImageSourceManager getInstance() {
        return instance;
    }

    private Map<String, ImageSourceInterface> sources = new HashMap<>();

    public void register(ImageSourceInterface source) {
        sources.put(source.getType(), source);
    }

    @PostConstruct
    public void init() {
        register(new LoliconSourceImpl());
        register(new NyanSourceImpl());
        register(new JitsuSourceImpl());
        register(new YdSourceImpl());
        register(new XljSourceImpl());
        register(new VvhanSourceImpl());
        register(new TangdouzSourceImpl());
        register(new StarChenTSourceImpl());
        register(new AcgMxSourceImpl());
        register(new AcgMxSourceImplNew());
        register(new PidUidSearchImpl());
        register(new PicSearchPicImpl());



        //加载当前配置
        currentTypeNormal = configManager.getProperty(CURRENT_TYPE_NORMAL) == null ? SourceTypeConstant.LOLICON : configManager.getProperty(CURRENT_TYPE_NORMAL);
        log.info("加载当前群组配置图库:{}", currentTypeNormal);
        currentTypeSp = configManager.getProperty(CURRENT_TYPE_SP) == null ? SourceTypeConstant.LOLICON : configManager.getProperty(CURRENT_TYPE_SP);
        log.info("加载当前个人配置图库:{}", currentTypeSp);
        //加载额外参数
        String additionParamNormalStr = configManager.getProperty(CommonConstant.ADDITION_PARAM_NORMAL);
        String additionParamSpStr = configManager.getProperty(CommonConstant.ADDITION_PARAM_SP);

        if (StringUtils.hasLength(additionParamNormalStr)) {
            additionParamNormal = JSONObject.toJavaObject((JSON) JSON.parse(additionParamNormalStr), Map.class);
        }
        if (StringUtils.hasLength(additionParamSpStr)) {
            additionParamSp = JSONObject.toJavaObject((JSON) JSON.parse(additionParamSpStr), Map.class);
        }

    }


    public List<InputStream> getImageStream(String type, Map<String, Object> params) {
        if (sources.containsKey(type)) {
            return sources.get(type).getImageStream(params);
        }
        else {
            throw new IllegalArgumentException("不支持类型");
        }
    }


    public List<String> getImageUrls(String type, Map<String, Object> params) {
        try {
            if (sources.containsKey(type)) {
                return sources.get(type).getImageUrl(filterNullValues(params));
            } else {
                return new ArrayList<>();
            }
        }
        catch (Exception e){
            return new ArrayList<>();
        }
    }


    public List<ImageUrlEntity> getImageUrlsEntity(String type, Map<String, Object> params) {
        try {
            if (sources.containsKey(type)) {
                return sources.get(type).getImageUrlEntity(filterNullValues(params));
            } else {
                return new ArrayList<>();
            }
        }
        catch (Exception e){
            log.error("",e);
            return new ArrayList<>();
        }
    }



    public List<ImageUrlEntity> getImageUrlsEntity(Map<String, Object> params, boolean isSp, boolean needAddParm) {
        try {

            if (isSp) {

                if (needAddParm) {
                    if (!additionParamSp.isEmpty()) {
                        putToMapForWithPrefix(params, additionParamSp, currentTypeSp);
                    }
                }


                if (sources.containsKey(currentTypeSp)) {
                    return sources.get(currentTypeSp).getImageUrlEntity(filterNullValues(params));
                } else {
                    return new ArrayList<>();
                }


            }
            else {

                if (needAddParm) {
                    if (!additionParamNormal.isEmpty()) {
                        putToMapForWithPrefix(params, additionParamNormal, currentTypeNormal);
                    }
                }


                if (sources.containsKey(currentTypeNormal)) {
                    return sources.get(currentTypeNormal).getImageUrlEntity(filterNullValues(params));
                } else {
                    return new ArrayList<>();
                }

            }

        }
        catch (Exception e){
            log.error("", e);
            return new ArrayList<>();
        }
    }




    public List<String> getImageUrlsNormal(Map<String, Object> params, boolean needAddParm) {
        try {
            if (needAddParm) {
                if (!additionParamNormal.isEmpty()) {
                    putToMapForWithPrefix(params, additionParamNormal, currentTypeNormal);
                }
            }


            if (sources.containsKey(currentTypeNormal)) {
                return sources.get(currentTypeNormal).getImageUrl(filterNullValues(params));
            } else {
                return new ArrayList<>();
            }
        }
        catch (Exception e){
            return new ArrayList<>();
        }
    }


    public List<String> getImageUrlsSp(Map<String, Object> params, boolean needAddParm) {
        try {

            if (needAddParm) {
                if (!additionParamSp.isEmpty()) {
                    putToMapForWithPrefix(params, additionParamSp, currentTypeSp);
                }
                
            }


            if (sources.containsKey(currentTypeSp)) {
                return sources.get(currentTypeSp).getImageUrl(filterNullValues(params));
            } else {
                return new ArrayList<>();
            }
        }
        catch (Exception e){
            return new ArrayList<>();
        }
    }

    public void clearAdditionParamNormal() {
        additionParamNormal = new ConcurrentHashMap<>();

        String str = JSONObject.toJSONString(additionParamNormal);
        configManager.setProperty(CommonConstant.ADDITION_PARAM_NORMAL, str);
    }

    public void clearAdditionParamSp() {
        additionParamSp = new ConcurrentHashMap<>();

        String str = JSONObject.toJSONString(additionParamSp);
        configManager.setProperty(CommonConstant.ADDITION_PARAM_SP, str);
    }

    public void putAdditionParamNormal(String key, Object value) {
        if (key == null || value == null) {
            return;
        }
        Object legalValue = castSomeValue(key, value);
        additionParamNormal.put(key, legalValue);

        String str = JSONObject.toJSONString(additionParamNormal);
        configManager.setProperty(CommonConstant.ADDITION_PARAM_NORMAL, str);
    }

    public void putAdditionParamSp(String key, Object value) {
        if (key == null || value == null) {
            return;
        }
        Object legalValue = castSomeValue(key, value);
        additionParamSp.put(key, legalValue);

        String str = JSONObject.toJSONString(additionParamSp);
        configManager.setProperty(CommonConstant.ADDITION_PARAM_SP, str);
    }


    public static Map<String, Object> filterNullValues(Map<String, Object> params) {
        Map<String, Object> filteredParams = new HashMap<>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value != null) {
                filteredParams.put(key, value);
            }
        }

        return filteredParams;
    }



    public boolean setCurrentTypeNormal(String type) {

        if (sources.containsKey(type)&& sources.get(type).visible()) {
            currentTypeNormal = type;

            configManager.setProperty(CURRENT_TYPE_NORMAL, type);
            log.info("设置普通图库为:{}",type);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean setCurrentTypeSp(String type) {

        if (sources.containsKey(type) && sources.get(type).visible()) {
            currentTypeSp = type;

            configManager.setProperty(CURRENT_TYPE_SP, type);
            log.info("设置个人图库为:{}",type);
            return true;
        }
        else {
            return false;
        }
    }

    public String getAllType() {
        return String.join(",",sources.keySet().stream().filter((p)->sources.get(p).visible()).collect(Collectors.toSet()));
    }


    public Object castSomeValue(String key, Object value) {
        if (ParamsConstant.NUM.equalsIgnoreCase(key)) {
            int num = castAutoSavedDataToInteger(value);
            if (num<=0 || num > 10) {
                log.info("num数量非法，设置为2");
                num = 2;
            }
            return num;
        }

        if (ParamsConstant.R18.equalsIgnoreCase(key)) {
            int r18 = castAutoSavedDataToInteger(value);
            return r18;
        }
        return value;
    }

    public Integer castAutoSavedDataToInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }
        else if (value instanceof String) {
            return Integer.valueOf((String) value);
        }
        else {
            throw new IllegalArgumentException("错误参数格式");
        }

    }

    /**
     * 根据参数用:分割来指定不同图库使用不同参数
     */
    public void putToMapForWithPrefix(Map<String, Object> params, Map<String, Object> additionParam, String type) {
        if (!additionParam.isEmpty()) {
            for (String paramKey : additionParam.keySet()) {
                if (paramKey == null) {
                    continue;
                }
                
                if (paramKey.contains(":")) {
                    String matchType = paramKey.split(":")[0];
                    String newKey = paramKey.split(":")[1];
                    if (matchType.equalsIgnoreCase(type)) {
                        params.put(newKey, additionParam.get(paramKey));
                    }
                }
                else {
                    params.put(paramKey, additionParam.get(paramKey));
                }
            }
        }
    }


}
