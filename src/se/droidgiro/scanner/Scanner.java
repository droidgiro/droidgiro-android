/*
 * Copyright (C) 2011 DroidGiro authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.droidgiro.scanner;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

/**
 * A scanner object with methods to determine coordinates of black pixels in the
 * provided bitmap and compare sections of those black pixels to a list of
 * reference bitmaps.
 * 
 * @author wulax
 */
public class Scanner {

	private final String TAG = "DroidGiro.Scanner";

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
	 * The color white in RGB images expressed as integer.
	 */
	protected int white = -1;
	/**
	 * The color black in RGB images expressed as integer.
	 */
	protected int black = -16777216;
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
	 * The minimum height a possible character must have to be registered.
	 * Expressed as fraction of height of the bitmap to be scanned.
	 */
	protected float charMinHeightFraction = 0.15f;
	/**
	 * The maximum height a possible character must have to be registered.
	 * Expressed as fraction of height of the bitmap to be scanned.
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
	 * The minimum height a possible character must have to be registered.
	 * Expressed as pixels.
	 */
	protected int charMinHeight;
	/**
	 * The maximum height a possible character must have to be registered.
	 * Expressed as pixels.
	 */
	protected int charMaxHeight;
	/**
	 * True if characters must be taller than they are wide.
	 */
	protected boolean charAlwaysPortrait = true;
	/**
	 * The minimum lenght the interpreted string must have for the scanner to
	 * produce a result.
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
	 * 
	 * @param width
	 *            The width of the bitmap to be scanned.
	 * @param height
	 *            The height of the bitmap to be scanned.
	 */
	protected void calculateCharSizeLimits(int width, int height) {
		charMinWidth = Math.round(width * charMinWidthFraction);
		charMaxWidth = Math.round(width * charMaxWidthFraction);
		charMinHeight = Math.round(height * charMinHeightFraction);
		charMaxHeight = Math.round(height * charMaxHeightFraction);
	}

	/**
	 * @param minResultLength
	 *            The minimum lenght the interpreted string must have for the
	 *            scanner to produce a result.
	 */
	public void setMinResultLength(int minResultLength) {
		this.minResultLength = minResultLength;
	}

	/**
	 * The minimum width a possible character must have to be registered.
	 * 
	 * @param charMinWidthFraction
	 *            Fraction of bitmap width.
	 */
	public void setCharMinWidthFraction(float charMinWidthFraction) {
		this.charMinWidthFraction = charMinWidthFraction;
	}

	/**
	 * The maximum width a possible character must have to be registered.
	 * 
	 * @param charMaxWidthFraction
	 *            Fraction of bitmap width.
	 */
	public void setCharMaxWidthFraction(float charMaxWidthFraction) {
		this.charMaxWidthFraction = charMaxWidthFraction;
	}

	/**
	 * The minumum height a possible character must have to be registered.
	 * 
	 * @param charMinHeightFraction
	 *            Fraction of bitmap height.
	 */
	public void setCharMinHeightFraction(float charMinHeightFraction) {
		this.charMinHeightFraction = charMinHeightFraction;
	}

	/**
	 * The maximum height a possible character must have to be registered.
	 * 
	 * @param charMaxHeightFraction
	 *            Fraction of bitmap height.
	 */
	public void setCharMaxHeightFraction(float charMaxHeightFraction) {
		this.charMaxHeightFraction = charMaxHeightFraction;
	}

	/**
	 * The resulting string from scan().
	 */
	protected String resultString = null;
	/**
	 * The bitmap to be analyzed.
	 */
	protected Bitmap targetBmp = null;
	/**
	 * The bitmap that is created from the getDebugBitmap() method. Shows three
	 * parts of the scan process, the noncontrasted bitmaps, the contrasted
	 * bitmaps, and the best matching reference bitmaps.
	 */
	protected Bitmap debugBmp = null;
	/**
	 * A bitmap composed of all the reference bitmaps.
	 */
	protected Bitmap referenceBmp = null;
	/**
	 * A bitmap composed of the best matching reference bitmaps.
	 */
	protected Bitmap matchingReferenceBmp = null;
	/**
	 * A bitmap composed of the found black pixel sections with contrast added.
	 */
	protected Bitmap contrastedDebugBmp = null;
	/**
	 * A bitmap composed of the original black pixels found.
	 */
	protected Bitmap nonContrastedDebugBmp = null;
	/**
	 * The target bitmap with contrast added.
	 */
	protected Bitmap contrastBmp = null;
	/**
	 * A list of the three different bitmaps in debugBmp.
	 */
	protected List<Bitmap> debugBmps;
	/**
	 * A list of bitmaps with the found black pixel sections, contrast added.
	 */
	protected List<Bitmap> foundContrastedBmps;
	/**
	 * A list of bitmaps with the found black pixel sections, no contrast.
	 */
	protected List<Bitmap> foundNonContrastedBmps;
	/**
	 * The coordinates of the cound black pixel sections, as Rects.
	 */
	protected List<Rect> targetRects;
	/**
	 * The reference character bitmaps in a Map.
	 */
	protected Map<Character, Bitmap> charMap;
	/**
	 * The reference character bitmaps in a Set.
	 */
	protected Set<Entry<Character, Bitmap>> charSet;

