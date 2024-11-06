#!/bin/bash

cd ../
APP_HOME=`pwd`
APP_SN=chatbot-ciki
JAVA_OPTS="-XX:-OmitStackTraceInFastThrow"
JAR="lib/cikibot-0.0.1-SNAPSHOT.jar"
#LANG=zh_CN.gbk

app_process_no=`ps -auxwww|grep $APP_SN|grep -v "grep"|awk '{print $2}'`

while [ -n "$app_process_no" ] ; do
 echo $APP_SN " 进程存在,需要先杀掉,进程号为:" $app_process_no
 kill -9 $app_process_no
 sleep 1
 app_process_no=`ps -ef|grep $app_sn|grep -v "grep"|awk '{print $2}'`
done

echo $APP_SN " 进程已不存在"
nohup java  -DAPP_SN=$APP_SN -DAPP_HOME=$APP_HOME -Dspring.main.allow-bean-definition-overriding=true  -Dloader.path=$APP_HOME/lib  $JAVA_OPTS -jar $JAR  >$APP_SN.out &
