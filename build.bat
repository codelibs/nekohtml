@echo off
if "%JAVA_HOME%" == "" goto error
if not exist lib\xalan.jar goto requirements
set LOCALCLASSPATH=%JAVA_HOME%\lib\tools.jar;%JAVA_HOME%\jre\lib\rt.jar;lib\xml-apis.jar;lib\xalan.jar;lib\xercesImpl.jar;lib\ant.jar
"%JAVA_HOME%\bin\java" -classpath "%LOCALCLASSPATH%" org.apache.tools.ant.Main %1 %2 %3 %4 %5
goto end
:error
echo error: JAVA_HOME not found in your environment.
goto end
:requirements
echo error: Missing required jar files.
echo.
echo Please download Xalan2 from http://xml.apache.org/dist/xalan-j/
echo and place the following files in the lib/ directory:
echo.
echo    xml-apis.jar
echo    xalan.jar
echo    xercesImpl.jar
echo.
echo Please download Xerces2 from http://xml.apache.org/dist/xerces-j/
echo and place the following files in the lib/ directory:
echo.
echo    xercesSamples.jar
echo.
echo Ant is also required. Download Ant from the following URL
echo http://jakarta.apache.org/ant/index.html and place the ant.jar
echo file in the lib/ directory.
if not exist lib md lib
goto end
:end
set LOCALCLASSPATH=
@echo on
