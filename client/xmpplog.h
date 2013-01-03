#ifndef _XMPPLOG_H_
#define _XMPPLOG_H_

#include "talk/base/logging.h"
#include "talk/base/sigslot.h"
#include "client/logging.h"

namespace tuenti {
class XmppLog : public sigslot::has_slots<> {
 public:
  XmppLog() :
    debug_input_buf_(NULL), debug_input_len_(0), debug_input_alloc_(0),
    debug_output_buf_(NULL), debug_output_len_(0), debug_output_alloc_(0),
    censor_password_(false)
      {}
  void Input(const char * data, int len);
  void Output(const char * data, int len);
 private:
  char * debug_input_buf_;
  int debug_input_len_;
  int debug_input_alloc_;
  char * debug_output_buf_;
  int debug_output_len_;
  int debug_output_alloc_;
  bool censor_password_;
  bool IsAuthTag(const char * str, size_t len);
  void DebugPrint(char * buf, int * plen, bool output);
};
}
#endif  // _XMPPLOG_H_
