package com.ranorat.app;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

//左右同時の画像保存・ファイルダイアログ処理
public class SnapshotHelper {
    private File lastSaveDirectory = null;

    public void saveBothSnapshots(Stage stage, VideoPlayerPane paneL, VideoPlayerPane paneR) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("左右の画像を保存");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG画像 (*.png)", "*.png"));
        
        File initialDir = null;
        if (lastSaveDirectory != null && lastSaveDirectory.exists()) {
            initialDir = lastSaveDirectory;
        } else if (paneL.getCurrentFile() != null && paneL.getCurrentFile().getParentFile().exists()) {
            initialDir = paneL.getCurrentFile().getParentFile();
        } else if (paneR.getCurrentFile() != null && paneR.getCurrentFile().getParentFile().exists()) {
            initialDir = paneR.getCurrentFile().getParentFile();
        }

        if (initialDir != null) {
            chooser.setInitialDirectory(initialDir);
        }

        String nameL = "left";
        File fileL = paneL.getCurrentFile();
        if (fileL != null) {
            String fname = fileL.getName();
            int idx = fname.lastIndexOf('.');
            nameL = (idx > 0) ? fname.substring(0, idx) : fname;
        }

        String nameR = "right";
        File fileR = paneR.getCurrentFile();
        if (fileR != null) {
            String fname = fileR.getName();
            int idx = fname.lastIndexOf('.');
            nameR = (idx > 0) ? fname.substring(0, idx) : fname;
        }

        long frameL = paneL.getController().getCurrentFrame();
        long frameR = paneR.getController().getCurrentFrame();

        String defaultFileName = String.format("%s_f%d_%s_f%d.png", nameL, frameL, nameR, frameR);
        chooser.setInitialFileName(defaultFileName);
        
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            lastSaveDirectory = file.getParentFile();

            String path = file.getAbsolutePath();
            String basePath = path.substring(0, path.lastIndexOf('.'));
            String ext = path.substring(path.lastIndexOf('.'));

            File targetFileL = new File(basePath + "_left" + ext);
            File targetFileR = new File(basePath + "_right" + ext);

            try {
                if (paneL.getMediaView().getMediaPlayer() != null) {
                    WritableImage imgL = paneL.captureSnapshotAtOriginalResolution();
                    if (imgL != null) {
                        ImageIO.write(SwingFXUtils.fromFXImage(imgL, null), "png", targetFileL);
                    }
                }
                if (paneR.getMediaView().getMediaPlayer() != null) {
                    WritableImage imgR = paneR.captureSnapshotAtOriginalResolution();
                    if (imgR != null) {
                        ImageIO.write(SwingFXUtils.fromFXImage(imgR, null), "png", targetFileR);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
