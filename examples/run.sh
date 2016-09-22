#!/bin/sh

projectroot=`pwd`/..

if [ ! -e KeySearch.class ]; then
    javac -classpath $projectroot/src KeySearch.java
fi

java -Djava.library.path=$projectroot/lib \
    -classpath ../build/jar/gnupg-for-java-0.2-pre.jar:. \
    KeySearch
