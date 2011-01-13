/*
 * Copyright (C) 2008 ZXing authors
 * Copyright (C) 2011 Agiro authors
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

package com.agiro.scanner.android;

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

import android.app.ListActivity;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
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

import com.agiro.scanner.android.camera.CameraManager;

/**
 * The main activity. Draws the views and shows the results in them.
 */
public final class CaptureActivity extends ListActivity implements SurfaceHolder.Callback {

  private static final String TAG = "aGiro.CaptureActivity";

  private static final long INTENT_RESULT_DURATION = 1500L;
  private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;
  private static final float BEEP_VOLUME = 0.10f;
  private static final long VIBRATE_DURATION = 200L;

  private static final String PACKAGE_NAME = "com.agiro.scanner.android";

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
  private static String amount = null;
  private static String account = null;
  private static String debug = null;

  private Button eraseButton;
  private Button sendButton;
  private Button scanButton;

  /*
   * When the beep has finished playing, rewind to queue up another one.
   *
  private final OnCompletionListener beepListener = new OnCompletionListener() {
    public void onCompletion(MediaPlayer mediaPlayer) {
      mediaPlayer.seekTo(0);
    }
  };
*/
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
    else if (position == 2) { account = null; }
    else                    { debug = null; }
    populateList(reference, amount, account, debug);
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

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.capture);

    this.eraseButton = (Button)this.findViewById(R.id.erase);
    this.eraseButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        reference = null;
        amount = null;
        account = null;
        debug = null;
        populateList(reference, amount, account, debug);
        onContentChanged();
      }
    });

    this.sendButton = (Button)this.findViewById(R.id.send);
    this.sendButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
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
    populateList(reference, amount, account, debug);
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
  }
/*
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
    copyToClipboard = prefs.getBoolean(PreferencesActivity.KEY_COPY_TO_CLIPBOARD, true);
    initBeepSound();
*/
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
    if (keyCode == KeyEvent.KEYCODE_SEARCH) {
      handler.sendEmptyMessage(R.id.restart_preview);
    }
      return super.onKeyDown(keyCode, event);
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

  public void handleDecode(final Invoice invoice, Bitmap debugBmp) {
//    inactivityTimer.onActivity();
//    playBeepSoundAndVibrate();
    ImageView debugImageView = (ImageView) findViewById(R.id.debug_image_view);
    debugImageView.setImageBitmap(debugBmp);
    
    Log.v(TAG, "Got invoice " + invoice);
    if(invoice.isComplete()) {
    	new Thread(new Runnable() {
			
			public void run() {
	        	AppEngineClient aec = new AppEngineClient(CaptureActivity.this, "test@example.com");
	        	List<NameValuePair> params = new ArrayList<NameValuePair>();
	        	params.add(new BasicNameValuePair("reference", invoice.getReference()));
	        	try {
	        		Writer w = new StringWriter();
					HttpResponse res = aec.makeRequest("/add", params);
					Reader reader = new BufferedReader(new InputStreamReader(res.getEntity().getContent(), "UTF-8"));
					int n;
					char[] buffer = new char[1024];
					while ((n = reader.read(buffer)) != -1) {
					w.write(buffer, 0, n);
					}
					Log.e(TAG, "Got: " + w.toString());
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}					

			}
		}).start();
    }
    
    //TODO: the isValidCC checking should be optional
//    if (resultMap.containsKey("reference")) {
//        String str = resultMap.get("reference").toString();
//        if (StringDecoder.isValidCC(str)) {
//        	new Thread(new Runnable() {
//				
//				public void run() {
//		        	AppEngineClient aec = new AppEngineClient(CaptureActivity.this, "patrik.akerfeldt@gmail.com");
//		        	List<NameValuePair> params = new ArrayList<NameValuePair>();
//		        	params.add(new BasicNameValuePair("reference", "12345678"));
//		        	try {
//		        		Writer w = new StringWriter();
//						HttpResponse res = aec.makeRequest("/add", params);
//						Reader reader = new BufferedReader(new InputStreamReader(res.getEntity().getContent(), "UTF-8"));
//						int n;
//						char[] buffer = new char[1024];
//						while ((n = reader.read(buffer)) != -1) {
//						w.write(buffer, 0, n);
//						}
//						Log.e(TAG, "Got: " + w.toString());
//					} catch (Exception e) {
//						Log.e(TAG, e.getMessage(), e);
//					}					
//
//				}
//			}).start();
//          reference = str;
//        }
//    }
//    if (resultMap.containsKey("amount")) {
//        String str = resultMap.get("amount").toString();
//        if (StringDecoder.isValidCC(str)) {
//            str = str.substring(0,str.length()-1);
//            str = new StringBuffer(str).insert((str.length()-2), ",").toString();
//            amount = str;
//        }
//    }
//    if (resultMap.containsKey("account")) {
//        String str = resultMap.get("account").toString();
//        if (StringDecoder.isValidCC(str)) {
//          account = str;
//        }
//    }
//    if (resultMap.containsKey("debug")) {
//        debug = resultMap.get("debug").toString();
//    }
    populateList(invoice.getReference(), invoice.getCompleteAmount(), invoice.getGiroAccount(), "NOT SUPPORTED");
    onContentChanged();
  }

  /*
   * Creates the beep MediaPlayer in advance so that the sound can be triggered with the least
   * latency possible.
   *
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
*/

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
  private void populateList(String reference, String amount, String account,
  String debug) {
    list.clear();
    HashMap<String,String> temp = new HashMap<String,String>();
    temp.put("data_type","Referensnummer:");
    temp.put("data", reference);
    list.add(temp);
    HashMap<String,String> temp1 = new HashMap<String,String>();
    temp1.put("data_type","Belopp:");
    temp1.put("data", amount);
    list.add(temp1);
    HashMap<String,String> temp2 = new HashMap<String,String>();
    temp2.put("data_type","Kontonummer:");
    temp2.put("data", account);
    list.add(temp2);
    HashMap<String,String> temp3 = new HashMap<String,String>();
    temp3.put("data_type","Debug:");
    temp3.put("data", debug);
    list.add(temp3);
    }

}
