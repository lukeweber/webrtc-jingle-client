#!/bin/bash

LIB_LIST="libjingle_unittest \
    libjingle_media_unittest \
    libjingle_p2p_unittest \
    libwebrtcjingle_unittest"

export ANDROID_SDK_VERSION=14

TOOLSDIR="$( cd "$( dirname "$0" )" && pwd )"
TRUNKDIR="$( dirname $TOOLSDIR )"

OUT_DIR=${TRUNKDIR}"/voice_test"
APP_ABI_LIST="armeabi armeabi-v7a mips x86"

rm -rf $OUT_DIR

for app_abi in $APP_ABI_LIST
do
    mkdir -p $OUT_DIR/$app_abi

    for lib in $LIB_LIST
    do
	${TRUNKDIR}/tools/test/generate_native_test.py  \
	    --native_library=${TRUNKDIR}/android/voice-client-core/libs/${app_abi}/${lib}.so  \
	    --output=$OUT_DIR/${app_abi}/${lib} \
	    --app_abi=${app_abi} \
        --strip-binary=i686-linux-android-strip \
	    --ant-args=-DPRODUCT_DIR=$OUT_DIR/${app_abi}
    done
done
