#!/bin/bash

# 定义变量
JAR_NAME="sqlconverter-0.0.1-SNAPSHOT.jar"  # 替换为你的jar包名称
LOG_PATH="./log/app.log"               # 日志输出文件路径

# 打印启动信息
echo "Starting application..."

# 启动应用，并将输出写入日志文件
nohup java -jar "$JAR_NAME" > "$LOG_PATH" 2>&1 &

# 获取进程ID
PID=$!

# 检查是否成功启动
if [ "$PID" -ne 0 ]; then
    echo "Application started with PID: $PID"
    echo "Log file: $LOG_PATH"
else
    echo "Failed to start application."
    exit 1
fi