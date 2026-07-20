@echo off
:: ===================================================================
:: 智办AI OA 数据库更新/初始化脚本 (Windows CMD 版本)
:: ===================================================================
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ========================================
echo   智办AI OA 数据库初始化与更新
echo ========================================

:: 默认参数
set DB_HOST=localhost
set DB_PORT=3306
set DB_USER=root
set DB_PASS=

:: 读取 .env 配置
if exist .env (
    echo 检测到 .env 配置文件，正在读取配置...
    for /f "usebackq delims=" %%a in (".env") do (
        set "line=%%a"
        :: 过滤注释
        if not "!line:~0,1!"=="#" (
            for /f "tokens=1* delims=" %%i in ("!line!") do (
                for /f "tokens=1,2 delims==" %%k in ("%%i") do (
                    set "key=%%k"
                    set "val=%%l"
                    :: 去除空格
                    set "key=!key: =!"
                    set "val=!val: =!"
                    if "!key!"=="MYSQL_USERNAME" set DB_USER=!val!
                    if "!key!"=="MYSQL_PASSWORD" if not "!val!"=="replace-me" set DB_PASS=!val!
                    if "!key!"=="MYSQL_URL" (
                        :: 粗略提取 Host 和 Port
                        for /f "tokens=3 delims=/" %%m in ("!val!") do (
                            for /f "tokens=1,2 delims=:" %%p in ("%%m") do (
                                set DB_HOST=%%p
                                set DB_PORT=%%q
                            )
                        )
                    )
                )
            )
        )
    )
) else (
    echo 未检测到 .env 文件，使用默认值。
)

:: 交互式确认
set /p input_host=请输入 MySQL 主机地址 (当前: %DB_HOST%): 
if not "%input_host%"=="" set DB_HOST=%input_host%

set /p input_port=请输入 MySQL 端口号 (当前: %DB_PORT%): 
if not "%input_port%"=="" set DB_PORT=%input_port%

set /p input_user=请输入 MySQL 用户名 (当前: %DB_USER%): 
if not "%input_user%"=="" set DB_USER=%input_user%

if "%DB_PASS%"=="" (
    set /p DB_PASS=请输入 MySQL 密码 (留空则无密码): 
) else (
    set /p input_pass=请输入 MySQL 密码 (直接回车使用 .env 中的密码): 
    if not "!input_pass!"=="" set DB_PASS=!input_pass!
)

set SQL_FILE=sql\data.sql
if not exist %SQL_FILE% (
    echo 错误: 未找到 SQL 脚本 %SQL_FILE%
    pause
    exit /b 1
)

echo 正在执行数据库更新...
if "%DB_PASS%"=="" (
    mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% < %SQL_FILE%
) else (
    mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% < %SQL_FILE%
)

if %ERRORLEVEL% equ 0 (
    echo ========================================
    echo   ✅ 数据库表结构更新成功！
    echo ========================================
) else (
    echo ========================================
    echo   ❌ 数据库更新失败，请检查参数与 mysql 命令！
    echo ========================================
)

pause
