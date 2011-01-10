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
 * A scanner object with methods to determine coordinates of black pixels in
 * the provided bitmap and compare sections of those black pixels to a list
 * of reference bitmaps.
 *
 * @author wulax
 */
public class ScanResult {

    private final String TAG = "Agiro.ScanResult";

    /**
     * The width of the bitmap to be scanned.
     */
    private static int bmpWidth;
    /**
     * The height of the bitmap to be scanned.
     */
    private static int bmpHeight;
    /**
     * The width the scanned character bitmaps should be scaled to.
     * Read from the reference bitmaps.
     */
    private int scaleWidth;
    /**
     * The width the scanned character bitmaps should be scaled to.
     * Read from the reference bitmaps.
     */
    private int scaleHeight;
    /**
     * The color black in RGB images expressed as integer.
     */
    private int black = -16777216;
    /**
     * The minimum amount of black pixels required for a column to be registerd.
     */
    protected int minBlackPerCol = 2;
    /**
     * The minimum amount of black pixels required for a row to be registered.
     */
    protected int minBlackPerRow = 2;
    /**
     * The minimum width a possible character must have to be registered.
     * Expressed as fraction of width of the bitmap to be scanned.
     */
    protected float charMinWidthFraction = 0.01f;
    /**
     * The maximum width a possible character must have to be registered.
     * Expressed as fraction of width of the bitmap to be scanned.
     */
    protected float charMaxWidthFraction = 0.1f;
    /**
     * The minimum height a possible character must have to be
     * registered. Expressed as fraction of height of the bitmap to be scanned.
     */
    protected float charMinHeightFraction = 0.2f;
    /**
     * The maximum height a possible character must have to be
     * registered. Expressed as fraction of height of the bitmap to be scanned.
     */
    protected float charMaxHeightFraction = 0.9f;
    /**
     * The minimum width a possible character must have to be registered.
     * Expressed as pixels.
     */
    protected int charMinWidth;
    /**
     * The maximum width a possible character must have to be registered.
     * Expressed as pixels
     */
    protected int charMaxWidth;
    /**
     * The minimum height a possible character must have to be
     * registered. Expressed as pixels.
     */
    protected int charMinHeight;
    /**
     * The maximum height a possible character must have to be
     * registered. Expressed as pixels.
     */
    protected int charMaxHeight;
    /**
     * True if characters must be taller than they are wide.
     */
    protected boolean charAlwaysPortrait = true;
    /**
     * The minimum lenght the interpreted string must have for the scanner
     * to produce a result.
     */
    protected int minStringLength = 4;
    /**
     * Image brightness expressed as fraction.
     */
    protected float colorScale = 0.3f;
    /**
     * Image brightness and contrast expressed as fraction.
     */
    protected float colorScaleTranslate = 100.f;

    /**
     * Calculates the limits in pixels for allowed/required character size.
     * @param width The width of the bitmap to be scanned.
     * @param height The height of the bitmap to be scanned.
     */
    private void calculateCharSizeLimits(int width, int height) {
        charMinWidth = (int) ((float) width*charMinWidthFraction);
        charMaxWidth = (int) ((float) width*charMaxWidthFraction);
        charMinHeight = (int) ((float) height*charMinHeightFraction);
        charMaxHeight = (int) ((float) height*charMaxHeightFraction);
    }

    /**
     * @param minStringLength  The minimum lenght the interpreted string must
     * have for the scanner to produce a result.
     */
    public void setMinStringLength(int minStringLength) {
        this.minStringLength = minStringLength;
    }

    /**
     * @param minBlackPerRow The minimum amount of black pixels required for
     * a row to be registerd.
     */
    public void setMinBlackPerRow(int minBlackPerRow) {
        this.minBlackPerRow = minBlackPerRow;
    }

    /**
     * @param minBlackPerCol The minimum amount of black pixels required for
     * column to be registerd.
     */
    public void setMinBlackPerCol(int minBlackPerCol) {
        this.minBlackPerCol = minBlackPerCol;
    }

    /**
     * The minimum width a possible character must have to be registered.
     * @param charMinWidthFraction Fraction of bitmap width.
     */
    public void setCharMinWidthFraction(float charMinWidthFraction) {
        this.charMinWidthFraction = charMinWidthFraction;
    }

    /**
     * The maximum width a possible character must have to be registered.
     * @param charMaxWidthFraction Fraction of bitmap width.
     */
    public void setCharMaxWidthFraction(float charMaxWidthFraction) {
        this.charMaxWidthFraction = charMaxWidthFraction;
    }

    /**
     * The minumum height a possible character must have to be registered.
     * @param charMinHeightFraction Fraction of bitmap height.
     */
    public void setCharMinHeightFraction(float charMinHeightFraction) {
        this.charMinHeightFraction = charMinHeightFraction;
    }

    /**
     * The maximum height a possible character must have to be registered.
     * @param charMaxHeightFraction Fraction of bitmap height.
     */
    public void setCharMaxHeightFraction(float charMaxHeightFraction) {
        this.charMaxHeightFraction = charMaxHeightFraction;
    }

    private String resultString = null;
    private Bitmap originalBmp;
    private Bitmap debugBmp;
    private Bitmap referenceBmp;
    private Bitmap contrastBmp;
    private List<Bitmap> referenceBmps;
    private List<Bitmap> scaledBmps;
    private List<Rect> blackPixelCoords;

    public ScanResult(Bitmap bmp) {
        ScanResources scanResources;
        scanResources = CaptureActivity.getScanResources();
        referenceBmps = scanResources.getReferenceList();
        Bitmap measure = referenceBmps.get(0);
        scaleWidth = measure.getWidth();
        scaleHeight = measure.getHeight();

        originalBmp = bmp;
        bmpHeight = originalBmp.getHeight();
        bmpWidth = originalBmp.getWidth();
        calculateCharSizeLimits(bmpWidth, bmpHeight);
    }

    /**
     * The bitmap decoding and interpreting method.
     */
    public void decode() {
        contrastBmp = Normalizer.scaleOnly(originalBmp, colorScale);
        contrastBmp = Normalizer.scaleTranslate(contrastBmp,
            colorScaleTranslate);
        blackPixelCoords = findBlackPixels(contrastBmp);
        if (blackPixelCoords.size() >= minStringLength) {
            scaledBmps = uniformBitmapList(originalBmp,
                blackPixelCoords, scaleWidth, scaleHeight);
            resultString = referenceCompare(scaledBmps);
        }
    }

    /**
     * @return A list containing coordinates of the black pixels expressed
     * as Rect types.
     */
    public List<Rect> getBlackPixelCoords() {
        return blackPixelCoords;
    }

    /**
     * @return Null if the scanner did not produce any results, else the
     * interpreted string.
     */
    public String getResultString() {
        return resultString;
    }

    /**
     * @return The original target bitmap passed to the scanner.
     */
    public Bitmap getOriginalBitmap() {
        return originalBmp;
    }

    /**
     * @return A bitmap composed of all the character bitmaps that was found.
     */
    public Bitmap getDebugBitmap() {
        debugBmp = composeFromBitmapList(scaledBmps);
        return debugBmp;
    }

    /**
     * @return A bitmap composed of all the character reference bitmaps.
     */
    public Bitmap getReferenceBitmap() {
        referenceBmp = composeFromBitmapList(referenceBmps);
        return referenceBmp;
    }

    /**
     * Scans a bitmap for continuous black pixels and lists their coordinates
     * in the bitmap as Rects.
     * @param bmp The bitmap to be scanned.
     * @return A List of coordinates in types Rect.
     */
    private List<Rect> findBlackPixels(Bitmap bmp) {
        //Scan columns for black pixels
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
            if (blackPixels >= minBlackPerCol) {
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
        //Scan rows of gathered cols for black pixels
        lastEmpty = -2;
        lastFull = -2;
        List<Rect> coordList = new ArrayList<Rect>();
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
                if (blackPixels >= minBlackPerRow) {
                    lastFull = y;
                } else {
                    lastEmpty = y;
                }
                if ((lastFull-1 == lastEmpty)||(y == 0 && y == lastFull)){
                    lastTop = y;
                } else if ((lastEmpty-1 == lastFull)||(y == bmpWidth-1 && y == lastFull)){
                    lastBottom = y;
                    Rect coordRect = new Rect(coords[0], lastTop, coords[1], lastBottom);
                    if ((coordRect.width() > charMinWidth)&&
                        (coordRect.width() < charMaxWidth)&&
                        (coordRect.height() > charMinHeight)&&
                        (coordRect.height() < charMaxHeight)) {
                        if (charAlwaysPortrait) {
                            if (coordRect.height() > coordRect.width() ) {
                                coordList.add(coordRect);
                            }
                        } else {
                            coordList.add(coordRect);
                        }
                    }
                }
            }
        }
        return coordList;
    }

    /**
    * Creates a List of bitmaps and scales them uniformly.
    * @param bmp The source bitmap.
    * @param coordList A list containing coordinates expressed as Rects
    * @param toWidth The target width to scale the bitmaps into.
    * @param toHeight The target height to scale the bitmaps into.
    * @return The list of scaled bitmaps.
    */
    private List<Bitmap> uniformBitmapList(Bitmap bmp, List coordList, int toWidth, int toHeight) {
        List<Bitmap> bmpList = new ArrayList<Bitmap>();
        ListIterator<Rect> li = coordList.listIterator();
        while (li.hasNext()) {
            Rect coordRect = li.next();
            int[] pixels;
            pixels = new int[coordRect.height() * coordRect.width()];
            bmp.getPixels(pixels, 0, coordRect.width(), coordRect.left,
                coordRect.top, coordRect.width(), coordRect.height());
            Bitmap unscaledBmp = Bitmap.createBitmap(pixels, 0, coordRect.width(),
                coordRect.width(), coordRect.height(), Bitmap.Config.ARGB_8888);
            Bitmap scaledBmp = Bitmap.createScaledBitmap(unscaledBmp, toWidth, toHeight, true);
            scaledBmp = Normalizer.scaleOnly(scaledBmp, colorScale);
            scaledBmp = Normalizer.scaleTranslate(scaledBmp,
                colorScaleTranslate);
            bmpList.add(scaledBmp);
        }
        return bmpList;
    }

    /**
    * Composes a single bitmap from a list of bitmaps. Bitmaps are appended
    * horizontally.
    * @param bmpList The list of bitmaps.
    * @return The composed bitmap.
    */
    private Bitmap composeFromBitmapList(List inbmpList) {
        //TODO
        List<Bitmap> bmpList = inbmpList;
        Bitmap measureBmp = bmpList.get(0);
        int toWidth = measureBmp.getWidth();
        int toHeight = measureBmp.getHeight();
        Bitmap resultBmp = Bitmap.createBitmap(toWidth * bmpList.size(),
            toHeight, Bitmap.Config.ARGB_8888);
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
     * Compare the list found bitmaps to the reference bitmaps and
     * interpret the best matching reference to a string.
     * @param bmpList The list of bitmaps to compare.
     * @return The resulting string
     */
    private String referenceCompare(List bmpList) {
        StringBuffer result = new StringBuffer();
        ListIterator<Bitmap> li = bmpList.listIterator();
        while (li.hasNext()) {
            Bitmap bmp = li.next();
            int bestIndex = -1;
            int bestScore = -1;
            ListIterator<Bitmap> rli = referenceBmps.listIterator();
            while (rli.hasNext()) {
                int truePixels = 0;

                Bitmap rbmp = rli.next();
                int rbmp_i = rli.nextIndex();
                for(int x = 0; x < bmp.getWidth(); ++x) {
                    for(int y = 0; y < bmp.getHeight(); ++y) {
                        int rp = rbmp.getPixel(x,y);
                        int p = bmp.getPixel(x,y);
                        if (rp == p){
                            truePixels++;
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
        return result.toString();
    }

}
