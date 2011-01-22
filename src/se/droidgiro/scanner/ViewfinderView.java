/*
 * Copyright (C) 2008 ZXing authors
 * Copyright (C) 2011 DroidGiro authors authors
 *
 * This file is mostly based on the file
 * com.google.zxing.client.android.ViewfinderView
 * by ZXing Authors
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

package se.droidgiro.scanner;

import se.droidgiro.R;
import se.droidgiro.scanner.camera.CameraManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder
 * rectangle and partial transparency outside it, as well as the laser scanner
 * animation and result points.
 * 
 * @author dswitkin@google.com (Daniel Switkin) (original author)
 */
public final class ViewfinderView extends View {

	private static final long ANIMATION_DELAY = 100L;

	private final Paint paint;
	private final int maskColor;
	private final int frameColor;

	// This constructor is used when the class is built from an XML resource.
	public ViewfinderView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// Initialize these once for performance rather than calling them every
		// time in onDraw().
		paint = new Paint();
		Resources resources = getResources();
		maskColor = resources.getColor(R.color.viewfinder_mask);
		frameColor = resources.getColor(R.color.viewfinder_frame);
	}

	@Override
	public void onDraw(Canvas canvas) {
		Rect frame = CameraManager.get().getFramingRect();
		if (frame == null) {
			return;
		}
		int width = canvas.getWidth();
		int height = canvas.getHeight();

		paint.setColor(maskColor);
		canvas.drawRect(0, 0, width, frame.top, paint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
				paint);
		canvas.drawRect(0, frame.bottom + 1, width, height, paint);

		paint.setColor(frameColor);
		paint.setStyle(Paint.Style.STROKE);
		canvas
				.drawRect(frame.left, frame.top, frame.right, frame.bottom,
						paint);
		paint.setStyle(Paint.Style.FILL);
		postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top,
				frame.right, frame.bottom);
	}

	public void drawViewfinder() {
		invalidate();
	}

}
