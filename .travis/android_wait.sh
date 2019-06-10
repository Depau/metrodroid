#!/bin/sh
echo "** Waiting for emulator..."
./.travis/android-wait-for-emulator

${ADB} shell input keyevent 82 &
