@rem Gradle start-up script for Windows
@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem  Gradle startup script for Windows
@rem ##########################################################################

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

set JAVA_EXE=java.exe
where java >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute
if defined JAVA_HOME (
  set JAVA_EXE=%JAVA_HOME%\bin\java.exe
  if exist "%JAVA_EXE%" goto execute
)
echo ERROR: JAVA_HOME is not set and no java in PATH
exit /b 1

:execute
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
