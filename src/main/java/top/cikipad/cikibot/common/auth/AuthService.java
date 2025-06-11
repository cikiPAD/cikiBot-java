package top.cikipad.cikibot.common.auth;

import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;

public interface AuthService {
    boolean checkAuth(PrivateMessageEvent event,boolean strict);

    boolean checkAuth(GroupMessageEvent event,boolean strict);

    boolean checkAuth(AnyMessageEvent event,boolean strict);
}
