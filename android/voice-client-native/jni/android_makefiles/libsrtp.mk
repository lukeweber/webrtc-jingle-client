LOCAL_PATH:= $(MY_THIRD_PARTY_PATH)/libsrtp
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	srtp/srtp/ekt.c \
	srtp/srtp/srtp.c \
	srtp/crypto/cipher/aes.c \
	srtp/crypto/cipher/aes_cbc.c \
	srtp/crypto/cipher/aes_icm.c \
	srtp/crypto/cipher/cipher.c \
	srtp/crypto/cipher/null_cipher.c \
	srtp/crypto/hash/auth.c \
	srtp/crypto/hash/hmac.c \
	srtp/crypto/hash/null_auth.c \
	srtp/crypto/hash/sha1.c \
	srtp/crypto/kernel/alloc.c \
	srtp/crypto/kernel/crypto_kernel.c \
	srtp/crypto/kernel/err.c \
	srtp/crypto/kernel/key.c \
	srtp/crypto/math/datatypes.c \
	srtp/crypto/math/gf2_8.c \
	srtp/crypto/math/stat.c \
	srtp/crypto/replay/rdb.c \
	srtp/crypto/replay/rdbx.c \
	srtp/crypto/replay/ut_sim.c \
	srtp/crypto/rng/ctr_prng.c \
	srtp/crypto/rng/prng.c \
	srtp/crypto/rng/rand_source.c

LOCAL_CFLAGS := \
	-DHAVE_STDLIB_H \
	-DHAVE_STRING_H \
	-DHAVE_STDINT_H \
	-DHAVE_INTTYPES_H \
	-DHAVE_NETINET_IN_H \
	-DHAVE_UINT64_T \
	-DHAVE_UINT32_T \
	-DHAVE_UINT16_T \
	-DHAVE_UINT8_T \
	-DHAVE_UINT_T \
	-DINLINE=inline \
    -DHAVE_SYS_SOCKET_H


# CPU_RISC doesn't work properly on android/arm
# self test for aes failed. CPU_RISC is used for optimization only,
# and CPU_CISC should just work just fine, it has been tested on android/arm with srtp
# test applications and libjingle.
# More info here http://src.chromium.org/svn/trunk/deps/third_party/libsrtp/libsrtp.gyp
LOCAL_CFLAGS += -DCPU_CISC

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/config \
	$(LOCAL_PATH)/srtp/include \
	$(LOCAL_PATH)/srtp/crypto/include \

LOCAL_MODULE:= libsrtp_static
include $(BUILD_STATIC_LIBRARY)
