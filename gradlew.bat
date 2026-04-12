@rem Gradle wrapper para Windows
@if "%DEBUG%"=="" @echo off
setlocal
set GRADLE_OPTS=%GRADLE_OPTS% -Xmx64m -Xms64m
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
