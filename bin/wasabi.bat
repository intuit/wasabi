@echo off 

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


call :init_logging

if "%1"=="" (
    call :invalid_command_issued
    exit /b 1
)

rem Enable delayed expansion for the parser
setlocal enabledelayedexpansion

rem Define valid commands
set valid_commands=
for /f %%a in ('dir /b bin\win') do set valid_commands=!valid_commands! %%~na


call :info Processing Wasabi commands
set remaining_commands=%*
:read_commands
    rem Pop currently first command (split at space)
    for /f "tokens=1*" %%C in ("%remaining_commands%") do (
        set current_command=%%C
        call :debug Current command: !current_command!
        set remaining_commands=%%D
        call :debug Remaining commands: !remaining_commands!
        
        rem Process current command. Split command and parameters at :
        for /f "tokens=1* delims=:" %%F in ("!current_command!") do (
            set command_name=%%F
            call :debug Command name: !command_name!
            set parameters=%%G
            call :debug Command parameters: !parameters!
            
            rem Check command validity
            for %%V in (%valid_commands%) do (
                if /I "!command_name!"=="%%V" goto :valid_command
            )
            call :invalid_command_issued !command_name!
            exit /b 1
            :valid_command
            set space_parameters=
            
            rem Read command paramaters from parameter string
            :read_parameters
                rem Pop first parameter (split at , )
                for /f "delims=, tokens=1*" %%P in ("!parameters!") do (
                    set param=%%P
                    call :debug Current parameter: !param!
                     set parameters=%%Q
                    call :debug Remaining parameters: !parameters!
                    
                    rem Append parameter to parameter list - now space separated
                    set space_parameters=!space_parameters! !param!
                    call :debug Collected parameters: !space_parameters!
                )
                if defined parameters goto :read_parameters
            
            set assembled_command=bin/win/!command_name!.bat!space_parameters!
            call :debug Assembled: !assembled_command!
            call !assembled_command!
        )
    )
    if defined remaining_commands goto read_commands
call :debug Wasabi done

endlocal
RefreshEnv 2>nul 1>nul

goto :eof
rem LABEL: Print error and show usage, then exit with exit code 1.
:invalid_command_issued
     call :error Invalid command: %1
     call "bin/win/usage.bat"
     goto :eof

rem FUNCTION: Initializes the debug log.
:init_logging
    call :log === Wasabi Windows Log === > wasabi_windows.log
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
