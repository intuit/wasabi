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
    
rem on empty no resource to be started
if "" == "%1" (
    call :error resource not specified
    goto :eof
)

rem start individual components
:read_params
    if "" == "%1" goto :eof
    call :resource_%1
    
    shift
    goto :read_params


goto :eof

rem FUNCTION: Runs the ui and opens it. Determines if wasabi
rem           runs locally or inside the docker machine to
rem           change the Gruntfile accordingly.
:resource_ui
    call :info Opening UI
    pushd modules\ui
    start grunt serve
    popd
    goto :eof

rem FUNCTION: Opens the api reference
:resource_api
    call :info Opening API reference
    pushd modules\swagger-ui\target\swaggerui
    start ruby -run -e httpd . -p 9090
    start http://localhost:9090/
    popd
    goto :eof
    
rem FUNCTION: Opens the javadoc
:resource_doc
    call :info Opening JavaDocs
    start target\site\apidocs\index.html
    goto :eof

rem FUNCTION: Opens mysql
:resource_mysql
    call :info Connecting to mysql
    start docker exec --interactive --tty wasabi-mysql mysql -uroot -pmypass
    goto :eof
    
rem FUNCTION: Opens cqlsh
:resource_cassandra
    call :info Connecting to cassandra
    start docker exec --interactive --tty wasabi-cassandra cqlsh
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
