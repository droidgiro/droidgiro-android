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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
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
import android.widget.Toast;

/**
 * The main activity. Draws the views and shows the results in them.
 */
public final class CaptureActivity extends ListActivity implements
		SurfaceHolder.Callback {

	private static final String TAG = "DroidGiro.CaptureActivity";

	private static final float BEEP_VOLUME = 0.10f;
	private static final long VIBRATE_DURATION = 200L;
	private static final long SCAN_DELAY_MS = 1000L;

	private CaptureActivityHandler handler;
	private MediaPlayer mediaPlayer;
	private ResultListHandler resultListHandler;
	private ViewfinderView viewfinderView;

	private boolean hasSurface;
	private boolean paused = false;
	private boolean playBeep;
	private boolean vibrate;

	private static Invoice currentInvoice = new Invoice();

	private Button eraseButton;
	private Button scanButton;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

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
		currentInvoice = new Invoice();
		channel = getIntent().getStringExtra("channel");
		if (channel == null)
			finish();

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.capture);

		this.eraseButton = (Button) this.findViewById(R.id.send_erase);
		this.eraseButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (currentInvoice != null)
					sendInvoice(CaptureActivity.this, currentInvoice);

				// if (currentInvoice != null && currentInvoice.isComplete()) {
				scanButton.setText(getString(R.string.scan_state_scan));
				handler.sendEmptyMessage(R.id.pause);
				paused = true;
				// }
				if (currentInvoice != null)
					currentInvoice.initFields();
				resultListHandler.clear();
