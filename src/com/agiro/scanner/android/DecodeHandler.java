/*
 * Copyright (C) 2010 ZXing authors
 *
 * Parts of the file was modified by aGiro authors
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

import java.util.HashMap;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.agiro.scanner.android.camera.CameraManager;
import com.google.zxing.PlanarYUVLuminanceSource;

final class DecodeHandler extends Handler {

	private static final String TAG = DecodeHandler.class.getSimpleName();

	private final CaptureActivity activity;

	private Invoice invoice;

	DecodeHandler(CaptureActivity activity) {
		this.activity = activity;
		invoice = new Invoice();
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case R.id.decode:
			decode((byte[]) message.obj, message.arg1, message.arg2);
			break;
		case R.id.quit:
			Looper.myLooper().quit();
			break;
		}
	}

	/**
	 * Decode the data within the viewfinder rectangle, and time how long it
	 * took.
	 * 
	 * @param data
	 *            The YUV preview frame.
	 * @param width
	 *            The width of the preview frame.
	 * @param height
	 *            The height of the preview frame.
	 */
	private void decode(byte[] data, int width, int height) {
		long start = System.currentTimeMillis();
		String resultString = null;
		PlanarYUVLuminanceSource source = CameraManager.get()
				.buildLuminanceSource(data, width, height);
		Bitmap bmp = source.renderCroppedGreyscaleBitmap();
		ScanResult scanResult;
		scanResult = new ScanResult(bmp);
		resultString = scanResult.getResultString();

		if (resultString != null) {
			invoice.decode(resultString);
			Bitmap debugBmp = scanResult.getDebugBitmap();
			long end = System.currentTimeMillis();
			Log.d(TAG, "Found result (" + (end - start) + " ms):\n"
					+ resultString);
			Message message = Message.obtain(activity.getHandler(),
					R.id.decode_succeeded, invoice);
			Bundle bundle = new Bundle();
			bundle.putParcelable(DecodeThread.DEBUG_BITMAP, debugBmp);
			message.setData(bundle);
			// Log.d(TAG, "Sending decode succeeded message...");
			message.sendToTarget();
		} else {
			Message message = Message.obtain(activity.getHandler(),
					R.id.decode_failed);
			message.sendToTarget();
		}
	}

}
