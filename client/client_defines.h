#ifndef _CLIENT_CLIENT_DEFINES_H_
#define _CLIENT_CLIENT_DEFINES_H_

#ifdef __APPLE__
//TODO: Why doesn't srtp(openssl incorrectly compiled issue) work on iOS(arm).
#include "TargetConditionals.h"
#define ENABLE_SRTP TARGET_IPHONE_SIMULATOR
#else
#define ENABLE_SRTP 1
#endif

#if LOGGING
#define XMPP_LOG_STANZAS 1
#endif

#ifdef IOS_XMPP_FRAMEWORK
#define XMPP_COMPATIBILITY 1
#define XMPP_WHITESPACE_KEEPALIVE_ENABLED 0
#define XMPP_CHAT_ENABLED 0
#define XMPP_PING_ENABLED 0
#define XMPP_ENABLE_ROSTER 0
#define XMPP_DISABLE_INCOMING_PRESENCE 0
#define ADD_RANDOM_RESOURCE_TO_JID 0

#elif TUENTI_CUSTOM_BUILD
#define XMPP_COMPATIBILITY 1
#define XMPP_CHAT_ENABLED 0
#define XMPP_WHITESPACE_KEEPALIVE_ENABLED 0
#define XMPP_PING_ENABLED 1
#define XMPP_ENABLE_ROSTER 0
#define XMPP_DISABLE_INCOMING_PRESENCE 1
#define ADD_RANDOM_RESOURCE_TO_JID 1

#else
#define XMPP_CHAT_ENABLED 1
#define XMPP_WHITESPACE_KEEPALIVE_ENABLED 1
#define XMPP_PING_ENABLED 1
#define XMPP_ENABLE_ROSTER 1
#define XMPP_DISABLE_INCOMING_PRESENCE 0
#define ADD_RANDOM_RESOURCE_TO_JID 0
#endif

#include <string>
#include <sstream>

namespace tuenti{

#ifdef IOS_XMPP_FRAMEWORK
    struct StunConfig {
        std::string stun;
        std::string turn;
        std::string turn_username;
        std::string turn_password;
        std::string ToString() {
            std::stringstream stream;
            stream << "[stun=(" << stun << "),";
            stream << "turn=(" << turn << ")]";
            return stream.str();
        }
    };
#endif
    
enum XmppTimings {
  LoginTimeout = 10000, // 10 Seconds
  PingTimeout = 10000, // 10 Seconds
  PingInterval = 360000, // 6 minutes
  XmppKeepAliveInterval = 360000, // 6 minutes
};

const std::string STR_DENY = "deny";
const std::string STR_ALLOW = "allow";

}  // namespace tuenti
#endif  //_CLIENT_CLIENT_DEFINES_H_
