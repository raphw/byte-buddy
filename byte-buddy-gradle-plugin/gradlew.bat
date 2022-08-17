@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem  (Modified for the purposes of building Byte Buddy)
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute

@rem Use legacy Gradle if Java 6 or 7 is used to build (Byte Buddy edit)
set WRAPPER_LOCATION=wrapper-legacy
set GRADLE_LEGACY="true"
for /f tokens^=2-5^ delims^=.-_^" %%j in ('%JAVA_HOME%\bin\java -fullversion 2^>^&1') do set "JAVA_VERSION_STRING=%%j.%%k"
IF NOT "%JAVA_VERSION_STRING:~0,3%"=="1.6" (
  IF NOT "%JAVA_VERSION_STRING:~0,3%"=="1.7" (
    set WRAPPER_LOCATION=wrapper
    set GRADLE_LEGACY="false"
  )
)

@rem Extension to allow automatically downloading the Gradle binary from Gradle's repository
@rem This is implemented similarly to the Maven Wrapper's download routine.
SET DISTRIBUTION_LOCATION=""
SET DISTRIBUTION_URL=""
for /f "usebackq tokens=1,2 delims==" %%A in ("%APP_HOME%\gradle\%WRAPPER_LOCATION%\gradle-wrapper.properties") do (
    if "%%A"=="distributionRepo" SET DISTRIBUTION_LOCATION=%%B
    if "%%A"=="distributionUrl" SET DISTRIBUTION_URL=%%B
)
SET DISTRIBUTION_SOURCE=%DISTRIBUTION_LOCATION%/%DISTRIBUTION_URL%
SET DISTRIBUTION_TARGET=%APP_HOME%\gradle\%WRAPPER_LOCATION%\%DISTRIBUTION_URL%
if not %DISTRIBUTION_LOCATION%=="" (
    if exist "%DISTRIBUTION_TARGET%" (
        if "%GRADLEW_VERBOSE%" == "true" (
            echo Found %DISTRIBUTION_TARGET%
        )
    ) else (
        if "%GRADLEW_VERBOSE%" == "true" (
            echo Couldn't find %DISTRIBUTION_TARGET%, downloading it ...
            echo Downloading from: %DISTRIBUTION_SOURCE%
        )
    
        powershell -Command "&{"^
            "$webclient = new-object System.Net.WebClient;"^
            "if (-not ([string]::IsNullOrEmpty('%GRADLEW_USERNAME%') -and [string]::IsNullOrEmpty('%GRADLEW_PASSWORD%'))) {"^
            "$webclient.Credentials = new-object System.Net.NetworkCredential('%GRADLEW_USERNAME%', '%GRADLEW_PASSWORD%');"^
            "}"^
            "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%DISTRIBUTION_SOURCE%', '%DISTRIBUTION_TARGET%')"^
            "}"
        if "%GRADLEW_VERBOSE%" == "true" (
            echo Finished downloading %DISTRIBUTION_TARGET%
        )
    )
)
@rem End of extension

@REM Download and validate the Gradle wrapper (Byte Buddy edit)
SET WRAPPER_URL=""
SET WRAPPER_HASH=""
FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%APP_HOME%\gradle\%WRAPPER_LOCATION%\gradle-wrapper.properties") DO (
    IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
    IF "%%A"=="wrapperHash" SET WRAPPER_HASH=%%B
)
SET WRAPPER_TARGET="%APP_HOME%\gradle\%WRAPPER_LOCATION%\gradle-wrapper.jar"
if not %WRAPPER_URL%=="" (
    if exist "%WRAPPER_URL%" (
        if "%GRADLEW_VERBOSE%" == "true" (
            echo Found %WRAPPER_URL%
        )
    ) else (
        if "%GRADLEW_VERBOSE%" == "true" (
            echo Couldn't find %WRAPPER_TARGET%, downloading it ...
            echo Downloading from: %WRAPPER_URL%
        )

        powershell -Command "&{"^
            "$webclient = new-object System.Net.WebClient;"^
            "if (-not ([string]::IsNullOrEmpty('%GRADLEW_USERNAME%') -and [string]::IsNullOrEmpty('%GRADLEW_PASSWORD%'))) {"^
            "$webclient.Credentials = new-object System.Net.NetworkCredential('%GRADLEW_USERNAME%', '%GRADLEW_PASSWORD%');"^
            "}"^
            "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $webclient.DownloadFile('%WRAPPER_URL%', '%WRAPPER_TARGET%')"^
            "}"
        if "%GRADLEW_VERBOSE%" == "true" (
            echo Finished downloading %WRAPPER_TARGET%
        )
    )
)
IF NOT %WRAPPER_HASH%=="" (
    powershell -Command "&{"^
       "$CHECKSUM = (Invoke-Expression \"certUtil -hashfile '%APP_HOME%\gradle\%WRAPPER_LOCATION%\gradle-wrapper.jar' SHA256\" | Select -Index 1);"^
       "If('%WRAPPER_HASH%' -ne $CHECKSUM){"^
       "  Write-Output 'Error: Failed to validate Maven checksum extension SHA-256, it might be compromised';"^
       "  Write-Output 'Investigate or delete %MAVEN_PROJECTBASEDIR%\.mvn\%WRAPPER_LOCATION%\maven-wrapper.jar to attempt a clean download.';"^
       "  exit 1;"^
       "}"^
       "}"
)

@rem Setup the command line
set CLASSPATH=%APP_HOME%\gradle\%WRAPPER_LOCATION%\gradle-wrapper.jar

@rem Execute Gradle
if %GRADLE_LEGACY%=="true" (
  "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain --build-file=build.legacy.gradle %*
) else (
  "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
)

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%GRADLE_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
