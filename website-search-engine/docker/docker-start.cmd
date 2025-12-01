@echo off
rem Устанавливаем кодировку UTF-8 для корректного отображения логов
chcp 65001 > nul

echo "========================================================"
echo "  1. Stopping and removing old containers and networks..."
echo "========================================================"
docker-compose -f docker\docker-compose.yml down -v

echo.
echo "========================================================"
echo "  2. Pruning old (dangling) Docker images..."
echo "========================================================"
docker image prune -f

echo.
echo "========================================================"
echo "  3. Cleaning and building the Gradle project..."
echo "========================================================"
call gradlew clean build -x test

rem Проверяем, успешно ли прошла сборка. Если нет - останавливаем скрипт.
if %errorlevel% neq 0 (
    echo.
    echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    echo "  ERROR: Gradle build failed. See the output above for details."
    echo "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    exit /b %errorlevel%
)

echo "Build successful."
echo.
echo "========================================================"
echo "  4. Building and starting new containers..."
echo "========================================================"
rem Запускаем Docker Compose. Весь вывод пойдет в docker-log.txt.
docker-compose -f docker\docker-compose.yml up --build -d > "docker\docker-log.txt" 2>&1

echo.
echo "========================================================"
echo "  5. Displaying application logs (app)..."
echo "  (Press Ctrl+C to stop)"
echo "========================================================"
docker-compose -f docker\docker-compose.yml logs -f app