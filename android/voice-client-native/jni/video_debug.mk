# Copyright (c) 2012 The WebRTC project authors. All Rights Reserved.
#
# Use of this source code is governed by a BSD-style license
# that can be found in the LICENSE file in the root of the source
# tree. An additional intellectual property rights grant can be found
# in the file PATENTS.  All contributing project authors may
# be found in the AUTHORS file in the root of the source tree.
APP_ABI := armeabi-v7a
APP_PLATFORM := android-9
APP_CPPFLAGS += -fno-rtti
APP_CFLAGS += \
	-DHAVE_WEBRTC_VIDEO \
	-D_DEBUG=1 \
	-DENABLE_DEBUG=1 \
	-DLOGGING=1 \
	-DWEBRTC_ANDROID_DEBUG=1
APP_STL := stlport_static
APP_OPTIM := debug
ENABLE_WEBRTC_TRACE := 1
ENABLE_UNITTEST := 1
ENABLE_VIDEO := 1
