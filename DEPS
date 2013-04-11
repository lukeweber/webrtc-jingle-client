#This comes directly from gclient revinfo and should ONLY look like this in the stable branch 
deps = {
  "trunk/build": "https://github.com/lukeweber/chromium-trunk-build-override.git@f02288",
  "trunk/testing": "http://src.chromium.org/svn/trunk/src/testing@182149",
  "trunk/testing/gmock": "http://googlemock.googlecode.com/svn/trunk@405",
  "trunk/third_party/ashmem": "http://src.chromium.org/svn/trunk/src/third_party/ashmem@182149",
  "trunk/third_party/expat": "http://src.chromium.org/svn/trunk/src/third_party/expat@182149",
  "trunk/third_party/google-gflags/src": "http://google-gflags.googlecode.com/svn/trunk/src@45",
  "trunk/third_party/gtest": "https://github.com/lukeweber/googletest.git@361c8f",
  "trunk/third_party/icu": "http://git.chromium.org/chromium/deps/icu46.git@53ac6db57d9d09e43079f54feec59b735ed23670",
  "trunk/third_party/libevent": "http://src.chromium.org/svn/trunk/src/third_party/libevent@182149",
  "trunk/third_party/libjingle": "https://github.com/lukeweber/libjingle-override.git@34ff9f",
  "trunk/third_party/libjpeg": "http://src.chromium.org/svn/trunk/src/third_party/libjpeg@182149",
  "trunk/third_party/libjpeg_turbo": "http://git.chromium.org/chromium/deps/libjpeg_turbo.git@82ce8a6d4ebe12a177c0c3597192f2b4f09e81c3",
  "trunk/third_party/libsrtp/source": "http://git.chromium.org/chromium/deps/libsrtp.git@362c71e8d0dc205a4ad9f4709d42c25864ac872a",
  "trunk/third_party/libvpx/source/libvpx": "http://git.chromium.org/webm/libvpx.git@7a09f6b8",
  "trunk/third_party/libyuv": "http://git.chromium.org/external/libyuv.git@a9c9242a557c0202454733bab66521bff2e35fc9",
  "trunk/third_party/mach_override": "http://src.chromium.org/svn/trunk/src/third_party/mach_override@182149",
  "trunk/third_party/modp_b64": "http://src.chromium.org/svn/trunk/src/third_party/modp_b64@182149",
  "trunk/third_party/openssl": "https://github.com/lukeweber/openssl-override.git@5c21728",
  "trunk/third_party/protobuf": "http://src.chromium.org/svn/trunk/src/third_party/protobuf@182149",
  "trunk/base":"https://github.com/lukeweber/chromium-trunk-base-override.git@455807",
  "trunk/third_party/webrtc": "https://github.com/lukeweber/webrtc-src-override.git@60c3258",
  "trunk/third_party/yasm": "http://src.chromium.org/svn/trunk/src/third_party/yasm@182149",
  "trunk/third_party/yasm/source/patched-yasm": "http://git.chromium.org/chromium/deps/yasm/patched-yasm.git@f164a228f51c17ffc3ed69516a7dc6abdf2d2c8e",
  "trunk/tools/clang": "http://src.chromium.org/svn/trunk/src/tools/clang@182149",
  "trunk/tools/gyp": "https://github.com/lukeweber/gyp-override.git@fac7cd",
  "trunk/tools/python": "http://src.chromium.org/svn/trunk/src/tools/python@182149",
  "trunk/tools/valgrind": "http://src.chromium.org/svn/trunk/src/tools/valgrind@182149",
  "trunk/tools/win/supalink": "http://src.chromium.org/svn/trunk/src/tools/win/supalink@182149",
}
hooks = [
  {
    # Create a supplement.gypi file under trunk/src.  This file will be picked
    # up by gyp and used to enable the standalone build.
    "pattern": ".",
    "action": ["python", "trunk/tools/create_supplement_gypi.py", "trunk/client/supplement.gypi"],
  },
  {
    # A change to a .gyp, .gypi, or to GYP itself should run the generator.
    "pattern": ".",
    "action": ["python", "trunk/build/gyp_chromium", "--depth=trunk", "trunk/webrtcjingle.gyp"],
  },
]
