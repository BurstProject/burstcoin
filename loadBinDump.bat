@echo off
IF %1.==. GOTO No1

java -cp burst.jar;conf nxt.db.quicksync.LoadBinDump %1
GOTO END

:No1
echo Missing filename! Usage: loadBinDump [filename or url]


:END