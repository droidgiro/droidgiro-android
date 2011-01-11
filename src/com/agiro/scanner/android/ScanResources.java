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
	"n0","n1","n2","n3","n4","n5","n6",
	"n7","n8","n9","nsquare","narrow"
	};

	public ScanResources(Context c) {
		loadInternalChars(c);
	}

	public void loadInternalChars(Context c) {
		Options o = new Options();
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
