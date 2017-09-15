echo This build method is prone to breaking - please install maven and use mvn package instead
echo When using maven you will find the build result as zip file in dist and the contents in dist/tmp/
CP=conf/:classes/:lib/*
SP=src/java/

/bin/mkdir -p classes/

javac -sourcepath $SP -classpath $CP -d classes/ src/java/nxt/*.java src/java/nxt/*/*.java src/java/fr/cryptohash/*.java || exit 1

/bin/rm -f burst.jar 
jar cf burst.jar -C classes . || exit 1
/bin/rm -rf classes

echo "burst.jar generated successfully"
