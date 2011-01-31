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
 * The bitmap analyzer for DroidGiro.
 * Provides methods to determine coordinates of black and white sections in a
 * bitmap, compare valid black sections to a list of reference bitmaps, and
 * calculate whitespace amount based on the mean width of valid black sections.
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
	 * The minimum amount of characters the resulting string must have for the
	 * scanner to return the string.
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
	 * A list of Section objects.
	 */
	protected List<Section> sectionList;
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
		sectionList = getSections(contrastBmp);
		if (sectionList != null) {
			/*
			 * It seems best to use the original bitmap for this method and
			 * change its contrast again. If the already contrasted bitmap is
			 * resized it produces a lot of artefacts which interferes with
			 * Scanner's bitmap comparison. It impacts performance, but it will
			 * have to do for now.
			 */
			sectionList = uniformBitmapList(targetBmp, sectionList,
					refCharWidth, refCharHeight);
			resultString = bitmapSectionComparison(sectionList, charMap);
		} else {
			resultString = null;
		}
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
	 * @return A bitmap composed of all the character bitmaps that were found,
	 *         contrasted.
	 */
	public Bitmap getContrastedDebugBitmap() {
		foundContrastedBmps = new ArrayList<Bitmap>();
		for (Section section : sectionList) {
			if (section.valid && !section.whitespace) {
				foundContrastedBmps.add(section.scaledContrastedBmp);
			}
		}
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
		foundNonContrastedBmps = new ArrayList<Bitmap>();
		for (Section section : sectionList) {
			if (section.valid && !section.whitespace) {
				foundNonContrastedBmps.add(section.scaledBmp);
			}
		}
		nonContrastedDebugBmp = composeFromBitmapList(foundNonContrastedBmps,
				false);
		return nonContrastedDebugBmp;
	}

	/**
	 * Section objects holds the coordinates, corresponding bitmaps from the
	 * target bitmap section analysis, and the resulting character from the
	 * reference comparison.
	 */
	public class Section {
		private Rect position;
		public Boolean whitespace;
		public Boolean valid = false;
		public int whitespaceCount = 0;
		public int left = 0;
		public int right = 0;
		public int top = 0;
		public int bottom = 0;
		private int height;
		private int width;
		public Bitmap scaledBmp;
		public Bitmap scaledContrastedBmp;
		public Character bestChar;

		public Section(Boolean whitespace, int left, int right) {
			this.whitespace = whitespace;
			this.left = left;
			this.right = right;
		}

		// TODO: Must check if Rect needs to be recalculated each time.
		public Rect getRect() {
			position = new Rect(left, top, right, bottom);
			return position;
		}

		public void setRect(Rect rect) {
			left = rect.left;
			top = rect.top;
			right = rect.right;
			bottom = rect.bottom;
		}

		public int getWidth() {
			width = right - left;
			return width;
		}

		public int getHeight() {
			height = bottom - top;
			return height;
		}
	}

	/**
	 * Determines coordinates of valid black and white sections, checks if
	 * black sections are valid and calculates whitespace amount based on the
	 * mean width of valid black sections.
	 * 
	 * @param bmp
	 *            The bitmap to be scanned.
	 * @return A list of Section objects or null if no valid sections were found.
	 */
	protected List<Section> getSections(Bitmap bmp) {
		/* Scan columns */
		List<Section> sectionList = new ArrayList<Section>();
		long fullCol = black * targetBmpHeight;
		long emptyCol = white * targetBmpHeight;
		int blackSections = 0;
		int lastFalse = -2;
		int lastTrue = -2;
		int lastLeftBlack = -2;
		int lastRightBlack = -2;
		int lastLeftWhite = -2;
		int lastRightWhite = -2;
		int lastTopBlack = -2;
		int lastBottomBlack = -2;
		int lastTopWhite = -2;
		int lastBottomWhite = -2;
		for (int x = 0; x < targetBmpWidth; ++x) {
			int[] colPixels = new int[targetBmpHeight];
			bmp.getPixels(colPixels, 0, 1, x, 0, 1, targetBmpHeight);
			long colSum = 0;
			for (int p : colPixels) {
				colSum += p;
			}
			if (colSum < emptyCol && colSum > fullCol) {
				lastTrue = x;
				if (x == 0) {
					lastLeftBlack = x;
				} else if (x - 1 == lastFalse) {
					lastLeftBlack = x;
					lastRightWhite = x -1;
					Section s = new Section(true, lastLeftWhite, lastRightWhite);
					sectionList.add(s);
				} else if (x == targetBmpWidth - 1
						&& lastLeftBlack != -2) {
					lastRightBlack = x;
					Section s = new Section(false, lastLeftBlack, lastRightBlack);
					sectionList.add(s);
					blackSections++;
				}
			} else {
				lastFalse = x;
				if (x == 0) {
					lastLeftWhite = x;
				} else if (x - 1 == lastTrue) {
					lastRightBlack = x -1;
					lastLeftWhite = x;
					Section s = new Section(false, lastLeftBlack, lastRightBlack);
					sectionList.add(s);
					blackSections++;
				} else if (x == targetBmpWidth - 1
						&& lastLeftWhite != -2) {
					lastRightWhite = x;
					Section s = new Section(true, lastLeftWhite, lastRightWhite);
					sectionList.add(s);
				}
			}
		}
		/* Stop if gathered nonblank cols are too few */
		if (blackSections < minResultLength) {
			return null;
		}
		/* Scan rows of gathered cols containing black pixels */
		for (Section section : sectionList) {
			lastFalse = -2;
			lastTrue = -2;
			lastTopBlack = -2;
			lastBottomBlack = -2;
			lastTopWhite = -2;
			lastBottomWhite = -2;
			if (!section.whitespace) {
				List<Rect> verticalRects = new ArrayList<Rect>();
				int sectionWidth = section.getWidth();
				int emptyRow = sectionWidth * white;
				for (int y = 0; y < targetBmpHeight; ++y) {
					int[] rowPixels = new int[sectionWidth];
					bmp.getPixels(rowPixels, 0, targetBmpWidth, section.left, y,
							sectionWidth, 1);
					long rowSum = 0;
					for (int p : rowPixels) {
						rowSum += p;
					}
					if (rowSum < emptyRow) {
						lastTrue = y;
						if (y == 0) {
							lastTopBlack = y;
						} else if (y == targetBmpHeight - 1 &&
								lastTopBlack != -2) {
							lastBottomBlack = y;
							Rect r = new Rect(section.left, lastTopBlack,
									section.right, lastBottomBlack);
							verticalRects.add(r);
						} else if (y - 1 == lastFalse) {
							lastTopBlack = y;
							lastBottomWhite = y -1;
							//ignore vertical whitespace
						}
					} else {
						lastFalse = y;
						if (y == 0) {
							lastTopWhite = y;
						} else if (y == targetBmpHeight - 1 &&
								lastTopWhite != -2) {
							lastBottomWhite = y;
							//ignore vertical whitespace
						} else if (y - 1 == lastTrue) {
							lastBottomBlack = y -1;
							lastTopWhite = y;
							Rect r = new Rect(section.left, lastTopBlack,
									section.right, lastBottomBlack);
							verticalRects.add(r);
						}
					}
				}
				/* Check if list of rects in nonblank section contains valid */
				int validRects = 0;
				for (Rect r : verticalRects) {
					if (isValidCharRect(r)) {
						validRects++;
						section.valid = true;
						section.setRect(r);
					}
				}
				if (validRects != 1) {
					section.valid = false;
				}
			}
		}
		int validBlack = 0;
		int validBlackWidthSum = 0;
		int validBlackPlusInvalidWhiteSum = 0;
		ListIterator<Section> sectionIter = sectionList.listIterator();
		while (sectionIter.hasNext()) {
			Section section = sectionIter.next();
			int sectionIndex = sectionIter.nextIndex();
			if (!section.whitespace) {
				if (!section.valid) {
				/* Set invalid nonblank section to whitespace */
					section.whitespace = true;
				} else if (section.valid && sectionIndex != 1) {
					int prevSectionIndex = sectionIndex -2;
					Section prevSection = sectionList.get(prevSectionIndex);
					if (prevSection.whitespace
							&& prevSection.getWidth() < section.getWidth() ) {
						validBlack++;
						validBlackWidthSum += section.getWidth();
						validBlackPlusInvalidWhiteSum += (section.getWidth() +
								prevSection.getWidth());
					}
				}
			}
		}
		/* Stop if no valid black sections are found */
		if (validBlack < minResultLength) {
			return null;
		}
		/* Calculate whitespace width */
		int whitespaceWidth = validBlackPlusInvalidWhiteSum / validBlack;
		int meanValidBlackWidth = validBlackWidthSum / validBlack;
		/* Join consecutive whitespace sections */
		ListIterator<Section> whitespaceIter = sectionList.listIterator();
		while (whitespaceIter.hasNext()) {
			Section section = whitespaceIter.next();
			int sectionIndex = whitespaceIter.nextIndex();
			if (section.whitespace && sectionIndex != 1) {
				Section prevSection = sectionList.get(sectionIndex - 2);
				if (prevSection.whitespace) {
					prevSection.right = section.right;
					whitespaceIter.remove();
				}
			}
		}
		/* Calculate whitespace amount */
		for (Section section : sectionList) {
			if (section.whitespace && section.getWidth() > meanValidBlackWidth) {
				section.valid = true;
				section.whitespaceCount = section.getWidth() / whitespaceWidth;
			}
		}
		return sectionList;
	}

	/**
	 * Determines whether a Rect has the correct dimensions for a character.
	 * @param rect The Rect to be validated.
	 */
	protected Boolean isValidCharRect(Rect rect) {
		if ((rect.width() > charMinWidth)
			&& (rect.width() < charMaxWidth)
			&& (rect.height() > charMinHeight)
			&& (rect.height() < charMaxHeight)) {
			if (charAlwaysPortrait) {
				if (rect.height() > rect.width()) {
					return true;
				}
			} else {
				return true;
			}
		}
		return false;
	}

	/**
	 * Adds uniformly scaled bitmaps to the Section list based on their
	 * coordinates.
	 * 
	 * @param targetBmp
	 *            The target bitmap.
	 * @param sectionList
	 *            The list of Section objects.
	 * @param toWidth
	 *            The target width to scale the bitmaps into.
	 * @param toHeight
	 *            The target height to scale the bitmaps into.
	 * @return The list of Section objects with bitmaps added.
	 */
	protected List<Section> uniformBitmapList(Bitmap targetBmp,
			List<Section> sectionList, int toWidth, int toHeight) {
		for (Section section : sectionList) {
			if (!section.whitespace && section.valid) {
				Rect targetRect = section.getRect();
				int[] pixels;
				pixels = new int[targetRect.height() * targetRect.width()];
				targetBmp.getPixels(pixels, 0, targetRect.width(), targetRect.left,
						targetRect.top, targetRect.width(), targetRect.height());
				Bitmap nonscaledBmp = Bitmap.createBitmap(pixels, 0, targetRect
						.width(), targetRect.width(), targetRect.height(),
						Bitmap.Config.RGB_565);
				Bitmap scaledBmp = Bitmap.createScaledBitmap(nonscaledBmp, toWidth,
						toHeight, true);
				section.scaledBmp = scaledBmp;
				Bitmap scaledContrastedBmp = setContrast(scaledBmp, colorScale,
						colorScaleTranslate);
				section.scaledContrastedBmp = scaledContrastedBmp;
			}
		}
		return sectionList;
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
		ListIterator<Bitmap> bmpIterator = bmpList.listIterator();
		int i = 0;
		while (bmpIterator.hasNext()) {
			Bitmap bmp = bmpIterator.next();
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
	 * @param sectionList
	 *            The list of Section objects.
	 * @param charMap
	 *            A map with character as key and corresponding bitmap as value.
	 * @return The resulting string
	 */
	protected String bitmapSectionComparison(List<Section> sectionList,
			Map<Character, Bitmap> charMap) {
		calculateMatchTolerencePixels();
		StringBuffer result = new StringBuffer();
		Bitmap currentCharBmp;
		Character currentChar;
		int midCharRow = refCharHeight/2;
		/* Iterate over the Section list. */
		for (Section section : sectionList) {
			if (!section.whitespace && section.valid) {
				Bitmap bmp = section.scaledContrastedBmp;
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
					/* Iterate over pixels in the target section. */
					currentCharLoop:
					for (int y = 0; y != -1;) {
						for (int x = 0; x < refCharWidth; x += compareColSpacing) {
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
									percent = ((float) matching / ((float) matching
											+ (float) nonmatching)) * 100;
									if (percent < bestScore) {
										break currentCharLoop;
									}
								}
							}
						}
						/* Alternate reading of top/bottom until middle row.
						TODO: This needs to check so that midCharRow can
						actually be reached */
						if (y == midCharRow) {
							y = -1;
						} else {
							if (y < midCharRow) {
								y += compareRowSpacing;
								y = refCharHeight - y;
							} else {
								y = refCharHeight - y;
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
			section.bestChar = bestChar;
			}
		}
		for (Section section : sectionList) {
			if (section.valid) {
				if (!section.whitespace) {
					result.append(section.bestChar);
				} else {
					for(int i=0; i < section.whitespaceCount; i++) {
						result.append((char)32);
					}
				}
			}
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
