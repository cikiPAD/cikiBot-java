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
import com.unfbx.chatgpt.entity.chat.ChatChoice;
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

    @Autowired
    private ConfigManager config;

    @Autowired
    private ContextManager contextManager;

    @Autowired
    private AuthService authService;

    @Value("${chatbot.apiKey}")
    private String apiKey;

    @Value("${chatbot.apiHost}")
    private String apiHost;

    @Value("${img.checkUrlValid:false}")
    private boolean checkUrlValid;

    @Value("${chatbot.intention.enable:false}")
    private boolean intentionEnable;

    public static final String PRIVATE_PREFIX = "对话 ";

    public static final String SET_MODEL_PREFIX = "设置模型 ";

    public static final String SET_PROMPT_PREFIX = "预设 ";

    public static final String ADD_WHITELIST_PREFIX = "添加白名单 ";

    public static final String REMOVE_WHITELIST_PREFIX = "移除白名单 ";


    OpenAiClient openAiClient = null;


    @Autowired
    private ImageSourceManager imageSourceManager;


    @PostConstruct
    private void init() {
        openAiClient = OpenAiClient.builder()
                .apiKey(Arrays.asList(apiKey))
                .apiHost(apiHost)
                .build();
    }

    @PrivateMessageHandler
    @MessageHandlerFilter(startWith = {PRIVATE_PREFIX})
    public void chatFromPrivate(Bot bot, PrivateMessageEvent event, Matcher matcher) {
//        String sendMsg = MsgUtils.builder().face(66).text("Hello, this is shiro demo.").build();
//        bot.sendPrivateMsg(event.getUserId(), sendMsg, false);

        if (!authService.checkAuth(event, false)) {
            bot.sendPrivateMsg(event.getUserId(), "未授权访问", false);
            return;
        }


        String text = event.getMessage();

        String param = getRealParam(PRIVATE_PREFIX, text);


        Message message = Message.builder().role(Message.Role.USER).content(param).build();
        ContextEntity contextEntity = contextManager.getContextEntityPrivate(event.getUserId() + "");
        List<Message> requestList = new ArrayList<>();
        requestList.addAll(contextEntity.getMsgList());
        requestList.add(message);


        ChatCompletion chatCompletion = ChatCompletion.builder().model(getCurrentModel()).messages(requestList).build();
        ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
        List<ChatChoice> choices = chatCompletionResponse.getChoices();
        if (choices != null && !choices.isEmpty()) {

            //存储上下文
            ContextEntity newEntity = new ContextEntity();
            newEntity.setLockedNext(contextEntity.getLockedNext());
            List<Message> newEntityMsgList = new ArrayList<>();
            newEntityMsgList.addAll(contextEntity.getMsgList());
            newEntityMsgList.add(message);
            newEntityMsgList.add(choices.get(0).getMessage());
            newEntity.setMsgList(newEntityMsgList);
            contextManager.setContextEntityPrivate(event.getUserId() + "",newEntity);

            bot.sendPrivateMsg(event.getUserId(), choices.get(0).getMessage().getContent(), false);
        }
        else {
            bot.sendPrivateMsg(event.getUserId(), "调用模型失败，未能够收到返回!", false);
        }
    }



    @GroupMessageHandler
    @MessageHandlerFilter(at = AtEnum.NEED)
    public void chatFromGroup(Bot bot, GroupMessageEvent event) {
        // 以注解方式调用可以根据自己的需要来为方法设定参数
        // 例如群组消息可以传递 GroupMessageEvent, Bot, Matcher 多余的参数会被设定为 null
        if (!authService.checkAuth(event, false)) {
            bot.sendGroupMsg(event.getGroupId(), "未授权访问", false);
            return;
        }


        String text = event.getMessage();

        String param = text.replaceAll("\\[CQ:at,qq=[^,\\[\\]:]{0,25}\\]","");


        IntentionEntity intention = null;
        if (intentionEnable) {
            intention = getIntention(param);
        }
        else {
            intention = new IntentionEntity();
            intention.setFlag(false);
        }
        //意图感知
        if (intention.isFlag()) {

            bot.sendGroupMsg(event.getGroupId(), String.format("猫娘正在为您寻找涩图..."), false);

            if (intention.getNum()>5 || intention.getNum()<1) {
                intention.setNum(2);
            }

            Map<String, Object> paramsMap = new HashMap<>();
            boolean gkd = StringUtils.hasLength(intention.getKeyword()) ? false : true;


            if (!gkd) {
                paramsMap.put(ParamsConstant.TAG, intention.getKeyword());
                paramsMap.put(ParamsConstant.R18, 0);
                paramsMap.put(ParamsConstant.NUM, 2);
            }


            List<ImageUrlEntity> imageUrlsEntity = new ArrayList<>();

            imageUrlsEntity = imageSourceManager.getImageUrlsEntity(paramsMap, false, gkd);


            List<String> msgList = new ArrayList<>();

            if (imageUrlsEntity == null || imageUrlsEntity.isEmpty()) {
                bot.sendGroupMsg(event.getGroupId(), MsgUtils.builder().at(event.getUserId()).text(" 无法获取图片").build(), false);
            }
            else {

                for (ImageUrlEntity entity:imageUrlsEntity) {
                    msgList.add(MsgUtils.builder().text(entity.getDisplayString()).build());
                    if (entity.getUrls() != null) {
                        for (String url:entity.getUrls()) {
                            if (checkUrlValid && !LoliHttpClient.isLinkValid(url)) {
                                log.info("{},链接无效,跳过",url);
                                continue;
                            }
                            msgList.add(MsgUtils.builder().img(url).build());
                        }
                    }
                }

                List<Map<String, Object>> forwardMsg = ShiroUtils.generateForwardMsg(bot, msgList);

                bot.sendGroupForwardMsg(event.getGroupId(), forwardMsg);

                log.info("发送图片至{},内容:{}",event.getGroupId()==null?event.getUserId():event.getGroupId(),forwardMsg);

            }
        }
        else{
            Message message = Message.builder().role(Message.Role.USER).content(param).build();
            ContextEntity contextEntity = contextManager.getContextEntityGroup(event.getGroupId() + "", event.getUserId() + "");
            List<Message> requestList = new ArrayList<>();
            requestList.addAll(contextEntity.getMsgList());
            requestList.add(message);


            ChatCompletion chatCompletion = ChatCompletion.builder().model(getCurrentModel()).messages(requestList).build();
            ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
            List<ChatChoice> choices = chatCompletionResponse.getChoices();
            if (choices != null && !choices.isEmpty()) {

                //存储上下文
                ContextEntity newEntity = new ContextEntity();
                newEntity.setLockedNext(contextEntity.getLockedNext());
                List<Message> newEntityMsgList = new ArrayList<>();
                newEntityMsgList.addAll(contextEntity.getMsgList());
                newEntityMsgList.add(message);
                newEntityMsgList.add(choices.get(0).getMessage());
                newEntity.setMsgList(newEntityMsgList);
                contextManager.setContextEntityGroup(event.getGroupId() + "", event.getUserId() + "", newEntity);

                bot.sendGroupMsg(event.getGroupId(), choices.get(0).getMessage().getContent(), false);
            } else {
                bot.sendGroupMsg(event.getGroupId(), "调用模型失败，未能够收到返回!", false);
            }
        }
    }

    // 同时监听群组及私聊消息 并根据消息类型（私聊，群聊）回复
