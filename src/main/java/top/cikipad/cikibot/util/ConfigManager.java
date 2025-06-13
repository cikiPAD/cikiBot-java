package top.cikipad.cikibot.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.cikipad.cikibot.constant.CommonConstant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@Slf4j
@Component
public class ConfigManager {

    private static final String CONFIG_FILE = "persis-config.properties";

    private static final Set<String> admins = new HashSet<>();

    @Value("${chatbot.admin.qqnum}")
    private String adminQQnum;

    private static final Set<String> whiteList = new HashSet<>();

    private Properties properties;

    @PostConstruct
    private void init() {
        String[] arr = adminQQnum.split(",");
        for (String one:arr) {
            admins.add(one);
        }

        String whiteListStr = properties.getProperty(CommonConstant.WHITE_LIST);
        if (whiteListStr != null && whiteListStr.trim().length() != 0) {
            String[] whhileLists = whiteListStr.split(",");
            for (String one:whhileLists) {
                whiteList.add(one);
            }
        }

    }

    public ConfigManager() {
        properties = new Properties();
        try {
            File file = new File(CONFIG_FILE);
            if (!file.exists()) {
                file.createNewFile();
            }
            properties.load(new FileInputStream(CONFIG_FILE));
        } catch (IOException e) {
            log.error("初始化配置失败！", e);
        }
    }


    public boolean isAdmin(String qq) {
        if (admins.contains(qq)) {
            return true;
        }
        return false;
    }

    public boolean isInWhileList(String id) {
        if (whiteList.contains(id)) {
            return true;
        }
        return false;
    }

    public void addWhiteList(String id) {
        if (whiteList.contains(id)) {
            return;
        }
        else {
            whiteList.add(id);
            setProperty(CommonConstant.WHITE_LIST,Strings.join(whiteList, ','));
        }
    }

    public void removeWhiteList(String id) {
        if (!whiteList.contains(id)) {
            return;
        }
        else {
            whiteList.remove(id);
            setProperty(CommonConstant.WHITE_LIST,Strings.join(whiteList, ','));
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        try {
            properties.store(new FileOutputStream(CONFIG_FILE), null);
        } catch (IOException e) {
            log.error("设置属性失败！", e);
        }
    }


}
