Placeholder for the iphone build.

Starting points
* Apply the following patches
* Autogenerate an xcode project with ./build/gyp_chromium --depth=.  -DOS=ios -Dtarget_arch=arm -Dinclude_tests=0 -Denable_protobuf=0 -Denable_video=0 webrtcjingle.gyp

===Patches===
.../webrtcjingleproject/trunk/third_party/openssl$ svn diff
Index: openssl.gyp
===================================================================
--- openssl.gyp (revision 169329)
+++ openssl.gyp (working copy)
@@ -13,6 +13,8 @@
         'PURIFY',
         'TERMIO',
         '_REENTRANT',
+        'OPENSSL_NO_HW',
+        'OPENSSL_NO_GOST',
         # We do not use TLS over UDP on Chromium so far.
         'OPENSSL_NO_DTLS1',
       ],
@@ -562,6 +564,14 @@
         'openssl/crypto/x509v3/v3err.c',
       ],
       'conditions': [
+        ['OS=="ios"', {
+          'defines!': [
+            'TERMIO',
+          ],
+          'defines': [
+            'TERMIOS',
+          ],
+        }],
         ['os_posix==1 and OS!="android"', {
           'defines': [
             # ENGINESDIR must be defined if OPENSSLDIR is.
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
