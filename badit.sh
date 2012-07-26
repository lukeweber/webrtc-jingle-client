#!/bin/bash
#
# badit.sh - build and debug it
# @author: Nick Flink <nick@tuenti.com>
##
##  The following options are provided.
##  -h             help. What you are reading now.
##  -g             gyp. do the gyp build
##  -s             sync. sync before doing the build
#
# default variables
#
BUILDSYSTEM="gyp"
SYNCREPOS="no"
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
#
# helper functions
#
print_usage(){
  echo "$1" >&2
  echo -e "Usage: $0 [-h|-s|-g|]\n" >&2 
  sed -n '/^## /s/^## //p' $0 >&2
  exit 1
}
function check_return_code(){
  if [ "$1" != "0" ];then
    echo "last command failed bailing"
    popd;
    exit $1
  fi
}
#
# arg parsing
#
validate_optarg(){
  [[ "${OPTARG:0:1}" = "-" ]] && print_usage "-$opt: requires an argument"
}



while getopts "hgs" opt
do
  case $opt in
    h ) #help
      print_usage
      ;;
    g ) #gyp
      BUILDSYSTEM="gyp"
      ;;
    s ) #sync
      SYNCREPOS="yes"
      ;;
    : )
      print_usage "Option -$OPTARG requires an argument."
      ;;
    ? )
      if [ "${!OPTIND}" == "--help" ]
      then
        print_usage
      else
        print_usage "Invalid option: -$OPTARG"
      fi
      ;;
  esac
done

pushd $TRUNKDIR;
echo "patching gyp"
echo "$OPENSSL_PATCH"|patch --strip=1 --forward
#check_return_code "$?" commented because if alreay applied it ignores it but returns an error code

echo "CHROME_SRC=$TRUNKDIR"
export CHROME_SRC="$TRUNKDIR"
source "$TRUNKDIR/build/android/envsetup.sh" || { echo Fatal: Cannot load $TRUNKDIR/build/android/envsetup.sh;  exit 1; }

if [ "$SYNCREPOS" == "yes" ]; then
  gclient sync;
  check_return_code "$?"
fi
if [ "$BUILDSYSTEM" == "gyp" ]; then
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
else
  print_usage "Only gdb build system available for now"
fi
popd;
