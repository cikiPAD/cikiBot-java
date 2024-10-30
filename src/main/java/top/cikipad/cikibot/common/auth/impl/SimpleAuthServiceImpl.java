package top.cikipad.cikibot.common.auth.impl;

import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.cikipad.cikibot.common.auth.AuthService;
import top.cikipad.cikibot.constant.CommonConstant;
import top.cikipad.cikibot.util.ConfigManager;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class SimpleAuthServiceImpl implements AuthService {
    @Autowired
    private ConfigManager config;

    private Set<String> authed = new HashSet<>();

    @Override
    public boolean checkAuth(PrivateMessageEvent event,boolean strict) {
        if (event.getUserId() == null) {
            return false;
        }

        //是管理员
        if (config.isAdmin(event.getUserId() + "")) {
            return true;
        }

        //在白名单中
        if (!strict && config.isInWhileList(CommonConstant.PRIVATE_PREFIX + event.getUserId())) {
            return true;
        }

        return false;
    }

    @Override
    public boolean checkAuth(GroupMessageEvent event, boolean strict) {
        if (event.getUserId() == null || event.getGroupId() == null) {
            return false;
        }

        //是管理员
        if (config.isAdmin(event.getUserId() + "")) {
            return true;
        }

        //个人在白名单中
        if (!strict && config.isInWhileList(CommonConstant.PRIVATE_PREFIX + event.getUserId())) {
            return true;
        }

        //群在白名单中
        if (!strict && config.isInWhileList(CommonConstant.GROUP_PREFIX + event.getGroupId())) {
            return true;
        }

        return false;
    }

    @Override
    public boolean checkAuth(AnyMessageEvent event, boolean strict) {
        if (event.getUserId() == null) {
            return false;
        }

        //是管理员
        if (config.isAdmin(event.getUserId() + "")) {
            return true;
        }

        //在白名单中
        //判断是否是群组消息
        if (event.getGroupId() == null) {
            if (!strict && config.isInWhileList(CommonConstant.PRIVATE_PREFIX + event.getUserId())) {
                return true;
            }
        }
        else {
            //个人在白名单中
            if (!strict && config.isInWhileList(CommonConstant.PRIVATE_PREFIX + event.getUserId())) {
                return true;
            }

            //群在白名单中
            if (!strict && config.isInWhileList(CommonConstant.GROUP_PREFIX + event.getGroupId())) {
                return true;
            }
        }

        return false;
    }
}
