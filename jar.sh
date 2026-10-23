#!/bin/bash
#入参设置
FUNC=$1
APP_NAME=$2
XMS=1024M
XMX=2048M
SCRIPT=$0

if [ "X$3" != "X" ]; then
    XMS=$3
fi

if [ "X$4" != "X" ]; then
    XMX=$4
fi

# echo $FUNC $APP_NAME $XMS $XMX

#使用说明，用来提示输入参数
usage() {
    echo "Usage: sh boot.sh [start|stop|restart|status]"
    exit 1
}

#检查程序是否在运行
is_exist(){
    # pid=`ps -ef|grep $APP_NAME|grep -v grep|awk '{print $2}' `
    pid=`ps -ef|grep $APP_NAME|grep -v grep|grep -v monitor|grep -v $SCRIPT|awk '{print $2}' `
    #如果不存在返回1，存在返回0
    # echo 'pid is ----'${pid}
    if [ -z "${pid}" ]; then
        return 1
    else
        return 0
    fi
}

#启动方法
start(){
    is_exist
    if [ "$?" -eq '0' ]; then
        echo "${APP_NAME} is running with pid: ${pid}."
    else
        # 启动应用程序
        nohup java -Xms$XMX -Xmx$XMX -jar -Duser.timezone=Asia/Shanghai  $APP_NAME.jar --spring.profiles.active=dev > $APP_NAME.out 2>&1 &
        echo "${APP_NAME} is starting..."
    fi
}

#停止方法
stop(){
    is_exist
    if [ $? -eq "0" ]; then
        kill -15 $pid
        echo "${APP_NAME} is stoping with pid: ${pid}..."
        # 等待进程停止，每秒检查一次
        while true; do
            is_exist
            if [ $? -eq "0" ]; then
                sleep 1
            else
                echo "${APP_NAME} is stoped."
                break
            fi
        done
    else
        echo "${APP_NAME} is not running."
    fi
}

#输出运行状态
status(){
    is_exist
    if [ $? -eq "0" ]; then
        echo "${APP_NAME} is running with pid: ${pid}."
    else
        echo "${APP_NAME} is not running."
    fi
}

#重启
restart(){
    stop
    start
}

#根据输入参数，选择执行对应方法，不输入则执行使用说明
case "$1" in
    "start")
        start
    ;;
    "stop")
        stop
    ;;
    "status")
        status
    ;;
    "restart")
        restart
    ;;
    *)
        usage
    ;;
esac
