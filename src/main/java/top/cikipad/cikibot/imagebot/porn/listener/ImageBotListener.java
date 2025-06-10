package top.cikipad.cikibot.imagebot.porn.listener;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.cikipad.cikibot.common.auth.AuthService;
import top.cikipad.cikibot.imagebot.porn.ImageSourceManager;
import top.cikipad.cikibot.imagebot.porn.LoliHttpClient;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;
import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;
import top.cikipad.cikibot.util.ConfigManager;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Shiro
@Slf4j
public class ImageBotListener {

    private static final String SET_POOL_TYPE_PREFIX = "设置图库 ";
    private static final String SET_POOL_PARAM_PREFIX = "设置图库参数 ";
    private static final String GET_IMAGE_PREFIX = "涩图 ";
    private static final String RANK_PREFIX = "排行 ";
    private static final String RANK2_PREFIX = "排行2 ";

    @Autowired
    private ConfigManager config;

    @Autowired
    private AuthService authService;

    @Autowired
    private ImageSourceManager imageSourceManager;

    @Value("${img.checkUrlValid:false}")
    private boolean checkUrlValid;

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {SET_POOL_TYPE_PREFIX})
    public void setImagePoolType(Bot bot, AnyMessageEvent event) {
        if (!checkAuthAndRespond(bot, event, true)) {
            return;
        }

        String type = getRealParam(SET_POOL_TYPE_PREFIX, event.getMessage()).toLowerCase();
        boolean isGroup = event.getGroupId() != null;
        boolean success = isGroup ? 
            imageSourceManager.setCurrentTypeNormal(type) : 
            imageSourceManager.setCurrentTypeSp(type);

        String message = success ? 
            String.format("成功设置%s图库为: %s", isGroup ? "群" : "私密", type) :
            String.format("设置%s图库失败,支持图库类型: %s", isGroup ? "群" : "私密", imageSourceManager.getAllType());

        bot.sendMsg(event, message, false);
        log.info(message);
    }

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {SET_POOL_PARAM_PREFIX})
    public void setImagePoolAdditionParam(Bot bot, AnyMessageEvent event) {
        if (!checkAuthAndRespond(bot, event, true)) {
            return;
        }

        String[] params = getRealParam(SET_POOL_PARAM_PREFIX, event.getMessage()).split(" ");
        if (params.length != 2) {
            bot.sendMsg(event, "参数异常!请输入 设置图库参数 参数key 参数param", false);
            return;
        }

        boolean isGroup = event.getGroupId() != null;
        if (isGroup) {
            imageSourceManager.putAdditionParamNormal(params[0], params[1]);
        } else {
            imageSourceManager.putAdditionParamSp(params[0], params[1]);
        }

        String message = String.format("成功设置%s图库参数 %s=%s", 
            isGroup ? "群组" : "私密", params[0], params[1]);
        bot.sendMsg(event, message, false);
        log.info(message);
    }

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {GET_IMAGE_PREFIX})
    public void getImage(Bot bot, AnyMessageEvent event) {
        if (!checkAuthAndRespond(bot, event, false)) {
            return;
        }

        String realParam = getRealParam(GET_IMAGE_PREFIX, event.getMessage());
        Map<String, Object> paramsMap = new HashMap<>();
        boolean gkd = "gkd".equalsIgnoreCase(realParam);
        int isRank = 0;

        if (gkd) {
            // Handle gkd case
        } else if (realParam.startsWith(RANK_PREFIX)) {
            paramsMap.put(ParamsConstant.TAG, getRealParam(RANK_PREFIX, realParam));
            isRank = 1;
        } else if (realParam.startsWith(RANK2_PREFIX)) {
            paramsMap.put(ParamsConstant.TAG, getRealParam(RANK_PREFIX, realParam));
            isRank = 2;
        } else {
            paramsMap.put(ParamsConstant.TAG, realParam);
            paramsMap.put(ParamsConstant.R18, 0);
            paramsMap.put(ParamsConstant.NUM, 2);
        }

        bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 获取中...").build(), false);
        List<ImageUrlEntity> imageUrlsEntity = getImageUrls(event, paramsMap, isRank, gkd);
        
        if (imageUrlsEntity == null || imageUrlsEntity.isEmpty()) {
            bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 无法获取图片").build(), false);
            return;
        }

        List<String> msgList = buildMessageList(imageUrlsEntity);
        List<Map<String, Object>> forwardMsg = ShiroUtils.generateForwardMsg(bot, msgList);
        bot.sendForwardMsg(event, forwardMsg);
        
        log.info("发送图片至{},内容:{}", 
            event.getGroupId() == null ? event.getUserId() : event.getGroupId(), 
            forwardMsg);
    }

    private boolean checkAuthAndRespond(Bot bot, AnyMessageEvent event, boolean requireAdmin) {
        if (!authService.checkAuth(event, requireAdmin)) {
            bot.sendMsg(event, "未授权访问", false);
            return false;
        }
        return true;
    }

    private List<ImageUrlEntity> getImageUrls(AnyMessageEvent event, Map<String, Object> paramsMap, int isRank, boolean gkd) {
        boolean isGroup = event.getGroupId() != null;
        
        if (isRank == 1) {
            return imageSourceManager.getImageUrlsEntity(SourceTypeConstant.ACGMX, paramsMap);
        } else if (isRank == 2) {
            return imageSourceManager.getImageUrlsEntity(SourceTypeConstant.ACGMX_NEW, paramsMap);
        } else {
            return imageSourceManager.getImageUrlsEntity(paramsMap, !isGroup, gkd);
        }
    }

    private List<String> buildMessageList(List<ImageUrlEntity> imageUrlsEntity) {
        List<String> msgList = new ArrayList<>();
        
        for (ImageUrlEntity entity : imageUrlsEntity) {
            msgList.add(MsgUtils.builder().text(entity.getDisplayString()).build());
            if (entity.getUrls() != null) {
                entity.getUrls().stream()
                    .filter(url -> !checkUrlValid || LoliHttpClient.isLinkValid(url))
                    .forEach(url -> msgList.add(MsgUtils.builder().img(url).build()));
            }
        }
        
        return msgList;
    }

    private String getRealParam(String prefix, String input) {
        return input == null ? "" : input.substring(prefix.length());
    }
}
