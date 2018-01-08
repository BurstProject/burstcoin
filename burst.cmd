@ECHO OFF

SET MY_SELF=%0
SET MY_CMD=%1
SET MY_OPT=%2

IF "%MY_JAVA%" == "" (
  for %%f in (java.exe) do if exist %%~$path:f (
      echo Java found at: %%~$path:f
      SET MY_JAVA=%%~$path:f
  )
)

IF "%MY_JAVA%" == "" (
  for /F "tokens=*" %%f in ('where /F /R %SYSTEMDRIVE%\ java.exe') do (
      SET MY_JAVA=%%f
  )
)

IF "%MY_JAVA%" == "" (
    echo please install java from eg. https://java.com/de/download/
    goto DONE
)

IF NOT "%MY_CMD%" == "" (
    IF "%MY_OPT%" == "" (
        goto USAGE
    )
    IF "%MY_CMD%" == "load" (
        %MY_JAVA% -cp burst.jar;conf brs.db.quicksync.LoadBinDump "$MY_OPT"
        goto DONE
    )
    IF "%MY_CMD%" == "loadsilent" (
        %MY_JAVA% -cp burst.jar;conf brs.db.quicksync.LoadBinDump "$MY_OPT" -y
        goto DONE
    )
    IF "%MY_CMD%" == "dump" (
        %MY_JAVA% -cp burst.jar;conf brs.db.quicksync.CreateBinDump "$MY_OPT"
        goto DONE
    )
    goto USAGE
)
%MY_JAVA% -cp burst.jar;conf brs.Burst

goto DONE

:USAGE

echo usage: %MY_SELF% [command] [arguments]
echo   load       [filename or url]  quick import by loading a binary dump
echo   loadsilent [filename or url]  ATTENTION .. same as load, but runs directly without asking for a confirmation
echo   dump       [filename]         create a binary dump usable for doing a quick import
echo   help                          shows the help you just read

:DONE
