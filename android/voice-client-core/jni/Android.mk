# Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
#
# Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file in the root of the source
# tree. An additional intellectual property rights grant can be found
# in the file PATENTS.  All contributing project authors may
# be found in the AUTHORS file in the root of the source tree.

ifdef NDK_ROOT

MY_ROOT_PATH := $(call my-dir)
MY_CLIENT_PATH := $(MY_ROOT_PATH)/../../../client
MY_THIRD_PARTY_PATH := $(MY_ROOT_PATH)/../../../third_party
MY_GTEST_PATH := $(MY_THIRD_PARTY_PATH)/gtest
MY_WEBRTC_PATH := $(MY_THIRD_PARTY_PATH)/webrtc
MY_ANDROID_MAKE_FILES_PATH := $(MY_ROOT_PATH)/android_makefiles

include $(MY_WEBRTC_PATH)/common_audio/resampler/Android.mk
include $(MY_WEBRTC_PATH)/common_audio/signal_processing/Android.mk
include $(MY_WEBRTC_PATH)/common_audio/vad/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_coding/neteq/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_coding/codecs/cng/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_coding/codecs/g711/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_coding/codecs/g722/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_coding/codecs/pcm16b/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_coding/codecs/ilbc/Android.mk

#Fixed point ISAC for arm, else floating point
ifeq ($(TARGET_ARCH),arm)
include $(MY_WEBRTC_PATH)/modules/audio_coding/codecs/isac/fix/source/Android.mk
else
include $(MY_WEBRTC_PATH)/modules/audio_coding/codecs/isac/main/source/Android.mk
endif

include $(MY_WEBRTC_PATH)/modules/audio_coding/main/source/Android.mk
include $(MY_WEBRTC_PATH)/modules/utility/source/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_conference_mixer/source/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_device/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_processing/aec/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_processing/aecm/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_processing/agc/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_processing/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_processing/ns/Android.mk
include $(MY_WEBRTC_PATH)/modules/audio_processing/utility/Android.mk
include $(MY_WEBRTC_PATH)/modules/media_file/source/Android.mk
include $(MY_WEBRTC_PATH)/modules/rtp_rtcp/source/Android.mk
include $(MY_WEBRTC_PATH)/system_wrappers/source/Android.mk
include $(MY_WEBRTC_PATH)/voice_engine/Android.mk

include $(MY_ANDROID_MAKE_FILES_PATH)/expat.mk
include $(MY_ANDROID_MAKE_FILES_PATH)/openssl.mk
include $(MY_ANDROID_MAKE_FILES_PATH)/libsrtp.mk
ifeq ($(ENABLE_UNITTEST), 1)
include $(MY_ANDROID_MAKE_FILES_PATH)/gtest.mk
endif

#Video specific stuff, not yet completed.
#ifeq ($(ENABLE_VIDEO), 1)
#include $(MY_WEBRTC_PATH)/common_video/libyuv/Android.mk
#include $(MY_WEBRTC_PATH)/common_video/jpeg/Android.mk
#include $(MY_WEBRTC_PATH)/modules/video_capture/main/source/Android.mk
#include $(MY_WEBRTC_PATH)/modules/video_coding/main/source/Android.mk
#include $(MY_WEBRTC_PATH)/modules/video_coding/codecs/vp8/Android.mk
#include $(MY_WEBRTC_PATH)/modules/video_coding/codecs/i420/main/source/Android.mk
#include $(MY_WEBRTC_PATH)/modules/video_processing/main/source/Android.mk
#include $(MY_WEBRTC_PATH)/modules/video_render/main/source/Android.mk
#include $(MY_WEBRTC_PATH)/modules/bitrate_controller/Android.mk
#include $(MY_WEBRTC_PATH)/video_engine/Android.mk
#include $(MY_ANDROID_MAKE_FILES_PATH)/yuv.mk
#include $(MY_ANDROID_MAKE_FILES_PATH)/libvpx.mk
#include $(MY_ANDROID_MAKE_FILES_PATH)/libjpeg_turbo.mk

#LOCAL_ARM_MODE := arm
#LOCAL_MODULE := libwebrtc_video
#LOCAL_MODULE_TAGS := optional
#LOCAL_WHOLE_STATIC_LIBRARIES := \
	libjpeg_turbo \
	libyuv \
    libvpx \
	libwebrtc_video_coding \
	libwebrtc_jpeg \
	libwebrtc_yuv \
	libwebrtc_vie_core \
	libwebrtc_video_render \
	libwebrtc_video_capture \
	libwebrtc_video_processing \
	libwebrtc_bitrate_controller \
	libwebrtc_vp8 \
	libwebrtc_i420
