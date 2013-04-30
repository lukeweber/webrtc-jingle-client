vars = {
  # Use this googlecode_url variable only if there is an internal mirror for it.
  # If you do not know, use the full path while defining your new deps entry.
  "googlecode_url": "http://%s.googlecode.com/svn",
  "chromium_trunk" : "http://src.chromium.org/svn/trunk",
  "chromium_git_url" : "http://git.chromium.org",
  "github_luke_url" : "https://www.github.com/lukeweber",
  "chromium_revision": "182149",

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

  "trunk/third_party/gtest":
    "https://github.com/lukeweber/googletest.git@dedaf70c278",

  "trunk/third_party/expat":
    Var("chromium_trunk") + "/src/third_party/expat@" + Var("chromium_revision"),

  "trunk/third_party/google-gflags/src":
    (Var("googlecode_url") % "google-gflags") + "/trunk/src@45",

  "trunk/third_party/libjpeg":
    Var("chromium_trunk") + "/src/third_party/libjpeg@" + Var("chromium_revision"),

  "trunk/third_party/mach_override":
    Var("chromium_trunk") + "/src/third_party/mach_override@" + Var("chromium_revision"),

  "trunk/third_party/libjpeg_turbo":
    Var("chromium_git_url") + "/chromium/deps/libjpeg_turbo.git@82ce8a6d4ebe12a177c0c3597192f2b4f09e81c3",

  "trunk/third_party/libyuv":
    Var("chromium_git_url") + "/external/libyuv.git@a9c9242a557c0202454733bab66521bff2e35fc9",

  "trunk/third_party/protobuf":
    Var("chromium_trunk") + "/src/third_party/protobuf@" + Var("chromium_revision"),

  "trunk/third_party/yasm":
    Var("chromium_trunk") + "/src/third_party/yasm@" + Var("chromium_revision"),

  "trunk/third_party/yasm/source/patched-yasm":
    Var("chromium_git_url") + "/chromium/deps/yasm/patched-yasm.git@f164a228f51c17ffc3ed69516a7dc6abdf2d2c8e",

  "trunk/tools/clang":
    Var("chromium_trunk") + "/src/tools/clang@" + Var("chromium_revision"),

  "trunk/tools/python":
    Var("chromium_trunk") + "/src/tools/python@" + Var("chromium_revision"),

  "trunk/tools/valgrind":
    Var("chromium_trunk") + "/src/tools/valgrind@" + Var("chromium_revision"),

  # Needed by build/common.gypi.
  "trunk/tools/win/supalink":
    Var("chromium_trunk") + "/src/tools/win/supalink@" + Var("chromium_revision"),

  "trunk/third_party/expat":
    Var("chromium_trunk") + "/src/third_party/expat@" + Var("chromium_revision"),

  "trunk/third_party/libsrtp/source":
    Var("chromium_git_url") + "/chromium/deps/libsrtp.git@362c71e8d0dc205a4ad9f4709d42c25864ac872a",

  "trunk/third_party/libjingle":
    "https://github.com/lukeweber/libjingle-override.git",

  "trunk/build":
    "https://github.com/lukeweber/chromium-trunk-build-override.git",

  "trunk/base":
    "https://github.com/lukeweber/chromium-trunk-base-override.git",

  "trunk/third_party/webrtc":
    "https://github.com/lukeweber/webrtc-src-override.git",

  "trunk/tools/gyp":
    "https://github.com/lukeweber/gyp-override.git",

  "trunk/third_party/modp_b64":
    Var("chromium_trunk") + "/src/third_party/modp_b64@" + Var("chromium_revision"),

  "trunk/third_party/ashmem":
    Var("chromium_trunk") + "/src/third_party/ashmem@" + Var("chromium_revision"),

  "trunk/third_party/libevent":
    Var("chromium_trunk") + "/src/third_party/libevent@" + Var("chromium_revision"),

  "trunk/third_party/icu":
    Var("chromium_git_url") + "/chromium/deps/icu46.git@53ac6db57d9d09e43079f54feec59b735ed23670",

  "trunk/third_party/openssl":
    "https://github.com/lukeweber/openssl-override.git",

  "trunk/third_party/libvpx/source/libvpx":
    "http://git.chromium.org/webm/libvpx.git@7a09f6b8",

  "trunk/third_party/opus/":
	Var("chromium_trunk") + "/deps/third_party/opus@185324",
}

deps_os = {
    "ios" : {
          "trunk/third_party/xmppframework":
            "https://github.com/lukeweber/XMPPFramework.git",
    }
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
