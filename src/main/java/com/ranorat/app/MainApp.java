package com.ranorat.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.List;

//アプリケーションのエントリーポイントおよび全体のレイアウト・同期管理
public class MainApp extends Application {
    private VideoPlayerPane paneL;
    private VideoPlayerPane paneR;
    private HBox viewerBox;
    private Label lblDiff;
    private boolean isSwapped = false;

    private final MarkerData[] markers = new MarkerData[5];
    private final SnapshotHelper snapshotHelper = new SnapshotHelper();

    // アプリケーション情報
    private static final String APP_NAME = "ツインビデオデュエット2Pic";
    private static final String APP_VERSION = "v1.0.0";
    private static final String APP_COPYRIGHT = "JTwinVideoDuet2Pic Copyright c 2026 ranorat";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(APP_NAME);

        for (int i = 0; i < 5; i++) {
            markers[i] = new MarkerData();
        }

        // --- メニューバーの作成 (バージョン情報用) ---
        MenuBar menuBar = createMenuBar(primaryStage);

        paneL = new VideoPlayerPane("左", primaryStage);
        paneR = new VideoPlayerPane("右", primaryStage);

        paneL.prefWidthProperty().bind(primaryStage.widthProperty().divide(2).subtract(15));
        paneR.prefWidthProperty().bind(primaryStage.widthProperty().divide(2).subtract(15));

        paneL.setMaxWidth(Double.MAX_VALUE);
        paneR.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(paneL, Priority.ALWAYS);
        HBox.setHgrow(paneR, Priority.ALWAYS);

        viewerBox = new HBox(10, paneL, paneR);
        viewerBox.setAlignment(Pos.CENTER);
        viewerBox.setPadding(new Insets(10));
        HBox.setHgrow(viewerBox, Priority.ALWAYS);

        paneL.setOnTimeUpdatedCallback(this::updateDiffDisplay);
        paneR.setOnTimeUpdatedCallback(this::updateDiffDisplay);

        viewerBox.setOnDragOver(event -> {
            if (event.getGestureSource() != viewerBox && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        viewerBox.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                handleDroppedFiles(db.getFiles());
            }
            event.setDropCompleted(true);
            event.consume();
        });

        Button btnSwap = new Button("左右入れ替え");
        btnSwap.setOnAction(e -> swapPanes());

        Button btnSyncPlay = new Button("同時 再生/一時停止");
        Button btnSyncPrev = new Button("同時 コマ戻し");
        Button btnSyncNext = new Button("同時 コマ送り");

        Button btnRotateLeft = new Button("? 左回転");
        Button btnRotateRight = new Button("右回転 ?");
        btnRotateLeft.setOnAction(e -> {
            paneL.rotate(false);
            paneR.rotate(false);
        });
        btnRotateRight.setOnAction(e -> {
            paneL.rotate(true);
            paneR.rotate(true);
        });

        Button btnSaveBoth = new Button("左右同時 画像保存");
        btnSaveBoth.setOnAction(e -> snapshotHelper.saveBothSnapshots(primaryStage, paneL, paneR));

//        lblDiff = new Label("時間差: 0ms (フレーム差: 0)");
//        lblDiff.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

        // 修正後：最小幅や最大幅を持たせ、桁数が変わってもラベル自体の大きさがブレないようにする
        lblDiff = new Label("時間差: 0ms (フレーム差: 0)");
        lblDiff.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");
        lblDiff.setMinWidth(240); // 桁数が増えても崩れない十分な幅を確保
        lblDiff.setAlignment(Pos.CENTER); // 中央揃え

