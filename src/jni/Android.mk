# Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
#
# Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file in the root of the source
# tree. An additional intellectual property rights grant can be found
# in the file PATENTS.  All contributing project authors may
# be found in the AUTHORS file in the root of the source tree.

ifdef NDK_ROOT

MY_WEBRTC_ROOT_PATH := $(call my-dir)

MY_WEBRTC_SRC_PATH := ../../trunk/

include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/common_audio/resampler/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/common_audio/signal_processing/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/common_audio/vad/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_coding/neteq/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_coding/codecs/cng/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_coding/codecs/g711/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_coding/codecs/g722/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_coding/codecs/pcm16b/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_coding/codecs/ilbc/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_coding/codecs/iSAC/fix/source/Android.mk
#include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_coding/codecs/iSAC/main/source/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_coding/main/source/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/utility/source/Android.mk
# include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/remote_bitrate_estimator/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_conference_mixer/source/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_device/main/source/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_processing/aec/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_processing/aecm/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_processing/agc/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_processing/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_processing/ns/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/audio_processing/utility/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/media_file/source/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/rtp_rtcp/source/Android.mk
#include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/modules/udp_transport/source/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/system_wrappers/source/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/src/voice_engine/main/source/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/third_party/expat/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/third_party/openssl/crypto/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/third_party/openssl/ssl/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/third_party/libsrtp/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/third_party/libjingle/Android.mk
include $(MY_WEBRTC_ROOT_PATH)/$(MY_WEBRTC_SRC_PATH)/testing/gtest/Android.mk

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE := libwebrtc_audio_preprocessing
LOCAL_MODULE_TAGS := optional
LOCAL_WHOLE_STATIC_LIBRARIES := libwebrtc_spl \
                                libwebrtc_resampler \
                                libwebrtc_apm \
                                libwebrtc_apm_utility \
                                libwebrtc_vad \
                                libwebrtc_ns \
                                libwebrtc_agc \
                                libwebrtc_aec \
                                libwebrtc_aecm \
                                libwebrtc_system_wrappers
                                # libwebrtc_remote_bitrate_estimator
LOCAL_SHARED_LIBRARIES := libcutils libstlport_shared
LOCAL_LDLIBS := -lgcc -llog
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE := libwebrtc_voice
LOCAL_MODULE_TAGS := optional
LOCAL_WHOLE_STATIC_LIBRARIES := libwebrtc_system_wrappers \
                                libwebrtc_audio_device \
                                libwebrtc_pcm16b \
                                libwebrtc_cng \
                                libwebrtc_ilbc \
                                libwebrtc_audio_coding \
                                libwebrtc_media_file \
                                libwebrtc_utility \
                                libwebrtc_neteq \
                                libwebrtc_audio_conference_mixer \
                                libwebrtc_isacfix \
                                libwebrtc_voe_core \
                                libwebrtc_g722 \
                                libwebrtc_g711 \
                                libwebrtc_rtp_rtcp
LOCAL_SHARED_LIBRARIES := libcutils \
                          libstlport_shared \
                          libwebrtc_audio_preprocessing
LOCAL_LDLIBS := -lgcc -llog -lOpenSLES
LOCAL_PRELINK_MODULE := false
include $(BUILD_SHARED_LIBRARY)

LOCAL_PATH := $(MY_WEBRTC_ROOT_PATH)
include $(CLEAR_VARS)
include $(LOCAL_PATH)/config.mk

LOCAL_MODULE_TAGS := tests
LOCAL_WHOLE_STATIC_LIBRARIES := gtest_static

LOCAL_MODULE := libwebrtc-voice-demo-jni
LOCAL_CPP_EXTENSION := .cc
LOCAL_SRC_FILES := tuenti/voiceclient.cc \
                   tuenti/txmppauth.cc \
                   tuenti/txmpppump.cc \
                   tuenti/txmppsocket.cc \
                   tuenti/clientsignalingthread.cc \
                   tuenti/presenceouttask.cc \
                   tuenti/presencepushtask.cc \
                   voiceclient_main.cc
LOCAL_CFLAGS := $(WEBRTC_LOGIN_CREDENTIALS) \
                $(JINGLE_CONFIG) \
                -DWEBRTC_RELATIVE_PATH \
                -DEXPAT_RELATIVE_PATH \
                -DJSONCPP_RELATIVE_PATH \
                -DHAVE_WEBRTC_VOICE \
                -DWEBRTC_TARGET_PC \
                -DWEBRTC_ANDROID \
                -DDISABLE_DYNAMIC_CAST \
                -D_REENTRANT \
                -DPOSIX \
                -DOS_LINUX=OS_LINUX \
                -DLINUX \
                -DANDROID \
                -DEXPAT_RELATIVE_PATH \
                -DXML_STATIC \
                -DFEATURE_ENABLE_SSL \
                -DHAVE_OPENSSL_SSL_H=1 \
                -DFEATURE_ENABLE_VOICEMAIL \
                -DFEATURE_ENABLE_PSTN \
                -DSRTP_RELATIVE_PATH \
                -DHAVE_SRTP
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../../trunk/src/voice_engine/main/test \
                    $(LOCAL_PATH)/../../trunk/src/voice_engine/main/interface \
                    $(LOCAL_PATH)/../../trunk/src \
                    $(LOCAL_PATH)/../../trunk/src/system_wrappers/interface \
                    $(LOCAL_PATH)/../../trunk/testing/gtest/include \
                    $(LOCAL_PATH)/../../trunk/third_party/libjingle/source
LOCAL_PRELINK_MODULE := false
LOCAL_SHARED_LIBRARIES := libutils \
                          libandroid \
                          libGLESv2 \
                          libjingle \
                          libstlport_shared \
                          libwebrtc_voice
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)

endif
