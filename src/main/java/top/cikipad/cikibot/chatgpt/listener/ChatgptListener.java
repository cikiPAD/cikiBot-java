package top.cikipad.cikibot.chatgpt.listener;

import com.alibaba.fastjson.JSONObject;
import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import com.mikuac.shiro.enums.AtEnum;
import com.unfbx.chatgpt.OpenAiClient;
import com.unfbx.chatgpt.entity.chat.ChatCompletion;
import com.unfbx.chatgpt.entity.chat.ChatCompletionResponse;
import com.unfbx.chatgpt.entity.chat.Message;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.cikipad.cikibot.chatgpt.context.ContextManager;
import top.cikipad.cikibot.chatgpt.entity.ContextEntity;
import top.cikipad.cikibot.chatgpt.entity.IntentionEntity;
import top.cikipad.cikibot.common.auth.AuthService;
import top.cikipad.cikibot.constant.CommonConstant;
import top.cikipad.cikibot.constant.StaticPrompt;
import top.cikipad.cikibot.imagebot.porn.ImageSourceManager;
import top.cikipad.cikibot.imagebot.porn.LoliHttpClient;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;
import top.cikipad.cikibot.util.ConfigManager;

import java.util.*;
import java.util.regex.Matcher;

@Component
@Shiro
@Slf4j
public class ChatgptListener {

    // Constants
    private static final String PRIVATE_PREFIX = "对话 ";
    private static final String SET_MODEL_PREFIX = "设置模型 ";
    private static final String SET_PROMPT_PREFIX = "预设 ";
    private static final String ADD_WHITELIST_PREFIX = "添加白名单 ";
    private static final String REMOVE_WHITELIST_PREFIX = "移除白名单 ";
    private static final int MAX_IMAGE_COUNT = 5;
    private static final int DEFAULT_IMAGE_COUNT = 2;
    private static final String ERROR_UNAUTHORIZED = "未授权访问";
    private static final String ERROR_INVALID_PARAM = "非法参数,请以-g/-p设置权限实体";
    private static final String ERROR_MODEL_CALL = "调用模型失败，未能够收到返回!";
    private static final String ERROR_NO_IMAGES = " 无法获取图片";

    // Dependencies
    @Autowired
    private ConfigManager config;
    @Autowired
    private ContextManager contextManager;
    @Autowired
    private AuthService authService;
    @Autowired
    private ImageSourceManager imageSourceManager;

    // Configuration
    @Value("${chatbot.apiKey}")
    private String apiKey;
    @Value("${chatbot.apiHost}")
    private String apiHost;
    @Value("${img.checkUrlValid:false}")
    private boolean checkUrlValid;
    @Value("${chatbot.intention.enable:false}")
    private boolean intentionEnable;

    private OpenAiClient openAiClient;

    @PostConstruct
    private void init() {
        openAiClient = OpenAiClient.builder()
                .apiKey(Arrays.asList(apiKey))
                .apiHost(apiHost)
                .build();
        log.info("ChatGPT client initialized successfully");
    }

