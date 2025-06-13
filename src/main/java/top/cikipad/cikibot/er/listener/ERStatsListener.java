package top.cikipad.cikibot.er.listener;

import com.mikuac.shiro.annotation.AnyMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.cikipad.cikibot.common.auth.AuthService;
import top.cikipad.cikibot.util.ERDataManager;
import top.cikipad.cikibot.util.ERStatsUtil;

@Component
@Shiro
@Slf4j
public class ERStatsListener {

    @Value("${autoLoadErDict:true}")
    private boolean autoLoadErDict = false;

    private static final String GET_STATS_PREFIX = "查询成分 ";
    private static final String GET_RECENT_MATCHES_PREFIX = "查询战绩 ";
    private static final String GET_META_STATS_PREFIX = "版本分析";
    private static final String ERROR_UNAUTHORIZED = "未授权访问";
    private static final String ERROR_PLAYER_NOT_FOUND = "未找到该玩家的战绩信息";


    @PostConstruct
    private void init() {
        if (autoLoadErDict) {
            ERDataManager.getInstance();
        }
    }

    @Autowired
    private AuthService authService;

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {GET_META_STATS_PREFIX})
    public void getMetaStats(Bot bot, AnyMessageEvent event) {
        if (!checkAuthAndRespond(bot, event, false)) {
            return;
        }

        bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 获取版本数据中...").build(), false);
        
        try {
            String metaReport = ERStatsUtil.generateCharacterMetaReport();
            bot.sendMsg(event, metaReport, false);
            log.info("发送ER版本分析至{}", 
                event.getGroupId() == null ? event.getUserId() : event.getGroupId());
        } catch (Exception e) {
            log.error("Failed to get ER meta stats", e);
            bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 获取版本数据失败").build(), false);
        }
    }

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {GET_STATS_PREFIX})
    public void getStats(Bot bot, AnyMessageEvent event) {
        if (!checkAuthAndRespond(bot, event, false)) {
            return;
        }

        String playerName = getRealParam(GET_STATS_PREFIX, event.getMessage());
        if (playerName.isEmpty()) {
            bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 请输入玩家名称").build(), false);
            return;
        }

        bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 查询中...").build(), false);
        
        try {
            String statsReport = ERStatsUtil.generateStatsReport(playerName);
            bot.sendMsg(event, statsReport, false);
            log.info("发送ER战绩至{},玩家:{}", 
                event.getGroupId() == null ? event.getUserId() : event.getGroupId(), 
                playerName);
        } catch (Exception e) {
            log.error("Failed to get ER stats for player: {}", playerName, e);
            bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" " + ERROR_PLAYER_NOT_FOUND).build(), false);
        }
    }

    @AnyMessageHandler
    @MessageHandlerFilter(startWith = {GET_RECENT_MATCHES_PREFIX})
    public void getRecentMatches(Bot bot, AnyMessageEvent event) {
        if (!checkAuthAndRespond(bot, event, false)) {
            return;
        }

        String playerName = getRealParam(GET_RECENT_MATCHES_PREFIX, event.getMessage());
        if (playerName.isEmpty()) {
            bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 请输入玩家名称").build(), false);
            return;
        }

        bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" 查询中...").build(), false);
        
        try {
            String matchesReport = ERStatsUtil.generateRecentMatchesReport(playerName);
            bot.sendMsg(event, matchesReport, false);
            log.info("发送ER近期战绩至{},玩家:{}", 
                event.getGroupId() == null ? event.getUserId() : event.getGroupId(), 
                playerName);
        } catch (Exception e) {
            log.error("Failed to get ER recent matches for player: {}", playerName, e);
            bot.sendMsg(event, MsgUtils.builder().at(event.getUserId()).text(" " + ERROR_PLAYER_NOT_FOUND).build(), false);
        }
    }

    private boolean checkAuthAndRespond(Bot bot, AnyMessageEvent event, boolean requireAdmin) {
        if (!authService.checkAuth(event, requireAdmin)) {
            bot.sendMsg(event, ERROR_UNAUTHORIZED, false);
            return false;
        }
        return true;
    }

    private String getRealParam(String prefix, String input) {
        return input == null ? "" : input.substring(prefix.length()).trim();
    }
} 