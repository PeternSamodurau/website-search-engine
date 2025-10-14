@echo off
echo "========================================="
echo "  Building Spring Boot application...    "
echo "========================================="
call .\gradlew clean build

if %errorlevel% neq 0 (
    echo "ERROR: Gradle build failed! Aborting."
    exit /b %errorlevel%
)

echo.
echo "========================================="
echo "  Starting Docker Compose...             "
echo "========================================="
docker-compose -f docker/docker-compose.yml up --build
