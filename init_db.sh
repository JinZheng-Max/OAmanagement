#!/bin/bash
# ===================================================================
# 智办AI OA 数据库更新/初始化脚本 (Bash 版本)
# ===================================================================

echo "========================================"
echo "  智办AI OA 数据库初始化与更新"
echo "========================================"

# 默认参数
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="root"
DB_PASS=""

# 读取 .env 配置
if [ -f .env ]; then
    echo "检测到 .env 配置文件，正在读取配置..."
    # 逐行读取并解析
    while IFS= read -r line || [ -n "$line" ]; do
        # 忽略空行和注释
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
        
        # 移除空格并拆分 key/value
        key=$(echo "$line" | cut -d'=' -f1 | tr -d '[:space:]')
        val=$(echo "$line" | cut -d'=' -f2- | tr -d '[:space:]')
        
        if [ "$key" = "MYSQL_USERNAME" ]; then
            DB_USER="$val"
        elif [ "$key" = "MYSQL_PASSWORD" ] && [ "$val" != "replace-me" ]; then
            DB_PASS="$val"
        elif [ "$key" = "MYSQL_URL" ]; then
            # 从 jdbcUrl 中提取 host 和 port (如 jdbc:mysql://localhost:3306/oa_db)
            host_port=$(echo "$val" | cut -d'/' -f3)
            DB_HOST=$(echo "$host_port" | cut -d':' -f1)
            DB_PORT=$(echo "$host_port" | cut -d':' -f2)
        fi
    done < .env
else
    echo "未检测到 .env 文件，使用默认值。"
fi

# 交互式确认
read -p "请输入 MySQL 主机地址 (当前: $DB_HOST): " input_host
if [ -n "$input_host" ]; then
    DB_HOST="$input_host"
fi

read -p "请输入 MySQL 端口号 (当前: $DB_PORT): " input_port
if [ -n "$input_port" ]; then
    DB_PORT="$input_port"
fi

read -p "请输入 MySQL 用户名 (当前: $DB_USER): " input_user
if [ -n "$input_user" ]; then
    DB_USER="$input_user"
fi

if [ -z "$DB_PASS" ]; then
    read -sp "请输入 MySQL 密码 (留空则无密码): " input_pass
    echo ""
    DB_PASS="$input_pass"
else
    read -sp "请输入 MySQL 密码 (直接回车使用 .env 中的密码): " input_pass
    echo ""
    if [ -n "$input_pass" ]; then
        DB_PASS="$input_pass"
    fi
fi

SQL_FILE="sql/data.sql"
if [ ! -f "$SQL_FILE" ]; then
    echo "错误: 未找到 SQL 脚本 $SQL_FILE"
    exit 1
fi

echo "正在执行数据库初始化脚本..."

# 构建 mysql 命令行参数
MYSQL_CMD="mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER}"
if [ -n "${DB_PASS}" ]; then
    # 注意 -p 后面直接接密码，不能有空格
    MYSQL_CMD="${MYSQL_CMD} -p${DB_PASS}"
fi

# 执行 SQL
${MYSQL_CMD} < "${SQL_FILE}"

if [ $? -eq 0 ]; then
    echo "========================================"
    echo "  ✅ 数据库表结构更新成功！"
    echo "========================================"
else
    echo "========================================"
    echo "  ❌ 数据库更新失败，请检查连接参数或权限！"
    echo "========================================"
    exit 1
fi
