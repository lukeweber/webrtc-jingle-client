webrtc-jingle for android 
=============
(libjingle signaling + webrtc voice engine)

About:
---------------------
* Working demo of libjingle and webrtc voice backend, with minimal UI. 
* Caveat, if you use this app to talk to gmail for example, it will work as is. If you want to test the app calling another person running this app, you'll need your own STUN server. To get this working you need to build libjingle per their instructions and use the stun server binary. Then set your stun server in config.mk instead of google's.
* Clean JAVA/JNI layer to call into and receive events from the native app.
* Out demo apk with support for arm/armv7 comes to 6.4 mb, approx a third when zipped in an apk. Without compiler optimizations it's a bit less.
* Based on libjingle trunk and webrtc trunk updated on regular intervals.
* Stability improvements needed for the c layer. Works decently, but almost certainly race cases that need to be addressed.

Prereqs:
---------------------
* Get yourself an [android NDK r8](http://developer.android.com/sdk/ndk/index.html). I have tested r8b and it doesn't work. Please use r8. Just copy the link on the page, and modify to be r8.
* Get an [Android SDK](http://developer.android.com/sdk/installing.html) installed as well.
* Install [eclipse](http://www.eclipse.org/downloads/)
* Install the [Android SDK plugin](http://developer.android.com/sdk/eclipse-adt.html) for eclipse if you didn't in the sdk steps.

Get started with the code:
----------------------
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
* Set your username, pass and connection setttings in app/jni/com/tuenti/voice/ui/VoiceClientActivity.java towards the top.
* CommandLine Quickstart: cd trunk/src && ./badit
* To make archives, release builds as well as working in eclipse
* run "ndk-build" in trunk/src
* Import src directory as an android project with existing sources into eclipse and build. Note that emulator may not work. 
* Build project and you should be ready to go and make calls between your browser and the app.
* As well the expiremental build options are badit in trunk directory which wraps some of the makefile logic, to utilize gyp for build, ie make webrtc_jingle. This isn't really recommeneded, but if you'd like to push forward tests, this is the area where work will happen.

Todo/Issues:
--------------------------
* 84f2385 -> Removed sound clip from webrtcvoiceengine.cc. This is how I would play a ringing tone, have to see how this should actually work.
* See Tickets
* Build tests automatically using chromium style apk test runner. (native_test.gyp)
 * Cross compile for mac/android pushed back into chromium at some point via gyp changes.
* Working on a stable TURN server option as libjingle is not compliant. 
* Modify project to have a build option so it's truly an sdk that can be used in other apps, with no UI.