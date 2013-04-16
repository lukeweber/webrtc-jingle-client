LOCAL_PATH:= $(MY_THIRD_PARTY_PATH)/expat
include $(CLEAR_VARS)

LOCAL_MODULE:= libexpat_static

LOCAL_SRC_FILES := \
	files/lib/xmlparse.c \
	files/lib/xmlrole.c \
	files/lib/xmltok.c

LOCAL_CFLAGS := \
	-DHAVE_EXPAT_CONFIG_H

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/files/lib

include $(BUILD_STATIC_LIBRARY)
