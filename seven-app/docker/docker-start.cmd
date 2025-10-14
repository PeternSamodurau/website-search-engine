@echo off
echo "========================================="
echo "  Building and starting containers...    "
echo "  (This output will go to docker-log.txt)"
echo "========================================="

REM Указываем путь к файлу docker-compose.yml и отправляем вывод в файл
docker-compose -f docker\docker-compose.yml up --build -d > "docker\docker-log.txt" 2>&1

echo.
echo "========================================="
echo "  Displaying application logs (app-1)... "
echo "  (Press Ctrl+C to stop)                 "
echo "========================================="

REM Указываем путь к файлу и выводим в консоль только логи сервиса "app"
docker-compose -f docker\docker-compose.yml logs -f app
