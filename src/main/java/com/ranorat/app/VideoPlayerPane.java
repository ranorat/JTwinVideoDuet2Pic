package com.ranorat.app;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

//個別動画プレイヤーのUI・回転・元解像度スナップショット取得
public class VideoPlayerPane extends VBox {

    private final VideoPlayerController controller;
    private final MediaView mediaView;
    private final StackPane canvasPane;
    private final Slider slider;
    private final Label lblTime;
    private double currentAngle = 0;
    private boolean isDragging = false;
    private Runnable onTimeUpdatedCallback;

    // ★ 再生が最後まで完了したかどうかのフラグ
    private boolean hasEnded = false;

    public boolean hasEnded() {
        return hasEnded;
    }

    public void setHasEnded(boolean hasEnded) {
        this.hasEnded = hasEnded;
    }

    // ★ 停止中、または再生終了状態であるかを管理する自前フラグ
    private boolean isStoppedOrEnded = true;

    public VideoPlayerPane(String titlePrefix, Stage stage) {
        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);
        
        canvasPane = new StackPane(mediaView);
        canvasPane.setStyle("-fx-background-color: black;");
        canvasPane.setPrefSize(500, 350);
        canvasPane.setMinWidth(100);
        canvasPane.setMinHeight(100);
        canvasPane.setMaxWidth(Double.MAX_VALUE);
        canvasPane.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(canvasPane, Priority.ALWAYS);
        
        mediaView.fitWidthProperty().bind(canvasPane.widthProperty());
        mediaView.fitHeightProperty().bind(canvasPane.heightProperty());

        controller = new VideoPlayerController(mediaView);

        Button btnLoad = new Button(titlePrefix + " 読込");
        Button btnPlay = new Button("再生/停止");
        Button btnPrev = new Button("? コマ戻し");
        Button btnNext = new Button("コマ送り ?");
        
        slider = new Slider();
        lblTime = new Label("00:00 (Frame: 1)");

