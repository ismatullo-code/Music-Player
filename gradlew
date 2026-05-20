#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"
java -version >/dev/null 2>&1 || { echo "Java not installed"; exit 1; }
echo "Gradle wrapper placeholder created."
echo "Open this project in Android Studio and it will auto-download Gradle."
