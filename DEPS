vars = {
  # Use this googlecode_url variable only if there is an internal mirror for it.
  # If you do not know, use the full path while defining your new deps entry.
  "googlecode_url": "http://%s.googlecode.com/svn",
  "chromium_trunk" : "http://src.chromium.org/svn/trunk",
  "github_luke_url" : "https://www.github.com/lukeweber",
  "chromium_revision": "140240",

  # External resources like video and audio files used for testing purposes.
  # Downloaded on demand when needed.
  "webrtc_resources_revision": "9",
}

# NOTE: Prefer revision numbers to tags for svn deps. Use http rather than
# https; the latter can cause problems for users behind proxies.
deps = {

  "trunk/testing":
    Var("chromium_trunk") + "/src/testing@" + Var("chromium_revision"),
  
  "trunk/testing/gmock":
    (Var("googlecode_url") % "googlemock") + "/trunk@405",

  "trunk/testing/gtest":
    (Var("googlecode_url") % "googletest") + "/trunk@617",

  "trunk/third_party/expat":
    Var("chromium_trunk") + "/src/third_party/expat@" + Var("chromium_revision"),

  "trunk/third_party/google-gflags/src":
    (Var("googlecode_url") % "google-gflags") + "/trunk/src@45",

  "trunk/third_party/libjpeg":
    Var("chromium_trunk") + "/src/third_party/libjpeg@" + Var("chromium_revision"),
  
  "trunk/third_party/libjpeg_turbo":
    Var("chromium_trunk") + "/deps/third_party/libjpeg_turbo@147428",

  "trunk/third_party/libvpx/source/libvpx":
    "http://git.chromium.org/webm/libvpx.git@cab6ac16",

  "trunk/third_party/libyuv":
    (Var("googlecode_url") % "libyuv") + "/trunk@255",

  "trunk/third_party/protobuf":
    Var("chromium_trunk") + "/src/third_party/protobuf@" + Var("chromium_revision"),

  "trunk/third_party/yasm":
    Var("chromium_trunk") + "/src/third_party/yasm@" + Var("chromium_revision"),

  "trunk/third_party/yasm/source/patched-yasm":
    Var("chromium_trunk") + "/deps/third_party/yasm/patched-yasm@134927",

  "trunk/tools/clang":
    Var("chromium_trunk") + "/src/tools/clang@" + Var("chromium_revision"),

  "trunk/tools/python":
    Var("chromium_trunk") + "/src/tools/python@" + Var("chromium_revision"),

  "trunk/tools/valgrind":
    Var("chromium_trunk") + "/src/tools/valgrind@" + Var("chromium_revision"),

  # Needed by build/common.gypi.
  "trunk/tools/win/supalink":
    Var("chromium_trunk") + "/src/tools/win/supalink@" + Var("chromium_revision"),

  "trunk/third_party/jsoncpp/":
    Var("chromium_trunk") + "/src/third_party/jsoncpp@" + Var("chromium_revision"),

  "trunk/third_party/jsoncpp/source":
    "http://jsoncpp.svn.sourceforge.net/svnroot/jsoncpp/trunk/jsoncpp@248",

  "trunk/third_party/expat":
    Var("chromium_trunk") + "/src/third_party/expat@" + Var("chromium_revision"),

  "trunk/third_party/libsrtp":
    Var("chromium_trunk") + "/deps/third_party/libsrtp@123853",

  "trunk/third_party/libjingle/":
    Var("chromium_trunk") + "/src/third_party/libjingle@" + Var("chromium_revision"),
  
  "trunk/third_party/libjingle/source":
    "https://github.com/lukeweber/libjingle-override.git",
  
  "trunk/build":
    "https://github.com/lukeweber/chromium-trunk-build-override.git",
  
  "trunk/third_party/webrtc":
    "https://github.com/lukeweber/webrtc-src-override.git",
  
  "trunk/tools/gyp":
    "https://github.com/lukeweber/gyp-override.git",
}


hooks = [

  {
    # Create a supplement.gypi file under trunk/src.  This file will be picked
    # up by gyp and used to enable the standalone build.
    "pattern": ".",
    "action": ["python", "trunk/tools/create_supplement_gypi.py", "trunk/src/supplement.gypi"],
  },
  {
    # A change to a .gyp, .gypi, or to GYP itself should run the generator.
    "pattern": ".",
    "action": ["python", "trunk/build/gyp_chromium", "--depth=trunk", "trunk/webrtcjingle.gyp"],
  },
]
#  "trunk/third_party/yasm/source/patched-yasm":
#    From("trunk/chromium_deps", "src/third_party/yasm/source/patched-yasm"),
# libjingle deps.
#  "trunk/third_party/libjpeg_turbo":
#    From("trunk/chromium_deps", "src/third_party/libjpeg_turbo"),

