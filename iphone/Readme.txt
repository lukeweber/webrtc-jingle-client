Placeholder for the iphone build.

Starting points
* Autogenerate an xcode project with ./build/gyp_chromium --depth=.  -DOS=ios -Dtarget_arch=arm -Dinclude_tests=0 -Denable_protobuf=0 -Denable_video=0 webrtcjingle.gyp
* May need to install http://www.iosopendev.com/ to get around compiling shared libs with iphone sdk.