	/**
	 * @param scanResources
	 *            The resource class for the scanner.
	 */
	public Scanner(ScanResources scanResources) {
		setupScanResources(scanResources);
	}

	/**
	 * @param scanResources
	 *            The resource class for the scanner.
	 * @param targetBmp
	 *            Sets the bitmap to be scanned.
	 */
	public Scanner(ScanResources scanResources, Bitmap targetBmp) {
		setupScanResources(scanResources);
		this.targetBmp = targetBmp;
	}

	/**
	 * Sets up the reference character Set and measures the height of the first
	 * bitmap.
	 * 
	 * @param scanResources
	 *            The resource class for the scanner.
	 */
	protected void setupScanResources(ScanResources scanResources) {
		charMap = scanResources.getCharMap();
		if (!charMap.isEmpty()) {
			charSet = charMap.entrySet();
			Iterator<Entry<Character, Bitmap>> it = charSet.iterator();
			Map.Entry<Character, Bitmap> entry = it.next();
			Bitmap measure = (Bitmap) entry.getValue();
			refCharWidth = measure.getWidth();
			refCharHeight = measure.getHeight();
		} else {
			Log.w(TAG, "Reference character map is empty.");
		}
	}

	/**
	 * The bitmap scanning and interpreting method.
	 */
	public void scan() {
		contrastBmp = setContrast(targetBmp, colorScale, colorScaleTranslate);
		targetRects = findTargetRects(contrastBmp);
		if (targetRects.size() >= minResultLength) {
			/*
			 * It seems best to use the original bitmap for this method and
			 * change its contrast again. If the already contrasted bitmap is
			 * resized it produces a lot of artefacts which interferes with
			 * Scanner's bitmap comparison. It impacts performance, but it will
			 * have to do for now.
			 */
			foundContrastedBmps = uniformBitmapList(targetBmp, targetRects,
					refCharWidth, refCharHeight, true);
			resultString = referenceCompare(foundContrastedBmps, charMap);
		} else {
			resultString = null;
		}
	}

	/**
	 * @return A list containing coordinates of the black pixels expressed as
	 *         Rect types.
	 */
	public List<Rect> getTargetPixels() {
		return targetRects;
	}

	/**
	 * @return Null if the scanner did not produce any results, else the
	 *         interpreted string.
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
	 * @param targetBmp
	 *            The bitmap to be scanned.
	 */
	public void setTargetBitmap(Bitmap targetBmp) {
		this.targetBmp = targetBmp;
		targetBmpHeight = targetBmp.getHeight();
		targetBmpWidth = targetBmp.getWidth();
		calculateCharSizeLimits(targetBmpWidth, targetBmpHeight);
	}

