#!/bin/sh
# Maven Wrapper script
# If you see "./mvnw: Permission denied" run: chmod +x mvnw

MAVEN_WRAPPER_JAR=".mvn/wrapper/maven-wrapper.jar"
MAVEN_PROJECTBASEDIR=$(pwd)
MAVEN_OPTS="${MAVEN_OPTS:-}"

# Download wrapper jar if not present
if [ ! -f "$MAVEN_WRAPPER_JAR" ]; then
    echo "Downloading Maven wrapper..."
    wget -q -O "$MAVEN_WRAPPER_JAR" \
        "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar" \
        || curl -sL -o "$MAVEN_WRAPPER_JAR" \
        "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
fi

exec java -cp ".mvn/wrapper/maven-wrapper.jar" \
    org.apache.maven.wrapper.MavenWrapperMain "$@"
