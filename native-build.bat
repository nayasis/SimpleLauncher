@echo off
REM ---------------------------------------------------------------------------
REM GraalVM native-image build for Windows.
REM
REM native-image drives cl.exe directly, so it needs the MSVC developer
REM environment (INCLUDE / LIB / PATH). Running gradlew from a plain shell fails
REM with "fatal error C1034: stdio.h", so initialize vcvars64 first.
REM
REM Requirements:
REM   - Liberica NIK 25 "full" in NATIVE_JAVA_HOME. The full bundle ships JavaFX
REM     as JDK modules together with the static libs (glass.lib, prism_d3d.lib,
REM     javafx_font.lib, javafx_iio.lib) that native-image links against. The
REM     plain NIK bundle lacks them and the link step fails with LNK2001 on
REM     JNI_OnLoad_glass and friends.
REM   - Visual Studio 2022 Build Tools with the C++ workload and Windows SDK
REM
REM Usage: native-build.bat [extra gradle args]
REM Output: build\native\nativeCompile\simplelauncher.exe
REM ---------------------------------------------------------------------------
setlocal

if "%NATIVE_JAVA_HOME%"=="" set "NATIVE_JAVA_HOME=%USERPROFILE%\.sdkman\candidates\java\25.0.4.1-nikfull"

if not exist "%NATIVE_JAVA_HOME%\bin\native-image.cmd" (
	echo [native-build] native-image not found under %NATIVE_JAVA_HOME%
	echo [native-build] install a GraalVM based JDK 25, e.g.
	echo [native-build]   https://bell-sw.com/pages/downloads/native-image-kit/ - bundle: full
	exit /b 1
)

set "VCVARS=%ProgramFiles(x86)%\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat"
if not exist "%VCVARS%" set "VCVARS=%ProgramFiles%\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
if not exist "%VCVARS%" (
	echo [native-build] vcvars64.bat not found. Install Visual Studio 2022 Build Tools
	echo [native-build] with the "Desktop development with C++" workload.
	exit /b 1
)

call "%VCVARS%" >nul
if errorlevel 1 (
	echo [native-build] failed to initialize the MSVC environment
	exit /b 1
)

REM vcvars64 changes the working directory, so return to the project root
cd /d "%~dp0."

set "JAVA_HOME=%NATIVE_JAVA_HOME%"
set "GRAALVM_HOME=%NATIVE_JAVA_HOME%"

echo [native-build] JAVA_HOME = %JAVA_HOME%
echo [native-build] building...

call "%~dp0gradlew.bat" nativeCompile %*
exit /b %errorlevel%
