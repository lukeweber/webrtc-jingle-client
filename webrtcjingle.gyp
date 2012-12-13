# Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
#
# Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file in the root of the source
# tree. An additional intellectual property rights grant can be found
# in the file PATENTS.  All contributing project authors may
# be found in the AUTHORS file in the root of the source tree.
#
{
  'includes': [ 'third_party/webrtc/build/common.gypi', ],
  'variables': {
    'webrtc_android': 'android/voice-client-core/jni',
    'webrtc_client': 'client',
  },  
  'targets': [
    {
      'target_name': 'Dummy',
      'type': 'none',
    },
  ],
  'conditions': [
    ['OS=="ios"',{
      'targets': [
        {
          'target_name': 'All',
          'message': 'building ios project',
          'type': 'none',
          'dependencies': [
            'voiceclient',
          ],
          'target_name': 'voiceclient',
          'message': 'building native pieces of the voiceclient',
          'type': 'executable',
          'mac_bundle': 1,
          'sources': [
            '<(webrtc_client)/logging.h',
            '<(webrtc_client)/presenceouttask.cc',
            '<(webrtc_client)/presenceouttask.h',
            '<(webrtc_client)/presencepushtask.cc',
            '<(webrtc_client)/presencepushtask.h',
            '<(webrtc_client)/status.h',
            '<(webrtc_client)/clientsignalingthread.cc',
            '<(webrtc_client)/clientsignalingthread.h',
            '<(webrtc_client)/voiceclient.cc',
            '<(webrtc_client)/voiceclient.h',
            '<(webrtc_client)/txmppauth.cc',
            '<(webrtc_client)/txmppauth.h',
            '<(webrtc_client)/txmpppump.cc',
            '<(webrtc_client)/txmpppump.h',
            '<(webrtc_client)/txmppsocket.cc',
            '<(webrtc_client)/txmppsocket.h',
          ],
          'include_dirs': [
            '<(webrtc_client)',
            '.',
          ],
          'dependencies': [ 
            'third_party/libjingle/libjingle.gyp:libjingle_audio_only',
          ],
        },
      ],
    }],
    ['OS=="android"', {
      'targets': [
        {
          'target_name': 'All',
          'message': 'building native webrtc_jingle apk',
          'type': 'none',
          'dependencies': [
            'voiceclient',
          ],
          'actions': [
            {
              'action_name': 'webrtc_jingle',
              'inputs': [
                '<(DEPTH)/voice-client-core/AndroidManifest.xml',
                '<!@(find <(DEPTH)/voice-client-core/src/main/java/ -name "*.java")',
                'voice-client-core/jni/voiceclient_main.cc'
              ],
              'outputs': [
                # Awkwardly, we build a Debug APK even when gyp is in
                # Release mode.  I don't think it matters (e.g. we're
                # probably happy to not codesign) but naming should be
                # fixed.  The -debug name is an aspect of the android
                # SDK antfiles (e.g. ${sdk.dir}/tools/ant/build.xml)
                '<(PRODUCT_DIR)/VoiceClientActivity-debug.apk',
              ],
              'action': [
                'ant',
                '-DPRODUCT_DIR=<(ant_build_out)',
                '-DAPP_ABI=<(android_app_abi)',
                '-buildfile',
                '<(DEPTH)/voice-client-core/build.xml',
                'debug',
              ]
            }
          ],
        },
        {
          'target_name': 'voiceclient',
          'message': 'building native pieces of the voiceclient',
          'type': 'shared_library',
          'sources': [
            '<(webrtc_android)/voiceclient_main.cc',
            '<(webrtc_client)/logging.h',
            '<(webrtc_client)/presenceouttask.cc',
            '<(webrtc_client)/presenceouttask.h',
            '<(webrtc_client)/presencepushtask.cc',
            '<(webrtc_client)/presencepushtask.h',
            '<(webrtc_client)/status.h',
            '<(webrtc_client)/clientsignalingthread.cc',
            '<(webrtc_client)/clientsignalingthread.h',
            '<(webrtc_client)/helpers.cc',
            '<(webrtc_client)/helpers.h',
            '<(webrtc_client)/voiceclient.cc',
            '<(webrtc_client)/voiceclient.h',
            '<(webrtc_client)/txmppauth.cc',
            '<(webrtc_client)/txmppauth.h',
            '<(webrtc_client)/txmpppump.cc',
            '<(webrtc_client)/txmpppump.h',
            '<(webrtc_client)/txmppsocket.cc',
            '<(webrtc_client)/txmppsocket.h',
          ],
          'include_dirs': [
            '<(webrtc_android)',
            '.',
          ],
          'dependencies': [ 
            'third_party/libjingle/libjingle.gyp:libjingle_audio_only',
            #'src/voiceclient.gypi:voiceclient',
            #'../../base/base.gyp:base',
            #'../../base/base.gyp:test_support_base',
            #'../gtest.gyp:gtest',
            #'voice_client_jni_headers',
          ],
          'outputs': [
            '<(PRODUCT_DIR)/<(android_app_abi)/libvoiceclient.so',
          ],
          #'link_settings': {
          #  'libraries': [
             # '-ljingle_audio_only',
             # '-llibjingle',
             # '-llibjingle_p2p',
            # Manually link the libgcc.a that the cross compiler uses.
            #'<!($CC -print-libgcc-file-name)',
            #'-lc',
            #'-ldl',
            #'-lstdc++',
            #'-lm',
          #  ],
          #},
          'direct_dependent_settings': {
            'ldflags!': [
              # JNI_OnLoad is implemented in a .a and we need to
              # re-export in the .so.
              #'-Wl,--exclude-libs=ALL',
              '-Wl',
            ],
          },
        },
        {
          'target_name': 'voice_client_jni_headers',
          'type': 'none',
          'actions': [
            {
              'action_name': 'generate_jni_headers',
              'inputs': [
                #'<(DEPTH)/base/android/jni_generator/jni_generator.py',
                'android/voice-client-core/src/main/java/com/tuenti/voice/core/VoiceClient.java',
              ],
              'outputs': [
                '<(DEPTH)/voice-client-core/jni/',
                #'com_tuenti_voice_VoiceClient.h',
              ],
              'action': [
                'python',
                '<(DEPTH)/base/android/jni_generator/jni_generator.py',
                '--input_file',
                '<@(_inputs)',
                '--output_dir',
                '<@(_outputs)',
              ],
            }
          ],
          # So generated jni headers can be found by targets that
          # depend on this.
          'direct_dependent_settings': {
            'include_dirs': [
              '<(webrtc_jingle)',
              '.',
            ],
          },
        },
      ],
    }]
  ],
}
