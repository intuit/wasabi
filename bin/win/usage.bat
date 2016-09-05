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

echo.
echo Usage: 
echo     bin\wasabi.bat command1[:arg1[,arg2]...] ...
echo.
echo Commands:
echo     bootstrap  Installs Chocolatey and Wasabi dependencies
echo     build      Builds Wasabi and the UI
echo         wasabi     Builds Wasabi
echo         ui         Builds UI
echo     start      Starts all containers
echo         wasabi     Starts Wasabi only
echo         cassandra  Starts Cassandra only
echo         mysql      Starts MySQL only
echo     test       Runs the integration tests
echo         MODULENAME Runs the unit tests for module MODULENAME
echo     stop       Stops all containers
echo         wasabi     Stops Wasabi only
echo         cassandra  Stops Cassandra only
echo         mysql      Stops MySQL only
echo     resource   Only usable with arguments:
echo         ui         Opens the UI
echo         api        Opens the Swagger API documentation
echo         doc        Opens the JavaDocs
echo         mysql      Opens mysql in the mysql container
echo         cqlsh      Opens cqlsh in the cassandra container
echo     package    Generates deployable packages
echo     remove     Removes all containers
echo         wasabi     Removes Wasabi container
echo         cassandra  Removes Cassandra container
echo         mysql      Removes MySQL container
echo         image      Removes the Wasabi image file
echo         network    Removes the virtual network adapter
echo         complete   Removes everything
echo     usage      Prints this help text
echo     colorusage Prints this help text with semantic colors
echo.
echo Examples:
echo Builds the service, starts components, runs tests, stops wasabi:
echo     bin\wasabi.bat build start:mysql,wasabi test stop:wasabi
echo.
echo Runs the unit tests for the api module and builds packages:
echo     bin\wasabi.bat test:api package
echo.