#LOCAL_LDLIBS := -lgcc -llog -lOpenSLES -lGLESv2

#LOCAL_CFLAGS := \
#	-DHAVE_WEBRTC_VIDEO

#LOCAL_C_INCLUDES := \
	$(MY_WEBRTC_PATH) \
	$(MY_THIRD_PARTY_PATH)/libjpeg_turbo \
	$(MY_WEBRTC_PATH)/video_engine/include \
	$(MY_WEBRTC_PATH)/modules/video_capture/main/interface \
	$(MY_WEBRTC_PATH)/modules/video_coding/main/interface \
	$(MY_WEBRTC_PATH)/modules/video_processing/main/interface \
	$(MY_WEBRTC_PATH)/modules/video_render/main/interface \
#endif

include $(MY_THIRD_PARTY_PATH)/libjingle/Android.mk

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE := libwebrtc_audio_preprocessing
LOCAL_MODULE_TAGS := optional
LOCAL_WHOLE_STATIC_LIBRARIES := \
	libwebrtc_spl \
	libwebrtc_resampler \
	libwebrtc_apm \
	libwebrtc_apm_utility \
	libwebrtc_vad \
	libwebrtc_ns \
	libwebrtc_agc \
	libwebrtc_aec \
	libwebrtc_aecm \
	libwebrtc_system_wrappers
LOCAL_LDLIBS := -lgcc -llog
LOCAL_PRELINK_MODULE := false
include $(BUILD_STATIC_LIBRARY)

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE := libwebrtc_voice
LOCAL_MODULE_TAGS := optional
LOCAL_WHOLE_STATIC_LIBRARIES := \
	libwebrtc_system_wrappers \
	libwebrtc_audio_device \
	libwebrtc_pcm16b \
	libwebrtc_cng \
	libwebrtc_ilbc \
	libwebrtc_audio_coding \
	libwebrtc_media_file \
	libwebrtc_utility \
	libwebrtc_neteq \
	libwebrtc_audio_conference_mixer \
	libwebrtc_isac \
	libwebrtc_isacfix \
	libwebrtc_voe_core \
	libwebrtc_g722 \
	libwebrtc_g711 \
	libwebrtc_rtp_rtcp \
	libwebrtc_audio_preprocessing
LOCAL_LDLIBS := -lgcc -llog -lOpenSLES
LOCAL_PRELINK_MODULE := false
include $(BUILD_STATIC_LIBRARY)

LOCAL_PATH := $(MY_ROOT_PATH)
include $(CLEAR_VARS)

LOCAL_MODULE := libvoiceclient
LOCAL_CPP_EXTENSION := .cc
LOCAL_SRC_FILES := \
	voiceclient_main.cc \
	../../../client/helpers.cc \
	../../../client/presenceouttask.cc \
	../../../client/presencepushtask.cc \
	../../../client/clientsignalingthread.cc \
	../../../client/voiceclient.cc \
	../../../client/txmppauth.cc \
	../../../client/txmpppump.cc \
	../../../client/txmppsocket.cc \
	../../../client/xmpplog.cc \

LOCAL_CFLAGS := \
	$(WEBRTC_LOGIN_CREDENTIALS) \
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
	-DHAVE_SRTP \
	-DWEBRTC_LINUX

LOCAL_C_INCLUDES := \
	$(MY_CLIENT_PATH)/../ \
    $(MY_WEBRTC_PATH) \
	$(MY_THIRD_PARTY_PATH)/libjingle \
	$(MY_THIRD_PARTY_PATH)/webrtc/system_wrappers/interface \
	$(MY_THIRD_PARTY_PATH)/webrtc/voice_engine/include \
	$(MY_THIRD_PARTY_PATH)/webrtc/modules/interface \
	$(MY_THIRD_PARTY_PATH)/webrtc/modules/utility/interface \
	$(MY_THIRD_PARTY_PATH)/webrtc/modules/audio_device/main/interface \
	$(MY_THIRD_PARTY_PATH)/webrtc/modules/audio_processing/include \
	$(MY_THIRD_PARTY_PATH)/webrtc/voice_engine

LOCAL_PRELINK_MODULE := false
LOCAL_WHOLE_STATIC_LIBRARIES := \
	libjingle \
	libwebrtc_voice
LOCAL_SHARED_LIBRARIES := \
	libutils \
	libandroid \
	libGLESv2
LOCAL_LDLIBS := -lOpenSLES -llog
include $(BUILD_SHARED_LIBRARY)

endif
