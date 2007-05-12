@echo off
if "%JAVA_HOME%" == "" goto error
if not exist lib\xalan.jar goto requirements
set LOCALCLASSPATH=%JAVA_HOME%\lib\tools.jar;%JAVA_HOME%\jre\lib\rt.jar;lib\xml-apis.jar;lib\xalan.jar;lib\xercesImpl.jar;lib\ant.jar;lib\jing.jar;lib\junit.jar
"%JAVA_HOME%\bin\java" -classpath "%LOCALCLASSPATH%" org.apache.tools.ant.Main %1 %2 %3 %4 %5
goto end
:error
echo error: JAVA_HOME not found in your environment.
goto end
:requirements
echo error: Missing required jar files.
echo.
echo The Ant tool is required. Download Ant from the following URL
echo http://jakarta.apache.org/ant/index.html and place the ant.jar
echo file in the lib/ directory.
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
echo If building ManekiNeko, James Clark's Jing Relax NG validator 
echo is also required. Please download the Jar file distribution 
echo from http://www.thaiopensource.com/relaxng/jing.html and place
echo the following file in the lib/ directory:
echo.
echo    jing.jar
echo.
if not exist lib md lib
goto end
:end
set LOCALCLASSPATH=
@echo on
