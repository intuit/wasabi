@echo off

rem ###############################################################################
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
rem ###############################################################################

rem list of dependencies to install
set choco_packages=docker 
rem docker-machine boot2docker
rem jdk8 nodejs.install maven python2

rem test if administrator (see http://stackoverflow.com/a/11995662)
net session >nul 2>&1
if not %errorLevel% == 0 (
  echo Are you an administrator? 1>&2
  echo For the bootstrap you need an elevated prompt. 1>&2
  goto end_of_file
)

rem enable hyper v
dism.exe /Online /Enable-Feature:Microsoft-Hyper-V /All /NoRestart 1>nul
if not %errorLevel% == 0 (
  echo Can not enable Hyper-V. Try it from "Turn Windows features on or off." 1>&2
  goto end_of_file
) else (
  echo Enabled Hyper-V. You might need to restart.
  echo Also make sure Hyper-V is enabled in your BIOS.
)

rem install chocolatey
echo Trying to find Chocolatey
if not exist C:\ProgramData\chocolatey\choco.exe (
  echo Installing Chocolatey
  powershell -NoProfile -ExecutionPolicy Bypass -Command "iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))" && SET PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin
  if errorlevel 1 (
    echo Can not install chocolatey 1>&2
    goto end_of_file
  )
  rem make sure choco writes its config
  C:\ProgramData\chocolatey\choco.exe
) else (
  echo Found Chocolatey.
)

rem install dependencies
for %%p in (%choco_packages%) do (
  C:\ProgramData\chocolatey\choco.exe upgrade %%p -y
)

rem create vm
rem powershell -NoProfile -ExecutionPolicy Bypass -Command "New-VM -Name 'wasabi-windows' -MemoryStartupBytes 512MB -NoVHD"
powershell -NoProfile -ExecutionPolicy Bypass -Command "New-VMSwitch -SwitchName 'VirtualSwitchWasabi' -SwitchType Internal"
docker-machine --debug create wasabi --driver hyperv --hyperv-virtual-switch "VirtualSwitchWasabi" --hyperv-memory "800" 

rem --hyperv-boot2docker-url "file:\\C:\Program Files\Boot2Docker for Windows\boot2docker.iso" 


:end_of_file


