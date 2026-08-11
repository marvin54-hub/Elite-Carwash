@REM Maven Wrapper for Windows
@echo off
set MAVEN_WRAPPER_JAR=.mvn\wrapper\maven-wrapper.jar
if not exist %MAVEN_WRAPPER_JAR% (
    echo Downloading Maven Wrapper...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar' -OutFile '%MAVEN_WRAPPER_JAR%'"
)
java -cp ".mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