//				handler.sendEmptyMessage(R.id.new_invoice);
				onContentChanged();
			}
		});

		this.scanButton = (Button) this.findViewById(R.id.pause);
		this.scanButton.setText(getString(R.string.scan_state_pause));
		this.scanButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (paused) {
					paused = false;
					handler.sendEmptyMessage(R.id.resume);
					scanButton.setText(getString(R.string.scan_state_pause));
				} else {
					paused = true;
					handler.sendEmptyMessage(R.id.pause);
					scanButton.setText(getString(R.string.scan_state_scan));
				}
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

	public boolean isPaused() {
		return paused;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	public void handleDecode(final Invoice invoice, int fieldsFound,
			Bitmap debugBmp) {
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
		int fieldsScanned = invoice.getLastFieldsDecoded();
		if (fieldsScanned > 0) {
			boolean scanContainsNewData = false;

			/*
			 * The following segment will copy data from scanned invoice object
			 * into our currentInvoice and keep track of new data read.
			 * Beep/Vibrate will only occur if scan contains new data.
			 */
			
			if ((fieldsScanned & Invoice.AMOUNT_FIELD) == Invoice.AMOUNT_FIELD) {
				if (!(currentInvoice.getAmount() == invoice.getAmount() && currentInvoice
						.getAmountFractional() == invoice.getAmountFractional())) {
					currentInvoice.setAmount(invoice.getAmount(), invoice
							.getAmountFractional());
					scanContainsNewData = true;
				}
			}

			if ((fieldsScanned & Invoice.DOCUMENT_TYPE_FIELD) == Invoice.DOCUMENT_TYPE_FIELD) {
				if (currentInvoice.getInternalDocumentType() != invoice
						.getInternalDocumentType()) {
					currentInvoice.setDocumentType(invoice
							.getInternalDocumentType());
					scanContainsNewData = true;
				}
			}

			if ((fieldsScanned & Invoice.GIRO_ACCOUNT_FIELD) == Invoice.GIRO_ACCOUNT_FIELD) {
				Log.v(TAG, "Giro accout scanned. Current account = " + currentInvoice.getGiroAccount() + ". New invoice giro account = " + invoice.getGiroAccount());
				if (!invoice.getGiroAccount().equals(currentInvoice.getGiroAccount())) {
					currentInvoice.setRawGiroAccount(invoice.getRawGiroAccount());
					Log.v(TAG, "Copied giro account = " + invoice.getGiroAccount());
					scanContainsNewData = true;
				}
			}

			if ((fieldsScanned & Invoice.REFERENCE_FIELD) == Invoice.REFERENCE_FIELD) {
				if (!invoice.getReference().equals(currentInvoice.getReference())) {
					currentInvoice.setReference(invoice.getReference());
					scanContainsNewData = true;
				}
			}
			
			if(scanContainsNewData)
				playBeepSoundAndVibrate();

		}
		Log.v(TAG, "CurrentInvoice = " + currentInvoice);
		if (currentInvoice.isReferenceDefined())
			resultListHandler.setReference(currentInvoice.getReference());
		if (currentInvoice.isAmountDefined())
			resultListHandler.setAmount(currentInvoice.getCompleteAmount());
		if (currentInvoice.isGiroAccountDefined())
			resultListHandler.setAccount(currentInvoice.getGiroAccount());
		if (resultListHandler.hasNewData()) {
			resultListHandler.setNewData(false);
		}

		Log.v(TAG, "Got invoice " + invoice);

		/* If scan on every hit */

		// int fieldsScanned = invoice.getLastFieldsDecoded();
		// if (fieldsScanned > 0) {
		// playBeepSoundAndVibrate();
		// final List<NameValuePair> params = new ArrayList<NameValuePair>();
		// if ((fieldsScanned & Invoice.AMOUN�T_FIELD) == Invoice.AMOUNT_FIELD)
		// params.add(new BasicNameValuePair("amount", invoice
		// .getCompleteAmount()));
		// if ((fieldsScanned & Invoice.DOCUMENT_TYPE_FIELD) ==
		// Invoice.DOCUMENT_TYPE_FIELD)
		// params.add(new BasicNameValuePair("type", invoice.getType()));
		// if ((fieldsScanned & Invoice.GIRO_ACCOUNT_FIELD) ==
		// Invoice.GIRO_ACCOUNT_FIELD)
		// params.add(new BasicNameValuePair("account", invoice
		// .getGiroAccount()));
		// if ((fieldsScanned & Invoice.REFERENCE_FIELD) ==
		// Invoice.REFERENCE_FIELD)
		// params.add(new BasicNameValuePair("reference", invoice
		// .getReference()));
		// // sendFields(params);
		//
		// }

		// if (invoice.isComplete()) {
		// resultListHandler.setSent(true);
		// this.scanButton.setText(getString(R.string.scan_state_scan));
		// onContentChanged();
		// } else
		if (!paused) {
			handler
					.sendEmptyMessageDelayed(R.id.restart_preview,
							SCAN_DELAY_MS);
		}
		onContentChanged();
	}

	private void sendInvoice(final Context context, final Invoice invoice) {
		List<NameValuePair> fields = new ArrayList<NameValuePair>();
		if (invoice.isAmountDefined())
			fields.add(new BasicNameValuePair("amount", invoice
					.getCompleteAmount()));
		if (invoice.isDocumentTypeDefined())
			fields.add(new BasicNameValuePair("type", invoice.getType()));
		if (invoice.isGiroAccountDefined())
			fields.add(new BasicNameValuePair("account", invoice
					.getGiroAccount()));
		if (invoice.isReferenceDefined())
			fields.add(new BasicNameValuePair("reference", invoice
					.getReference()));

		if (fields.size() > 0)
			sendFields(context, fields);
	}

	private void sendFields(final Context context,
			final List<NameValuePair> fields) {
		new Thread(new Runnable() {
			public void run() {
				fields.add(new BasicNameValuePair("channel",
						CaptureActivity.this.channel));
				try {
					boolean res = CloudClient.postFields(fields);
					Log.v(TAG, "Result from posting invoice " + fields
							+ " to channel " + channel + ": " + res);
					final String msg = (res ? "Fält har skickats till webbläsaren"
							: "Kunde inte skicka fält till webbläsaren");

					handler.post(new Runnable() {

						public void run() {
							Toast.makeText(context, msg, Toast.LENGTH_LONG)
									.show();
						}
					});

				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}

			}
		}).start();
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
