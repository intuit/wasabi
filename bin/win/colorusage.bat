rem ############################################################################
rem # Copyright 2016 Intuit
rem #
rem # Licensed under the Apache License, Version 2.0 (the "License");
rem # you may not use this file except in compliance with the License.
rem # You may obtain a copy of the License at
rem #
rem #     http://www.apache.org/licenses/LICENSE-2.0
rem #
rem # Unless required by applicable law or agreed to in writing, software
rem # distributed under the License is distributed on an "AS IS" BASIS,
rem # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem # See the License for the specific language governing permissions and
rem # limitations under the License.
rem ############################################################################

rem Prepare for colored output (see http://stackoverflow.com/a/10407642 )
setlocal disabledelayedexpansion
set q=^"

rem normal color
set nc=0d
rem command color
set cc=0b
rem argument color
set ac=0e
rem required color
set rc=0C
rem ignore color
set ic=08
rem wasabi color
set wc=0a

echo.
call :color %nc% "Usage:"&echo.

call :color %wc% "    bin\wasabi.bat "
call :color %cc% "command1"
call :color %ic% "["
call :color %rc% ":"
call :color %ac% "arg1"
call :color %ic% "["
call :color %rc% ","
call :color %ac% "arg2"
call :color %ic% "]...] ..."&echo.&echo.

call :color %nc% "Commands:"&echo.

call :color %cc% "    bootstrap  "
echo Installs Chocolatey and Wasabi dependencies

call :color %cc% "    build      "
echo Builds Wasabi and the UI
call :color %ac% "        wasabi     "
echo Builds Wasabi
call :color %ac% "        ui         "
echo Builds the UI

call :color %cc% "    start      "
echo Starts all containers
call :color %ac% "        wasabi     "
echo Starts Wasabi only
call :color %ac% "        cassandra  "
echo Starts Cassandra only
call :color %ac% "        mysql      "
echo Starts MySQL only

call :color %cc% "    test       "
echo Runs the integration tests
call :color %ac% "        MODULENAME "
echo Runs the unit tests for module MODULENAME

call :color %cc% "    stop       "
echo Stops all containers
call :color %ac% "        wasabi     "
echo Stops Wasabi only
call :color %ac% "        cassandra  "
echo Stops cassandra only
call :color %ac% "        mysql      "
echo Stops MySQL only

call :color %cc% "    resource   "
echo Only usable with arguments:
call :color %ac% "        ui         "
echo Opens the UI
call :color %ac% "        api        "
echo Opens the Swagger API documentation
call :color %ac% "        doc        "
echo Opens the JavaDocs
call :color %ac% "        mysql      "
echo Opens mysql in the mysql container
call :color %ac% "        cassandra  "
echo Opens cqlsh in the cassandra container

call :color %cc% "    package    "
echo Generates deployable packages

call :color %cc% "    remove     "
echo Removes all containers
call :color %ac% "        wasabi     "
echo Removes Wasabi container
call :color %ac% "        cassandra  "
echo Removes Cassandra container
call :color %ac% "        mysql      "
echo Removes MySQL container
call :color %ac% "        image      "
echo Removes the Wasabi image file
call :color %ac% "        network    "
echo Removes the virtual network adapter
call :color %ac% "        complete    "
echo Removes everything

call :color %cc% "    usage      "
echo Prints the help text
call :color %cc% "    colorusage "
echo Prints this colored help text

echo.
call :color %nc% "Examples:"&echo.
echo Builds the service, starts components, runs tests, stops wasabi: &echo.
call :color %wc% "    bin\wasabi.bat"
call :color %cc% " build "
call :color %cc% "start"
call :color %rc% ":"
call :color %ac% "mysql"
call :color %rc% ","
call :color %ac% "wasabi "
call :color %cc% "test "
call :color %cc% "stop"
call :color %rc% ":"
call :color %ac% "wasabi "
echo.&echo.
echo Runs the unit tests for the api module and builds packages: 
call :color %wc% "    bin\wasabi.bat"
call :color %cc% " test"
call :color %rc% ":"
call :color %ac% "api "
call :color %cc% "package"
echo.
echo.


endlocal
goto :eof

rem Colored output (see http://stackoverflow.com/a/10407642 )
:color
setlocal enableDelayedExpansion
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:colorPrint Color  Str  [/n]
setlocal
set "s=%~2"
call :colorPrintVar %1 s %3
exit /b

:colorPrintVar  Color  StrVar  [/n]
if not defined DEL call :initColorPrint
setlocal enableDelayedExpansion
pushd .
':
cd \
set "s=!%~2!"
:: The single blank line within the following IN() clause is critical - DO NOT REMOVE
for %%n in (^"^

^") do (
  set "s=!s:\=%%~n\%%~n!"
  set "s=!s:/=%%~n/%%~n!"
  set "s=!s::=%%~n:%%~n!"
)
for /f delims^=^ eol^= %%s in ("!s!") do (
  if "!" equ "" setlocal disableDelayedExpansion
  if %%s==\ (
    findstr /a:%~1 "." "\'" nul
    <nul set /p "=%DEL%%DEL%%DEL%"
  ) else if %%s==/ (
    findstr /a:%~1 "." "/.\'" nul
    <nul set /p "=%DEL%%DEL%%DEL%%DEL%%DEL%"
  ) else (
    >colorPrint.txt (echo %%s\..\')
    findstr /a:%~1 /f:colorPrint.txt "."
    <nul set /p "=%DEL%%DEL%%DEL%%DEL%%DEL%%DEL%%DEL%"
  )
)
if /i "%~3"=="/n" echo(
popd
exit /b


:initColorPrint
for /f %%A in ('"prompt $H&for %%B in (1) do rem"') do set "DEL=%%A %%A"
<nul >"%temp%\'" set /p "=."
subst ': "%temp%" >nul
exit /b


:cleanupColorPrint
2>nul del "%temp%\'"
2>nul del "%temp%\colorPrint.txt"
>nul subst ': /d
exit /b