        btnSyncPlay.setOnAction(e -> {
            // ★ 片方でも再生しきっている（終了している）場合はボタンを押しても反応させない
            if (paneL.hasEnded() || paneR.hasEnded()) {
                return;
            }

            var ctrlL = paneL.getController();
            var ctrlR = paneR.getController();

            // MediaPlayer本体を取得
            var playerL = ctrlL.getMediaPlayer();
            var playerR = ctrlR.getMediaPlayer();

            // 既存の再生状態チェック
            boolean bothPlaying = (ctrlL.getStatus() == MediaPlayer.Status.PLAYING && 
                                   ctrlR.getStatus() == MediaPlayer.Status.PLAYING);
            if (bothPlaying) {
                ctrlL.pause();
                ctrlR.pause();
                paneL.setStoppedOrEnded(true);
                paneR.setStoppedOrEnded(true);
            } else {
                // 終了時の同期停止設定

                // ★ 左側が先に最後まで再生しきった場合の処理
                Runnable stopWhenLeftEnds = () -> {
                    Platform.runLater(() -> {
                        ctrlL.pause();
                        ctrlR.pause();
                        paneL.setStoppedOrEnded(true);
                        paneR.setStoppedOrEnded(true);
                        
                        paneL.setHasEnded(true); // 左は実際に終了した
                        // 右側はまだ終わっていないため paneR.setHasEnded は true にしない
                        
                        paneL.updateDisplayInfo();
                        paneR.updateDisplayInfo();
                    });
                };

                // ★ 右側が先に最後まで再生しきった場合の処理
                Runnable stopWhenRightEnds = () -> {
                    Platform.runLater(() -> {
                        ctrlL.pause();
                        ctrlR.pause();
                        paneL.setStoppedOrEnded(true);
                        paneR.setStoppedOrEnded(true);
                        
                        paneR.setHasEnded(true); // 右は実際に終了した
                        // 左側はまだ終わっていないため paneL.setHasEnded は true にしない
                        
                        paneL.updateDisplayInfo();
                        paneR.updateDisplayInfo();
                    });
                };

                if (playerL != null) playerL.setOnEndOfMedia(stopWhenLeftEnds);
                if (playerR != null) playerR.setOnEndOfMedia(stopWhenRightEnds);

                ctrlL.play();
                ctrlR.play();
                paneL.setStoppedOrEnded(false);
                paneR.setStoppedOrEnded(false);
                paneL.setHasEnded(false); // 再生開始時に終了フラグをクリア
                paneR.setHasEnded(false);

            }
            paneL.updateDisplayInfo();
            paneR.updateDisplayInfo();
        });

        btnSyncPrev.setOnAction(e -> {
            paneL.getController().stepFrame(false);
            paneR.getController().stepFrame(false);
            paneL.updateDisplayInfo();
            paneR.updateDisplayInfo();
            updateDiffDisplay();
        });

        btnSyncNext.setOnAction(e -> {
            paneL.getController().stepFrame(true);
            paneR.getController().stepFrame(true);
            paneL.updateDisplayInfo();
            paneR.updateDisplayInfo();
            updateDiffDisplay();
        });

        HBox syncButtonsRow = new HBox(10, btnSyncPrev, btnSyncNext);
        syncButtonsRow.setAlignment(Pos.CENTER);

        HBox rotateButtonsRow = new HBox(10, btnRotateLeft, btnRotateRight);
        rotateButtonsRow.setAlignment(Pos.CENTER);

        VBox markerBox = new VBox(4);
        markerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label markerTitle = new Label("フレームマーカー (全5個)");
        markerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        markerBox.getChildren().add(markerTitle);

        Label[] markerLabels = new Label[5];
        for (int i = 0; i < 5; i++) {
            final int index = i;
            HBox markerRow = new HBox(5);
            markerRow.setAlignment(Pos.CENTER_LEFT);

            Button btnMarkerSave = new Button("M" + (i + 1) + "保存");
            Button btnMarkerLoad = new Button("読込");
            Button btnMarkerClear = new Button("クリア");

            markerLabels[i] = new Label("未設定");
            markerLabels[i].setMinWidth(130);
            markerLabels[i].setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");

            btnMarkerSave.setOnAction(e -> {
                markers[index].timeL = paneL.getController().getCurrentTime();
                markers[index].frameL = paneL.getController().getCurrentFrame();
                markers[index].timeR = paneR.getController().getCurrentTime();
                markers[index].frameR = paneR.getController().getCurrentFrame();
                markers[index].isSaved = true;

                markerLabels[index].setText(String.format("L:f%d / R:f%d", markers[index].frameL, markers[index].frameR));
                markerLabels[index].setStyle("-fx-font-size: 10px; -fx-text-fill: #000000; -fx-font-weight: bold;");
            });

            btnMarkerLoad.setOnAction(e -> {
                if (markers[index].isSaved) {
                    paneL.getController().seek(markers[index].timeL);
                    paneR.getController().seek(markers[index].timeR);
                    paneL.updateDisplayInfo();
                    paneR.updateDisplayInfo();
                    updateDiffDisplay();
                }
            });

            btnMarkerClear.setOnAction(e -> {
                markers[index].isSaved = false;
                markers[index].timeL = null;
                markers[index].timeR = null;
                markerLabels[index].setText("未設定");
                markerLabels[index].setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");
            });

            markerRow.getChildren().addAll(btnMarkerSave, btnMarkerLoad, btnMarkerClear, markerLabels[i]);
            markerBox.getChildren().add(markerRow);
        }

