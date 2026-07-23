#!/bin/bash
# OA管理系统 一键启动（Git Bash 用）
# 用法: bash start.sh

export JAVA_HOME="C:/Program Files/Java/jdk-21"
JAVA_ARGS="-Dspring.cloud.nacos.config.enabled=false -Dspring.cloud.nacos.discovery.enabled=false"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "================================"
echo "  OA 管理系统 一键启动"
echo "================================"

# 1. 启动 Redis
echo "[1/4] 检查 Redis..."
redis-cli ping > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "      正在启动 Redis..."
    redis-server &
    sleep 3
else
    echo "      Redis 已在运行"
fi

# 2. 启动 identity-service
echo "[2/4] 启动 identity-service..."
cd "$PROJECT_DIR"
bash mvnw spring-boot:run -pl identity-service -q -Dmaven.test.skip=true \
  -Dspring-boot.run.jvmArguments="$JAVA_ARGS" &
echo "      等待启动（约 40 秒）..."
sleep 40

# 3. 启动 oa-gateway
echo "[3/4] 启动 oa-gateway..."
bash mvnw spring-boot:run -pl oa-gateway -q -Dmaven.test.skip=true \
  -Dspring-boot.run.jvmArguments="$JAVA_ARGS" &
echo "      等待启动（约 25 秒）..."
sleep 25

# 4. 启动前端
echo "[4/4] 启动前端..."
cd "$PROJECT_DIR/oa-web" && npm run dev &

echo ""
echo "================================"
echo "  ✅ 启动完成！"
echo "  前端地址: http://localhost:5173"
echo "  登录账号: admin / Admin@123456"
echo "================================"
echo ""
echo "按 Ctrl+C 停止所有服务"
wait
