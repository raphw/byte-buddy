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

@REM Possible user configuration.
SET checksumUrl="https://repo.maven.apache.org/maven2/codes/rafael/mavenchecksumextension/maven-checksum-extension/0.0.2/maven-checksum-extension-0.0.2.jar"
SET checksumJar=maven-checksum-extension.jar
SET checksumSha256Sum=3cfb53b1f9ac03a140e7f04f6083953cbcb7da3bfbedc5c9c53f9e1b5265e7d5

@REM Setting artifact directory.
SET SCRIPT=%~dp0
SET SCRIPT_PATH=%SCRIPT:~0,-1%
SET checksumJarPath=%SCRIPT_PATH%\%checksumJar%

@REM Download checksum extension if not available.
if exist %checksumJarPath% (
    if "%MVNC_VERBOSE%" == "true" (
        echo Found %checksumJarPath%
    )
) else (
    powershell -Command "&{"^
		"$webclient = new-object System.Net.WebClient;"^
		"if (-not ([string]::IsNullOrEmpty('%MVNC_USERNAME%') -and [string]::IsNullOrEmpty('%MVNC_PASSWORD%'))) {"^
		"$webclient.Credentials = new-object System.Net.NetworkCredential('%MVNC_USERNAME%', '%MVNC_PASSWORD%');"^
		"}"^
		"[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%checksumUrl%', '%checksumJarPath%')"^
		"}"
    if "%MVNC_VERBOSE%" == "true" (
        echo Finished downloading %checksumJarPath%
    )
)

@REM Validate the checksum of the checksum jar file.
FOR /F "tokens=*" %%A in ('certUtil -hashfile "%checksumJarPath%" SHA256 ^| findstr /v "hash"') do (
    IF NOT %%A==%checksumSha256Sum% (
        echo Error: Failed to validate Maven wrapper SHA-256, your Maven wrapper might be compromised. >&2
        echo Investigate or delete %checksumJarPath% to attempt a clean download. >&2
        goto error
    )
)

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