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

rem stop all
if "" == "%1" (
    call :stop_cassandra
    call :stop_mysql
    call :stop_wasabi
    call :stop_docker
    goto :eof
)

rem start individual components
:read_params
    if "" == "%1" goto :eof
    call :stop_%1
    
    shift
    goto :read_params

set running=docker-machine ls
echo %running%
if "%running%" == "" (
    call :stop_docker
)


goto :eof

rem FUNCTION: Stops cassandra
:stop_cassandra
    call :info Stopping cassandra.
    rem docker kill wasabi-cassandra
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