        VBox actionBox = new VBox(6, btnSwap, btnSyncPlay, syncButtonsRow, rotateButtonsRow, btnSaveBoth, lblDiff);
        actionBox.setAlignment(Pos.CENTER);

        // --- 修正：BorderPaneをやめてHBoxで配置 ---
        // HBox(20) の 20 がマーカーとボタン群の間の距離です。ここを調整してください。
        HBox bottomLayout = new HBox(20, markerBox, actionBox);
        bottomLayout.setAlignment(Pos.CENTER); // 全体を中央寄せ
        bottomLayout.setPadding(new Insets(10));
        bottomLayout.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");


        HBox statusBar = new HBox();
        statusBar.setAlignment(Pos.CENTER_RIGHT);
        statusBar.setPadding(new Insets(5, 15, 5, 15));
        statusBar.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #bbbbbb; -fx-border-width: 1 0 0 0;");

        Label lblCopyright = new Label(APP_COPYRIGHT);
        lblCopyright.setStyle("-fx-font-size: 10px; -fx-text-fill: #555555;");
        statusBar.getChildren().add(lblCopyright);

        VBox bottomContainer = new VBox(0, bottomLayout, statusBar);

        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(viewerBox);
        root.setBottom(bottomContainer);

        Scene scene = new Scene(root, 1300, 810);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar createMenuBar(Stage ownerStage) {
        MenuBar menuBar = new MenuBar();
        Menu helpMenu = new Menu("ヘルプ");
        MenuItem aboutItem = new MenuItem("バージョン情報");
        
        aboutItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.initOwner(ownerStage);
            alert.setTitle("バージョン情報");
            alert.setHeaderText(APP_NAME + "  " + APP_VERSION);
            alert.setContentText(
                APP_COPYRIGHT + "\n\n" +
                "【使用ライブラリについて】\n" +
                "本ソフトウェアは JavaFX (OpenJFX) を使用しています。\n" +
                "JavaFX is licensed under the GNU General Public License (GPL) version 2 with Classpath Exception."
            );
            alert.showAndWait();
        });

        helpMenu.getItems().add(aboutItem);
        menuBar.getMenus().add(helpMenu);
        return menuBar;
    }

    private void updateDiffDisplay() {
        var ctrlL = paneL.getController();
        var ctrlR = paneR.getController();

        long frameL = ctrlL.getCurrentFrame();
        long frameR = ctrlR.getCurrentFrame();
        long frameDiff = frameL - frameR;

        double frameDurationMs = ctrlL.getFrameDuration().toMillis();
        long timeDiff = Math.round(frameDiff * frameDurationMs);

        if (frameDiff == 0) {
            timeDiff = 0;
        }

        lblDiff.setText(String.format("時間差: %d ms (フレーム差: L %s%d)", timeDiff, frameDiff >= 0 ? "+" : "", frameDiff));
    }

    private void handleDroppedFiles(List<File> files) {
        if (files.isEmpty()) return;
        if (files.size() == 1) {
            File f = files.get(0);
            paneL.loadSpecificFile(f);
            paneR.loadSpecificFile(f);
        } else {
            paneL.loadSpecificFile(files.get(0));
            paneR.loadSpecificFile(files.get(1));
        }
        updateDiffDisplay();
    }

    private void swapPanes() {
        viewerBox.getChildren().clear();
        if (isSwapped) {
            viewerBox.getChildren().addAll(paneL, paneR);
            isSwapped = false;
        } else {
            viewerBox.getChildren().addAll(paneR, paneL);
            isSwapped = true;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
