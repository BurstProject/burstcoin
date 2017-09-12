#!/bin/bash

SCRIPTNAME=`basename $0`

usage ()
{
    echo "usage: $SCRIPTNAME [filename or url]"

}

if [ -z "$1" ]; then
    usage
    exit 1
fi

java -cp burst.jar nxt.db.quicksync.LoadBinDump $1
