@echo off

cd /d %~dp0
echo %cd%

java --module-path ".\javafx-sdk\lib" --add-modules javafx.controls,javafx.media,javafx.swing --enable-native-access=javafx.graphics,javafx.media -jar .\JTwinVideoDuet2Pic.jar

pause
