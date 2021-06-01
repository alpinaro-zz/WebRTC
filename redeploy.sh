mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dgpg.skip=true

OUT=$?

if [ $OUT -ne 0 ]; then
    exit $OUT
fi

SRC=target/webrtctest*-SNAPSHOT.jar

if [ -d ~/test/webrtctest/ ]
then
    DEST=~/test/webrtctest/webrtc-test.jar
    cp $SRC $DEST
    echo "$SRC's copied to $DEST"
fi



