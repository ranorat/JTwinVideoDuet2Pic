package com.ranorat.app;

import javafx.util.Duration;

//時間のフォーマット処理
public class TimeUtils {
    public static String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown()) return "00:00";
        int millis = (int) duration.toMillis();
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
