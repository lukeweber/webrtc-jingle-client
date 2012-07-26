#!/bin/bash
TRUNKDIR="$( cd "$( dirname "$0" )" && pwd )"
SRCDIR="$TRUNKDIR/src"

OPENSSL_PATCH="--- a/third_party/openssl/openssl.gyp
+++ b/third_party/openssl/openssl.gyp
@@ -13,6 +13,8 @@
         'PURIFY',
         'TERMIO',
         '_REENTRANT',
+        'OPENSSL_NO_HW',
+        'OPENSSL_NO_GOST',
         # We do not use TLS over UDP on Chromium so far.
         'OPENSSL_NO_DTLS1',
       ],"
function check_return_code(){
  if [ "$1" != "0" ];then
    echo "last command failed bailing"
    popd;
    exit $1
  fi
}

pushd $TRUNKDIR;
echo "patching gyp"
echo "$OPENSSL_PATCH"|patch --strip=1 --forward
#check_return_code "$?" commented because if alreay applied it ignores it but returns an error code

echo "CHROME_SRC=$TRUNKDIR"
export CHROME_SRC="$TRUNKDIR"
source "$TRUNKDIR/build/android/envsetup.sh" || { echo Fatal: Cannot load $TRUNKDIR/build/android/envsetup.sh;  exit 1; }

gclient sync;
check_return_code "$?"

gclient runhooks;
check_return_code "$?"

make webrtc_jingle -j4 BUILDTYPE=Debug
check_return_code "$?"

adb uninstall com.tuenti.voice;
check_return_code "$?"

adb install $TRUNKDIR/out/Debug/VoiceClient/VoiceClient-debug.apk
check_return_code "$?"

adb shell am start -a android.intent.action.VIEW  -n com.tuenti.voice/.VoiceClientActivity
check_return_code "$?"

$TRUNKDIR/build/android/gdb_apk -p com.tuenti.voice -l out/Debug/obj.target/
check_return_code "$?"
popd;
