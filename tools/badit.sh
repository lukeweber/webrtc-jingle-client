#!/bin/bash
#
# badit.sh - build and debug it
# @author: Nick Flink <nick@tuenti.com>
##
##  The following options are provided.
##  -h             help. What you are reading now.
##  -g             gyp. do the gyp build
##  -m             maven. do the maven build (this depends on the ndk build)
##  -c             clean. do a clean install
##  -s             sync. sync before doing the build
##  -j             jobs. force the number of processes
##  -p             patch. apply required patch for ssl
#
# default variables
#
CLEAN=""
SNAPSHOT_VERSION="1.0-SNAPSHOT"
BUILDSYSTEM="mvn"
SYNCREPOS="no"
TOOLSDIR="$( cd "$( dirname "$0" )" && pwd )"
TRUNKDIR="$( dirname $TOOLSDIR )"
SRCDIR="$TRUNKDIR/src"
OPENSSL_PATCH=""
BUILD_PROFILE="default_debug"
#BUILD_PROFILE="tuenti_debug"
#BUILD_PROFILE="default_release"
#BUILD_PROFILE="tuenti_release"
#
# helper functions
#
print_usage(){
  echo "$1" >&2
  echo -e "Usage: $0 [-h|-s|-g|j]\n" >&2 
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



while getopts "hgmcsj:" opt
do
  case $opt in
    h ) #help
      print_usage
      ;;
    g ) #gyp
      BUILDSYSTEM="gyp"
      ;;
    m ) #mvn
      BUILDSYSTEM="mvn"
      ;;
    c ) #clean
      CLEAN="clean"
      ;;
    s ) #sync
      SYNCREPOS="yes"
      ;;
    p ) #patch
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
      ;;
    j ) #jobs
      num_of_cores="${OPTARG}"
      export num_of_cores
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
if [ "$OPENSSL_PATCH" != "" ];then
    echo "patching gyp"
    echo -e "-------------------------------\nPATCHING_SSL\n-------------------------------"
    echo "$OPENSSL_PATCH"|patch --strip=1 --forward;
    check_return_code "$?"
fi

echo "CHROME_SRC=$TRUNKDIR"
export CHROME_SRC="$TRUNKDIR"
source "$TRUNKDIR/build/android/envsetup.sh" || { echo Fatal: Cannot load $TRUNKDIR/build/android/envsetup.sh;  exit 1; }

if [ "$SYNCREPOS" == "yes" ]; then
  gclient sync;
  check_return_code "$?"
fi
if [ -z $FORCE_CPU_ABI ]; then
   FORCE_CPU_ABI=`adb shell cat /system/build.prop | grep "ro.product.cpu.abi="|awk '{sub("="," ");print $2}'`
   echo "FORCE_CPU_ABI=$FORCE_CPU_ABI"
fi

if [ "$BUILDSYSTEM" == "gyp" ]; then
  echo -e "-------------------------------\nRUNNING_HOOKS\n-------------------------------"
  gclient runhooks;
  check_return_code "$?"

  make -j4 BUILDTYPE=Debug
  check_return_code "$?"

  adb uninstall com.tuenti.voice;
  check_return_code "$?"

  adb install $TRUNKDIR/out/Debug/VoiceClient/VoiceClient-debug.apk
  check_return_code "$?"

  adb shell am start -a android.intent.action.VIEW  -n com.tuenti.voice/.VoiceClientActivity
  check_return_code "$?"

  $TRUNKDIR/build/android/gdb_apk -p com.tuenti.voice -l out/Debug/obj.target/
  check_return_code "$?"
elif [ "$BUILDSYSTEM" == "mvn" ]; then
  if [ "$CLEAN" == "clean" ]; then
    echo -e "-------------------------------\nCLEANING\n-------------------------------"
    mvn clean
  fi
  
  echo -e "-------------------------------\nBUILDING/INSTALLING\n-------------------------------";
  #$TRUNKDIR/voice-client-core/build.sh
  mvn $CLEAN install -P $BUILD_PROFILE
  check_return_code "$?"
  
  echo -e "-------------------------------\nHEADERGEN\n-------------------------------"
  CLASSESPATH="$TRUNKDIR/voice-client-core/target/classes"
  pushd $TRUNKDIR/voice-client-core/jni
  javah -classpath $CLASSESPATH com.tuenti.voice.core.VoiceClient
  RET_CODE_CACHE="$?"
  popd
  check_return_code "$RET_CODE_CACHE"
  

  echo -e "-------------------------------\nUNINSTALLING\n-------------------------------"
  adb uninstall com.tuenti.voice.example
  check_return_code "$?"

  echo -e "-------------------------------\nDEPLOYING\n-------------------------------"
  adb install $HOME/.m2/repository/com/tuenti/voice/voice-example/$SNAPSHOT_VERSION/voice-example-$SNAPSHOT_VERSION.apk
  #mvn -pl voice-client-example android:deploy
  check_return_code "$?"

  echo -e "-------------------------------\nRUNNING\n-------------------------------"
  adb shell am start -a android.intent.action.VIEW  -n com.tuenti.voice.example/.ui.activity.VoiceClientActivity
  #mvn -pl voice-client-example android:run
  check_return_code "$?"

  echo -e "-------------------------------\nDEBUGGING\n-------------------------------"
  DBG_CMD="$TRUNKDIR/build/android/gdb_apk -p com.tuenti.voice.example -s VoiceClientService -l voice-client-core/obj/$BUILD_PROFILE/local/$FORCE_CPU_ABI"
  echo "DBG_CMD:$DBG_CMD"
  $DBG_CMD
  check_return_code "$?"
else
  print_usage "Only gyp & mvn build systems available for now"
fi
popd;
