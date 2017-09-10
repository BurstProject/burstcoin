@echo off
IF %1.==. GOTO No1

java -cp burst.jar nxt.db.quicksync.CreateBinDump %1
GOTO END

:No1
echo Missing filename! Usage: createBinDump [filename]


:END