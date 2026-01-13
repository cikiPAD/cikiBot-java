//package top.cikipad.cikibot.er.listener;
//
//import com.mikuac.shiro.annotation.AnyMessageHandler;
//import com.mikuac.shiro.annotation.MessageHandlerFilter;
//import com.mikuac.shiro.annotation.common.Shiro;
//import com.mikuac.shiro.common.utils.MsgUtils;
//import com.mikuac.shiro.core.Bot;
//import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import top.cikipad.cikibot.common.auth.AuthService;
//import top.cikipad.cikibot.util.ERPatchNotesUtil;
//import top.cikipad.cikibot.util.StringSplitter;
//
//import java.util.List;
//
//@Component
//@Shiro
//@Slf4j
//public class ERPatchNotesListener {
//    private static final String GET_PATCH_NOTES_PREFIX = "查询更新";
//    private static final String ERROR_UNAUTHORIZED = "未授权访问";
//
//    @Autowired
//    private AuthService authService;
//
//    @AnyMessageHandler
//    @MessageHandlerFilter(startWith = {GET_PATCH_NOTES_PREFIX})
//    public void getPatchNotes(Bot bot, AnyMessageEvent event) {
//        if (!checkAuthAndRespond(bot, event, false)) {
//            return;
//        }
//
//        bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 获取更新日志中...").build(), false);
//
//        try {
//            String patchNotesReport = ERPatchNotesUtil.generateLatestPatchNotesReport();
//            List<String> strings = StringSplitter.forceSplit(patchNotesReport, 1970);
//            for (int i=0;i<strings.size();i++) {
//                if (i>=10) {
//                    bot.sendMsg(event, "...", false);
//                    break;
//                }
//                bot.sendMsg(event, strings.get(i), false);
//            }
//            log.info("发送ER更新日志至{}",
//                event.getGroupId() == null ? event.getUserId() : event.getGroupId());
//        } catch (Exception e) {
//            log.error("Failed to get ER patch notes", e);
//            bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 获取更新日志失败").build(), false);
//        }
//    }
//
//    private boolean checkAuthAndRespond(Bot bot, AnyMessageEvent event, boolean requireAdmin) {
//        if (!authService.checkAuth(event, requireAdmin)) {
//            bot.sendMsg(event, ERROR_UNAUTHORIZED, false);
//            return false;
//        }
//        return true;
//    }
//}