@echo off
title SingleMillionClient
SET MAIN_CLASS="com.fandou.learning.netty.core.chapter15.client.PerformanceClient"

SET APP_HOME="%~dp0.."
SET LOG_DIR="%APP_HOME%/logs"

echo JAVA_HOME = "%JAVA_HOME%"
SET JAVA_EXE="%JAVA_HOME%\bin\java.exe"

SET CLASSPATH=.;../classes;
FOR %%F IN (..\lib\*.jar) DO call :ADDCP %%F
goto RUN

:ADDCP
set CLASSPATH=%CLASSPATH%;%1
goto :EOF 

:RUN
echo %CLASSPATH%
%JAVA_EXE% -DlogDir=%LOG_DIR% -classpath %CLASSPATH% %MAIN_CLASS% "192.168.8.114"

CMD
