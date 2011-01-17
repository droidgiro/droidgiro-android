/*
 * Copyright (C) 2011 DroidGiro authors
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

package se.droidgiro.scanner;

import java.lang.Math;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

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

	private final String TAG = "DroidGiro.Scanner";
	private boolean DEBUG = false;

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
	protected int minBlackPerCol = 1;
	/**
	 * The minimum amount of black pixels required for a row to be registered.
	 */
	protected int minBlackPerRow = 1;
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
		charMinWidth = Math.round(width*charMinWidthFraction);
		charMaxWidth = Math.round(width*charMaxWidthFraction);
		charMinHeight = Math.round(height*charMinHeightFraction);
		charMaxHeight = Math.round(height*charMaxHeightFraction);
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
	 * a row to be registered.
	 */
	public void setMinBlackPerRow(int minBlackPerRow) {
		this.minBlackPerRow = minBlackPerRow;
	}

	/**
	 * @param minBlackPerCol The minimum amount of black pixels required for
	 * column to be registered.
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
	private Bitmap nonContrastedDebugBmp = null;
	private Bitmap contrastBmp = null;
	private List<Bitmap> debugBmps;
	private List<Bitmap> foundContrastedBmps;
	private List<Bitmap> foundNonContrastedBmps;
	private List<Rect> blackPixelCoords;
	private Map<Character,Bitmap> charMap;

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
			/* It seems best to use the original bitmap for this
			method and change its contrast again. If the already contrasted
			bitmap is resized it produces a lot of artefacts which interferes
			with Scanner's bitmap comparison. It impacts performance, but it
			will have to do for now. */
			foundContrastedBmps = uniformBitmapList(targetBmp,
				blackPixelCoords, refCharWidth, refCharHeight, true);
			resultString = referenceCompare(foundContrastedBmps, charMap);
		} else {
			resultString = null;
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
	 * the contrasted and the noncontrasted characters found in the scanned
	 * bitmap.
	 */
	public Bitmap getDebugBitmap() {
		debugBmps = new ArrayList<Bitmap>();
		debugBmps.add(getMatchingReferenceBitmap());
		debugBmps.add(getContrastedDebugBitmap());
		debugBmps.add(getNonContrastedDebugBitmap());
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
				if (charMap.containsKey(ch)) {
					matchingBmpList.add(charMap.get(ch));
				}
			}
			matchingReferenceBmp = composeFromBitmapList(matchingBmpList, false);
		}
		return matchingReferenceBmp;
	}

	/**
	 * @return A bitmap composed of all the character bitmaps that was found,
	 * noncontrasted.
	 */
	public Bitmap getNonContrastedDebugBitmap() {
		foundNonContrastedBmps = uniformBitmapList(targetBmp,
				blackPixelCoords, refCharWidth, refCharHeight, false);
		nonContrastedDebugBmp = composeFromBitmapList(foundNonContrastedBmps,
		false);
		return nonContrastedDebugBmp;
	}

	/**
	 * Scans a bitmap for continuous black pixels and lists their coordinates
	 * in the bitmap as Rects.
	 * @param bmp The bitmap to be scanned.
	 * @return A List of coordinates in types Rect.
	 */
	protected List<Rect> findBlackPixels(Bitmap bmp) {
		/* Scan columns for black pixels */
		blackPixelCoords = new ArrayList<Rect>();
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
			return blackPixelCoords;
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
							(y == targetBmpHeight-1 && y == lastFull)) {
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
								blackPixelCoords.add(coordRect);
							}
						} else {
							blackPixelCoords.add(coordRect);
						}
					}
				}
			}
		}
		return blackPixelCoords;
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
	protected Bitmap composeFromBitmapList(List bmpList, boolean vertical) {
		//TODO: Probably unnecessary object creations in this method but
		// not very important since it's only for debug bitmap
		Bitmap measureBmp = (Bitmap)bmpList.get(0);
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

	protected float minInitMatchPercent = 50f;
	protected int matchTolerenceRows = 3;
	protected int matchTolerencePixels;
	protected int compareRowSpacing = 2;
	protected int compareColSpacing = 2;

	/**
	 * @param Number of rows to skip over when comparing character bitmaps.
	 */
	public void setCompareRowSpacing(int compareRowSpacing) {
		if	(compareRowSpacing <= 0) {
			this.compareRowSpacing = 1;
		} else if
			(compareRowSpacing > refCharWidth) {
			this.compareRowSpacing = refCharWidth;
		} else {
			this.compareRowSpacing = compareRowSpacing + 1;
		}
	}

	/**
	 * @param Number of columns to skip over when comparing character bitmaps.
	 */
	public void setCompareColumnSpacing(int compareColSpacing) {
		if	(compareColSpacing <= 0) {
			this.compareColSpacing = 1;
		} else if
			(compareColSpacing > refCharHeight) {
			this.compareColSpacing = refCharWidth;
		} else {
			this.compareColSpacing = compareColSpacing + 1;
		}
	}

	/**
	 * @param When iterating through the scanned characters in the comparison
	 * loop, if no reference character has a higher match percent than this, the
	 * scanned character is translated as an "X", which is invalid.
	 */
	public void setMinInitialMatchPercent(float minInitMatchPercent) {
		this.minInitMatchPercent = minInitMatchPercent;
	}

	/**
	 * @param When iterating through the scanned characters in the comparison
	 * loop, ignore this amount of nonmatching rows in the character bitmap
	 * when calculating match percentage. If match percentage falls below the
	 * minimum initial amount, or the best match percent of the currently best
	 * matching reference bitmap, it breaks the loop and tries the next
	 * character. A low number results in a faster comparison but more
	 * wrong matches.
	 */
	public void setMatchTolerenceRows(int matchTolerenceRows) {
		this.matchTolerenceRows = matchTolerenceRows;
	}

	/**
	 * Calculates the match tolerence rows to pixels.
	 */
	protected void calculateMatchTolerencePixels() {
		if (refCharWidth != 0) {
			matchTolerencePixels = refCharWidth*matchTolerenceRows;
		} else {
			matchTolerencePixels = 16*matchTolerenceRows;
		}
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
		calculateMatchTolerencePixels();
		StringBuffer result = new StringBuffer();
		ListIterator<Bitmap> li = bmpList.listIterator();
		int totalPixels = refCharWidth*refCharHeight;
		int index = 0;
		/* Iterate over the target bitmap list. */
		while (li.hasNext()) {
			Bitmap bmp = li.next();
			Character bestChar = (char)88;
			float bestScore = minInitMatchPercent;
			Set set = charMap.entrySet();
			Iterator it = set.iterator();
			/* Iterate over the reference bitmap list. */
			while (it.hasNext()) {
				int matching = matchTolerencePixels;
				int nonmatching = 0;
				float percent = 0;
				Map.Entry me = (Map.Entry)it.next();
				Character currentChar = (Character)me.getKey();
				Bitmap currentCharBmp = (Bitmap)me.getValue();
				/* Iterate over pixels in the target bitmap. */
				currentCharLoop:
				/* The spacing on y and x will probably not produce faster
				   results, it will not have to read the whole bitmap anyway,
				   but will make the comparison more spread out before it
				   skips to the next character. Possibly this will improve
				   accuracy. I have experimented with creating a set path
				   to check but so far it has mostly decreased reading speed
				   significantly.. */
				for(int y = 0; y < bmp.getHeight(); y+=compareRowSpacing) {
					for(int x = 0; x < bmp.getWidth(); x+=compareColSpacing) {
						int refPixel = currentCharBmp.getPixel(x,y);
						int foundPixel = bmp.getPixel(x,y);
						/* Compare pixels between target and reference. */
						if (refPixel == foundPixel){
							matching++;
						} else {
							nonmatching++;
							/* If current gets a lower match percent than best
							   previous, break the loop even if there are pixels
							   left to compare.*/
							if (nonmatching % 2 == 0) { //check every other
								percent = ((float)matching/
									((float)matching+(float)nonmatching))*100;
								if (percent < bestScore) {
									if (DEBUG) {Log.d(TAG,
										"Skipping: "+currentChar+
										" true:"+matching+
										" false:"+nonmatching+
										" scanned:"+(matching+nonmatching)+
										" percent:"+percent);
									}
									break currentCharLoop;
								}
							}
						}
					}
				}
				/* If current has a higher match percent than any before, update
				   bestScore */
				if (percent > bestScore) {
					bestScore = percent;
					bestChar = currentChar;
					if (DEBUG) {Log.d(TAG, "Better match found= "+bestChar+
						" true:"+matching+
						" false:"+nonmatching+
						" scan:"+(matching+nonmatching)+
						" percent:"+percent);
					}
				}
			}
			if (DEBUG) {Log.d(TAG, "Final char= "+bestChar);}
			result.append(bestChar);
		}
		return result.toString();
	}

}
