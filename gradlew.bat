@echo off
setlocal

set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

set JAVA_EXE=java
if not "%JAVA_HOME%"=="" (
  if exist "%JAVA_HOME%\\bin\\java.exe" set JAVA_EXE="%JAVA_HOME%\\bin\\java.exe"
)

%JAVA_EXE% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

endlocal

