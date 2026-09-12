#!/usr/bin/env bash
set -e

# Use JDK 21
if [ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
elif [ -d "/usr/lib/jvm/temurin-21-jdk-amd64" ]; then
    export JAVA_HOME="/usr/lib/jvm/temurin-21-jdk-amd64"
fi
export PATH="$JAVA_HOME/bin:$PATH"

echo "Using Java: $(java -version 2>&1 | head -1)"

cd "$(dirname "$0")"

./gradlew clean build deployMod

echo "Build successful! Output:"
ls -lh build/libs/*.jar
echo "Deployed mod jar:"
ls -lh "/home/lunarea/.var/app/com.modrinth.ModrinthApp/data/ModrinthApp/profiles/Farlands MSMP (2)/mods/copper_age_patch-1.0.0.jar"
if [ -f "/home/lunarea/.var/app/com.modrinth.ModrinthApp/data/ModrinthApp/profiles/Farlands MSMP/mods/copper_age_patch-1.0.0.jar" ]; then
    echo "Also deployed to Farlands MSMP profile:"
    ls -lh "/home/lunarea/.var/app/com.modrinth.ModrinthApp/data/ModrinthApp/profiles/Farlands MSMP/mods/copper_age_patch-1.0.0.jar"
fi
