webrtc-jingle for android
=============
(libjingle signaling + webrtc voice engine) 

Discussion: [webrtc-jingle](https://groups.google.com/forum/?fromgroups#!forum/webrtc-jingle)

## About:

* Working example android and ios apps of libjingle and webrtc voice backend.
* Based on libjingle trunk and webrtc trunk updated on regular intervals.
* Added improvements for stability and missing pieces for mobile implementation.
* Can make calls between two phones, or between gmail and a phone.
* Happy for any help, please see tickets, and send a pull request.

## Getting the code:

* Download and install [depot_tools](http://dev.chromium.org/developers/how-tos/install-depot-tools)

```
# mkdir webrtcjingleproject
# cd webrtcjingleproject
# gclient config https://github.com/lukeweber/webrtc-jingle-client.git --name trunk
# gclient sync

or for an older stable build, take the head of the stable branch revision.
# gclient sync --revision PUT_STABLE_HEAD_REV_HERE
```

## Android:

### Prereqs:
* [android NDK r8d](http://developer.android.com/sdk/ndk/index.html)
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

### Running the project

* Set your username, pass and connection setttings in android/voice-client-example/src/main/java/com/tuenti/voice/example/ui/LoginView.java.
* Build the core(c++ code): cd trunk/android/voice-client-core && ./build.sh
* Build the apks: cd trunk/android && mvn install
* To run a debugger: build/android/gdb_apk -p com.tuenti.voice.example -l android/voice-client-core/obj/local/${app_abi}
* Build, deploy to phone, and start debugger in one script: tools/badit_android.py

### Run unittest
* Build debug code jni in debug mode: cd trunk/android/voice-client-core && ./build.sh
* Generate unittest apk: tools/gen_tests_apk.sh
* Install unittest : adb install -r adb install -r voice_testing/${app_abi}/${lib}/${lib}-debug.apk
* Prepare
* Run unittest:

```
# adb shell mkdir /sdcard/talk
# adb shell am start -n org.chromium.native_test/org.chromium.native_test.ChromeNativeTestActivity
# See adb logcat adb | grep libjingle
```
* Fetch unittest logs:  adb pull /sdcard/talk  talk-logs

## IOS
### Prereqs:
* OSX machine
* Download the latest xcode and command line tools.

### Running the project
* Apply the following patch to third_party/expat

```diff
Index: expat.gyp
===================================================================
--- expat.gyp   (revision 169394)
+++ expat.gyp   (working copy)
@@ -7,7 +7,7 @@
     'conditions': [
       # On Linux, we implicitly already depend on expat via fontconfig;
       # let's not pull it in twice.
-      ['os_posix == 1 and OS != "mac" and OS != "android"', {
+      ['os_posix == 1 and OS != "mac" and OS != "android" and OS != "ios"', {
         'use_system_expat%': 1,
       }, {
         'use_system_expat%': 0,
```
* Autogenerate an xcode project with gyp with the following command:

```
./build/gyp_chromium --depth=.  -DOS=ios -Dinclude_tests=0 -Denable_protobuf=0 -Denable_video=0 webrtcjingle.gyp
```
* open trunk/webrtcjingle.xcodeproj
* Modify users/hardcoded setttings, in ios/VoiceClientExample/VoiceClientDelegate.mm
* In xcode, build and deploy
* May experience issues about sse from audio_processing.gypi. If you push to an IOS device add -Dtarget_arch=arm. If emulator, the other command will probably work. 
