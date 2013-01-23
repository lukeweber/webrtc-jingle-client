LOCAL_PATH:= $(MY_THIRD_PARTY_PATH)/libjpeg_turbo
include $(CLEAR_VARS)

LOCAL_MODULE:= libjpeg_turbo

LOCAL_SRC_FILES := \
	jcapimin.c \
	jcapistd.c \
	jccoefct.c \
	jccolor.c \
	jcdctmgr.c \
	jchuff.c \
	jcinit.c \
	jcmainct.c \
	jcmarker.c \
	jcmaster.c \
	jcomapi.c \
	jcparam.c \
	jcphuff.c \
	jcprepct.c \
	jcsample.c \
	jdapimin.c \
	jdapistd.c \
	jdatadst.c \
	jdatasrc.c \
	jdcoefct.c \
	jdcolor.c \
	jddctmgr.c \
	jdhuff.c \
	jdinput.c \
	jdmainct.c \
	jdmarker.c \
	jdmaster.c \
	jdmerge.c \
	jdphuff.c \
	jdpostct.c \
	jdsample.c \
	jerror.c \
	jfdctflt.c \
	jfdctfst.c \
	jfdctint.c \
	jidctflt.c \
	jidctfst.c \
	jidctint.c \
	jidctred.c \
	jmemmgr.c \
	jmemnobs.c \
	jquant1.c \
	jquant2.c \
	jutils.c

ifeq ($(ARCH_ARM_HAVE_ARMV7A),true)
LOCAL_SRC_FILES += \
	simd/jsimd_arm.c \
	simd/jsimd_arm_neon.S
endif

LOCAL_CFLAGS := \
	-DWITH_SIMD \
	-DMOTION_JPEG_SUPPORTED

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)

include $(BUILD_STATIC_LIBRARY)
