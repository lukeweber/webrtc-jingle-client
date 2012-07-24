# Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
#
# Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file in the root of the source
# tree. An additional intellectual property rights grant can be found
# in the file PATENTS.  All contributing project authors may
# be found in the AUTHORS file in the root of the source tree.

{
  'includes': [ 'third_party/webrtc/build/common.gypi', ],
  'variables': {
    'webrtc_jingle': 'src/jni',
  },  

  'targets': [
    {
      'target_name': 'webrtc_jingle',
      'type': 'executable',
      'sources': [
        '<(webrtc_jingle)/voiceclient_main.cc',
        '<(webrtc_jingle)/tuenti/logging.h',
        '<(webrtc_jingle)/tuenti/presenceouttask.cc',
        '<(webrtc_jingle)/tuenti/presenceouttask.h',
        '<(webrtc_jingle)/tuenti/presencepushtask.cc',
        '<(webrtc_jingle)/tuenti/presencepushtask.h',
        '<(webrtc_jingle)/tuenti/status.h',
        '<(webrtc_jingle)/tuenti/threadpriorityhandler.cc',
        '<(webrtc_jingle)/tuenti/threadpriorityhandler.h',
        '<(webrtc_jingle)/tuenti/voiceclient.cc',
        '<(webrtc_jingle)/tuenti/voiceclient.h',
      ],
      'include_dirs': [
        '<(webrtc_jingle)',
      ],
      'dependencies': [
        'third_party/libjingle/libjingle.gyp:libjingle_audio_only',
      ],
      'defines': [
        'EXPAT_RELATIVE_PATH',
      ]
    },
  ],
}
