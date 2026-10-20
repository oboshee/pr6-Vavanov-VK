@echo off
chcp 65001 >nul
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt || (del sources.txt & exit /b 1)
del sources.txt
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out ru.edu.pr03.SelfCheck %*
