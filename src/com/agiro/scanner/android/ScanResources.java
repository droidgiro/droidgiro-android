package com.agiro.scanner.android;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

public final class ScanResources {

	private final String TAG = "aGiro.ScanResources";

	private Map<String,Bitmap> charMap;

	private Map<String,String> charIds = new HashMap<String,String>() {
		{
			put("#","char35_16x24");
			put("0","char48_16x24");
			put("1","char49_16x24");
			put("2","char50_16x24");
			put("3","char51_16x24");
			put("4","char52_16x24");
			put("5","char53_16x24");
			put("6","char54_16x24");
			put("7","char55_16x24");
			put("8","char56_16x24");
			put("9","char57_16x24");
			put(">","char62_16x24");
		}
	};

	public ScanResources(Context c) {
		loadCharsFromRes(c);
	}

	public ScanResources(String path) {
		loadCharsFromExt(path);
	}

	public void loadCharsFromExt(String path) {
	}

	public void loadCharsFromRes(Context c) {
		Options o = new Options();
		o.inPreferredConfig = Bitmap.Config.RGB_565;
		o.inScaled = false;
		charMap = new HashMap<String,Bitmap>();
		try {
			Class res = R.drawable.class;
			Set set = charIds.entrySet();
			Iterator it = set.iterator();
			while(it.hasNext()) {
				Map.Entry me = (Map.Entry)it.next();
				String cha = (String)me.getKey();
				String id = (String)me.getValue();
				Field field = res.getField(id);
				int drawableId = field.getInt(null);
				Bitmap chaBmp = BitmapFactory.decodeResource(
					c.getResources(), drawableId, o);
				charMap.put(cha, chaBmp);
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Failed to get character ids.", e);
		}
	}

	public Map getCharMap() {
		return charMap;
	}

}
