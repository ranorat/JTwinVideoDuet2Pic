@echo off

cd /d %~dp0

echo %cd%

echo コンパイル
rem javac -Xlint:unchecked -d .\classes .\src\main\java\com\ranorat\app\*.java
javac -Xlint:unchecked --module-path ".\jar\javafx-sdk\lib" --add-modules javafx.controls,javafx.media,javafx.swing -d .\classes .\src\main\java\com\ranorat\app\*.java
echo;

pause

echo java実行
cd .\classes
java --module-path "..\jar\javafx-sdk\lib" --add-modules javafx.controls,javafx.media,javafx.swing --enable-native-access=javafx.graphics,javafx.media com.ranorat.app.MainApp
echo;

pause

echo jarファイル作成
jar cfm ..\jar\JTwinVideoDuet2Pic.jar .\MANIFEST.MF .\com\ranorat\app\*.class
echo;

pause

echo jarファイル実行

java --module-path "..\jar\javafx-sdk\lib" --add-modules javafx.controls,javafx.media,javafx.swing --enable-native-access=javafx.graphics,javafx.media -jar ..\jar\JTwinVideoDuet2Pic.jar
echo;

pause

