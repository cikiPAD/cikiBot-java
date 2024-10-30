package top.cikipad.cikibot.test;


import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.DownloadFileResp;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.cikipad.cikibot.imagebot.porn.ImageSourceManager;
import top.cikipad.cikibot.imagebot.porn.constant.ParamsConstant;
import top.cikipad.cikibot.imagebot.porn.constant.SourceTypeConstant;
import top.cikipad.cikibot.imagebot.porn.entity.ImageUrlEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class Test {

    @Autowired
    ImageSourceManager manager;


    @Resource
    private BotContainer botContainer;

    @Autowired
    private ImageSourceManager imageSourceManager;


    @PostConstruct
    public void test() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Map<String, Object> param = new HashMap<>();
                param.put(ParamsConstant.TAG, "daily");
                param.put(ParamsConstant.NUM, 5);
                List<ImageUrlEntity> imageUrlsEntity = manager.getImageUrlsEntity(SourceTypeConstant.ACGMX_NEW, param);
                System.out.println(imageUrlsEntity);

                // 机器人账号
                long botId = 3057925040L;

                // 通过机器人账号取出 Bot 对象
                Bot bot = botContainer.robots.get(botId);

                ActionData<DownloadFileResp> downloadFileRespActionData = bot.downloadFile(imageUrlsEntity.get(0).getUrls().get(0));
                System.out.println(downloadFileRespActionData);
                // 调用 Bot 对象方法
                //bot.sendPrivateMsg(505013146L, "Hi~", false);

            }
        }).start();





    }



}