//    @AnyMessageHandler
//    @MessageHandlerFilter(cmd = "设置模型 ")
//    public void fun3(Bot bot, AnyMessageEvent event) {
//        String param;
//
//        bot.sendMsg(event, "成功将模型设置为：" + , false);
//    }


    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {SET_MODEL_PREFIX})
    public void setCurrentModel(Bot bot, AnyMessageEvent event) {

        if (!authService.checkAuth(event, true)) {
            bot.sendMsg(event, "未授权访问", false);
            return;
        }
        String text = event.getMessage();
        String model = getRealParam(SET_MODEL_PREFIX, text);
        config.setProperty(CommonConstant.CHATBOT_CURRENT_MODEL, model);

        bot.sendMsg(event, "成功设置模型为 " + model, false);

        log.info("成功设置模型为 " + model);
    }


    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {SET_PROMPT_PREFIX})
    public void setCurrentPrompt(Bot bot, AnyMessageEvent event) {
        if (!authService.checkAuth(event, true)) {
            bot.sendMsg(event, "未授权访问", false);
            return;
        }
        String text = event.getMessage();
        String promptKey = getRealParam(SET_PROMPT_PREFIX, text);
        config.setProperty(CommonConstant.CHATBOT_CURRENT_PROMPT, promptKey);

        bot.sendMsg(event, "成功设置预设为 " + promptKey, false);

        log.info("成功设置预设为 " + promptKey);
    }


    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {ADD_WHITELIST_PREFIX})
    public void addWhileList(Bot bot, AnyMessageEvent event) {
        if (!authService.checkAuth(event, true)) {
            bot.sendMsg(event, "未授权访问", false);
            return;
        }
        String text = event.getMessage();
        String auth = getRealParam(ADD_WHITELIST_PREFIX, text).toLowerCase();

        if (!auth.startsWith(CommonConstant.GROUP_PREFIX) && !auth.startsWith(CommonConstant.PRIVATE_PREFIX)) {
            bot.sendMsg(event, "非法参数,请以-g/-p设置权限实体", false);
            return;
        }

        config.addWhiteList(auth);

        bot.sendMsg(event, "成功添加权限", false);

        log.info("成功添加权限" + auth);
    }


    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {REMOVE_WHITELIST_PREFIX})
    public void removeWhileList(Bot bot, AnyMessageEvent event) {
        if (!authService.checkAuth(event, true)) {
            bot.sendMsg(event, "未授权访问", false);
            return;
        }
        String text = event.getMessage();
        String auth = getRealParam(REMOVE_WHITELIST_PREFIX, text).toLowerCase();

        if (!auth.startsWith(CommonConstant.GROUP_PREFIX) && !auth.startsWith(CommonConstant.PRIVATE_PREFIX)) {
            bot.sendMsg(event, "非法参数,请以-g/-p设置权限实体", false);
            return;
        }

        config.removeWhiteList(auth);

        bot.sendMsg(event, "成功移除权限", false);

        log.info("成功移除权限" + auth);
    }

    private String getRealParam(String prefix,String input) {
        if (input == null) {
            return "";
        }
        return input.substring(prefix.length());
    }


    public String getCurrentModel() {
        String property = config.getProperty(CommonConstant.CHATBOT_CURRENT_MODEL);
        if (StringUtils.hasLength(property)) {
            return property;
        }
        else {
            return CommonConstant.DEFAULT_MODEL;
        }
    }



    private IntentionEntity getIntention(String msg) {
        try {
            String input = String.format(StaticPrompt.INTENTION_PROMPT, msg);
            Message message = Message.builder().role(Message.Role.USER).content(input).build();
            List<Message> msgs = new ArrayList<>();
            msgs.add(message);
            ChatCompletion chatCompletion = ChatCompletion.builder().model("gpt-3.5-turbo").messages(msgs).build();
            ChatCompletionResponse chatCompletionResponse = openAiClient.chatCompletion(chatCompletion);
            List<ChatChoice> choices = chatCompletionResponse.getChoices();
            if (choices != null && !choices.isEmpty()) {
                String jsonStr = choices.get(0).getMessage().getContent();
                IntentionEntity intentionEntity = JSONObject.parseObject(jsonStr, IntentionEntity.class);
                log.info("成功感知意图{}",intentionEntity);
                return intentionEntity;
            }
        }
        catch (Exception e) {
            log.error("", e);
            log.error("感知用户意图失败!输入为:{}", msg);
        }
        IntentionEntity ret =  new IntentionEntity();
        ret.setFlag(false);
        log.info("感知意图失败");
        return ret;
    }


}
