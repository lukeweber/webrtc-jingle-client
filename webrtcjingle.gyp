# Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
#
# Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file in the root of the source
# tree. An additional intellectual property rights grant can be found
# in the file PATENTS.  All contributing project authors may
# be found in the AUTHORS file in the root of the source tree.
#
{
  'includes': [ 'third_party/webrtc/build/common.gypi'],
  'variables': {
    'webrtc_ios': 'ios',
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
        },
        {
          'defines': [
            'IOS',
            'LOGGING=1',
            'IOS_XMPP_FRAMEWORK=1',
          ],
          'target_name': 'voiceclient',
          'message': 'building native pieces of the voiceclient',
          'type': 'executable',
          'mac_bundle': 1,
          'sources': [
            '<(webrtc_ios)/VoiceClientExample/main.m',
            '<(webrtc_ios)/VoiceClientExample/AppDelegate.h',
            '<(webrtc_ios)/VoiceClientExample/AppDelegate.mm',
            '<(webrtc_ios)/VoiceClientExample/ViewController.h',
            '<(webrtc_ios)/VoiceClientExample/ViewController.mm',
            '<(webrtc_ios)/VoiceClientExample/VoiceClientDelegate.h',
            '<(webrtc_ios)/VoiceClientExample/VoiceClientDelegate.mm',
            '<(webrtc_ios)/VoiceClientExample/IOSXmppClient.h',
            '<(webrtc_ios)/VoiceClientExample/IOSXmppClient.mm',
            '<(webrtc_ios)/VoiceClientExample/main.m',
            '<(webrtc_ios)/VoiceClientExample/XmppClientDelegate.h',
            '<(webrtc_ios)/VoiceClientExample/XmppClientDelegate.mm',
            '<(webrtc_client)/client_defines.h',
            '<(webrtc_client)/logging.h',
            '<(webrtc_client)/xmppmessage.h',
            '<(webrtc_client)/keepalivetask.cc',
            '<(webrtc_client)/keepalivetask.h',
            '<(webrtc_client)/receivemessagetask.cc',
            '<(webrtc_client)/receivemessagetask.h',
            '<(webrtc_client)/sendmessagetask.cc',
            '<(webrtc_client)/sendmessagetask.h',
            '<(webrtc_client)/presenceouttask.cc',
            '<(webrtc_client)/presenceouttask.h',
            '<(webrtc_client)/rosterhandler.h',
            '<(webrtc_client)/rosterhandler.cc',
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
            '<(webrtc_client)/xmpplog.cc',
            '<(webrtc_client)/xmpplog.h',
            '$(SDKROOT)/usr/lib/libresolv.dylib',
            '$(SDKROOT)/usr/lib/libxml2.dylib',
          ],
          'mac_bundle_resources': [
            'third_party/xmppframework/Extensions/Roster/CoreDataStorage/XMPPRoster.xcdatamodel',
            'third_party/xmppframework/Extensions/XEP-0045/CoreDataStorage/XMPPRoom.xcdatamodeld/XMPPRoom.xcdatamodel',
            'third_party/xmppframework/Extensions/XEP-0045/HybridStorage/XMPPRoomHybrid.xcdatamodeld/XMPPRoomHybrid.xcdatamodel',
            'third_party/xmppframework/Extensions/XEP-0054/CoreDataStorage/XMPPvCard.xcdatamodeld/XMPPvCard.xcdatamodel',
            'third_party/xmppframework/Extensions/XEP-0115/CoreDataStorage/XMPPCapabilities.xcdatamodel',
            'third_party/xmppframework/Extensions/XEP-0136/CoreDataStorage/XMPPMessageArchiving.xcdatamodeld/XMPPMessageArchiving.xcdatamodel',
            '<(webrtc_ios)/VoiceClientExample/en.lproj/InfoPlist.strings',
            '<(webrtc_ios)/VoiceClientExample/MainStoryboard_iPad.storyboard',
            '<(webrtc_ios)/VoiceClientExample/MainStoryboard_iPhone.storyboard',
            '<(webrtc_ios)/VoiceClientExample/Default.png',
            '<(webrtc_ios)/VoiceClientExample/Default@2x.png',
            '<(webrtc_ios)/VoiceClientExample/Default-568h@2x.png',
          ],
          'include_dirs': [
            '<(webrtc_client)',
            '<(webrtc_ios)',
            'third_party/',
            'third_party/xmppframework/Core',
            'third_party/xmppframework/Authentication',
            'third_party/xmppframework/Categories',
            'third_party/xmppframework/Utilities',
            'third_party/xmppframework/Vendor/CocoaAsyncSocket',
            'third_party/xmppframework/Vendor/CocoaLumberjack',
            'third_party/xmppframework/Vendor/KissXml',
            'third_party/xmppframework/Extensions/Reconnect',
            'third_party/xmppframework/Extensions/Roster',
            'third_party/xmppframework/Extensions/Roster/CoreDataStorage',
            'third_party/xmppframework/Extensions/CoreDataStorage',
            'third_party/xmppframework/Extensions/XEP-0054',
            'third_party/xmppframework/Extensions/XEP-0153',
            'third_party/xmppframework/Extensions/XEP-0054/CoreDataStorage',
            'third_party/xmppframework/Extensions/XEP-0115',
            'third_party/xmppframework/Extensions/XEP-0115/CoreDataStorage',
            'third_party/xmppframework/Extensions/XEP-0045',
            'third_party/xmppframework/Extensions/XEP-0045/CoreDataStorage',
            "$(SDKROOT)/usr/include/libxml2",
            '.',
          ],
          'xcode_settings': {
            'OTHER_LDFLAGS': [
                '-framework UIKit',
                '-framework CoreGraphics',
                '-framework CoreLocation',
                '-framework Foundation',
                '-framework CoreData',
                '-framework CFNetwork',
                '-framework Security',
                '-lresolv',
                '-lxml2',
            ],
            'CLANG_ENABLE_OBJC_ARC': 'YES',
            'INFOPLIST_FILE': '<(webrtc_ios)/VoiceClientExample/VoiceClientExample-Info.plist',
          },
          'dependencies': [
            'third_party/xmppframework/xmppframework.gyp:All',
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
            '<(webrtc_client)/xmpplog.cc',
            '<(webrtc_client)/xmpplog.h',
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
