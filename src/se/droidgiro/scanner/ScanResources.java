/*
 * Copyright (C) 2011 DroidGiro authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.droidgiro.scanner;

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

/**
 * Loads the the resources used by the Scanner class.
 *
 * @author wulax
 */
public final class ScanResources {

	private final String TAG = "DroidGiro.ScanResources";

	private Map<Character,Bitmap> charMap;

	//TODO: Make this more dynamic
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

	public ScanResources(Context context) {
		loadCharsFromRes(context);
	}

	public ScanResources(String path) {
		loadCharsFromExt(path);
	}

	/**
	 * Loads the the reference bitmaps from an external path into a map, with
	 * a Character as key and its corresponding Bitmap as value.
	 * @param path The path to the bitmaps.
	 */
	public void loadCharsFromExt(String path) {
	//TODO
	}

	/**
	 * Loads the the reference bitmaps from the application resources into a map,
	 * with a Character as key and its corresponding Bitmap as value.
	 * @param context The application context.
	 */
	public void loadCharsFromRes(Context context) {
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
					context.getResources(), drawableId, o);
				charMap.put(cha, chaBmp);
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Failed to get character ids.", e);
		}
	}

	/**
	 * @return The reference character map to use in the bitmap analysis method.
	 */
	public Map<Character,Bitmap> getCharMap() {
		return charMap;
	}

}
