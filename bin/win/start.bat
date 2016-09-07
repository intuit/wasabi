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

rem always start docker
call :start_docker

rem start all
if "" == "%1" (
    call :start_cassandra
    call :start_mysql
    call :start_wasabi
    goto :eof
)

rem start individual components
:read_params
    if "" == "%1" goto :eof
    call :start_%1
    
    shift
    goto :read_params


goto :eof

rem FUNCTION: Checks the status of cassandra and starts it if needed.
:start_cassandra
    call :info Starting cassandra
    docker ps -a | findstr /c:wasabi-cassandra 1>nul 2>nul
    if errorlevel 1 (
        docker run --net=wasabinet --name wasabi-cassandra --privileged=true -p 9042:9042 -p 9160:9160 -d cassandra:2.1
    ) else (
        docker start wasabi-cassandra 1>nul
    )
    goto :eof

rem FUNCTION: Checks the status of mysql and starts it if needed.
:start_mysql
    call :info Starting mysql
    
    docker ps -a | findstr /c:wasabi-mysql 1>nul 2>nul
    if errorlevel 1 (
        docker run --net=wasabinet --name wasabi-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=mypass -e MYSQL_DATABASE=wasabi -e MYSQL_USER=readwrite -e MYSQL_PASSWORD=readwrite -d mysql:5.6
    ) else (
        docker start wasabi-mysql 1>nul
    )
    goto :eof
    
rem FUNCTION: Checks the status of wasabi and starts it if needed.
:start_wasabi
    call :info Starting wasabi
    docker ps -a | findstr /c:wasabi-main 1>nul 2>nul
    if errorlevel 1 (
        git status 1>nul 2>nul
        if errorlevel 0 (
            for /f %%H in ('"git rev-parse --short=8 HEAD"') do (
                docker build -t wasabi-main:%%H target\app
                docker create --net=wasabinet --name wasabi-main -p 8080:8080 -p 8090:8090 -p 8180:8180 -e WASABI_CONFIGURATION="-DnodeHosts=wasabi-cassandra -Ddatabase.url.host=wasabi-mysql" wasabi-main:%%H
            )
        ) else (
            docker build -t wasabi-main:windemo target\app
            docker create --net=wasabinet --name wasabi-main -p 8080:8080 -p 8090:8090 -p 8180:8180 -e WASABI_CONFIGURATION="-DnodeHosts=wasabi-cassandra -Ddatabase.url.host=wasabi-mysql" wasabi-main:windemo
        )
    )
    docker start wasabi-main 1>nul
    goto :eof

rem FUNCTION: Checks the status of the docker machine and starts it if needed.
:start_docker
    call :debug Checking for docker machine
    docker-machine ls -q | findstr /c:wasabi 1>nul 2>nul
    if errorlevel 1 (
        call :create_docker_machine
    ) else (
        call :debug Docker machine exists. Checking it's status.
        docker-machine status wasabi | findstr /c:Running 1>nul 2>nul
        if errorlevel 1 (
            call :info Docker machine restarting.
            docker-machine restart wasabi
        ) else (
            call :debug Docker machine is already running.
        )
    )
    call :set_docker_env
    call :create_docker_net
    goto :eof

rem FUNCTION: Set docker environment variables correctly.
:set_docker_env
    call :debug Setting environment variables to use docker.
    
    rem Thanks to setlocal this won't enable the right environment variables.
    rem Instead we hope for now for the best (i.e. people don't tinker with the 
    rem docker settings) and set them globally for the next shells and reload
    rem the env variables.)
    for /f "tokens=*" %%I in ('"C:\ProgramData\chocolatey\lib\docker-machine\bin\docker-machine.exe" env wasabi') do %%I
    
    for /f %%I in ('"C:\ProgramData\chocolatey\lib\docker-machine\bin\docker-machine.exe" ip wasabi') do set DOCKER_IP=%%I
    rem set the env variables also globally (they are refreshed after wasabi.bat
    rem automatically!)
    setx DOCKER_TLS_VERIFY 1 1>nul
    setx DOCKER_HOST tcp://%DOCKER_IP%:2376 1>nul
    setx DOCKER_CERT_PATH %USERPROFILE%\.docker\machine\machines\wasabi 1>nul
    setx DOCKER_MACHINE_NAME wasabi 1>nul
    goto :eof
    
rem FUNCTION: Create a docker machine
:create_docker_machine
    call :info Creating docker machine
    docker-machine create --driver virtualbox wasabi
    call :info Done creating docker-machine
    goto :eof

rem FUNCTION: Create a docker network for wasabi
:create_docker_net
    call :info Checking for existing network.
    docker network ls | findstr /c:wasabinet 1>nul
    if errorlevel 1 (
        call :info Creating network.
        docker network create --driver bridge wasabinet 1>nul
        call :info Network created.
    ) else (
        call :info Network exists.
    )
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
