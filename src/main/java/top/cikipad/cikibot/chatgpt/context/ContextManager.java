package top.cikipad.cikibot.chatgpt.context;

import com.unfbx.chatgpt.entity.chat.Message;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.cikipad.cikibot.chatgpt.entity.ContextEntity;
import top.cikipad.cikibot.constant.CommonConstant;
import top.cikipad.cikibot.constant.StaticPrompt;
import top.cikipad.cikibot.util.ConfigManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 获取对话上下文
 */
@Component
@Slf4j
public class ContextManager {

    private Long contextTimeoutInternal = 10 * 60 * 1000L;



    private Map<String, ContextEntity> contextEntityMap = new ConcurrentHashMap<>();

    @Value("${contextHoldLength:4}")
    private int contextHoldLength;


    @Autowired
    private ConfigManager config;


    @PostConstruct
    private void init() {
        CheckThread checkThread = new CheckThread();
        checkThread.setName("checkThread");
        checkThread.start();
        log.info("容器检查线程启动");
    }


    public class CheckThread extends Thread {
        @Override
        public void run() {
            while(true) {

                try {
                    Thread.sleep(30000L);
                } catch (InterruptedException e) {
                    log.error("检查线程异常!", e);
                }

                for (Map.Entry<String, ContextEntity> entry:contextEntityMap.entrySet()) {
                    Long lastTime = entry.getValue().getTimeStamp();
                    String key = entry.getKey();
                    //超时移除会话
                    if (System.currentTimeMillis() - lastTime >= contextTimeoutInternal) {
                        contextEntityMap.remove(key);
                        log.info("会话:{},超过规定时间，清除上下文",key);
                    }
                }

            }
        }
    }


    public ContextEntity getContextEntityGroup(String groupId,String userId) {
        String realId = CommonConstant.GROUP_PREFIX + groupId + "_" + userId;

        return doGetContextEntity(realId);
    }

    public void setContextEntityGroup(String groupId,String userId, ContextEntity context) {
        String realId = CommonConstant.GROUP_PREFIX + groupId + "_" + userId;

        doSetContextEntity(realId, context);
    }

    public ContextEntity getContextEntityPrivate(String privateId) {
        String realId = CommonConstant.PRIVATE_PREFIX + privateId;

        return doGetContextEntity(realId);
    }

    public void setContextEntityPrivate(String privateId, ContextEntity context) {
        String realId = CommonConstant.PRIVATE_PREFIX + privateId;

        doSetContextEntity(realId, context);
    }


    public ContextEntity doGetContextEntity(String realId) {
        ContextEntity context = contextEntityMap.get(realId);

        if (context == null) {
            //构建上下文
            String prompt = getPrompt();
            if (prompt == null) {
                List<Message> list = new ArrayList<>();
                ContextEntity newContext = new ContextEntity();
                newContext.setTimeStamp(System.currentTimeMillis());
                newContext.setMsgList(list);
                return newContext;
            }
            else {
                List<Message> list = new ArrayList<>();
                ContextEntity newContext = new ContextEntity();
                newContext.setTimeStamp(System.currentTimeMillis());
                newContext.setMsgList(list);
                Message message = Message.builder().role(Message.Role.USER).content(prompt).build();
                list.add(message);
                newContext.setLockedNext(1);
                return newContext;
            }

        }
        else {
            return context;
        }
    }

    public void doSetContextEntity(String realId, ContextEntity context) {
        //裁剪上下文
        if (context.getMsgList().size() - context.getLockedNext() > contextHoldLength) {
            int count = context.getMsgList().size() - context.getLockedNext() - contextHoldLength;

            for (int i=0;i<count;i++) {
                context.getMsgList().remove(context.getLockedNext());
            }

        }

        context.setTimeStamp(System.currentTimeMillis());

        contextEntityMap.put(realId, context);
    }

    public String getPrompt() {
        String key = getCurrentPromptKey();
        if (key == null) {
            return null;
        }
        return StaticPrompt.staticPromptMap.get(key);
    }


    public String getCurrentPromptKey() {
        String property = config.getProperty(CommonConstant.CHATBOT_CURRENT_PROMPT);
        if (StringUtils.hasLength(property)) {
            return property;
        }
        else {
            return null;
        }
    }




}
