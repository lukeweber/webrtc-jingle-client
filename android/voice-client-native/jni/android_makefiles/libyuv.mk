LOCAL_PATH:= $(MY_THIRD_PARTY_PATH)/libyuv
include $(CLEAR_VARS)

LOCAL_MODULE:= libyuv

LOCAL_SRC_FILES := \
	source/compare.cc \
	source/compare_common.cc \
	source/compare_neon.cc \
	source/compare_posix.cc \
	source/compare_win.cc \
	source/convert.cc \
	source/convert_argb.cc \
	source/convert_from.cc \
	source/convert_from_argb.cc \
	source/cpu_id.cc \
	source/format_conversion.cc \
	source/memcpy_mips.S \
	source/mjpeg_decoder.cc \
	source/planar_functions.cc \
	source/rotate.cc \
	source/rotate_argb.cc \
	source/rotate_mips.cc \
	source/rotate_neon.cc \
	source/row_any.cc \
	source/row_common.cc \
	source/row_mips.cc \
	source/row_neon.cc \
	source/row_posix.cc \
	source/row_win.cc \
	source/scale.cc \
	source/scale_argb.cc \
	source/scale_argb_neon.cc \
	source/scale_mips.cc \
	source/scale_neon.cc \
	source/video_common.cc \

LOCAL_CFLAGS := \
	-DHAVE_JPEG

LOCAL_C_INCLUDES += \
	$(MY_THIRD_PARTY_PATH)/libjpeg_turbo \
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)

include $(BUILD_STATIC_LIBRARY)
