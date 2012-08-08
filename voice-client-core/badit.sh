#!/bin/bash
BASEDIR=$(dirname $0)
LOCALDIR=$PWD

function check_return_code(){
	if [ "$1" != "0" ];then
		echo "last command failed bailing"
		exit $1
	fi
}

function check_environment_vars(){
	if [ -z $JAVA_HOME ];then
		echo "missing JAVA_HOME please set"
		echo "eg. on mac mine is JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/"
		exit 1
	fi
	if [ -z $ANDROID_NDK_ROOT ];then
		echo "missing ANDROID_NDK_ROOT please set"
		echo "eg. on mac mine is ANDROID_NDK_ROOT=\$HOME/android-ndks/android-ndk-r8"
		exit 1
	fi
}
check_environment_vars
rm -rf $BASEDIR/bin/*

adb uninstall com.tuenti.voice;
check_return_code "$?"

./build.sh dd;
check_return_code "$?"

ant debug
check_return_code "$?"

adb install $BASEDIR/bin/VoiceClientActivity-debug.apk
check_return_code "$?"

$ANDROID_NDK_ROOT/ndk-gdb --start
check_return_code "$?"

echo "TOTAL SUCCESS!"

#from what I can tell the above is trying to do the same as is commented below, but below doesn't work
#$JAVA_HOME/bin/keytool -genkeypair -validity 10000 -dname "CN=tuenti, OU=voice, O=tuenti, L=Spain S=Madrid C=44" -keystore $BASEDIR/AndroidTest.keystore -storepass tuenti -keypass tuenti -alias AndroidTestKey -keyalg RSA -v
#check_return_code "$?"
#$JAVA_HOME/bin/javac -verbose -d $BASEDIR/obj -classpath $ANDROID_HOME/platforms/android-16/android.jar -sourcepath $BASEDIR/src $BASEDIR/src/com/tuenti/voice/*.java;
#check_return_code "$?"
#$ANDROID_HOME/platform-tools/dx --dex --verbose --output=$BASEDIR/bin/classes.dex $BASEDIR/obj $BASEDIR/libs/;
#check_return_code "$?"
#aapt package -v -f -S $BASEDIR/res -J $BASEDIR/src -M $BASEDIR/AndroidManifest.xml -I $ANDROID_HOME/platforms/android-16/android.jar -F $BASEDIR/bin/com.tuenti.voice.unsigned.apk $BASEDIR/bin;
#check_return_code "$?"
#$JAVA_HOME/bin/jarsigner -verbose -keystore $BASEDIR/AndroidTest.keystore -storepass tuenti -keypass tuenti -signedjar $BASEDIR/bin/com.tuenti.voice.signed.apk $BASEDIR/bin/com.tuenti.voice.unsigned.apk AndroidTestKey;
#check_return_code "$?"
#$ANDROID_HOME/tools/zipalign -v -f 4 $BASEDIR/bin/com.tuenti.voice.signed.apk $BASEDIR/bin/com.tuenti.voice.apk
#adb install $BASEDIR/bin/com.tuenti.voice.apk;
