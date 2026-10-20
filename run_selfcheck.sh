#!/bin/sh
set -e
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out ru.edu.pr03.SelfCheck "$@"
