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


rem List of dependencies to install
setlocal 
set choco_packages=docker,docker-machine,jdk8,maven,nodejs.install,ruby,virtualbox --allowEmptyChecksums
set npm_packages=bower,grunt-cli,yo
set gem_packages=compass,fpm

rem Test if administrator (see http://stackoverflow.com/a/11995662)
net session >nul 2>&1
if not %errorLevel% == 0 (
    call :error You need administrator rights to bootstrap Wasabi.
    endlocal
    exit /b 1
)

rem install chocolatey
call :debug Trying to find Chocolatey
if not exist C:\ProgramData\chocolatey\choco.exe (
  call :info Installing Chocolatey
  powershell -NoProfile -ExecutionPolicy Bypass -Command "iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))" && SET PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin
  if errorlevel 1 (
      call :error Can not install Chocolatey
      endlocal
      exit /b 1
  )
  rem make sure choco writes its config
  C:\ProgramData\chocolatey\choco.exe
) else (
    call :debug Found Chocolatey
)

call :info Installing/Upgrading Chocolatey dependencies
set remaining_packages=%choco_packages%
:inst_choco_deps
    for /f "tokens=1* delims=," %%P in ("%remaining_packages%") do (
        set current_package=%%P
        set remaining_packages=%%Q
      
        call :debug Installing/Upgrading via choco - !current_package!
        cmd /c C:\ProgramData\chocolatey\choco.exe upgrade !current_package! -y
    )
    if defined remaining_packages goto :inst_choco_deps


call :info Installing/Upgrading Node.JS dependencies
set remaining_packages=%npm_packages%
:inst_npm_deps
    for /f "tokens=1* delims=," %%P in ("%remaining_packages%") do (
        set current_package=%%P
        set remaining_packages=%%Q
      
        call :debug Installing/Upgrading via npm - !current_package!
        cmd /c "C:\Program Files\nodejs\npm.cmd" -g install !current_package!
    )
    if defined remaining_packages goto :inst_npm_deps


call :info Installing/Upgrading Ruby dependencies
set remaining_packages=%gem_packages%
:inst_gem_deps
    for /f "tokens=1* delims=," %%P in ("%remaining_packages%") do (
        set current_package=%%P
        set remaining_packages=%%Q
      
        call :debug Installing/Upgrading via gem - !current_package!
        cmd /c C:\Tools\ruby23\bin\gem.cmd install !current_package!
    )
    if defined remaining_packages goto :inst_gem_deps

call :info Bootstrapping done. 


endlocal
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
