Placeholder for the iphone build.

Starting points
* Apply the following patches
* Autogenerate an xcode project with
./build/gyp_chromium --depth=.  -DOS=ios -Dinclude_tests=0 -Denable_protobuf=0 -Denable_video=0 webrtcjingle.gyp

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
@@ -562,8 +564,16 @@
         'openssl/crypto/x509v3/v3err.c',
       ],
       'conditions': [
-        ['os_posix==1 and OS!="android"', {
+        ['OS=="ios"', {
+          'defines!': [
+            'TERMIO',
+          ],
           'defines': [
+            'TERMIOS',
+          ],
+        }],
+        ['os_posix==1 and OS!="android" and OS!="ios"', {
+          'defines': [
             # ENGINESDIR must be defined if OPENSSLDIR is.
             'ENGINESDIR="/dev/null"',
             # Set to ubuntu default path for convenience. If necessary, override
@@ -580,12 +590,17 @@
             ],
           },
         }],
+        ['OS=="ios"', {
+          'variables': {
+            'openssl_config_path': 'config/ios',
+          }
+        }],
         ['OS=="android"', {
           'variables': {
             'openssl_config_path': 'config/android',
           },
         }],
-        ['target_arch == "arm"', {
+        ['target_arch == "arm" and OS!="ios"', {
           'sources!': [
             # Use assembly version of this source file for ARM.
             'openssl/crypto/aes/aes_core.c',
@@ -600,7 +615,7 @@
             'openssl/crypto/sha/asm/sha512-armv4.S',
           ],
         }],
-        ['target_arch == "ia32"', {
+        ['target_arch == "ia32" and OS!= "ios"', {
           'sources!': [
             # Use assembly version of this source file for ARM.
             'openssl/crypto/aes/aes_core.c',

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
