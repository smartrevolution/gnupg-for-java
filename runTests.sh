#STYLE=textui
STYLE=swingui
#STYLE=awtui
java -Djava.library.path=`pwd`/jni:`pwd`/lib -cp build/jar/gnupg-for-java-0.2-pre.jar:lib/junit-3.8.1.jar junit.${STYLE}.TestRunner com.freiheit.gnupg.tests.GnuPGTestSuite
