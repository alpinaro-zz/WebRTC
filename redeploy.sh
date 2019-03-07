mvn clean install -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dgpg.skip=true

OUT=$?

if [ $OUT -ne 0 ]; then
    exit $OUT
fi

SRC=target/webrtctest-0.0.1-SNAPSHOT.jar
DEST=/home/burak/test/webrtctest/webrtc-test.jar

cp $SRC $DEST

