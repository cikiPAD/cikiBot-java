package top.cikipad.cikibot.er.listener;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.common.utils.OneBotMedia;
import com.mikuac.shiro.common.utils.ShiroUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.cikipad.cikibot.common.auth.AuthService;
import top.cikipad.cikibot.imagebot.porn.LoliHttpClient;
import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;
import top.cikipad.cikibot.util.ImagePatchNotesUtils;
import top.cikipad.cikibot.util.PatchLocalEntity;
import top.cikipad.cikibot.util.PatchNote;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Shiro
@Slf4j
public class ERImagePatchNotesListener {

    @Value("${screeshot.url:http://localhost:14140}")
    private String screenShotUrl;

    private static final String GET_PATCH_NOTES_PREFIX = "获取更新";
    private static final String ERROR_UNAUTHORIZED = "未授权访问";

    @Autowired
    private AuthService authService;

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {GET_PATCH_NOTES_PREFIX})
    public void getPatchNotes(Bot bot, AnyMessageEvent event) {
        if (!checkAuthAndRespond(bot, event, false)) {
            return;
        }

        bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 获取更新日志中...").build(), false);

        List<PatchLocalEntity> entities = null;
        try {
            entities = ImagePatchNotesUtils.captureScreenshots(ImagePatchNotesUtils.getLatestPatchNotesUrls(), screenShotUrl);

        }
        catch (Exception e) {
            log.error("获取截图失败");
            log.error("", e);
        }

        if (entities == null) {
            bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 截图失败...").build(), false);
        }

        try {

            List<String> msg = buildMessageList(entities);

            List<Map<String, Object>> forwardMsg = ShiroUtils.generateForwardMsg(bot, msg);
            bot.sendForwardMsg(event, forwardMsg);

            log.info("发送ER更新日志至{}",
                event.getGroupId() == null ? event.getUserId() : event.getGroupId());
        } catch (Exception e) {
            log.error("Failed to get ER patch notes", e);
            bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 获取更新日志失败").build(), false);
        }


    }

    private boolean checkAuthAndRespond(Bot bot, AnyMessageEvent event, boolean requireAdmin) {
        if (!authService.checkAuth(event, requireAdmin)) {
            bot.sendMsg(event, ERROR_UNAUTHORIZED, false);
            return false;
        }
        return true;
    }


    private List<String> buildMessageList(List<PatchLocalEntity> entities) {
        List<String> msgList = new ArrayList<>();

        for (PatchLocalEntity entity : entities) {
            msgList.add(MsgUtils.builder().text(entity.getTitle()).build());
            if (entity.getPath() != null) {
                msgList.add(MsgUtils.builder().img("file://" + entity.getPath()).build());
            }
        }

        return msgList;
    }
}