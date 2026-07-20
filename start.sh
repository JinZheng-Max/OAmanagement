#!/bin/bash
# =============================================
# OA管理系统 一键启动脚本
# 用法: bash start.sh
# 注：需先确保 MySQL 已启动
# =============================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "================================"
echo "  OA 管理系统 一键启动"
echo "================================"

# 1. 启动 Redis（如果没在运行的话）
redis-cli ping > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "[1/4] 启动 Redis..."
    redis-server &
    sleep 2
else
    echo "[1/4] Redis 已在运行 ✓"
fi

# 2. 启动 identity-service
echo "[2/4] 启动 identity-service..."
cd "$PROJECT_DIR"
bash mvnw spring-boot:run -pl identity-service -q -Dmaven.test.skip=true &

# 等待身份服务就绪（首次可能依赖下载较慢）
echo "      等待启动（约 40 秒）..."
sleep 40

# 3. 启动 oa-gateway
echo "[3/4] 启动 oa-gateway..."
bash mvnw spring-boot:run -pl oa-gateway -q -Dmaven.test.skip=true &

sleep 20

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
