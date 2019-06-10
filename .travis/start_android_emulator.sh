#!/bin/sh
# Starts up the Android emulator, and waits for it to be ready.
echo "** Starting emulator in background..."
${EMULATOR} -avd "emu" \
    -no-skin \
    -no-window \
    -gpu swiftshader_indirect \
    -camera-back none \
    -camera-front none \
    -no-snapshot-save \
    -no-snapstorage \
    ${EMULATOR_ARGS} &

echo "** Waiting for emulator..."
./.travis/android-wait-for-emulator || exit 1

echo "** Connected devices:"
${ADB} devices

echo "** Device kernel:"
${ADB} shell uname -a

echo "** Device info:"
${ADB} shell am get-config
${ADB} shell getprop ro.build.version.release

echo "** Device features:"
${ADB} shell pm list features
