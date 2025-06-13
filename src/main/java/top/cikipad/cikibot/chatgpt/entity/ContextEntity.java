package top.cikipad.cikibot.chatgpt.entity;

import com.unfbx.chatgpt.entity.chat.Message;
import lombok.Data;

import java.util.List;

@Data
public class ContextEntity {
    private Long timeStamp;
    private List<Message> msgList;
    //移除上下文时，不移除预设
    private int lockedNext;
}
