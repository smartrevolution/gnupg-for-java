#STYLE=textui
STYLE=swingui
#STYLE=awtui
java -Djava.library.path=`pwd`/src/c -cp dist/gnupg-for-java-0.1.5.jar:lib/junit-3.8.1.jar junit.${STYLE}.TestRunner com.freiheit.gnupg.GnuPGTestSuite
