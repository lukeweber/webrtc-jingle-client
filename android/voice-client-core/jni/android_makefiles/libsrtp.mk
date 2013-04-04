LOCAL_PATH:= $(MY_THIRD_PARTY_PATH)/libsrtp
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
	source/srtp/srtp/ekt.c \
	source/srtp/srtp/srtp.c \
	source/srtp/crypto/cipher/aes.c \
	source/srtp/crypto/cipher/aes_cbc.c \
	source/srtp/crypto/cipher/aes_icm.c \
	source/srtp/crypto/cipher/cipher.c \
	source/srtp/crypto/cipher/null_cipher.c \
	source/srtp/crypto/hash/auth.c \
	source/srtp/crypto/hash/hmac.c \
	source/srtp/crypto/hash/null_auth.c \
	source/srtp/crypto/hash/sha1.c \
	source/srtp/crypto/kernel/alloc.c \
	source/srtp/crypto/kernel/crypto_kernel.c \
	source/srtp/crypto/kernel/err.c \
	source/srtp/crypto/kernel/key.c \
	source/srtp/crypto/math/datatypes.c \
	source/srtp/crypto/math/gf2_8.c \
	source/srtp/crypto/math/stat.c \
	source/srtp/crypto/replay/rdb.c \
	source/srtp/crypto/replay/rdbx.c \
	source/srtp/crypto/replay/ut_sim.c \
	source/srtp/crypto/rng/ctr_prng.c \
	source/srtp/crypto/rng/prng.c \
	source/srtp/crypto/rng/rand_source.c

LOCAL_CFLAGS := \
	-DHAVE_STDLIB_H \
	-DHAVE_STRING_H \
	-DSIZEOF_UNSIGNED_LONG=4 \
	-DSIZEOF_UNSIGNED_LONG_LONG=8 \
	-DHAVE_STDINT_H \
	-DHAVE_INTTYPES_H \
	-DHAVE_NETINET_IN_H \
	-DHAVE_UINT64_T \
	-DHAVE_UINT32_T \
	-DHAVE_UINT16_T \
	-DHAVE_UINT8_T \
	-DHAVE_UINT_T


# CPU_RISC doesn't work properly on android/arm
# self test for aes failed. CPU_RISC is used for optimization only,
# and CPU_CISC should just work just fine, it has been tested on android/arm with srtp
# test applications and libjingle.
# More info here http://src.chromium.org/svn/trunk/deps/third_party/libsrtp/libsrtp.gyp
LOCAL_CFLAGS += -DCPU_CISC

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/source/config \
	$(LOCAL_PATH)/source/srtp/include \
	$(LOCAL_PATH)/source/srtp/crypto/include \

LOCAL_MODULE:= libsrtp_static
include $(BUILD_STATIC_LIBRARY)
