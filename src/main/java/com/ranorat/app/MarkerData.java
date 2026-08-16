package com.ranorat.app;

import javafx.util.Duration;

//フレームマーカーのデータ構造
public class MarkerData {
    public Duration timeL;
    public long frameL;
    public Duration timeR;
    public long frameR;
    public boolean isSaved = false;
}