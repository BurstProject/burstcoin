@echo off
echo Checking Path only
for %%f in (java.exe) do if exist %%~$path:f (
echo Java found at: %%~$path:f
start "BURST" "%%~$path:f" -cp burst.jar;lib\*;conf nxt.Nxt
goto done
) else (
echo Not found in Path, Searching C drive for java
for /F "tokens=*" %%f in ('where /F /R C:\ java.exe') do (
echo Java found at: %%f
start "BURST" %%f -cp burst.jar;lib\*;conf nxt.Nxt
goto done
)
)
echo No Java Found on this Computer
goto done
:done

