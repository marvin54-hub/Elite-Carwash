#!/bin/sh
echo "=== Java Version Check ==="
java -version 2>&1
echo ""
echo "=== Required: Java 17 or newer ==="
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -ge 17 ] 2>/dev/null; then
    echo "✅ Java $JAVA_VER — OK"
    echo ""
    echo "Run: mvn spring-boot:run"
    echo "  OR: ./mvnw spring-boot:run"
else
    echo "❌ Java $JAVA_VER is too old. This project requires Java 17+."
    echo ""
    echo "Download Java 17: https://adoptium.net/temurin/releases/?version=17"
fi
