package top.cikipad.cikibot.imagebot.porn.listener;

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
import top.cikipad.cikibot.common.auth.AuthService;
import top.cikipad.cikibot.constant.CommonConstant;
import top.cikipad.cikibot.imagebot.porn.ImageSourceManager;
import top.cikipad.cikibot.imagebot.porn.LoliHttpClient;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;
import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;
import top.cikipad.cikibot.util.ConfigManager;

import java.util.*;
import java.util.regex.Matcher;


@Component
@Shiro
@Slf4j
public class ImageBotListener {

    @Autowired
    private ConfigManager config;

    @Autowired
    private AuthService authService;

    @Value("${img.checkUrlValid:false}")
    private boolean checkUrlValid;

    public static final String SET_POOL_TYPE_PREFIX = "设置图库 ";

    public static final String SET_POOL_PARAM_PREFIX = "设置图库参数 ";

    public static final String GET_IMAGE_PREFIX = "涩图 ";

    @Autowired
    private ImageSourceManager imageSourceManager;




    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {SET_POOL_TYPE_PREFIX})
    public void setImagePoolType(Bot bot, AnyMessageEvent event) {
        if (!authService.checkAuth(event, true)) {
            bot.sendMsg(event, "未授权访问", false);
            return;
        }
        String text = event.getMessage();
        String tpye = getRealParam(SET_POOL_TYPE_PREFIX, text).toLowerCase();


        boolean succ = false;
        if (event.getGroupId() != null) {
            succ = imageSourceManager.setCurrentTypeNormal(tpye);

            if (succ) {
                bot.sendMsg(event, "成功设置群图库为:" + tpye, false);
                log.info("成功设置群图库为:" + tpye);
            }
            else {
                bot.sendMsg(event, "设置群图库失败,支持图库类型: " + imageSourceManager.getAllType(), false);
                log.info("设置群图库失败,支持图库类型: " + imageSourceManager.getAllType());
            }
        }
        else {
            succ = imageSourceManager.setCurrentTypeSp(tpye);

            if (succ) {
                bot.sendMsg(event, "成功设置私密图库为:" + tpye, false);
                log.info("成功设置私密图库为:" + tpye);
            }
            else {
                bot.sendMsg(event, "设置私密图库失败,支持图库类型: " + imageSourceManager.getAllType(), false);
                log.info("设置私密图库失败,支持图库类型: " + imageSourceManager.getAllType());
            }
        }
    }



    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {SET_POOL_PARAM_PREFIX})
    public void setImagePoolAdditionParam(Bot bot, AnyMessageEvent event) {
        if (!authService.checkAuth(event, true)) {
            bot.sendMsg(event, "未授权访问", false);
            return;
        }
        String text = event.getMessage();
        String paramPair = getRealParam(SET_POOL_PARAM_PREFIX, text);

        String[] param = paramPair.split(" ");

        if (param.length != 2) {
            bot.sendMsg(event, "参数异常!请输入 设置图库参数 参数key 参数param", false);
        }

        if (event.getGroupId() != null) {
            imageSourceManager.putAdditionParamNormal(param[0], param[1]);
            bot.sendMsg(event, "成功设置群组图库参数", false);
            log.info("成功设置群组图库参数{}={}",param[0], param[1]);

        }
        else {
            imageSourceManager.putAdditionParamSp(param[0], param[1]);
            bot.sendMsg(event, "成功设置私密图库参数", false);
            log.info("成功设置私密图库参数{}={}",param[0], param[1]);
        }
    }


    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {GET_IMAGE_PREFIX})
    public void getImage(Bot bot, AnyMessageEvent event) {
        if (!authService.checkAuth(event, false)) {
            bot.sendMsg(event, "未授权访问", false);
            return;
        }
        String text = event.getMessage();
        String realParam = getRealParam(GET_IMAGE_PREFIX, text);



        Map<String, Object> paramsMap = new HashMap<>();
        boolean gkd = false;
        int isRank = 0;

        if ("gkd".equalsIgnoreCase(realParam)) {
            gkd = true;
        } else if (realParam.startsWith("排行 ")) {
            String realParam2 = getRealParam("排行 ", realParam);
            paramsMap.put(ParamsConstant.TAG, realParam2);
            isRank = 1;
        }
        else if (realParam.startsWith("排行2 ")) {
            String realParam2 = getRealParam("排行 ", realParam);
            paramsMap.put(ParamsConstant.TAG, realParam2);
            isRank = 2;
        }
        else {
            paramsMap.put(ParamsConstant.TAG, realParam);
            paramsMap.put(ParamsConstant.R18, 0);
            paramsMap.put(ParamsConstant.NUM, 2);
        }

        bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 获取中...").build(), false);

        List<ImageUrlEntity> imageUrlsEntity = new ArrayList<>();

        if (isRank == 1) {
            if (event.getGroupId() != null) {
                imageUrlsEntity = imageSourceManager.getImageUrlsEntity(SourceTypeConstant.ACGMX,paramsMap);
            } else {
                imageUrlsEntity = imageSourceManager.getImageUrlsEntity(SourceTypeConstant.ACGMX,paramsMap);
            }

        }
        else if (isRank == 2 ) {

            if (event.getGroupId() != null) {
                imageUrlsEntity = imageSourceManager.getImageUrlsEntity(SourceTypeConstant.ACGMX_NEW,paramsMap);
            } else {
                imageUrlsEntity = imageSourceManager.getImageUrlsEntity(SourceTypeConstant.ACGMX_NEW,paramsMap);
            }
        }
        else {
            if (event.getGroupId() != null) {
                imageUrlsEntity = imageSourceManager.getImageUrlsEntity(paramsMap, false, gkd);
            } else {
                imageUrlsEntity = imageSourceManager.getImageUrlsEntity(paramsMap, true, gkd);
            }
        }

        List<String> msgList = new ArrayList<>();

        if (imageUrlsEntity == null || imageUrlsEntity.isEmpty()) {
            bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 无法获取图片").build(), false);
        }
        else {

            for (ImageUrlEntity entity:imageUrlsEntity) {
                msgList.add(MsgUtils.builder().text(entity.getDisplayString()).build());
                if (entity.getUrls() != null) {
                    for (String url:entity.getUrls()) {
                        if (checkUrlValid && !LoliHttpClient.isLinkValid(url)) {
                            log.info("{},链接无效,跳过");
                            continue;
                        }
                        msgList.add(MsgUtils.builder().img(url).build());
                    }
                }
            }

            List<Map<String, Object>> forwardMsg = ShiroUtils.generateForwardMsg(bot, msgList);

            bot.sendForwardMsg(event, forwardMsg);

            log.info("发送图片至{},内容:{}",event.getGroupId()==null?event.getUserId():event.getGroupId(),forwardMsg);

        }


    }




    private String getRealParam(String prefix,String input) {
        if (input == null) {
            return "";
        }
        return input.substring(prefix.length());
    }





}
