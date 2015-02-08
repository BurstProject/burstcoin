title BURST WALLET
set MAXRAM=700m
set LAUNCH=-Xmx%MAXRAM% -cp burst.jar;lib\*;conf nxt.Nxt
set JAVAVERSION=jre1.8.0_31
set JAVAEXEC=\Java\%JAVAVERSION%\bin\java.exe
:runagain
@ECHO OFF
IF EXIST java (
	java %LAUNCH% 
) ELSE (
	IF EXIST "%PROGRAMFILES%\Java\%JAVAVERSION%" (
		"%PROGRAMFILES%%JAVAEXEC%" %LAUNCH% 
	) ELSE (
		IF EXIST "%PROGRAMFILES(X86)%\%JAVAVERSION%" (
			"%PROGRAMFILES(X86)%%JAVAEXEC%" %LAUNCH% 
		) ELSE (
			ECHO Java software not found on your system. Please go to http://java.com/en/ to download a copy of Java.
			PAUSE
		)
	)
)
goto runagain