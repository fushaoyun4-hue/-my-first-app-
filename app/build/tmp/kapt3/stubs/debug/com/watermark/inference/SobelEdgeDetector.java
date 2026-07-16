package com.watermark.inference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Sobel 边缘检测
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0015\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J$\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u00042\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\t\u00a8\u0006\u000b"}, d2 = {"Lcom/watermark/inference/SobelEdgeDetector;", "", "()V", "detect", "", "Lcom/watermark/inference/RectF;", "pixels", "", "w", "", "h", "app_debug"})
public final class SobelEdgeDetector {
    @org.jetbrains.annotations.NotNull()
    public static final com.watermark.inference.SobelEdgeDetector INSTANCE = null;
    
    private SobelEdgeDetector() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.util.List<com.watermark.inference.RectF> detect(@org.jetbrains.annotations.NotNull()
    int[] pixels, int w, int h) {
        return null;
    }
}