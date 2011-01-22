/*
 * Copyright (C) 2008 ZXing authors
 * Copyright (C) 2011 DroidGiro authors
 *
 * This file is mostly based on
 * com.google.zxing.client.android.CaptureActivity
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import se.droidgiro.About;
import se.droidgiro.PreferencesActivity;
import se.droidgiro.R;
import se.droidgiro.scanner.camera.CameraManager;
import se.droidgiro.scanner.resultlist.ResultListAdapter;
import se.droidgiro.scanner.resultlist.ResultListHandler;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

/**
 * The main activity. Draws the views and shows the results in them.
 */
public final class CaptureActivity extends ListActivity implements
		SurfaceHolder.Callback {

	private static final String TAG = "DroidGiro.CaptureActivity";

	private static final float BEEP_VOLUME = 0.10f;
	private static final long VIBRATE_DURATION = 200L;
	private static final long SCAN_DELAY_MS = 1500L;

	private CaptureActivityHandler handler;

	private ViewfinderView viewfinderView;
	private MediaPlayer mediaPlayer;
	private boolean hasSurface;
	private boolean playBeep;
	private boolean vibrate;

	private static Invoice currentInvoice = null;
	private ResultListHandler resultListHandler;

	private Button eraseButton;
	private Button scanButton;

	/**
	 * When the beep has finished playing, rewind to queue up another one.
	 */
	private final OnCompletionListener beepListener = new OnCompletionListener() {
		public void onCompletion(MediaPlayer mediaPlayer) {
			mediaPlayer.seekTo(0);
		}
	};

	private String channel;

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (currentInvoice != null) {
			super.onListItemClick(l, v, position, id);
			if (position == 0) {
				resultListHandler.setReference(null);
				currentInvoice.initReference();
			} else if (position == 1) {
				resultListHandler.setAmount(null);
				currentInvoice.initAmount();
			} else {
				resultListHandler.setAccount(null);
				currentInvoice.initGiroAccount();
			}
			onContentChanged();
		}
	}

	ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Log.d(TAG, "onCreate");
		channel = getIntent().getStringExtra("channel");
		if (channel == null)
			finish();

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.capture);

		this.eraseButton = (Button) this.findViewById(R.id.erase);
		this.eraseButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(currentInvoice != null)
					currentInvoice.initFields();
				resultListHandler.clear();
				handler.sendEmptyMessage(R.id.new_invoice);
				onContentChanged();
			}
		});

		this.scanButton = (Button) this.findViewById(R.id.scan);
		this.scanButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				handler.sendEmptyMessage(R.id.restart_preview);
			}
		});

		resultListHandler = new ResultListHandler(this);
		List<ResultListHandler.ListItem> resultList = resultListHandler
				.getList();
		ResultListAdapter adapter = new ResultListAdapter(this,
				R.layout.result_list_item, resultList);
		setListAdapter(adapter);

		CameraManager.init(getApplication());
		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		handler = null;
		hasSurface = false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		resetStatusView();
		Log.d(TAG, "onResume");

		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		playBeep = prefs.getBoolean(PreferencesActivity.KEY_PLAY_BEEP, true);
		if (playBeep) {
			// See if sound settings overrides this
			AudioManager audioService = (AudioManager) getSystemService(AUDIO_SERVICE);
			if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
				playBeep = false;
			}
		}
		vibrate = prefs.getBoolean(PreferencesActivity.KEY_VIBRATE, false);
		initBeepSound();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		CameraManager.get().closeDriver();
	}

	@Override
	protected void onDestroy() {
		// inactivityTimer.shutdown();
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about: {
			Intent intent = new Intent(this, About.class);
			startActivity(intent);
			break;
		}
		case R.id.settings: {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			intent.setClassName(this, PreferencesActivity.class.getName());
			startActivity(intent);
			break;
		}
		}
		return super.onOptionsItemSelected(item);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	public void handleDecode(final Invoice invoice, int fieldsFound,
			Bitmap debugBmp) {
		// inactivityTimer.onActivity();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		ImageView debugImageView = (ImageView) findViewById(R.id.debug_image_view);
		if (prefs.getBoolean(PreferencesActivity.KEY_DEBUG_IMAGE, false)
				&& (debugBmp != null)) {
			debugImageView.setVisibility(View.VISIBLE);
			debugImageView.setImageBitmap(debugBmp);
		} else if (debugImageView.getVisibility() != View.GONE) {
			debugImageView.setVisibility(View.GONE);
		}
		currentInvoice = invoice;

		if(invoice.isReferenceDefined())
			resultListHandler.setReference(invoice.getReference());
		if(invoice.isAmountDefined())
			resultListHandler.setAmount(invoice.getCompleteAmount());
		if(invoice.isGiroAccountDefined())
			resultListHandler.setAccount(invoice.getGiroAccount());
		if (resultListHandler.hasNewData()) {
			playBeepSoundAndVibrate();
			resultListHandler.setNewData(false);
		}

		Log.v(TAG, "Got invoice " + invoice);
		int fieldsScanned = invoice.getLastFieldsDecoded();
		if (fieldsScanned > 0) {
			final List<NameValuePair> params = new ArrayList<NameValuePair>();
			if ((fieldsScanned & Invoice.AMOUNT_FIELD) == Invoice.AMOUNT_FIELD)
				params.add(new BasicNameValuePair("amount", invoice
						.getCompleteAmount()));
			if ((fieldsScanned & Invoice.DOCUMENT_TYPE_FIELD) == Invoice.DOCUMENT_TYPE_FIELD)
				params.add(new BasicNameValuePair("type", invoice
						.getInternalDocumentType()));
			if ((fieldsScanned & Invoice.GIRO_ACCOUNT_FIELD) == Invoice.GIRO_ACCOUNT_FIELD)
				params.add(new BasicNameValuePair("account", invoice
						.getGiroAccount()));
			if ((fieldsScanned & Invoice.REFERENCE_FIELD) == Invoice.REFERENCE_FIELD)
				params.add(new BasicNameValuePair("reference", invoice
						.getReference()));

			new Thread(new Runnable() {
				public void run() {
					params.add(new BasicNameValuePair("channel",
							CaptureActivity.this.channel));
					try {
						boolean res = CloudClient.postFields(params);
						Log.v(TAG, "Result from posting invoice " + params
								+ " to channel " + channel + ": " + res);
						invoice.initFields();
						// initLocalInvoice();
						currentInvoice = null;
					} catch (Exception e) {
						Log.e(TAG, e.getMessage(), e);
					}

				}
			}).start();
		}
		if (invoice.isComplete()) {
			resultListHandler.setSent(true);
		} else {
			handler
					.sendEmptyMessageDelayed(R.id.restart_preview,
							SCAN_DELAY_MS);
		}
		onContentChanged();
	}

	/**
	 * Creates the beep MediaPlayer in advance so that the sound can be
	 * triggered with the least latency possible.
	 */
	private void initBeepSound() {
		if (playBeep && mediaPlayer == null) {
			// The volume on STREAM_SYSTEM is not adjustable, and users found it
			// too loud,
			// so we now play on the music stream.
			setVolumeControlStream(AudioManager.STREAM_MUSIC);
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mediaPlayer.setOnCompletionListener(beepListener);

			AssetFileDescriptor file = getResources().openRawResourceFd(
					R.raw.beep);
			try {
				mediaPlayer.setDataSource(file.getFileDescriptor(), file
						.getStartOffset(), file.getLength());
				file.close();
				mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
				mediaPlayer.prepare();
			} catch (IOException e) {
				mediaPlayer = null;
			}
		}
	}

	private void playBeepSoundAndVibrate() {
		if (playBeep && mediaPlayer != null) {
			mediaPlayer.start();
		}
		if (vibrate) {
			Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
			vibrator.vibrate(VIBRATE_DURATION);
		}
	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		try {
			CameraManager.get().openDriver(surfaceHolder);
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			return;
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.w(TAG, "Unexpected error initializating camera", e);
			return;
		}
		if (handler == null) {
			handler = new CaptureActivityHandler(this);
		}
	}

	private void resetStatusView() {
		viewfinderView.setVisibility(View.VISIBLE);
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

}
