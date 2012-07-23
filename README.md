mkdir webrtcjinleproject
cd webrtcjingleproject
gclient config https://github.com/lukeweber/webrtc-jingle-client.git --name trunk
gclient sync

cd trunk
source build/envsetup.sh
gclient runhooks
make