        btnLoad.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(titlePrefix + "の動画を選択");
            chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("動画ファイル (*.mp4, *.ts, *.mpg, *.mpeg)", "*.mp4", "*.ts", "*.mpg", "*.mpeg"),
                new FileChooser.ExtensionFilter("すべてのファイル", "*.*")
            );
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                loadSpecificFile(file);
            }
        });

        btnPlay.setOnAction(e -> {
            // ★ 再生しきっている（終了している）場合は、個別ボタンを押しても再生させない
            if (hasEnded) {
                return;
            }

            controller.togglePlay();
            // 状態が切り替わったためフラグを更新
            isStoppedOrEnded = (controller.getStatus() != MediaPlayer.Status.PLAYING);
            updateDisplayInfo();
        });

        btnPrev.setOnAction(e -> {
            // ★ 再生しきった状態であってもなくても、コマ戻し時は「常に停止状態」を維持する
            isStoppedOrEnded = true;

            // 再生しきった状態（末尾）からコマ戻しする場合の処理
            if (hasEnded) {
                hasEnded = false;         // 終了状態を解除
                
                // または末尾ピタリを指しているため、まずは1コマ分手前の時間に強制補正してあげる。
                double fMillis = controller.getFrameDuration().toMillis();
                double targetTime = Math.max(0, slider.getValue() - fMillis);
                controller.seek(Duration.millis(targetTime));
                controller.stepFrame(false);
            } else {
                // 通常時のコマ戻し
                controller.seek(Duration.millis(slider.getValue()));
                controller.stepFrame(false);
            }

            // シークやコマ戻しによって勝手に再生が走らないよう、明示的にポーズをかける
            controller.pause();
            updateDisplayInfo();
        });

        btnNext.setOnAction(e -> {
            // 再生しきった状態のときは、コマ送りは完全にガードして動かさない
            if (hasEnded) {
                return; 
            }
            controller.seek(Duration.millis(slider.getValue()));
            controller.stepFrame(true);
//            hasEnded = false; // 操作により終了状態を解除
            updateDisplayInfo();
        });

        slider.setOnMousePressed(e -> {
            isDragging = true;
            controller.pause();
        });

        slider.setOnMouseReleased(e -> {
            isDragging = false;
            controller.seek(Duration.millis(slider.getValue()));
            hasEnded = false; // 操作により終了状態を解除

            // ★ 停止・終了状態であれば、シーク後に強制的にポーズして再生を封じる
            if (isStoppedOrEnded) {
                controller.pause();
            }

            updateDisplayInfo();
        });

        slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (isDragging) {
                controller.seek(Duration.millis(newVal.doubleValue()));
                hasEnded = false; // ドラッグ中も終了状態を解除

                // ★ ドラッグ中も停止・終了状態であれば強制ポーズ
                if (isStoppedOrEnded) {
                    controller.pause();
                }

                updateDisplayInfo();
            }
        });

        HBox row1 = new HBox(5, btnLoad, btnPlay);
        HBox row2 = new HBox(5, btnPrev, btnNext);
        row1.setAlignment(Pos.CENTER);
        row2.setAlignment(Pos.CENTER);

        VBox controls = new VBox(5, row1, row2, slider, lblTime);
        controls.setPadding(new Insets(5));

        this.getChildren().addAll(canvasPane, controls);
        this.setPadding(new Insets(5));
        this.setAlignment(Pos.CENTER);
    }

    // ★ 外部（MainApp等）から停止・終了状態フラグを変更するためのセッター
    public void setStoppedOrEnded(boolean stoppedOrEnded) {
        this.isStoppedOrEnded = stoppedOrEnded;
    }

    public void rotate(boolean clockwise) {
        currentAngle = (currentAngle + (clockwise ? 90 : -90)) % 360;
        if (currentAngle < 0) currentAngle += 360;
        mediaView.setRotate(currentAngle);
        
        if (Math.abs(currentAngle % 180) == 90) {
            mediaView.fitWidthProperty().bind(canvasPane.heightProperty());
            mediaView.fitHeightProperty().bind(canvasPane.widthProperty());
        } else {
            mediaView.fitWidthProperty().bind(canvasPane.widthProperty());
            mediaView.fitHeightProperty().bind(canvasPane.heightProperty());
        }
    }

    public void updateDisplayInfo() {
        Duration time = controller.getCurrentTime();
        if (!isDragging && controller.getTotalDuration().greaterThan(Duration.ZERO)) {
            slider.setValue(time.toMillis());
        }
        
        long frame = controller.getCurrentFrame();
        lblTime.setText(String.format("%s (Frame: %d)", TimeUtils.formatDuration(time), frame));

        if (onTimeUpdatedCallback != null) {
            onTimeUpdatedCallback.run();
        }
    }

    public void setOnTimeUpdatedCallback(Runnable callback) { this.onTimeUpdatedCallback = callback; }

    public void loadSpecificFile(File file) {
        controller.loadMedia(file, 
            () -> {
                slider.setMax(controller.getTotalDuration().toMillis());
                isStoppedOrEnded = true; // 新規読込時は停止状態
                hasEnded = false; // 新規読込時は終了状態を解除

                // ★ 個別プレイヤー単体で最後まで再生しきった時の処理
                var player = controller.getMediaPlayer();
                if (player != null) {
                    player.setOnEndOfMedia(() -> {
                        javafx.application.Platform.runLater(() -> {
                            isStoppedOrEnded = true;
                            hasEnded = true;
                            controller.pause();
                            updateDisplayInfo();
                        });
                    });
                }

                updateDisplayInfo();
            },
            time -> updateDisplayInfo()
        );
    }

    public File getCurrentFile() { return controller.getCurrentFile(); }
    public VideoPlayerController getController() { return controller; }
    public MediaView getMediaView() { return mediaView; }

    public WritableImage captureSnapshotAtOriginalResolution() {
        if (mediaView.getMediaPlayer() == null || mediaView.getMediaPlayer().getMedia() == null) {
            return null;
        }
        Media media = mediaView.getMediaPlayer().getMedia();
        double origW = media.getWidth();
        double origH = media.getHeight();

        if (origW <= 0 || origH <= 0) {
            return mediaView.snapshot(null, null);
        }

        mediaView.fitWidthProperty().unbind();
        mediaView.fitHeightProperty().unbind();

        boolean isRotated = Math.abs(currentAngle % 180) == 90;
        if (isRotated) {
            mediaView.setFitWidth(origH);
            mediaView.setFitHeight(origW);
        } else {
            mediaView.setFitWidth(origW);
            mediaView.setFitHeight(origH);
        }

        WritableImage rawImage = mediaView.snapshot(null, null);

        if (isRotated) {
            mediaView.fitWidthProperty().bind(canvasPane.heightProperty());
            mediaView.fitHeightProperty().bind(canvasPane.widthProperty());
        } else {
            mediaView.fitWidthProperty().bind(canvasPane.widthProperty());
            mediaView.fitHeightProperty().bind(canvasPane.heightProperty());
        }

        int targetW = isRotated ? (int) origH : (int) origW;
        int targetH = isRotated ? (int) origW : (int) origH;

        if (rawImage != null && ((int) rawImage.getWidth() != targetW || (int) rawImage.getHeight() != targetH)) {
            PixelReader reader = rawImage.getPixelReader();
            int actualW = Math.min((int) rawImage.getWidth(), targetW);
            int actualH = Math.min((int) rawImage.getHeight(), targetH);
            
            WritableImage trimmedImage = new WritableImage(targetW, targetH);
            PixelWriter writer = trimmedImage.getPixelWriter();
            
            for (int y = 0; y < actualH; y++) {
                for (int x = 0; x < actualW; x++) {
                    writer.setArgb(x, y, reader.getArgb(x, y));
                }
            }
            return trimmedImage;
        }

        return rawImage;
    }
}
