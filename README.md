Deprecated
==========
This project began when webrtc was just getting it's start and there were many bugs and sdp implementation wasn't even code complete, and drivers for the different phones weren't working and the build for ios/android was broken from one version to another, and the code wasn't even compatible at times with native builds for hundreds of revisions. I built this project out of a necessity to have a more stable project, that was cut down and had the right build variables for the native project, i.e. video didn't work, so I removed it. Over time, many issues have been sovled in the core, and I've moved on to other projects after launching a voip solution on this codebase quite a while ago.

I haven't dropped in on webrtc in quite a while now, but I do know that you can use wrappers to convert jingle to sdp and that they do work. If you're set on this solution, instead of a native converter and using webrtc, I suggest you update all the linked repos in this project and because they're probably quite out of date at this point.

One of the java engineers who worked on this codebase went on to switch to using the webrtc api directly, and using a converter for the xmpp stack. https://github.com/tuenti/sdp-to-jingle-java

webrtc-jingle for android
=============
(libjingle signaling + webrtc voice engine) 

Discussion: [webrtc-jingle](https://groups.google.com/forum/?fromgroups#!forum/webrtc-jingle)

## About

* Working example android and ios apps of libjingle and webrtc voice backend.
* Based on libjingle trunk and webrtc trunk updated on regular intervals.
* Added improvements for stability and missing pieces for mobile implementation.
* Can make calls between two phones, or between gmail and a phone.
* Happy for any help, please see tickets, and send a pull request.

## Getting the code

* Download and install [depot_tools](http://dev.chromium.org/developers/how-tos/install-depot-tools)

```
# IMPORTANT:- gclient command from [depot_tools](http://dev.chromium.org/developers/how-tos/install-depot-tools) 
# has to be used to checkout the project else the third_party dependencies don't get pulled 
# 

# mkdir webrtcjingleproject
# cd webrtcjingleproject
# gclient config https://github.com/lukeweber/webrtc-jingle-client.git --name trunk
# gclient sync

or for an older stable build, take the head of the stable branch revision.
# gclient sync --revision PUT_STABLE_HEAD_REV_HERE
```

## Android

### Prereqs
* [android NDK r8e](http://developer.android.com/sdk/ndk/index.html)
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

* Build, deploy to phone, and start debugger in one script: tools/badit_android.py
* Build the apks: cd trunk/android && mvn install
* To run a debugger: build/android/gdb_apk -p com.tuenti.voice.example -s VoiceClientService -l android/voice-client-native/obj/${build_profile}/local/${app_abi}
* For debugging the ndk compile outside of maven, I sometimes prefer and use a light shell wrapper instead of maven for building only the c++ project. trunk/android/voice-client-native && ./build.sh

### Run unittests
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

### Build for Video(Experimental)
* NO UI YET, need more changes in libjingle core to make this work.
* DEPS build on linux and mac.
* Need to wire the java code in example app, using third_party/webrtc/video_engine/test/android/src/org/webrtc/videoengineapp/WebRTCDemo.java as a template.
* webrtcvideoengine.cc will certainly need changes, as will VideoRenderer to enable passing a java ref down to webrtc, contact me if you want to give this a shot.

```
# cd android/voice-client-core/
# ln -s [insert_full_path_here]/trunk/third_party/libvpx/source/libvpx jni/libvpx
# cd third_party/libvpx/source 
# git pull https://gerrit.chromium.org/gerrit/webm/libvpx refs/changes/99/41299/1
# libvpx/configure --target=armv7-android-gcc --disable-examples --sdk-path=$ANDROID_NDK_ROOT --enable-error-concealment --enable-realtime-only --disable-vp9 --enable-pic
# Open jni/libvpx/build/make/Android.mk => change BUILD_SHARED_LIBRARY to BUILD_STATIC_LIBRARY
# 
```

## IOS
### Prereqs
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
* Change your .gclient file in trunk/../.gclient

```diff
--- .gclient
+++ .gclient
@@ -8,3 +8,4 @@
     "safesync_url": "",
   },
 ]
+target_os = ['ios']
```
* Run gclient sync again to fetch xmppframework.
* Autogenerate an xcode project with gyp with the following command:

```
./build/gyp_chromium --depth=.  -DOS=ios -Dinclude_tests=0 -Denable_protobuf=0 -Denable_video=0 webrtcjingle.gyp
```
* open trunk/webrtcjingle.xcodeproj
* Modify myJID, and myPassword in AppDelegate.mm.
* Modify user you wish to call in ios/VoiceClientExample/ViewController.mm => [appDelegate call:@"user@gmail.com"];
* If using an xmpp server make sure to change the flag isGtalk in login in VoiceClientDelegate.mm.
* In xcode, build and deploy
* May experience issues about sse from audio_processing.gypi. If you push to an IOS device add -Dtarget_arch=arm. If emulator, the other command will probably work. 

## Opus
* Opus is currently alpha in implementation, is hard set to 48kHz in webrtc. Android mic is set to 16kHz, meaning you'll probably upsample/downsample all audio by 3x.
* Add "WEBRTC_BUILD_WITH_OPUS := true" to android/voice-client-core/jni/Android.mk
* Modify offer kCodecPrefs in third_party/libjingle/talk/media/webrtc/webrtcvoiceengine.cc to include OPUS and exclude ISAC. 

## Windows
* Native code won't build on a windows machine.
* VM with Ubuntu 64bit Linux
* Recommended disk of at least 3GB. Current build cache is approx, 1.5GB.
* 64bit Android Linux NDK required.
* Video from jreyes https://www.youtube.com/watch?v=f0NU-E8l_qQ
