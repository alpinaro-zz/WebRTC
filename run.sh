CP="webrtc-test.jar:libs/*"
NATIVE=-Djava.library.path=libs/native
MAIN=./java.io.antmedia.Starter

java -cp $CP $NATIVE $MAIN $@
