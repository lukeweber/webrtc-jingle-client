#include "talk/base/gunit.h"
#include "client/clientsignalingthread.h"

namespace tuenti {

TEST(WebrtcJingleTest, ConstructDestruct) {
  for (int i = 0; i < 5; ++i) {
    StunConfig* cfg = new StunConfig();
    ClientSignalingThread *clientsignalingthread = new ClientSignalingThread();
    clientsignalingthread->Login("lukewebertest@gmail.com", "testtester", cfg, "talk.google.com", 5222, false, 0, true);
    delete clientsignalingthread;
    delete cfg;
  }
}
}  //namespace tuenti
