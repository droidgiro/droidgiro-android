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

	private Map<Character,Bitmap> charMap;

	private Map<Character,String> charIds = new HashMap<Character,String>() {
		{
			put((char)35,"char35_16x24");
			put((char)48,"char48_16x24");
			put((char)49,"char49_16x24");
			put((char)50,"char50_16x24");
			put((char)51,"char51_16x24");
			put((char)52,"char52_16x24");
			put((char)53,"char53_16x24");
			put((char)54,"char54_16x24");
			put((char)55,"char55_16x24");
			put((char)56,"char56_16x24");
			put((char)57,"char57_16x24");
			put((char)62,"char62_16x24");
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
		charMap = new HashMap<Character,Bitmap>();
		try {
			Class res = R.drawable.class;
			Set set = charIds.entrySet();
			Iterator it = set.iterator();
			while(it.hasNext()) {
				Map.Entry me = (Map.Entry)it.next();
				Character cha = (Character)me.getKey();
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

	public Map<Character,Bitmap> getCharMap() {
		return charMap;
	}

}
