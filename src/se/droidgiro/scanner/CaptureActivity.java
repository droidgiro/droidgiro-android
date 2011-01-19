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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import se.droidgiro.scanner.camera.CameraManager;

import android.app.ListActivity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.preference.PreferenceManager;
import android.view.Menu;
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
import android.widget.SimpleAdapter;


/**
 * The main activity. Draws the views and shows the results in them.
 */
public final class CaptureActivity extends ListActivity implements SurfaceHolder.Callback {

  private static final String TAG = "DroidGiro.CaptureActivity";

  private static final int SETTINGS_ID = Menu.FIRST;

  private static final float BEEP_VOLUME = 0.10f;
  private static final long VIBRATE_DURATION = 200L;
  private static final long SCAN_DELAY_MS = 1500L;

  private static final String PACKAGE_NAME = "se.droidgiro.scanner";

  private CaptureActivityHandler handler;

  private ViewfinderView viewfinderView;
  private MediaPlayer mediaPlayer;
  private boolean hasSurface;
  private boolean playBeep;
  private boolean vibrate;
  private boolean copyToClipboard;
//  private InactivityTimer inactivityTimer;

  private static final ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
  private static String reference = null;
  private static String incomingReference = null;
  private static String amount = null;
  private static String incomingAmount = null;
  private static String account = null;
  private static String incomingAccount = null;

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

private String identifier;

private String channel;

/*
  private final DialogInterface.OnClickListener aboutListener =
      new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialogInterface, int i) {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.zxing_url)));
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      startActivity(intent);
    }
  };
*/
  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    if      (position == 0) { reference = null; }
    else if (position == 1) { amount = null; }
    else                    { account = null; }
    populateList(reference, amount, account);
    onContentChanged();
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
    identifier = getIntent().getStringExtra("identifier");
    try {
    	channel = CloudClient.register(identifier);
    } catch (Exception e) {
    	finish();
    }
    if(channel == null) 
    	finish();
	
    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.capture);

    this.eraseButton = (Button)this.findViewById(R.id.erase);
    this.eraseButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        handler.sendEmptyMessage(R.id.new_invoice);
        reference = null;
        amount = null;
        account = null;
        populateList(reference, amount, account);
        onContentChanged();
      }
    });

    this.scanButton = (Button)this.findViewById(R.id.scan);
    this.scanButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
      handler.sendEmptyMessage(R.id.restart_preview);
      }
    });

    SimpleAdapter adapter = new SimpleAdapter(this, list,
    R.layout.result_list_item,
    new String[] {"data_type","data"},
    new int[] {R.id.data_type,R.id.data});
    populateList(reference, amount, account);
    setListAdapter(adapter);

    CameraManager.init(getApplication());
    viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    handler = null;
    hasSurface = false;
//    inactivityTimer = new InactivityTimer(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    resetStatusView();
      Log.d(TAG, "onResume");

    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
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
//    inactivityTimer.shutdown();
    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
      return super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, SETTINGS_ID, 0, R.string.preferences_name);
	return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case SETTINGS_ID: {
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

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  public void handleDecode(final Invoice invoice, int fieldsFound, Bitmap debugBmp) {
//    inactivityTimer.onActivity();
	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
	ImageView debugImageView = (ImageView) findViewById(R.id.debug_image_view);
    if (prefs.getBoolean(PreferencesActivity.KEY_DEBUG_IMAGE, false) && (debugBmp != null)) {
			debugImageView.setVisibility(View.VISIBLE);
			debugImageView.setImageBitmap(debugBmp);
    } else if (debugImageView.getVisibility() != View.GONE) {
		debugImageView.setVisibility(View.GONE);
	}

	// This needs to check if a change has occured to invoice somehow. Now it
	// beeps on any result, valid or not.
	try {
		incomingReference = invoice.getReference();
		if (!incomingReference.equals(reference)) {
			playBeepSoundAndVibrate();
			reference = incomingReference;
		}
	} catch(NullPointerException e){}
	try {
		incomingAmount = invoice.getCompleteAmount();
		if (!incomingAmount.equals(amount)) {
			playBeepSoundAndVibrate();
			amount = incomingAmount;
		}
	} catch(NullPointerException e) {}
	try {
		incomingAccount = invoice.getGiroAccount();
		if (!incomingAccount.equals(account)) {
			playBeepSoundAndVibrate();
			account = incomingAccount;
		}
	} catch(NullPointerException e) {}

    Log.v(TAG, "Got invoice " + invoice);
    if(invoice.isComplete()) {
    	new Thread(new Runnable() {
			
			public void run() {
	        	List<NameValuePair> params = new ArrayList<NameValuePair>();
	        	params.add(new BasicNameValuePair("identifier", CaptureActivity.this.identifier));
	        	params.add(new BasicNameValuePair("reference", invoice.getReference()));
	        	params.add(new BasicNameValuePair("amount", invoice.getCompleteAmount()));
	        	params.add(new BasicNameValuePair("document_type", invoice.getInternalDocumentType()));
	        	try {
	        		boolean res = CloudClient.postFields(identifier, params);
	        		Log.v(TAG, "Result from posting invoice " + params + " to channel " + channel + ": " + res);
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}					

			}
		}).start();
    } else {
              handler.sendEmptyMessageDelayed(R.id.restart_preview, SCAN_DELAY_MS);
	}
    
    populateList(invoice.getReference(), invoice.getCompleteAmount(), invoice.getGiroAccount());
    onContentChanged();
  }

  /**
   * Creates the beep MediaPlayer in advance so that the sound can be triggered with the least
   * latency possible.
   */
  private void initBeepSound() {
    if (playBeep && mediaPlayer == null) {
      // The volume on STREAM_SYSTEM is not adjustable, and users found it too loud,
      // so we now play on the music stream.
      setVolumeControlStream(AudioManager.STREAM_MUSIC);
      mediaPlayer = new MediaPlayer();
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mediaPlayer.setOnCompletionListener(beepListener);

      AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
      try {
        mediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(),
            file.getLength());
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

  //TODO: all this should be done in a database probably
  private void populateList(String reference, String amount, String account) {
    list.clear();
    HashMap<String,String> temp = new HashMap<String,String>();
    temp.put("data_type", getString(R.string.reference_field));
    temp.put("data", reference);
    list.add(temp);
    HashMap<String,String> temp1 = new HashMap<String,String>();
    temp1.put("data_type", getString(R.string.amount_field));
    temp1.put("data", amount);
    list.add(temp1);
    HashMap<String,String> temp2 = new HashMap<String,String>();
    temp2.put("data_type", getString(R.string.account_field));
    temp2.put("data", account);
    list.add(temp2);
    }

}
