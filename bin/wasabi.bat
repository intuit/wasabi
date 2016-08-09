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

rem define valid commands
set valid_commands=(bootstrap test usage)

rem loop over passed commands and call subsequent files
rem until no commands left to parse or an invalid command is
rem issued.
:read_params
    if [%1]==[] goto end_of_file
    
    rem check if valid command was issued, otherwise stop
    rem and show usage
    for %%i in %valid_commands% do (
        if /I %%i == %1 (
           goto valid_command_issued
        )
    )
    echo Invalid command: %1 1>&2
    call "bin/win/usage.bat"
    goto end_of_file
    :valid_command_issued

    rem call valid command file
    call "bin/win/%1.bat"

    rem next parameter
    shift
    goto read_params

:end_of_file