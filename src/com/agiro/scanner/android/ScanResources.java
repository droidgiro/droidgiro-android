package com.agiro.scanner.android;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;

public final class ScanResources {

    private List<Bitmap> referenceList;

    public ScanResources(Context context) {
        Options o = new Options();
        o.inScaled = false;
        referenceList = new ArrayList<Bitmap>();
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.n0, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.n1, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.n2, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.n3, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.n4, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.n5, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.n6, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.n7, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.n8, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.n9, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.nsquare, o));
        referenceList.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.narrow, o));
  }

    public List<Bitmap> getReferenceList() {
        return referenceList;
    }

}
