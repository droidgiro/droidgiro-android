/*
 * Copyright (C) 2011 aGiro authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.agiro.scanner.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;

/**
* This class is some kind of test/placeholder. A histogram should be used to
* determine contrast values.
*/
public final class Normalizer {

    private static final String TAG = "Normalizer:";

    public static Bitmap scaleOnly(Bitmap bmpOriginal, float contrast) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
            float scale = contrast + 1.f;
               float translate = (-.5f * scale + .5f) * 255.f;
            cm.set(new float[] {
                   scale, 0, 0, 0, 0,
                   0, scale, 0, 0, 0,
                   0, 0, scale, 0, 0,
                   0, 0, 0, 1, 0 });
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public static Bitmap scaleTranslate(Bitmap bmpOriginal, float contrast) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
            float scale = contrast + 1.f;
               float translate = (-.5f * scale + .5f) * 255.f;
            cm.set(new float[] {
                   scale, 0, 0, 0, translate,
                   0, scale, 0, 0, translate,
                   0, 0, scale, 0, translate,
                   0, 0, 0, 1, 0});
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
}
