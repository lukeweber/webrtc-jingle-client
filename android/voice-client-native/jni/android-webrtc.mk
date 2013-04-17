# Copyright (c) 2011 The WebRTC project authors. All Rights Reserved.
#
# Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file in the root of the source
# tree. An additional intellectual property rights grant can be found
# in the file PATENTS.  All contributing project authors may
# be found in the AUTHORS file in the root of the source tree.

# These defines will apply to all source files
# Think again before changing it
MY_WEBRTC_COMMON_DEFS := \
    '-DWEBRTC_TARGET_PC' \
    '-DWEBRTC_LINUX' \
    '-DWEBRTC_THREAD_RR' \
    '-DWEBRTC_CLOCK_TYPE_REALTIME' \
    '-DWEBRTC_ANDROID' \
    '-DWEBRTC_SVNREVISION="2356"' \
    '-DWEBRTC_EXTERNAL_TRANSPORT'

ifeq ($(TARGET_ARCH), arm)
MY_WEBRTC_COMMON_DEFS += \
    '-DWEBRTC_ARCH_ARM'
endif

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
WEBRTC_BUILD_NEON_LIBS := true
MY_WEBRTC_COMMON_DEFS += \
    '-DWEBRTC_ARCH_ARM_V7A' \
	'-DWEBRTC_DETECT_ARM_NEON'

MY_ARM_CFLAGS_NEON := \
	-flax-vector-conversions
else 
WEBRTC_BUILD_NEON_LIBS := false
endif
