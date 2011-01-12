/*
 * Copyright (C) 2011 aGiro authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.agiro.scanner.android;

import java.util.ArrayList;
import java.text.CharacterIterator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.text.StringCharacterIterator;


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
public class Scanner {

	private final String TAG = "aGiro.Scanner";

	/**
	 * The width of the bitmap to be scanned.
	 */
	protected static int targetBmpWidth;
	/**
	 * The height of the bitmap to be scanned.
	 */
	protected static int targetBmpHeight;
	/**
	 * The width of the reference characters.
	 */
	protected int refCharWidth;
	/**
	 * The height of the reference characters.
	 */
	protected int refCharHeight;
	/**
	 * The color black in RGB images expressed as integer.
	 */
	protected int black = -16777216;
	/**
	 * The minimum amount of black pixels required for a column to be registered.
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
	protected int minResultLength = 4;
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
	protected void calculateCharSizeLimits(int width, int height) {
		charMinWidth = (int) ((float) width*charMinWidthFraction);
		charMaxWidth = (int) ((float) width*charMaxWidthFraction);
		charMinHeight = (int) ((float) height*charMinHeightFraction);
		charMaxHeight = (int) ((float) height*charMaxHeightFraction);
	}

	/**
	 * @param minResultLength  The minimum lenght the interpreted string must
	 * have for the scanner to produce a result.
	 */
	public void setMinResultLength(int minResultLength) {
		this.minResultLength = minResultLength;
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
	private Bitmap targetBmp = null;
	private Bitmap debugBmp = null;
	private Bitmap referenceBmp = null;
	private Bitmap matchingReferenceBmp = null;
	private Bitmap contrastedDebugBmp = null;
	private Bitmap unContrastedDebugBmp = null;
	private Bitmap contrastBmp = null;
	private List<Bitmap> debugBmps;
	private List<Bitmap> foundContrastedBmps;
	private List<Bitmap> foundUnContrastedBmps;
	private List<Rect> blackPixelCoords;
	private Map<Character,Bitmap> charMap;
	private Map<Integer,Integer> percentMatchMap;

	public Scanner(ScanResources scanResources) {
		setupScanResources(scanResources);
	}

	public Scanner(ScanResources scanResources, Bitmap targetBmp) {
		setupScanResources(scanResources);
		this.targetBmp = targetBmp;
	}

	private void setupScanResources(ScanResources scanResources) {
		charMap = scanResources.getCharMap();
		//TODO
		Bitmap measure = charMap.get((char)35);
		refCharWidth = measure.getWidth();
		refCharHeight = measure.getHeight();
	}

	/**
	 * The bitmap scanning and interpreting method.
	 */
	public void scan() {
		contrastBmp = Normalizer.scaleOnly(targetBmp, colorScale);
		contrastBmp = Normalizer.scaleTranslate(contrastBmp,
			colorScaleTranslate);
		blackPixelCoords = findBlackPixels(contrastBmp);
		if (blackPixelCoords.size() >= minResultLength) {
			foundContrastedBmps = uniformBitmapList(targetBmp,
				blackPixelCoords, refCharWidth, refCharHeight, true);
			resultString = referenceCompare(foundContrastedBmps, charMap);
		}
	}

	/**
	 * Clears the scanner object for a new scan.
	 */
	public void clear() {
		resultString = null;
		targetBmp = null;
		debugBmp = null;
		referenceBmp = null;
		matchingReferenceBmp = null;
		contrastedDebugBmp = null;
		unContrastedDebugBmp = null;
		contrastBmp = null;
		try {
			debugBmps.clear();
			foundContrastedBmps.clear();
			foundUnContrastedBmps.clear();
			blackPixelCoords.clear();
		}
		catch ( NullPointerException e ) {
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
	 * @return The target bitmap that was passed to the scanner.
	 */
	public Bitmap getTargetBitmap() {
		return targetBmp;
	}

	/**
	 * @param targetBmp The bitmap to be scanned.
	 */
	public void setTargetBitmap(Bitmap targetBmp) {
		this.targetBmp = targetBmp;
		targetBmpHeight = targetBmp.getHeight();
		targetBmpWidth = targetBmp.getWidth();
		calculateCharSizeLimits(targetBmpWidth, targetBmpHeight);
	}

	/**
	 * @return A bitmap composed of the matching reference character bitmaps,
	 * the contrasted and the uncontrasted characters found in the scanned
	 * bitmap.
	 */
	public Bitmap getDebugBitmap() {
		debugBmps = new ArrayList<Bitmap>();
		debugBmps.add(getMatchingReferenceBitmap());
		debugBmps.add(getContrastedDebugBitmap());
		debugBmps.add(getUnContrastedDebugBitmap());
		debugBmp = composeFromBitmapList(debugBmps, true);
		return debugBmp;
	}

	/**
	 * @return A bitmap composed of all the character bitmaps that was found,
	 * contrasted.
	 */
	public Bitmap getContrastedDebugBitmap() {
		contrastedDebugBmp = composeFromBitmapList(foundContrastedBmps, false);
		return contrastedDebugBmp;
	}

	/**
	 * @return A bitmap composed of all the character reference bitmaps.
	 */
	public Bitmap getReferenceBitmap() {
		List<Bitmap> charList = new ArrayList<Bitmap>(charMap.values());
		referenceBmp = composeFromBitmapList(charList, false);
		return referenceBmp;
	}

	/**
	 * @return A bitmap composed of all the character reference bitmaps
	 * ordered to match the resulting string.
	 */
	public Bitmap getMatchingReferenceBitmap() {
		List<Bitmap> matchingBmpList = new ArrayList<Bitmap>();
		if (resultString != null) {
			CharacterIterator it = new StringCharacterIterator(resultString);
			for (char ch = it.first(); ch != CharacterIterator.DONE; ch = it.next()){
				matchingBmpList.add(charMap.get(ch));
			}
			matchingReferenceBmp = composeFromBitmapList(matchingBmpList, false);
		}
		return matchingReferenceBmp;
	}

	/**
	 * @return A bitmap composed of all the character bitmaps that was found,
	 * uncontrasted.
	 */
	public Bitmap getUnContrastedDebugBitmap() {
		foundUnContrastedBmps = uniformBitmapList(targetBmp,
				blackPixelCoords, refCharWidth, refCharHeight, false);
		unContrastedDebugBmp = composeFromBitmapList(foundUnContrastedBmps,
		false);
		return unContrastedDebugBmp;
	}

	/**
	 * @return A map showing percent match with reference bitmaps, the key is its
	 * index in result string, the value its percentage.
	 */
	public Map<Integer,Integer> getPercentMatchMap() {
		return percentMatchMap;
	}
	/**
	 * Scans a bitmap for continuous black pixels and lists their coordinates
	 * in the bitmap as Rects.
	 * @param bmp The bitmap to be scanned.
	 * @return A List of coordinates in types Rect.
	 */
	protected List<Rect> findBlackPixels(Bitmap bmp) {
		/* Scan columns for black pixels */
		List<Rect> coordList = new ArrayList<Rect>();
		List<int[]> cols = new ArrayList<int[]>();
		int lastEmpty = -2;
		int lastFull = -2;
		int lastLeft = -2;
		int lastRight = -2;
		int lastTop = -2;
		int lastBottom = -2;
		for(int x = 0; x < targetBmpWidth; ++x) {
			int blackPixels = 0;
			for(int y = 0; y < targetBmpHeight; ++y) {
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
			if ((lastFull-1 == lastEmpty)||
				(x == 0 && x == lastFull)){
				lastLeft = x;
			} else if ((lastEmpty-1 == lastFull)||
						(x == targetBmpWidth-1 && x == lastFull)){
				lastRight = x;
				int[] c = new int[] {lastLeft, lastRight};
				cols.add(c);
			}
		}
		/* Stop if gathered cols are too few */
		if (cols.size() < minResultLength) {
			return coordList;
		}
		/* Scan rows of gathered cols for black pixels */
		lastEmpty = -2;
		lastFull = -2;
		ListIterator<int[]> li = cols.listIterator();
		while (li.hasNext()) {
			int[] coords = li.next();
			for(int y = 0; y < targetBmpHeight; ++y) {
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
				if ((lastFull-1 == lastEmpty)||
					(y == 0 && y == lastFull)) {
					lastTop = y;
				} else if ((lastEmpty-1 == lastFull)||
							(y == targetBmpWidth-1 && y == lastFull)) {
					lastBottom = y;
					Rect coordRect = new Rect(coords[0], lastTop,
						coords[1], lastBottom);
					/* Ignore sections of the wrong size or shape */
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
	* @param contrast True if bitmap should be contrasted.
	* @return The list of scaled bitmaps.
	*/
	protected List<Bitmap> uniformBitmapList(Bitmap bmp, List coordList,
			int toWidth, int toHeight, boolean contrast) {
		List<Bitmap> bmpList = new ArrayList<Bitmap>();
		ListIterator<Rect> li = coordList.listIterator();
		while (li.hasNext()) {
			Rect coordRect = li.next();
			int[] pixels;
			pixels = new int[coordRect.height() * coordRect.width()];
			bmp.getPixels(pixels, 0, coordRect.width(), coordRect.left,
				coordRect.top, coordRect.width(), coordRect.height());
			Bitmap unscaledBmp =
				Bitmap.createBitmap(pixels, 0, coordRect.width(),
				coordRect.width(), coordRect.height(), Bitmap.Config.RGB_565);
			Bitmap scaledBmp =
				Bitmap.createScaledBitmap(unscaledBmp, toWidth, toHeight, true);
			if (contrast) {
				scaledBmp = Normalizer.scaleOnly(scaledBmp, colorScale);
				scaledBmp = Normalizer.scaleTranslate(scaledBmp,
					colorScaleTranslate);
			}
			bmpList.add(scaledBmp);
		}
		return bmpList;
	}

	/**
	* Composes a single bitmap from a list of bitmaps.
	* @param bmpList The list of bitmaps.
	* @param vertical True if bitmaps should be appended vertically.
	* @return The composed bitmap.
	*/
	protected Bitmap composeFromBitmapList(List toCompose, boolean vertical) {
		List<Bitmap> bmpList = toCompose;
		Bitmap measureBmp = bmpList.get(0);
		int toWidth = measureBmp.getWidth();
		int toHeight = measureBmp.getHeight();
		Bitmap resultBmp;
		Bitmap.Config config = Bitmap.Config.RGB_565;
		if (vertical) {
			resultBmp = Bitmap.createBitmap(toWidth,
				toHeight * bmpList.size(), config);
		} else {
			resultBmp = Bitmap.createBitmap(toWidth * bmpList.size(),
				toHeight, config);
		}
		Canvas canvas = new Canvas(resultBmp);
		ListIterator<Bitmap> li = bmpList.listIterator();
		int i = 0;
		while (li.hasNext()) {
			Bitmap bmp = li.next();
			Rect srcRect = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
			Rect dstRect = new Rect(srcRect);
			if (i != 0){
				if (vertical) {
					dstRect.offset(0, i*bmp.getHeight());
				} else {
					dstRect.offset(i*bmp.getWidth(), 0);
				}
			}
			i++;
			canvas.drawBitmap(bmp, srcRect, dstRect, null);
		}
		return resultBmp;
	}

	/**
	 * Compare the list of collected bitmaps to the reference bitmaps and
	 * interpret the best matching reference to a string.
	 * @param bmpList The list of bitmaps to compare.
	 * @param charMap
	 *			A map with character as key and corresponding bitmap as value.
	 * @return The resulting string
	 */
	protected String referenceCompare(List bmpList, Map charMap) {
	/* TODO: Make a limit for how dissimilar the best matching is
			 allowed to be, and stop iterating if match is ~99%. */
		StringBuffer result = new StringBuffer();
		ListIterator<Bitmap> li = bmpList.listIterator();
		int totalPixels = refCharWidth*refCharHeight;
		percentMatchMap = new HashMap<Integer,Integer>();
		int percentMatch = 0;
		int index = 0;
		while (li.hasNext()) {
			Bitmap bmp = li.next();
			Character bestChar = (char)88;
			int bestScore = -1;
			Set set = charMap.entrySet();
			Iterator it = set.iterator();
			while (it.hasNext()) {
				int matchingPixels = 0;
				Map.Entry me = (Map.Entry)it.next();
				Character currentChar = (Character)me.getKey();
				Bitmap currentCharBmp = (Bitmap)me.getValue();
				for(int x = 0; x < bmp.getWidth(); ++x) {
					for(int y = 0; y < bmp.getHeight(); ++y) {
						int refPixel = currentCharBmp.getPixel(x,y);
						int foundPixel = bmp.getPixel(x,y);
						if (refPixel == foundPixel){
							matchingPixels++;
						}
					}
				}
				if (matchingPixels > bestScore){
					bestScore = matchingPixels;
					bestChar = currentChar;
					percentMatch = (int)(((double)matchingPixels/totalPixels)*100);
				}
			}
			percentMatchMap.put(index, percentMatch);
			result.append(bestChar);
			index++;
		}
		return result.toString();
	}

}
