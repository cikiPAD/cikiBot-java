//package top.cikipad.cikibot.imagebot.porn.plugin;
//
//import com.mikuac.shiro.core.Bot;
//import com.mikuac.shiro.core.BotPlugin;
//import com.mikuac.shiro.dto.event.message.AnyMessageEvent;
//import top.cikipad.cikibot.imagebot.porn.ImageSourceManager;
//import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;
//
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
//public class ImagePlugin extends BotPlugin {
//
//    public static final Set<String> KEYWORD_SET = new HashSet<>();
//
//    static {
//        KEYWORD_SET.add("来点涩图 ");
//    }
//
//    @Override
//    public int onAnyMessage(Bot bot, AnyMessageEvent event) {
//        String message = event.getMessage();
//        if (message == null || message.trim().length() == 0) {
//            return MESSAGE_IGNORE;
//        }
//
//        for (String key:KEYWORD_SET) {
//            if (message.startsWith(key)) {
//                String param = message.substring(key.length());
//                Map<String, Object> map = new HashMap<>();
//                ImageSourceManager.getInstance().getImageUrlsEntity(SourceTypeConstant.LOLICON, );
//
//            }
//        }
//
//
//
//
//        return MESSAGE_BLOCK;
//    }
//}
