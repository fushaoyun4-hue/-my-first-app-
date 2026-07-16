package com.watermark.inference;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import dagger.hilt.android.qualifiers.ApplicationContext;
import kotlinx.coroutines.Dispatchers;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * LaMa-Fourier ONNX 鎺ㄧ悊寮曟搸
 *
 * 璐熻矗鍔犺浇妯″瀷銆佸崟甯у幓姘村嵃鎺ㄧ悊
 * 鏀鎸 INT8 閲忓寲妯″瀷锛屽唴瀛樺崰鐢?< 500MB
 */
@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000j\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0010\u0014\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0016\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0010\u0012\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001B\u0011\b\u0007\u0012\b\b\u0001\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J$\u0010\r\u001a\u000e\u0012\u0004\u0012\u00020\u000f\u0012\u0004\u0012\u00020\u000f0\u000e2\u0006\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u000fH\u0002J \u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u000fH\u0002J&\u0010\u0016\u001a\u00020\u00152\u0006\u0010\u0017\u001a\u00020\u000f2\u0006\u0010\u0018\u001a\u00020\u000f2\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001aH\u0002J\u0018\u0010\u001c\u001a\u00020\u001d2\u0006\u0010\u001e\u001a\u00020\u00132\u0006\u0010\u001f\u001a\u00020 H\u0002J \u0010!\u001a\u00020\u00152\u0006\u0010\u001e\u001a\u00020\u00132\u0006\u0010\u0010\u001a\u00020\u000f2\u0006\u0010\u0011\u001a\u00020\u000fH\u0002J\u0018\u0010\"\u001a\u00020#2\b\b\u0002\u0010$\u001a\u00020\bH\u0086@\u00a2\u0006\u0002\u0010%J$\u0010&\u001a\u00020\u00152\u0006\u0010\'\u001a\u00020\u00152\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\u001b0\u001aH\u0086@\u00a2\u0006\u0002\u0010(J\u0010\u0010)\u001a\u00020*2\u0006\u0010+\u001a\u00020\bH\u0002J\u0006\u0010,\u001a\u00020#R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0005\u001a\u0004\u0018\u00010\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\bX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\bX\u0082D\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006-"}, d2 = {"Lcom/watermark/inference/LaMaEngine;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "env", "Lai/onnxruntime/OrtEnvironment;", "inputName", "", "maskName", "outputName", "session", "Lai/onnxruntime/OrtSession;", "alignToModelInput", "Lkotlin/Pair;", "", "w", "h", "bitmapToFloatArray", "", "bitmap", "Landroid/graphics/Bitmap;", "buildMask", "width", "height", "masks", "", "Landroid/graphics/RectF;", "createTensor", "Lai/onnxruntime/OrtSession$RunOptions;", "data", "shape", "", "floatArrayToBitmap", "init", "", "modelPath", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "inpaint", "image", "(Landroid/graphics/Bitmap;Ljava/util/List;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "loadModelFromAssets", "", "assetPath", "release", "app_debug"})
public final class LaMaEngine {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.Nullable()
    private ai.onnxruntime.OrtSession session;
    @org.jetbrains.annotations.Nullable()
    private ai.onnxruntime.OrtEnvironment env;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String inputName = "input";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String maskName = "mask";
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String outputName = "output";
    
    @javax.inject.Inject()
    public LaMaEngine(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    /**
     * 鍒濆嬪寲妯″瀷锛堥栨¤皟鐢锛?     * @param modelPath assets/models/lama-fourier.onnx
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object init(@org.jetbrains.annotations.NotNull()
    java.lang.String modelPath, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * 浠?assets 鍔犺浇妯″瀷鏂囦欢鍒扮紦瀛?
     */
    private final byte[] loadModelFromAssets(java.lang.String assetPath) {
        return null;
    }
    
    /**
     * 瀵瑰崟甯у浘鐗囪繘琛屽幓姘村嵃
     *
     * @param image 杈撳叆鍘熷浘锛圔itmap锛?     * @param masks 姘村嵃/鏂囧瓧鍖哄煙鍒楄〃锛堝潗鏍囦负鐩稿逛?image 鐨勬瘮渚?0~1锛?     * @return 淇澶嶅悗鐨 Bitmap
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object inpaint(@org.jetbrains.annotations.NotNull()
    android.graphics.Bitmap image, @org.jetbrains.annotations.NotNull()
    java.util.List<? extends android.graphics.RectF> masks, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super android.graphics.Bitmap> $completion) {
        return null;
    }
    
    /**
     * 灏嗗氫?mask 鍚堝苟涓轰竴涓浜屽?mask Bitmap
     */
    private final android.graphics.Bitmap buildMask(int width, int height, java.util.List<? extends android.graphics.RectF> masks) {
        return null;
    }
    
    /**
     * 灏?Bitmap 杞涓哄綊涓鍖?float 鏁扮粍 [BCHW 鏍煎紡]
     */
    private final float[] bitmapToFloatArray(android.graphics.Bitmap bitmap, int w, int h) {
        return null;
    }
    
    /**
     * 灏嗘ā鍨嬭緭鍑?float 鏁扮粍杞鍥 Bitmap
     */
    private final android.graphics.Bitmap floatArrayToBitmap(float[] data, int w, int h) {
        return null;
    }
    
    /**
     * ONNX float tensor 鍒涘缓
     */
    private final ai.onnxruntime.OrtSession.RunOptions createTensor(float[] data, long[] shape) {
        return null;
    }
    
    /**
     * 璋冩暣灏哄稿?64 鐨勫嶆暟锛圤NNX 妯″瀷瑕佹眰锛?
     */
    private final kotlin.Pair<java.lang.Integer, java.lang.Integer> alignToModelInput(int w, int h) {
        return null;
    }
    
    public final void release() {
    }
}