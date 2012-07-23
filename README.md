Hot mess of code at the moment. Idea is to get everything that is overridden under separate repos to facilitate syncing. As well, using gyp will hopefully allow us to use apk unit testing of chromium. Pending first steps are to create a new libjingle.gyp based on respective Android.mk file for libjilngle. Next is to then build our app(.so), based in webtcjingle.gyp. Lastly we should be able to autogenerate .h jni files from java files, and apk files for our app directly through make. After all this is working, unit tests.

```
# mkdir webrtcjinleproject
# cd webrtcjingleproject
# gclient config https://github.com/lukeweber/webrtc-jingle-client.git --name trunk
# gclient sync

# cd trunk
# export CHROME_SRC=`pwd`
# source build/android/envsetup.sh
# gclient runhooks
# make
```
