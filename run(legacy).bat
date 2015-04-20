@ECHO OFF
IF EXIST java (
	start "BURST" java -cp burst.jar;lib\*;conf nxt.Nxt
) ELSE (
	IF EXIST "%PROGRAMFILES%\Java\jre7" (
		start "BURST" "%PROGRAMFILES%\Java\jre7\bin\java.exe" -cp burst.jar;lib\*;conf nxt.Nxt
	) ELSE (
		IF EXIST "%PROGRAMFILES(X86)%\Java\jre7" (
			start "BURST" "%PROGRAMFILES(X86)%\Java\jre7\bin\java.exe" -cp burst.jar;lib\*;conf nxt.Nxt
		) ELSE (
			ECHO Java software not found on your system. Please go to http://java.com/en/ to download a copy of Java.
			PAUSE
		)
	)
)

