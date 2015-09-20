set CP=conf\;classes\;lib\*
set SP=src\java\
set JRE=jdk1.8.0_40

md classes

"C:\Program Files\Java\%JRE%\bin\javac.exe" -sourcepath %SP% -classpath %CP% -d classes\ src\java\nxt\*.java src\java\nxt\crypto\*.java src\java\nxt\util\*.java  src\java\nxt\http\*.java  src\java\fr\cryptohash\*.java src\java\nxt\at\*.java src\java\nxt\peer\*.java src\java\nxt\user\*.java  src\java\nxt\db\*.java  src\java\fr\cryptohash\test\*.java


:/bin/rm -f burst.jar 
:this seems to be overwritten by the compiler so not really any need to remove it on windows

"C:\Program Files\Java\%JRE%\bin\jar.exe" cf burst.jar -C classes .
:/bin/rm -rf classes
:seems to update fine even if not removed, so no immediate need to remove the directory

echo "burst.jar generated successfully"
pause
