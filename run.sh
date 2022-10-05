CP="webrtc-test.jar:libs/*"
NATIVE=-Djava.library.path=libs/native
MAIN=io.antmedia.Starter

# shellcheck disable=SC2068
java -cp $CP $NATIVE $MAIN $@
