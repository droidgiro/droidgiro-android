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

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.agiro.scanner.android.camera.CameraManager;
import com.google.zxing.PlanarYUVLuminanceSource;

final class DecodeHandler extends Handler {

	private static final String TAG = "aGiro.DecodeHandler";

	private final CaptureActivity activity;
	private ScanResources scanResources;
	private SharedPreferences prefs;
	private Invoice invoice;

	private Scanner scanner;

	DecodeHandler(CaptureActivity activity) {
		this.activity = activity;
		invoice = new Invoice();
		scanResources = new ScanResources(activity);
		scanner = new Scanner(scanResources);
		prefs =  PreferenceManager.getDefaultSharedPreferences(activity);
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
		scanner.setTargetBitmap(bmp);
		scanner.scan();
		resultString = scanner.getResultString();

		if (resultString != null) {
			int fieldsFound = invoice.parse(resultString);
			long end = System.currentTimeMillis();
			Log.d(TAG, "Found result (" + (end - start) + " ms):\n"
					+ resultString);
			if(fieldsFound != 0) {
				Message message = Message.obtain(activity.getHandler(),
						R.id.decode_succeeded, invoice);
				Bitmap debugBmp = null;
				if (prefs.getBoolean(PreferencesActivity.KEY_DEBUG_IMAGE, false)) {
					debugBmp = scanner.getDebugBitmap();
				}
				Bundle bundle = new Bundle();
				bundle.putParcelable(DecodeThread.DEBUG_BITMAP, debugBmp);
				bundle.putInt(Invoice.FIELDS_FOUND, fieldsFound);
				message.setData(bundle);
				// Log.d(TAG, "Sending decode succeeded message...");
				message.sendToTarget();
			} else {
				Message message = Message.obtain(activity.getHandler(),
						R.id.decode_failed);
				message.sendToTarget();
			}
		} else {
			Message message = Message.obtain(activity.getHandler(),
					R.id.decode_failed);
			message.sendToTarget();
		}
	}

}
