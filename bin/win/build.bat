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
 
rem start all
if "" == "%1" (
    call :build_wasabi
    call :build_ui
    call :build_files
    goto :eof
)

rem start individual components
:read_params
    if "" == "%1" goto :eof
    call :build_%1
    
    shift
    goto :read_params
call :build_files


goto :eof

rem FUNCTION: builds wasabi
:build_wasabi
    call :info Building Wasabi Service
    cmd /c "mvn -Pdevelopment clean test package javadoc:aggregate"
    goto :eof

rem FUNCTION: builds ui
:build_ui
    call :info Building UI
    pushd modules\ui
    cmd /c "call npm install & call %APPDATA%\npm\bower.cmd install & call %APPDATA%\npm\grunt.cmd build"
    popd
    goto :eof

rem FUNCTION: copies the build files to target\app to properly be used with docker
:build_files
    call :info Aggregating files
    
    rem wasabi files
    set app-dir=target\app
    set bin-dir=%app-dir%\bin
    set conf-dir=%app-dir%\conf
    set lib-dir=%app-dir%\lib
    rem ensure directories
    mkdir %app-dir% 2>nul
    mkdir %conf-dir% 2>nul
    mkdir %bin-dir% 2>nul
    mkdir %lib-dir% 2>nul
    for %%M in (analytics api assignment auditlog authentication authorization database email event eventlog export main repository user-directory) do (
        copy modules\%%M\target\classes\*.properties %conf-dir% 1>nul
    )
    copy modules\main\target\classes\logback_acccess.xml %conf-dir% /y 1>nul
    copy modules\main\target\classes\logback.xml %conf-dir% /y 1>nul
    copy modules\main\target\extra-resources\service\run %bin-dir% /y 1>nul
    copy modules\main\target\extra-resources\docker\wasabi\Dockerfile %app-dir% /y 1>nul
    copy modules\main\target\extra-resources\docker\wasabi\entrypoint.sh %app-dir% /y 1>nul
    copy modules\main\target\extra-resources\docker\wasabi\.dockerignore %app-dir% /y 1>nul
    rem The normal copy does not support * officially and creates only almost empty 
    rem thus we fall back to the powershell copy here.
    powershell -Command "Copy-item -path modules\main\target\wasabi-main-*-all.jar -destination %lib-dir%\ -force" 1>nul
    
    rem swagger doc files
    copy modules\api\target\generated\document.html modules\swagger-ui\target\swaggerui\ >nul
    mkdir modules\swagger-ui\target\swaggerui\swagger 2>nul
    mkdir modules\swagger-ui\target\swaggerui\swagger\swaggerjson 2>nul
    powershell -Command "Get-Content modules\api\target\generated\swagger-ui\swagger.json | %{$_ -replace 'localhost', '192.168.99.100'}" > modules\swagger-ui\target\swaggerui\swagger\swaggerjson\swagger.json
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
