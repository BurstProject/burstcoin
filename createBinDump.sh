#!/bin/bash

SCRIPTNAME=`basename $0`

usage ()
{
    echo "usage: $SCRIPTNAME [filename]"

}

if [ -z "$1" ]; then
    usage
    exit 1
fi

java -cp burst.jar:conf nxt.db.quicksync.CreateBinDump $1
