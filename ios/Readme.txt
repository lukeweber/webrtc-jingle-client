IPhone build working in a basic state with a login, logout and call button.

Starting points
* Apply the following patches
* Autogenerate an xcode project with
./build/gyp_chromium --depth=.  -DOS=ios -Dinclude_tests=0 -Denable_protobuf=0 -Denable_video=0 webrtcjingle.gyp
* open webrtcjingle.xcodeproj 
* Modify usernames/servers, etc in ios/VoiceClientExample/VoiceClientDelegate.mm
* Build and deploy
* May experience issues about sse from audio_processing.gypi. If you push to an IOS device add -Dtarget_arch=arm. If emulator, the other command will probably work. 

===Patches===
.../webrtcjingleproject/trunk/third_party/expat$ svn diff
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
