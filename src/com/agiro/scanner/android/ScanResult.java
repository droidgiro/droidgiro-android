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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;

/**
* This class is supposed to decode the provided camera bitmap. It finds black
* continuous pixels, adds them as rects in a list, resizes them to match a
* reference bitmap with a known character and compares them pixel for pixel.
* The most matching character bitmap is translated to a string.
*
* This should probably be converted to use something like BinaryBitmap like
* the zxing barcode scanner does.
*/
public class ScanResult {

    private final String TAG = "Agiro.ScanResult";
    private int bmpWidth;
    private int bmpHeight;
    private int black = -16777216;
    private String rawResult = null;

    private List<Bitmap> resList;

    private Bitmap debugBmp = null;

    public ScanResult(Bitmap rawBmp) {
        ScanResources scanResources;
        scanResources = CaptureActivity.getScanResources();
        resList = scanResources.getReferenceList();
        decode(rawBmp);
    }

    private void decode(Bitmap bmp){
        bmpHeight = bmp.getHeight();
        bmpWidth = bmp.getWidth();
        Bitmap sbmp = Normalizer.scaleOnly(bmp, 0.3f);
        sbmp = Normalizer.scaleTranslate(sbmp, 100.f);
        List<Rect> rects = getSections(sbmp);
        //list to be decoded must have 4 or more possible characters
        if (rects.size() > 3) {
            List<Bitmap> bmpList = getScaledBitmapList(bmp, rects);
            debugBmp = drawDebugBitmap(bmpList);
            rawResult = getChars(bmpList);
        }
    }

    public String getResultString() {
        return rawResult;
    }

    /**
     * Finds continuous black pixels in a bitmap and adds their coordinates
     * to a list of rectangles.
     */
    private List<Rect> getSections(Bitmap bmp) {
        //find which cols contain black pixels
        List<int[]> cols = new ArrayList<int[]>();
        int lastEmpty = -2;
        int lastFull = -2;
        int lastLeft = -2;
        int lastRight = -2;
        int lastTop = -2;
        int lastBottom = -2;
        for(int x = 0; x < bmpWidth; ++x) {
            int blackPixels = 0;
            for(int y = 0; y < bmpHeight; ++y) {
                int p = bmp.getPixel(x,y);
                if (p == black) {
                    ++blackPixels;
                }
            }
            //save cols with more than one black pixel (TODO: should not be a
            if (blackPixels > 1) {
                lastFull = x;
            } else {
                lastEmpty = x;
            }
            if ((lastFull-1 == lastEmpty)||(x == 0 && x == lastFull)){
                lastLeft = x;
            } else if ((lastEmpty-1 == lastFull)||(x == bmpWidth-1 && x == lastFull)){
                lastRight = x;
                int[] c = new int[] {lastLeft, lastRight};
                cols.add(c);
            }
        }
        //find which rows contain black pixels
        lastEmpty = -2;
        lastFull = -2;
        List<Rect> rects = new ArrayList<Rect>();
        ListIterator<int[]> li = cols.listIterator();
        while (li.hasNext()) {
            int[] coords = li.next();
            for(int y = 0; y < bmpHeight; ++y) {
                int blackPixels = 0;
                for(int x = coords[0]; x <= coords[1]; ++x) {
                    int p = bmp.getPixel(x,y);
                    if (p == black) {
                        ++blackPixels;
                    }
                }
                //save rows with more than one black pixel
                if (blackPixels > 1) {
                    lastFull = y;
                } else {
                    lastEmpty = y;
                }
                if ((lastFull-1 == lastEmpty)||(y == 0 && y == lastFull)){
                    lastTop = y;
                } else if ((lastEmpty-1 == lastFull)||(y == bmpWidth-1 && y == lastFull)){
                    lastBottom = y;
                    Rect rect = new Rect(coords[0], lastTop, coords[1], lastBottom);
                    //ignore rects of wrong size or shape
                    if ((rect.width() > bmpWidth/100)&&
                        (rect.width() < (bmpWidth/10))&&
                        (rect.height() > (bmpHeight/5))&&
                        (rect.height() < bmpHeight-(bmpHeight/8))&&
                        (rect.height() > rect.width()) ) {
                        rects.add(rect);
                    }
                }
            }
        }
        return rects;
    }

    /**
    * Creates a list of several bitmaps from the bitmap to be decoded and scales
    * them uniformly. Their coorinates should be provided as a list of rects,
    * like from the method getSections.
    */
    private List<Bitmap> getScaledBitmapList(Bitmap bmp, List rects) {
        List<Bitmap> bmpList = new ArrayList<Bitmap>();
        ListIterator<Rect> li = rects.listIterator();
        while (li.hasNext()) {
            Rect rect = li.next();
            int[] pixels;
            pixels = new int[rect.height() * rect.width()];
            bmp.getPixels(pixels, 0, rect.width(), rect.left, rect.top, rect.width(),
            rect.height());
            Bitmap rectBmp = Bitmap.createBitmap(pixels, 0, rect.width(),
            rect.width(), rect.height(), Bitmap.Config.ARGB_8888);
            //scale to 16*24px to match reference bitmaps but should be
            //changable in some variable.
            Bitmap sbmp = Bitmap.createScaledBitmap(rectBmp, 16, 24, true);
            sbmp = Normalizer.scaleOnly(sbmp, 0.3f);
            sbmp = Normalizer.scaleTranslate(sbmp, 100.f);
            bmpList.add(sbmp);
        }
        return bmpList;
    }

    /**
    * Draws a single bitmap from the list of bitmaps for use in contrast debug.
    */
    private Bitmap drawDebugBitmap(List bmpList) {
        Bitmap resultBmp = Bitmap.createBitmap(16 * bmpList.size(), 24, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resultBmp);
        ListIterator<Bitmap> li = bmpList.listIterator();
        int i = 0;
        while (li.hasNext()) {
            Bitmap bmp = li.next();
            Rect srcRect = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
            Rect dstRect = new Rect(srcRect);
            if (i != 0){
            dstRect.offset(i*bmp.getWidth(), 0);
            }
            i++;
            canvas.drawBitmap(bmp, srcRect, dstRect, null);

        }
        return resultBmp;
    }

    /**
    * Compare the list of uniformly scaled bitmaps to the reference bitmaps and
    * translate the best matching to a string.
    */
    private String getChars(List bmpList) {
        StringBuffer result = new StringBuffer();
        ListIterator<Bitmap> li = bmpList.listIterator();
        while (li.hasNext()) {
            Bitmap bmp = li.next();
            int bestIndex = -1;
            int bestScore = -1;
            ListIterator<Bitmap> rli = resList.listIterator();
            while (rli.hasNext()) {
                int truePixels = 0;
                int falsePixels = 0;

                Bitmap rbmp = rli.next();
                int rbmp_i = rli.nextIndex();
                for(int x = 0; x < bmp.getWidth(); ++x) {
                    for(int y = 0; y < bmp.getHeight(); ++y) {
                        int rp = rbmp.getPixel(x,y);
                        int p = bmp.getPixel(x,y);
                        if (rp == p){
                            truePixels++;
                        } else {
                            falsePixels++;
                        }
                    }
                }
                if (truePixels > bestScore){
                    bestScore = truePixels;
                    bestIndex = rbmp_i;
                }
            }
            int character = bestIndex-1;
            if (character == 10) {
                result.append((char)35);
            }
            else if (character == 11) {
                result.append((char)62);
            } else {
                result.append(Integer.toString(character));
            }
        }
        Log.d(TAG,"Complete Result: "+result.toString());
        return result.toString();
    }

    public Bitmap getDebugBitmap() {
        return debugBmp;
    }

}
