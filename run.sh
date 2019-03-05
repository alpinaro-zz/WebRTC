CP="webrtc-test.jar:libs/*"
NATIVE=-Djava.library.path=libs/native
MAIN=io.antmedia.webrtctest.Starter

java -cp $CP $NATIVE $MAIN $@
