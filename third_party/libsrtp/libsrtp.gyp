# Copyright (c) 2012 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

{
  'target_defaults': {
    'defines': [
      'HAVE_STDLIB_H',
      'HAVE_STRING_H',
    ],
    'include_dirs': [
      'source/config',
      'source/srtp/include',
      'source/srtp/crypto/include',
    ],
    'conditions': [
      ['target_arch=="x64"', {
        'defines': [
          'CPU_CISC',
          'SIZEOF_UNSIGNED_LONG=8',
          'SIZEOF_UNSIGNED_LONG_LONG=8',
        ],
      }],
      ['target_arch=="ia32"', {
        'defines': [
          'CPU_CISC',
          'SIZEOF_UNSIGNED_LONG=4',
          'SIZEOF_UNSIGNED_LONG_LONG=8',
        ],
      }],
      ['target_arch=="arm"', {
        'defines': [
          'CPU_RISC',
          'SIZEOF_UNSIGNED_LONG=4',
          'SIZEOF_UNSIGNED_LONG_LONG=8',
        ],
      }],
      ['OS!="win"', {
        'defines': [
          'HAVE_STDINT_H',
          'HAVE_INTTYPES_H',
          'HAVE_NETINET_IN_H',
          'HAVE_UINT64_T',
          'HAVE_UINT32_T',
          'HAVE_UINT16_T',
          'HAVE_UINT8_T',
          'HAVE_UINT_T',	
         ],
      }],
      ['OS=="win"', {
        'defines': [
          'inline=__inline',
          'HAVE_BYTESWAP_METHODS_H',
         ],
      }],
    ],
    'direct_dependent_settings': {
      'include_dirs': [
        'source/config',
        'source/srtp/include',
        'source/srtp/crypto/include',
      ],
      'conditions': [
        ['target_arch=="x64"', {
          'defines': [
            'CPU_CISC',
            'SIZEOF_UNSIGNED_LONG=8',
            'SIZEOF_UNSIGNED_LONG_LONG=8',
          ],
        }],
        ['target_arch=="ia32"', {
          'defines': [
            'CPU_CISC',
            'SIZEOF_UNSIGNED_LONG=4',
            'SIZEOF_UNSIGNED_LONG_LONG=8',
          ],
        }],
        ['target_arch=="arm"', {
          'defines': [
            'CPU_RISC',
            'SIZEOF_UNSIGNED_LONG=4',
            'SIZEOF_UNSIGNED_LONG_LONG=8',
          ],
        }],
        ['OS!="win"', {
          'defines': [
            'HAVE_STDINT_H',
            'HAVE_INTTYPES_H',
            'HAVE_NETINET_IN_H',
          ],
        }],
        ['OS=="win"', {
          'defines': [
            'inline=__inline',
            'HAVE_BYTESWAP_METHODS_H',
          ],
        }],
      ],
    },
  },
  'targets': [
    {
      'target_name': 'libsrtp',
      'type': 'static_library',
      'sources': [
        # includes
        'source/srtp/include/ekt.h',
        'source/srtp/include/getopt_s.h',
        'source/srtp/include/rtp.h',
        'source/srtp/include/rtp_priv.h',
        'source/srtp/include/srtp.h',
        'source/srtp/include/srtp_priv.h',
        'source/srtp/include/ut_sim.h',

        # headers
        'source/srtp/crypto/include/aes_cbc.h',
        'source/srtp/crypto/include/aes.h',
        'source/srtp/crypto/include/aes_icm.h',
        'source/srtp/crypto/include/alloc.h',
        'source/srtp/crypto/include/auth.h',
        'source/srtp/crypto/include/cipher.h',
        'source/srtp/crypto/include/cryptoalg.h',
        'source/srtp/crypto/include/crypto.h',
        'source/srtp/crypto/include/crypto_kernel.h',
        'source/srtp/crypto/include/crypto_math.h',
        'source/srtp/crypto/include/crypto_types.h',
        'source/srtp/crypto/include/datatypes.h',
        'source/srtp/crypto/include/err.h',
        'source/srtp/crypto/include/gf2_8.h',
        'source/srtp/crypto/include/hmac.h',
        'source/srtp/crypto/include/integers.h',
        'source/srtp/crypto/include/kernel_compat.h',
        'source/srtp/crypto/include/key.h',
        'source/srtp/crypto/include/null_auth.h',
        'source/srtp/crypto/include/null_cipher.h',
        'source/srtp/crypto/include/prng.h',
        'source/srtp/crypto/include/rand_source.h',
        'source/srtp/crypto/include/rdb.h',
        'source/srtp/crypto/include/rdbx.h',
        'source/srtp/crypto/include/sha1.h',
        'source/srtp/crypto/include/stat.h',
        'source/srtp/crypto/include/xfm.h',

        # sources
        'source/srtp/srtp/ekt.c',
        'source/srtp/srtp/srtp.c',
        
        'source/srtp/crypto/cipher/aes.c',
        'source/srtp/crypto/cipher/aes_cbc.c',
        'source/srtp/crypto/cipher/aes_icm.c',
        'source/srtp/crypto/cipher/cipher.c',
        'source/srtp/crypto/cipher/null_cipher.c',
        'source/srtp/crypto/hash/auth.c',
        'source/srtp/crypto/hash/hmac.c',
        'source/srtp/crypto/hash/null_auth.c',
        'source/srtp/crypto/hash/sha1.c',
        'source/srtp/crypto/kernel/alloc.c',
        'source/srtp/crypto/kernel/crypto_kernel.c',
        'source/srtp/crypto/kernel/err.c',
        'source/srtp/crypto/kernel/key.c',
        'source/srtp/crypto/math/datatypes.c',
        'source/srtp/crypto/math/gf2_8.c',
        'source/srtp/crypto/math/stat.c',
        'source/srtp/crypto/replay/rdb.c',
        'source/srtp/crypto/replay/rdbx.c',
        'source/srtp/crypto/replay/ut_sim.c',
        'source/srtp/crypto/rng/ctr_prng.c',
        'source/srtp/crypto/rng/prng.c',
        'source/srtp/crypto/rng/rand_source.c',
      ],
    },
  ], # targets
}

# Local Variables:
# tab-width:2
# indent-tabs-mode:nil
# End:
# vim: set expandtab tabstop=2 shiftwidth=2:
