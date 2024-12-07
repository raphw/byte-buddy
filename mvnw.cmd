@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.1.1
@REM (Modified for the purposes of building Byte Buddy)
@REM
@REM Required ENV vars:
@REM JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM MAVEN_BATCH_ECHO - set to 'on' to enable the echoing of the batch commands
@REM MAVEN_BATCH_PAUSE - set to 'on' to wait for a keystroke before ending
@REM MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM ----------------------------------------------------------------------------

@REM Begin all REM lines with '@' in case MAVEN_BATCH_ECHO is 'on'
@echo off
@REM set title of command window
title %0
@REM enable echoing by setting MAVEN_BATCH_ECHO to 'on'
@if "%MAVEN_BATCH_ECHO%" == "on"  echo %MAVEN_BATCH_ECHO%

@REM set %HOME% to equivalent of $HOME
if "%HOME%" == "" (set "HOME=%HOMEDRIVE%%HOMEPATH%")

@REM Execute a user defined script before this one
if not "%MAVEN_SKIP_RC%" == "" goto skipRcPre
@REM check for pre script, once with legacy .bat ending and once with .cmd ending
if exist "%USERPROFILE%\mavenrc_pre.bat" call "%USERPROFILE%\mavenrc_pre.bat" %*
if exist "%USERPROFILE%\mavenrc_pre.cmd" call "%USERPROFILE%\mavenrc_pre.cmd" %*
:skipRcPre

@setlocal

set ERROR_CODE=0

@REM To isolate internal variables from possible post scripts, we use another setlocal
@setlocal

@REM ==== START VALIDATION ====
if not "%JAVA_HOME%" == "" goto OkJHome

echo.
echo Error: JAVA_HOME not found in your environment. >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo.
goto error

:OkJHome
if exist "%JAVA_HOME%\bin\java.exe" goto init

echo.
echo Error: JAVA_HOME is set to an invalid directory. >&2
echo JAVA_HOME = "%JAVA_HOME%" >&2
echo Please set the JAVA_HOME variable in your environment to match the >&2
echo location of your Java installation. >&2
echo.
goto error

@REM ==== END VALIDATION ====

:init

@REM Find the project base dir, i.e. the directory that contains the folder ".mvn".
@REM Fallback to current working directory if not found.

set MAVEN_PROJECTBASEDIR=%MAVEN_BASEDIR%
IF NOT "%MAVEN_PROJECTBASEDIR%"=="" goto endDetectBaseDir

set EXEC_DIR=%CD%
set WDIR=%EXEC_DIR%
:findBaseDir
IF EXIST "%WDIR%"\.mvn goto baseDirFound
cd ..
IF "%WDIR%"=="%CD%" goto baseDirNotFound
set WDIR=%CD%
goto findBaseDir

:baseDirFound
set MAVEN_PROJECTBASEDIR=%WDIR%
cd "%EXEC_DIR%"
goto endDetectBaseDir

:baseDirNotFound
set MAVEN_PROJECTBASEDIR=%EXEC_DIR%
cd "%EXEC_DIR%"

:endDetectBaseDir

IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config" goto endReadAdditionalConfig

@setlocal EnableExtensions EnableDelayedExpansion
for /F "usebackq delims=" %%a in ("%MAVEN_PROJECTBASEDIR%\.mvn\jvm.config") do set JVM_CONFIG_MAVEN_PROPS=!JVM_CONFIG_MAVEN_PROPS! %%a
@endlocal & set JVM_CONFIG_MAVEN_PROPS=%JVM_CONFIG_MAVEN_PROPS%

:endReadAdditionalConfig

@REM Use HTTP endpoints and legacy Maven if Java 6 or 7 is used to build (Byte Buddy edit)
set REPO_URL=http://insecure.repo1.maven.org
set WRAPPER_LOCATION=wrapper-legacy
set WRAPPER_PATH=io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar
for /f tokens^=2-5^ delims^=.-_^" %%j in ('%JAVA_HOME%\bin\java -fullversion 2^>^&1') do set "JAVA_VERSION_STRING=%%j.%%k"
IF NOT "%JAVA_VERSION_STRING:~0,3%"=="1.6" (
  IF NOT "%JAVA_VERSION_STRING:~0,3%"=="1.7" (
    set REPO_URL=https://repo.maven.apache.org
    set WRAPPER_LOCATION=wrapper
    set WRAPPER_PATH=org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
  )
)

SET MAVEN_JAVA_EXE="%JAVA_HOME%\bin\java.exe"
set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\%WRAPPER_LOCATION%\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

set WRAPPER_URL="%REPO_URL%/maven2/%WRAPPER_PATH%"

FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\%WRAPPER_LOCATION%\maven-wrapper.properties") DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
)

@REM Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
@REM This allows using the maven wrapper in projects that prohibit checking in binary data.
if exist %WRAPPER_JAR% (
    if "%MVNW_VERBOSE%" == "true" (
        echo Found %WRAPPER_JAR%
    )
) else (
    if not "%MVNW_REPOURL%" == "" (
        SET WRAPPER_URL="%MVNW_REPOURL%/%WRAPPER_PATH%"
    )
    if "%MVNW_VERBOSE%" == "true" (
        echo Couldn't find %WRAPPER_JAR%, downloading it ...
        echo Downloading from: %WRAPPER_URL%
    )

    powershell -Command "&{"^
		"$webclient = new-object System.Net.WebClient;"^
		"if (-not ([string]::IsNullOrEmpty('%MVNW_USERNAME%') -and [string]::IsNullOrEmpty('%MVNW_PASSWORD%'))) {"^
		"$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNW_USERNAME%', '%MVNW_PASSWORD%');"^
		"}"^
		"[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"^
		"}"
    if "%MVNW_VERBOSE%" == "true" (
        echo Finished downloading %WRAPPER_JAR%
    )
)
@REM End of extension

@REM Validate the Maven wrapper's hash (Byte Buddy edit)
SET FILE_HASH=""
FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%MAVEN_PROJECTBASEDIR%\.mvn\%WRAPPER_LOCATION%\maven-wrapper.properties") DO (
    IF "%%A"=="wrapperSha256Sum" SET FILE_HASH=%%B
)
IF NOT %FILE_HASH%=="" (
    powershell -Command "&{"^
       "$CHECKSUM = (Invoke-Expression \"certUtil -hashfile '%MAVEN_PROJECTBASEDIR%\.mvn\%WRAPPER_LOCATION%\maven-wrapper.jar' SHA256\" | Select -Index 1);"^
       "If('%FILE_HASH%' -ne $CHECKSUM){"^
       "  Write-Output 'Error: Failed to validate Maven checksum extension SHA-256, it might be compromised';"^
       "  Write-Output 'Investigate or delete %MAVEN_PROJECTBASEDIR%\.mvn\%WRAPPER_LOCATION%\maven-wrapper.jar to attempt a clean download.';"^
       "  exit 1;"^
       "}"^
       "}"
)

@REM If requested, add Maven checksum extension (Byte Buddy edit).
@setlocal EnableDelayedExpansion
set MAVEN_CHECKSUM_EXTENSION_TYPE=
FOR %%a IN (%*) DO (
    if "%%a"=="-Pchecksum-collect" set MAVEN_CHECKSUM_EXTENSION_TYPE=collect
    if "%%a"=="-Pchecksum-enforce" set MAVEN_CHECKSUM_EXTENSION_TYPE=enforce
)
@endlocal & set MAVEN_CHECKSUM_EXTENSION_TYPE=%MAVEN_CHECKSUM_EXTENSION_TYPE%
@setlocal EnableDelayedExpansion
set MAVEN_CHECKSUM_EXTENSION_COMMAND=
if not "%MAVEN_CHECKSUM_EXTENSION%"=="" (
  CALL %MAVEN_PROJECTBASEDIR%/.mvn/checksum/mvnc.cmd
  if ERRORLEVEL 1 goto error
  SET MAVEN_CHECKSUM_EXTENSION_COMMAND=-Dcodes.rafael.mavenchecksumextension.mode=%MAVEN_CHECKSUM_EXTENSION_TYPE%^
    -Dcodes.rafael.mavenchecksumextension.file=%MAVEN_PROJECTBASEDIR%\.mvn\checksums.sha256
    -Dcodes.rafael.mavenchecksumextension.append=true
    -Dmaven.ext.class.path=%MAVEN_PROJECTBASEDIR%\.mvn\checksum\maven-checksum-extension.jar
)
@endlocal & set MAVEN_CHECKSUM_EXTENSION_COMMAND=%MAVEN_CHECKSUM_EXTENSION_COMMAND%

@REM Provide a "standardized" way to retrieve the CLI args that will
@REM work with both Windows and non-Windows executions.
set MAVEN_CMD_LINE_ARGS=%*

%MAVEN_JAVA_EXE% ^
  %JVM_CONFIG_MAVEN_PROPS% ^
  %MAVEN_OPTS% ^
  %MAVEN_DEBUG_OPTS% ^
  -classpath %WRAPPER_JAR% ^
  "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" ^
  %WRAPPER_LAUNCHER% %MAVEN_CONFIG% %MAVEN_CHECKSUM_EXTENSION_COMMAND% %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%

if not "%MAVEN_SKIP_RC%"=="" goto skipRcPost
@REM check for post script, once with legacy .bat ending and once with .cmd ending
if exist "%USERPROFILE%\mavenrc_post.bat" call "%USERPROFILE%\mavenrc_post.bat"
if exist "%USERPROFILE%\mavenrc_post.cmd" call "%USERPROFILE%\mavenrc_post.cmd"
:skipRcPost

@REM pause the script if MAVEN_BATCH_PAUSE is set to 'on'
if "%MAVEN_BATCH_PAUSE%"=="on" pause

if "%MAVEN_TERMINATE_CMD%"=="on" exit %ERROR_CODE%

cmd /C exit /B %ERROR_CODE%
