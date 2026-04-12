#!/bin/sh
# Gradle wrapper para Linux/Mac
GRADLE_OPTS="${GRADLE_OPTS:-} -Xmx64m -Xms64m"
APP_NAME="MediQuest"
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd)
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"

# Procura por um JDK válido
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVACMD="java"
else
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found." >&2
    exit 1
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
