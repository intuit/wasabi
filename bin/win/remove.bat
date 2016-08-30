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


call :info Removing Wasabi images
docker rmi wasabi-main >nul

call :info Removing Wasabi containers
docker rm -fv wasabi-cassandra >nul
docker rm -fv wasabi-mysql >nul
docker rm -fv wasabi-main >nul

call :info Removing Wasabi network
docker network rm wasabinet >nul

call :info Removing wasabi machine
docker-machine rm -f wasabi >nul

call :info Clearing environment variables
setx DOCKER_TLS_VERIFY "" >nul
setx DOCKER_HOST "" >nul
setx DOCKER_CERT_PATH "" >nul
setx DOCKER_MACHINE_NAME "" >nul
RefreshEnv

call :info Done


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
