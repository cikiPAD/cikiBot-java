# cikiBot-java
ontbot 11 qq机器人实现，自娱自乐的项目，喜欢请任意取用  
java onebot sdk使用shiro sdk  
chatgpt java sdk 使用 chatgpt-java    

# 功能介绍
1.权限控制，支持白名单和管理员  
2.chatgpt聊天，支持自行切换模型，支持上下文定时保持，支持可配置上下文裁剪个数，支持群聊和私聊，支持根据qq号不同独立维护上下文  
3.涩图机器人，支持设置多种不同的图库，支持pixiv排行榜，支持自定义数量以及不可描述的参数，支持私聊群聊独立配置  

# 部署相关：  
目前只提供了linux的部署包,本质上就是springboot runnable jar包,windows可以自行琢磨  

1.需要一个机器人肉体夺舍，推荐napcatqq，onebot机器人启动自行查看对应实现的说明书。本项目主要需要webservice反向代理地址  
2.下载release的tar.gz,解压到linux主机任何目录  
3.修改config中application.properties对应的配置  
4.执行start-vm.sh启动项目  

配置说明  
spring.application.name=cikibot  //不用改  

shiro.ws.client.enable=true  //参考shiro对应配置  
shiro.ws.client.url=ws://your_host:wsport  //反向ws地址  

chatbot.admin.qqnum=admin_qq_num  //管理员qq号 多个qq用,分割  
chatbot.apiKey=your_openaikey    //openai的key  
chatbot.apiHost=openai_host     //openai的地址,有代理用代理,没有就填原地址,例如：https://api.openai.com 或者 https://api.chatanywhere.tech   
contextHoldLength=4      //chatgpt聊天持有上下文长度  

acgmx.token=your_token  //涩图 排行 用到 ，到  https://www.acgmx.com/ 自行申请  
saucenao.apikey=your_apikey  //搜图用到，太久没维护了，不确保功能正常，暂时未开放，不用管  

# 功能说明：  
# # 1.权限相关  
权限有两种，配置在application.properties中的管理员qq拥有全部权限  
添加白名单中的用户可以拥有实例指令权限，比如请求涩图，使用gpt聊天，但是无法修改配置，比如设置模型，设置图库  

 1)添加白名单  
      群白名单用 g-开头,个人白名单用p-开头  
      例如：  
        添加白名单 g-qq群号码  
        添加白名单 p-qq码  
 2)移除白名单  
      如上不难理解  
      例如:  
        移除白名单 g-qq群号码  
    
  
# # 2.涩图机器人  
 1)设置图库  
   支持多种图库
   例如 :  
    设置图库 lolicon  
    乱写一个参数，会返回所有支持的图库  
 2)设置图库参数   
   设置所有图库的参数或者为每个图库单独设置参数  
   例如 :  
    设置图库参数 lolicon:num 1  
    设置图库参数 num 1  
    设置图库参数 r18 0  

  参数不多解释，可以理解为就支持num和r18。  
  免责声明：  
    图片来源取决于图库，会获取到什么尺度的图我无法决定，此项目的默认参数设置确保了使用此项目的人能够看到绿色健康尺度的图片，请不要尝试修改r18 参数，传播不好的图片带来的     负面影响与本人无关！  

3)涩图 gkd  
   按照设置参数发点好康的涩图  
4)涩图 排行  
   从acgmx取pixiv对应排行  
   例如：  
    涩图 排行 day  
    涩图 排行 week  
    涩图 排行 month  
5)涩图 排行2  
  同上 key不同罢了  
    例如:  
    涩图 排行 daily  
   依次类推  
6)涩图 tag  
  例如 ：  
    涩图 原神  
   给你推原神随机涩图  
   tag根据不同图库，可能支持，可能不支持，自行尝试  


# # 3.chatgpt聊天机器人  
 1)设置模型 参数  
      例如：  
           设置模型 gpt-3.5-turbo  
           设置模型 gpt-4o  
           设置模型 gpt-4o-mini  
      模型可以自行切换  
 2)预设 参数  
      目前预设只有一个   
      例如：  
        预设 猫娘  
 3)私聊触发  
     固定前缀  对话   
     例如：  
        对话 测试  
 4)群聊触发  
     直接@机器人  
     例如：
       @机器人 测试

     

