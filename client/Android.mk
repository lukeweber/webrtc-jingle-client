LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_CPP_EXTENSION := .cc
LOCAL_MODULE := webrtcjingle
LOCAL_MODULE_TAGS := optional
LOCAL_CPP_EXTENSION := .cc
LOCAL_SRC_FILES := \
	receivemessagetask.cc \
	sendmessagetask.cc \
	keepalivetask.cc \
	presenceouttask.cc \
	rosterhandler.cc \
	clientsignalingthread.cc \
	voiceclient.cc \
	txmppauth.cc \
	txmpppump.cc \
	txmppsocket.cc \
	xmpplog.cc

LOCAL_CFLAGS := \
	$(LIBJINGLE_CPPFLAGS) \
	-DJSONCPP_RELATIVE_PATH \
	-DWEBRTC_TARGET_PC \
	-DWEBRTC_ANDROID \
	-DWEBRTC_LINUX

LOCAL_C_INCLUDES := \
    $(MY_ROOT_PATH) \
	$(MY_CLIENT_PATH)/../ \
    $(MY_WEBRTC_PATH) \
	$(MY_THIRD_PARTY_PATH) \
	$(MY_THIRD_PARTY_PATH)/libjingle \
	$(MY_THIRD_PARTY_PATH)/webrtc/system_wrappers/interface \
	$(MY_THIRD_PARTY_PATH)/webrtc/voice_engine/include \
	$(MY_THIRD_PARTY_PATH)/webrtc/modules/interface \
	$(MY_THIRD_PARTY_PATH)/webrtc/modules/utility/interface \
	$(MY_THIRD_PARTY_PATH)/webrtc/modules/audio_device/main/interface \
	$(MY_THIRD_PARTY_PATH)/webrtc/modules/audio_processing/include \
	$(MY_THIRD_PARTY_PATH)/webrtc/voice_engine

LOCAL_LDLIBS := -lgcc -llog
LOCAL_PRELINK_MODULE := false
include $(BUILD_STATIC_LIBRARY)

ifeq ($(ENABLE_UNITTEST), 1)
include $(CLEAR_VARS)
LOCAL_ARM_MODE := arm
LOCAL_MODULE := webrtcjingle_unittest

LOCAL_CPP_EXTENSION:= .cc

LOCAL_SRC_FILES := \
	unit/webrtcjingletest.cc \
	unit/sendmessagetask_unittest.cc \
	unit/receivemessagetask_unittest.cc \
	unit/clientsignalingthread_unittest.cc

LOCAL_C_INCLUDES := \
	$(MY_LIBJINGLE_C_INCLUDES) \
	$(GTEST_C_INCLUDES) \
	$(MY_CLIENT_PATH)/../ \

LOCAL_WHOLE_STATIC_LIBRARIES := \
	libjingle_unittest_main \
	libwebrtcjingle \
	libjingle \
	libwebrtc_voice \
	libexpat_static \
	libsrtp_static \
	libssl_static

LOCAL_CFLAGS +=  $(MY_UNITTEST_CFLAGS)
LOCAL_LDLIBS := -llog -lOpenSLES
include $(BUILD_SHARED_LIBRARY)
endif #ENABLE_UNITTEST 1
