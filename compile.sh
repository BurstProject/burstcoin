#!/bin/bash
## Do we have a local copy?
if [ -d "maven/apache-maven-3.5.0" ]; then
    export PATH=maven/apache-maven-3.5.0/bin:$PATH
fi

## check if command exists
  if hash mvn 2>/dev/null; then
        mvn package
        mvn javadoc:javadoc-no-fork
	cp -r target/site/apidocs html/ui/doc
        cp dist/tmp/burst.jar .
        echo a .zip file has been built for distribution in dist/, its contents are in dist/tmp
        echo Nevertheless, now you can start the wallet with ./run.sh
    else
        echo This build method is no longer supported. Please install maven.
        echo https://maven.apache.org/install.html
        if hash wget 2>/dev/null; then
	    read -p "Do you want me to install a local copy of maven in this directory? " -n 1 -r
	    echo
	    if [[ $REPLY =~ ^[Yy]$ ]]
		then
		    mkdir maven
		    cd maven
		    ## This is an official mirror
		    wget http://mirror.23media.de/apache/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.tar.gz
		    tar -xvzf apache-maven-3.5.0-bin.tar.gz
		    rm -f apache-maven-3.5.0-bin.tar.gz
		    echo Please try again, it should work now. You might want to check if the environment variable JAVA_HOME points to a valid JDK.
		fi

        fi
    fi