    // Message Handlers
    @PrivateMessageHandler
    @MessageHandlerFilter(startWith = {PRIVATE_PREFIX})
    public void chatFromPrivate(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        if (!authService.checkAuth(event, false)) {
            sendPrivateMessage(bot, event.getUserId(), ERROR_UNAUTHORIZED);
            return;
        }

        String param = getRealParam(PRIVATE_PREFIX, event.getMessage());
        handleChatResponse(bot, event.getUserId(), param, true);
    }

    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NEED)
    public void chatFromGroup(Bot bot, GroupMessageEvent event) {
        if (!authService.checkAuth(event, false)) {
            sendGroupMessage(bot, event.getGroupId(), ERROR_UNAUTHORIZED);
            return;
        }

        String param = event.getMessage().replaceAll("\\[CQ:at,qq=[^,\\[\\]:]{0,25}\\]", "");

        if (intentionEnable) {
            IntentionEntity intention = getIntention(param);
            if (intention.isFlag()) {
                handleImageRequest(bot, event, intention);
                return;
            }
        }

        handleChatResponse(bot, event.getGroupId(), param, false);
    }

    // Command Handlers
    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {SET_MODEL_PREFIX})
    public void setCurrentModel(Bot bot, AnyMessageEvent event) {
        if (!authService.checkAuth(event, true)) {
            sendMessage(bot, event, ERROR_UNAUTHORIZED);
            return;
        }

        String model = getRealParam(SET_MODEL_PREFIX, event.getMessage());
        config.setProperty(CommonConstant.CHATBOT_CURRENT_MODEL, model);
        sendMessage(bot, event, "成功设置模型为 " + model);
        log.info("Model set to: {}", model);
    }

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {SET_PROMPT_PREFIX})
    public void setCurrentPrompt(Bot bot, AnyMessageEvent event) {
        if (!authService.checkAuth(event, true)) {
            sendMessage(bot, event, ERROR_UNAUTHORIZED);
            return;
        }

        String promptKey = getRealParam(SET_PROMPT_PREFIX, event.getMessage());
        config.setProperty(CommonConstant.CHATBOT_CURRENT_PROMPT, promptKey);
        sendMessage(bot, event, "成功设置预设为 " + promptKey);
        log.info("Prompt set to: {}", promptKey);
    }

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {ADD_WHITELIST_PREFIX})
    public void addWhileList(Bot bot, AnyMessageEvent event) {
        if (!authService.checkAuth(event, true)) {
            sendMessage(bot, event, ERROR_UNAUTHORIZED);
            return;
        }

        String auth = getRealParam(ADD_WHITELIST_PREFIX, event.getMessage()).toLowerCase();
        if (!isValidAuthFormat(auth)) {
            sendMessage(bot, event, ERROR_INVALID_PARAM);
            return;
        }

        config.addWhiteList(auth);
        sendMessage(bot, event, "成功添加权限");
        log.info("Added to whitelist: {}", auth);
    }

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {REMOVE_WHITELIST_PREFIX})
    public void removeWhileList(Bot bot, AnyMessageEvent event) {
        if (!authService.checkAuth(event, true)) {
            sendMessage(bot, event, ERROR_UNAUTHORIZED);
            return;
        }

        String auth = getRealParam(REMOVE_WHITELIST_PREFIX, event.getMessage()).toLowerCase();
        if (!isValidAuthFormat(auth)) {
            sendMessage(bot, event, ERROR_INVALID_PARAM);
            return;
        }

        config.removeWhiteList(auth);
        sendMessage(bot, event, "成功移除权限");
        log.info("Removed from whitelist: {}", auth);
    }

    // Helper Methods
    private void handleChatResponse(Bot bot, long targetId, String param, boolean isPrivate) {
        try {
            Message message = Message.builder().role(Message.Role.USER).content(param).build();
            ContextEntity contextEntity = isPrivate ? 
                contextManager.getContextEntityPrivate(targetId + "") :
                contextManager.getContextEntityGroup(targetId + "", targetId + "");

            List<Message> requestList = new ArrayList<>(contextEntity.getMsgList());
            requestList.add(message);

            ChatCompletion chatCompletion = ChatCompletion.builder()
                .model(getCurrentModel())
                .messages(requestList)
                .build();
            
            ChatCompletionResponse response = openAiClient.chatCompletion(chatCompletion);
            
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                updateContextAndSendResponse(bot, targetId, contextEntity, message, 
                    response.getChoices().get(0).getMessage(), isPrivate);
            } else {
                sendErrorMessage(bot, targetId, isPrivate);
            }
        } catch (Exception e) {
            log.error("Chat completion failed for targetId: {}, isPrivate: {}", targetId, isPrivate, e);
            sendErrorMessage(bot, targetId, isPrivate);
        }
    }

    private void handleImageRequest(Bot bot, GroupMessageEvent event, IntentionEntity intention) {
        sendGroupMessage(bot, event.getGroupId(), "猫娘正在为您寻找涩图...");

        int imageCount = validateImageCount(intention.getNum());
        Map<String, Object> paramsMap = new HashMap<>();
        boolean gkd = !StringUtils.hasLength(intention.getKeyword());

        if (!gkd) {
            paramsMap.put(ParamsConstant.TAG, intention.getKeyword());
            paramsMap.put(ParamsConstant.R18, 0);
            paramsMap.put(ParamsConstant.NUM, imageCount);
        }

        List<ImageUrlEntity> imageUrlsEntity = imageSourceManager.getImageUrlsEntity(paramsMap, false, gkd);
        if (imageUrlsEntity == null || imageUrlsEntity.isEmpty()) {
            sendGroupMessage(bot, event.getGroupId(), 
                MsgUtils.builder().at(event.getUserId()).text(ERROR_NO_IMAGES).build());
            return;
        }

        List<String> msgList = buildImageMessageList(imageUrlsEntity);
        List<Map<String, Object>> forwardMsg = ShiroUtils.generateForwardMsg(bot, msgList);
        bot.sendGroupForwardMsg(event.getGroupId(), forwardMsg);
        log.info("Sent images to group: {}, content: {}", event.getGroupId(), forwardMsg);
    }

    private List<String> buildImageMessageList(List<ImageUrlEntity> imageUrlsEntity) {
        List<String> msgList = new ArrayList<>();
        for (ImageUrlEntity entity : imageUrlsEntity) {
            msgList.add(MsgUtils.builder().text(entity.getDisplayString()).build());
            if (entity.getUrls() != null) {
                for (String url : entity.getUrls()) {
                    if (checkUrlValid && !LoliHttpClient.isLinkValid(url)) {
                        log.info("Invalid link skipped: {}", url);
                        continue;
                    }
                    msgList.add(MsgUtils.builder().img(url).build());
                }
            }
        }
        return msgList;
    }

    private void updateContextAndSendResponse(Bot bot, long targetId, ContextEntity contextEntity, 
            Message userMessage, Message responseMessage, boolean isPrivate) {
        ContextEntity newEntity = new ContextEntity();
        newEntity.setLockedNext(contextEntity.getLockedNext());
        List<Message> newEntityMsgList = new ArrayList<>(contextEntity.getMsgList());
        newEntityMsgList.add(userMessage);
        newEntityMsgList.add(responseMessage);
        newEntity.setMsgList(newEntityMsgList);

        if (isPrivate) {
            contextManager.setContextEntityPrivate(targetId + "", newEntity);
            sendPrivateMessage(bot, targetId, responseMessage.getContent());
        } else {
            contextManager.setContextEntityGroup(targetId + "", targetId + "", newEntity);
            sendGroupMessage(bot, targetId, responseMessage.getContent().trim());
        }
    }

    private IntentionEntity getIntention(String msg) {
        try {
            String input = String.format(StaticPrompt.INTENTION_PROMPT, msg);
            Message message = Message.builder().role(Message.Role.USER).content(input).build();
            List<Message> msgs = Collections.singletonList(message);
            
            ChatCompletion chatCompletion = ChatCompletion.builder()
                .model("gpt-3.5-turbo")
                .messages(msgs)
                .build();
            
            ChatCompletionResponse response = openAiClient.chatCompletion(chatCompletion);
            
            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                String jsonStr = response.getChoices().get(0).getMessage().getContent();
                IntentionEntity intentionEntity = JSONObject.parseObject(jsonStr, IntentionEntity.class);
                log.info("Successfully detected intention: {}", intentionEntity);
                return intentionEntity;
            }
        } catch (Exception e) {
            log.error("Failed to detect intention for input: {}", msg, e);
        }
        
        IntentionEntity ret = new IntentionEntity();
        ret.setFlag(false);
        log.info("Intention detection failed");
        return ret;
    }

    // Utility Methods
    private String getRealParam(String prefix, String input) {
        return input == null ? "" : input.substring(prefix.length());
    }

    private String getCurrentModel() {
        String property = config.getProperty(CommonConstant.CHATBOT_CURRENT_MODEL);
        return StringUtils.hasLength(property) ? property : CommonConstant.DEFAULT_MODEL;
    }

    private boolean isValidAuthFormat(String auth) {
        return auth.startsWith(CommonConstant.GROUP_PREFIX) || 
               auth.startsWith(CommonConstant.PRIVATE_PREFIX);
    }

    private int validateImageCount(Integer num) {
        if (num == null || num > MAX_IMAGE_COUNT || num < 1) {
            return DEFAULT_IMAGE_COUNT;
        }
        return num;
    }

    // Message Sending Methods
    private void sendPrivateMessage(Bot bot, long userId, String message) {
        bot.sendPrivateMsg(userId, message, false);
    }

    private void sendGroupMessage(Bot bot, long groupId, String message) {
        bot.sendGroupMsg(groupId, message, false);
    }

    private void sendMessage(Bot bot, AnyMessageEvent event, String message) {
        bot.sendMsg(event, message, false);
    }

    private void sendErrorMessage(Bot bot, long targetId, boolean isPrivate) {
        if (isPrivate) {
            sendPrivateMessage(bot, targetId, ERROR_MODEL_CALL);
        } else {
            sendGroupMessage(bot, targetId, ERROR_MODEL_CALL);
        }
    }
}
