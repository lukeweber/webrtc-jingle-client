Placeholder for the iphone build.

Starting points.

* Main client entry point/wrapper is client/voiceclient.cc. It could use a refactor to really separate Android client from java, but for now, ifdefs will have to do.
* Poke around build/ios/.. see what's reusable, try to put gyp into OS=ios mode with one of these scripts.
* Create an IOS case for code in trunk/webrtcjingle.gyp/possibly create a new target in trunk/iphone/webrtc-iphone.gyp
* Set IOS specific flags in build, namely -DIPHONE, maybe more.
* GYP Should auto generate a huge xcode project, but might be for xcode from OSX 10.6
 * Might have better luck with a new version of tools/gyp.
 * I've patched trunk of gyp with my code, but haven't pushed/tested properly. If you need an update, ping me.
