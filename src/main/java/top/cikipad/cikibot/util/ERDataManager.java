package top.cikipad.cikibot.util;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import top.cikipad.cikibot.constant.ERDataConstant;

import java.io.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ERDataManager {
    private static final ERDataManager instance = new ERDataManager();
    private final Map<String, List<Map<String, Object>>> dataCache = new ConcurrentHashMap<>();
    private static final String CACHE_FILE = "er_data_cache.json";
    private static final long CACHE_EXPIRY_DAYS = 7;
    private long lastUpdateTimestamp;

    @Data
    private static class CacheData {
        private long timestamp;
        private Map<String, List<Map<String, Object>>> data;
    }

    public ERDataManager() {
        loadAllData();
    }
    
    public static ERDataManager getInstance() {
        return instance;
    }

    private void loadAllData() {
        log.info("开始加载ER游戏数据...");
        
        if (loadFromCache()) {
            log.info("从缓存文件加载数据成功");
            return;
        }
        
        loadData("seasons", ERDataConstant.SEASONS_URL);
        loadData("masteries", ERDataConstant.MASTERIES_URL);
        loadData("monsters", ERDataConstant.MONSTERS_URL);
        loadData("characters", ERDataConstant.CHARACTERS_URL);
        loadData("items", ERDataConstant.ITEMS_URL);
        loadData("skills", ERDataConstant.SKILLS_URL);
        loadData("tacticalSkills", ERDataConstant.TACTICAL_SKILLS_URL);
        loadData("traitSkills", ERDataConstant.TRAIT_SKILLS_URL);
        loadData("infusions", ERDataConstant.INFUSIONS_URL);
        loadData("tiers", ERDataConstant.TIERS_URL);
        loadData("areas", ERDataConstant.AREAS_URL);
        loadData("weathers", ERDataConstant.WEATHERS_URL);
        
        lastUpdateTimestamp = Instant.now().getEpochSecond();
        saveToCache();
        
        log.info("ER游戏数据加载完成");
    }

    private boolean loadFromCache() {
        File cacheFile = new File(CACHE_FILE);
        if (!cacheFile.exists()) {
            return false;
        }

        try (FileReader reader = new FileReader(cacheFile)) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
            CacheData cacheData = JSONObject.parseObject(content.toString(), CacheData.class);
            
            // Check if cache is expired
            long currentTime = Instant.now().getEpochSecond();
            if (currentTime - cacheData.getTimestamp() > CACHE_EXPIRY_DAYS * 24 * 60 * 60) {
                log.info("缓存数据已过期，将重新加载");
                return false;
            }

            dataCache.putAll(cacheData.getData());
            lastUpdateTimestamp = cacheData.getTimestamp();
            return true;
        } catch (Exception e) {
            log.error("从缓存文件加载数据失败: {}", e.getMessage());
            return false;
        }
    }

    private void saveToCache() {
        CacheData cacheData = new CacheData();
        cacheData.setTimestamp(lastUpdateTimestamp);
        cacheData.setData(dataCache);

        try (FileWriter writer = new FileWriter(CACHE_FILE)) {
            writer.write(JSONObject.toJSONString(cacheData));
            log.info("数据已保存到缓存文件");
        } catch (IOException e) {
            log.error("保存数据到缓存文件失败: {}", e.getMessage());
        }
    }
    
    private void loadData(String key, String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONObject json = JSONObject.parseObject(jsonResponse);
                
                List<Map<String, Object>> data = JSONObject.parseObject(
                    json.getJSONArray(key).toJSONString(),
                    new TypeReference<List<Map<String, Object>>>() {}
                );

                
                dataCache.put(key, data);
                log.info("成功加载{}数据，共{}条", key, data.size());
            }
        } catch (IOException e) {
            log.error("加载{}数据失败: {}", key, e.getMessage());
        }
    }
    
    public List<Map<String, Object>> getData(String key) {
        return dataCache.get(key);
    }
    
    public Map<String, Object> findById(String key, Object id) {
        List<Map<String, Object>> data = dataCache.get(key);
        if (data == null) {
            return null;
        }
        
        return data.stream()
            .filter(item -> id.equals(item.get("id")))
            .findFirst()
            .orElse(null);
    }
    
    public Map<String, Object> findByName(String key, String name) {
        List<Map<String, Object>> data = dataCache.get(key);
        if (data == null) {
            return null;
        }
        
        return data.stream()
            .filter(item -> name.equals(item.get("name")))
            .findFirst()
            .orElse(null);
    }
    
    public void reloadData() {
        dataCache.clear();
        loadAllData();
    }
} 