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

"%MY_JAVA%" -cp burst.jar;conf brs.Burst

:DONE
