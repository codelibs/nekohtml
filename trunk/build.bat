@echo off
if "%JAVA_HOME%" == "" goto error
if not exist lib\xercesImpl.jar goto requirements
set LOCALCLASSPATH=%JAVA_HOME%\lib\tools.jar;%JAVA_HOME%\lib\classes.zip;.\lib\ant.jar;.\lib\xercesImpl.jar;.\lib\xmlParserAPIs.jar
"%JAVA_HOME%\bin\java" -Dant.home="./lib" -classpath "%LOCALCLASSPATH%" org.apache.tools.ant.Main %1 %2 %3 %4 %5
goto end
:error
echo error: JAVA_HOME not found in your environment.
goto end
:requirements
echo error: Missing required jar files.
echo.
echo Please download Xerces2 from http://xml.apache.org/dist/xerces-j/
echo and place the following files in the lib/ directory:
echo.
echo    xmlParserAPIs.jar
echo    xercesImpl.jar
echo.
echo Ant is also required to build NekoHTML. Download Ant from
echo http://jakarta.apache.org/ant/index.html and place the ant.jar
echo file in the lib/ directory.
if not exist lib md lib
goto end
:end
set LOCALCLASSPATH=
@echo on
