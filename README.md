webrtc-jingle for android
=============
(libjingle signaling + webrtc voice engine)

## About:

* Working example app of libjingle and webrtc voice backend, with a few buttons.
* Our demo apk with support for arm/armv7 comes to 6.4 mb, approx a third when
zipped in an apk. Without compiler optimizations it's a bit less.
* Based on libjingle trunk and webrtc trunk updated on regular intervals.
* Stability improvements needed for the c layer and misc. pieces that were
needed for Android support.
* Calling between two of these clients may require your own stun server. There
is a working stun server included with libjingle, and you have to modify source
where google explicitly defines stun.l.google.com. If one client is web based
gmail for example, just for testing, I haven't seen any problems.

## Prereqs:

* [android NDK r8](http://developer.android.com/sdk/ndk/index.html). r8b was
tested and it doesn't work. Just modify the download link to get r8
* [Android SDK](http://developer.android.com/sdk/index.html)
* [eclipse](http://www.eclipse.org/downloads/)
* [Maven](http://maven.apache.org/download.html) v3.0.3+
* Add the following to your environment, i.e. .bashrc or .bash_profile

```
#Android
export ANDROID_SDK_ROOT=/path/to/sdk/
export ANDROID_NDK_ROOT=/path/to/ndk/
export PATH=$PATH:$ANDROID_NDK_ROOT:$ANDROID_SDK_ROOT:$ANDROID_SDK_ROOT/platform-tools

#mvn variables
export ANDROID_HOME=$ANDROID_SDK_ROOT
export ANDROID_NDK_HOME=$ANDROID_NDK_ROOT
```

## Get started with the code:

```
# mkdir webrtcjingleproject
# cd webrtcjingleproject
# gclient config https://github.com/lukeweber/webrtc-jingle-client.git --name trunk
# gclient sync

# cd trunk
# export CHROME_SRC=`pwd`
# source build/android/envsetup.sh
# gclient runhooks
```
* Set your username, pass and connection setttings in voice-client-example/src/main/java/com/tuenti/voice/example/ui/VoiceClientActivity.java.
* Build the core(c++ code): cd trunk/voice-client-core && ./build.sh
* Build the apks: cd trunk && mvn install
* To run a debugger: build/android/gdb_apk -p com.tuenti.voice.example -l voice-client-core/obj/local/armeabi-v7a/
* Build, deploy to phone, and start debugger in one script: badit.sh


## Run unittest
'''
* Build debug code jni in debug mode: cd trunk/voice-client-core && ./build.sh
* Generate unittest apk: tools/gen_tests_apk.sh
* Install unittest : adb install -r adb install -r voice_testing/${app_abi}/${lib}/${lib}-debug.apk
* Prepare
* Run unittest:
# adb shell mkdir /sdcard/talk
# adb shell am start -n org.chromium.native_test/org.chromium.native_test.ChromeNativeTestActivity
# See adb logcat adb | grep libjingle
* Fetch unittest logs:  adb pull /sdcard/talk  talk-logs

## Todo/Issues:
* See Tickets
