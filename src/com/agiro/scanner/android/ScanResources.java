package com.agiro.scanner.android;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.util.Log;

public final class ScanResources {

	private final String TAG = "aGiro.ScanResources";
	private List<Bitmap> referenceList;

	private String[] internalChars = {
	"char48_16x24","char49_16x24","char50_16x24",
	"char51_16x24","char52_16x24","char53_16x24",
	"char54_16x24","char55_16x24","char56_16x24",
	"char57_16x24","char35_16x24","char62_16x24"
	};

	public ScanResources(Context c) {
		loadInternalChars(c);
	}

	public void loadInternalChars(Context c) {
		Options o = new Options();
		o.inPreferredConfig = Bitmap.Config.RGB_565;
		o.inScaled = false;
		referenceList = new ArrayList<Bitmap>();
		try {
			Class res = R.drawable.class;
			for (String name : internalChars) {
				Field field = res.getField(name);
				int drawableId = field.getInt(null);
				referenceList.add(BitmapFactory.decodeResource(c.getResources(), drawableId, o));
			}
		}
		catch (Exception e) {
			Log.e(TAG, "Failed to get character ids.", e);
		}
	}

	public List<Bitmap> getReferenceList() {
		return referenceList;
	}

}
