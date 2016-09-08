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

rem check if wasabi machine is running, if not: return, else: integration tests
if "" == "%1" (
    for /f %%R in ('docker-machine status wasabi') do if not "%%R"=="Running" (
        call :error Wasabi is not running, please use bin\wasabi.bat start first.
        goto :eof
    )
    call :integration_tests
)

rem test individual modules
:read_params
    if "" == "%1" goto :eof
    call :unit_test %1
    
    shift
    goto :read_params
call :info Tests done.

goto :eof

rem FUNCTION: Runs the unit tests for the provided module.
:unit_test
    call :info Running unit tests for %1.
    for /f %%P in ('powershell -Command "\"%1\" -replace \"-\", \"\""') do (
        mvn "-Dtest=com.intuit.wasabi.%%P.**" test -pl modules\%1 --also-make -DfailIfNoTests=false -q
    )
    call :info Unit tests for %1 done.
    goto :eof

rem FUNCTION: Runs the integration tests.
:integration_tests
    call :info Running integration tests.

    rem fix time delays in case they exist
    for /f "usebackq" %%I in (`ruby -e "puts Time.now.utc.strftime(%%Q{%%Y%%m%%d%%H%%M.%%S})"`) do docker-machine ssh wasabi "sudo date --set %%I" 1>nul 2>&1

    rem run integration tests
    cd modules\functional-test\target
    for /f %%J in ('dir /b *-with-dependencies.jar') do (
		for /f %%I in ('docker-machine ip wasabi') do (
			java -Dapi.server.name=%%I:8080 -Duser.name=admin -Duser.password=admin -Ddatabase.url=jdbc:mysql://%%I/wasabi -classpath classes;%%J org.testng.TestNG -d ..\..\..\functional-test.log classes\testng.xml
		)
    )
    call :info Integration tests done.
    goto :eof

rem FUNCTION: Logs the parameters as DEBUG.
:debug
    rem call :log [DEBUG] %*
    call :log [DEBUG] %* >> wasabi_windows.log
    goto :eof

rem FUNCTION: Logs the parameters as INFO.
:info
    call :log [INFO] %*
    call :log [INFO] %* >> wasabi_windows.log
    goto :eof

rem FUNCTION: Logs the parameters as ERROR.
:error
    call :log [ERROR] %* 1>&2
    call :log [ERROR] %* >> wasabi_windows.log
    goto :eof

rem FUNCTION: Logs the parameters.
:log
    for /f "tokens=*" %%D in ('date /t') do (
        for /f "tokens=*" %%T in ('time /t') do echo %%D%%T  %*
    )
    goto :eof
