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

rem default: just remove all containers
if "" == "%1" (
    call :remove_cassandra
    call :remove_mysql
    call :remove_wasabi
    call :info Done
    goto :eof
)

rem remove everything: containers, volumes, images, network, ...
if "complete" == "%1" (
    call :remove_cassandra v
    call :remove_mysql v
    call :remove_wasabi v
    call :remove_image
    call :remove_network
    call :remove_machine
    call :clear_env
    call :info Done
    goto :eof
)

rem remove individual components
:read_params
    if "" == "%1" goto :eof
    call :remove_%1
    
    shift
    goto :read_params
call :info Done


goto :eof

rem FUNCTION: Removes the cassandra container - pass v to remove the volume as well
:remove_cassandra
    call :info Removing Wasabi's cassandra
    for /f "tokens=1" %%C in ('"docker ps -a | findstr /c:wasabi-cassandra"') do call docker rm -f%1 %%C >nul
    goto :eof

rem FUNCTION: Removes the mysql container - pass v to remove the volume as well
:remove_mysql
    call :info Removing Wasabi's mysql
    for /f "tokens=1" %%C in ('"docker ps -a | findstr /c:wasabi-mysql"') do call docker rm -f%1 %%C >nul
    goto :eof

rem FUNCTION: Removes the wasabi container - pass v to remove the volume as well
:remove_wasabi
    call :info Removing Wasabi
    for /f "tokens=1" %%C in ('"docker ps -a | findstr /c:wasabi-main"') do call docker rm -f%1 %%C >nul
    goto :eof

rem FUNCTION: Removes the wasabi image
:remove_image
    call :info Removing Wasabi images
    for /f "tokens=2" %%T in ('"docker images | findstr /c:wasabi-main"') do call docker rmi wasabi-main:%%T >nul
    goto :eof

rem FUNCTION: Removes the wasabi network
:remove_network
    call :info Removing Wasabi network
    call docker network rm wasabinet >nul
    goto :eof

rem FUNCTION: Removes the wasabi machine
:remove_machine
    call :info Removing wasabi machine
    call docker-machine rm -f wasabi >nul
    goto :eof
    
rem FUNCTION: Clears the docker machine variables
:clear_env
    call :info Clearing environment variables
    setx DOCKER_TLS_VERIFY "" >nul
    setx DOCKER_HOST "" >nul
    setx DOCKER_CERT_PATH "" >nul
    setx DOCKER_MACHINE_NAME "" >nul
    call RefreshEnv
    goto :eof


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
