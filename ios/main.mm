#import "client/voiceclient.h"

int main(int argc, char *argv[]) {
    tuenti::VoiceClient* vc = new tuenti::VoiceClient();
    vc->Init();
    return 0;
}
