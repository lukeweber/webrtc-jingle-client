Work in progress:
* Dependencies in place for build
* webrtcjingle.gyp needs to actually gather all dependencies into a single large static voiceclient.a(not yet working)
* Build voiceclient.a into apk via build scripts.
* Build tests automatically using chromium style apk test runner. (native_test.gyp)


```
# mkdir webrtcjingleproject
# cd webrtcjingleproject
# gclient config https://github.com/lukeweber/webrtc-jingle-client.git --name trunk
# gclient sync

# cd trunk
# export CHROME_SRC=`pwd`
# source build/android/envsetup.sh
# gclient runhooks
# make webrtc_jingle
```