	/**
	 * @return A bitmap composed of the matching reference character bitmaps,
	 *         the contrasted and the noncontrasted characters found in the
	 *         scanned bitmap.
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
	 *         contrasted.
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
	 * @return A bitmap composed of all the character reference bitmaps ordered
	 *         to match the resulting string.
	 */
	public Bitmap getMatchingReferenceBitmap() {
		List<Bitmap> matchingBmpList = new ArrayList<Bitmap>();
		if (resultString != null) {
			CharacterIterator it = new StringCharacterIterator(resultString);
			for (char ch = it.first(); ch != CharacterIterator.DONE; ch = it
					.next()) {
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
	 *         noncontrasted.
	 */
	public Bitmap getNonContrastedDebugBitmap() {
		foundNonContrastedBmps = uniformBitmapList(targetBmp, targetRects,
				refCharWidth, refCharHeight, false);
		nonContrastedDebugBmp = composeFromBitmapList(foundNonContrastedBmps,
				false);
		return nonContrastedDebugBmp;
	}

	/**
	 * Scans a bitmap for continuous black pixels and lists their coordinates in
	 * the bitmap as Rects.
	 * 
	 * @param bmp
	 *            The bitmap to be scanned.
	 * @return A List of coordinates in types Rect.
	 */
	protected List<Rect> findTargetRects(Bitmap bmp) {
		/* Scan columns for black pixels */
		targetRects = new ArrayList<Rect>();
		List<int[]> cols = new ArrayList<int[]>();
		int fullCol = black * targetBmpHeight;
		int emptyCol = white * targetBmpHeight;
		int lastFalse = -2;
		int lastTrue = -2;
		int lastLeft = -2;
		int lastRight = -2;
		for (int x = 0; x < targetBmpWidth; ++x) {
			int[] colPixels = new int[targetBmpHeight];
			bmp.getPixels(colPixels, 0, 1, x, 0, 1, targetBmpHeight);
			int colSum = 0; // 128 pixels max for integer I think
			for (int p : colPixels) {
				colSum += p;
			}
			if (colSum < emptyCol && colSum > fullCol) {
				lastTrue = x;
			} else {
				lastFalse = x;
			}
			if ((lastTrue - 1 == lastFalse) || (x == 0 && x == lastTrue)) {
				lastLeft = x;
			} else if ((lastFalse - 1 == lastTrue)
					|| (x == targetBmpWidth - 1 && x == lastTrue)) {
				lastRight = x;
				int[] c = new int[] { lastLeft, lastRight };
				cols.add(c);
			}
		}
		/* Stop if gathered cols are too few */
		if (cols.size() < minResultLength) {
			return targetRects;
		}
		/* Scan rows of gathered cols for black pixels */
		lastFalse = -2;
		lastTrue = -2;
		int lastTop = -2;
		int lastBottom = -2;
		ListIterator<int[]> li = cols.listIterator();
		while (li.hasNext()) {
			int[] leftRight = li.next();
			int left = leftRight[0];
			int right = leftRight[1];
			int sectionWidth = right - left;
			int emptyRow = sectionWidth * white;
			for (int y = 0; y < targetBmpHeight; ++y) {
				int[] rowPixels = new int[sectionWidth];
				bmp.getPixels(rowPixels, 0, targetBmpWidth, left, y,
						sectionWidth, 1);
				int rowSum = 0;
				for (int p : rowPixels) {
					rowSum += p;
				}
				if (rowSum < emptyRow) {
					lastTrue = y;
				} else {
					lastFalse = y;
				}
				if ((lastTrue - 1 == lastFalse) || (y == 0 && y == lastTrue)) {
					lastTop = y;
				} else if ((lastFalse - 1 == lastTrue)
						|| (y == targetBmpHeight - 1 && y == lastTrue)) {
					lastBottom = y;
				}
			}
			Rect targetRect = new Rect(left, lastTop, right, lastBottom);
			/* Ignore sections of the wrong size or shape */
			if ((targetRect.width() > charMinWidth)
					&& (targetRect.width() < charMaxWidth)
					&& (targetRect.height() > charMinHeight)
					&& (targetRect.height() < charMaxHeight)) {
				if (charAlwaysPortrait) {
					if (targetRect.height() > targetRect.width()) {
						targetRects.add(targetRect);
					}
				} else {
					targetRects.add(targetRect);
				}
			}
		}
		return targetRects;
	}

	/**
	 * Creates a List of bitmaps and scales them uniformly.
	 * 
	 * @param bmp
	 *            The source bitmap.
	 * @param coordList
	 *            A list containing coordinates expressed as Rects
	 * @param toWidth
	 *            The target width to scale the bitmaps into.
	 * @param toHeight
	 *            The target height to scale the bitmaps into.
	 * @param contrast
	 *            True if bitmap should be contrasted.
	 * @return The list of scaled bitmaps.
	 */
	protected List<Bitmap> uniformBitmapList(Bitmap bmp, List<Rect> coordList,
			int toWidth, int toHeight, boolean contrast) {
		List<Bitmap> bmpList = new ArrayList<Bitmap>();
		ListIterator<Rect> li = coordList.listIterator();
		while (li.hasNext()) {
			Rect targetRect = li.next();
			int[] pixels;
			pixels = new int[targetRect.height() * targetRect.width()];
			bmp.getPixels(pixels, 0, targetRect.width(), targetRect.left,
					targetRect.top, targetRect.width(), targetRect.height());
			Bitmap nonscaledBmp = Bitmap.createBitmap(pixels, 0, targetRect
					.width(), targetRect.width(), targetRect.height(),
					Bitmap.Config.RGB_565);
			Bitmap scaledBmp = Bitmap.createScaledBitmap(nonscaledBmp, toWidth,
					toHeight, true);
			if (contrast) {
				scaledBmp = setContrast(scaledBmp, colorScale,
						colorScaleTranslate);
			}
			bmpList.add(scaledBmp);
		}
		return bmpList;
	}

	/**
	 * Composes a single bitmap from a list of bitmaps.
	 * 
	 * @param bmpList
	 *            The list of bitmaps.
	 * @param vertical
	 *            True if bitmaps should be appended vertically.
	 * @return The composed bitmap.
	 */
	protected Bitmap composeFromBitmapList(List<Bitmap> bmpList,
			boolean vertical) {
		// TODO: Probably unnecessary object creations in this method but
		// not very important since it's only for debug bitmap
		Bitmap measureBmp = bmpList.get(0);
		int toWidth = measureBmp.getWidth();
		int toHeight = measureBmp.getHeight();
		Bitmap resultBmp;
		Bitmap.Config config = Bitmap.Config.RGB_565;
		if (vertical) {
			resultBmp = Bitmap.createBitmap(toWidth, toHeight * bmpList.size(),
					config);
		} else {
			resultBmp = Bitmap.createBitmap(toWidth * bmpList.size(), toHeight,
					config);
		}
		Canvas canvas = new Canvas(resultBmp);
		ListIterator<Bitmap> li = bmpList.listIterator();
		int i = 0;
		while (li.hasNext()) {
			Bitmap bmp = li.next();
			Rect srcRect = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
			Rect dstRect = new Rect(srcRect);
			if (i != 0) {
				if (vertical) {
					dstRect.offset(0, i * bmp.getHeight());
				} else {
					dstRect.offset(i * bmp.getWidth(), 0);
				}
			}
			i++;
			canvas.drawBitmap(bmp, srcRect, dstRect, null);
		}
		return resultBmp;
	}

	/**
	 * The initial minimum percent matching pixels the reference bitmaps must
	 * have in the pixel comparison method.
	 */
	protected float minInitMatchPercent = 50f;
	/**
	 * Maximum amount of nonmatching rows to ignore when comparing with the
	 * reference bitmaps.
	 */
	protected int matchTolerenceRows = 3;
	/**
	 * Maximum amount of nonmatching pixels to ignore when comparing with the
	 * reference bitmaps.
	 */
	protected int matchTolerencePixels;
	/**
	 * Number of rows to skip over when comparing character bitmaps.
	 */
	protected int compareRowSpacing = 2;
	/**
	 * Number of columns to skip over when comparing character bitmaps.
	 */
	protected int compareColSpacing = 2;

	/**
	 * @param compareRowSpacing
	 *            Number of rows to skip over when comparing character bitmaps.
	 */
	public void setCompareRowSpacing(int compareRowSpacing) {
		if (compareRowSpacing <= 0) {
			this.compareRowSpacing = 1;
		} else if (compareRowSpacing > refCharWidth) {
			this.compareRowSpacing = refCharWidth;
		} else {
			this.compareRowSpacing = compareRowSpacing + 1;
		}
	}

	/**
	 * @param compareColSpacing
	 *            Number of columns to skip over when comparing character bitmaps.
	 */
	public void setCompareColumnSpacing(int compareColSpacing) {
		if (compareColSpacing <= 0) {
			this.compareColSpacing = 1;
		} else if (compareColSpacing > refCharHeight) {
			this.compareColSpacing = refCharWidth;
		} else {
			this.compareColSpacing = compareColSpacing + 1;
		}
	}

	/**
	 * @param minInitMatchPercent
	 *            When iterating through the scanned characters in the
	 *            comparison loop, if no reference character has a higher
	 *            match percent than this, the scanned character is
	 *            translated as an "X", which is invalid.
	 */
	public void setMinInitialMatchPercent(float minInitMatchPercent) {
		this.minInitMatchPercent = minInitMatchPercent;
	}

	/**
	 * @param matchTolerenceRows
	 *            When iterating through scanned characters in the comparison
	 *            loop, ignore this amount of nonmatching rows in the character
	 *            bitmap when calculating match percentage. If match percentage
	 *            falls below the minimum initial amount, or the best match
	 *            percent of the currently best matching reference bitmap, it
	 *            breaks the loop and tries the next character. A low number
	 *            results in a faster comparison but more wrong matches.
	 */
	public void setMatchTolerenceRows(int matchTolerenceRows) {
		this.matchTolerenceRows = matchTolerenceRows;
	}

	/**
	 * Calculates the match tolerence rows to pixels.
	 */
	protected void calculateMatchTolerencePixels() {
		if (refCharWidth != 0) {
			matchTolerencePixels = refCharWidth * matchTolerenceRows;
		} else {
			matchTolerencePixels = 16 * matchTolerenceRows;
		}
	}

	/**
	 * Compare the list of collected bitmaps to the reference bitmaps and
	 * interpret the best matching reference to a string.
	 * 
	 * @param bmpList
	 *            The list of bitmaps to compare.
	 * @param charMap
	 *            A map with character as key and corresponding bitmap as value.
	 * @return The resulting string
	 */
	protected String referenceCompare(List<Bitmap> bmpList,
			Map<Character, Bitmap> charMap) {
		calculateMatchTolerencePixels();
		StringBuffer result = new StringBuffer();
		ListIterator<Bitmap> li = bmpList.listIterator();
		Bitmap currentCharBmp;
		Character currentChar;
		/* Iterate over the target bitmap list. */
		while (li.hasNext()) {
			Bitmap bmp = li.next();
			Character bestChar = (char) 88;
			float bestScore = minInitMatchPercent;
			Iterator<Entry<Character, Bitmap>> it = charSet.iterator();
			/* Iterate over the reference bitmap list. */
			while (it.hasNext()) {
				int matching = matchTolerencePixels;
				int nonmatching = 0;
				float percent = 0;
				Map.Entry<Character, Bitmap> charSetEntry = it.next();
				currentChar = (Character) charSetEntry.getKey();
				currentCharBmp = (Bitmap) charSetEntry.getValue();
				/* Iterate over pixels in the target bitmap. */
				currentCharLoop:
				/*
				 * The spacing on y and x will probably not produce faster
				 * results, it will not have to read the whole bitmap anyway,
				 * but will make the comparison more spread out before it skips
				 * to the next character. Possibly this will improve accuracy. I
				 * have experimented with creating a set path to check but so
				 * far it has mostly decreased reading speed significantly..
				 */
				for (int y = 0; y < bmp.getHeight(); y += compareRowSpacing) {
					for (int x = 0; x < bmp.getWidth(); x += compareColSpacing) {
						int refPixel = currentCharBmp.getPixel(x, y);
						int foundPixel = bmp.getPixel(x, y);
						/* Compare pixels between target and reference. */
						if (refPixel == foundPixel) {
							matching++;
						} else {
							nonmatching++;
							/*
							 * If current gets a lower match percent than best
							 * previous, break the loop even if there are pixels
							 * left to compare.
							 */
							if (nonmatching % 2 == 0) { // check every other
								percent = ((float) matching / ((float) matching + (float) nonmatching)) * 100;
								if (percent < bestScore) {
									break currentCharLoop;
								}
							}
						}
					}
				}
				/*
				 * If current has a higher match percent than any before, update
				 * bestScore
				 */
				if (percent > bestScore) {
					bestScore = percent;
					bestChar = currentChar;
				}
			}
			result.append(bestChar);
		}
		return result.toString();
	}

	/**
	 * Sets the contrast for the bitmap to be scanned. To be replaced some day
	 * with a histogram method.
	 * 
	 * @param bmpOriginal
	 *            The bitmap to be contrasted.
	 * @param scaleonly
	 *            The amount of brightness to set at the start.
	 * @param scaletrans
	 *            The amount of contrast,
	 */
	public static Bitmap setContrast(Bitmap bmpOriginal, float scaleonly,
			float scaletrans) {
		int width, height;
		height = bmpOriginal.getHeight();
		width = bmpOriginal.getWidth();
		Bitmap bmpGrayscale = Bitmap.createBitmap(width, height,
				Bitmap.Config.RGB_565);
		Canvas c = new Canvas(bmpGrayscale);
		Paint paint = new Paint();
		ColorMatrix cm1 = new ColorMatrix();
		float scale1 = scaleonly + 1.f;
		cm1.set(new float[] {
			scale1, 0, 0, 0, 0,
			0, scale1, 0, 0, 0,
			0, 0, scale1, 0, 0,
			0, 0, 0, 1, 0 });
		ColorMatrixColorFilter f1 = new ColorMatrixColorFilter(cm1);
		paint.setColorFilter(f1);
		c.drawBitmap(bmpOriginal, 0, 0, paint);
		ColorMatrix cm2 = new ColorMatrix();
		float scale2 = scaletrans + 1.f;
		float translate = (-.5f * scale2 + .5f) * 255.f;
		cm2.set(new float[] {
			scale2, 0, 0, 0, translate,
			0, scale2, 0, 0, translate,
			0, 0, scale2, 0, translate,
			0, 0, 0, 1, 0 });
		ColorMatrixColorFilter f2 = new ColorMatrixColorFilter(cm2);
		paint.setColorFilter(f2);
		c.drawBitmap(bmpGrayscale, 0, 0, paint);
		return bmpGrayscale;
	}

}
