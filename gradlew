#!/bin/sh
APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Cari Java
if [ -n "$JAVA_HOME" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA="java"
fi

exec "$JAVA" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
