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

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal EnableDelayedExpansion

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
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
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar


@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
@rem End local scope for the variables with windows NT shell
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 (
	for /f "delims=" %%G in ('git rev-parse --short HEAD 2^>NUL') do set "GIT_SHA=%%G"
	if "!GIT_SHA!"=="" if exist "%APP_HOME%\.git\HEAD" (
		set /p "HEAD_CONTENT="<"%APP_HOME%\.git\HEAD"
		if /i "!HEAD_CONTENT:~0,4!"=="ref:" (
			set "REF_PATH=!HEAD_CONTENT:~5!"
			if exist "%APP_HOME%\.git\!REF_PATH!" (
				set /p "GIT_SHA="<"%APP_HOME%\.git\!REF_PATH!"
			) else if exist "%APP_HOME%\.git\packed-refs" (
				for /f "usebackq tokens=1" %%R in (`findstr /R /C:"[0-9a-f][0-9a-f]* !REF_PATH!" "%APP_HOME%\.git\packed-refs"`) do set "GIT_SHA=%%R"
			)
		) else (
			set "GIT_SHA=!HEAD_CONTENT!"
		)
	)
	if "!GIT_SHA!"=="" set "GIT_SHA=unknown"
	goto reportSuccess
)

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:reportSuccess
if not defined GIT_SHA set "GIT_SHA=unknown"
set "GIT_SHA=!GIT_SHA:~0,10!"
echo Gradle build completed at commit !GIT_SHA!
:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
