package com.ranorat.app;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.io.File;
import java.util.function.Consumer;

//動画の再生・フレーム・シーク制御  
public class VideoPlayerController {
    private MediaPlayer mediaPlayer;
    private final MediaView mediaView;
    
    private Duration frameDuration = Duration.millis(1000.0 / 60.0);
    private long currentFrameIndex = 1; 
    private double baseTimeMillis = -1;
    private File currentFile;

    public VideoPlayerController(MediaView mediaView) {
        this.mediaView = mediaView;
    }

    public void loadMedia(File file, Runnable onReadyCallback, Consumer<Duration> onTimeChanged) {
        try {
            this.currentFile = file;
            String uri = file.toURI().toString();
            Media media = new Media(uri);

            media.setOnError(() -> showAlert("メディアエラー", "動画の解析に失敗しました。"));

            MediaPlayer newPlayer = new MediaPlayer(media);
            newPlayer.setOnReady(() -> {
                currentFrameIndex = 1;
                baseTimeMillis = -1;
                if (onReadyCallback != null) onReadyCallback.run();
            });

            newPlayer.setOnEndOfMedia(() -> {
/*
                currentFrameIndex = 1;
                newPlayer.seek(baseTimeMillis >= 0 ? Duration.millis(baseTimeMillis) : Duration.ZERO);
                newPlayer.pause();
*/
                // 自動リセットを無効化

            });

            newPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                double fMillis = frameDuration.toMillis();
                if (fMillis > 0) {
                    double millis = newVal.toMillis();
                    if (baseTimeMillis < 0) {
                        baseTimeMillis = millis;
                    }
                    double elapsed = millis - baseTimeMillis;
                    if (elapsed < 0) elapsed = 0;

                    if (elapsed < fMillis / 2.0) {
                        currentFrameIndex = 1;
                    } else {
                        currentFrameIndex = Math.round(elapsed / fMillis) + 1;
                    }
                }
                if (onTimeChanged != null) onTimeChanged.accept(newVal);
            });

            if (this.mediaPlayer != null) {
                this.mediaPlayer.dispose();
            }
            this.mediaPlayer = newPlayer;
            this.mediaView.setMediaPlayer(this.mediaPlayer);

        } catch (Exception e) {
            showAlert("読み込みエラー", "ファイルのオープンに失敗しました: " + e.getMessage());
        }
    }

    public void setFps(double fps) {
        if (fps > 0) {
            this.frameDuration = Duration.millis(1000.0 / fps);
        }
    }

    public void stepFrame(boolean forward) {
        if (mediaPlayer == null) return;
        mediaPlayer.pause();

        double fMillis = frameDuration.toMillis();
        if (fMillis > 0 && baseTimeMillis >= 0) {
            double currentMillis = mediaPlayer.getCurrentTime().toMillis();
            double elapsed = currentMillis - baseTimeMillis;
            if (elapsed < 0) elapsed = 0;

            if (elapsed < fMillis / 2.0) {
                currentFrameIndex = 1;
            } else {
                currentFrameIndex = Math.round(elapsed / fMillis) + 1;
            }
        }

        if (forward) {
            currentFrameIndex++;
        } else {
            currentFrameIndex = Math.max(1, currentFrameIndex - 1);
        }

        double targetMillis;
        if (baseTimeMillis < 0) {
            baseTimeMillis = 0;
        }
        targetMillis = baseTimeMillis + ((currentFrameIndex - 1) * fMillis);

        Duration target = Duration.millis(targetMillis);

        if (mediaPlayer.getTotalDuration() != null && target.greaterThan(mediaPlayer.getTotalDuration())) {
            target = mediaPlayer.getTotalDuration();
            if (fMillis > 0 && baseTimeMillis >= 0) {
                currentFrameIndex = Math.round((target.toMillis() - baseTimeMillis) / fMillis) + 1;
            }
        }

        mediaPlayer.seek(target);
    }

    public void seek(Duration duration) {
        if (mediaPlayer != null) {
            mediaPlayer.seek(duration);
            double targetMillis = duration.toMillis();
            double fMillis = frameDuration.toMillis();
            
            if (fMillis > 0) {
                if (targetMillis < fMillis / 2.0) {
                    currentFrameIndex = 1;
                    baseTimeMillis = 0;
                } else {
                    long calcFrame = Math.round(targetMillis / fMillis) + 1;
                    currentFrameIndex = Math.max(1, calcFrame);
                    baseTimeMillis = targetMillis - ((currentFrameIndex - 1) * fMillis);
                }
            }
        }
    }

    public long getCurrentFrame() { return currentFrameIndex; }
    public Duration getFrameDuration() { return frameDuration; }
    public File getCurrentFile() { return currentFile; }
    public MediaPlayer.Status getStatus() { return mediaPlayer != null ? mediaPlayer.getStatus() : MediaPlayer.Status.UNKNOWN; }
    public void togglePlay() { if (mediaPlayer == null) return; if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) mediaPlayer.pause(); else mediaPlayer.play(); }
    public void play() { if (mediaPlayer != null) mediaPlayer.play(); }
    public void pause() { if (mediaPlayer != null) mediaPlayer.pause(); }
    public Duration getCurrentTime() { return mediaPlayer != null ? mediaPlayer.getCurrentTime() : Duration.ZERO; }
    public Duration getTotalDuration() { return mediaPlayer != null && mediaPlayer.getTotalDuration() != null ? mediaPlayer.getTotalDuration() : Duration.ZERO; }
    public MediaPlayer getMediaPlayer() { return mediaPlayer; }
    private void showAlert(String t, String m) { Platform.runLater(() -> { Alert a = new Alert(Alert.AlertType.ERROR, m); a.setTitle(t); a.showAndWait(); }); }
}
